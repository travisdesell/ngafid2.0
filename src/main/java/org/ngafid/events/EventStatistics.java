package org.ngafid.events;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.ngafid.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.logging.Logger;

import org.ngafid.events.EventStatisticsFetch.CacheObject;
import static org.ngafid.events.EventStatisticsFetch.JSON_CACHE_FILE_NAME;
import org.ngafid.flights.Airframes;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

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

    private static final String EVENT_COUNT_BASE_QUERY_PARAMS = """
             SELECT COUNT(DISTINCT e.id) AS 
                    event_count, 
                    COUNT(DISTINCT flights.id) as flight_count,
                    event_definition_id,
                    flights.fleet_id as fleet_id,
                    flights.airframe_id as airframe_id
    """;

    private static final String EVENT_COUNT_BASE_QUERY_CONDITIONS = """
               FROM events AS e 
         INNER JOIN flights 
                 ON flights.id = e.flight_id
    """;
    
    private static final String EVENT_COUNT_BASE_QUERY_GROUP_BY = """
           GROUP BY event_definition_id,
                    flights.airframe_id,
                    flights.fleet_id
    """;
    
    private static final String EVENT_COUNT_BETWEEN_DATE_CLAUSE = """
              WHERE e.start_time BETWEEN ? AND ?
    """;

    private static final String MONTHLY_EVENT_COUNT_QUERY_PARAMS = EVENT_COUNT_BASE_QUERY_PARAMS + """
                  , YEAR(e.start_time) as year,
                    MONTH(e.start_time) as month
    """;
    
    private static final String MONTHLY_EVENT_COUNT_QUERY_GROUP_BY = EVENT_COUNT_BASE_QUERY_GROUP_BY + """
                  , YEAR(e.start_time), 
                    MONTH(e.start_time)

    """;

    // NOTES: You are going to have to mess with the js code that calls this, because it wont look in the aggregate fields for osme reason.
    // this isnt a problem w the monthly stuff.
    public static Map<String, EventCounts> getEventCounts(Connection connection, LocalDate startTime, LocalDate endTime) throws SQLException {
        return getEventCounts(connection, -1, startTime, endTime);
    }

    public static Map<String, EventCounts> getEventCounts(Connection connection, int fleetId, LocalDate startDate, LocalDate endDate) throws SQLException {
        if (startDate == null)
            startDate = LocalDate.of(0, 1, 1);

        if (endDate == null)
            endDate = LocalDate.now();

        String query =  EVENT_COUNT_BASE_QUERY_PARAMS
                      + EVENT_COUNT_BASE_QUERY_CONDITIONS
                      + EVENT_COUNT_BETWEEN_DATE_CLAUSE
                      + EVENT_COUNT_BASE_QUERY_GROUP_BY;

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, startDate.toString());
        statement.setString(2, endDate.toString());

        ResultSet result = statement.executeQuery();

        Map<Integer, String> idToAirframeNameMap = Airframes.getIdToNameMap(connection);
        Map<Integer, String> idToEventNameMap = EventDefinition.getEventDefinitionIdToNameMap(connection);

        FlightCounts fc = getFlightCounts(connection, startDate, endDate);
        Map<String, EventCountsBuilder> eventCounts = new HashMap<>();

        while (result.next()) {
            int fleet = result.getInt("fleet_id");
            int eventCount = result.getInt("event_count");
            int flightCount = result.getInt("flight_count");
            int eventDefinitionId = result.getInt("event_definition_id");
            int airframeId = result.getInt("airframe_id");

            String eventName = idToEventNameMap.get(eventDefinitionId);
            String airframeName = idToAirframeNameMap.get(airframeId);
            
            EventCountsBuilder ec = eventCounts.computeIfAbsent(airframeName, EventCountsBuilder::new);
            ec.updateAggregate(eventName, flightCount, 0, eventCount);
            ec.setAggregateTotalFlights(eventName, fc.getAggregateCounts().get(airframeId));

            if (fleetId == fleet) {
                ec.update(eventName, flightCount, fc.getFleetCounts(fleet).get(airframeId), eventCount);
            }
        }

        return eventCounts
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().build()
                )
            );
    }

    public static Map<String, Map<String, MonthlyEventCounts>> getMonthlyEventCounts(Connection connection, int fleetId, LocalDate startDateNullable, LocalDate endDateNullable) throws SQLException {
        final LocalDate startDate = startDateNullable == null ? LocalDate.of(0, 1, 1) : startDateNullable;
        final LocalDate endDate = endDateNullable == null ? LocalDate.now() : endDateNullable;

        String query =  MONTHLY_EVENT_COUNT_QUERY_PARAMS
                      + EVENT_COUNT_BASE_QUERY_CONDITIONS
                      + EVENT_COUNT_BETWEEN_DATE_CLAUSE
                      + MONTHLY_EVENT_COUNT_QUERY_GROUP_BY;

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, startDate.toString());
        statement.setString(2, endDate.toString());

        ResultSet result = statement.executeQuery();

        Map<Integer, String> idToAirframeNameMap = Airframes.getIdToNameMap(connection);
        Map<Integer, String> idToEventNameMap = EventDefinition.getEventDefinitionIdToNameMap(connection);
    
        FlightCounts fc = getFlightCounts(connection, startDate, endDate);
        Map<String, Map<String, MonthlyEventCountsBuilder>> eventCounts = new HashMap<>();
   
        while (result.next()) {
            final int fleet = result.getInt("fleet_id");
            final int eventCount = result.getInt("event_count");
            final int flightCount = result.getInt("flight_count");
            final int eventDefinitionId = result.getInt("event_definition_id");
            final int airframeId = result.getInt("airframe_id");
            final int month = result.getInt("month");
            final int year = result.getInt("year");
            final int totalFlights = fc.getFleetCounts(fleet).get(airframeId);

            final String date = LocalDate.of(year, month, 1).toString();
            final String eventName = idToEventNameMap.get(eventDefinitionId);
            final String airframeName = idToAirframeNameMap.get(airframeId);
            
            MonthlyEventCountsBuilder mec = eventCounts
                .computeIfAbsent(eventName, k -> new HashMap<>())
                .computeIfAbsent(airframeName, k -> new MonthlyEventCountsBuilder(airframeName, eventName, startDate, endDate));

            mec.updateAggregate(date, flightCount, totalFlights, eventCount);
            if (fleetId == fleet)
                mec.update(date, flightCount, totalFlights, eventCount);
        }

        // Create a new map, where each value is itself a map that has MonthlyEventCounts for values.
        // Map<String, Map<String, MonthlyEventCounts>> monthlyEventCounts = new HashMap<>(eventCounts.size());

        // for (Map.Entry<String, Map<String, MonthlyEventCountsBuilder>> entry : eventCounts.entrySet()) {
        //     Map<String, MonthlyEventCounts> 
        // }

        return eventCounts
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().entrySet().stream()
                        .collect(
                            Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().build()
                            )
                        )
                )
            );
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
                .merge(fleetId, flightCount, Integer::sum);

            fleetToAirframeCounts
                .computeIfAbsent(fleetId, k -> new HashMap<>())
                .merge(airframeId, flightCount, Integer::sum);

            aggregateCounts
                .merge(airframeId, flightCount, Integer::sum);
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
    public static FlightCounts getFlightCounts(Connection connection, LocalDate startDate, LocalDate endDate) throws SQLException {
        if (startDate == null)
            startDate = LocalDate.of(0, 1, 1);

        if (endDate == null)
            endDate = LocalDate.now();

        Map<Integer, Map<Integer, Integer>> out = new HashMap<>();

        String query = "SELECT COUNT(DISTINCT id) as flight_count, airframe_id, fleet_id FROM flights WHERE start_time BETWEEN ? AND ? GROUP BY flights.airframe_id, flights.fleet_id ";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, startDate.toString());
        ps.setString(2, endDate.toString());

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
        if (startDateTime.length() < 8) {
            LOG.severe("could not update event statistics because startDateTime '" + startDateTime + "' was improperly formatted!");
            return;
        }
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

    public static abstract class EventCountsWithAggregateBuilder<T extends EventCountsWithAggregate> {
        HashMap<String, Integer> flightsWithEventMap = new HashMap<>();
        HashMap<String, Integer> totalFlightsMap = new HashMap<>();
        HashMap<String, Integer> totalEventsMap = new HashMap<>();

        HashMap<String, Integer> aggregateFlightsWithEventMap = new HashMap<>();
        HashMap<String, Integer> aggregateTotalFlightsMap = new HashMap<>();
        HashMap<String, Integer> aggregateTotalEventsMap = new HashMap<>();

        TreeSet<String> keys = new TreeSet<>();

        public abstract T build();

        public static int[] linearize(List<String> keys, HashMap<String, Integer> map) {
            int[] out = new int[keys.size()];
            
            for (int i = 0; i < keys.size(); i++)
                out[i] = map.getOrDefault(keys.get(i), 0);

            return out;
        }

        public void update(String key, int flightsWithEvent, int totalFlights, int totalEvents) {
            keys.add(key);

            flightsWithEventMap.merge(key, flightsWithEvent, Integer::sum);
            totalFlightsMap.merge(key, totalFlights, Integer::sum);
            totalEventsMap.merge(key, totalEvents, Integer::sum);

            System.out.println("" + flightsWithEvent + " : " + totalFlights + " : " + totalFlights);
        }

        public void updateAggregate(String key, int flightsWithEvent, int totalFlights, int totalEvents) {
            keys.add(key);

            aggregateFlightsWithEventMap.merge(key, flightsWithEvent, Integer::sum);
            aggregateTotalFlightsMap.merge(key, totalFlights, Integer::sum);
            aggregateTotalEventsMap.merge(key, totalEvents, Integer::sum);
        }
    }

    public static class EventCountsWithAggregate {
        public final int[] flightsWithEventCounts;
        public final int[] totalFlightsCounts;
        public final int[] totalEventsCounts;
        
        public final int[] aggregateFlightsWithEventCounts;
        public final int[] aggregateTotalFlightsCounts;
        public final int[] aggregateTotalEventsCounts;
    
        public EventCountsWithAggregate(
            int[] flightsWithEventCounts, int[] totalFlightsCounts, int[] totalEventsCounts,
            int[] aggregateFlightsWithEventCounts, int[] aggregateTotalFlightsCounts, int[] aggregateTotalEventsCounts
        ) {
            this.flightsWithEventCounts = flightsWithEventCounts;
            this.totalFlightsCounts = totalFlightsCounts;
            this.totalEventsCounts = totalEventsCounts;

            this.aggregateFlightsWithEventCounts = aggregateFlightsWithEventCounts;
            this.aggregateTotalFlightsCounts = aggregateTotalFlightsCounts;
            this.aggregateTotalEventsCounts = aggregateTotalEventsCounts;
        }
    }

    public static class MonthlyEventCounts extends EventCountsWithAggregate {
        public final String airframeName, eventName;
        public final List<String> dates;
        
        public MonthlyEventCounts(
            String airframeName, String eventName, List<String> dates,
            int[] flightsWithEventCounts, int[] totalFlightsCounts, int[] totalEventsCounts,
            int[] aggregateFlightsWithEventCounts, int[] aggregateTotalFlightsCounts, int[] aggregateTotalEventsCounts
        ) {
            super(flightsWithEventCounts, totalFlightsCounts, totalEventsCounts, aggregateFlightsWithEventCounts, aggregateTotalFlightsCounts, aggregateTotalEventsCounts);
            
            this.airframeName = airframeName;
            this.eventName = eventName;
            this.dates = dates;
        }
    }

    public static class MonthlyEventCountsBuilder extends EventCountsWithAggregateBuilder<MonthlyEventCounts> {
        final String airframeName;
        final String eventName;

        final List<String> dates;

        public MonthlyEventCountsBuilder(String airframeName, String eventName, LocalDate startDate, LocalDate endDate) {
            this.airframeName = airframeName;
            this.eventName = eventName;
            
            // Create a date for each month between start date and end date.
            dates = Stream.iterate(startDate, date -> date.plusMonths(1))
                .limit(ChronoUnit.MONTHS.between(startDate, endDate))
                .map(LocalDate::toString)
                .collect(Collectors.toList());
        }

        private int[] linearize(HashMap<String, Integer> map) {
            return linearize(dates, map);
        }

        @Override
        public MonthlyEventCounts build() {
            return new MonthlyEventCounts(
                airframeName, eventName, dates,
                linearize(flightsWithEventMap), linearize(totalFlightsMap), linearize(totalEventsMap),
                linearize(aggregateFlightsWithEventMap), linearize(aggregateTotalFlightsMap), linearize(aggregateTotalEventsMap)
            );
        }

    }

    public static class EventCounts extends EventCountsWithAggregate {
        public final String airframeName;
        public final List<String> names;
        
        public EventCounts(
            String airframeName, List<String> names,
            int[] flightsWithEventCounts, int[] totalFlightsCounts, int[] totalEventsCounts,
            int[] aggregateFlightsWithEventCounts, int[] aggregateTotalFlightsCounts, int[] aggregateTotalEventsCounts
        ) {
            super(flightsWithEventCounts, totalFlightsCounts, totalEventsCounts, aggregateFlightsWithEventCounts, aggregateTotalFlightsCounts, aggregateTotalEventsCounts);

            this.names = names;
            this.airframeName = airframeName;
        }
    }

    public static class EventCountsBuilder extends EventCountsWithAggregateBuilder<EventCounts> {
        public final String airframeName;

        public EventCountsBuilder(String airframeName) {
            this.airframeName = airframeName;
        }

        public void setAggregateTotalFlights(String eventName, int totalFlights) {
            aggregateTotalFlightsMap.put(eventName, totalFlights);
        }

        /**
         * Place Map values into lists and dead store eliminate maps
         */
        public EventCounts build() {
            ArrayList<String> sortedKeys = new ArrayList<>(keys);
            // Should be sorted because keys is a tree set.

            return new EventCounts(
                airframeName, sortedKeys,
                linearize(sortedKeys, flightsWithEventMap), linearize(sortedKeys, totalFlightsMap), linearize(sortedKeys, totalEventsMap),
                linearize(sortedKeys, aggregateFlightsWithEventMap), linearize(sortedKeys, aggregateTotalFlightsMap), linearize(sortedKeys, aggregateTotalEventsMap)
            );
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

    // /**
    //  * Gets the number of exceedences for each type and airframe. It will be organized into a data structure
    //  * so plotly can display it on the webpage. Fleet can be specified or left null for all fleets
    //  *
    //  * @param connection is the connection to the database
    //  * @param startTime  is the earliest time to start counting events (it will count from the beginning of time if it is null)
    //  * @param endTime    is the latest time to count events (it will count until the current date if it is null)
    //  */
    // public static Map<String, EventCounts> getEventCounts(Connection connection, LocalDate startTime, LocalDate endTime) throws SQLException {
    //     Map<String, EventCounts> eventCounts = new HashMap<>();
    //     Set<String> eventNames = new HashSet<>();
    //     Set<String> nullDataAirframes = new HashSet<>();
    //     Map<String, Integer> totalFlightsMap = new HashMap<>();

    //     try (PreparedStatement ps = buildEventCountsQuery(connection, startTime, endTime)) {
    //         ResultSet resultSet = ps.executeQuery();

    //         //get the event statistics for each airframe
    //         while (resultSet.next()) {
    //             String airframeName = resultSet.getString("airframe");
    //             String eventName = resultSet.getString("event_name");

    //             int flightsWithEvent = resultSet.getInt("flights_with_event");
    //             int flightsTotal = totalFlightsMap.computeIfAbsent(airframeName, k -> calculateAirframeFleetFlights(airframeName));
    //             int totalEvents = resultSet.getInt("total_events");

    //             if (eventName == null) { // No events for this airframe, skip
    //                 nullDataAirframes.add(airframeName);
    //                 continue;
    //             } else {
    //                 eventNames.add(eventName);
    //             }

    //             EventCounts eventCount;
    //             if (eventCounts.containsKey(airframeName)) {
    //                 eventCount = eventCounts.get(airframeName);
    //             } else {
    //                 eventCount = new EventCounts(airframeName);
    //                 eventCounts.put(airframeName, eventCount);
    //             }

    //             eventCount.initializeEvent(eventName);
    //             eventCount.update(eventName, flightsWithEvent, flightsTotal, totalEvents);
    //         }

    //         resultSet.close();
    //     }

    //     for (String airframeName : nullDataAirframes) { // Add empty event counts for airframes with no events
    //         EventCounts eventCount = new EventCounts(airframeName);
    //         eventCounts.put(airframeName, eventCount);

    //         for (String eventName : eventNames) {
    //             eventCount.initializeEvent(eventName);
    //         }
    //     }

    //     for (EventCounts eventCount : eventCounts.values()) {
    //         eventCount.assignLists();
    //     }

    //     return eventCounts;
    // }

    // /**
    //  * Gets the number of exceedences for each type and airframe. It will be organized into a data structure
    //  * so plotly can display it on the webpage. Fleet can be specified or left null for all fleets
    //  *
    //  * @param connection is the connection to the database
    //  * @param fleetId    is the id of the fleet, fleetId needs to be > 0 (i.e., a valid fleet id). Null for all fleets.
    //  * @param startTime  is the earliest time to start counting events (it will count from the beginning of time if it is null)
    //  * @param endTime    is the latest time to count events (it will count until the current date if it is null)
    //  */
    // public static Map<String, EventCounts> getEventCounts(Connection connection, int fleetId, LocalDate startTime, LocalDate endTime) throws SQLException {
    //     Map<String, EventCounts> eventCounts = new HashMap<>();
    //     Map<String, Integer> totalFlightsMap = new HashMap<>();
    //     Set<String> eventNames = new HashSet<>();
    //     Set<String> nullDataAirframes = new HashSet<>();

    //     try (PreparedStatement ps = buildEventCountsQuery(connection, startTime, endTime)) {
    //         ResultSet resultSet = ps.executeQuery();

    //         //get the event statistics for each airframe
    //         while (resultSet.next()) {
    //             String airframeName = resultSet.getString("airframe");
    //             String eventName = resultSet.getString("event_name");

    //             int flightsWithEvent = resultSet.getInt("flights_with_event");
    //             int flightsTotal = totalFlightsMap.computeIfAbsent(airframeName, k -> calculateAirframeFleetFlights(airframeName));
    //             int totalEvents = resultSet.getInt("total_events");
    //             int resultFleetID = resultSet.getInt("fleet_id");

    //             if (eventName == null) { // No events for this airframe, skip
    //                 nullDataAirframes.add(airframeName);
    //                 continue;
    //             } else {
    //                 eventNames.add(eventName);
    //             }

    //             EventCounts eventCount;
    //             if (eventCounts.containsKey(airframeName)) {
    //                 eventCount = eventCounts.get(airframeName);
    //             } else {
    //                 eventCount = new EventCounts(airframeName);
    //                 eventCounts.put(airframeName, eventCount);
    //             }

    //             eventCount.initializeEvent(eventName);

    //             if (resultFleetID == fleetId) {
    //                 eventCount.update(eventName, flightsWithEvent, flightsTotal, totalEvents);
    //             } else {
    //                 eventCount.updateAggregate(eventName, flightsWithEvent, flightsTotal, totalEvents);
    //             }
    //         }

    //         resultSet.close();
    //     }

    //     for (String airframeName : nullDataAirframes) { // Add empty event counts for airframes with no events
    //         EventCounts eventCount = new EventCounts(airframeName);
    //         eventCounts.put(airframeName, eventCount);

    //         for (String eventName : eventNames) {
    //             eventCount.initializeEvent(eventName);
    //         }
    //     }

    //     for (EventCounts eventCount : eventCounts.values()) {
    //         eventCount.assignLists();
    //     }

    //     return eventCounts;
    // }

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

    // /**
    //  * Gets the number of exceedences for each type and airframe for a fleet, ordered by months, for a given event name. It will be organized into a data structure
    //  * so plotly can display it on the webpage
    //  *
    //  * @param connection is the connection to the database
    //  * @param eventName  is the name of the event
    //  * @param startTime  is the earliest time to start getting events (it will get events from the beginning of time if it is null)
    //  * @param endTime    is the latest time to getting events (it will get events until the current date if it is null)
    //  */
    // public static Map<String, MonthlyEventCounts> getMonthlyEventCounts(Connection connection, String eventName, LocalDate startTime, LocalDate endTime) throws SQLException {
    //     Map<String, MonthlyEventCounts> eventCounts = new HashMap<>();

    //     try (PreparedStatement ps = buildMonthlyEventsQuery(connection, eventName, startTime, endTime)) {
    //         ResultSet resultSet = ps.executeQuery();
    //         if (resultSet == null) {
    //             LOG.warning("Querying monthly events returned null");
    //             return eventCounts;
    //         }

    //         while (resultSet.next()) {
    //             String airframeName = resultSet.getString("airframe");
    //             MonthlyEventCounts eventCount = eventCounts.get(airframeName);

    //             if (eventCount == null) {
    //                 eventCount = new MonthlyEventCounts(airframeName, eventName);
    //                 eventCounts.put(airframeName, eventCount);
    //             }

    //             String date = resultSet.getString("month_first_day");
    //             int flightsWithEvent = resultSet.getInt("flights_with_event");
    //             int totalEvents = resultSet.getInt("event_count");
    //             int totalFlights = calculateTotalMonthAirframeFlights(airframeName, date);

    //             LOG.info(airframeName + " - " + date + ": " + flightsWithEvent + ", " + totalFlights + ", " + totalEvents);
    //             eventCount.updateAggregate(date, flightsWithEvent, totalFlights, totalEvents);
    //         }

    //         resultSet.close();
    //         for (MonthlyEventCounts eventCount : eventCounts.values()) {
    //             eventCount.zeroMissingMonths(startTime, endTime);
    //             eventCount.setDates(eventCount.aggregateFlightsWithEventMap);
    //             eventCount.assignAggregateLists();
    //         }

    //     } catch (SQLException e) {
    //         LOG.severe(e.getMessage());
    //         throw e;
    //     }


    //     return eventCounts;
    // }

    // /**
    //  * Gets the number of exceedences for each type and airframe for a fleet, ordered by months, for a given event name. It will be organized into a data structure
    //  * so plotly can display it on the webpage
    //  *
    //  * @param connection is the connection to the database
    //  * @param fleetId    is the id of the fleet, if null get data for all fleets
    //  * @param eventName  is the name of the event
    //  * @param startTime  is the earliest time to start getting events (it will get events from the beginning of time if it is null)
    //  * @param endTime    is the latest time to getting events (it will get events until the current date if it is null)
    //  */
    // public static Map<String, MonthlyEventCounts> getMonthlyEventCounts(Connection connection, int fleetId, String eventName, LocalDate startTime, LocalDate endTime) throws SQLException {
    //     LOG.info("Getting monthly event counts for fleet: " + fleetId);
    //     Map<String, MonthlyEventCounts> eventCounts = new HashMap<>();
    //     try (PreparedStatement ps = buildMonthlyEventsQuery(connection, eventName, startTime, endTime)) {
    //         ResultSet resultSet = ps.executeQuery();

    //         while (resultSet.next()) {

    //             String airframeName = resultSet.getString("airframe");
    //             MonthlyEventCounts eventCount = eventCounts.get(airframeName);

    //             if (eventCount == null) {
    //                 eventCount = new MonthlyEventCounts(airframeName, eventName);
    //                 eventCounts.put(airframeName, eventCount);
    //             }

    //             String date = resultSet.getString("month_first_day");
    //             int statFleetId = resultSet.getInt("fleet_id");
    //             int flightsWithEvent = resultSet.getInt("flights_with_event");
    //             Integer totalFlights = calculateTotalMonthAirframeFleetFlights(fleetId, airframeName, date);
    //             if (totalFlights == null) {
    //                 totalFlights = 0;
    //             }
    //             int totalEvents = resultSet.getInt("event_count");

    //             LOG.info(statFleetId + " " + airframeName + " - " + date + ": " + flightsWithEvent + ", " + totalFlights + ", " + totalEvents);

    //             if (statFleetId == fleetId) {
    //                 eventCount.update(date, flightsWithEvent, totalFlights, totalEvents);
    //             } else {
    //                 eventCount.updateAggregate(date, flightsWithEvent, totalFlights, totalEvents);
    //             }
    //         }
    //     } catch (SQLException e) {
    //         LOG.severe(e.getMessage());
    //         throw e;
    //     }

    //     for (MonthlyEventCounts eventCount : eventCounts.values()) {
    //         eventCount.zeroMissingMonths(startTime, endTime);
    //         eventCount.setDates(eventCount.flightsWithEventMap);
    //         eventCount.assignLists();
    //         eventCount.assignAggregateLists();
    //     }


    //     return eventCounts;
    // }

    public int getAirframeNameId() {
        return airframeNameId;
    }

    public String getAirframeName() {
        return airframeName;
    }

    public static CacheObject importFromJsonCache() {

        CacheObject loadedCache = new CacheObject();

        //File doesn't exist, log a warning and return an empty CacheObject
        if (!Files.exists(Paths.get(JSON_CACHE_FILE_NAME))) {

            LOG.warning("importFromJsonCache: File not found at '" + JSON_CACHE_FILE_NAME + "'. Returning empty CacheObject.");
            return loadedCache;

        }

        try {
            
            //Read the file content
            String existingJson = Files.readString(Paths.get(JSON_CACHE_FILE_NAME));
            Gson gson = new Gson();

            //Parse the JSON
            JsonElement jsonElement = JsonParser.parseString(existingJson);
            if (jsonElement.isJsonObject()) {

                CacheObject parsed = gson.fromJson(jsonElement, CacheObject.class);
                if (parsed != null)
                    loadedCache = parsed;

                else
                    LOG.severe("importFromJsonCache: Parsed JSON was null!");
                
            } else {
                LOG.severe("importFromJsonCache: Expected JSON object in the cache file but found something else.");
            }

        } catch (IOException e) {
            LOG.severe("importFromJsonCache: IOException reading cache file:\n\t" + e.getMessage());
        } catch (JsonSyntaxException | JsonIOException e) {
            LOG.severe("importFromJsonCache: JSON parse error:\n\t" + e.getMessage());
        }

        return loadedCache;

    }

    public static Map<Integer, EventStatisticsFetch.CacheObject> importAllFleetsCache() {

        Map<Integer, EventStatisticsFetch.CacheObject> fleetMap = new HashMap<>();

        //No cache file, return the empty map
        if (!Files.exists(Paths.get(EventStatisticsFetch.JSON_CACHE_FILE_NAME))) {
            LOG.warning("importAllFleetsCache: Cache file not found: " + EventStatisticsFetch.JSON_CACHE_FILE_NAME);
            return fleetMap; 
        }

        try {
            String json = Files.readString(Paths.get(EventStatisticsFetch.JSON_CACHE_FILE_NAME));
            Gson gson = new Gson();

            //Deserializing Map<Integer, CacheObject>, use TypeToken to get the generic type:
            Type typeOfMap = new TypeToken<Map<Integer, EventStatisticsFetch.CacheObject>>() {}.getType();

            Map<Integer, EventStatisticsFetch.CacheObject> parsed = gson.fromJson(json, typeOfMap);
            if (parsed != null)
                fleetMap = parsed;
            else
                LOG.severe("importAllFleetsCache: Parsed JSON was null!");

        } catch (IOException e) {
            LOG.severe("importAllFleetsCache: IOException while reading cache file:\n\t" + e.getMessage());
        } catch (Exception e) {
            LOG.severe("importAllFleetsCache: Unexpected error while parsing:\n\t" + e.getMessage());
        }

        return fleetMap;
    }


}
