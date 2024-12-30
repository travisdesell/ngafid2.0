package org.ngafid;

import org.ngafid.events.Event;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.StringTimeSeries;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.TreeSet;

import org.ngafid.events.EventDefinition;
import org.ngafid.events.EventStatistics;

import org.ngafid.filters.Conditional;
import org.ngafid.filters.Filter;
import org.ngafid.filters.Pair;
import java.util.logging.*;

public class CalculateExceedences {
    private static final Logger LOG = Logger.getLogger(CalculateExceedences.class.getName());

    private static final String TIME_SERIES_NAME = "Lcl Time";
    private static final String DATE_SERIES_NAME = "Lcl Date";
    private static ArrayList<EventDefinition> allEvents = null;

    private final Conditional conditional;
    private final int startBuffer;
    private final int stopBuffer;

    /**
     * Constructor
     * @param eventDefinition the event definition to calculate exceedences for
     */
    public CalculateExceedences(EventDefinition eventDefinition) {
        Filter filter = eventDefinition.getFilter();
        this.conditional = new Conditional(filter);
        this.startBuffer = eventDefinition.getStartBuffer();
        this.stopBuffer = eventDefinition.getStopBuffer();
    }

    public void processFlight(Connection connection, Flight flight, EventDefinition eventDefinition,
            UploadProcessedEmail uploadProcessedEmail) throws IOException, SQLException {
        int fleetId = flight.getFleetId();
        int flightId = flight.getId();
        int airframeNameId = flight.getAirframeNameId();
        String flightFilename = flight.getFilename();

        LOG.info("Processing flight: " + flightId + ", " + flightFilename);

        LOG.info("Event is: '" + eventDefinition.getName() + "'");

        // first check and see if this was actually a flight (RPM > 800)
        Pair<Double, Double> minMaxRPM1 = DoubleTimeSeries.getMinMax(connection, flightId, "E1 RPM");
        Pair<Double, Double> minMaxRPM2 = DoubleTimeSeries.getMinMax(connection, flightId, "E2 RPM");

        // LOG.info("minMaxRPM1: " + minMaxRPM1);
        // LOG.info("minMaxRPM2: " + minMaxRPM2);

        final int rpmThreshold = 800;
        if ((minMaxRPM1 == null && minMaxRPM2 == null) // both RPM values are null, can't calculate exceedence
                || (minMaxRPM2 == null && minMaxRPM1.second() < rpmThreshold) // RPM2 is null, RPM1 is
                                                                                           // < 800
                || (minMaxRPM1 == null && minMaxRPM2.second() < rpmThreshold) // RPM1 is null, RPM2 is
                                                                                           // < 800
                || (minMaxRPM1 != null && minMaxRPM1.second() < rpmThreshold)
                        && (minMaxRPM2 != null && minMaxRPM2.second() < rpmThreshold)) { // RPM1 and RPM2 < 800
            // couldn't calculate exceedences for this flight because the engines never
            // kicked on (it didn't fly)
            LOG.info("engines never turned on, setting flight_processed.had_error = 1");

            if (uploadProcessedEmail != null) {
                uploadProcessedEmail.addExceedenceError(flightFilename,
                        "could not calculate exceedences for flight " + flightId + ", '" + flightFilename
                                + "' - engines never turned on");
            }

            try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET " +
                            "fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 1")) {
                stmt.setInt(1, fleetId);
                stmt.setInt(2, flightId);
                stmt.setInt(3, eventDefinition.getId());
                // LOG.info(stmt.toString());
                stmt.executeUpdate();
            }
            return;
        }

        TreeSet<String> columnNames = eventDefinition.getColumnNames();
        // LOG.info("Number of Column Name(s): [ " + columnNames.size() + " ]");

        // first test and see if min/max values can violate exceedence, otherwise we can
        // skip
        conditional.reset();
        for (String columnName : columnNames) {
            Pair<Double, Double> minMax = DoubleTimeSeries.getMinMax(connection, flightId, columnName);

            if (minMax == null) {
                LOG.info("minMax was null, setting flight_processed.had_error = 1");
                // couldn't calculate this exceedence because at least one of the columns was
                // missing
                if (uploadProcessedEmail != null) {
                    uploadProcessedEmail.addExceedenceError(flightFilename,
                            "could not calculate '" + eventDefinition.getName() + "' for flight " + flightId + ", '"
                                    + flightFilename + "' - " + columnName + " was missing");
                }

                try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET " +
                        "fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 1")) {
                    stmt.setInt(1, fleetId);
                    stmt.setInt(2, flightId);
                    stmt.setInt(3, eventDefinition.getId());
                    // LOG.info(stmt.toString());
                    stmt.executeUpdate();
                }
                return;
            }

            LOG.info(columnName + ", min: " + minMax.first() + ", max: " + minMax.second());
            conditional.set(columnName, minMax);
        }

        LOG.info("Post-set conditional: " + conditional);
        boolean result = conditional.evaluate();
        LOG.info("overall result: " + result);

        if (!result) {
            // this flight could not have caused one of these events
            try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET " +
                    "fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 0")) {
                stmt.setInt(1, fleetId);
                stmt.setInt(2, flightId);
                stmt.setInt(3, eventDefinition.getId());
                // LOG.info(stmt.toString());
                stmt.executeUpdate();
            }

            EventStatistics.updateFlightsWithoutEvent(connection, fleetId, airframeNameId, eventDefinition.getId(),
                    flight.getStartDateTime());
            return;
        }

        StringTimeSeries timeSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, TIME_SERIES_NAME);
        StringTimeSeries dateSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, DATE_SERIES_NAME);

        if (timeSeries == null || dateSeries == null) {
            // couldn't calculate this exceedence because the date or time column was
            // missing
            LOG.info("time series or date series was missing, setting flight_processed.had_error = 1");
            if (uploadProcessedEmail != null)
                uploadProcessedEmail.addExceedenceError(flightFilename,
                        "could not calculate exceedences for flight " + flightId + ", '" + flightFilename
                                + "' - date or time was missing");

            try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET " +
                    "fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 1")) {
                stmt.setInt(1, fleetId);
                stmt.setInt(2, flightId);
                stmt.setInt(3, eventDefinition.getId());
                LOG.info(stmt.toString());
                stmt.executeUpdate();
            }
            return;
        }

        DoubleTimeSeries[] doubleSeries = new DoubleTimeSeries[columnNames.size()];
        int i = 0;
        for (String columnName : columnNames) {
            doubleSeries[i++] = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, columnName);
        }

        // Step 1: Calculate all the pitch events and put them in this pitchEvents
        // ArrayList
        ArrayList<Event> eventList = new ArrayList<>();
        int lineNumber = 0;
        String startTime = null;
        String endTime = null;
        int startLine = -1;
        int endLine = -1;

        int startCount = 0;
        int stopCount = 0;
        double severity = 0;

        // skip the first 30 seconds as that is usually the FDR being initialized
        for (i = 30; i < doubleSeries[0].size(); i++) {
            // for (i = 0; i < doubleSeries[0].size(); i++) {
            lineNumber = i;

            // LOG.info("Pre-set conditional: " + conditional.toString());

            conditional.reset();
            for (DoubleTimeSeries series : doubleSeries) {
                conditional.set(series.getName(), series.get(i));
            }
            // LOG.info("Post-set conditional: " + conditional.toString());

            result = conditional.evaluate();

            // LOG.info(conditional + ", result: " + result);

            if (!result) {
                if (startTime != null) {
                    // we're tracking an event, so increment the stopCount
                    stopCount++;
                    LOG.info("stopCount: " + stopCount + " with on line: " + lineNumber);

                    if (stopCount == stopBuffer) {
                        System.err.println("Stop count (" + stopCount + ") reached the stop buffer (" + stopBuffer
                                + "), new event created!");

                        if (startCount >= startBuffer) {
                            // we had enough triggers to reach the start count so create the event
                            Event event = new Event(startTime, endTime, startLine, endLine, severity);
                            eventList.add(event);
                        }

                        // reset the event values
                        startTime = null;
                        endTime = null;
                        startLine = -1;
                        endLine = -1;

                        // reset the start and stop counts
                        startCount = 0;
                        stopCount = 0;
                    }
                }
            } else {
                // row triggered exceedence

                // startTime is null if an exceedence is not being tracked
                if (startTime == null) {
                    startTime = dateSeries.get(i) + " " + timeSeries.get(i);
                    startLine = lineNumber;
                    severity = eventDefinition.getSeverity(doubleSeries, i);

                    LOG.info("start date time: " + startTime + ", start line number: " + startLine);
                }
                endLine = lineNumber;
                endTime = dateSeries.get(i) + " " + timeSeries.get(i);
                severity = eventDefinition.updateSeverity(severity, doubleSeries, i);

                // increment the startCount, reset the endCount
                startCount++;
                stopCount = 0;
            }
        }

        if (startTime != null) {
            Event event = new Event(startTime, endTime, startLine, endLine, severity);
            eventList.add(event);
        }
        LOG.info("");

        for (i = 0; i < eventList.size(); i++) {
            Event event = eventList.get(i);
            LOG.info("Event : [line: " + event.getStartLine() + " to " + event.getEndLine() + ", time: "
                    + event.getStartTime() + " to " + event.getEndTime() + "]");
            if (uploadProcessedEmail != null) {
                uploadProcessedEmail.addExceedence(flightFilename,
                        "flight " + flightId + ", '" + flightFilename + "' - '" + eventDefinition.getName()
                                + "' from " + event.getStartTime() + " to " + event.getEndTime());
            }
        }

        // Step 2: export the pitch events to the database
        double sumDuration = 0.0;
        double sumSeverity = 0.0;
        double minSeverity = Double.MAX_VALUE;
        double maxSeverity = -Double.MAX_VALUE;
        double minDuration = Double.MAX_VALUE;
        double maxDuration = -Double.MAX_VALUE;
        for (i = 0; i < eventList.size(); i++) {
            Event event = eventList.get(i);

            event.updateDatabase(connection, fleetId, flightId, eventDefinition.getId());
            event.updateStatistics(connection, fleetId, airframeNameId, eventDefinition.getId());

            double currentSeverity = eventList.get(i).getSeverity();
            double currentDuration = eventList.get(i).getDuration();
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
            try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET " +
                    "fleet_id = ?, flight_id = ?, event_definition_id = ?, count = ?, sum_duration = ?, " +
                    "min_duration = ?, max_duration = ?, sum_severity = ?, min_severity = ?, max_severity = ?, " +
                    "had_error = 0")) {
                stmt.setInt(1, fleetId);
                stmt.setInt(2, flightId);
                stmt.setInt(3, eventDefinition.getId());
                stmt.setInt(4, eventList.size());
                stmt.setDouble(5, sumDuration);
                stmt.setDouble(6, minDuration);
                stmt.setDouble(7, maxDuration);
                stmt.setDouble(8, sumSeverity);
                stmt.setDouble(9, minSeverity);
                stmt.setDouble(10, maxSeverity);
                LOG.info(stmt.toString());
                stmt.executeUpdate();
            }

            EventStatistics.updateFlightsWithEvent(connection, fleetId, airframeNameId, eventDefinition.getId(),
                    flight.getStartDateTime());

        } else {
            try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET " +
                    "fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 0")) {
                stmt.setInt(1, fleetId);
                stmt.setInt(2, flightId);
                stmt.setInt(3, eventDefinition.getId());
                LOG.info(stmt.toString());
                stmt.executeUpdate();
            }

            EventStatistics.updateFlightsWithoutEvent(connection, fleetId, airframeNameId, eventDefinition.getId(),
                    flight.getStartDateTime());
        }
    }

    /**
     * Calculate exceedences for all events
     * @param connection the database connection
     * @param uploadId the upload id to calculate exceedences for
     * @param uploadProcessedEmail the email object to send notifications to
     * @throws IOException IO exception
     * @throws SQLException SQL exception
     */
    public static void calculateExceedences(Connection connection, int uploadId,
            UploadProcessedEmail uploadProcessedEmail) throws IOException, SQLException {
        Instant start = Instant.now();
        if (allEvents == null) {
            allEvents = EventDefinition.getAll(connection, "id > ?", new Object[] { 0 });
        }
        LOG.info("n events = " + allEvents.size());

        int airframeTypeId = new Airframes.Airframe(connection, "Fixed Wing").getId();

        for (EventDefinition currentDefinition : allEvents) {
            // process events for this event type
            LOG.info("\t" + currentDefinition.toString());

            CalculateExceedences currentCalculator = new CalculateExceedences(currentDefinition);

            ArrayList<Flight> flights = null;

            if (currentDefinition.getAirframeNameId() == 0) {
                flights = Flight.getFlights(connection,
                        "airframe_type_id = " + airframeTypeId + " AND upload_id = " + uploadId
                                + " AND NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = "
                                + currentDefinition.getId() + " AND flight_processed.flight_id = flights.id)");
            } else {
                flights = Flight.getFlights(connection,
                        "flights.airframe_id = " + currentDefinition.getAirframeNameId() + " AND upload_id = "
                                + uploadId + " AND airframe_type_id = " + airframeTypeId
                                + " AND NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = "
                                + currentDefinition.getId() + " AND flight_processed.flight_id = flights.id)");
            }

            for (Flight flight : flights) {
                if (!flight.insertCompleted()) {
                    // this flight is currently being inserted to
                    // the database by ProcessFlights
                    continue;
                }

                currentCalculator.processFlight(connection, flight, currentDefinition, uploadProcessedEmail);
            }
        }

        Instant end = Instant.now();
        long elapsedMillis = Duration.between(start, end).toMillis();
        double elapsedSeconds = ((double) elapsedMillis) / 1000;
        LOG.info("finished in " + elapsedSeconds);

        if (uploadProcessedEmail != null) {
            uploadProcessedEmail.setExceedencesElapsedTime(elapsedSeconds);
        }
    }

    public static void main(String[] arguments) {
        while (true) {
            try (Connection connection = Database.getConnection()) {
                // for now only calculate exceedences for fixed wing aircraft
                int airframeTypeId = new Airframes.Airframe(connection, "Fixed Wing").getId();

                Instant start = Instant.now();

                ArrayList<EventDefinition> allEvents = EventDefinition.getAll(connection, "id > ?", new Object[]{0});
                LOG.info("n events = " + allEvents.size());

                int flightsProcessed = 0;
                for (EventDefinition currentDefinition : allEvents) {
                    // process events for this event type
                    LOG.info("\t" + currentDefinition.toString());

                    CalculateExceedences currentCalculator = new CalculateExceedences(currentDefinition);

                    ArrayList<Flight> flights = null;

                    if (currentDefinition.getAirframeNameId() == 0) {
                        flights = Flight.getFlights(connection, "airframe_type_id = " + airframeTypeId
                                + " AND NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = "
                                + currentDefinition.getId() + " AND flight_processed.flight_id = flights.id)", 100);
                    } else {
                        flights = Flight.getFlights(connection, "flights.airframe_id = "
                                + currentDefinition.getAirframeNameId() + " AND airframe_type_id = " + airframeTypeId
                                + " AND NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = "
                                + currentDefinition.getId() + " AND flight_processed.flight_id = flights.id)", 100);
                    }

                    for (Flight flight : flights) {
                        if (!flight.insertCompleted()) {
                            // this flight is currently being inserted to
                            // the database by ProcessFlights
                            continue;
                        }
                        flightsProcessed += 1;
                        currentCalculator.processFlight(connection, flight, currentDefinition, null);
                    }
                }

                Instant end = Instant.now();
                long elapsedMillis = Duration.between(start, end).toMillis();
                double elapsedSeconds = ((double) elapsedMillis) / 1000;
                LOG.info("finished in " + elapsedSeconds + ", processed " + flightsProcessed);

                try {
                    if (flightsProcessed == 0) {
                        Thread.sleep(3000);
                    }
                } catch (Exception e) {
                    LOG.severe(e.toString());
                    e.printStackTrace();
                }

                // connection.close();
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
