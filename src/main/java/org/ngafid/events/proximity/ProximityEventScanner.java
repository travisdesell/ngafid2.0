package org.ngafid.events.proximity;

import org.ngafid.common.Database;
import org.ngafid.events.*;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.Parameters;
import org.ngafid.flights.StringTimeSeries;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    }

    @Override
    protected List<String> getRequiredDoubleColumns() {
        return List.of(Parameters.ALT_MSL, Parameters.ALT_AGL, Parameters.LATITUDE, Parameters.LONGITUDE, Parameters.IAS);
    }

    public List<Event> scanFlightPair(Connection connection, Flight flight, FlightTimeLocation flightInfo, Flight otherFlight, FlightTimeLocation otherFlightInfo) throws SQLException, NullPointerException {

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
            return List.of();
        }

        // Skip the first 30 seconds as it is usually the FDR being initialized
        final int SKIP_SECONDS = 30;
        int i = SKIP_SECONDS, j = SKIP_SECONDS;

        while (i < flightInfo.epochTime.length && j < otherFlightInfo.epochTime.length) {
            // Skip entries where the epoch time was 0 (the date/time was null)
            if (flightInfo.epochTime[i] == 0) {
                i++;
                continue;
            }
            if (otherFlightInfo.epochTime[j] == 0) {
                j++;
                continue;
            }

            // Ensure both iterators are for the same time
            if (flightInfo.epochTime[i] < otherFlightInfo.epochTime[j]) {
                i++;
                continue;
            }
            if (otherFlightInfo.epochTime[j] < flightInfo.epochTime[i]) {
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

                // If an exceedence is not being tracked, startTime is null
                if (startTime == null) {

                    // Start tracking a new exceedence
                    startTime = flightInfo.dateSeries.get(i) + " " + flightInfo.timeSeries.get(i);
                    otherStartTime = otherFlightInfo.dateSeries.get(j) + " " + otherFlightInfo.timeSeries.get(j);

                    startLine = i;
                    otherStartLine = j;
                    severity = distanceFt;
                    lateralDistance = lateralDistanceFt;
                    verticalDistance = verticalDistanceFt;
                }

                endLine = i;
                otherEndLine = j;
                endTime = flightInfo.dateSeries.get(i) + " " + flightInfo.timeSeries.get(i);
                otherEndTime = otherFlightInfo.dateSeries.get(j) + " " + otherFlightInfo.timeSeries.get(j);

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
                    emitProximityEventPair(flight, flightInfo, otherFlight, otherFlightInfo, eventList, startTime, endTime, otherStartTime, otherEndTime, startLine, endLine, otherStartLine, otherEndLine, severity, lateralDistance, verticalDistance);
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
            emitProximityEventPair(flight, flightInfo, otherFlight, otherFlightInfo, eventList, startTime, endTime, otherStartTime, otherEndTime, startLine, endLine, otherStartLine, otherEndLine, severity, lateralDistance, verticalDistance);
        }

        return eventList;
    }

    private void emitProximityEventPair(Flight flight, FlightTimeLocation flightInfo, Flight otherFlight, FlightTimeLocation otherFlightInfo, ArrayList<Event> eventList, String startTime, String endTime, String otherStartTime, String otherEndTime, int startLine, int endLine, int otherStartLine, int otherEndLine, double severity, double lateralDistance, double verticalDistance) {
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

        LOG.info("Processing flight: " + flight.getId() + ", " + flight.getFilename());
        int fleetId = flight.getFleetId();
        int flightId = flight.getId();
        int airframeNameId = flight.getAirframeNameId();
        String flightFilename = flight.getFilename();

        // Get enough information about the flight to determine if we can calculate adjacencies with it
        FlightTimeLocation flightInfo = new FlightTimeLocation(connection, flight);
        boolean hasSeriesData = flightInfo.getSeriesData(connection), gotSeriesData = flightInfo.hasSeriesData(), isValid = flightInfo.isValid();
        if (!hasSeriesData || !gotSeriesData || !isValid) {
            LOG.info("Flight is invalid for some reason " + hasSeriesData + ", " + gotSeriesData + ", " + isValid);
            return List.of();
        }

        ArrayList<Flight> potentialFlights = Flight.getFlights(connection, "(id != " + flightId + " AND " +
                "start_time <= '" + flightInfo.endDateTime + "' AND end_time >= " +
                "'" + flightInfo.startDateTime + "')");

        LOG.info("Found " + potentialFlights.size() + " potential time matched flights.");

        List<Event> allEvents = new ArrayList<>();
        for (Flight otherFlight : potentialFlights) {
            LOG.info("Scanning flight pair");
            FlightTimeLocation otherFlightInfo = new FlightTimeLocation(connection, otherFlight);
            allEvents.addAll(scanFlightPair(connection, flight, flightInfo, otherFlight, otherFlightInfo));
        }

        return allEvents;
    }

    @Override
    public List<Event> scan(Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            return processFlight(connection, flight);
        }
    }
}
