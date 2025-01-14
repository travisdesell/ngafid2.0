package org.ngafid.proximity;

import org.ngafid.Database;
import org.ngafid.UploadProcessedEmail;
import org.ngafid.airports.Airports;
import org.ngafid.events.Event;
import org.ngafid.events.EventMetaData;
import org.ngafid.events.EventStatistics;
import org.ngafid.events.RateOfClosure;
import org.ngafid.flights.Flight;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class CalculateProximity {

    public static class EventStats {

        int eventCount = 0;
        double sumDuration = 0.0;
        double sumSeverity = 0.0;
        double minSeverity = Double.MAX_VALUE;
        double maxSeverity = -Double.MAX_VALUE;
        double minDuration = Double.MAX_VALUE;
        double maxDuration = -Double.MAX_VALUE;

        boolean entryAlreadyExists = false;

    }

    private static final Map<Integer, EventStats> flightEventStats = new HashMap<>();

    private static final Logger LOG = Logger.getLogger(CalculateProximity.class.getName());

    // Proximity events (and potentially other complicated event calculations) will have negative IDs so they
    // can be excluded from the regular event calculation process
    public static final int ADJACENCY_EVENT_DEFINITION_ID = -1;

    // Use this to get a representation of a flight's current time, position and altitude

    public static long timeMatchFlights = 0;
    public static long locMatchFlights = 0;
    public static long eventsFound = 0;

    /*
     * static String timeSeriesName = "Lcl Time";
     * static String dateSeriesName = "Lcl Date";
     */

    public static double calculateDistance(
            double flightLatitude, double flightLongitude, double flightAltitude,
            double otherFlightLatitude, double otherFlightLongitude, double otherFlightAltitude) {

        double lateralDistance = Airports.calculateDistanceInFeet(flightLatitude, flightLongitude, otherFlightLatitude,
                otherFlightLongitude);
        double altDiffFt = Math.abs(flightAltitude - otherFlightAltitude);

        double distanceFt = Math.sqrt((lateralDistance * lateralDistance) + (altDiffFt * altDiffFt));
        return distanceFt;

    }

    public static double calculateLateralDistance(double flightLatitude, double flightLongitude,
                                                  double otherFlightLatitude, double otherFlightLongitude) {

        double lateralDistance = Airports.calculateDistanceInFeet(flightLatitude, flightLongitude, otherFlightLatitude,
                otherFlightLongitude);
        return lateralDistance;

    }

    public static double calculateVerticalDistance(double flightAltitude, double otherFlightAltitude) {

        double verticalDistance = Math.abs(flightAltitude - otherFlightAltitude);
        return verticalDistance;

    }

    public static double[] calculateRateOfClosure(
            FlightTimeLocation flightInfo, FlightTimeLocation otherInfo,
            int startLine, int endLine,
            int otherStartLine, int otherEndLine) {

        int shift = 5;
        int newStart1 = (startLine - shift) >= 0 ? (startLine - shift) : 0;
        int newStart2 = (otherStartLine - shift) >= 0 ? (otherStartLine - shift) : 0;
        int startShift1 = startLine - newStart1;
        int startShift2 = otherStartLine - newStart2;
        int startShift = Math.min(startShift1, startShift2);

        LOG.info("original start shift: " + shift + ", new start shift: " + startShift);

        newStart1 = startLine - startShift;
        newStart2 = otherStartLine - startShift;

        LOG.info("start line: " + startLine + ", otherStartLine: " + otherStartLine);
        LOG.info("shifted start line: " + newStart1 + ", otherStartLine: " + newStart2);

        int newEnd1 = (endLine + shift) <= flightInfo.epochTime.length ? (endLine + shift) : flightInfo.epochTime.length;
        int newEnd2 = (otherEndLine + shift) <= otherInfo.epochTime.length ? (otherEndLine + shift)
                : otherInfo.epochTime.length;
        int endShift1 = newEnd1 - endLine;
        int endShift2 = newEnd2 - otherEndLine;
        int endShift = Math.min(endShift1, endShift2);

        LOG.info("original end shift: " + shift + ", new end shift: " + endShift);

        newEnd1 = endLine + endShift;
        newEnd2 = otherEndLine + endShift;

        LOG.info("end line: " + endLine + ", otherEndLine: " + otherEndLine);
        LOG.info("shifted end line: " + newEnd1 + ", otherEndLine: " + newEnd2);

        startLine = newStart1;
        otherStartLine = newStart2;
        endLine = newEnd1;
        otherEndLine = newEnd2;

        double previousDistance = calculateDistance(
                flightInfo.latitude[startLine], flightInfo.longitude[startLine], flightInfo.altitudeMSL[startLine],
                otherInfo.latitude[otherStartLine], otherInfo.longitude[otherStartLine], otherInfo.altitudeMSL[otherStartLine]);

        ArrayList<Double> rateOfClosure = new ArrayList<Double>();
        int i = (startLine + 1), j = (otherStartLine + 1);
        while (i < endLine && j < otherEndLine) {

            /*
             * LOG.info(
             * "flight1.epochTime[" + i + "]: " + flightInfo.epochTime[i] +
             * ", flight2.epochTime[" + j + "]: " + otherInfo.epochTime[j] +
             * ", previousDistance: " + previousDistance
             * );
             */

            if (flightInfo.epochTime[i] == 0) {
                i++;
                continue;
            }
            if (otherInfo.epochTime[j] == 0) {
                j++;
                continue;
            }

            // Ensure both iterators are for the same time
            if (flightInfo.epochTime[i] < otherInfo.epochTime[j]) {
                i++;
                continue;
            }
            if (otherInfo.epochTime[j] < flightInfo.epochTime[i]) {
                j++;
                continue;
            }

            double currentDistance = calculateDistance(
                    flightInfo.latitude[i], flightInfo.longitude[i], flightInfo.altitudeMSL[i],
                    otherInfo.latitude[j], otherInfo.longitude[j], otherInfo.altitudeMSL[j]);

            rateOfClosure.add(previousDistance - currentDistance);
            previousDistance = currentDistance;
            i++;
            j++;

        }

        // Convert the ArrayList to a primitive array
        double[] roc = new double[rateOfClosure.size()];

        LOG.info("rate of closure, length:" + roc.length);
        for (int k = 0; k < roc.length; k++) {
            roc[k] = rateOfClosure.get(k);
            LOG.info("\t" + roc[k]);
        }

        // Leave in to verify how things work in these edge cases
        if (startShift < 5 || endShift < 5)
            System.exit(1);

        return roc;

    }

    public static boolean addProximityIfNotInList(ArrayList<Event> eventList, Event testEvent) {

        for (Event event : eventList) {

            boolean hasSameFlightIDs = (event.getFlightId() == testEvent.getFlightId()
                    && event.getOtherFlightId() == testEvent.getOtherFlightId());
            boolean hasSameTimestamps = (event.getStartTime().equals(testEvent.getStartTime())
                    && event.getEndTime().equals(testEvent.getEndTime()));

            // Event already in the list, don't add it again
            if (hasSameFlightIDs && hasSameTimestamps)
                return false;

        }

        // Event not in the list, add it
        eventList.add(testEvent);
        return true;

    }

    public static void processFlightWithError(Connection connection, int fleetId, int flightId) throws SQLException {

        final String query = "INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 1";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, fleetId);
            stmt.setInt(2, flightId);
            stmt.setInt(3, ADJACENCY_EVENT_DEFINITION_ID);
            // LOG.info(stmt.toString());
            stmt.executeUpdate();

        }

    }

    @SuppressWarnings("unused")
    public static void processFlight(Connection connection, Flight flight, UploadProcessedEmail uploadProcessedEmail) {

        LOG.info("Processing flight: " + flight.getId() + ", " + flight.getFilename());
        int fleetId = flight.getFleetId();
        int flightId = flight.getId();
        int airframeNameId = flight.getAirframeNameId();
        String flightFilename = flight.getFilename();

        try {

            // Get enough information about the flight to determine if we can calculate adjacencies with it
            FlightTimeLocation flightInfo = new FlightTimeLocation(connection, flight.getFleetId(), flightId,
                    flight.getAirframeNameId(), flight.getStartDateTime(), flight.getEndDateTime());

            // Flight is invalid, process error
            if (!flightInfo.isValid()) {
                uploadProcessedEmail.addProximityError(flightFilename, "could not calculate proximity for flight " + flightId
                        + ", '" + flightFilename
                        + "' - was missing required data columns (date, time, latitude, longitude, altitude and/or indicated airspeed)");
                processFlightWithError(connection, fleetId, flightId);
                return;
            }

            ArrayList<Flight> potentialFlights = Flight.getFlights(connection,
                    "(id != " + flightId + " AND start_timestamp <= UNIX_TIMESTAMP('" + flightInfo.endDateTime
                            + "') AND end_timestamp >= UNIX_TIMESTAMP('" + flightInfo.startDateTime + "'))");

            LOG.info("Found " + potentialFlights.size() + " potential time matched flights.");
            // LOG.info("Flight start time: " + flightInfo.startDateTime + ", end time: " + flightInfo.endDateTime);
            // LOG.info("Flight latitude min: " + flightInfo.minLatitude + ", max: " + flightInfo.maxLatitude);
            // LOG.info("Flight longitude min: " + flightInfo.minLongitude + ", max: " + flightInfo.maxLongitude);

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

            for (Flight otherFlight : potentialFlights) {

                /*
                 * LOG.info("\tmatched to flight with start time: " + otherFlight.getStartDateTime() + ", end time: " +
                 * otherFlight.getEndDateTime());
                 */

                timeMatchFlights++;

                FlightTimeLocation otherInfo = new FlightTimeLocation(connection, otherFlight.getFleetId(), otherFlight.getId(),
                        otherFlight.getAirframeNameId(), otherFlight.getStartDateTime(), otherFlight.getEndDateTime());

                // Matched flight did not have all the information necessary to compute adjacency, skip
                if (!otherInfo.isValid())
                    continue;

                // Proximity between these two flights was already calculated, skip
                if (FlightTimeLocation.proximityAlreadyCalculated(connection, otherInfo, flightInfo)) {
                    LOG.info("Not re-performing proximity calculation");
                    continue;
                }

                /*
                 * LOG.info("\t\tother latitude min: " + otherInfo.minLatitude + ", max: " + otherInfo.maxLatitude);
                 * LOG.info("\t\tother longitude min: " + otherInfo.minLongitude + ", max: " + otherInfo.maxLongitude);
                 */

                if (flightInfo.hasRegionOverlap(otherInfo)) {

                    /*
                     * LOG.info("\t\tLatitude/Longitude overlap!");
                     */

                    locMatchFlights++;

                    if (!flightInfo.hasSeriesData()) {

                        if (!flightInfo.getSeriesData(connection)) {

                            // Could not get the required time series data columns
                            processFlightWithError(connection, fleetId, flightId);
                            return;

                        }

                    }

                    // Other flight didn't have the necesary time series data columns, skip
                    if (!otherInfo.getSeriesData(connection))
                        continue;

                    // Skip the first 30 seconds as it is usually the FDR being initialized
                    final int SKIP_SECONDS = 30;
                    int i = SKIP_SECONDS, j = SKIP_SECONDS;

                    while (i < flightInfo.epochTime.length && j < otherInfo.epochTime.length) {

                        // Skip entries where the epoch time was 0 (the date/time was null)
                        if (flightInfo.epochTime[i] == 0) {
                            i++;
                            continue;
                        }
                        if (otherInfo.epochTime[j] == 0) {
                            j++;
                            continue;
                        }

                        // Ensure both iterators are for the same time
                        if (flightInfo.epochTime[i] < otherInfo.epochTime[j]) {
                            i++;
                            continue;
                        }
                        if (otherInfo.epochTime[j] < flightInfo.epochTime[i]) {
                            j++;
                            continue;
                        }

                        double distanceFt = calculateDistance(
                                flightInfo.latitude[i], flightInfo.longitude[i], flightInfo.altitudeMSL[i],
                                otherInfo.latitude[j], otherInfo.longitude[j], otherInfo.altitudeMSL[j]);
                        double lateralDistanceFt = calculateLateralDistance(
                                flightInfo.latitude[i], flightInfo.longitude[i],
                                otherInfo.latitude[j], otherInfo.longitude[j]);
                        double verticalDistanceFt = calculateVerticalDistance(
                                flightInfo.altitudeMSL[i], otherInfo.altitudeMSL[j]);

                        final double MAX_DISTANCE_FT = 1000.0;
                        final double MIN_ALTITUDE_AGL = 50.0;
                        final double MIN_AIRSPEED = 20.0;

                        boolean distanceCheck = (distanceFt < MAX_DISTANCE_FT);
                        boolean altitudeCheck = (flightInfo.altitudeAGL[i] >= MIN_ALTITUDE_AGL)
                                && (otherInfo.altitudeAGL[j] >= MIN_ALTITUDE_AGL);
                        boolean airspeedCheck = (flightInfo.indicatedAirspeed[i] > MIN_AIRSPEED)
                                && (otherInfo.indicatedAirspeed[j] > MIN_AIRSPEED);

                        if (distanceCheck && altitudeCheck && airspeedCheck) {

                            /*
                             * LOG.info(
                             * "\t\t\tother time[" + j + "]: " + otherInfo.epochTime[j] + " == flight time[" + i + "]: " +
                             * flightInfo.epochTime[i]
                             * + ", flight lat/lon: " + flightInfo.latitude[i] + " " + flightInfo.longitude[i] + ", other lat/lon: " +
                             * otherInfo.latitude[j] + " " + otherInfo.longitude[j]
                             * + " -- distance: " + distanceFt
                             * );
                             *
                             * LOG.info(
                             * "\t\t\t\t\tflight alt AGL: " + flightInfo.altitudeAGL[i]
                             * + ", other alt AGL: " + otherInfo.altitudeAGL[j]
                             * + ", final distance: " + distanceFt
                             * );
                             */

                            // If an exceedence is not being tracked, startTime is null
                            if (startTime == null) {

                                // Start tracking a new exceedence
                                startTime = flightInfo.dateSeries.get(i) + " " + flightInfo.timeSeries.get(i);
                                otherStartTime = otherInfo.dateSeries.get(j) + " " + otherInfo.timeSeries.get(j);

                                startLine = i;
                                otherStartLine = j;
                                severity = distanceFt;
                                lateralDistance = lateralDistanceFt;
                                verticalDistance = verticalDistanceFt;

                                /*
                                 * LOG.info("start date time: " + startTime + ", start line number: " + startLine);
                                 */

                            }

                            endLine = i;
                            otherEndLine = j;
                            endTime = flightInfo.dateSeries.get(i) + " " + flightInfo.timeSeries.get(i);
                            otherEndTime = otherInfo.dateSeries.get(j) + " " + otherInfo.timeSeries.get(j);

                            // New closest proximity this time, update the severity
                            if (distanceFt < severity) {

                                LOG.info("CalculateProximity.java -- New smallest distance: " + distanceFt + " < " + severity
                                        + " at time " + endTime + " and other time " + otherEndTime + " with lateral distance: "
                                        + lateralDistanceFt + " and vertical distance: " + verticalDistanceFt);
                                severity = distanceFt;

                                // Record Lateral & Vertical Distances when the new smallest Euclidean distance is found
                                lateralDistance = lateralDistanceFt;
                                verticalDistance = verticalDistanceFt;

                            }

                            // Increment the startCount, reset the stopCount
                            startCount++;
                            stopCount = 0;

                        } else {

                            // This time didn't trigger proximity...
                            if (startTime != null) {

                                // Already tracking a proximity event, increment the stop count
                                stopCount++;

                                if (stopCount == stopBuffer) {

                                    /*
                                     * System.err.println("Stop count (" + stopCount + ") reached the stop buffer (" + stopBuffer +
                                     * "), new event created!");
                                     */

                                    // Not enough triggers to reach the start count, don't create the event (i.e. do nothing)
                                    if (startCount < startBuffer) {

                                        LOG.info("CalculateProximity.java -- Not enough triggers, will not create Proximity event: "
                                                + endTime + " - " + startTime + " = "
                                                + (Duration.between(Instant.parse(startTime), Instant.parse(endTime)).toMillis() / 1000.0)
                                                + " seconds");

                                    } else {

                                        // Had enough triggers to reach the start count, create the event
                                        LOG.info("(A) Creating events for flights with IDs : " + flightId + " and " + otherFlight.getId());

                                        Event event = new Event(
                                                startTime, endTime,
                                                startLine, endLine,
                                                severity,
                                                flightId, otherFlight.getId());
                                        Event otherEvent = new Event(
                                                otherStartTime, otherEndTime,
                                                otherStartLine, otherEndLine,
                                                severity,
                                                otherFlight.getId(), flightId);

                                        EventMetaData lateralDistanceMetaData = new EventMetaData("lateral_distance", lateralDistance);
                                        EventMetaData verticalDistanceMetaData = new EventMetaData("vertical_distance", verticalDistance);

                                        event.addMetaData(lateralDistanceMetaData);
                                        event.addMetaData(verticalDistanceMetaData);
                                        otherEvent.addMetaData(lateralDistanceMetaData);
                                        otherEvent.addMetaData(verticalDistanceMetaData);

                                        if (severity > 0) {
                                            double[] rateOfClosureArray = calculateRateOfClosure(flightInfo, otherInfo, startLine, endLine,
                                                    otherStartLine, otherEndLine);
                                            RateOfClosure rateOfClosure = new RateOfClosure(rateOfClosureArray);
                                            event.setRateOfClosure(rateOfClosure);
                                            otherEvent.setRateOfClosure(rateOfClosure);
                                        }

                                        addProximityIfNotInList(eventList, event);
                                        addProximityIfNotInList(eventList, otherEvent);

                                    }

                                    // Reset the event values
                                    startTime = null;
                                    otherStartTime = null;
                                    endTime = null;
                                    otherEndTime = null;
                                    startLine = -1;
                                    otherEndLine = -1;
                                    endLine = -1;
                                    otherEndLine = -1;

                                    // Reset the start and stop counts
                                    startCount = 0;
                                    stopCount = 0;

                                }

                            }

                        }

                        // Iterate both as they had matching times
                        i++;
                        j++;

                    }

                    /*
                     * LOG.info("\t\tseries matched time on " + totalMatches + " rows");
                     */

                    // An event was still going when one flight ended, create it and add it to the list...
                    if (startTime != null) {

                        LOG.info("(B) Creating events for flights with IDs : " + flightId + " and " + otherFlight.getId());

                        Event event = new Event(
                                startTime, endTime,
                                startLine, endLine,
                                severity,
                                flightId, otherFlight.getId());
                        Event otherEvent = new Event(
                                otherStartTime, otherEndTime,
                                otherStartLine, otherEndLine,
                                severity,
                                otherFlight.getId(), flightId);

                        EventMetaData lateralDistanceMetaData = new EventMetaData("lateral_distance", lateralDistance);
                        EventMetaData verticalDistanceMetaData = new EventMetaData("vertical_distance", verticalDistance);

                        event.addMetaData(lateralDistanceMetaData);
                        event.addMetaData(verticalDistanceMetaData);
                        otherEvent.addMetaData(lateralDistanceMetaData);
                        otherEvent.addMetaData(verticalDistanceMetaData);

                        if (severity > 0) {

                            double[] rateOfClosureArray = calculateRateOfClosure(flightInfo, otherInfo, startLine, endLine,
                                    otherStartLine, otherEndLine);
                            RateOfClosure rateOfClosure = new RateOfClosure(rateOfClosureArray);
                            event.setRateOfClosure(rateOfClosure);
                            otherEvent.setRateOfClosure(rateOfClosure);

                        }

                        addProximityIfNotInList(eventList, event);
                        addProximityIfNotInList(eventList, otherEvent);

                    }
                }
                // END: Loop processing a *particular* flight
            }
            // END: Loop processing *all* flights

            for (Event event : eventList) {
                LOG.info("\t" + event.toString());
                eventsFound++;
                uploadProcessedEmail.addProximity(flightFilename,
                        "flight " + flightId + ", '" + flightFilename + "' - had a proximity event with flight "
                                + event.getOtherFlightId() + " from " + event.getStartTime() + " to " + event.getEndTime());
            }

            LOG.info("\n");

            // Step 2: export the events and their statistics in the database
            double sumDuration = 0.0;
            double sumSeverity = 0.0;
            double minSeverity = Double.MAX_VALUE;
            double maxSeverity = -Double.MAX_VALUE;
            double minDuration = Double.MAX_VALUE;
            double maxDuration = -Double.MAX_VALUE;
            for (Event event : eventList) {
                int eventFlightId = event.getFlightId();
                event.updateDatabase(connection, fleetId, eventFlightId, ADJACENCY_EVENT_DEFINITION_ID);

                // Fetch / Create EventStats for this flight
                EventStats stats = flightEventStats.get(eventFlightId);
                if (stats == null) {
                    stats = new EventStats();
                    flightEventStats.put(eventFlightId, stats);
                }
                stats.eventCount++;

                double currentSeverity = event.getSeverity();
                double currentDuration = event.getDuration();
                sumDuration += currentDuration;
                sumSeverity += currentSeverity;

                if (currentSeverity > maxSeverity)
                    maxSeverity = currentSeverity;
                if (currentSeverity < minSeverity)
                    minSeverity = currentSeverity;
                if (currentDuration > maxDuration)
                    maxDuration = currentDuration;
                if (currentDuration < minDuration)
                    minDuration = currentDuration;

                // Update the EventStats for this flight
                if (event.getStartTime() != null) {
                    EventStatistics.updateEventStatistics(connection, fleetId, airframeNameId, ADJACENCY_EVENT_DEFINITION_ID,
                            event.getStartTime(), event.getSeverity(), event.getDuration());
                } else if (event.getEndTime() != null) {
                    EventStatistics.updateEventStatistics(connection, fleetId, airframeNameId, ADJACENCY_EVENT_DEFINITION_ID,
                            event.getEndTime(), event.getSeverity(), event.getDuration());
                } else {
                    LOG.warning("WARNING: could not update event statistics for event: " + event);
                    LOG.warning("WARNING: event start and end time were both null.");
                }

            }

            // Convert Map to set to remove duplicates
            for (Map.Entry<Integer, EventStats> entry : flightEventStats.entrySet()) {

                int eventFlightId = entry.getKey();
                EventStats stats = entry.getValue();

                if (stats.entryAlreadyExists)
                    continue;

                /*
                 * LOG.info("CalculateProximity.java -- Updating prox. stats for flight ID: " + eventFlightId
                 * +" with event count: " + stats.eventCount);
                 */

                if (stats.eventCount > 0) {

                    final String query = "INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = ?, sum_duration = ?, min_duration = ?, max_duration = ?, sum_severity = ?, min_severity = ?, max_severity = ?, had_error = 0";
                    try (PreparedStatement stmt = connection.prepareStatement(query)) {
                        stmt.setInt(1, fleetId);
                        stmt.setInt(2, eventFlightId);
                        stmt.setInt(3, ADJACENCY_EVENT_DEFINITION_ID);
                        stmt.setInt(4, stats.eventCount);
                        stmt.setDouble(5, stats.sumDuration);
                        stmt.setDouble(6, stats.minDuration);
                        stmt.setDouble(7, stats.maxDuration);
                        stmt.setDouble(8, stats.sumSeverity);
                        stmt.setDouble(9, stats.minSeverity);
                        stmt.setDouble(10, stats.maxSeverity);
                        stmt.executeUpdate();
                    }

                    EventStatistics.updateFlightsWithEvent(connection, fleetId, airframeNameId, ADJACENCY_EVENT_DEFINITION_ID,
                            flight.getStartDateTime());

                    stats.entryAlreadyExists = true;

                } else {

                    final String query = "INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 0";
                    try (PreparedStatement stmt = connection.prepareStatement(query)) {
                        stmt.setInt(1, fleetId);
                        stmt.setInt(2, eventFlightId);
                        stmt.setInt(3, ADJACENCY_EVENT_DEFINITION_ID);
                        stmt.executeUpdate();
                    }

                    EventStatistics.updateFlightsWithoutEvent(connection, fleetId, airframeNameId, ADJACENCY_EVENT_DEFINITION_ID,
                            flight.getStartDateTime());

                    stats.entryAlreadyExists = true;

                }

            }

        } catch (SQLException | IOException e) {

            LOG.severe("CalculateProximity.java -- SQLException: " + e);
            LOG.severe("CalculateProximity.java -- SQLException message: " + e.getMessage());
            System.exit(1);

        }

    }

    public static void calculateProximity(Connection connection, int uploadId, UploadProcessedEmail uploadProcessedEmail)
            throws SQLException {

        Instant start = Instant.now();

        LOG.info("Commencing proximity calculation for upload ID: " + uploadId);

        ArrayList<Flight> flights = Flight.getFlights(connection,
                "upload_id = " + uploadId
                        + " AND NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = "
                        + ADJACENCY_EVENT_DEFINITION_ID + " AND flight_processed.flight_id = flights.id)");

        int count = 0;
        for (int j = 0; j < flights.size(); j++) {

            // Flight is currently being inserted to the database by ProcessFlights, skip
            if (!flights.get(j).insertCompleted())
                continue;

            processFlight(connection, flights.get(j), uploadProcessedEmail);
            count++;

        }

        Instant end = Instant.now();
        double elapsed_seconds = (double) Duration.between(start, end).toMillis() / 1000.0;
        double average_seconds = (elapsed_seconds / (double) count);
        double avgTimeMatchedFlights = ((double) timeMatchFlights / (double) count);
        double avgLocationMatchedFlights = ((double) locMatchFlights / (double) count);

        LOG.info("Calculated " + count + " Proximity evaluations in " + elapsed_seconds + " seconds, averaged: "
                + average_seconds + " seconds per flight");
        LOG.info("Average time-matched flights: " + avgTimeMatchedFlights + ", Average location-matched flights: "
                + avgLocationMatchedFlights);
        LOG.info("Proximity events found:" + eventsFound);

        uploadProcessedEmail.setProximityElapsedTime(elapsed_seconds, average_seconds, avgTimeMatchedFlights,
                avgLocationMatchedFlights);

        LOG.info("Finished proximity calculation for upload ID: " + uploadId);

    }

    public static void main(String[] arguments) {
        final int flightsPerQuery = 5000;
        while (true) {
            try (Connection connection = Database.getConnection()) {

                Instant start = Instant.now();

                ArrayList<Flight> flights = Flight.getFlights(connection,
                        "NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = "
                                + ADJACENCY_EVENT_DEFINITION_ID + " AND flight_processed.flight_id = flights.id)",
                        flightsPerQuery);

                int count = 0;
                for (int j = 0; j < flights.size(); j++) {

                    // Flight is currently being inserted to the database by ProcessFlights, skip
                    if (!flights.get(j).insertCompleted())
                        continue;

                    processFlight(connection, flights.get(j), null);
                    count++;

                }

                Instant end = Instant.now();
                double elapsed_seconds = (double) Duration.between(start, end).toMillis() / 1000.0;
                double average_seconds = ((double) elapsed_seconds) / (double) count;

                LOG.info("Calculated " + count + " adjacency evaluations in " + elapsed_seconds + " seconds, averaged: "
                        + average_seconds + " seconds per flight");
                LOG.info("Average time-matched flights: " + ((double) timeMatchFlights / (double) count)
                        + ", Average location-matched flights: " + ((double) locMatchFlights / (double) count));
                LOG.info("Events found:" + eventsFound);

                /*
                 * System.exit(1);
                 */

                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    LOG.severe("CalculateProximity.java -- Exception: " + e);
                    LOG.severe("CalculateProximity.java -- Exception message: " + e.getMessage());
                }

                /*
                 * connection.close();
                 */

            } catch (SQLException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

}
