package org.ngafid.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Calendar;

import java.util.logging.Logger;

public class EventStatistics {
    private static final Logger LOG = Logger.getLogger(EventStatistics.class.getName());

    private static class EventRow {
        String rowName;

        int flightsWithoutError;
        int flightsWithEvent;
        int totalEvents;
        double avgEvents;
        double avgDuration;
        double minDuration;
        double maxDuration;
        double avgSeverity;
        double minSeverity;
        double maxSeverity;

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

        public static EventRow getStatistics(Connection connection, String rowName, int fleetId, int eventId, String extraQuery, int[] extraParams) throws SQLException {
            EventRow eventRow = new EventRow(rowName);

            String query = "SELECT count(*), min(min_severity), max(max_severity), min(min_duration), max(max_duration) FROM flight_processed INNER JOIN flights ON flights.id = flight_processed.flight_id WHERE flight_processed.fleet_id = ? AND flight_processed.event_definition_id = ? AND flight_processed.had_error = 0";
            if (extraQuery != "") {
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
            resultSet.next();

            eventRow.flightsWithoutError = resultSet.getInt(1);
            eventRow.minSeverity = resultSet.getDouble(2);
            eventRow.maxSeverity = resultSet.getDouble(3);
            eventRow.minDuration = resultSet.getDouble(4);
            eventRow.maxDuration = resultSet.getDouble(5);

            preparedStatement.close();

            query = "SELECT count(*), sum(count), sum(sum_duration), sum(sum_severity) FROM flight_processed INNER JOIN flights ON flights.id = flight_processed.flight_id WHERE flight_processed.fleet_id = ? AND flight_processed.event_definition_id = ? AND flight_processed.had_error = 0 and count > 0";
            if (extraQuery != "") {
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
            resultSet.next();

            eventRow.flightsWithEvent = resultSet.getInt(1);
            eventRow.totalEvents = resultSet.getInt(2);
            eventRow.avgDuration = resultSet.getDouble(3) / eventRow.totalEvents;
            eventRow.avgSeverity = resultSet.getDouble(4) / eventRow.totalEvents;
            eventRow.avgEvents = (double)eventRow.totalEvents / (double)eventRow.flightsWithEvent;

            if (Double.isNaN(eventRow.avgDuration)) eventRow.avgDuration = 0;
            if (Double.isNaN(eventRow.avgSeverity)) eventRow.avgSeverity = 0;
            if (Double.isNaN(eventRow.avgEvents)) eventRow.avgEvents = 0;

            preparedStatement.close();

            //get aggregate statistics for all other fleets
            query = "SELECT count(*), min(min_severity), max(max_severity), min(min_duration), max(max_duration) FROM flight_processed INNER JOIN flights ON flights.id = flight_processed.flight_id WHERE flight_processed.fleet_id != ? AND flight_processed.event_definition_id = ? AND flight_processed.had_error = 0";
            if (extraQuery != "") {
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
            resultSet.next();

            eventRow.aggFlightsWithoutError = resultSet.getInt(1);
            eventRow.aggMinSeverity = resultSet.getDouble(2);
            eventRow.aggMaxSeverity = resultSet.getDouble(3);
            eventRow.aggMinDuration = resultSet.getDouble(4);
            eventRow.aggMaxDuration = resultSet.getDouble(5);

            preparedStatement.close();

            query = "SELECT count(*), sum(count), sum(sum_duration), sum(sum_severity) FROM flight_processed INNER JOIN flights ON flights.id = flight_processed.flight_id WHERE flight_processed.fleet_id != ? AND flight_processed.event_definition_id = ? AND flight_processed.had_error = 0 and count > 0";
            if (extraQuery != "") {
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
            resultSet.next();

            eventRow.aggFlightsWithEvent = resultSet.getInt(1);
            eventRow.aggTotalEvents = resultSet.getInt(2);
            eventRow.aggAvgDuration = resultSet.getDouble(3) / eventRow.aggTotalEvents;
            eventRow.aggAvgSeverity = resultSet.getDouble(4) / eventRow.aggTotalEvents;
            eventRow.aggAvgEvents = (double)eventRow.aggTotalEvents / (double)eventRow.aggFlightsWithEvent;

            if (Double.isNaN(eventRow.aggAvgDuration)) eventRow.aggAvgDuration = 0;
            if (Double.isNaN(eventRow.aggAvgEvents)) eventRow.aggAvgEvents = 0;
            if (Double.isNaN(eventRow.aggAvgSeverity)) eventRow.aggAvgSeverity = 0;

            preparedStatement.close();

            return eventRow;
        }
    }

    private static class AirframeStatistics {
        int eventId;
        String eventName;

        int totalFlights;
        int processedFlights;

        ArrayList<EventRow> monthStats = new ArrayList<EventRow>();

        AirframeStatistics(Connection connection, int eventId, String eventName, int fleetId) throws SQLException {
            this.eventId = eventId;
            this.eventName = eventName;

            String query = "SELECT count(*) FROM flights WHERE fleet_id = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, fleetId);

            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();

            totalFlights = resultSet.getInt(1);

            preparedStatement.close();

            //get number flights processed 
            query = "SELECT count(*) FROM flight_processed WHERE fleet_id = ? AND event_definition_id = ?";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setInt(2, eventId);

            resultSet = preparedStatement.executeQuery();
            resultSet.next();

            processedFlights = resultSet.getInt(1);

            preparedStatement.close();

            int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);

            //LOG.info("current year: " + currentYear + ", current month: " + currentMonth);

            monthStats.add(EventRow.getStatistics(connection, "Month to Date", fleetId, eventId, "YEAR(start_time) >= ? AND MONTH(start_time) >= ?", new int[]{currentYear, currentMonth} ));

            int previousMonth = currentMonth - 1;
            int tempYear = currentYear;
            if (currentMonth == 0) {
                currentMonth = 12;
                tempYear = currentYear - 1;
            }

            monthStats.add(EventRow.getStatistics(connection, "Previous Month", fleetId, eventId, "YEAR(start_time) = ? AND MONTH(start_time) = ?", new int[]{currentYear, currentMonth} ));

            monthStats.add(EventRow.getStatistics(connection, "Year to Date", fleetId, eventId, "YEAR(start_time) >= ? AND MONTH(start_time) >= ?", new int[]{currentYear, 1} ));

            monthStats.add(EventRow.getStatistics(connection, "Previous Year", fleetId, eventId, "YEAR(start_time) = ?", new int[]{currentYear - 1} ));

            monthStats.add(EventRow.getStatistics(connection, "Overall", fleetId, eventId, "", new int[]{}));
        }

    }

    int airframeId;
    String airframeName;
    ArrayList<AirframeStatistics> events;

    public EventStatistics(Connection connection, int airframeId, String airframeName, int fleetId) throws SQLException {
        this.airframeId = airframeId;
        this.airframeName = airframeName;
        events = new ArrayList<>();

        String query = "SELECT id, name FROM event_definitions WHERE airframe_id = ? AND (fleet_id = 0 OR fleet_id = ?)";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, airframeId);
        preparedStatement.setInt(2, fleetId);

        ResultSet resultSet = preparedStatement.executeQuery();

        events = new ArrayList<>();
        while (resultSet.next()) {
            int eventId = resultSet.getInt(1);
            String eventName = resultSet.getString(2);

            events.add(new AirframeStatistics(connection, eventId, eventName, fleetId));
        }

        preparedStatement.close();
    }

    public static ArrayList<EventStatistics> getAll(Connection connection, int fleetId) throws SQLException {
        String query = "SELECT id, airframe FROM airframes WHERE fleet_id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);

        ArrayList<EventStatistics> eventStatistics = new ArrayList<>();
        eventStatistics.add(new EventStatistics(connection, 0, "Generic", fleetId));

        ResultSet resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
            int airframeId = resultSet.getInt(1);
            String airframeName = resultSet.getString(2);
            eventStatistics.add(new EventStatistics(connection, airframeId, airframeName, fleetId));
        }

        preparedStatement.close();

        return eventStatistics;
    }

}

