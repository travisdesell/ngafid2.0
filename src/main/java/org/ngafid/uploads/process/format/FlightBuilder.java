package org.ngafid.uploads.process.format;

import org.ngafid.events.Event;
import org.ngafid.events.EventDefinition;
import org.ngafid.events.calculations.TurnToFinal;
import org.ngafid.flights.*;
import org.ngafid.uploads.process.DependencyGraph;
import org.ngafid.uploads.process.FlightMeta;
import org.ngafid.uploads.process.FlightProcessingException;
import org.ngafid.uploads.process.MalformedFlightFileException;
import org.ngafid.uploads.process.steps.*;

import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.ngafid.uploads.process.steps.ComputeStep.required;

/**
 * Intermediate flight representation, before it has been placed into the database. The `meta` field contains basic
 * meta information like the type of aircraft, the start and end time of the flight, etc. The actual flight data is
 * stored in the `doubleTimeSeries` and `stringTimeSeries` fields.
 *
 * @author Joshua Karns (josh@karns.dev)
 */
public class FlightBuilder {
    private static Logger LOG = Logger.getLogger(FlightBuilder.class.getName());

    //CHECKSTYLE:OFF
    // Note that event steps will be added at a separate time as they are a bit more dynamic.
    private static final List<ComputeStep.Factory> PROCESS_STEPS = List.of(
            required(ComputeStartEndTime::new),
            ComputeAirportProximity::new,
            ComputeLaggedAltMSL::new,
            ComputeStallIndex::new,
            ComputeTotalFuel::new,
            ComputeDivergence::new,
            ComputeLOCI::new,
            ComputeItinerary::new,
            ComputeAltAGL::new,
            ComputeTurnToFinal::new
    );

    // Flight metadata - see FlightMeta definition for details.
    public final FlightMeta meta;

    // A list of non-fatal exceptions: issues with the data that don't prevent us from ingesting the data.
    public final ArrayList<MalformedFlightFileException> exceptions = new ArrayList<>();
    /**
     * Contains the double time series' for this flight. This object can safely be read and written to concurrently,
     * but you are still responsible for ensuring things are not overwritten.
     */
    private final ConcurrentHashMap<String, DoubleTimeSeries> doubleTimeSeries;
    /**
     * Same as `doubleTimeSeries`, but for String series.
     */
    private final ConcurrentHashMap<String, StringTimeSeries> stringTimeSeries;
    //CHECKSTYLE:ON
    // A list of airports this aircraft visited.
    private ArrayList<Itinerary> itinerary = new ArrayList<>();

    /**
     * List of events found in this flight.
     */
    private final ArrayList<Event> events = new ArrayList<>();

    /**
     * Turn to final objects for this flight.
     */
    private final ArrayList<TurnToFinal> turnToFinals = new ArrayList<>();

    /**
     * List of all events definitions the flight data was searched for.
     */
    private final ArrayList<EventDefinition> computedEvents = new ArrayList<>();

    /**
     * The flight object of this flight builder - will only be populated after FlightBuilder::build is called.
     */
    private Flight flight = null;

    /**
     * Only constructor for FlightBuilder. Copies the entries in the time series maps.
     *
     * @param meta             The metadata for the flight
     * @param doubleTimeSeries The double time series for the flight
     * @param stringTimeSeries The string time series for the flight
     */
    public FlightBuilder(FlightMeta meta, Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String,
            StringTimeSeries> stringTimeSeries) {
        this.doubleTimeSeries = new ConcurrentHashMap<>(doubleTimeSeries);
        this.stringTimeSeries = new ConcurrentHashMap<>(stringTimeSeries);
        this.meta = meta;
    }

    /**
     * Gathers processing steps together which do not overwrite any existing time series.
     *
     * @param connection The connection to the database
     * @return A list of processing steps
     */
    protected List<ComputeStep> gatherSteps(Connection connection) {
        // Add all of our processing steps here... The order doesn't matter; the DependencyGraph will resolve the order
        // in the event that there are dependencies. Note that steps that output any columns that are already in
        // doubleTimeSeries or stringTimeSeries are ignored.
        ArrayList<ComputeStep> steps =
                Stream.concat(PROCESS_STEPS.stream()
                                .map(factory -> factory.create(connection, this))
                                .filter(step -> step.getOutputColumns().stream()
                                        .noneMatch(x -> doubleTimeSeries.containsKey(x) || stringTimeSeries.containsKey(x))),
                        ComputeEvent.getAllApplicable(connection, this).stream()
                ).collect(Collectors.toCollection(ArrayList::new));

        // Some file processors will compute this, others will not. If we don't have UTC_DATE_TIME, add it as a required step.
        if (!doubleTimeSeries.containsKey(Parameters.UNIX_TIME_SECONDS) || !stringTimeSeries.containsKey(Parameters.UTC_DATE_TIME)) {
            steps.add(required(ComputeUTCTime::new).create(connection, this));
        }

        return steps;
    }

    /**
     * Construct and execute the dependency graph formed by the process steps. This method should only be executed
     * within some sort of `Executor` to enable concurrent flight processing.
     * <p>
     * All recoverable exceptions / flight processing issues will be caught and stored in this flight builder, otherwise
     * this will raise a FlightProcessingException which indicates an irrecoverable issue.
     *
     * @param connection The connection to the database
     * @return A flight object
     * @throws FlightProcessingException if an irrecoverable processing issue is encountered
     */
    public FlightBuilder build(Connection connection) throws FlightProcessingException {
        DependencyGraph dg = new DependencyGraph(this, gatherSteps(connection));

        // We can process individual steps in parallel as well, but it might not be worth the overhead.
        // dg.computeParallel();
        dg.computeSequential();

        flight = new Flight(meta, doubleTimeSeries, stringTimeSeries, itinerary, exceptions, events);

        return this;
    }

    public final Flight getFlight() {
        return flight;
    }

    protected Map<String, Set<String>> getAliases() {
        return Collections.emptyMap();
    }

    /**
     * Adds an entry to `doubleTimeSeries`, mapping the supplied name to the supplied time series.
     *
     * @param name       The name of the time series
     * @param timeSeries The time series to add
     * @return this flight builder
     */
    public FlightBuilder addTimeSeries(String name, DoubleTimeSeries timeSeries) {
        doubleTimeSeries.put(name, timeSeries);
        return this;
    }

    public FlightBuilder addTimeSeries(DoubleTimeSeries timeSeries) {
        return addTimeSeries(timeSeries.getName(), timeSeries);
    }

    private <T> T getSeries(String name, Map<String, T> map) {
        T value = map.get(name);
        if (value != null) return value;

        var aliases = getAliases().get(name);
        if (aliases == null) return null;

        for (var alias : aliases) {
            value = map.get(alias);
            if (value != null) return value;
        }

        return null;
    }

    private <T> Set<String> getKeySet(Map<String, T> map) {
        var set = new HashSet<>(map.keySet());

        for (Map.Entry<String, Set<String>> alias : getAliases().entrySet()) {
            for (var a : alias.getValue()) {
                if (set.contains(a)) {
                    set.add(alias.getKey());
                }
            }
        }

        return set;
    }

    public final Map<String, DoubleTimeSeries> getDoubleTimeSeriesMap() {
        return doubleTimeSeries;
    }

    /**
     * Fetches a double series with the supplied name.
     *
     * @param name The name of the time series
     * @return null if there is no time series with that name
     */
    public final DoubleTimeSeries getDoubleTimeSeries(String name) {
        return getSeries(name, doubleTimeSeries);
    }

    /**
     * Returns the key set of `this.doubleTimeSeries`
     *
     * @return the key set of `this.doubleTimeSeries`
     */
    public final Set<String> getDoubleTimeSeriesKeySet() {
        return getKeySet(doubleTimeSeries);
    }

    /**
     * Adds an entry to `stringTimeSeries`, mapping the supplied name to the supplied time series.
     *
     * @param name       The name of the time series
     * @param timeSeries The time series to add
     * @return this flight builder
     */
    public final FlightBuilder addTimeSeries(String name, StringTimeSeries timeSeries) {
        stringTimeSeries.put(name, timeSeries);
        return this;
    }

    public final FlightBuilder addTimeSeries(StringTimeSeries timeSeries) {
        return addTimeSeries(timeSeries.getName(), timeSeries);
    }

    /**
     * Fetches a string series with the supplied name.
     *
     * @param name The name of the time series
     * @return null if there is no time series with that name
     */
    public final StringTimeSeries getStringTimeSeries(String name) {
        return getSeries(name, stringTimeSeries);
    }

    /**
     * Returns the key set of `this.stringTimeSeries`
     *
     * @return the key set of `this.stringTimeSeries`
     */
    public final Set<String> getStringTimeSeriesKeySet() {
        return getKeySet(stringTimeSeries);
    }

    /**
     * Sets the `startDateTime` field of `this.meta`. This method is synchronized to prevent concurrent access of the
     * `meta` object.
     *
     * @param startDateTime The start date time
     * @return this flight builder
     */
    public synchronized FlightBuilder setStartDateTime(OffsetDateTime startDateTime) {
        this.meta.startDateTime = startDateTime;
        return this;
    }

    /**
     * Sets the `endDateTime` field of `this.meta`. This method is synchronized to prevent concurrent access of the
     * `meta` object.
     *
     * @param odt the offset date time the flight ends at
     * @return this flight builder
     */
    public synchronized FlightBuilder setEndDateTime(OffsetDateTime odt) {
        this.meta.endDateTime = odt;
        return this;
    }

    /**
     * Synchronized method to set the itinerary.
     *
     * @param newItinerary The itinerary
     * @return this flight builder
     */
    public synchronized FlightBuilder setItinerary(ArrayList<Itinerary> newItinerary) {
        this.itinerary = newItinerary;
        return this;
    }

    public List<Itinerary> getItinerary() {
        return itinerary;
    }

    /**
     * Masks in the supplied bits into the `processingStatus` field of `meta.` This is sychronized to avoid race
     * conditions.
     *
     * @param processingStatus The bits to mask in
     * @return this flight builder
     */
    public synchronized FlightBuilder updateProcessingStatus(int processingStatus) {
        this.meta.processingStatus |= processingStatus;
        return this;
    }

    public synchronized void emitEvent(Event event) {
        this.events.add(event);
    }

    public synchronized void emitEvents(List<Event> events) {
        this.events.addAll(events);
    }

    public List<Event> getEvents() {
        return events;
    }

    public synchronized void emitTurnToFinals(List<TurnToFinal> turnToFinals) {
        this.turnToFinals.addAll(turnToFinals);
    }

    public ArrayList<TurnToFinal> getTurnToFinals() {
        return turnToFinals;
    }

    public synchronized void addComputedEvent(EventDefinition eventDefinition) {
        this.computedEvents.add(eventDefinition);
    }

    public List<EventDefinition> getEventDefinitions() {
        return computedEvents;
    }

    public Map<String, StringTimeSeries> getStringTimeSeriesMap() {
        return stringTimeSeries;
    }
}
