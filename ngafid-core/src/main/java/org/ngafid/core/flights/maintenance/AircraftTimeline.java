package org.ngafid.core.flights.maintenance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Logger;

public class AircraftTimeline implements Comparable<AircraftTimeline> {
    private static final Logger LOG = Logger.getLogger(AircraftTimeline.class.getName());

    private final int flightId;
    private final LocalDate startTime;
    private final LocalDate endTime;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final String startDateTimeUtc;
    private final String endDateTimeUtc;

    private MaintenanceRecord previousEvent = null;
    private long daysSincePrevious = 0;
    private long flightsSincePrevious = -1;

    private MaintenanceRecord nextEvent = null;
    private long daysToNext = 0;
    private long flightsToNext = -1;

    private static final DateTimeFormatter FORMAT_DT_SEC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMAT_DT_MIN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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

    /** Full start datetime (GMT) for phase comparison with maintenance open/close. */
    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    /** Full end datetime (GMT) for phase comparison. */
    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    /** Full start datetime string from DB (GMT). */
    public String getStartDateTimeUtc() {
        return startDateTimeUtc;
    }

    /** Full end datetime string from DB (GMT). */
    public String getEndDateTimeUtc() {
        return endDateTimeUtc;
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


    private static LocalDateTime parseDateTime(String s) {
        try {
            return LocalDateTime.parse(s, FORMAT_DT_SEC);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(s, FORMAT_DT_MIN);
        }
    }

    public AircraftTimeline(int flightId, String startTime, String endTime) {
        this.flightId = flightId;
        this.startDateTimeUtc = startTime;
        this.endDateTimeUtc = endTime;
        this.startTime = LocalDate.parse(startTime.substring(0, 10), java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        this.endTime = LocalDate.parse(endTime.substring(0, 10), java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        this.startDateTime = parseDateTime(startTime);
        this.endDateTime = parseDateTime(endTime);
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

