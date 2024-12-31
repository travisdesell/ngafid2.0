package org.ngafid.maintenance;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class AircraftTimeline implements Comparable<AircraftTimeline> {
    private static final Logger LOG = Logger.getLogger(AircraftTimeline.class.getName());

    private final int flightId;
    private final LocalDate startTime;
    private final LocalDate endTime;

    private MaintenanceRecord previousEvent = null;
    private long daysSincePrevious = 0;
    private long flightsSincePrevious = -1;

    private MaintenanceRecord nextEvent = null;
    private long daysToNext = 0;
    private long flightsToNext = -1;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    //private ArrayList<AircraftTimeline> combinedRecords = new ArrayList<AircraftTimeline>();


    public int getFlightId() {
        return flightId;
    }

    public LocalDate getStartTime() {
        return startTime;
    }

    public LocalDate getEndTime() {
        return endTime;
    }

    public long getDaysToNext() {
        return daysToNext;
    }

    public long getDaysSincePrevious() {
        return daysSincePrevious;
    }

    public long getFlightsToNext() {
        return flightsToNext;
    }

    public long getFlightsSincePrevious() {
        return flightsSincePrevious;
    }

    public MaintenanceRecord getNextEvent() {
        return nextEvent;
    }

    public MaintenanceRecord getPreviousEvent() {
        return previousEvent;
    }


    public void setPreviousEvent(MaintenanceRecord record, long newDaysSincePreviousValue) {
        previousEvent = record;
        this.daysSincePrevious = newDaysSincePreviousValue;
    }

    public void setNextEvent(MaintenanceRecord record, long newDaysToNextValue) {
        nextEvent = record;
        this.daysToNext = newDaysToNextValue;
    }

    public void setFlightsSincePrevious(int flightsSincePrevious) {
        this.flightsSincePrevious = flightsSincePrevious;
    }

    public void setFlightsToNext(int flightsToNext) {
        this.flightsToNext = flightsToNext;
    }


    public AircraftTimeline(int flightId, String startTime, String endTime) {
        this.flightId = flightId;
        //System.out.println("parsing start time: '" + startTime + "'");
        this.startTime = LocalDate.parse(startTime, formatter);
        //System.out.println("parsing end time: '" + endTime + "'");
        this.endTime = LocalDate.parse(endTime, formatter);
    }

    public int compareTo(AircraftTimeline other) {
        return startTime.compareTo(other.startTime);
    }

    public String toString() {
        return "[Aircraft Timeline - flightId: '" + flightId
                + "', startTime: '" + startTime
                + "', endTime: '" + endTime
                + "', daysToNext: " + daysToNext
                + ", flightsToNext: " + flightsToNext
                + ", daysSincePrevious: " + daysSincePrevious
                + ", flightsSincePrevious: " + flightsSincePrevious
                + "]";
    }
}

