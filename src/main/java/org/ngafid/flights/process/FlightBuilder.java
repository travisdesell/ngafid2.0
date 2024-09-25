package org.ngafid.flights.process;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.ngafid.flights.*;
import static org.ngafid.flights.process.ProcessStep.required;
import org.ngafid.flights.process.*;

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
    public final ConcurrentHashMap<String, DoubleTimeSeries> doubleTimeSeries;
    /**
     * Same as `doubleTimeSeries`, but for String series.
     */
    public final ConcurrentHashMap<String, StringTimeSeries> stringTimeSeries;

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

    /**
     * Adds an entry to `doubleTimeSeries`, mapping the supplied name to the supplied time series.
     *
     * @returns this flight builder
     */
    public FlightBuilder addTimeSeries(String name, DoubleTimeSeries timeSeries) {
        doubleTimeSeries.put(name, timeSeries);
        return this;
    }

    /**
     * Adds an entry to `stringTimeSeries`, mapping the supplied name to the supplied time series.
     *
     * @returns this flight builder
     */
    public FlightBuilder addTimeSeries(String name, StringTimeSeries timeSeries) {
        stringTimeSeries.put(name, timeSeries);
        return this;
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
            ProcessItinerary::new);

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

        // This is cheeky
        Executor executor = Runnable::run;
        executor.execute(() -> {
            try {
                dg.compute();
            } catch (FlightProcessingException e) {
                exception[0] = e;
            }
        });

        if (exception[0] != null) {
            throw exception[0];
        }

        try {
            return new Flight(connection, meta, doubleTimeSeries, stringTimeSeries, itinerary, exceptions);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new FlightProcessingException(e);
        }
    }

    // TODO: implement this
    public void validate() {
    }
}
