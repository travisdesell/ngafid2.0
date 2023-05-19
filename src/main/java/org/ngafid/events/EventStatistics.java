package org.ngafid.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;

public class EventStatistics {
    private static final Logger LOG = Logger.getLogger(EventStatistics.class.getName());

    public static String getFirstOfMonth(String dateTime) {
        return dateTime.substring(0, 8) + "01";
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

    public static void updateFlightsWithEvent(Connection connection, int fleetId, int airframeNameId, int eventId, String startDateTime) throws SQLException {
        //cannot update event statistics if the flight had no startDateTime
        if (startDateTime == null) return;

        String firstOfMonth = getFirstOfMonth(startDateTime);

        String query = "INSERT INTO event_statistics (fleet_id, airframe_id, event_definition_id, month_first_day, flights_with_event, total_flights, min_severity, sum_severity, max_severity, min_duration, sum_duration, max_duration) VALUES (?, ?, ?, ?, 1, 1, 999999, 0, -999999, 999999, 0, -999999) ON DUPLICATE KEY UPDATE flights_with_event = flights_with_event + 1, total_flights = total_flights + 1";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);
        preparedStatement.setInt(2, airframeNameId);
        preparedStatement.setInt(3, eventId);
        preparedStatement.setString(4, firstOfMonth);

        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static void updateFlightsWithoutEvent(Connection connection, int fleetId, int airframeNameId, int eventId, String startDateTime) throws SQLException {
        //cannot update event statistics if the flight had no startDateTime
        if (startDateTime == null) return;

        String firstOfMonth = getFirstOfMonth(startDateTime);

        String query = "INSERT INTO event_statistics (fleet_id, airframe_id, event_definition_id, month_first_day, flights_with_event, total_flights, min_severity, sum_severity, max_severity, min_duration, sum_duration, max_duration) VALUES (?, ?, ?, ?, 0, 1, 999999, 0, -999999, 999999, 0, -999999) ON DUPLICATE KEY UPDATE total_flights = total_flights + 1";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);
        preparedStatement.setInt(2, airframeNameId);
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

    }

    int airframeNameId;
    String airframeName;
    ArrayList<AirframeStatistics> events;

    public EventStatistics(Connection connection, int airframeNameId, String airframeName, int fleetId) throws SQLException {
        this.airframeNameId = airframeNameId;
        this.airframeName = airframeName;
        events = new ArrayList<>();

        ArrayList<EventDefinition> eventDefinitions = EventDefinition.getAll(connection, "airframe_id = ? AND  (fleet_id = 0 OR fleet_id = ?)", new Object[]{airframeNameId,  fleetId});

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


        public void assignLists() {
            ArrayList<String> sortedKeys = new ArrayList<String>(flightsWithEventMap.keySet());;
            Collections.sort(sortedKeys);

            for (String date : sortedKeys) {
                dates.add(date);

                Integer flightsWithEventCount = flightsWithEventMap.get(date);
                Integer totalFlightsCount = totalFlightsMap.get(date);
                Integer totalEventsCount = totalEventsMap.get(date);

                flightsWithEventCounts.add(flightsWithEventCount);
                totalFlightsCounts.add(totalFlightsCount);
                totalEventsCounts.add(totalEventsCount);

            }

        }

        public void assignAggregateLists() {
            ArrayList<String> sortedKeys = new ArrayList<String>(aggregateFlightsWithEventMap.keySet());
            Collections.sort(sortedKeys);
            for (String date : sortedKeys) {
                dates.add(date);
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
     * @param fleetId is the id of the fleet, if null get data for all fleets
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
            eventCount.assignLists();
            eventCount.assignAggregateLists();
        }

        return eventCounts;
    }
}

