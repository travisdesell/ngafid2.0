package org.ngafid.flights.process;

import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.ngafid.flights.*;

public class FlightBuilder {

    private final ConcurrentHashMap<String, DoubleTimeSeries> doubleTimeSeries;
    private final ConcurrentHashMap<String, StringTimeSeries> stringTimeSeries;

    private ArrayList<Itinerary> itinerary = null;

    private String startDateTime = null, endDateTime = null;

    // Cosntrutor for each file type...
    public FlightBuilder(Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) {
        this.doubleTimeSeries = new ConcurrentHashMap<>(doubleTimeSeries);
        this.stringTimeSeries = new ConcurrentHashMap<>(stringTimeSeries);
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

    // TODO: implement this
    public void validate() {}
}
