package org.ngafid.core.flights.maintenance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Logger;

/**
 * One flight positioned on an aircraft's maintenance timeline, linking it to the surrounding maintenance events.
 *
 * <p>Records the flight's time window and tracks the previous and next {@link MaintenanceRecord} relative to the
 * flight along with the elapsed days and flight counts to each, so flights can be ordered and related to maintenance
 * activity. Instances are naturally ordered by their timeline position.
 */
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

    // private ArrayList<AircraftTimeline> combinedRecords = new ArrayList<AircraftTimeline>();

    public int getFlightId() {
        return flightId;
    }

    public LocalDate getStartTime() {
        return startTime;
    }

    public LocalDate getEndTime() {
        return endTime;
    }

    /**
     * Full start datetime (GMT) for phase comparison with maintenance open/close.
     *
     * @return full start datetime (GMT)
     */
    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    /**
     * Full end datetime (GMT) for phase comparison.
     *
     * @return full end datetime (GMT)
     */
    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    /**
     * Full start datetime string from DB (GMT).
     *
     * @return full start datetime string from DB (GMT)
     */
    public String getStartDateTimeUtc() {
        return startDateTimeUtc;
    }

    /**
     * Full end datetime string from DB (GMT).
     *
     * @return full end datetime string from DB (GMT)
     */
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

    /**
     * Links the most recent maintenance event preceding this flight and records how many days elapsed between that
     * event and this flight.
     *
     * @param record the preceding maintenance record
     * @param newDaysSincePreviousValue the number of days from that record to this flight
     */
    public void setPreviousEvent(MaintenanceRecord record, long newDaysSincePreviousValue) {
        previousEvent = record;
        this.daysSincePrevious = newDaysSincePreviousValue;
    }

    /**
     * Links the next maintenance event following this flight and records how many days remain from this flight
     * until that event.
     *
     * @param record the following maintenance record
     * @param newDaysToNextValue the number of days from this flight to that record
     */
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

    /**
     * Constructs a timeline entry for one flight from its GMT start and end datetime strings. The raw strings are
     * retained, the date portion (first 10 characters) is parsed into {@link LocalDate} bounds, and the full strings
     * are parsed into {@link LocalDateTime} values accepting either second- or minute-precision formats.
     *
     * @param flightId the flight this timeline entry represents
     * @param startTime the flight's GMT start datetime string ({@code yyyy-MM-dd HH:mm[:ss]})
     * @param endTime the flight's GMT end datetime string ({@code yyyy-MM-dd HH:mm[:ss]})
     */
    public AircraftTimeline(int flightId, String startTime, String endTime) {
        this.flightId = flightId;
        this.startDateTimeUtc = startTime;
        this.endDateTimeUtc = endTime;
        this.startTime = LocalDate.parse(startTime.substring(0, 10), java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        this.endTime = LocalDate.parse(endTime.substring(0, 10), java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        this.startDateTime = parseDateTime(startTime);
        this.endDateTime = parseDateTime(endTime);
    }

    /**
     * Orders timeline entries chronologically by their flight start date.
     *
     * @param other the entry to compare against
     * @return a negative, zero, or positive value as this entry's start date is before, equal to, or after the other's
     */
    @Override
    public int compareTo(AircraftTimeline other) {
        return startTime.compareTo(other.startTime);
    }

    /**
     * Returns a human-readable summary of this timeline entry: flight id, start/end dates, and the day/flight gaps
     * to the surrounding maintenance events.
     *
     * @return a debug string describing this timeline entry
     */
    @Override
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
