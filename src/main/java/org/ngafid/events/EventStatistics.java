package org.ngafid.events;

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

import org.ngafid.flights.Airframes;

public class EventStatistics {
    private static final Logger LOG = Logger.getLogger(EventStatistics.class.getName());

    // Fleet ID - Airframe - Month - Total Flights
    private static Map<Integer, Map<String, Map<String, Integer>>> monthlyTotalFlightsMap = new HashMap<>();

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

    // NOTES: You are going to have to mess with the js code that calls this,
    // because it wont look in the aggregate fields for osme reason.
    // this isnt a problem w the monthly stuff.
    public static Map<String, EventCounts> getEventCounts(Connection connection, LocalDate startTime, LocalDate endTime)
            throws SQLException {
        return getEventCounts(connection, -1, startTime, endTime);
    }

    public static Map<String, EventCounts> getEventCounts(Connection connection, int fleetId, LocalDate startDate,
            LocalDate endDate) throws SQLException {
        if (startDate == null)
            startDate = LocalDate.of(0, 1, 1);

        if (endDate == null)
            endDate = LocalDate.now();

        String query = EVENT_COUNT_BASE_QUERY_PARAMS
                + EVENT_COUNT_BASE_QUERY_CONDITIONS
                + EVENT_COUNT_BETWEEN_DATE_CLAUSE
                + EVENT_COUNT_BASE_QUERY_GROUP_BY;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, startDate.toString());
            statement.setString(2, endDate.toString());

            Map<Integer, String> idToAirframeNameMap = Airframes.getIdToNameMap(connection);
            Map<Integer, String> idToEventNameMap = EventDefinition.getEventDefinitionIdToNameMap(connection);

            FlightCounts fc = getFlightCounts(connection, startDate, endDate);
            Map<String, EventCountsBuilder> eventCounts = new HashMap<>();

            try (ResultSet result = statement.executeQuery()) {

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
            }

            return eventCounts
                    .entrySet()
                    .stream()
                    .collect(
                            Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue().build()));
        }
    }

    public static Map<String, Map<String, MonthlyEventCounts>> getMonthlyEventCounts(Connection connection, int fleetId,
            LocalDate startDateNullable, LocalDate endDateNullable) throws SQLException {
        final LocalDate startDate = startDateNullable == null ? LocalDate.of(0, 1, 1) : startDateNullable;
        final LocalDate endDate = endDateNullable == null ? LocalDate.now() : endDateNullable;

        String query = MONTHLY_EVENT_COUNT_QUERY_PARAMS
                + EVENT_COUNT_BASE_QUERY_CONDITIONS
                + EVENT_COUNT_BETWEEN_DATE_CLAUSE
                + MONTHLY_EVENT_COUNT_QUERY_GROUP_BY;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, startDate.toString());
            statement.setString(2, endDate.toString());

            Map<Integer, String> idToAirframeNameMap = Airframes.getIdToNameMap(connection);
            Map<Integer, String> idToEventNameMap = EventDefinition.getEventDefinitionIdToNameMap(connection);

            FlightCounts fc = getFlightCounts(connection, startDate, endDate);
            Map<String, Map<String, MonthlyEventCountsBuilder>> eventCounts = new HashMap<>();

            try (ResultSet result = statement.executeQuery()) {
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
                            .computeIfAbsent(airframeName,
                                    k -> new MonthlyEventCountsBuilder(airframeName, eventName, startDate, endDate));

                    mec.updateAggregate(date, flightCount, totalFlights, eventCount);
                    if (fleetId == fleet)
                        mec.update(date, flightCount, totalFlights, eventCount);
                }
            }

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
                                                            e -> e.getValue().build()))));
        }
    }

    public static class FlightCounts {

        // Maps airframeId to another map, which maps fleetId to the number of flights
        // in that fleet.
        private Map<Integer, Map<Integer, Integer>> airframeToFleetCounts = new HashMap<>();

        // Maps fleetId to another map, which maps airframeId to the number of flights
        // of that airframe type in the specified fleet.
        private Map<Integer, Map<Integer, Integer>> fleetToAirframeCounts = new HashMap<>();

        // Total flight counts for all fleets. Maps airframe to the number of flights
        // with that airframe.
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
     * Returns a map of airframe id to the total number of flights with that id, one
     * map per fleet id.
     **/
    public static FlightCounts getFlightCounts(Connection connection, LocalDate startDate, LocalDate endDate)
            throws SQLException {
        if (startDate == null)
            startDate = LocalDate.of(0, 1, 1);

        if (endDate == null)
            endDate = LocalDate.now();

        String query = "SELECT COUNT(DISTINCT id) as flight_count, airframe_id, fleet_id FROM flights WHERE start_time BETWEEN ? AND ? GROUP BY flights.airframe_id, flights.fleet_id ";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, startDate.toString());
            ps.setString(2, endDate.toString());

            try (ResultSet results = ps.executeQuery()) {
                return new FlightCounts(results);
            }
        }
    }

    public static void updateMonthlyTotalFlights(Connection connection, int fleetId) throws SQLException {
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

        }
    }

    public static String getFirstOfMonth(String dateTime) {
        return dateTime.substring(0, 8) + "01";
    }

    public static void updateEventStatistics(Connection connection, int fleetId, int airframeNameId, int eventId,
            String startDateTime, double severity, double duration) throws SQLException {
        if (startDateTime.length() < 8) {
            LOG.severe("could not update event statistics because startDateTime was improperly formatted!");
            System.exit(1);
        }
        String firstOfMonth = getFirstOfMonth(startDateTime);

        String query = "INSERT INTO event_statistics (fleet_id, airframe_id, event_definition_id, month_first_day, total_events, min_severity, sum_severity, max_severity, min_duration, sum_duration, max_duration) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE min_severity = LEAST(min_severity, ?), sum_severity = sum_severity + ?, max_severity = GREATEST(max_severity, ?), min_duration = LEAST(min_duration, ?), sum_duration = sum_duration + ?, max_duration = GREATEST(max_duration, ?), total_events = total_events + 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
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
        }
    }

    public static void updateFlightsWithEvent(Connection connection, int fleetId, int airframeNameId, int eventId,
            String startDateTime) throws SQLException {
        // cannot update event statistics if the flight had no startDateTime
        if (startDateTime == null)
            return;

        String firstOfMonth = getFirstOfMonth(startDateTime);

        String query = "INSERT INTO event_statistics (fleet_id, airframe_id, event_definition_id, month_first_day, flights_with_event, total_flights, min_severity, sum_severity, max_severity, min_duration, sum_duration, max_duration) VALUES (?, ?, ?, ?, 1, 1, 999999, 0, -999999, 999999, 0, -999999) ON DUPLICATE KEY UPDATE flights_with_event = flights_with_event + 1, total_flights = total_flights + 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setInt(2, airframeNameId);
            preparedStatement.setInt(3, eventId);
            preparedStatement.setString(4, firstOfMonth);

            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }
    }

    public static void updateFlightsWithoutEvent(Connection connection, int fleetId, int airframeNameId, int eventId,
            String startDateTime) throws SQLException {
        // cannot update event statistics if the flight had no startDateTime
        if (startDateTime == null)
            return;

        String firstOfMonth = getFirstOfMonth(startDateTime);

        String query = "INSERT INTO event_statistics (fleet_id, airframe_id, event_definition_id, month_first_day, flights_with_event, total_flights, min_severity, sum_severity, max_severity, min_duration, sum_duration, max_duration) VALUES (?, ?, ?, ?, 0, 1, 999999, 0, -999999, 999999, 0, -999999) ON DUPLICATE KEY UPDATE total_flights = total_flights + 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setInt(2, airframeNameId);
            preparedStatement.setInt(3, eventId);
            preparedStatement.setString(4, firstOfMonth);

            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }
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

        public static EventRow getStatistics(Connection connection, String rowName, int fleetId, int eventId,
                String extraQuery, int[] extraParams) throws SQLException {
            EventRow eventRow = new EventRow(rowName);

            String query = "SELECT SUM(flights_with_event), SUM(total_flights), SUM(total_events), MIN(min_duration), SUM(sum_duration), MAX(max_duration), MIN(min_severity), SUM(sum_severity), MAX(max_severity) FROM event_statistics WHERE fleet_id = ? AND event_definition_id = ?";

            if (!extraQuery.equals("")) {
                query += " AND " + extraQuery;
            }

            // LOG.info(query);

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, fleetId);
                preparedStatement.setInt(2, eventId);

                for (int i = 0; i < extraParams.length; i++) {
                    preparedStatement.setInt(3 + i, extraParams[i]);
                }

                try (ResultSet resultSet = preparedStatement.executeQuery()) {

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
                }
            }

            query = "SELECT SUM(flights_with_event), SUM(total_flights), SUM(total_events), MIN(min_duration), SUM(sum_duration), MAX(max_duration), MIN(min_severity), SUM(sum_severity), MAX(max_severity) FROM event_statistics WHERE fleet_id != ? AND event_definition_id = ?";

            if (!extraQuery.equals("")) {
                query += " AND " + extraQuery;
            }

            // LOG.info(query);

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, fleetId);
                preparedStatement.setInt(2, eventId);
                for (int i = 0; i < extraParams.length; i++) {
                    preparedStatement.setInt(3 + i, extraParams[i]);
                }
                // LOG.info(preparedStatement.toString());

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
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

                    // zero out the default values if there are no events
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

                    if (Double.isNaN(eventRow.avgDuration))
                        eventRow.avgDuration = 0;
                    if (Double.isNaN(eventRow.avgEvents))
                        eventRow.avgEvents = 0;
                    if (Double.isNaN(eventRow.avgSeverity))
                        eventRow.avgSeverity = 0;

                    if (Double.isNaN(eventRow.aggAvgDuration))
                        eventRow.aggAvgDuration = 0;
                    if (Double.isNaN(eventRow.aggAvgEvents))
                        eventRow.aggAvgEvents = 0;
                    if (Double.isNaN(eventRow.aggAvgSeverity))
                        eventRow.aggAvgSeverity = 0;

                    return eventRow;
                }
            }
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

            if (airframeNameId == 0) {
                query = "SELECT count(*) FROM flights WHERE fleet_id = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.setInt(1, fleetId);

                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        resultSet.next();
                        totalFlights = resultSet.getInt(1);
                    }
                }

                query = "SELECT count(*) FROM flight_processed WHERE flight_processed.fleet_id = ? AND flight_processed.event_definition_id = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.setInt(1, fleetId);
                    preparedStatement.setInt(2, eventId);

                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        processedFlights = resultSet.getInt(1);
                        resultSet.next();
                    }
                }

            } else {
                query = "SELECT count(*) FROM flights WHERE fleet_id = ? AND airframe_id = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.setInt(1, fleetId);
                    preparedStatement.setInt(2, airframeNameId);

                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        resultSet.next();
                        totalFlights = resultSet.getInt(1);
                    }
                }

                query = "SELECT count(*) FROM flight_processed INNER JOIN flights ON flights.airframe_id = ? AND flights.id = flight_processed.flight_id WHERE flight_processed.fleet_id = ? AND flight_processed.event_definition_id = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.setInt(1, airframeNameId);
                    preparedStatement.setInt(2, fleetId);
                    preparedStatement.setInt(3, eventId);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        resultSet.next();
                        processedFlights = resultSet.getInt(1);
                    }
                }
            }

            int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);

            // LOG.info("current year: " + currentYear + ", current month: " +
            // currentMonth);

            monthStats.add(EventRow.getStatistics(connection, "Month to Date", fleetId, eventId,
                    "YEAR(month_first_day) >= ? AND MONTH(month_first_day) >= ?",
                    new int[] { currentYear, currentMonth }));

            int previousMonth = currentMonth - 1;
            int tempYear = currentYear;
            if (currentMonth == 0) {
                currentMonth = 12;
                tempYear = currentYear - 1;
            }

            monthStats.add(EventRow.getStatistics(connection, "Previous Month", fleetId, eventId,
                    "YEAR(month_first_day) = ? AND MONTH(month_first_day) = ?",
                    new int[] { currentYear, currentMonth }));

            monthStats.add(EventRow.getStatistics(connection, "Year to Date", fleetId, eventId,
                    "YEAR(month_first_day) >= ? AND MONTH(month_first_day) >= ?", new int[] { currentYear, 1 }));

            monthStats.add(EventRow.getStatistics(connection, "Previous Year", fleetId, eventId,
                    "YEAR(month_first_day) = ?", new int[] { currentYear - 1 }));

            monthStats.add(EventRow.getStatistics(connection, "Overall", fleetId, eventId, "", new int[] {}));
        }
    }

    int airframeNameId;
    String airframeName;
    ArrayList<AirframeStatistics> events;

    public EventStatistics(Connection connection, int airframeNameId, String airframeName, int fleetId)
            throws SQLException {
        this.airframeNameId = airframeNameId;
        this.airframeName = airframeName;
        events = new ArrayList<>();

        ArrayList<EventDefinition> eventDefinitions = EventDefinition.getAll(connection,
                "airframe_id = ? AND  (fleet_id = 0 OR fleet_id = ?)", new Object[] { airframeNameId, fleetId });

        events = new ArrayList<>();
        for (int i = 0; i < eventDefinitions.size(); i++) {

            events.add(new AirframeStatistics(connection, eventDefinitions.get(i), fleetId));
        }
    }

    public static ArrayList<EventStatistics> getAll(Connection connection, int fleetId) throws SQLException {
        String query = "SELECT id, airframe FROM airframes INNER JOIN fleet_airframes ON airframes.id = fleet_airframes.airframe_id WHERE fleet_airframes.fleet_id = ? ORDER BY airframe";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, fleetId);

            ArrayList<EventStatistics> eventStatistics = new ArrayList<>();

            long startTime = System.currentTimeMillis();
            eventStatistics.add(new EventStatistics(connection, 0, "Generic", fleetId));
            long endTime = System.currentTimeMillis();
            LOG.info("Took " + (endTime - startTime) + " ms to calculate event statistics for airframeNameId: " + 0);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                // get the event statistics for each airframe
                while (resultSet.next()) {
                    startTime = System.currentTimeMillis();
                    int airframeNameId = resultSet.getInt(1);
                    String airframeName = resultSet.getString(2);
                    eventStatistics.add(new EventStatistics(connection, airframeNameId, airframeName, fleetId));
                    endTime = System.currentTimeMillis();

                    LOG.info("Took " + (endTime - startTime) + " ms to calculate event statistics for airframeNameId: "
                            + airframeNameId);
                }

                return eventStatistics;
            }
        }
    }

    /**
     * Gets the number of exceedences for the entire NGAFID between two dates. If
     * either date is null it will
     * select greater than or less than the date specified. If both are null it gets
     * the total amount
     * of exceedences.
     *
     * @param connection is the connection to the database
     * @param startTime  is the earliest time to start counting events (it will
     *                   count from the beginning of time if it is null)
     * @param endTime    is the latest time to count events (it will count until the
     *                   current date if it is null)
     * @return the number of events between the two given times
     */
    public static int getEventCount(Connection connection, LocalDate startTime, LocalDate endTime)
            throws SQLException {
        return getEventCount(connection, 0, startTime, endTime);
    }

    /**
     * Gets the number of exceedences for a given fleet between two dates. If either
     * is null it will
     * select greater than or less than the date specified. If both are null it gets
     * the total amount
     * of exceedences.
     *
     * @param connection is the connection to the database
     * @param fleetId    is the id of the fleet, if fleetId <= 0 it will return for
     *                   the entire NGAFID
     * @param startTime  is the earliest time to start counting events (it will
     *                   count from the beginning of time if it is null)
     * @param endTime    is the latest time to count events (it will count until the
     *                   current date if it is null)
     * @return the number of events between the two given times
     */
    public static int getEventCount(Connection connection, int fleetId, LocalDate startTime, LocalDate endTime)
            throws SQLException {
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

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
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

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
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
                int[] aggregateFlightsWithEventCounts, int[] aggregateTotalFlightsCounts,
                int[] aggregateTotalEventsCounts) {
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
                int[] aggregateFlightsWithEventCounts, int[] aggregateTotalFlightsCounts,
                int[] aggregateTotalEventsCounts) {
            super(flightsWithEventCounts, totalFlightsCounts, totalEventsCounts, aggregateFlightsWithEventCounts,
                    aggregateTotalFlightsCounts, aggregateTotalEventsCounts);

            this.airframeName = airframeName;
            this.eventName = eventName;
            this.dates = dates;
        }
    }

    public static class MonthlyEventCountsBuilder extends EventCountsWithAggregateBuilder<MonthlyEventCounts> {
        final String airframeName;
        final String eventName;

        final List<String> dates;

        public MonthlyEventCountsBuilder(String airframeName, String eventName, LocalDate startDate,
                LocalDate endDate) {
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
                    linearize(aggregateFlightsWithEventMap), linearize(aggregateTotalFlightsMap),
                    linearize(aggregateTotalEventsMap));
        }

    }

    public static class EventCounts extends EventCountsWithAggregate {
        public final String airframeName;
        public final List<String> names;

        public EventCounts(
                String airframeName, List<String> names,
                int[] flightsWithEventCounts, int[] totalFlightsCounts, int[] totalEventsCounts,
                int[] aggregateFlightsWithEventCounts, int[] aggregateTotalFlightsCounts,
                int[] aggregateTotalEventsCounts) {
            super(flightsWithEventCounts, totalFlightsCounts, totalEventsCounts, aggregateFlightsWithEventCounts,
                    aggregateTotalFlightsCounts, aggregateTotalEventsCounts);

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
                    linearize(sortedKeys, flightsWithEventMap), linearize(sortedKeys, totalFlightsMap),
                    linearize(sortedKeys, totalEventsMap),
                    linearize(sortedKeys, aggregateFlightsWithEventMap),
                    linearize(sortedKeys, aggregateTotalFlightsMap),
                    linearize(sortedKeys, aggregateTotalEventsMap));
        }
    }

}
