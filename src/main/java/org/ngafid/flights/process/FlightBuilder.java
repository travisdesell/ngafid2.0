package org.ngafid.flights.process;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.ngafid.flights.*;
import static org.ngafid.flights.process.ProcessStep.required;
import org.ngafid.flights.process.*;

public class FlightBuilder {

    public final ConcurrentHashMap<String, DoubleTimeSeries> doubleTimeSeries;
    public final ConcurrentHashMap<String, StringTimeSeries> stringTimeSeries;

    private ArrayList<Itinerary> itinerary = null;

    public final FlightMeta meta;

    public final ArrayList<MalformedFlightFileException> exceptions = new ArrayList<>();

    public FlightBuilder(FlightMeta meta, Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) {
        this.doubleTimeSeries = new ConcurrentHashMap<>(doubleTimeSeries);
        this.stringTimeSeries = new ConcurrentHashMap<>(stringTimeSeries);
        this.meta = meta;
    }

    public FlightBuilder addTimeSeries(String name, DoubleTimeSeries timeSeries) {
        doubleTimeSeries.put(name, timeSeries);
        return this;
    }
    
    public FlightBuilder addTimeSeries(String name, StringTimeSeries timeSeries) {
        stringTimeSeries.put(name, timeSeries);
        return this;
    }

    public synchronized FlightBuilder setStartDateTime(String startDateTime) {
        this.meta.startDateTime = startDateTime;
        return this;
    }

    public synchronized FlightBuilder setEndDateTime(String endDateTime) {
        this.meta.endDateTime = endDateTime;
        return this;
    }

    public synchronized FlightBuilder setItinerary(ArrayList<Itinerary> itinerary) {
        this.itinerary = itinerary;
        return this;
    }

    public synchronized FlightBuilder updateProcessingStatus(int processingStatus) {
        this.meta.processingStatus |= processingStatus;
        return this;
    }

    private static final List<ProcessStep.Factory> processSteps = List.of(
        required(ProcessAltAGL::new),
        required(ProcessAirportProximity::new),
        required(ProcessStartEndTime::new),
        ProcessLaggedAltMSL::new,
        ProcessStallIndex::new,
        ProcessTotalFuel::new,
        ProcessDivergence::new,
        ProcessLOCI::new,
        ProcessItinerary::new
    );

    // This can be overridden.
    protected List<ProcessStep> gatherSteps(Connection connection) {
        // Add all of our processing steps here...
        // The order doesn't matter; the DependencyGraph will resolve
        // the order in the event that there are dependencies.
        return processSteps.stream().map(factory -> factory.create(connection, this))
          .filter(step -> 
              step.getOutputColumns()
              .stream()
              .anyMatch(x -> doubleTimeSeries.contains(x) || stringTimeSeries.contains(x))
          ).collect(Collectors.toList());
    }

    // throws a flight processing exception if an unrecoverable error occurred.
    public Flight build(Connection connection) throws FlightProcessingException {
        DependencyGraph dg = new DependencyGraph(this, gatherSteps(connection));

        dg.compute();

        // TODO: Make sure headers are calculated appropriately.
        // TODO: Make sure hasAGL and hasCoords get set correctly
        try {
            return new Flight(connection, meta, doubleTimeSeries, stringTimeSeries, itinerary, exceptions);
        } catch (SQLException e) {
            throw new FlightProcessingException(e);
        }
    }

    // TODO: implement this
    public void validate() {}
}
