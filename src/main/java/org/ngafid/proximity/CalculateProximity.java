package org.ngafid.proximity;

import org.ngafid.Database;
import org.ngafid.UploadProcessedEmail;
import org.ngafid.events.Event;
import org.ngafid.events.RateOfClosure;
import org.ngafid.flights.Flight;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import org.ngafid.events.EventMetaData;
import org.ngafid.events.EventStatistics;
import org.ngafid.airports.Airports;

import java.util.List;
import java.util.logging.*;

public final class CalculateProximity {
    private static final Logger LOG = Logger.getLogger(CalculateProximity.class.getName());

    private CalculateProximity() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    // Proximity events (and potentially other complicated event calculations) will
    // have negative IDs so they
    // can be excluded from the regular event calculation process
    public static final int ADJACENCY_EVENT_DEFINITION_ID = -1;

    // use this to get a representation of a flight's current time, position and
    // altitude

    private static long timeMatchFlights = 0;
    private static long locMatchFlights = 0;
    private static long eventsFound = 0;

    public static double calculateDistance(double flightLatitude, double flightLongitude, double otherFlightLatitude,
            double otherFlightLongitude, double flightAltitude, double otherFlightAltitude) {

        double distanceFt = Airports.calculateDistanceInFeet(flightLatitude, flightLongitude, otherFlightLatitude,
                otherFlightLongitude);
        double altDiff = Math.abs(flightAltitude - otherFlightAltitude);
        distanceFt = Math.sqrt((distanceFt * distanceFt) + (altDiff * altDiff));
        return distanceFt;
    }

    public static double calculateLateralDistance(double flightLatitude, double flightLongitude,
            double otherFlightLatitude,
            double otherFlightLongitude) {

        return Airports.calculateDistanceInFeet(flightLatitude, flightLongitude, otherFlightLatitude,
                otherFlightLongitude);
    }

    public static double calculateVerticalDistance(double flightAltitude, double otherFlightAltitude) {

        return Math.abs(flightAltitude - otherFlightAltitude);
    }

    public static double[] calculateRateOfClosure(FlightTimeLocation flightInfo, FlightTimeLocation otherInfo,
            int startLine,
            int endLine, int otherStartLine, int otherEndLine) {

        int shift = 5;

        int newStart1 = Math.max((startLine - shift), 0);
        int newStart2 = Math.max((otherStartLine - shift), 0);

        int startShift1 = startLine - newStart1;
        int startShift2 = otherStartLine - newStart2;

        int startShift = Math.min(startShift1, startShift2);

        System.out.println("original start shift: " + shift + ", new start shift: " + startShift);

        newStart1 = startLine - startShift;
        newStart2 = otherStartLine - startShift;

        System.out.println("start line: " + startLine + ", otherStartLine: " + otherStartLine);
        System.out.println("shifted start line: " + newStart1 + ", otherStartLine: " + newStart2);

        int newEnd1 = Math.min((endLine + shift), flightInfo.epochTime.length);
        int newEnd2 = Math.min((otherEndLine + shift), otherInfo.epochTime.length);

        int endShift1 = newEnd1 - endLine;
        int endShift2 = newEnd2 - otherEndLine;

        int endShift = Math.min(endShift1, endShift2);

        System.out.println("original end shift: " + shift + ", new end shift: " + endShift);

        newEnd1 = endLine + endShift;
        newEnd2 = otherEndLine + endShift;

        System.out.println("end line: " + endLine + ", otherEndLine: " + otherEndLine);
        System.out.println("shifted end line: " + newEnd1 + ", otherEndLine: " + newEnd2);

        startLine = newStart1;
        otherStartLine = newStart2;
        endLine = newEnd1;
        otherEndLine = newEnd2;

        double previousDistance = calculateDistance(flightInfo.latitude[startLine], flightInfo.longitude[startLine],
                otherInfo.latitude[otherStartLine], otherInfo.longitude[otherStartLine],
                flightInfo.altitudeMSL[startLine], otherInfo.altitudeMSL[otherStartLine]);

        List<Double> rateOfClosure = new ArrayList<>();
        int i = startLine + 1;
        int j = otherStartLine + 1;

        while (i < endLine && j < otherEndLine) {
            // System.out.println("flight1.epochTime[" + i + "]: " + flightInfo.epochTime[i]
            // + ", flight2.epochTime[" + j + "]: " + otherInfo.epochTime[j] + ",
            // previousDistance: " + previousDistance);
            if (flightInfo.epochTime[i] == 0) {
                i++;
                continue;
            }
            if (otherInfo.epochTime[j] == 0) {
                j++;
                continue;
            }
            // make sure both iterators are for the same time
            if (flightInfo.epochTime[i] < otherInfo.epochTime[j]) {
                i++;
                continue;
            }
            if (otherInfo.epochTime[j] < flightInfo.epochTime[i]) {
                j++;
                continue;
            }
            double currentDistance = calculateDistance(flightInfo.latitude[i], flightInfo.longitude[i],
                    otherInfo.latitude[j], otherInfo.longitude[j], flightInfo.altitudeMSL[i], otherInfo.altitudeMSL[j]);

            rateOfClosure.add(previousDistance - currentDistance);
            previousDistance = currentDistance;
            i++;
            j++;
        }

        // convert the arraylist to a primitive array
        double[] roc = new double[rateOfClosure.size()];

        System.out.println("rate of closure, length:" + roc.length);
        for (int k = 0; k < roc.length; k++) {
            roc[k] = rateOfClosure.get(k);
            System.out.println("\t" + roc[k]);
        }

        // leave in to verify how things work in these edge cases
        if (startShift < 5 || endShift < 5) {
            System.exit(1);
        }

        return roc;
    }

    public static void addProximityIfNotInList(List<Event> eventList, Event testEvent) {
        for (Event event : eventList) {
            if (event.getFlightId() == testEvent.getFlightId()
                    && event.getOtherFlightId() == testEvent.getOtherFlightId()) {
                return;
            }
        }
        eventList.add(testEvent);

    }

    public static void processFlightWithError(Connection connection, int fleetId, int flightId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?," +
                        " count = 0, had_error = 1")) {
            stmt.setInt(1, fleetId);
            stmt.setInt(2, flightId);
            stmt.setInt(3, ADJACENCY_EVENT_DEFINITION_ID);
            // LOG.info(stmt.toString());
            stmt.executeUpdate();
        }
    }

    private static void exportEventsAndStatistics(Connection connection, List<Event> eventList, Flight flight)
            throws SQLException, IOException {
        int fleetId = flight.getFleetId();
        int flightId = flight.getId();
        int airframeNameId = flight.getAirframeNameId();

        double sumDuration = 0.0;
        double sumSeverity = 0.0;
        double minSeverity = Double.MAX_VALUE;
        double maxSeverity = -Double.MAX_VALUE;
        double minDuration = Double.MAX_VALUE;
        double maxDuration = -Double.MAX_VALUE;
        for (Event event : eventList) {
            event.updateDatabase(connection, fleetId, flightId, ADJACENCY_EVENT_DEFINITION_ID);
            if (event.getStartTime() != null) {
                EventStatistics.updateEventStatistics(connection, fleetId, airframeNameId,
                        ADJACENCY_EVENT_DEFINITION_ID, event.getStartTime(), event.getSeverity(), event.getDuration());
            } else if (event.getEndTime() != null) {
                EventStatistics.updateEventStatistics(connection, fleetId, airframeNameId,
                        ADJACENCY_EVENT_DEFINITION_ID, event.getEndTime(), event.getSeverity(), event.getDuration());
            } else {
                LOG.info("WARNING: could not update event statistics for event: " + event);
                LOG.info("WARNING: event start and end time were both null.");
            }

            double currentSeverity = event.getSeverity();
            double currentDuration = event.getDuration();
            sumDuration += currentDuration;
            sumSeverity += currentSeverity;

            if (currentSeverity > maxSeverity) {
                maxSeverity = currentSeverity;
            }

            if (currentSeverity < minSeverity) {
                minSeverity = currentSeverity;
            }

            if (currentDuration > maxDuration) {
                maxDuration = currentDuration;
            }

            if (currentDuration < minDuration) {
                minDuration = currentDuration;
            }
        }

        if (!eventList.isEmpty()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, " +
                            "count = ?, sum_duration = ?, min_duration = ?, max_duration = ?, sum_severity = ?, " +
                            "min_severity = ?, max_severity = ?, had_error = 0")) {
                stmt.setInt(1, fleetId);
                stmt.setInt(2, flightId);
                stmt.setInt(3, ADJACENCY_EVENT_DEFINITION_ID);
                stmt.setInt(4, eventList.size());
                stmt.setDouble(5, sumDuration);
                stmt.setDouble(6, minDuration);
                stmt.setDouble(7, maxDuration);
                stmt.setDouble(8, sumSeverity);
                stmt.setDouble(9, minSeverity);
                stmt.setDouble(10, maxSeverity);
                // LOG.info(stmt.toString());
                stmt.executeUpdate();
            }

            EventStatistics.updateFlightsWithEvent(connection, fleetId, airframeNameId, ADJACENCY_EVENT_DEFINITION_ID,
                    flight.getStartDateTime());

        } else {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, " +
                            "count = 0, had_error = 0")) {
                stmt.setInt(1, fleetId);
                stmt.setInt(2, flightId);
                stmt.setInt(3, ADJACENCY_EVENT_DEFINITION_ID);
                // LOG.info(stmt.toString());
                stmt.executeUpdate();
            }

            EventStatistics.updateFlightsWithoutEvent(connection, fleetId, airframeNameId,
                    ADJACENCY_EVENT_DEFINITION_ID, flight.getStartDateTime());
        }
    }

    private static FlightTimeLocation canAdjacencyBeCalculated(Connection connection, Flight flight,
                                                    UploadProcessedEmail uploadProcessedEmail) throws SQLException {
        int fleetId = flight.getFleetId();
        int flightId = flight.getId();
        String flightFilename = flight.getFilename();
        FlightTimeLocation flightInfo = new FlightTimeLocation(connection, fleetId, flightId,
                flight.getAirframeNameId(), flight.getStartDateTime(), flight.getEndDateTime());

        if (!flightInfo.isValid()) {
            uploadProcessedEmail.addProximityError(flight.getFilename(), "could not calculate proximity for flight "
                    + flightId + ", '" + flightFilename + "' - was missing required data columns " +
                    "(date, time, latitude, longitude, altitude and/or indicated airspeed)");
            processFlightWithError(connection, fleetId, flightId);
            return null;
        }

        return flightInfo;
    }

    public static void processFlight(Connection connection, Flight flight, UploadProcessedEmail uploadProcessedEmail)
            throws IOException, SQLException {
        LOG.info("Processing flight: " + flight.getId() + ", " + flight.getFilename());

        int fleetId = flight.getFleetId();
        int flightId = flight.getId();
        int airframeNameId = flight.getAirframeNameId();
        String flightFilename = flight.getFilename();

        // get enough information about the flight to determine if we can calculate adjacency with it
        FlightTimeLocation flightInfo = canAdjacencyBeCalculated(connection, flight, uploadProcessedEmail);
        if (flightInfo == null) {
            return;
        }

        ArrayList<Flight> potentialFlights = Flight.getFlights(connection,
                "(id != " + flightId + " AND start_timestamp <= UNIX_TIMESTAMP('" + flightInfo.endDateTime
                        + "') AND end_timestamp >= UNIX_TIMESTAMP('" + flightInfo.startDateTime + "'))");

        LOG.info("Found " + potentialFlights.size() + " potential time matched flights.");

        List<Event> eventList = new ArrayList<>();
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

        // TODO: should probably grab these from the database event definition instead of hard coding them;
        //  but we don't need to pull the event definition so this is a tad bit faster.
        int startBuffer = 1;
        int stopBuffer = 30;

        for (Flight otherFlight : potentialFlights) {
            timeMatchFlights++;

            FlightTimeLocation otherInfo = new FlightTimeLocation(connection, otherFlight.getFleetId(),
                    otherFlight.getId(), otherFlight.getAirframeNameId(), otherFlight.getStartDateTime(),
                    otherFlight.getEndDateTime());
            if (!otherInfo.isValid()) {
                // matched flight did not have all the information necessary to compute adjacency
                continue;
            }

            // see if proximity between these two flights was already calculated, if so we can skip
            if (FlightTimeLocation.proximityAlreadyCalculated(connection, otherInfo, flightInfo)) {
                LOG.info("Not re-performing proximity calculation");
                continue;
            }

            if (flightInfo.hasRegionOverlap(otherInfo)) {
                locMatchFlights++;

                if (!flightInfo.hasSeriesData()) {
                    if (!flightInfo.getSeriesData(connection)) {
                        // could not get the required time series data columns
                        processFlightWithError(connection, fleetId, flightId);
                        return;
                    }
                }

                if (!otherInfo.getSeriesData(connection)) {
                    // the other flight didn't have the necesary time series data columns
                    continue;
                }

                // skip the first 30 seconds as it is usually the FDR being initialized
                int i = 30;
                int j = 30;
                while (i < flightInfo.epochTime.length && j < otherInfo.epochTime.length) {
                    // skip entries where the epoch time was 0 (the date/time was null)
                    if (flightInfo.epochTime[i] == 0) {
                        i++;
                        continue;
                    }

                    if (otherInfo.epochTime[j] == 0) {
                        j++;
                        continue;
                    }

                    // make sure both iterators are for the same time
                    if (flightInfo.epochTime[i] < otherInfo.epochTime[j]) {
                        i++;
                        continue;
                    }

                    if (otherInfo.epochTime[j] < flightInfo.epochTime[i]) {
                        j++;
                        continue;
                    }

                    double distanceFt = calculateDistance(flightInfo.latitude[i], flightInfo.longitude[i],
                            otherInfo.latitude[j],
                            otherInfo.longitude[j], flightInfo.altitudeMSL[i], otherInfo.altitudeMSL[j]);
                    double lateralDistanceFt = calculateLateralDistance(flightInfo.latitude[i],
                            flightInfo.longitude[i], otherInfo.latitude[j],
                            otherInfo.longitude[j]);
                    double verticalDistanceFt = calculateVerticalDistance(flightInfo.altitudeMSL[i],
                            otherInfo.altitudeMSL[j]);

                    if (distanceFt < 1000.0 && flightInfo.altitudeAGL[i] >= 50 && otherInfo.altitudeAGL[j] >= 50
                            && flightInfo.indicatedAirspeed[i] > 20 && otherInfo.indicatedAirspeed[j] > 20) {

                        // startTime is null if an exceedence is not being tracked
                        if (startTime == null) { // start tracking a new exceedence
                            startTime = flightInfo.dateSeries.get(i) + " " + flightInfo.timeSeries.get(i);
                            otherStartTime = otherInfo.dateSeries.get(j) + " " + otherInfo.timeSeries.get(j);
                            startLine = i;
                            otherStartLine = j;
                            severity = distanceFt;
                            lateralDistance = lateralDistanceFt;
                            verticalDistance = verticalDistanceFt;
                        }
                        endLine = i;
                        otherEndLine = j;
                        endTime = flightInfo.dateSeries.get(i) + " " + flightInfo.timeSeries.get(i);
                        otherEndTime = otherInfo.dateSeries.get(j) + " " + otherInfo.timeSeries.get(j);

                        if (distanceFt < severity) {
                            // this time was even closer than the last closest proximity
                            // for this event, update the severity
                            severity = distanceFt;
                        }
                        lateralDistance = Math.min(lateralDistanceFt, lateralDistance);
                        verticalDistance = Math.min(verticalDistanceFt, verticalDistance);
                        // increment the startCount, reset the endCount
                        startCount++;
                        stopCount = 0;

                    } else {
                        // this time didn't trigger proximity
                        if (startTime != null) {
                            // we're already tracking a proximity event, so increment
                            // the stop count
                            stopCount++;

                            if (stopCount == stopBuffer) {
                                if (startCount >= startBuffer) {
                                    // we had enough triggers to reach the start count so create the event
                                    System.out.println("Creating event for flight : " + flightId);
                                    createEventForFlight(flightId, flightInfo, eventList,
                                            startTime, endTime, otherStartTime, otherEndTime,
                                            startLine, endLine, otherStartLine, otherEndLine,
                                            severity, lateralDistance, verticalDistance, otherFlight, otherInfo);
                                }

                                // reset the event values
                                startTime = null;
                                otherStartTime = null;
                                endTime = null;
                                otherEndTime = null;
                                startLine = -1;
                                endLine = -1;
                                otherEndLine = -1;

                                // reset the start and stop counts
                                startCount = 0;
                                stopCount = 0;
                            }
                        }
                    }

                    // iterate both as they had matching times
                    i++;
                    j++;
                }

                // if there was an event still going when one flight ended, create it and add it to the list
                if (startTime != null) {
                    createEventForFlight(flightId, flightInfo, eventList, startTime, endTime,
                            otherStartTime, otherEndTime, startLine, endLine, otherStartLine, otherEndLine,
                            severity, lateralDistance, verticalDistance, otherFlight, otherInfo);
                }
            } // end the loop processing a particular flight
        } // end the loop processing all flights

        for (Event event : eventList) {
            LOG.info("\t" + event.toString());
            eventsFound++;
            uploadProcessedEmail.addProximity(flightFilename,
                    "flight " + flightId + ", '" + flightFilename + "' - had a proximity event with flight "
                            + event.getOtherFlightId() + " from " + event.getStartTime() + " to "
                            + event.getEndTime());
        }

        LOG.info("\n");

        // Step 2: export the events and their statistics in the database
        exportEventsAndStatistics(connection, eventList, flight);
    }

    // Disabling parameters check here
    //CHECKSTYLE:OFF
    private static void createEventForFlight(int flightId, FlightTimeLocation flightInfo, List<Event> eventList,
                                             String startTime, String endTime,
                                             String otherStartTime, String otherEndTime,
                                             int startLine, int endLine,
                                             int otherStartLine, int otherEndLine,
                                             double severity, double lateralDistance, double verticalDistance,
                                             Flight otherFlight, FlightTimeLocation otherInfo) {
    //CHECKSTYLE:ON
        Event event = new Event(startTime, endTime, startLine, endLine, severity,
                otherFlight.getId());
        Event otherEvent = new Event(otherStartTime, otherEndTime, otherStartLine,
                otherEndLine, severity, flightId);
        EventMetaData lateralDistanceMetaData = new EventMetaData("lateral_distance",
                lateralDistance);
        EventMetaData verticalDistanceMetaData = new EventMetaData("vertical_distance",
                verticalDistance);
        event.addMetaData(lateralDistanceMetaData);
        event.addMetaData(verticalDistanceMetaData);

        otherEvent.addMetaData(lateralDistanceMetaData);
        otherEvent.addMetaData(verticalDistanceMetaData);
        if (severity > 0) {
            double[] rateOfClosureArray = calculateRateOfClosure(flightInfo, otherInfo,
                    startLine, endLine, otherStartLine, otherEndLine);
            RateOfClosure rateOfClosure = new RateOfClosure(rateOfClosureArray);
            event.setRateOfClosure(rateOfClosure);
            otherEvent.setRateOfClosure(rateOfClosure);
        }

        addProximityIfNotInList(eventList, event);
        addProximityIfNotInList(eventList, otherEvent);
    }

    public static void calculateProximity(Connection connection, int uploadId,
            UploadProcessedEmail uploadProcessedEmail) throws IOException, SQLException {
        Instant start = Instant.now();

        ArrayList<Flight> flights = Flight.getFlights(connection,
                "upload_id = " + uploadId
                        + " AND NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = "
                        + ADJACENCY_EVENT_DEFINITION_ID + " AND flight_processed.flight_id = flights.id)");

        int count = 0;
        for (Flight flight : flights) {
            if (!flight.insertCompleted()) {
                // this flight is currently being inserted to
                // the database by ProcessFlights
                continue;
            }

            processFlight(connection, flight, uploadProcessedEmail);
            count++;
        }

        Instant end = Instant.now();
        double elapsedSeconds = (double) Duration.between(start, end).toMillis() / 1000.0;
        double avgSeconds = elapsedSeconds / (double) count;
        double avgTimeMatchedFlights = ((double) timeMatchFlights / (double) count);
        double avgLocationMatchedFlights = ((double) locMatchFlights / (double) count);

        LOG.info("calculated " + count + " proximity evaluations in " + elapsedSeconds + " seconds, averaged: "
                + avgSeconds + " seconds per flight");
        LOG.info("avg time matched flights: " + avgTimeMatchedFlights + ", avg loc matched flights: "
                + avgLocationMatchedFlights);
        LOG.info("proximity events found:" + eventsFound);

        uploadProcessedEmail.setProximityElapsedTime(elapsedSeconds, avgSeconds, avgTimeMatchedFlights,
                avgLocationMatchedFlights);
    }

    public static void main(String[] arguments) {
        try (Connection connection = Database.getConnection()) {
            int flightsPerQuery = 5000;
            while (true) {
                Instant start = Instant.now();

                ArrayList<Flight> flights = Flight.getFlights(connection,
                        "NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = "
                                + ADJACENCY_EVENT_DEFINITION_ID + " AND flight_processed.flight_id = flights.id)",
                        flightsPerQuery);

                int count = 0;
                for (Flight flight : flights) {
                    if (!flight.insertCompleted()) {
                        // this flight is currently being inserted to
                        // the database by ProcessFlights
                        continue;
                    }

                    processFlight(connection, flight, null);
                    count++;
                }

                Instant end = Instant.now();
                double elapsedSeconds = (double) Duration.between(start, end).toMillis() / 1000.0;
                double avgSeconds = elapsedSeconds / (double) count;
                LOG.info("calculated " + count + " adjacency evaluations in " + elapsedSeconds + " seconds, averaged: "
                        + avgSeconds + " seconds per flight");
                LOG.info("avg time matched flights: " + ((double) timeMatchFlights / (double) count)
                        + ", avg loc matched flights: " + ((double) locMatchFlights / (double) count));
                LOG.info("evnets found:" + eventsFound);
                // System.exit(1);

                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    System.err.println(e);
                    e.printStackTrace();
                }
            }

            // connection.close();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.err.println("finished!");
        System.exit(1);
    }
}
