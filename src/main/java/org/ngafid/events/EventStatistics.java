package org.ngafid.events;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.ngafid.Database;
import org.ngafid.accounts.Fleet;
import org.ngafid.flights.DoubleTimeSeries;


import java.util.logging.Logger;

public class EventStatistics {
    private static final Logger LOG = Logger.getLogger(EventStatistics.class.getName());

    private static final int MIN_VALUE = -999999;
    private static final int MAX_VALUE = 999999;

    public static String getFirstOfMonth(String dateTime) {
        return dateTime.substring(0, 8) + "01";
    }

    public static void clearAllStatistics(Connection connection, Fleet fleet, EventDefinition eventDefinition) throws SQLException {
        String sql = "DELETE FROM event_statistics WHERE fleet_id = ? AND event_definition_id = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, fleet.getId());
        query.setInt(2, eventDefinition.getId());

        query.executeUpdate();
    }

    public static void updateEventStatistics(Connection connection, int fleetId, int airframeNameId, int eventId, String startDateTime, double severity, double duration) throws SQLException {
        String firstOfMonth = getFirstOfMonth(startDateTime);

        String query = "INSERT INTO event_statistics (fleet_id, airframe_id, event_definition_id, month_first_day, total_events, min_severity, sum_severity, max_severity, min_duration, sum_duration, max_duration) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE min_severity = LEAST(min_severity, ?), sum_severity = sum_severity + ?, max_severity = GREATEST(max_severity, ?), min_duration = LEAST(min_duration, ?), sum_duration = sum_duration + ?, max_duration = GREATEST(max_duration, ?), total_events = total_events + 1";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);
        preparedStatement.setInt(2, airframeNameId);
        preparedStatement.setInt(3, eventId);
        preparedStatement.setString(4, firstOfMonth);
        preparedStatement.setInt(5, 0);

        preparedStatement.setDouble(6, severity);
        preparedStatement.setDouble(7, severity);
        preparedStatement.setDouble(8, severity);

        preparedStatement.setDouble(9, duration);
        preparedStatement.setDouble(10, duration);
        preparedStatement.setDouble(11, duration);

        preparedStatement.setDouble(12, severity);
        preparedStatement.setDouble(13, severity);
        preparedStatement.setDouble(14, severity);

        preparedStatement.setDouble(15, duration);
        preparedStatement.setDouble(16, duration);
        preparedStatement.setDouble(17, duration);

        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static void updateFlightsWithEvent(Connection connection, int fleetId, int airframeNameId, int eventDefinitionId, String startDateTime) throws SQLException {
        //cannot update event statistics if the flight had no startDateTime
        if (startDateTime == null) return;

        String firstOfMonth = getFirstOfMonth(startDateTime);

        String query = "INSERT INTO event_statistics (fleet_id, airframe_id, event_definition_id, month_first_day, flights_with_event, total_flights, min_severity, sum_severity, max_severity, min_duration, sum_duration, max_duration) VALUES (?, ?, ?, ?, 1, 1, 999999, 0, -999999, 999999, 0, -999999) ON DUPLICATE KEY UPDATE flights_with_event = flights_with_event + 1, total_flights = total_flights + 1";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);
        preparedStatement.setInt(2, airframeNameId);
        preparedStatement.setInt(3, eventDefinitionId);
        preparedStatement.setString(4, firstOfMonth);

        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static void updateFlightsWithoutEvent(Connection connection, int fleetId, int airframeNameId, int eventDefinitionId, String startDateTime) throws SQLException {
        //cannot update event statistics if the flight had no startDateTime
        if (startDateTime == null) return;

        String firstOfMonth = getFirstOfMonth(startDateTime);

        String query = "INSERT INTO event_statistics (fleet_id, airframe_id, event_definition_id, month_first_day, flights_with_event, total_flights, min_severity, sum_severity, max_severity, min_duration, sum_duration, max_duration) VALUES (?, ?, ?, ?, 0, 1, 999999, 0, -999999, 999999, 0, -999999) ON DUPLICATE KEY UPDATE total_flights = total_flights + 1";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);
        preparedStatement.setInt(2, airframeNameId);
        preparedStatement.setInt(3, eventDefinitionId);
        preparedStatement.setString(4, firstOfMonth);

        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    // "struct" that can represent a row of the EventStatistics table
    private static class EventStatisticsRow {
        int fleetId, airframeId, eventDefinitionId, flightsWithEvent, totalFlights, totalEvents, durMin, durMax, durSum;
        Date monthFirstDay;
        double sevMin, sevMax, sevSum;

        public void insert(Connection connection) throws SQLException {
            String sql = "INSERT INTO event_statistics(fleet_id, airframe_id, event_definition_id, month_first_day, flights_with_event, total_flights, total_events, min_duration, sum_duration, max_duration, min_severity, sum_severity, max_severity) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement query = connection.prepareStatement(sql);

            query.setInt(1, this.fleetId);
            query.setInt(2, this.airframeId);
            query.setInt(3, this.eventDefinitionId);
            query.setDate(4, this.monthFirstDay);
            query.setInt(5, this.flightsWithEvent);
            query.setInt(6, this.totalFlights);
            query.setInt(7, this.totalEvents);
            query.setInt(8, this.durMin);
            query.setInt(9, this.durSum);
            query.setInt(10, this.durMax);
            query.setDouble(11, this.sevMin);
            query.setDouble(12, this.sevSum);
            query.setDouble(13, this.sevMax);

            query.executeUpdate();
        }
    }


    private static class EventRow {
        String rowName;
        String humanReadable;

        int flightsWithoutError;
        double avgEvents;

        int flightsWithEvent;
        int totalEvents;
        int processedFlights;
        double avgDuration;
        double minDuration;
        double maxDuration;
        double avgSeverity;
        double minSeverity;
        double maxSeverity;

        // TODO No longer used?
        int aggFlightsWithoutError;
        int aggFlightsWithEvent;
        int aggTotalEvents;
        double aggAvgEvents;
        double aggAvgDuration;
        double aggMinDuration;
        double aggMaxDuration;
        double aggAvgSeverity;
        double aggMinSeverity;
        double aggMaxSeverity;


        public EventRow(String rowName) {
            this.rowName = rowName;
        }

        public static EventRow getNewStatistics(Connection connection, EventDefinition eventDefinition, int fleetId, int uploadId, boolean isGeneric) throws SQLException {
            EventRow eventRow = new EventRow(eventDefinition.getName());
            eventRow.humanReadable = eventDefinition.toHumanReadable();
            int airframeNameId = eventDefinition.getAirframeNameId();
            int eventId = eventDefinition.getId();
            PreparedStatement preparedStatement;
            ResultSet resultSet;

            // Query Strings
            String flightsProcessedQ = "SELECT count(*) FROM flight_processed JOIN flights ON flights.id = flight_processed.flight_id WHERE flight_processed.fleet_id = ? AND flight_processed.event_definition_id = ? AND flights.upload_id = ? " + (!isGeneric ? " AND flights.airframe_id = ?" : "");
            String flightsWithEventQ = "SELECT count(DISTINCT e.flight_id) FROM events AS e JOIN flights AS f ON e.flight_id = f.id WHERE e.event_definition_id = ? AND e.fleet_id = ? AND f.upload_id = ? " + (!isGeneric ? " AND f.airframe_id = ? " : "" );
            String allStatsQ = "SELECT count(*), min(e.severity), avg(e.severity), max(e.severity), min(e.end_time - e.start_time), avg(e.end_time - e.start_time), max(e.end_time - e.start_time) FROM events AS e JOIN flights AS f ON e.flight_id = f.id WHERE e.fleet_id = ? AND f.upload_id = ? AND e.event_definition_id = ? " + (!isGeneric ? " AND f.airframe_id = ?" : "");

            // Get number flights processed from this upload and airframe
            preparedStatement = connection.prepareStatement(flightsProcessedQ);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setInt(2, eventId);
            preparedStatement.setInt(3, uploadId);
            if (!isGeneric) preparedStatement.setInt(4, airframeNameId);

            resultSet = preparedStatement.executeQuery();
            resultSet.next();
            eventRow.processedFlights = resultSet.getInt(1);
            resultSet.close();
            preparedStatement.close();

            // Get number flights with event from this upload and airframe
            preparedStatement = connection.prepareStatement(flightsWithEventQ);
            preparedStatement.setInt(1, eventId);
            preparedStatement.setInt(2, fleetId);
            preparedStatement.setInt(3, uploadId);
            if (!isGeneric) preparedStatement.setInt(4, airframeNameId);

            resultSet = preparedStatement.executeQuery();
            eventRow.flightsWithEvent = resultSet.next() ? resultSet.getInt(1) : 0;
            resultSet.close();
            preparedStatement.close();

            // Get the rest of the stats for this upload and airframe
            preparedStatement = connection.prepareStatement(allStatsQ);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setInt(2, uploadId);
            preparedStatement.setInt(3, eventId);
            if (!isGeneric) preparedStatement.setInt(4, airframeNameId);

            resultSet = preparedStatement.executeQuery();
            eventRow.totalEvents = (resultSet.next() ? resultSet.getInt(1) : 0);

            if (eventRow.totalEvents != 0) {
                eventRow.minSeverity = resultSet.getDouble(2);
                eventRow.avgSeverity = resultSet.getDouble(3);
                eventRow.maxSeverity = resultSet.getDouble(4);
                eventRow.minDuration = resultSet.getDouble(5);
                eventRow.avgDuration = resultSet.getDouble(6);
                eventRow.maxDuration = resultSet.getDouble(7);
            } else {
                eventRow.minSeverity = 0;
                eventRow.avgSeverity = 0;
                eventRow.maxSeverity = 0;
                eventRow.minDuration = 0;
                eventRow.avgDuration = 0;
                eventRow.maxDuration = 0;
            }
            resultSet.close();
            preparedStatement.close();

            return eventRow;
        }

        // TODO Remove because not used anymore
        public static EventRow getStatistics(Connection connection, String rowName, int fleetId, int eventId, String extraQuery, int[] extraParams) throws SQLException {
            EventRow eventRow = new EventRow(rowName);

            String query = "SELECT SUM(flights_with_event), SUM(total_flights), SUM(total_events), MIN(min_duration), SUM(sum_duration), MAX(max_duration), MIN(min_severity), SUM(sum_severity), MAX(max_severity) FROM event_statistics WHERE fleet_id = ? AND event_definition_id = ?";

            if (!extraQuery.equals("")) {
                query += " AND " + extraQuery;
            }

            //LOG.info(query);

            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setInt(2, eventId);
            for (int i = 0; i < extraParams.length; i++) {
                preparedStatement.setInt(3 + i, extraParams[i]);
            }

            //LOG.info(preparedStatement.toString());

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                eventRow.flightsWithEvent = resultSet.getInt(1);
                eventRow.flightsWithoutError = resultSet.getInt(2);
                eventRow.totalEvents = resultSet.getInt(3);
                eventRow.minDuration = resultSet.getDouble(4);
                eventRow.avgDuration = resultSet.getDouble(5) / eventRow.totalEvents;
                eventRow.maxDuration = resultSet.getDouble(6);
                eventRow.minSeverity = resultSet.getDouble(7);
                eventRow.avgSeverity = resultSet.getDouble(8) / eventRow.totalEvents;
                eventRow.maxSeverity = resultSet.getDouble(9);

            } else {
                eventRow.flightsWithEvent = 0;
                eventRow.flightsWithoutError = 0;
                eventRow.totalEvents = 0;
                eventRow.minDuration = 0;
                eventRow.avgDuration = 0;
                eventRow.maxDuration = 0;
                eventRow.minSeverity = 0;
                eventRow.avgSeverity = 0;
                eventRow.maxSeverity = 0;
            }

            resultSet.close();
            preparedStatement.close();

            query = "SELECT SUM(flights_with_event), SUM(total_flights), SUM(total_events), MIN(min_duration), SUM(sum_duration), MAX(max_duration), MIN(min_severity), SUM(sum_severity), MAX(max_severity) FROM event_statistics WHERE fleet_id != ? AND event_definition_id = ?";

            if (!extraQuery.equals("")) {
                query += " AND " + extraQuery;
            }

            //LOG.info(query);

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setInt(2, eventId);
            for (int i = 0; i < extraParams.length; i++) {
                preparedStatement.setInt(3 + i, extraParams[i]);
            }

            //LOG.info(preparedStatement.toString());

            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                eventRow.aggFlightsWithEvent = resultSet.getInt(1);
                eventRow.aggFlightsWithoutError = resultSet.getInt(2);
                eventRow.aggTotalEvents = resultSet.getInt(3);
                eventRow.aggMinDuration = resultSet.getDouble(4);
                eventRow.aggAvgDuration = resultSet.getDouble(5) / eventRow.aggTotalEvents;
                eventRow.aggMaxDuration = resultSet.getDouble(6);
                eventRow.aggMinSeverity = resultSet.getDouble(7);
                eventRow.aggAvgSeverity = resultSet.getDouble(8) / eventRow.aggTotalEvents;
                eventRow.aggMaxSeverity = resultSet.getDouble(9);

            } else {
                eventRow.aggFlightsWithEvent = 0;
                eventRow.aggFlightsWithoutError = 0;
                eventRow.aggTotalEvents = 0;
                eventRow.aggMinDuration = 0;
                eventRow.aggAvgDuration = 0;
                eventRow.aggMaxDuration = 0;
                eventRow.aggMinSeverity = 0;
                eventRow.aggAvgSeverity = 0;
                eventRow.aggMaxSeverity = 0;
            }


            //zero out the default values if there are no events
            if (eventRow.totalEvents == 0) {
                eventRow.minDuration = 0;
                eventRow.avgDuration = 0;
                eventRow.maxDuration = 0;
                eventRow.minSeverity = 0;
                eventRow.avgSeverity = 0;
                eventRow.maxSeverity = 0;
            }

            if (eventRow.aggTotalEvents == 0) {
                eventRow.aggMinDuration = 0;
                eventRow.aggAvgDuration = 0;
                eventRow.aggMaxDuration = 0;
                eventRow.aggMinSeverity = 0;
                eventRow.aggAvgSeverity = 0;
                eventRow.aggMaxSeverity = 0;
            }

            if (Double.isNaN(eventRow.avgDuration)) eventRow.avgDuration = 0;
            if (Double.isNaN(eventRow.avgEvents)) eventRow.avgEvents = 0;
            if (Double.isNaN(eventRow.avgSeverity)) eventRow.avgSeverity = 0;

            if (Double.isNaN(eventRow.aggAvgDuration)) eventRow.aggAvgDuration = 0;
            if (Double.isNaN(eventRow.aggAvgEvents)) eventRow.aggAvgEvents = 0;
            if (Double.isNaN(eventRow.aggAvgSeverity)) eventRow.aggAvgSeverity = 0;

            resultSet.close();
            preparedStatement.close();

            return eventRow;
        }
    }

    // TODO Remove because not used anymore
    private static class AirframeStatistics {
        int eventId;
        String eventName;

        int totalFlights;
        int processedFlights;

        String humanReadable;

        ArrayList<EventRow> monthStats = new ArrayList<EventRow>();

        // TODO Remove because not used anymore
        AirframeStatistics(Connection connection, EventDefinition eventDefinition, int fleetId) throws SQLException {
            this.eventId = eventDefinition.getId();
            this.eventName = eventDefinition.getName();
            this.humanReadable = eventDefinition.toHumanReadable();

            int airframeNameId = eventDefinition.getAirframeNameId();

            String query;
            PreparedStatement preparedStatement;
            ResultSet resultSet;
            if (airframeNameId == 0) {
                query = "SELECT count(*) FROM flights WHERE fleet_id = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, fleetId);

                resultSet = preparedStatement.executeQuery();
            } else {
                query = "SELECT count(*) FROM flights WHERE fleet_id = ? AND airframe_id = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, fleetId);
                preparedStatement.setInt(2, airframeNameId);

                resultSet = preparedStatement.executeQuery();
            }
            resultSet.next();

            totalFlights = resultSet.getInt(1);

            resultSet.close();
            preparedStatement.close();

            //get number flights processed
            if (airframeNameId == 0) {
                query = "SELECT count(*) FROM flight_processed WHERE flight_processed.fleet_id = ? AND flight_processed.event_definition_id = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, fleetId);
                preparedStatement.setInt(2, eventId);

            } else {
                query = "SELECT count(*) FROM flight_processed INNER JOIN flights ON flights.airframe_id = ? AND flights.id = flight_processed.flight_id WHERE flight_processed.fleet_id = ? AND flight_processed.event_definition_id = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, airframeNameId);
                preparedStatement.setInt(2, fleetId);
                preparedStatement.setInt(3, eventId);
            }
            resultSet = preparedStatement.executeQuery();
            resultSet.next();

            processedFlights = resultSet.getInt(1);

            resultSet.close();
            preparedStatement.close();

            int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);

            //LOG.info("current year: " + currentYear + ", current month: " + currentMonth);

            monthStats.add(EventRow.getStatistics(connection, "Month to Date", fleetId, eventId, "YEAR(month_first_day) >= ? AND MONTH(month_first_day) >= ?", new int[]{currentYear, currentMonth} ));

            int previousMonth = currentMonth - 1;
            int tempYear = currentYear;
            if (currentMonth == 0) {
                currentMonth = 12;
                tempYear = currentYear - 1;
            }

            monthStats.add(EventRow.getStatistics(connection, "Previous Month", fleetId, eventId, "YEAR(month_first_day) = ? AND MONTH(month_first_day) = ?", new int[]{currentYear, currentMonth} ));

            monthStats.add(EventRow.getStatistics(connection, "Year to Date", fleetId, eventId, "YEAR(month_first_day) >= ? AND MONTH(month_first_day) >= ?", new int[]{currentYear, 1} ));

            monthStats.add(EventRow.getStatistics(connection, "Previous Year", fleetId, eventId, "YEAR(month_first_day) = ?", new int[]{currentYear - 1} ));

            monthStats.add(EventRow.getStatistics(connection, "Overall", fleetId, eventId, "", new int[]{}));
        }

        int uploadID;
        EventRow eventRow;

        // TODO Remove because not used anymore
        AirframeStatistics(Connection connection, EventDefinition eventDefinition, int uploadID, int fleetId) throws SQLException {
            this.eventId = eventDefinition.getId();
            this.eventName = eventDefinition.getName();
            this.humanReadable = eventDefinition.toHumanReadable();
            this.uploadID = uploadID;

            int airframeNameId = eventDefinition.getAirframeNameId();
            String query;
            PreparedStatement preparedStatement;
            ResultSet resultSet;

            // Get number flights processed from this upload and airframe
            int paramCount = 1;
            String flightsProcessedQ = "SELECT count(*) FROM flight_processed " + (airframeNameId != 0 ? "INNER JOIN flights ON flights.airframe_id = ? AND flights.id = flight_processed.flight_id " : "") + "WHERE flight_processed.fleet_id = ? AND flight_processed.event_definition_id = ?";
            preparedStatement = connection.prepareStatement(flightsProcessedQ);
            if (airframeNameId == 0) preparedStatement.setInt(paramCount++, airframeNameId);
            preparedStatement.setInt(paramCount++, fleetId);
            preparedStatement.setInt(paramCount++, eventId);

            resultSet = preparedStatement.executeQuery();
            resultSet.next();
            processedFlights = resultSet.getInt(1);
            resultSet.close();
            preparedStatement.close();

            this.eventRow = new EventRow("Stats");
            if (airframeNameId == 0) {
                query = "SELECT count(*), min(e.severity), avg(e.severity), max(e.severity), min(e.end_time - e.start_time), avg(e.end_time - e.start_time), max(e.end_time - e.start_time) FROM events AS e JOIN flights AS f ON e.flight_id = f.id WHERE e.fleet_id = ? AND f.upload_id = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, fleetId);
                preparedStatement.setInt(2, uploadID);
            } else {
                query = "SELECT count(*), min(e.severity), avg(e.severity), max(e.severity), min(e.end_time - e.start_time), avg(e.end_time - e.start_time), max(e.end_time - e.start_time) FROM events AS e JOIN flights AS f ON e.flight_id = f.id WHERE e.fleet_id = ? AND f.upload_id = ? AND f.airframe_id = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, fleetId);
                preparedStatement.setInt(2, uploadID);
                preparedStatement.setInt(3, airframeNameId);
            }
            resultSet = preparedStatement.executeQuery();
            this.eventRow.totalEvents = (resultSet.next() ? resultSet.getInt(1) : 0);

            if (this.eventRow.totalEvents != 0) {
                this.eventRow.minSeverity = resultSet.getDouble(2);
                this.eventRow.avgSeverity = resultSet.getDouble(3);
                this.eventRow.maxSeverity = resultSet.getDouble(4);
                this.eventRow.minDuration = resultSet.getDouble(5);
                this.eventRow.avgDuration = resultSet.getDouble(6);
                this.eventRow.maxDuration = resultSet.getDouble(7);
            } else {
                this.eventRow.minSeverity = 0;
                this.eventRow.avgSeverity = 0;
                this.eventRow.maxSeverity = 0;
                this.eventRow.minDuration = 0;
                this.eventRow.avgDuration = 0;
                this.eventRow.maxDuration = 0;
            }
            resultSet.close();
            preparedStatement.close();
        }
    }

    // TODO Remove OLD...
    ArrayList<AirframeStatistics> events;

    public EventStatistics(Connection connection, int airframeNameId, String airframeName, int fleetId) throws SQLException {
        this.airframeNameId = airframeNameId;
        this.airframeName = airframeName;
        events = new ArrayList<>();

        LOG.info("Accidentally entered old stuff");

        ArrayList<EventDefinition> eventDefinitions = EventDefinition.getAll(connection, "airframe_id = ? AND  (fleet_id = 0 OR fleet_id = ?)", new Object[]{airframeNameId,  fleetId});

        events = new ArrayList<>();
        for (int i = 0; i < eventDefinitions.size(); i++) {

            events.add(new AirframeStatistics(connection, eventDefinitions.get(i), fleetId));
        }
    }


    int airframeNameId;
    String airframeName;
    int totalFlights;
    ArrayList<EventRow> eventRows;

    public EventStatistics(Connection connection, int uploadID, int airframeNameId, String airframeName, int fleetID) throws SQLException {
        this.airframeNameId = airframeNameId;
        this.airframeName = airframeName;
        ArrayList<EventDefinition> eventDefinitions = EventDefinition.getAll(connection, "airframe_id = ? AND  (fleet_id = 0 OR fleet_id = ?)", new Object[]{airframeNameId,  fleetID});
        PreparedStatement preparedStatement;
        ResultSet resultSet;

        // Get number of flights from this upload and airframe
        String totFlightsQ = "SELECT count(*) FROM flights WHERE fleet_id = ? AND upload_id = ? " + (airframeNameId != 0 ? "AND airframe_id = ?" : "");
        preparedStatement = connection.prepareStatement(totFlightsQ);
        preparedStatement.setInt(1, fleetID);
        preparedStatement.setInt(2, uploadID);
        if (airframeNameId != 0) preparedStatement.setInt(3, airframeNameId);

        LOG.info(preparedStatement.toString());
        resultSet = preparedStatement.executeQuery();
        resultSet.next();
        totalFlights = resultSet.getInt(1);
        resultSet.close();
        preparedStatement.close();

        // Calculate stats of individual events
        eventRows = new ArrayList<>();
        for (EventDefinition ed : eventDefinitions) {
            eventRows.add(EventRow.getNewStatistics(connection, ed, fleetID, uploadID, airframeNameId == 0));
        }
    }

    // TODO Not used anymore?
    public static ArrayList<EventStatistics> getAll(Connection connection, int fleetId) throws SQLException {
        String query = "SELECT id, airframe FROM airframes INNER JOIN fleet_airframes ON airframes.id = fleet_airframes.airframe_id WHERE fleet_airframes.fleet_id = ? ORDER BY airframe";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);

        ArrayList<EventStatistics> eventStatistics = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        eventStatistics.add(new EventStatistics(connection, 0, "Generic", fleetId));
        long endTime = System.currentTimeMillis();
        LOG.info("Took " + (endTime - startTime) + " ms to calculate event statistics for airframeNameId: " + 0);

        ResultSet resultSet = preparedStatement.executeQuery();

        //get the event statistics for each airframe
        while (resultSet.next()) {
            startTime = System.currentTimeMillis();
            int airframeNameId = resultSet.getInt(1);
            String airframeName = resultSet.getString(2);
            eventStatistics.add(new EventStatistics(connection, airframeNameId, airframeName, fleetId));
            endTime = System.currentTimeMillis();

            LOG.info("Took " + (endTime - startTime) + " ms to calculate event statistics for airframeNameId: " + airframeNameId);
        }

        resultSet.close();
        preparedStatement.close();

        return eventStatistics;
    }

    /**
     * Gets the number of exceedences for the entire NGAFID between two dates. If either date is null it will
     * select greater than or less than the date specified. If both are null it gets the total amount
     * of exceedences.
     *
     * @param connection is the connection to the database
     * @param startTime is the earliest time to start counting events (it will count from the beginning of time if it is null)
     * @param endTime is the latest time to count events (it will count until the current date if it is null)
     *
     * @return the number of events between the two given times
     */
    public static int getEventCount(Connection connection, LocalDate startTime, LocalDate endTime) throws SQLException {
        return getEventCount(connection, 0, startTime, endTime);
    }


    /**
     * Gets the number of exceedences for a given fleet between two dates. If either is null it will
     * select greater than or less than the date specified. If both are null it gets the total amount
     * of exceedences.
     *
     * @param connection is the connection to the database
     * @param fleetId is the id of the fleet, if fleetId <= 0 it will return for the entire NGAFID
     * @param startTime is the earliest time to start counting events (it will count from the beginning of time if it is null)
     * @param endTime is the latest time to count events (it will count until the current date if it is null)
     *
     * @return the number of events between the two given times
     */
    public static int getEventCount(Connection connection, int fleetId, LocalDate startTime, LocalDate endTime) throws SQLException {
        String query;
        if (fleetId > 0) {
            query = "SELECT count(events.id) FROM events, flights WHERE flights.fleet_id = ? AND events.flight_id = flights.id";
        } else {
            query = "SELECT count(events.id) FROM events, flights WHERE events.flight_id = flights.id";
        }

        if (startTime != null) {
            query += " AND events.end_time >= ?";
        }

        if (endTime != null) {
            query += " AND events.end_time <= ?";
        }

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        int current = 1;
        if (fleetId > 0) {
            preparedStatement.setInt(1, fleetId);
            current = 2;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if (startTime != null) {
            preparedStatement.setString(current, startTime.format(formatter));
            current++;
        }

        if (endTime != null) {
            preparedStatement.setString(current, endTime.format(formatter));
            current++;
        }
        LOG.info(preparedStatement.toString());

        ResultSet resultSet = preparedStatement.executeQuery();

        resultSet.next();
        int count = resultSet.getInt(1);

        resultSet.close();
        preparedStatement.close();

        return count;
    }

    public static class MonthlyEventCounts {
        String airframeName;
        String eventName;

        ArrayList<String> dates = new ArrayList<String>();

        ArrayList<Integer> flightsWithEventCounts = new ArrayList<Integer>();
        ArrayList<Integer> totalFlightsCounts = new ArrayList<Integer>();
        ArrayList<Integer> totalEventsCounts = new ArrayList<Integer>();

        HashMap<String,Integer> flightsWithEventMap = new HashMap<String, Integer>();
        HashMap<String,Integer> totalFlightsMap = new HashMap<String, Integer>();
        HashMap<String,Integer> totalEventsMap = new HashMap<String, Integer>();

        ArrayList<Integer> aggregateFlightsWithEventCounts = new ArrayList<Integer>();
        ArrayList<Integer> aggregateTotalFlightsCounts = new ArrayList<Integer>();
        ArrayList<Integer> aggregateTotalEventsCounts = new ArrayList<Integer>();

        HashMap<String,Integer> aggregateFlightsWithEventMap = new HashMap<String, Integer>();
        HashMap<String,Integer> aggregateTotalFlightsMap = new HashMap<String, Integer>();
        HashMap<String,Integer> aggregateTotalEventsMap = new HashMap<String, Integer>();


        public MonthlyEventCounts(String airframeName, String eventName) {
            this.airframeName = airframeName;
            this.eventName = eventName;
        }

        public void update(String date, Integer flightsWithEvent, Integer totalFlights, Integer totalEvents) {
            Integer flightsWithEventCount = flightsWithEventMap.get(eventName);
            Integer totalFlightsCount = totalFlightsMap.get(eventName);
            Integer totalEventsCount = totalEventsMap.get(eventName);

            /*
            Integer aggregateFlightsWithEventCount = aggregateFlightsWithEventMap.get(eventName);
            Integer aggregateTotalFlightsCount = aggregateTotalFlightsMap.get(eventName);
            Integer aggregateTotalEventsCount = aggregateTotalEventsMap.get(eventName);
            */

            if (flightsWithEventCount == null) flightsWithEventCount = 0;
            if (totalFlightsCount == null) totalFlightsCount = 0;
            if (totalEventsCount == null) totalEventsCount = 0;

            /*
            if (aggregateFlightsWithEventCount == null) aggregateFlightsWithEventCount = 0;
            if (aggregateTotalFlightsCount == null) aggregateTotalFlightsCount = 0;
            if (aggregateTotalEventsCount == null) aggregateTotalEventsCount = 0;
            */

            flightsWithEventCount += flightsWithEvent;
            totalFlightsCount += totalFlights;
            totalEventsCount += totalEvents;

            /*
            aggregateFlightsWithEventCount += flightsWithEvent;
            aggregateTotalFlightsCount += totalFlights;
            aggregateTotalEventsCount += totalEvents;
            */

            flightsWithEventMap.put(date, flightsWithEventCount);
            totalFlightsMap.put(date, totalFlightsCount);
            totalEventsMap.put(date, totalEventsCount);

            /*
            aggregateFlightsWithEventMap.put(date, aggregateFlightsWithEventCount);
            aggregateTotalFlightsMap.put(date, aggregateTotalFlightsCount);
            aggregateTotalEventsMap.put(date, aggregateTotalEventsCount);
            */
        }

        public void updateAggregate(String date, Integer flightsWithEvent, Integer totalFlights, Integer totalEvents) {
            Integer aggregateFlightsWithEventCount = aggregateFlightsWithEventMap.get(date);
            Integer aggregateTotalFlightsCount = aggregateTotalFlightsMap.get(date);
            Integer aggregateTotalEventsCount = aggregateTotalEventsMap.get(date);

            if (aggregateFlightsWithEventCount == null) aggregateFlightsWithEventCount = 0;
            if (aggregateTotalFlightsCount == null) aggregateTotalFlightsCount = 0;
            if (aggregateTotalEventsCount == null) aggregateTotalEventsCount = 0;

            aggregateFlightsWithEventCount += flightsWithEvent;
            aggregateTotalFlightsCount += totalFlights;
            aggregateTotalEventsCount += totalEvents;

            aggregateFlightsWithEventMap.put(date, aggregateFlightsWithEventCount);
            aggregateTotalFlightsMap.put(date, aggregateTotalFlightsCount);
            aggregateTotalEventsMap.put(date, aggregateTotalEventsCount);
        }

        public void setDates(HashMap<String, Integer> eventMap) {
            ArrayList<String> sortedKeys = new ArrayList<String>(eventMap.keySet());;
            Collections.sort(sortedKeys);
            dates.addAll(sortedKeys);
        }

        public void assignLists() {

            for (String date : dates) {

                Integer flightsWithEventCount = flightsWithEventMap.get(date);
                Integer totalFlightsCount = totalFlightsMap.get(date);
                Integer totalEventsCount = totalEventsMap.get(date);

                flightsWithEventCounts.add(flightsWithEventCount);
                totalFlightsCounts.add(totalFlightsCount);
                totalEventsCounts.add(totalEventsCount);

            }

        }

        public void assignAggregateLists() {

            for (String date : dates) {

                Integer aggregateFlightsWithEventCount = aggregateFlightsWithEventMap.get(date);
                Integer aggregateTotalFlightsCount = aggregateTotalFlightsMap.get(date);
                Integer aggregateTotalEventsCount = aggregateTotalEventsMap.get(date);

                aggregateFlightsWithEventCounts.add(aggregateFlightsWithEventCount);
                aggregateTotalFlightsCounts.add(aggregateTotalFlightsCount);
                aggregateTotalEventsCounts.add(aggregateTotalEventsCount);

            }
            //we don't need the map anymore, set it to null to reduce
            //transfer costs when sending this to the webpages
            flightsWithEventMap = null;
            totalFlightsMap = null;
            totalEventsMap = null;
            aggregateFlightsWithEventMap = null;
            aggregateTotalFlightsMap = null;
            aggregateTotalEventsMap = null;
        }
    }



    public static class EventCounts {
        String airframeName;

        ArrayList<String> names = new ArrayList<String>();
        ArrayList<Integer> flightsWithEventCounts = new ArrayList<Integer>();
        ArrayList<Integer> totalFlightsCounts = new ArrayList<Integer>();
        ArrayList<Integer> totalEventsCounts = new ArrayList<Integer>();

        HashMap<String,Integer> flightsWithEventMap = new HashMap<String, Integer>();
        HashMap<String,Integer> totalFlightsMap = new HashMap<String, Integer>();
        HashMap<String,Integer> totalEventsMap = new HashMap<String, Integer>();

        ArrayList<Integer> aggregateFlightsWithEventCounts = new ArrayList<Integer>();
        ArrayList<Integer> aggregateTotalFlightsCounts = new ArrayList<Integer>();
        ArrayList<Integer> aggregateTotalEventsCounts = new ArrayList<Integer>();

        HashMap<String,Integer> aggregateFlightsWithEventMap = new HashMap<String, Integer>();
        HashMap<String,Integer> aggregateTotalFlightsMap = new HashMap<String, Integer>();
        HashMap<String,Integer> aggregateTotalEventsMap = new HashMap<String, Integer>();


        public EventCounts(String airframeName) {
            this.airframeName = airframeName;
        }

        public void initializeEvent(String eventName) {
            flightsWithEventMap.put(eventName, 0);
            totalFlightsMap.put(eventName, 0);
            totalEventsMap.put(eventName, 0);

            aggregateFlightsWithEventMap.put(eventName, 0);
            aggregateTotalFlightsMap.put(eventName, 0);
            aggregateTotalEventsMap.put(eventName, 0);
        }

        public void update(String eventName, Integer flightsWithEvent, Integer totalFlights, Integer totalEvents) {
            Integer flightsWithEventCount = flightsWithEventMap.get(eventName);
            Integer totalFlightsCount = totalFlightsMap.get(eventName);
            Integer totalEventsCount = totalEventsMap.get(eventName);

            /*
            Integer aggregateFlightsWithEventCount = aggregateFlightsWithEventMap.get(eventName);
            Integer aggregateTotalFlightsCount = aggregateTotalFlightsMap.get(eventName);
            Integer aggregateTotalEventsCount = aggregateTotalEventsMap.get(eventName);
            */

            flightsWithEventCount += flightsWithEvent;
            totalFlightsCount += totalFlights;
            totalEventsCount += totalEvents;

            /*
            aggregateFlightsWithEventCount += flightsWithEvent;
            aggregateTotalFlightsCount += totalFlights;
            aggregateTotalEventsCount += totalEvents;
            */

            flightsWithEventMap.put(eventName, flightsWithEventCount);
            totalFlightsMap.put(eventName, totalFlightsCount);
            totalEventsMap.put(eventName, totalEventsCount);

            /*
            aggregateFlightsWithEventMap.put(eventName, aggregateFlightsWithEventCount);
            aggregateTotalFlightsMap.put(eventName, aggregateTotalFlightsCount);
            aggregateTotalEventsMap.put(eventName, aggregateTotalEventsCount);
            */
        }

        public void updateAggregate(String eventName, Integer flightsWithEvent, Integer totalFlights, Integer totalEvents) {
            Integer aggregateFlightsWithEventCount = aggregateFlightsWithEventMap.get(eventName);
            Integer aggregateTotalFlightsCount = aggregateTotalFlightsMap.get(eventName);
            Integer aggregateTotalEventsCount = aggregateTotalEventsMap.get(eventName);

            aggregateFlightsWithEventCount += flightsWithEvent;
            aggregateTotalFlightsCount += totalFlights;
            aggregateTotalEventsCount += totalEvents;

            aggregateFlightsWithEventMap.put(eventName, aggregateFlightsWithEventCount);
            aggregateTotalFlightsMap.put(eventName, aggregateTotalFlightsCount);
            aggregateTotalEventsMap.put(eventName, aggregateTotalEventsCount);
        }


        public void assignLists() {
            ArrayList<String> sortedKeys = new ArrayList<String>(flightsWithEventMap.keySet());
            Collections.sort(sortedKeys, Collections.reverseOrder());

            for (String eventName : sortedKeys) {
                names.add(eventName);

                Integer flightsWithEventCount = flightsWithEventMap.get(eventName);
                Integer totalFlightsCount = totalFlightsMap.get(eventName);
                Integer totalEventsCount = totalEventsMap.get(eventName);

                flightsWithEventCounts.add(flightsWithEventCount);
                totalFlightsCounts.add(totalFlightsCount);
                totalEventsCounts.add(totalEventsCount);

                Integer aggregateFlightsWithEventCount = aggregateFlightsWithEventMap.get(eventName);
                Integer aggregateTotalFlightsCount = aggregateTotalFlightsMap.get(eventName);
                Integer aggregateTotalEventsCount = aggregateTotalEventsMap.get(eventName);

                aggregateFlightsWithEventCounts.add(aggregateFlightsWithEventCount);
                aggregateTotalFlightsCounts.add(aggregateTotalFlightsCount);
                aggregateTotalEventsCounts.add(aggregateTotalEventsCount);
            }

            //we don't need the map anymore, set it to null to reduce
            //transfer costs when sending this to the webpages
            flightsWithEventMap = null;
            totalFlightsMap = null;
            totalEventsMap = null;

            aggregateFlightsWithEventMap = null;
            aggregateTotalFlightsMap = null;
            aggregateTotalEventsMap = null;

        }
    }

    /**
     * Gets the number of exceedences for each type and airframe for the entire NGAFID. It will be organized into a data structure
     * so plotly can display it on the webpage
     *
     * @param connection is the connection to the database
     * @param startTime is the earliest time to start counting events (it will count from the beginning of time if it is null)
     * @param endTime is the latest time to count events (it will count until the current date if it is null)
     */
    public static HashMap<String, EventCounts> getEventCounts(Connection connection, LocalDate startTime, LocalDate endTime) throws SQLException {
        //String query = "SELECT id, airframe FROM airframes INNER JOIN fleet_airframes ON airframes.id = fleet_airframes.airframe_id WHERE fleet_airframes.fleet_id = ? ORDER BY airframe";
        String query = "SELECT id, airframe FROM airframes ORDER BY airframe";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        //preparedStatement.setInt(1, fleetId);

        ArrayList<Integer> airframeNameIds = new ArrayList<Integer>();
        ArrayList<String> airframeNames = new ArrayList<String>();
        //airframeNameIds.add(0);

        ResultSet resultSet = preparedStatement.executeQuery();

        //get the event statistics for each airframe
        while (resultSet.next()) {
            int airframeNameId = resultSet.getInt(1);
            String airframeName = resultSet.getString(2);
            airframeNameIds.add(airframeNameId);
            airframeNames.add(airframeName);
        }

        resultSet.close();
        preparedStatement.close();

        HashMap<String, EventCounts> eventCounts = new HashMap<String, EventCounts>();

        for (int i = 0; i < airframeNameIds.size(); i++) {
            eventCounts.put(airframeNames.get(i), new EventCounts(airframeNames.get(i)));
        }

        for (int i = 0; i < airframeNameIds.size(); i++) {
            int airframeNameId = airframeNameIds.get(i);
            query = "SELECT id, name FROM event_definitions WHERE (airframe_id = ? OR airframe_id = 0) ORDER BY name";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, airframeNameId);

            LOG.info(preparedStatement.toString());

            resultSet = preparedStatement.executeQuery();

            EventCounts eventCount = eventCounts.get(airframeNames.get(i));

            while (resultSet.next()) {
                int definitionId = resultSet.getInt(1);
                String eventName = resultSet.getString(2);

                //LOG.info("initialzing event counts for id: " + definitionId + " and name: '" + eventName + "'");

                eventCount.initializeEvent(eventName);

                query = "SELECT flights_with_event, total_flights, total_events FROM event_statistics WHERE event_statistics.event_definition_id = ? AND airframe_id = ?";

                if (startTime != null) {
                    query += " AND month_first_day >= ?";
                }

                if (endTime != null) {
                    query += " AND month_first_day <= ?";
                }

                PreparedStatement statStatement = connection.prepareStatement(query);
                statStatement.setInt(1, definitionId);
                statStatement.setInt(2, airframeNameId);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                int current = 3;
                if (startTime != null) {
                    statStatement.setString(current, startTime.format(formatter));
                    current++;
                }

                if (endTime != null) {
                    statStatement.setString(current, endTime.format(formatter));
                    current++;
                }
                LOG.info(statStatement.toString());

                ResultSet statSet = statStatement.executeQuery();

                while (statSet.next()) {
                    int flightsWithEvent = statSet.getInt(1);
                    int totalFlights = statSet.getInt(2);
                    int totalEvents = statSet.getInt(3);

                    //LOG.info("event name: '" + eventName + "', statFleetId: " + statFleetId + ", flightsWithEvent: " + flightsWithEvent + ", totalFlights: " + totalFlights + ", totalEvents: " + totalEvents);

                    eventCount.update(eventName, flightsWithEvent, totalFlights, totalEvents);
                    //eventCount.updateAggregate(eventName, flightsWithEvent, totalFlights, totalEvents);
                }

                statSet.close();
                statStatement.close();
            }

            resultSet.close();
            preparedStatement.close();
        }

        for (EventCounts eventCount : eventCounts.values()) {
            eventCount.assignLists();
        }

        return eventCounts;
    }

    /**
     * Gets the number of exceedences for each type and airframe for a fleet. It will be organized into a data structure
     * so plotly can display it on the webpage
     *
     * @param connection is the connection to the database
     * @param fleetId is the id of the fleet, fleetId needs to be > 0 (i.e., a valid fleet id)
     * @param startTime is the earliest time to start counting events (it will count from the beginning of time if it is null)
     * @param endTime is the latest time to count events (it will count until the current date if it is null)
     */
    public static HashMap<String, EventCounts> getEventCounts(Connection connection, int fleetId, LocalDate startTime, LocalDate endTime) throws SQLException {
        //String query = "SELECT id, airframe FROM airframes INNER JOIN fleet_airframes ON airframes.id = fleet_airframes.airframe_id WHERE fleet_airframes.fleet_id = ? ORDER BY airframe";
        String query = "SELECT id, airframe FROM airframes ORDER BY airframe";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        //preparedStatement.setInt(1, fleetId);

        ArrayList<Integer> airframeNameIds = new ArrayList<Integer>();
        ArrayList<String> airframeNames = new ArrayList<String>();
        //airframeNameIds.add(0);

        ResultSet resultSet = preparedStatement.executeQuery();

        //get the event statistics for each airframe
        while (resultSet.next()) {
            int airframeNameId = resultSet.getInt(1);
            String airframeName = resultSet.getString(2);
            airframeNameIds.add(airframeNameId);
            airframeNames.add(airframeName);
        }

        resultSet.close();
        preparedStatement.close();

        HashMap<String, EventCounts> eventCounts = new HashMap<String, EventCounts>();

        for (int i = 0; i < airframeNameIds.size(); i++) {
            eventCounts.put(airframeNames.get(i), new EventCounts(airframeNames.get(i)));
        }

        for (int i = 0; i < airframeNameIds.size(); i++) {
            int airframeNameId = airframeNameIds.get(i);
            query = "SELECT id, name FROM event_definitions WHERE (fleet_id = 0 OR fleet_id = ?) AND (airframe_id = ? OR airframe_id = 0) ORDER BY name";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setInt(2, airframeNameId);

            LOG.info(preparedStatement.toString());

            resultSet = preparedStatement.executeQuery();

            EventCounts eventCount = eventCounts.get(airframeNames.get(i));

            while (resultSet.next()) {
                int definitionId = resultSet.getInt(1);
                String eventName = resultSet.getString(2);

                //LOG.info("initialzing event counts for id: " + definitionId + " and name: '" + eventName + "'");

                eventCount.initializeEvent(eventName);

                query = "SELECT fleet_id, flights_with_event, total_flights, total_events FROM event_statistics WHERE event_statistics.event_definition_id = ? AND airframe_id = ?";

                if (startTime != null) {
                    query += " AND month_first_day >= ?";
                }

                if (endTime != null) {
                    query += " AND month_first_day <= ?";
                }

                PreparedStatement statStatement = connection.prepareStatement(query);
                statStatement.setInt(1, definitionId);
                statStatement.setInt(2, airframeNameId);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                int current = 3;
                if (startTime != null) {
                    statStatement.setString(current, startTime.format(formatter));
                    current++;
                }

                if (endTime != null) {
                    statStatement.setString(current, endTime.format(formatter));
                    current++;
                }
                LOG.info(statStatement.toString());

                ResultSet statSet = statStatement.executeQuery();

                while (statSet.next()) {
                    int statFleetId = statSet.getInt(1);
                    int flightsWithEvent = statSet.getInt(2);
                    int totalFlights = statSet.getInt(3);
                    int totalEvents = statSet.getInt(4);

                    //LOG.info("event name: '" + eventName + "', statFleetId: " + statFleetId + ", flightsWithEvent: " + flightsWithEvent + ", totalFlights: " + totalFlights + ", totalEvents: " + totalEvents);

                    if (statFleetId == fleetId) {
                        eventCount.update(eventName, flightsWithEvent, totalFlights, totalEvents);
                    } else {
                        eventCount.updateAggregate(eventName, flightsWithEvent, totalFlights, totalEvents);
                    }
                }

                statSet.close();
                statStatement.close();
            }

            resultSet.close();
            preparedStatement.close();
        }

        for (EventCounts eventCount : eventCounts.values()) {
            eventCount.assignLists();
        }

        return eventCounts;
    }
    /**
     * Gets the number of exceedences for each type and airframe for a fleet, ordered by months, for a given event name. It will be organized into a data structure
     * so plotly can display it on the webpage
     *
     * @param connection is the connection to the database
     * @param eventName is the name of the event
     * @param startTime is the earliest time to start getting events (it will get events from the beginning of time if it is null)
     * @param endTime is the latest time to getting events (it will get events until the current date if it is null)
     */
    public static HashMap<String, MonthlyEventCounts> getMonthlyEventCounts(Connection connection, String eventName, LocalDate startTime, LocalDate endTime) throws SQLException {
        String query = "SELECT id, airframe FROM airframes ORDER BY airframe";
        PreparedStatement preparedStatement = connection.prepareStatement(query);

        ArrayList<Integer> airframeNameIds = new ArrayList<Integer>();
        ArrayList<String> airframeNames = new ArrayList<String>();

        ResultSet resultSet = preparedStatement.executeQuery();

        //get the event statistics for each airframe
        while (resultSet.next()) {
            int airframeNameId = resultSet.getInt(1);
            String airframeName = resultSet.getString(2);
            airframeNameIds.add(airframeNameId);
            airframeNames.add(airframeName);
        }

        resultSet.close();
        preparedStatement.close();

        HashMap<String, MonthlyEventCounts> eventCounts = new HashMap<String, MonthlyEventCounts>();

        for (int i = 0; i < airframeNameIds.size(); i++) {
            eventCounts.put(airframeNames.get(i), new MonthlyEventCounts(airframeNames.get(i), eventName));
        }

        for (int i = 0; i < airframeNameIds.size(); i++) {
            int airframeNameId = airframeNameIds.get(i);

            query = "SELECT id FROM event_definitions WHERE (airframe_id = ? OR airframe_id = 0) AND name LIKE ? ORDER BY name";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, airframeNameId);
            preparedStatement.setString(2, eventName);

            LOG.info(preparedStatement.toString());

            resultSet = preparedStatement.executeQuery();

            MonthlyEventCounts eventCount = eventCounts.get(airframeNames.get(i));

            if (!resultSet.next()) continue;

            int definitionId = resultSet.getInt(1);

            query = "SELECT fleet_id, flights_with_event, total_flights, total_events, month_first_day FROM event_statistics WHERE event_statistics.event_definition_id = ? AND airframe_id = ?";

            if (startTime != null) {
                query += " AND month_first_day >= ?";
            }

            if (endTime != null) {
                query += " AND month_first_day <= ?";
            }

            query += " ORDER BY month_first_day";

            PreparedStatement statStatement = connection.prepareStatement(query);
            statStatement.setInt(1, definitionId);
            statStatement.setInt(2, airframeNameId);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            int current = 3;
            if (startTime != null) {
                statStatement.setString(current, startTime.format(formatter));
                current++;
            }

            if (endTime != null) {
                statStatement.setString(current, endTime.format(formatter));
                current++;
            }
            LOG.info(statStatement.toString());

            ResultSet statSet = statStatement.executeQuery();

            while (statSet.next()) {
                int statFleetId = statSet.getInt(1);
                int flightsWithEvent = statSet.getInt(2);
                int totalFlights = statSet.getInt(3);
                int totalEvents = statSet.getInt(4);
                String date = statSet.getString(5);
//                LOG.info("event name: '" + eventName + "', statFleetId: " + statFleetId + ", flightsWithEvent: " + flightsWithEvent + ", totalFlights: " + totalFlights + ", totalEvents: " + totalEvents);
                eventCount.updateAggregate(date, flightsWithEvent, totalFlights, totalEvents);

            }

            statSet.close();
            statStatement.close();

            if (resultSet.next()) {
                LOG.severe("Had two event entries for event name: '" + eventName + "', this should never happen.");
                System.exit(1);
            }
            resultSet.close();
            preparedStatement.close();
        }

        for (MonthlyEventCounts eventCount : eventCounts.values()) {

            eventCount.setDates(eventCount.aggregateFlightsWithEventMap);
            eventCount.assignAggregateLists();
            //we don't need the fleetEvent data in aggregate page, set it to null to reduce
            //transfer costs when sending this to the webpages
            eventCount.flightsWithEventCounts = null;
            eventCount.totalFlightsCounts = null;
            eventCount.totalEventsCounts = null;


        }

        return eventCounts;
    }

    /**
     * Gets the number of exceedences for each type and airframe for a fleet, ordered by months, for a given event name. It will be organized into a data structure
     * so plotly can display it on the webpage
     *
     * @param connection is the connection to the database
     * @param fleetId is the id of the fleet
     * @param eventName is the name of the event
     * @param startTime is the earliest time to start getting events (it will get events from the beginning of time if it is null)
     * @param endTime is the latest time to getting events (it will get events until the current date if it is null)
     */
    public static HashMap<String, MonthlyEventCounts> getMonthlyEventCounts(Connection connection, int fleetId, String eventName, LocalDate startTime, LocalDate endTime) throws SQLException {
        String query = "SELECT id, airframe FROM airframes ORDER BY airframe";
        PreparedStatement preparedStatement = connection.prepareStatement(query);

        ArrayList<Integer> airframeNameIds = new ArrayList<Integer>();
        ArrayList<String> airframeNames = new ArrayList<String>();

        ResultSet resultSet = preparedStatement.executeQuery();

        //get the event statistics for each airframe
        while (resultSet.next()) {
            int airframeNameId = resultSet.getInt(1);
            String airframeName = resultSet.getString(2);
            airframeNameIds.add(airframeNameId);
            airframeNames.add(airframeName);
        }

        resultSet.close();
        preparedStatement.close();

        HashMap<String, MonthlyEventCounts> eventCounts = new HashMap<String, MonthlyEventCounts>();

        for (int i = 0; i < airframeNameIds.size(); i++) {
            eventCounts.put(airframeNames.get(i), new MonthlyEventCounts(airframeNames.get(i), eventName));
        }

        for (int i = 0; i < airframeNameIds.size(); i++) {
            int airframeNameId = airframeNameIds.get(i);


            query = "SELECT id FROM event_definitions WHERE (fleet_id = 0 OR fleet_id = ?) AND (airframe_id = ? OR airframe_id = 0) AND name LIKE ? ORDER BY name";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setInt(2, airframeNameId);
            preparedStatement.setString(3, eventName);


            LOG.info(preparedStatement.toString());

            resultSet = preparedStatement.executeQuery();

            MonthlyEventCounts eventCount = eventCounts.get(airframeNames.get(i));

            if (!resultSet.next()) continue;

            int definitionId = resultSet.getInt(1);

            query = "SELECT fleet_id, flights_with_event, total_flights, total_events, month_first_day FROM event_statistics WHERE event_statistics.event_definition_id = ? AND airframe_id = ?";

            if (startTime != null) {
                query += " AND month_first_day >= ?";
            }

            if (endTime != null) {
                query += " AND month_first_day <= ?";
            }

            query += " ORDER BY month_first_day";

            PreparedStatement statStatement = connection.prepareStatement(query);
            statStatement.setInt(1, definitionId);
            statStatement.setInt(2, airframeNameId);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            int current = 3;
            if (startTime != null) {
                statStatement.setString(current, startTime.format(formatter));
                current++;
            }

            if (endTime != null) {
                statStatement.setString(current, endTime.format(formatter));
                current++;
            }
            LOG.info(statStatement.toString());

            ResultSet statSet = statStatement.executeQuery();

            while (statSet.next()) {
                int statFleetId = statSet.getInt(1);
                int flightsWithEvent = statSet.getInt(2);
                int totalFlights = statSet.getInt(3);
                int totalEvents = statSet.getInt(4);
                String date = statSet.getString(5);
//                LOG.info("event name: '" + eventName + "', statFleetId: " + statFleetId + ", flightsWithEvent: " + flightsWithEvent + ", totalFlights: " + totalFlights + ", totalEvents: " + totalEvents);

                if (statFleetId == fleetId) {
                    eventCount.update(date, flightsWithEvent, totalFlights, totalEvents);
                } else {
                    eventCount.updateAggregate(date, flightsWithEvent, totalFlights, totalEvents);
                }

            }

            statSet.close();
            statStatement.close();

            if (resultSet.next()) {
                LOG.severe("Had two event entries for event name: '" + eventName + "', this should never happen.");
                System.exit(1);
            }
            resultSet.close();
            preparedStatement.close();
        }

        for (MonthlyEventCounts eventCount : eventCounts.values()) {
            eventCount.setDates(eventCount.flightsWithEventMap);
            eventCount.assignLists();
            eventCount.assignAggregateLists();

        }

        return eventCounts;
    }

    public static LocalDate getEarliestMonth(Connection connection, int fleetId, int eventDefinitionId) throws SQLException {
        String sql = "SELECT MIN(month_first_day) FROM event_statistics WHERE fleet_id = ? AND event_definition_id = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, fleetId);
        query.setInt(2, eventDefinitionId);

        ResultSet resultSet = query.executeQuery();

        LocalDate earliest = null;
        if (resultSet.next()) {
            Date date = resultSet.getDate(1);

            if (date != null) {
                earliest = date.toLocalDate();
            }
        }

        return earliest;
    }

    public static void clearMonthStatistics(Connection connection, LocalDate month, int fleetId, int eventDefinitionId) throws SQLException {
        String sql = "DELETE FROM event_statistics WHERE month_first_day = ? AND fleet_id = ? AND event_definition_id = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setDate(1, Date.valueOf(month));
        query.setInt(2, fleetId);
        query.setInt(3, eventDefinitionId);

        query.executeUpdate();
    }

    /**
     * Gets the total number of VALID flights in a month. A valid flight is defined as one with non-null RPM data,
     * non-null {@link org.ngafid.flights.DoubleTimeSeries} columns, and non-null date and time {@link org.ngafid.flights.StringTimeSeries} data.
     *
     * @param connection the database connection
     * @param row is the EventStatisticsRow that should already contain the fleetId, event def id and airframe id
     * @param month the current month to get the count for -- this MUST be the first of such month
     * @param tsColumnIds are the ids of the DoubleTimeSeries columns that constitute a valid flight
     *
     * @throws SQLException should there be an issue with the query
     */
    public static void calculateTotalNumberOfFlights(Connection connection, EventStatisticsRow row, LocalDate month, int ... tsColumnIds) throws SQLException {
        final LocalDate nextMonth = month.plusMonths(1);
        String sql = "SELECT id FROM flights WHERE start_time >= ? AND start_time < ? AND fleet_id = ? " + (row.airframeId > 0 ? (" AND airframe_id = " + row.airframeId) : "");

        PreparedStatement query = connection.prepareStatement(sql);

        query.setDate(1, Date.valueOf(month));
        query.setDate(2, Date.valueOf(nextMonth));
        query.setInt(3, row.fleetId);

        //LOG.info("Getting flights count with query:");
        //LOG.info(query.toString());

        int sum = 0;

        ResultSet rs = query.executeQuery();
        while (rs.next()) {
            int flightId = rs.getInt(1);

            if (checkFlight(connection, flightId, row.airframeId, tsColumnIds)) {
                sum++;
            }
        }

        row.totalFlights = sum;
    }

    /**
     * Checks a flight to see if it is valid for calculations and statistics
     *
     * @param connection the database connection
     * @param flightId the flights id to check
     * @param airframeId the airframe id to check
     * @param tsColumnIds the time series column ids that constitute a valid flight (i.e. ones that must not be null)
     *
     * @throws SQLException if there is an issue with the query
     */
    public static boolean checkFlight(Connection connection, int flightId, int airframeId, int ... tsColumnIds) throws SQLException {
        String sql = "SELECT name_id FROM double_series WHERE flight_id = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, flightId);

        ResultSet rs = query.executeQuery();
        List<Integer> results = new ArrayList<>();

        while (rs.next()) {
            results.add(rs.getInt(1));
        }

        for (int id : tsColumnIds) {
            if (!results.contains(id)) {
                return false;
            }
        }

        sql = "SELECT EXISTS (SELECT id FROM string_series WHERE flight_id = ? AND name_id = (SELECT id FROM string_series_names WHERE name = \"Lcl Time\")) AND EXISTS (SELECT id FROM string_series WHERE flight_id = ? AND name_id = (SELECT id FROM string_series_names WHERE name = \"Lcl Date\"))";
        //ensure these are the right date/time names!

        query = connection.prepareStatement(sql);
        query.setInt(1, flightId);
        query.setInt(2, flightId);

        rs = query.executeQuery();

        if (rs.next()) {
            if (!rs.getBoolean(1)) {
                return false;
            }
        }

        return !DoubleTimeSeries.flightHasInvalidRPMData(connection, flightId);
    }

    /**
     * Gets the number of flights with an event
     *
     * @param connection the database connection
     * @param row the EventStatisticsRow that holds this statistics data
     * @param month the month to calculate for
     *
     * @throws SQLException if there is a query issue
     */
    public static void calculateFlightsWithEvent(Connection connection, EventStatisticsRow row, LocalDate month) throws SQLException {
        final LocalDate nextMonth = month.plusMonths(1);

        String sql = "SELECT COUNT(*) FROM flights WHERE id IN (SELECT flight_id FROM events WHERE event_definition_id = ? AND fleet_id = ? AND start_time >= ? AND end_time < ? " + (row.airframeId > 0 ? ("AND airframe_id = " + row.airframeId) : "") + ")";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, row.eventDefinitionId);
        query.setInt(2, row.fleetId);
        query.setDate(3, Date.valueOf(month));
        query.setDate(4, Date.valueOf(nextMonth));

        ResultSet resultSet = query.executeQuery();
        if (resultSet.next()) {
            row.flightsWithEvent = resultSet.getInt(1);
        }
    }

    /**
     * Gets the statistics (severity, duration)
     *
     * @param connection the database connection
     * @param row the EventStatisticsRow that holds this statistics data
     * @param month the month to calculate for
     *
     * @throws SQLException if there is a query issue
     */
    public static void calculateStatistics(Connection connection, EventStatisticsRow row, LocalDate month) throws SQLException {
        final LocalDate nextMonth = month.plusMonths(1);

        String sql = "SELECT MIN(1 + end_line - start_line), MAX(1 + end_line - start_line), SUM(1 + end_line - start_line) FROM events WHERE fleet_id = ? AND event_definition_id = ? AND start_time >= ? AND end_time < ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, row.fleetId);
        query.setInt(2, row.eventDefinitionId);
        query.setDate(3, Date.valueOf(month));
        query.setDate(4, Date.valueOf(nextMonth));

        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            row.durMin = resultSet.getInt(1);
            if (resultSet.wasNull()) {
                row.durMin = MIN_VALUE;
            }

            row.durMax = resultSet.getInt(2);
            if (resultSet.wasNull()) {
                row.durMax = MAX_VALUE;
            }

            // No need to do a null check, default is 0 for NULL in SQL
            row.durSum = resultSet.getInt(3);
        }

        sql = "SELECT MIN(severity), MAX(severity), SUM(severity) FROM events WHERE fleet_id = ? AND event_definition_id = ? AND start_time >= ? AND end_time < ?";
        query = connection.prepareStatement(sql);

        query.setInt(1, row.fleetId);
        query.setInt(2, row.eventDefinitionId);
        query.setDate(3, Date.valueOf(month));
        query.setDate(4, Date.valueOf(nextMonth));

        resultSet = query.executeQuery();

        if (resultSet.next()) {
            row.sevMin = resultSet.getDouble(1);
            if (resultSet.wasNull()) {
                row.sevMin = (double) MIN_VALUE;
            }

            row.sevMax = resultSet.getDouble(2);
            if (resultSet.wasNull()) {
                row.sevMax = (double) MAX_VALUE;
            }

            row.sevSum = resultSet.getDouble(3);
        }

        //System.out.print("FleetID: " + row.fleetId + " defId: " + row.eventDefinitionId + " Month: " + month.toString() + " dur. min,sum,max: " + row.durMin + "\t" + row.durSum + "\t" + row.durMax + " sev. min,sum,max: " + row.sevMin + "\t" + row.sevSum + "\t" + row.sevMax);

    }

    /**
     * Gets the total event count from the database for a given statistic
     *
     * @param connection the database connection
     * @param row the EventStatisticsRow that holds this statistics data
     * @param month the month to calculate for
     *
     * @throws SQLException if there is a query issue
     */
    public static void calculateTotalEventCount(Connection connection, EventStatisticsRow row, LocalDate month) throws SQLException {
        final LocalDate nextMonth = month.plusMonths(1);
        String sql = "SELECT COUNT(*) FROM events WHERE fleet_id = ? AND event_definition_id = ? AND start_time >= ? AND end_time < ?";

        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, row.fleetId);
        query.setInt(2, row.eventDefinitionId);
        query.setDate(3, Date.valueOf(month));
        query.setDate(4, Date.valueOf(nextMonth));

        ResultSet resultSet = query.executeQuery();
        if (resultSet.next()) {
            row.totalEvents = resultSet.getInt(1);
        }
    }

    public static void calculateMonthStatistics(Connection connection, LocalDate month, int fleetId, int airframeId, int eventDefinitionId, int ... tsColumnIds) throws SQLException {
        EventStatisticsRow row = new EventStatisticsRow();

        row.monthFirstDay = Date.valueOf(month);
        row.fleetId = fleetId;
        row.airframeId = airframeId;
        row.eventDefinitionId = eventDefinitionId;

        calculateTotalNumberOfFlights(connection, row, month, tsColumnIds);
        calculateTotalEventCount(connection, row, month);

        if (row.totalFlights > 0) {
            calculateFlightsWithEvent(connection, row, month);
            calculateStatistics(connection, row, month);
            //System.out.print(" eventFlights: " + row.flightsWithEvent + " totalFlights: " + row.totalFlights + " totalEvents: " + row.totalEvents + "\n");

            if (row.airframeId == 0) {
                row.airframeId++;
            }

            row.insert(connection);
        }
        // No point in wasting CPU time...

    }

    public static void main(String [] args) {
        Options options = new Options();

        final LocalDate thisMonth = LocalDate.now();

        CommandLineParser parser = new DefaultParser();

        Option fleetIds = new Option("f", "fleet_ids", true, "fleet id to recalculate for");
        fleetIds.setArgs(Option.UNLIMITED_VALUES);
        fleetIds.setRequired(false);

        Option eventIds = new Option("e", "event_def_ids", true, "list of the event definition ids to clear");
        eventIds.setArgs(Option.UNLIMITED_VALUES);
        eventIds.setRequired(false);

        options.addOption(fleetIds);
        options.addOption(eventIds);

        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("EventStatistics", options);

            System.exit(1);
        }

        Connection connection = Database.getConnection();

        String [] fleetIdStrs = cmd.getOptionValues("f");
        String [] eventIdStrs = cmd.getOptionValues("e");

        List<Fleet> fleets = null;
        List<EventDefinition> eventDefinitions = null;

        try {
            if (fleetIdStrs == null || fleetIdStrs.length == 0) {
                fleets = Fleet.getAllFleets(connection);
            } else {
                fleets = new ArrayList<>();
                for (String fleetId : fleetIdStrs) {
                    int id = Integer.parseInt(fleetId);
                    fleets.add(Fleet.get(connection, id));
                }
            }

            if (eventIdStrs == null || eventIdStrs.length == 0) {
                eventDefinitions = EventDefinition.getAll(connection);
            } else {
                eventDefinitions = new ArrayList<>();
                for (String eventId : eventIdStrs) {
                    int id = Integer.parseInt(eventId);
                    eventDefinitions.add(EventDefinition.getEventDefinition(connection, id));
                }
            }

            StringBuilder sb = new StringBuilder();
            int nFleets = fleets.size();
            for (int i = 0; i < nFleets; i++) {
                Fleet fleet = fleets.get(i);
                sb.append(fleet.getName());
                sb.append(i < nFleets - 1 ? ", " : ";");
            }

            LOG.info("Clearing event statistics for fleets: " + sb.toString() + " and event definitions: " + Arrays.toString(eventIdStrs));

            for (EventDefinition def : eventDefinitions) {
                int [] columnIds = DoubleTimeSeries.getDoubleSeriesColumnIds(connection, def);

                int defId = def.getId();
                for (Fleet fleet : fleets) {
                    int fleetId = fleet.getId();

                    LocalDate month = getEarliestMonth(connection, fleetId, defId);
                    while (month != null) {
                        clearMonthStatistics(connection, month, fleetId, defId);

                        calculateMonthStatistics(connection, month, fleetId, def.getAirframeNameId(), defId, columnIds);

                        month = month.plusMonths(1);

                        if (month.compareTo(thisMonth) > 0) {
                            month = null;
                        }
                    }
                }
            }

            LOG.info("Finished!");
            System.exit(0);
        } catch (SQLException se) {
            se.printStackTrace();
            System.exit(1);
        }

    }
}

