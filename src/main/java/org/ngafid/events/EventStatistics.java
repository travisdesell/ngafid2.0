package org.ngafid.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.Logger;

public class EventStatistics {
    private static final Logger LOG = Logger.getLogger(EventStatistics.class.getName());

    public static String getFirstOfMonth(String dateTime) {
        return dateTime.substring(0, 8) + "01";
    }

    public static void updateEventStatistics(Connection connection, int fleetId, int airframeId, int eventId, String startDateTime, double severity, double duration) throws SQLException {
        String firstOfMonth = getFirstOfMonth(startDateTime);

        String query = "INSERT INTO event_statistics (fleet_id, airframe_id, event_definition_id, month_first_day, total_events, min_severity, sum_severity, max_severity, min_duration, sum_duration, max_duration) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE min_severity = LEAST(min_severity, ?), sum_severity = sum_severity + ?, max_severity = GREATEST(max_severity, ?), min_duration = LEAST(min_duration, ?), sum_duration = sum_duration + ?, max_duration = GREATEST(max_duration, ?), total_events = total_events + 1";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);
        preparedStatement.setInt(2, airframeId);
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

    public static void updateFlightsWithEvent(Connection connection, int fleetId, int airframeId, int eventId, String startDateTime) throws SQLException {
        String firstOfMonth = getFirstOfMonth(startDateTime);

        String query = "INSERT INTO event_statistics (fleet_id, airframe_id, event_definition_id, month_first_day, flights_with_event, total_flights, min_severity, sum_severity, max_severity, min_duration, sum_duration, max_duration) VALUES (?, ?, ?, ?, 1, 1, 999999, 0, -999999, 999999, 0, -999999) ON DUPLICATE KEY UPDATE flights_with_event = flights_with_event + 1, total_flights = total_flights + 1";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);
        preparedStatement.setInt(2, airframeId);
        preparedStatement.setInt(3, eventId);
        preparedStatement.setString(4, firstOfMonth);

        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static void updateFlightsWithoutEvent(Connection connection, int fleetId, int airframeId, int eventId, String startDateTime) throws SQLException {
        String firstOfMonth = getFirstOfMonth(startDateTime);

        String query = "INSERT INTO event_statistics (fleet_id, airframe_id, event_definition_id, month_first_day, flights_with_event, total_flights, min_severity, sum_severity, max_severity, min_duration, sum_duration, max_duration) VALUES (?, ?, ?, ?, 0, 1, 999999, 0, -999999, 999999, 0, -999999) ON DUPLICATE KEY UPDATE total_flights = total_flights + 1";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);
        preparedStatement.setInt(2, airframeId);
        preparedStatement.setInt(3, eventId);
        preparedStatement.setString(4, firstOfMonth);

        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }


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

    private static class AirframeStatistics {
        int eventId;
        String eventName;

        int totalFlights;
        int processedFlights;

        String humanReadable;

        ArrayList<EventRow> monthStats = new ArrayList<EventRow>();

        AirframeStatistics(Connection connection, EventDefinition eventDefinition, int fleetId) throws SQLException {
            this.eventId = eventDefinition.getId();
            this.eventName = eventDefinition.getName();
            this.humanReadable = eventDefinition.toHumanReadable();

            int airframeId = eventDefinition.getAirframeId();

            String query;
            PreparedStatement preparedStatement;
            ResultSet resultSet;
            if (airframeId == 0) {
                query = "SELECT count(*) FROM flights WHERE fleet_id = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, fleetId);

                resultSet = preparedStatement.executeQuery();
            } else {
                query = "SELECT count(*) FROM flights WHERE fleet_id = ? AND airframe_id = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, fleetId);
                preparedStatement.setInt(2, airframeId);

                resultSet = preparedStatement.executeQuery();
            } 
            resultSet.next();

            totalFlights = resultSet.getInt(1);

            resultSet.close();
            preparedStatement.close();

            //get number flights processed 
            if (airframeId == 0) {
                query = "SELECT count(*) FROM flight_processed WHERE flight_processed.fleet_id = ? AND flight_processed.event_definition_id = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, fleetId);
                preparedStatement.setInt(2, eventId);

            } else {
                query = "SELECT count(*) FROM flight_processed INNER JOIN flights ON flights.airframe_id = ? AND flights.id = flight_processed.flight_id WHERE flight_processed.fleet_id = ? AND flight_processed.event_definition_id = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, airframeId);
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

    }

    int airframeId;
    String airframeName;
    ArrayList<AirframeStatistics> events;

    public EventStatistics(Connection connection, int airframeId, String airframeName, int fleetId) throws SQLException {
        this.airframeId = airframeId;
        this.airframeName = airframeName;
        events = new ArrayList<>();

        ArrayList<EventDefinition> eventDefinitions = EventDefinition.getAll(connection, "airframe_id = ? AND  (fleet_id = 0 OR fleet_id = ?)", new Object[]{airframeId,  fleetId});

        events = new ArrayList<>();
        for (int i = 0; i < eventDefinitions.size(); i++) {

            events.add(new AirframeStatistics(connection, eventDefinitions.get(i), fleetId));
        }
    }

    public static ArrayList<EventStatistics> getAll(Connection connection, int fleetId) throws SQLException {
        String query = "SELECT id, airframe FROM airframes INNER JOIN fleet_airframes ON airframes.id = fleet_airframes.airframe_id WHERE fleet_airframes.fleet_id = ? ORDER BY airframe";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);

        ArrayList<EventStatistics> eventStatistics = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        eventStatistics.add(new EventStatistics(connection, 0, "Generic", fleetId));
        long endTime = System.currentTimeMillis();
        LOG.info("Took " + (endTime - startTime) + " ms to calculate event statistics for airframeId: " + 0);

        ResultSet resultSet = preparedStatement.executeQuery();

        //get the event statistics for each airframe
        while (resultSet.next()) {
            startTime = System.currentTimeMillis();
            int airframeId = resultSet.getInt(1);
            String airframeName = resultSet.getString(2);
            eventStatistics.add(new EventStatistics(connection, airframeId, airframeName, fleetId));
            endTime = System.currentTimeMillis();

            LOG.info("Took " + (endTime - startTime) + " ms to calculate event statistics for airframeId: " + airframeId);
        }

        resultSet.close();
        preparedStatement.close();

        return eventStatistics;
    }

    /**
     * Gets the number of exceedences for a given fleet between two dates. If either is null it will
     * select greater than or less than the date specified. If both are null it gets the total amount
     * of exceedences.
     *
     * @param connection is the connection to the database
     * @param fleetId is the id of the fleet
     * @param startTime is the earliest time to start counting events (it will count from the beginning of time if it is null)
     * @param endTime is the latest time to count events (it will count until the current date if it is null)
     *
     * @return the number of events between the two given times
     */
    public static int getEventCount(Connection connection, int fleetId, LocalDate startTime, LocalDate endTime) throws SQLException {
        String query = "SELECT count(events.id) FROM events, flights WHERE flights.fleet_id = ? AND events.flight_id = flights.id";

        if (startTime != null) {
            query += " AND events.end_time >= ?";
        }

        if (endTime != null) {
            query += " AND events.end_time <= ?";
        }

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        int current = 2;
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


    public static class EventCounts {
        String airframeName;

        ArrayList<String> names = new ArrayList<String>();
        ArrayList<Integer> flightsWithEventCounts = new ArrayList<Integer>();
        ArrayList<Integer> totalFlightsCounts = new ArrayList<Integer>();
        ArrayList<Integer> totalEventsCounts = new ArrayList<Integer>();

        //ArrayList<String> hovertext = new ArrayList<String>();

        HashMap<String,Integer> flightsWithEventMap = new HashMap<String, Integer>();
        HashMap<String,Integer> totalFlightsMap = new HashMap<String, Integer>();
        HashMap<String,Integer> totalEventsMap = new HashMap<String, Integer>();

        public EventCounts(String airframeName) {
            this.airframeName = airframeName;
        }

        public void update(String eventName, Integer flightsWithEvent, Integer totalFlights, Integer totalEvents) {
            Integer flightsWithEventCount = flightsWithEventMap.get(eventName);
            Integer totalFlightsCount = totalFlightsMap.get(eventName);
            Integer totalEventsCount = totalEventsMap.get(eventName);

            if (flightsWithEventCount == null) {
                //if one is null they'll all be null
                flightsWithEventCount = flightsWithEvent;
                totalFlightsCount = totalFlights;
                totalEventsCount = totalEvents;
            } else {
                flightsWithEventCount += flightsWithEvent;
                totalFlightsCount += totalFlights;
                totalEventsCount += totalEvents;
            }

            flightsWithEventMap.put(eventName, flightsWithEventCount);
            totalFlightsMap.put(eventName, totalFlightsCount);
            totalEventsMap.put(eventName, totalEventsCount);
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

                //hovertext.add(eventName + " hover!");
            }

            //we don't need the map anymore, set it to null to reduce
            //transfer costs when sending this to the webpages
            flightsWithEventMap = null;
            totalFlightsMap = null;
            totalEventsMap = null;
        }
    }

    /**
     * Gets the number of exceedences for each type and airframe for a fleet. It will be organized into a data structure
     * so plotly can display it on the webpage
     *
     * @param connection is the connection to the database
     * @param fleetId is the id of the fleet
     * @param startTime is the earliest time to start counting events (it will count from the beginning of time if it is null)
     * @param endTime is the latest time to count events (it will count until the current date if it is null)
     */
    public static HashMap<String, EventCounts> getEventCounts(Connection connection, int fleetId, LocalDate startTime, LocalDate endTime) throws SQLException {
        String query = "SELECT event_definitions.name, airframes.airframe, event_statistics.flights_with_event, event_statistics.total_flights, event_statistics.total_events FROM event_definitions, event_statistics, airframes WHERE event_statistics.fleet_id = ? AND event_statistics.event_definition_id = event_definitions.id AND airframes.id = event_statistics.airframe_id";

        if (startTime != null) {
            query += " AND event_statistics.month_first_day >= ?";
        }

        if (endTime != null) {
            query += " AND event_statistics.month_first_day <= ?";
        }

        query += " ORDER BY event_definitions.name";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        int current = 2;
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

        HashMap<String, EventCounts> eventCounts = new HashMap<String, EventCounts>();

        //ArrayList<EventDefinition> eventDefinitions = EventDefinition.getAll(connection, "airframe_id = ? AND  (fleet_id = 0 OR fleet_id = ?)", new Object[]{airframeId,  fleetId});

        while (resultSet.next()) {
            String eventName = resultSet.getString(1);
            String airframe = resultSet.getString(2);
            int flightsWithEvent = resultSet.getInt(3);
            int totalFlights = resultSet.getInt(4);
            int totalEvents = resultSet.getInt(5);

            LOG.info("event name: '" + eventName + "', flightsWithEvent: " + flightsWithEvent + ", totalFlights: " + totalFlights + ", totalEvents: " + totalEvents);

            EventCounts eventCount = eventCounts.get(airframe);
            EventCounts fleetEventCount = eventCounts.get("fleet"); //will store the sum of all the fleet's events
            if (eventCount == null) {
                eventCount = new EventCounts(airframe);
                eventCount.update(eventName, flightsWithEvent, totalFlights, totalEvents);
                eventCounts.put(airframe, eventCount);

                fleetEventCount = new EventCounts("fleet");
                fleetEventCount.update(eventName, flightsWithEvent, totalFlights, totalEvents);
                eventCounts.put("fleet", fleetEventCount);
            } else {
                eventCount.update(eventName, flightsWithEvent, totalFlights, totalEvents);
                fleetEventCount.update(eventName, flightsWithEvent, totalFlights, totalEvents);
            }
        }

        resultSet.close();
        preparedStatement.close();


        //now do the same except for every fleet
        query = "SELECT event_definitions.name, airframes.airframe, event_statistics.flights_with_event, event_statistics.total_flights, event_statistics.total_events, event_statistics.fleet_id FROM event_definitions, event_statistics, airframes WHERE event_statistics.event_definition_id = event_definitions.id AND airframes.id = event_statistics.airframe_id";

        if (startTime != null) {
            query += " AND event_statistics.month_first_day >= ?";
        }

        if (endTime != null) {
            query += " AND event_statistics.month_first_day <= ?";
        }

        query += " ORDER BY event_definitions.name";

        preparedStatement = connection.prepareStatement(query);

        current = 1;
        if (startTime != null) {
            preparedStatement.setString(current, startTime.format(formatter));
            current++;
        }

        if (endTime != null) {
            preparedStatement.setString(current, endTime.format(formatter));
            current++;
        }
        LOG.info(preparedStatement.toString());

        resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
            String eventName = resultSet.getString(1);
            String airframe = resultSet.getString(2);
            int flightsWithEvent = resultSet.getInt(3);
            int totalFlights = resultSet.getInt(4);
            int totalEvents = resultSet.getInt(5);
            int rowFleetId = resultSet.getInt(6);

            LOG.info("event name: '" + eventName + "', flightsWithEvent: " + flightsWithEvent + ", totalFlights: " + totalFlights + ", totalEvents: " + totalEvents + ", rowFleetId: " + rowFleetId);

            EventCounts allEventCount = eventCounts.get("all"); //will store the sum of all the fleet's events
            if (allEventCount == null) {
                allEventCount = new EventCounts("all");
                allEventCount.update(eventName, flightsWithEvent, totalFlights, totalEvents);
                eventCounts.put("all", allEventCount);
            } else {
                allEventCount.update(eventName, flightsWithEvent, totalFlights, totalEvents);
            }
        }

        resultSet.close();
        preparedStatement.close();

        for (EventCounts eventCount : eventCounts.values()) {
            eventCount.assignLists();
        }

        return eventCounts;
    }

}

