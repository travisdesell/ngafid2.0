package org.ngafid.flights.process;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.ngafid.flights.*;
import org.ngafid.flights.process.*;

public class FlightBuilder {

    public final ConcurrentHashMap<String, DoubleTimeSeries> doubleTimeSeries;
    public final ConcurrentHashMap<String, StringTimeSeries> stringTimeSeries;

    private ArrayList<Itinerary> itinerary = null;
    private String  startDateTime = null,
                    endDateTime = null;

    public final int fleetId;
    public final String  airframeName,
                          tailNumber;

    public final ArrayList<MalformedFlightFileException> exceptions = new ArrayList<>();

    public FlightBuilder(int fleetId, String tailNumber, String airframeName, Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) {
        this.doubleTimeSeries = new ConcurrentHashMap<>(doubleTimeSeries);
        this.stringTimeSeries = new ConcurrentHashMap<>(stringTimeSeries);
        this.fleetId = fleetId;
        this.airframeName = airframeName;
        this.tailNumber = tailNumber;
    }

    public void addTimeSeries(String name, DoubleTimeSeries timeSeries) {
        doubleTimeSeries.put(name, timeSeries);
    }
    
    public void addTimeSeries(String name, StringTimeSeries timeSeries) {
        stringTimeSeries.put(name, timeSeries);
    }

    public synchronized void setStartDateTime(String startDateTime) {
        this.startDateTime = startDateTime;
    }

    public synchronized void setEndDateTime(String endDateTime) {
        this.endDateTime = endDateTime;
    }

    public synchronized void setItinerary(ArrayList<Itinerary> itinerary) {
        this.itinerary = itinerary;
    }

    // This can be overridden.
    public List<ProcessStep> gatherSteps(Connection connection) {
        // Add all of our processing steps here...
        // The order doesn't matter; the DependencyGraph will resolve
        // the order in the event that there are dependencies.
        return List.of(new ProcessAltAGL(connection, this));
    }

    // throws a flight processing exception if an unrecoverable error occurred.
    public Flight build(Connection connection) throws FlightProcessingException {
        DependencyGraph dg = new DependencyGraph(this, gatherSteps());

        dg.compute();

        // TODO: Make sure headers are calculated appropriately.
        // TODO: Make sure hasAGL and hasCoords get set correctly
        return new Flight(fleetId, tailNumber, airframeName, doubleTimeSeries, stringTimeSeries, exceptions);
    }

    // TODO: implement this
    public void validate() {}
}
