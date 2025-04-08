package org.ngafid.events.proximity;

import org.jline.utils.Log;
import org.ngafid.common.Database;
import org.ngafid.common.TimeUtils;
import org.ngafid.events.*;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.Parameters;
import org.ngafid.flights.StringTimeSeries;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Logger;

import static org.ngafid.events.proximity.CalculateProximity.*;

/**
 * Refactored proximity event scanner. Still could use an overhaul, this is directly derived from the original code.
 */
public class ProximityEventScanner extends AbstractEventScanner {
    private static Logger LOG = Logger.getLogger(ProximityEventScanner.class.getName());

    private final Flight flight;

    public ProximityEventScanner(Flight flight, EventDefinition eventDefinition) {
        super(eventDefinition);
        this.flight = flight;
        // Set the column names to only include what we need
        TreeSet<String> requiredColumns = new TreeSet<>();
        requiredColumns.add(Parameters.ALT_AGL);
        requiredColumns.add(Parameters.LATITUDE);
        requiredColumns.add(Parameters.LONGITUDE);
        requiredColumns.add(Parameters.ALT_MSL);
        requiredColumns.add(Parameters.IAS);
        requiredColumns.add(Parameters.UNIX_TIME_SECONDS);
        requiredColumns.add(Parameters.UTC_DATE_TIME);

        //definition.setColumnNames(requiredColumns);

        try (Connection connection = Database.getConnection()) {
            this.gatherRequiredColumns(connection, flight);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load required columns for proximity scanner");
        }

    }

    @Override
    protected List<String> getRequiredDoubleColumns() {
        LOG.info("Getting required double columns for ProximityEventScanner");
        return List.of(Parameters.ALT_AGL, Parameters.LATITUDE, Parameters.LONGITUDE);
    }

    public List<Event> scanFlightPair(Connection connection, Flight flight, FlightTimeLocation flightInfo, Flight otherFlight, FlightTimeLocation otherFlightInfo) throws SQLException, NullPointerException {
        LOG.info("Proximity event. ScanFlight pair");
        ArrayList<Event> eventList = new ArrayList<>();
        String startTime = null;
        String endTime = null;
        String otherStartTime = null;
        String otherEndTime = null;

        int startLine = -1;
        int endLine = -1;
        int otherStartLine = -1;
        int otherEndLine = -1;

        int startCount = 0;
        int stopCount = 0;
        double severity = 0;
        double lateralDistance = 0;
        double verticalDistance = 0;

        /*
         * TODO:
         * Should probably grab these from the database event definition instead of hard-coding them;
         * but we don't need to pull the event definition so this is a tad bit faster.
         */
        final int startBuffer = 1;
        final int stopBuffer = 30;

        if (!flightInfo.hasRegionOverlap(otherFlightInfo) || !otherFlightInfo.getSeriesData(connection)) {
            LOG.info("No region overlap or missing series data - Flight IDs: " + flight.getId() + " and " + otherFlight.getId());
            return List.of();
        }

        LOG.info("Found region overlap - Flight IDs: " + flight.getId() + " and " + otherFlight.getId());
        // Skip the first 30 seconds as it is usually the FDR being initialized
        final int SKIP_SECONDS = 30;
        int i = SKIP_SECONDS, j = SKIP_SECONDS;

        while (i < flightInfo.epochTime.size() && j < otherFlightInfo.epochTime.size()) {
            // Skip entries where the epoch time was 0 (the date/time was null)
            if (flightInfo.epochTime.get(i) == 0) {
                i++;
                continue;
            }
            if (otherFlightInfo.epochTime.get(j) == 0) {
                j++;
                continue;
            }

            // Ensure both iterators are for the same time
            if (flightInfo.epochTime.get(i) < otherFlightInfo.epochTime.get(j)) {
                i++;
                continue;
            }
            if (otherFlightInfo.epochTime.get(j) < flightInfo.epochTime.get(i)) {
                j++;
                continue;
            }

            double distanceFt = calculateDistance(flightInfo.latitude[i], flightInfo.longitude[i],
                    flightInfo.altitudeMSL[i], otherFlightInfo.latitude[j], otherFlightInfo.longitude[j],
                    otherFlightInfo.altitudeMSL[j]);
            double lateralDistanceFt = calculateLateralDistance(flightInfo.latitude[i],
                    flightInfo.longitude[i], otherFlightInfo.latitude[j], otherFlightInfo.longitude[j]);
            double verticalDistanceFt = calculateVerticalDistance(flightInfo.altitudeMSL[i],
                    otherFlightInfo.altitudeMSL[j]);

            final double maxDistanceFt = 1000.0;
            final double minAltitudeAgl = 50.0;
            final double minAirspeed = 20.0;

            boolean distanceCheck = (distanceFt < maxDistanceFt);
            boolean altitudeCheck =
                    (flightInfo.altitudeAGL[i] >= minAltitudeAgl) && (otherFlightInfo.altitudeAGL[j] >= minAltitudeAgl);
            boolean airspeedCheck =
                    (flightInfo.indicatedAirspeed[i] > minAirspeed) && (otherFlightInfo.indicatedAirspeed[j] > minAirspeed);

            if (distanceCheck && altitudeCheck && airspeedCheck) {
                LOG.info("Proximity condition met - Distance: " + distanceFt + "ft, Altitude AGL: " + flightInfo.altitudeAGL[i] + "m, Airspeed: " + flightInfo.indicatedAirspeed[i] + "kt");
                // If an exceedence is not being tracked, startTime is null
                if (startTime == null) {

                    // Start tracking a new exceedence
                    startTime = flightInfo.utc.get(i);
                    otherStartTime = otherFlightInfo.utc.get(j);

                    startLine = i;
                    otherStartLine = j;
                    severity = distanceFt;
                    lateralDistance = lateralDistanceFt;
                    verticalDistance = verticalDistanceFt;
                }

                endLine = i;
                otherEndLine = j;
                endTime = flightInfo.utc.get(i);
                otherEndTime = otherFlightInfo.utc.get(j);

                // New closest proximity this time, update the severity
                if (distanceFt < severity) {

                    LOG.info("CalculateProximity.java -- New smallest distance: " + distanceFt + " < "
                            + severity + " at time " + endTime + " and other time " + otherEndTime
                            + " with lateral distance: " + lateralDistanceFt + " and vertical distance: "
                            + verticalDistanceFt);
                    severity = distanceFt;

                    // Record Lateral & Vertical Distances when the new smallest Euclidean distance is found
                    lateralDistance = lateralDistanceFt;
                    verticalDistance = verticalDistanceFt;

                }

                // Increment the startCount, reset the stopCount
                startCount++;
                stopCount = 0;

            } else if (startTime != null && ++stopCount == stopBuffer) {
                // Not enough triggers to reach the start count, don't create the event (i.e. do
                // nothing)
                if (startCount < startBuffer) {
                    LOG.info("CalculateProximity.java -- Not enough triggers, will not create " +
                            "Proximity event: " + endTime + " - " + startTime + " = " +
                            (Duration.between(Instant.parse(startTime),
                                    Instant.parse(endTime)).toMillis() / 1000.0) + " seconds");
                } else {
                    // Had enough triggers to reach the start count, create the event
                    LOG.info("(A) Creating events for flights with IDs : " + flight.getId() + " and " + otherFlight.getId());
                    emitProximityEventPair(flight, flightInfo, otherFlight, otherFlightInfo, eventList, TimeUtils.parseUTC(startTime), TimeUtils.parseUTC(endTime), TimeUtils.parseUTC(otherStartTime), TimeUtils.parseUTC(otherEndTime), startLine, endLine, otherStartLine, otherEndLine, severity, lateralDistance, verticalDistance);
                }

                // Reset the event values
                startTime = null;
                otherStartTime = null;
                endTime = null;
                otherEndTime = null;
                startLine = -1;
                endLine = -1;
                otherEndLine = -1;

                // Reset the start and stop counts
                startCount = 0;
                stopCount = 0;
            }

            i += 1;
            j += 1;
        }

        // An event was still going when one flight ended, create it and add it to the list...
        if (startTime != null) {
            LOG.info("(B) Creating events for flights with IDs : " + flight.getId() + " and " + otherFlight.getId());
            emitProximityEventPair(flight, flightInfo, otherFlight, otherFlightInfo, eventList, TimeUtils.parseUTC(startTime), TimeUtils.parseUTC(endTime), TimeUtils.parseUTC(otherStartTime), TimeUtils.parseUTC(otherEndTime), startLine, endLine, otherStartLine, otherEndLine, severity, lateralDistance, verticalDistance);
        }

        LOG.info("Finished scanFlightPair - Found " + eventList.size() + " events");
        return eventList;
    }

    private void emitProximityEventPair(Flight flight, FlightTimeLocation flightInfo, Flight otherFlight, FlightTimeLocation otherFlightInfo, ArrayList<Event> eventList, OffsetDateTime startTime, OffsetDateTime endTime, OffsetDateTime otherStartTime, OffsetDateTime otherEndTime, int startLine, int endLine, int otherStartLine, int otherEndLine, double severity, double lateralDistance, double verticalDistance) {
        Event event = new Event(startTime, endTime, startLine, endLine, super.definition.getId(), severity,
                flight.getId(), otherFlight.getId());
        Event otherEvent = new Event(otherStartTime, otherEndTime, otherStartLine,
                otherEndLine, super.definition.getId(), severity, otherFlight.getId(), flight.getId());

        EventMetaData lateralDistanceMetaData = new EventMetaData(EventMetaData.EventMetaDataKey.LATERAL_DISTANCE,
                lateralDistance);
        EventMetaData verticalDistanceMetaData = new EventMetaData(EventMetaData.EventMetaDataKey.VERTICAL_DISTANCE,
                verticalDistance);

        event.addMetaData(lateralDistanceMetaData);
        event.addMetaData(verticalDistanceMetaData);
        otherEvent.addMetaData(lateralDistanceMetaData);
        otherEvent.addMetaData(verticalDistanceMetaData);

        if (severity > 0) {
            double[] rateOfClosureArray = calculateRateOfClosure(flightInfo,
                    otherFlightInfo, startLine, endLine, otherStartLine, otherEndLine);
            RateOfClosure rateOfClosure = new RateOfClosure(rateOfClosureArray);
            event.setRateOfClosure(rateOfClosure);
            otherEvent.setRateOfClosure(rateOfClosure);
        }

        addProximityIfNotInList(eventList, event);
        addProximityIfNotInList(eventList, otherEvent);
    }

    public List<Event> processFlight(Connection connection, Flight flight) throws SQLException {
        LOG.info("Starting processFlight - Flight ID: " + flight.getId() + ", Filename: " + flight.getFilename());
        int fleetId = flight.getFleetId();
        int flightId = flight.getId();
        int airframeNameId = flight.getAirframeNameId();
        String flightFilename = flight.getFilename();

        // Get enough information about the flight to determine if we can calculate adjacencies with it
        FlightTimeLocation flightInfo = new FlightTimeLocation(connection, flight);
        boolean hasSeriesData = flightInfo.getSeriesData(connection), gotSeriesData = flightInfo.hasSeriesData(), isValid = flightInfo.isValid();

        if (flight.getFilename().contains("parquet")){
            isValid = true;
        }

        if (!hasSeriesData || !gotSeriesData || !isValid) {
            LOG.warning("Flight validation failed - hasSeriesData: " + hasSeriesData + ", gotSeriesData: " + gotSeriesData + ", isValid: " + isValid);
            return List.of();
        }

        ArrayList<Flight> potentialFlights = Flight.getFlights(connection, "(id != " + flightId + " AND " +
                "start_time <= '" + flightInfo.endDateTime + "' AND end_time >= " +
                "'" + flightInfo.startDateTime + "')");

        LOG.info("Found " + potentialFlights.size() + " potential time matched flights for Flight ID: " + flight.getId());

        List<Event> allEvents = new ArrayList<>();
        for (Flight otherFlight : potentialFlights) {
            LOG.info("Scanning flight pair - Flight IDs: " + flight.getId() + " and " + otherFlight.getId());
            FlightTimeLocation otherFlightInfo = new FlightTimeLocation(connection, otherFlight);
            allEvents.addAll(scanFlightPair(connection, flight, flightInfo, otherFlight, otherFlightInfo));
        }

        LOG.info("Finished processFlight - Total events found: " + allEvents.size());
        return allEvents;
    }

    @Override
    public List<Event> scan(Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            List<Event> events = processFlight(connection, flight);
            LOG.info("Completed scan method - Found " + events.size() + " events");
            return events;
        }
    }
}
