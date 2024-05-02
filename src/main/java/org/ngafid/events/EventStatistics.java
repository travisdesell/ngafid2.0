package org.ngafid.events;

import org.ngafid.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;

public class EventStatistics {
    private static final Logger LOG = Logger.getLogger(EventStatistics.class.getName());

    // Fleet ID - Airframe - Month - Total Flights
    private static Map<Integer, Map<String, Map<String, Integer>>> monthlyTotalFlightsMap = new HashMap<>();

    static {
        Connection connection = Database.getConnection();
        // try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM fleet")) {
        //     ResultSet result = ps.executeQuery();
        //     while (result.next()) {
        //         int fleetId = result.getInt("id");
        //         updateMonthlyTotalFlights(connection, fleetId);
        //     }
        // } catch (SQLException e) {
        //     throw new RuntimeException(e);
        // }
    }

    public static class EventCount {
        public final EventDefinition eventDefinition;
        public final int eventCount;
        public final int flightCount;

        public EventCount(EventDefinition eventDefinition, int eventCount, int flightCount) {
            this.eventDefinition = eventDefinition;
            this.eventCount = eventCount;
            this.flightCount = flightCount;
        }

        public String toString() {
            return "EventCount(eventCount=" + eventCount + ", flightCount=" + flightCount + ")";
        }
    }

    public static class AirframeEventCount {
        public final int fleetId, eventId, airframeId;

        public AirframeEventCount(int fleetId, int eventId, int airframeId) {
            this.fleetId = fleetId;
            this.eventId = eventId;
            this.airframeId = airframeId;
        }

        public int hashCode() {
            return eventId ^ airframeId ^ fleetId;
        }

        public boolean equals(Object obj) {
            if (obj instanceof AirframeEventCount) {
                AirframeEventCount other = (AirframeEventCount) obj;
                return eventId == other.eventId && airframeId == other.airframeId;
            } else {
                return false;
            }
        }

        public String toString() {
            return "AirframeEventCount(fleetId=" + fleetId + ", eventId=" + eventId + ", airframeId=" + airframeId + ")";
        }
    }

    public static Map<AirframeEventCount, EventCount> getEventCountsFast(Connection connection, String startDate, String endDate) throws SQLException {
        if (startDate == null)
            startDate = LocalDate.of(0, 1, 1).toString();

        if (endDate == null)
            endDate = LocalDate.now().toString();

        String dateClause = "start_time BETWEEN ? AND ? ";
        // String fleetClause = fleetId == -1 ? "" : "AND e.fleet_id = " + fleetId; 

        String query =  "SELECT COUNT(DISTINCT e.id) AS event_count, COUNT(DISTINCT flights.id) as flight_count, event_definition_id, flights.fleet_id as fleet_id, flights.airframe_id as airframe_id FROM events AS e" 
                        + " INNER JOIN flights ON flights.id = e.flight_id"
                        + " WHERE e.start_time BETWEEN ? AND ? " + " GROUP BY event_definition_id, flights.airframe_id, flights.fleet_id";

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, startDate);
        statement.setString(2, endDate);

        ResultSet result = statement.executeQuery();

        Map<AirframeEventCount, EventCount> out = new HashMap<>();

        while (result.next()) {
            int fleet = result.getInt("fleet_id");
            int count = result.getInt("event_count");
            int flightCount = result.getInt("flight_count");
            int eventDefinitionId = result.getInt("event_definition_id");
            int airframeId = result.getInt("airframe_id");
            EventDefinition ed = EventDefinition.getEventDefinition(connection, eventDefinitionId);
            out.put(new AirframeEventCount(fleet, eventDefinitionId, airframeId), new EventCount(ed, count, flightCount));
        }

        result.close();
        statement.close();

        return out;
    }

    public static class FlightCounts {
        
        // Maps airframeId to another map, which maps fleetId to the number of flights in that fleet.
        private Map<Integer, Map<Integer, Integer>> airframeToFleetCounts = new HashMap<>();

        // Maps fleetId to another map, which maps airframeId to the number of flights of that airframe type in the specified fleet.
        private Map<Integer, Map<Integer, Integer>> fleetToAirframeCounts = new HashMap<>();

        // Total flight counts for all fleets. Maps airframe to the number of flights with that airframe.
        private Map<Integer, Integer> aggregateCounts = new HashMap<>();

        public FlightCounts(ResultSet results) throws SQLException {
            while (results.next()) {
                int airframeId = results.getInt("airframe_id");
                int fleetId = results.getInt("fleet_id");
                int flightCount = results.getInt("flight_count");

                this.addRow(airframeId, fleetId, flightCount);
            }
        }

        private void addRow(int airframeId, int fleetId, int flightCount) {
            airframeToFleetCounts
                .computeIfAbsent(airframeId, k -> new HashMap<>())
                .compute(fleetId, (k, v) -> (v == null ? 0 : v) + flightCount);

            fleetToAirframeCounts
                .computeIfAbsent(fleetId, k -> new HashMap<>())
                .compute(airframeId, (k, v) -> (v == null ? 0 : v) + flightCount);

            aggregateCounts
                .compute(airframeId, (k, v) -> (v == null ? 0 : v) + flightCount);
        }

        public Map<Integer, Integer> getAggregateCounts() {
            return aggregateCounts;
        }

        public Map<Integer, Integer> getFleetCounts(int fleetId) {
            return fleetToAirframeCounts.get(fleetId);
        }
    }

    /**
     * Returns a map of airframe id to the total number of flights with that id, one map per fleet id.
     **/
    public static FlightCounts getFlightCounts(Connection connection, String startDate, String endDate) throws SQLException {
        if (startDate == null)
            startDate = LocalDate.of(0, 1, 1).toString();

        if (endDate == null)
            endDate = LocalDate.now().toString();


        Map<Integer, Map<Integer, Integer>> out = new HashMap<>();

        String query = "SELECT COUNT(DISTINCT id) as flight_count, airframe_id, fleet_id FROM flights WHERE start_time BETWEEN ? AND ? GROUP BY flights.airframe_id, flights.fleet_id ";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, startDate);
        ps.setString(2, endDate);

        ResultSet results = ps.executeQuery();
        
        FlightCounts counts = new FlightCounts(results);

        results.close();
        ps.close();

        return counts;
    }

    public static void updateMonthlyTotalFlights(Connection connection, int fleetId) {
        String query = "SELECT airframes.airframe AS airframe, DATE_FORMAT(flights.start_time, '%Y-%m-01') " +
                        "AS month, COUNT(*) AS total_flights FROM flights " +
                        "JOIN airframes ON flights.airframe_id = airframes.id WHERE flights.fleet_id = ? " +
                        "GROUP BY airframes.airframe, month, flights.fleet_id ORDER BY month, airframes.airframe, flights.fleet_id";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, fleetId);
            LOG.info("Executing query: " + ps);

            ResultSet result = ps.executeQuery();
            Map<String, Map<String, Integer>> newMonthlyTotalFlightsMap = new HashMap<>();

            while (result.next()) {
                int flights = result.getInt("total_flights");
                String airframe = result.getString("airframe");
                String month = result.getString("month");
                LOG.info(flights + " flights for " + airframe + " during " + month);

                if (!newMonthlyTotalFlightsMap.containsKey(airframe)) {
                    newMonthlyTotalFlightsMap.put(airframe, new HashMap<>());
                }

                newMonthlyTotalFlightsMap.get(airframe).put(month, flights);
            }

            if (!monthlyTotalFlightsMap.containsKey(fleetId)) {
                monthlyTotalFlightsMap.put(fleetId, newMonthlyTotalFlightsMap);
            } else {
                monthlyTotalFlightsMap.get(fleetId).putAll(newMonthlyTotalFlightsMap);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static int calculateAirframeFleetFlights(String airframe) {
        int result = 0;

        LOG.info("calculateAirframeFleetFlights: Checking " + airframe);

        for (Integer fleetID : monthlyTotalFlightsMap.keySet()) {
            try {
                result += monthlyTotalFlightsMap.get(fleetID).get(airframe).values().stream().mapToInt(Integer::intValue).sum();
            } catch (NullPointerException e) {
            }
        }

        return result;
    }

    private static int calculateTotalMonthAirframeFlights(String airframe, String date) {
        int result = 0;

        LOG.info("calculateTotalMonthAirframeFlights: Checking " + airframe + " during " + date);

        String firstOfTheMonth = date.substring(0, 8) + "01";
        for (Integer fleetID : monthlyTotalFlightsMap.keySet()) {
            try {
                result += monthlyTotalFlightsMap.get(fleetID).get(airframe).getOrDefault(firstOfTheMonth, 0);
            } catch (NullPointerException e) {
            }
        }

        return result;
    }

    private static int calculateTotalMonthAirframeFleetFlights(int fleetID, String airframe, String date) {
        LOG.info("calculateTotalMonthAirframeFleetFlights: Checking " + airframe + " during " + date);

        String firstOfTheMonth = date.substring(0, 8) + "01";
        Map<String, Map<String, Integer>> fleetAirframeMap = monthlyTotalFlightsMap.get(fleetID);
        if (fleetAirframeMap == null) {
            return 0;
        }

        Map<String, Integer> monthlyFlightsMap = fleetAirframeMap.get(airframe);
        if (monthlyFlightsMap == null) {
            return 0;
        }

        int result = monthlyFlightsMap.getOrDefault(firstOfTheMonth, 0);
        LOG.info("calculateTotalMonthAirframeFleetFlights: " + result + " flights for " + airframe + " during " + date);

        return result;
    }


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

            monthStats.add(EventRow.getStatistics(connection, "Month to Date", fleetId, eventId, "YEAR(month_first_day) >= ? AND MONTH(month_first_day) >= ?", new int[]{currentYear, currentMonth}));

            int previousMonth = currentMonth - 1;
            int tempYear = currentYear;
            if (currentMonth == 0) {
                currentMonth = 12;
                tempYear = currentYear - 1;
            }

            monthStats.add(EventRow.getStatistics(connection, "Previous Month", fleetId, eventId, "YEAR(month_first_day) = ? AND MONTH(month_first_day) = ?", new int[]{currentYear, currentMonth}));

            monthStats.add(EventRow.getStatistics(connection, "Year to Date", fleetId, eventId, "YEAR(month_first_day) >= ? AND MONTH(month_first_day) >= ?", new int[]{currentYear, 1}));

            monthStats.add(EventRow.getStatistics(connection, "Previous Year", fleetId, eventId, "YEAR(month_first_day) = ?", new int[]{currentYear - 1}));

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

        ArrayList<EventDefinition> eventDefinitions = EventDefinition.getAll(connection, "airframe_id = ? AND  (fleet_id = 0 OR fleet_id = ?)", new Object[]{airframeNameId, fleetId});

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
     * @param startTime  is the earliest time to start counting events (it will count from the beginning of time if it is null)
     * @param endTime    is the latest time to count events (it will count until the current date if it is null)
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
     * @param fleetId    is the id of the fleet, if fleetId <= 0 it will return for the entire NGAFID
     * @param startTime  is the earliest time to start counting events (it will count from the beginning of time if it is null)
     * @param endTime    is the latest time to count events (it will count until the current date if it is null)
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

        ArrayList<String> dates = new ArrayList<>();

        ArrayList<Integer> flightsWithEventCounts = new ArrayList<>();
        ArrayList<Integer> totalFlightsCounts = new ArrayList<>();
        ArrayList<Integer> totalEventsCounts = new ArrayList<>();

        HashMap<String, Integer> flightsWithEventMap = new HashMap<>();
        HashMap<String, Integer> totalFlightsMap = new HashMap<>();
        HashMap<String, Integer> totalEventsMap = new HashMap<>();

        ArrayList<Integer> aggregateFlightsWithEventCounts = new ArrayList<>();
        ArrayList<Integer> aggregateTotalFlightsCounts = new ArrayList<>();
        ArrayList<Integer> aggregateTotalEventsCounts = new ArrayList<>();

        HashMap<String, Integer> aggregateFlightsWithEventMap = new HashMap<>();
        HashMap<String, Integer> aggregateTotalFlightsMap = new HashMap<>();
        HashMap<String, Integer> aggregateTotalEventsMap = new HashMap<>();


        public MonthlyEventCounts(String airframeName, String eventName) {
            this.airframeName = airframeName;
            this.eventName = eventName;
        }

        public void update(String date, Integer flightsWithEvent, Integer totalFlights, Integer totalEvents) {
            Integer flightsWithEventCount = flightsWithEventMap.get(eventName);
            Integer totalFlightsCount = totalFlightsMap.get(eventName);
            Integer totalEventsCount = totalEventsMap.get(eventName);

            if (flightsWithEventCount == null) flightsWithEventCount = 0;
            if (totalFlightsCount == null) totalFlightsCount = 0;
            if (totalEventsCount == null) totalEventsCount = 0;

            flightsWithEventCount += flightsWithEvent;
            totalFlightsCount += totalFlights;
            totalEventsCount += totalEvents;

            flightsWithEventMap.put(date, flightsWithEventCount);
            totalFlightsMap.put(date, totalFlightsCount);
            totalEventsMap.put(date, totalEventsCount);

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

        public void zeroMissingMonths(LocalDate startDate, LocalDate endDate) {
            LocalDate current = startDate;
            while (current.isBefore(endDate)) {
                String date = current.toString();
                if (!flightsWithEventMap.containsKey(date)) {
                    flightsWithEventMap.put(date, 0);
                    totalFlightsMap.put(date, 0);
                    totalEventsMap.put(date, 0);
                }

                if (!aggregateFlightsWithEventMap.containsKey(date)) {
                    aggregateFlightsWithEventMap.put(date, 0);
                    aggregateTotalFlightsMap.put(date, 0);
                    aggregateTotalEventsMap.put(date, 0);
                }

                current = current.plusMonths(1);
            }
        }


        public void setDates(HashMap<String, Integer> eventMap) {
            ArrayList<String> sortedKeys = new ArrayList<String>(eventMap.keySet());
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

        ArrayList<String> names = new ArrayList<>();
        ArrayList<Integer> flightsWithEventCounts = new ArrayList<>();
        ArrayList<Integer> totalFlightsCounts = new ArrayList<>();
        ArrayList<Integer> totalEventsCounts = new ArrayList<>();

        HashMap<String, Integer> flightsWithEventMap = new HashMap<>();
        HashMap<String, Integer> totalFlightsMap = new HashMap<>();
        HashMap<String, Integer> totalEventsMap = new HashMap<>();

        ArrayList<Integer> aggregateFlightsWithEventCounts = new ArrayList<>();
        ArrayList<Integer> aggregateTotalFlightsCounts = new ArrayList<>();
        ArrayList<Integer> aggregateTotalEventsCounts = new ArrayList<>();

        HashMap<String, Integer> aggregateFlightsWithEventMap = new HashMap<>();
        HashMap<String, Integer> aggregateTotalFlightsMap = new HashMap<>();
        HashMap<String, Integer> aggregateTotalEventsMap = new HashMap<>();


        public EventCounts(String airframeName) {
            this.airframeName = airframeName;
        }

        /**
         * Zero out values in Maps for a given event name
         *
         * @param eventName - Name of event
         */
        public void initializeEvent(String eventName) {
            if (flightsWithEventMap.containsKey(eventName)) return;

            flightsWithEventMap.put(eventName, 0);
            totalFlightsMap.put(eventName, 0);
            totalEventsMap.put(eventName, 0);

            aggregateFlightsWithEventMap.put(eventName, 0);
            aggregateTotalFlightsMap.put(eventName, 0);
            aggregateTotalEventsMap.put(eventName, 0);
        }

        /**
         * Add to the values in the Maps for a given event name
         *
         * @param eventName        - Name of event
         * @param flightsWithEvent - Number of flights with event
         * @param totalFlights     - Number of total flights
         * @param totalEvents      - Number of total events
         */
        public void update(String eventName, Integer flightsWithEvent, Integer totalFlights, Integer totalEvents) {
            flightsWithEventMap.put(eventName, flightsWithEventMap.get(eventName) + flightsWithEvent);
            totalFlightsMap.put(eventName, totalFlightsMap.get(eventName) + totalFlights);
            totalEventsMap.put(eventName, totalEventsMap.get(eventName) + totalEvents);
        }

        /**
         * Add to the values in the aggregate Maps for a given event name
         *
         * @param eventName        - Name of event
         * @param flightsWithEvent - Number of flights with event
         * @param totalFlights     - Number of total flights
         * @param totalEvents      - Number of total events
         */
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

        /**
         * Place Map values into lists and dead store eliminate maps
         */
        public void assignLists() {
            ArrayList<String> sortedKeys = new ArrayList<>(flightsWithEventMap.keySet());
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

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("airframeName: ").append(airframeName).append("\n");
            for (String name : names) {
                builder.append("\t").append(name).append("\n");
                builder.append("\t\tTotal Flights Count").append(totalFlightsCounts).append("\n");
                builder.append("\t\tTotal Events Count").append(totalEventsCounts).append("\n");
                builder.append("\t\tFlights With Event Count").append(flightsWithEventCounts).append("\n");
            }

            return builder.toString();
        }
    }

    private static List<Integer> getAllAirframesEvents(Connection connection) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT id FROM event_definitions WHERE airframe_id = 0")) {
            ResultSet resultSet = preparedStatement.executeQuery();
            List<Integer> eventIds = new ArrayList<>();
            while (resultSet.next()) {
                eventIds.add(resultSet.getInt(1));
            }
            return eventIds;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static PreparedStatement buildEventCountsQuery(Connection connection, LocalDate startTime, LocalDate endTime) throws SQLException {
        if (startTime == null) {
            startTime = LocalDate.of(0, 1, 1);
        }

        if (endTime == null) {
            endTime = LocalDate.now();
        }

        String query = "SELECT airframes.airframe AS airframe, events.fleet_id AS fleet_id, event_definitions.name AS event_name," +
                "COUNT(DISTINCT flights.id) AS flights_with_event, COUNT(events.id) AS total_events " +
                "FROM flights JOIN airframes ON flights.airframe_id = airframes.id " +
                "LEFT JOIN events ON events.flight_id = flights.id " +
                "LEFT JOIN event_definitions ON events.event_definition_id = event_definitions.id " +
                "WHERE events.start_time BETWEEN ? AND ? " +
                "GROUP BY airframes.airframe, events.fleet_id, event_definitions.name";

        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, startTime.toString());
        ps.setString(2, endTime.toString());

        LOG.info("Query: " + ps);
        return ps;
    }

    /**
     * Gets the number of exceedences for each type and airframe. It will be organized into a data structure
     * so plotly can display it on the webpage. Fleet can be specified or left null for all fleets
     *
     * @param connection is the connection to the database
     * @param startTime  is the earliest time to start counting events (it will count from the beginning of time if it is null)
     * @param endTime    is the latest time to count events (it will count until the current date if it is null)
     */
    public static Map<String, EventCounts> getEventCounts(Connection connection, LocalDate startTime, LocalDate endTime) throws SQLException {
        Map<String, EventCounts> eventCounts = new HashMap<>();
        Set<String> eventNames = new HashSet<>();
        Set<String> nullDataAirframes = new HashSet<>();
        Map<String, Integer> totalFlightsMap = new HashMap<>();

        try (PreparedStatement ps = buildEventCountsQuery(connection, startTime, endTime)) {
            ResultSet resultSet = ps.executeQuery();

            //get the event statistics for each airframe
            while (resultSet.next()) {
                String airframeName = resultSet.getString("airframe");
                String eventName = resultSet.getString("event_name");

                int flightsWithEvent = resultSet.getInt("flights_with_event");
                int flightsTotal = totalFlightsMap.computeIfAbsent(airframeName, k -> calculateAirframeFleetFlights(airframeName));
                int totalEvents = resultSet.getInt("total_events");

                if (eventName == null) { // No events for this airframe, skip
                    nullDataAirframes.add(airframeName);
                    continue;
                } else {
                    eventNames.add(eventName);
                }

                EventCounts eventCount;
                if (eventCounts.containsKey(airframeName)) {
                    eventCount = eventCounts.get(airframeName);
                } else {
                    eventCount = new EventCounts(airframeName);
                    eventCounts.put(airframeName, eventCount);
                }

                eventCount.initializeEvent(eventName);
                eventCount.update(eventName, flightsWithEvent, flightsTotal, totalEvents);
            }

            resultSet.close();
        }

        for (String airframeName : nullDataAirframes) { // Add empty event counts for airframes with no events
            EventCounts eventCount = new EventCounts(airframeName);
            eventCounts.put(airframeName, eventCount);

            for (String eventName : eventNames) {
                eventCount.initializeEvent(eventName);
            }
        }

        for (EventCounts eventCount : eventCounts.values()) {
            eventCount.assignLists();
        }

        return eventCounts;
    }

    /**
     * Gets the number of exceedences for each type and airframe. It will be organized into a data structure
     * so plotly can display it on the webpage. Fleet can be specified or left null for all fleets
     *
     * @param connection is the connection to the database
     * @param fleetId    is the id of the fleet, fleetId needs to be > 0 (i.e., a valid fleet id). Null for all fleets.
     * @param startTime  is the earliest time to start counting events (it will count from the beginning of time if it is null)
     * @param endTime    is the latest time to count events (it will count until the current date if it is null)
     */
    public static Map<String, EventCounts> getEventCounts(Connection connection, int fleetId, LocalDate startTime, LocalDate endTime) throws SQLException {
        Map<String, EventCounts> eventCounts = new HashMap<>();
        Map<String, Integer> totalFlightsMap = new HashMap<>();
        Set<String> eventNames = new HashSet<>();
        Set<String> nullDataAirframes = new HashSet<>();

        try (PreparedStatement ps = buildEventCountsQuery(connection, startTime, endTime)) {
            ResultSet resultSet = ps.executeQuery();

            //get the event statistics for each airframe
            while (resultSet.next()) {
                String airframeName = resultSet.getString("airframe");
                String eventName = resultSet.getString("event_name");

                int flightsWithEvent = resultSet.getInt("flights_with_event");
                int flightsTotal = totalFlightsMap.computeIfAbsent(airframeName, k -> calculateAirframeFleetFlights(airframeName));
                int totalEvents = resultSet.getInt("total_events");
                int resultFleetID = resultSet.getInt("fleet_id");

                if (eventName == null) { // No events for this airframe, skip
                    nullDataAirframes.add(airframeName);
                    continue;
                } else {
                    eventNames.add(eventName);
                }

                EventCounts eventCount;
                if (eventCounts.containsKey(airframeName)) {
                    eventCount = eventCounts.get(airframeName);
                } else {
                    eventCount = new EventCounts(airframeName);
                    eventCounts.put(airframeName, eventCount);
                }

                eventCount.initializeEvent(eventName);

                if (resultFleetID == fleetId) {
                    eventCount.update(eventName, flightsWithEvent, flightsTotal, totalEvents);
                } else {
                    eventCount.updateAggregate(eventName, flightsWithEvent, flightsTotal, totalEvents);
                }
            }

            resultSet.close();
        }

        for (String airframeName : nullDataAirframes) { // Add empty event counts for airframes with no events
            EventCounts eventCount = new EventCounts(airframeName);
            eventCounts.put(airframeName, eventCount);

            for (String eventName : eventNames) {
                eventCount.initializeEvent(eventName);
            }
        }

        for (EventCounts eventCount : eventCounts.values()) {
            eventCount.assignLists();
        }

        return eventCounts;
    }

    private static PreparedStatement buildMonthlyEventsQuery(Connection connection, String eventName, LocalDate startTime, LocalDate endTime) throws SQLException {
        if (startTime == null) {
            startTime = LocalDate.of(0, 1, 1);
        }

        if (endTime == null) {
            endTime = LocalDate.now();
        }

        String query = "SELECT airframes.airframe AS airframe, events.fleet_id AS fleet_id, event_definitions.name AS event_name, " +
                        "COUNT(DISTINCT flights.id) AS flights_with_event, COUNT(events.id) AS event_count, DATE_FORMAT(events.start_time, '%Y-%m-01') AS month_first_day " +
                        "FROM flights JOIN airframes ON flights.airframe_id = airframes.id LEFT JOIN events ON events.flight_id = flights.id " +
                        "LEFT JOIN event_definitions ON events.event_definition_id = event_definitions.id WHERE events.start_time BETWEEN ? AND ? " +
                        "AND event_definitions.name = ? GROUP BY airframes.airframe, events.fleet_id, month_first_day";

        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, startTime.toString());
        ps.setString(2, endTime.toString());
        ps.setString(3, eventName);

        LOG.info("Query: %s" + ps);
        return ps;
    }

    /**
     * Gets the number of exceedences for each type and airframe for a fleet, ordered by months, for a given event name. It will be organized into a data structure
     * so plotly can display it on the webpage
     *
     * @param connection is the connection to the database
     * @param eventName  is the name of the event
     * @param startTime  is the earliest time to start getting events (it will get events from the beginning of time if it is null)
     * @param endTime    is the latest time to getting events (it will get events until the current date if it is null)
     */
    public static Map<String, MonthlyEventCounts> getMonthlyEventCounts(Connection connection, String eventName, LocalDate startTime, LocalDate endTime) throws SQLException {
        Map<String, MonthlyEventCounts> eventCounts = new HashMap<>();

        try (PreparedStatement ps = buildMonthlyEventsQuery(connection, eventName, startTime, endTime)) {
            ResultSet resultSet = ps.executeQuery();
            if (resultSet == null) {
                LOG.warning("Querying monthly events returned null");
                return eventCounts;
            }

            while (resultSet.next()) {
                String airframeName = resultSet.getString("airframe");
                MonthlyEventCounts eventCount = eventCounts.get(airframeName);

                if (eventCount == null) {
                    eventCount = new MonthlyEventCounts(airframeName, eventName);
                    eventCounts.put(airframeName, eventCount);
                }

                String date = resultSet.getString("month_first_day");
                int flightsWithEvent = resultSet.getInt("flights_with_event");
                int totalEvents = resultSet.getInt("event_count");
                int totalFlights = calculateTotalMonthAirframeFlights(airframeName, date);

                LOG.info(airframeName + " - " + date + ": " + flightsWithEvent + ", " + totalFlights + ", " + totalEvents);
                eventCount.updateAggregate(date, flightsWithEvent, totalFlights, totalEvents);
            }

            resultSet.close();
            for (MonthlyEventCounts eventCount : eventCounts.values()) {
                eventCount.zeroMissingMonths(startTime, endTime);
                eventCount.setDates(eventCount.aggregateFlightsWithEventMap);
                eventCount.assignAggregateLists();
            }

        } catch (SQLException e) {
            LOG.severe(e.getMessage());
            throw e;
        }


        return eventCounts;
    }

    /**
     * Gets the number of exceedences for each type and airframe for a fleet, ordered by months, for a given event name. It will be organized into a data structure
     * so plotly can display it on the webpage
     *
     * @param connection is the connection to the database
     * @param fleetId    is the id of the fleet, if null get data for all fleets
     * @param eventName  is the name of the event
     * @param startTime  is the earliest time to start getting events (it will get events from the beginning of time if it is null)
     * @param endTime    is the latest time to getting events (it will get events until the current date if it is null)
     */
    public static Map<String, MonthlyEventCounts> getMonthlyEventCounts(Connection connection, int fleetId, String eventName, LocalDate startTime, LocalDate endTime) throws SQLException {
        LOG.info("Getting monthly event counts for fleet: " + fleetId);
        Map<String, MonthlyEventCounts> eventCounts = new HashMap<>();
        try (PreparedStatement ps = buildMonthlyEventsQuery(connection, eventName, startTime, endTime)) {
            ResultSet resultSet = ps.executeQuery();

            while (resultSet.next()) {

                String airframeName = resultSet.getString("airframe");
                MonthlyEventCounts eventCount = eventCounts.get(airframeName);

                if (eventCount == null) {
                    eventCount = new MonthlyEventCounts(airframeName, eventName);
                    eventCounts.put(airframeName, eventCount);
                }

                String date = resultSet.getString("month_first_day");
                int statFleetId = resultSet.getInt("fleet_id");
                int flightsWithEvent = resultSet.getInt("flights_with_event");
                Integer totalFlights = calculateTotalMonthAirframeFleetFlights(fleetId, airframeName, date);
                if (totalFlights == null) {
                    totalFlights = 0;
                }
                int totalEvents = resultSet.getInt("event_count");

                LOG.info(statFleetId + " " + airframeName + " - " + date + ": " + flightsWithEvent + ", " + totalFlights + ", " + totalEvents);

                if (statFleetId == fleetId) {
                    eventCount.update(date, flightsWithEvent, totalFlights, totalEvents);
                } else {
                    eventCount.updateAggregate(date, flightsWithEvent, totalFlights, totalEvents);
                }
            }
        } catch (SQLException e) {
            LOG.severe(e.getMessage());
            throw e;
        }

        for (MonthlyEventCounts eventCount : eventCounts.values()) {
            eventCount.zeroMissingMonths(startTime, endTime);
            eventCount.setDates(eventCount.flightsWithEventMap);
            eventCount.assignLists();
            eventCount.assignAggregateLists();
        }


        return eventCounts;
    }
}

