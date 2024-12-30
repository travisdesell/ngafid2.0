package org.ngafid.flights.process;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.ngafid.flights.*;
import static org.ngafid.flights.process.ProcessStep.required;

/**
 * Intermediate flight representation, before it has been placed into the database. The `meta` field contains basic meta
 * information like the type of aircraft, the start and end time of the flight, etc. The actual flight data is stored in
 * the `doubleTimeSeries` and `stringTimeSeries` fields.
 *
 * @author Joshua Karns (josh@karns.dev)
 */
public class FlightBuilder {

    /**
     * Contains the double time series' for this flight. This object can safely be read and written to concurrently, but
     * you are still responsible for ensuring things are not overwritten.
     */
    private final ConcurrentHashMap<String, DoubleTimeSeries> doubleTimeSeries;
    /**
     * Same as `doubleTimeSeries`, but for String series.
     */
    private final ConcurrentHashMap<String, StringTimeSeries> stringTimeSeries;

    // A list of airports this aircraft visited.
    private ArrayList<Itinerary> itinerary = null;

    // Flight meta data - see FlightMeta definition for details.
    public final FlightMeta meta;

    // A list of non-fatal exceptions: issues with the data that don't prevent us from ingesting the data.
    public final ArrayList<MalformedFlightFileException> exceptions = new ArrayList<>();

    /**
     * Only constructor for FlightBuilder. Copies the entries in the time series maps.
     */
    public FlightBuilder(FlightMeta meta, Map<String, DoubleTimeSeries> doubleTimeSeries,
            Map<String, StringTimeSeries> stringTimeSeries) {
        this.doubleTimeSeries = new ConcurrentHashMap<>(doubleTimeSeries);
        this.stringTimeSeries = new ConcurrentHashMap<>(stringTimeSeries);
        this.meta = meta;
    }

    // The only thing we require, by default, is a start and end time.
    // TODO: Determine if this is the exact behavior we want.
    private static final List<ProcessStep.Factory> processSteps = List.of(
            required(ProcessStartEndTime::new),
            ProcessAirportProximity::new,
            ProcessLaggedAltMSL::new,
            ProcessStallIndex::new,
            ProcessTotalFuel::new,
            ProcessDivergence::new,
            ProcessLOCI::new,
            ProcessItinerary::new,
            ProcessAltAGL::new);

    /**
     * Gathers processing steps together which do not overwrite any existing time series.
     *
     * @returns A list of processing steps
     */
    protected List<ProcessStep> gatherSteps(Connection connection) {
        // Add all of our processing steps here... The order doesn't matter; the DependencyGraph will resolve the order
        // in the event that there are dependencies. Note that steps that output any columns that are already in
        // doubleTimeSeries or stringTimeSeries are ignored.
        return processSteps.stream().map(factory -> factory.create(connection, this))
                .filter(step -> step.getOutputColumns()
                        .stream()
                        .noneMatch(x -> doubleTimeSeries.contains(x) || stringTimeSeries.contains(x)))
                .collect(Collectors.toList());
    }

    /**
     * Construct and execute the dependency graph formed by the process steps. This method should only be executed
     * within some sort of `Executor` to enable concurrent flight processing.
     *
     * All recoverable exceptions / flight processing issues will be caught and stored in this flight builder, otherwise
     * this will raise a FlightProcessingException which indicates an irrecoverable issue.
     *
     * @returns A flight object
     * @throws FlightProcessingException if an irrecoverable processing issue is encountered
     */
    public Flight build(Connection connection) throws FlightProcessingException {
        DependencyGraph dg = new DependencyGraph(this, gatherSteps(connection));
        FlightProcessingException[] exception = new FlightProcessingException[] { null };

        // We can do this:
        // (1) in serial
        // (2) in parallel
        //
        // My (josh) unscientific testing shows that the parallel version is faster for tough uploads,
        // and the same speed for easy uploads

        // This is a cheeky way to execute things sequentially. This method will usually be invoked in a ThreadPool
        // anyways, so if you want to make it concurrent just call dg.compute directly
        // Executor executor = Runnable::run;
        // executor.execute(() -> {
        // try {
        // dg.compute();
        // } catch (FlightProcessingException e) {
        // exception[0] = e;
        // }
        // });

        // if (exception[0] != null) {
        // throw exception[0];
        // }

        dg.compute();

        try {
            return new Flight(connection, meta, doubleTimeSeries, stringTimeSeries, itinerary, exceptions);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new FlightProcessingException(e);
        }
    }

    // TODO: implement this hehe
    public void validate() {
    }

    protected Map<String, Set<String>> getAliases() {
        return Collections.emptyMap();
    }

    /**
     * Adds an entry to `doubleTimeSeries`, mapping the supplied name to the supplied time series.
     *
     * @returns this flight builder
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
        if (value != null)
            return value;

        var aliases = getAliases().get(name);
        if (aliases == null)
            return null;

        for (var alias : aliases) {
            value = map.get(alias);
            if (value != null)
                return value;
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

    /**
     * Fetches a double series with the supplied name.
     *
     * @returns null if there is no time series with that name
     */
    public final DoubleTimeSeries getDoubleTimeSeries(String name) {
        return getSeries(name, doubleTimeSeries);
    }

    /**
     * Returns the key set of `this.doubleTimeSeries`
     */
    public final Set<String> getDoubleTimeSeriesKeySet() {
        return getKeySet(doubleTimeSeries);
    }

    /**
     * Adds an entry to `stringTimeSeries`, mapping the supplied name to the supplied time series.
     *
     * @returns this flight builder
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
     * @returns null if there is no time series with that name
     */
    public final StringTimeSeries getStringTimeSeries(String name) {
        return getSeries(name, stringTimeSeries);
    }

    /**
     * Returns the key set of `this.stringTimeSeries`
     */
    public final Set<String> getStringTimeSeriesKeySet() {
        return getKeySet(stringTimeSeries);
    }

    /**
     * Sets the `startDateTime` field of `this.meta`. This method is synchronized to prevent concurrent access of the
     * `meta` object.
     *
     * @returns this flight builder
     */
    public synchronized FlightBuilder setStartDateTime(String startDateTime) {
        this.meta.startDateTime = startDateTime;
        return this;
    }

    /**
     * Sets the `endDateTime` field of `this.meta`. This method is synchronized to prevent concurrent access of the
     * `meta` object.
     *
     * @returns this flight builder
     */
    public synchronized FlightBuilder setEndDateTime(String endDateTime) {
        this.meta.endDateTime = endDateTime;
        return this;
    }

    /**
     * Synchronized method to set the itinerary.
     *
     * @returns this flight builder
     */
    public synchronized FlightBuilder setItinerary(ArrayList<Itinerary> itinerary) {
        this.itinerary = itinerary;
        return this;
    }

    /**
     * Masks in the supplied bits into the `processingStatus` field of `meta.` This is sychronized to avoid race
     * conditions.
     *
     * @returns this flight builder
     */
    public synchronized FlightBuilder updateProcessingStatus(int processingStatus) {
        this.meta.processingStatus |= processingStatus;
        return this;
    }
}
