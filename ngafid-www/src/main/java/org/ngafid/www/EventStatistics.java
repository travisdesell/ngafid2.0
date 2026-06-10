package org.ngafid.www;

import static org.ngafid.www.routes.StatisticsJavalinRoutes.buildDateAirframeClause;
import static org.ngafid.www.routes.StatisticsJavalinRoutes.buildDateClause;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ngafid.core.event.EventDefinition;
import org.ngafid.core.flights.Airframes;
import org.ngafid.core.util.TimeUtils;
import org.ngafid.www.flights.FlightStatistics;

public class EventStatistics {
    private static final Logger LOG = Logger.getLogger(EventStatistics.class.getName());

    private final int airframeNameId;
    private final String airframeName;
    private ArrayList<AirframeStatistics> events;

    public EventStatistics(Connection connection, int airframeNameId, String airframeName, int fleetId)
            throws SQLException {
        this.airframeNameId = airframeNameId;
        this.airframeName = airframeName;
        events = new ArrayList<>();

        // TODO: We should store all event defs and just filter them
        ArrayList<EventDefinition> eventDefinitions = EventDefinition.getAll(
                connection, "airframe_id = ? AND (fleet_id = 0 OR fleet_id = ?)", new Object[] {airframeNameId, fleetId
                });

        events = new ArrayList<>();
        for (int i = 0; i < eventDefinitions.size(); i++) {
            events.add(new AirframeStatistics(connection, eventDefinitions.get(i), fleetId));
        }
    }

    // NOTES: You are going to have to mess with the js code that calls this,
    // because it wont look in the aggregate fields for osme reason.
    // this isnt a problem w the monthly stuff.
    public static Map<String, EventCounts> getEventCounts(Connection connection, LocalDate startTime, LocalDate endTime)
            throws SQLException {
        return getEventCounts(connection, -1, startTime, endTime);
    }

    public static Map<String, EventCounts> getEventCounts(
            Connection connection, int fleetId, LocalDate startDate, LocalDate endDate) throws SQLException {
        if (startDate == null) startDate = LocalDate.of(0, 1, 1);

        if (endDate == null) endDate = LocalDate.now();

        final String dateClause = buildDateClause(startDate, endDate);

        /*
            Only include events that occur at least once in the
            selected date range.

            We still want airframes with 0 occurrences to be
            present for those events, otherwise percentages can
            appear unexpectedly high for events that only occur
            on a small subset of airframes.
        */
        final Set<Integer> eventDefinitionIdsInRange = new HashSet<>();

        final String distinctEventIdsQuery =
                "SELECT DISTINCT event_definition_id FROM m_fleet_airframe_monthly_event_counts WHERE " + dateClause;

        String query = """
            SELECT fleet_id, event_definition_id, airframe_id,
                SUM(event_count) as event_count, SUM(flight_count) as flight_count
            FROM m_fleet_airframe_monthly_event_counts
            WHERE %s
            GROUP BY fleet_id, event_definition_id, airframe_id
        """.formatted(dateClause);

        Map<Integer, String> idToAirframeNameMap = Airframes.getIdToNameMap(connection);
        Map<Integer, String> idToEventNameMap = EventDefinition.getEventDefinitionIdToNameMap(connection);

        try (PreparedStatement distinctEventIdsStatement = connection.prepareStatement(distinctEventIdsQuery);
                ResultSet distinctEventIdsResult = distinctEventIdsStatement.executeQuery()) {

            while (distinctEventIdsResult.next()) {
                eventDefinitionIdsInRange.add(distinctEventIdsResult.getInt(1));
            }
        }

        final List<String> eventNamesInRange = eventDefinitionIdsInRange.stream()
                .map(idToEventNameMap::get)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        FlightCounts fc = getFlightCounts(connection, startDate, endDate);
        Map<String, EventCountsBuilder> eventCounts = new HashMap<>();

        /*
            Seed (airframe, eventName) keys for every airframe that
            has flights in range.

            Prevents downstream percentage calculations from dropping
            airframes with 0 occurrences.
        */
        for (Map.Entry<Integer, Integer> entry : fc.getAggregateCounts().entrySet()) {

            final int airframeId = entry.getKey();
            final int aggregateTotalFlights = entry.getValue();

            final String airframeName = idToAirframeNameMap.get(airframeId);
            if (airframeName == null) {
                LOG.log(
                        Level.WARNING,
                        "Got null airframe name for id {0} while seeding event counts, skipping",
                        airframeId);
                continue;
            }

            final EventCountsBuilder ecBuilder = eventCounts.computeIfAbsent(airframeName, EventCountsBuilder::new);
            final Map<Integer, Integer> fleetCounts = fleetId >= 0 ? fc.getFleetCounts(fleetId) : null;
            final int fleetTotalFlights = (fleetCounts == null) ? 0 : fleetCounts.getOrDefault(airframeId, 0);

            for (String eventName : eventNamesInRange) {

                ecBuilder.ensureEventKey(eventName);
                ecBuilder.setAggregateTotalFlights(eventName, aggregateTotalFlights);

                if (fleetId >= 0) ecBuilder.setTotalFlights(eventName, fleetTotalFlights);
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(query);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                int fleet = result.getInt("fleet_id");
                int eventCount = result.getInt("event_count");
                int flightCount = result.getInt("flight_count");
                int eventDefinitionId = result.getInt("event_definition_id");
                int airframeId = result.getInt("airframe_id");

                // Fetch event name, skip when null
                String eventName = idToEventNameMap.get(eventDefinitionId);
                if (eventName == null) {
                    LOG.log(Level.WARNING, "Got null event name for id {0}, skipping", eventDefinitionId);
                    continue;
                }

                // Fetch airframe name, skip when null
                String airframeName = idToAirframeNameMap.get(airframeId);
                if (airframeName == null) {
                    LOG.log(Level.WARNING, "Got null airframe name for id {0}, skipping", airframeId);
                    continue;
                }

                EventCountsBuilder ec = eventCounts.computeIfAbsent(airframeName, EventCountsBuilder::new);
                ec.updateAggregate(eventName, flightCount, 0, eventCount);
                if (fleetId == fleet) ec.update(eventName, flightCount, 0, eventCount);
            }

            return eventCounts.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()
                    .build()));
        }
    }

    public static Map<String, Map<String, MonthlyEventCounts>> getMonthlyEventCounts(
            Connection connection, int fleetId, LocalDate startDateNullable, LocalDate endDateNullable)
            throws SQLException {
        Map<Integer, String> idToAirframeNameMap = Airframes.getIdToNameMap(connection);
        Map<Integer, String> idToEventNameMap = EventDefinition.getEventDefinitionIdToNameMap(connection);

        return getMonthlyEventCounts(
                connection, fleetId, startDateNullable, endDateNullable, idToAirframeNameMap, idToEventNameMap);
    }

    static Map<String, Map<String, MonthlyEventCounts>> getMonthlyEventCounts(
            Connection connection,
            int fleetId,
            LocalDate startDateNullable,
            LocalDate endDateNullable,
            Map<Integer, String> idToAirframeNameMap,
            Map<Integer, String> idToEventNameMap)
            throws SQLException {
        final LocalDate startDate = startDateNullable == null ? LocalDate.of(0, 1, 1) : startDateNullable;
        final LocalDate endDate = endDateNullable == null ? LocalDate.now() : endDateNullable;

        final String dateClause = buildDateClause(startDate, endDate);

        String query =
                "SELECT year, month, fleet_id, event_definition_id, airframe_id, "
                        + "SUM(event_count) as event_count, SUM(flight_count) as flight_count "
                        + "FROM m_fleet_airframe_monthly_event_counts WHERE "
                        + dateClause + " GROUP BY fleet_id, event_definition_id, airframe_id, year, month";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            Map<MonthlyCountKey, Integer> fleetMonthlyFlightCounts = getFleetMonthlyFlightCounts(connection, dateClause);
            Map<MonthlyCountKey, Integer> aggregateMonthlyFlightCounts =
                    getAggregateMonthlyFlightCounts(connection, dateClause);
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
                    final int totalFlights = fleetMonthlyFlightCounts.getOrDefault(
                            monthlyCountKey(fleet, airframeId, year, month), 0);
                    final int aggregateTotalFlights = aggregateMonthlyFlightCounts.getOrDefault(
                            monthlyCountKey(-1, airframeId, year, month), 0);

                    final String date = LocalDate.of(year, month, 1).toString();
                    final String eventName = idToEventNameMap.get(eventDefinitionId);
                    final String airframeName = idToAirframeNameMap.get(airframeId);

                    MonthlyEventCountsBuilder mec = eventCounts
                            .computeIfAbsent(eventName, k -> new HashMap<>())
                            .computeIfAbsent(
                                    airframeName,
                                    k -> new MonthlyEventCountsBuilder(airframeName, eventName, startDate, endDate));

                    mec.updateAggregateWithTotalFlightsMax(date, flightCount, aggregateTotalFlights, eventCount);
                    if (fleetId == fleet) mec.updateWithTotalFlightsMax(date, flightCount, totalFlights, eventCount);
                }
            }

            addAnyEventMonthlyCounts(
                    connection,
                    fleetId,
                    startDate,
                    endDate,
                    dateClause,
                    idToAirframeNameMap,
                    fleetMonthlyFlightCounts,
                    aggregateMonthlyFlightCounts,
                    eventCounts);

            seedMonthlyFlightDenominators(
                    fleetId,
                    startDate,
                    endDate,
                    idToAirframeNameMap,
                    fleetMonthlyFlightCounts,
                    aggregateMonthlyFlightCounts,
                    eventCounts);

            return eventCounts.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey, e -> e.getValue().build()))));
        }
    }

    private record MonthlyCountKey(int fleetId, int airframeId, int year, int month) {}

    private static MonthlyCountKey monthlyCountKey(int fleetId, int airframeId, int year, int month) {
        return new MonthlyCountKey(fleetId, airframeId, year, month);
    }

    private static Map<MonthlyCountKey, Integer> getFleetMonthlyFlightCounts(Connection connection, String dateClause)
            throws SQLException {
        String query = """
            SELECT fleet_id, airframe_id, year, month, SUM(count) AS flight_count
            FROM m_fleet_monthly_flight_counts
            WHERE %s
            GROUP BY fleet_id, airframe_id, year, month
        """.formatted(dateClause);

        Map<MonthlyCountKey, Integer> counts = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(query);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                counts.put(
                        monthlyCountKey(
                                result.getInt("fleet_id"),
                                result.getInt("airframe_id"),
                                result.getInt("year"),
                                result.getInt("month")),
                        result.getInt("flight_count"));
            }
        }
        return counts;
    }

    private static Map<MonthlyCountKey, Integer> getAggregateMonthlyFlightCounts(Connection connection, String dateClause)
            throws SQLException {
        String query = """
            SELECT airframe_id, year, month, SUM(count) AS flight_count
            FROM m_fleet_monthly_flight_counts
            WHERE %s
            GROUP BY airframe_id, year, month
        """.formatted(dateClause);

        Map<MonthlyCountKey, Integer> counts = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(query);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                counts.put(
                        monthlyCountKey(
                                -1,
                                result.getInt("airframe_id"),
                                result.getInt("year"),
                                result.getInt("month")),
                        result.getInt("flight_count"));
            }
        }
        return counts;
    }

    private static void addAnyEventMonthlyCounts(
            Connection connection,
            int selectedFleetId,
            LocalDate startDate,
            LocalDate endDate,
            String dateClause,
            Map<Integer, String> idToAirframeNameMap,
            Map<MonthlyCountKey, Integer> fleetMonthlyFlightCounts,
            Map<MonthlyCountKey, Integer> aggregateMonthlyFlightCounts,
            Map<String, Map<String, MonthlyEventCountsBuilder>> eventCounts)
            throws SQLException {

        final String anyEventName = "ANY Event";
        Map<String, MonthlyEventCountsBuilder> anyEventCounts =
                eventCounts.computeIfAbsent(anyEventName, k -> new HashMap<>());

        if (selectedFleetId >= 0) {
            String fleetQuery = """
                SELECT fleet_id, airframe_id, year, month,
                    COUNT(event_id) AS event_count,
                    COUNT(DISTINCT flight_id) AS flight_count
                FROM (
                    SELECT e.fleet_id, f.airframe_id, YEAR(e.start_time) AS year, MONTH(e.start_time) AS month,
                        e.id AS event_id, f.id AS flight_id
                    FROM events e
                    INNER JOIN flights f ON f.id = e.flight_id
                    WHERE e.fleet_id = ?
                ) monthly_events
                WHERE %s
                GROUP BY fleet_id, airframe_id, year, month
            """.formatted(dateClause);

            try (PreparedStatement statement = connection.prepareStatement(fleetQuery)) {
                statement.setInt(1, selectedFleetId);

                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        final int fleetId = result.getInt("fleet_id");
                        final int airframeId = result.getInt("airframe_id");
                        final int year = result.getInt("year");
                        final int month = result.getInt("month");
                        final String airframeName = idToAirframeNameMap.get(airframeId);
                        if (airframeName == null) continue;

                        final String date = LocalDate.of(year, month, 1).toString();
                        final int totalFlights = fleetMonthlyFlightCounts.getOrDefault(
                                monthlyCountKey(fleetId, airframeId, year, month), 0);

                        MonthlyEventCountsBuilder builder = anyEventCounts.computeIfAbsent(
                                airframeName,
                                k -> new MonthlyEventCountsBuilder(airframeName, anyEventName, startDate, endDate));
                        builder.updateWithTotalFlightsMax(
                                date, result.getInt("flight_count"), totalFlights, result.getInt("event_count"));
                    }
                }
            }
        }

        String aggregateQuery = """
            SELECT airframe_id, year, month,
                COUNT(event_id) AS event_count,
                COUNT(DISTINCT flight_id) AS flight_count
            FROM (
                SELECT f.airframe_id, YEAR(e.start_time) AS year, MONTH(e.start_time) AS month,
                    e.id AS event_id, f.id AS flight_id
                FROM events e
                INNER JOIN flights f ON f.id = e.flight_id
            ) monthly_events
            WHERE %s
            GROUP BY airframe_id, year, month
        """.formatted(dateClause);

        try (PreparedStatement statement = connection.prepareStatement(aggregateQuery);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                final int airframeId = result.getInt("airframe_id");
                final int year = result.getInt("year");
                final int month = result.getInt("month");
                final String airframeName = idToAirframeNameMap.get(airframeId);
                if (airframeName == null) continue;

                final String date = LocalDate.of(year, month, 1).toString();
                final int totalFlights = aggregateMonthlyFlightCounts.getOrDefault(
                        monthlyCountKey(-1, airframeId, year, month), 0);

                MonthlyEventCountsBuilder builder = anyEventCounts.computeIfAbsent(
                        airframeName,
                        k -> new MonthlyEventCountsBuilder(airframeName, anyEventName, startDate, endDate));
                builder.updateAggregate(
                        date, result.getInt("flight_count"), totalFlights, result.getInt("event_count"));
            }
        }
    }

    private static void seedMonthlyFlightDenominators(
            int selectedFleetId,
            LocalDate startDate,
            LocalDate endDate,
            Map<Integer, String> idToAirframeNameMap,
            Map<MonthlyCountKey, Integer> fleetMonthlyFlightCounts,
            Map<MonthlyCountKey, Integer> aggregateMonthlyFlightCounts,
            Map<String, Map<String, MonthlyEventCountsBuilder>> eventCounts) {

        final String anyEventName = "ANY Event";
        Map<String, MonthlyEventCountsBuilder> anyEventCounts =
                eventCounts.computeIfAbsent(anyEventName, k -> new HashMap<>());

        for (Map.Entry<MonthlyCountKey, Integer> entry : aggregateMonthlyFlightCounts.entrySet()) {
            final MonthlyCountKey key = entry.getKey();
            final String airframeName = idToAirframeNameMap.get(key.airframeId());
            if (airframeName == null) continue;

            anyEventCounts.computeIfAbsent(
                    airframeName,
                    k -> new MonthlyEventCountsBuilder(airframeName, anyEventName, startDate, endDate));

            final String date = LocalDate.of(key.year(), key.month(), 1).toString();
            for (Map<String, MonthlyEventCountsBuilder> countsByAirframe : eventCounts.values()) {
                MonthlyEventCountsBuilder builder = countsByAirframe.get(airframeName);
                if (builder != null) {
                    builder.updateAggregateWithTotalFlightsMax(date, 0, entry.getValue(), 0);
                }
            }
        }

        if (selectedFleetId < 0) return;

        for (Map.Entry<MonthlyCountKey, Integer> entry : fleetMonthlyFlightCounts.entrySet()) {
            final MonthlyCountKey key = entry.getKey();
            if (key.fleetId() != selectedFleetId) continue;

            final String airframeName = idToAirframeNameMap.get(key.airframeId());
            if (airframeName == null) continue;

            anyEventCounts.computeIfAbsent(
                    airframeName,
                    k -> new MonthlyEventCountsBuilder(airframeName, anyEventName, startDate, endDate));

            final String date = LocalDate.of(key.year(), key.month(), 1).toString();
            for (Map<String, MonthlyEventCountsBuilder> countsByAirframe : eventCounts.values()) {
                MonthlyEventCountsBuilder builder = countsByAirframe.get(airframeName);
                if (builder != null) {
                    builder.updateWithTotalFlightsMax(date, 0, entry.getValue(), 0);
                }
            }
        }
    }

    /**
     * Returns a map of airframe id to the total number of flights with that id, one
     * map per fleet id.
     *
     * @param connection is the connection to the database
     * @param startDate  is the earliest date to count flights from
     * @param endDate    is the latest date to count flights from
     * @return Flight counts
     **/
    public static FlightCounts getFlightCounts(Connection connection, LocalDate startDate, LocalDate endDate)
            throws SQLException {
        if (startDate == null) startDate = LocalDate.of(0, 1, 1);

        if (endDate == null) endDate = LocalDate.now();

        String query = "SELECT COUNT(DISTINCT id) as flight_count, airframe_id, fleet_id FROM flights WHERE "
                + "start_time BETWEEN ? AND ? GROUP BY flights.airframe_id, flights.fleet_id ";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, startDate.toString());
            ps.setString(2, endDate.toString());

            try (ResultSet results = ps.executeQuery()) {
                return new FlightCounts(results);
            }
        }
    }

    private static int getEventCount(Connection connection, String tableName, String condition) throws SQLException {
        String query = "SELECT SUM(event_count) FROM " + tableName;
        if (condition != null) query += " WHERE " + condition;

        try (PreparedStatement statement = connection.prepareStatement(query);
                ResultSet results = statement.executeQuery()) {
            if (results.next()) {
                return results.getInt(1);
            }

            throw new SQLException("Failed to read from table " + tableName + ":" + statement);
        }
    }

    /**
     * @param connection the database connection
     * @return the total number of events over all fleets, airframes, and event types so far for the current year.
     * @throws SQLException if the table is empty
     */
    public static int getAggregateCurrentYearEventCount(Connection connection) throws SQLException {
        return getEventCount(connection, "v_fleet_yearly_event_counts", " year = " + TimeUtils.getCurrentYearUTC());
    }

    /**
     * @param connection the database connection
     * @param fleetId the fleet identifier
     * @return the total number of events for the specified fleet over all airframes and event types so far
     *     for the current year.
     * @throws SQLException if the table is empty
     */
    public static int getCurrentYearEventCount(Connection connection, int fleetId) throws SQLException {
        return getEventCount(
                connection,
                "v_fleet_yearly_event_counts",
                "fleet_id = " + fleetId + " AND year = " + TimeUtils.getCurrentYearUTC());
    }

    /**
     * @param connection the database connection
     * @return the total number of events over all fleets, airframes, and event types so far for the current month.
     * @throws SQLException if the table is empty
     */
    public static int getAggregateCurrentMonthEventCount(Connection connection) throws SQLException {
        return getEventCount(
                connection,
                "v_aggregate_monthly_event_counts",
                " year = " + TimeUtils.getCurrentYearUTC() + " AND month = " + TimeUtils.getCurrentMonthUTC());
    }

    /**
     * @param connection the database connection
     * @param fleetId the fleet identifier
     * @return the total number of events for the specified fleet over all airframes and event types so far
     *     for the current month.
     * @throws SQLException if the table is empty
     */
    public static int getCurrentMonthEventCount(Connection connection, int fleetId) throws SQLException {
        return getEventCount(
                connection,
                "v_fleet_monthly_event_counts",
                "fleet_id = " + fleetId + " AND year = " + TimeUtils.getCurrentYearUTC() + " AND month = "
                        + TimeUtils.getCurrentMonthUTC());
    }

    /**
     * @param connection the database connection
     * @return the total number of events over all fleets, airframes, and event types -- every event in the database.
     * @throws SQLException if the table is empty
     */
    public static int getAggregateTotalEventCount(Connection connection) throws SQLException {
        return getEventCount(connection, "v_aggregate_total_event_count", null);
    }

    public static int getAggregateTotalEventCountDated(Connection connection, LocalDate startDate, LocalDate endDate)
            throws SQLException {
        return getAggregateTotalEventCountDated(connection, startDate, endDate, -1);
    }

    public static int getAggregateTotalEventCountDated(
            Connection connection, LocalDate startDate, LocalDate endDate, int airframeID) throws SQLException {

        // final String dateClause = buildDateClause(startDate, endDate);
        // final String dateAirframeClause = buildDateAirframeClause(startDate, endDate, airframeName);

        String clause;
        if (airframeID < 0) clause = buildDateClause(startDate, endDate);
        else clause = buildDateAirframeClause(startDate, endDate, airframeID);

        return getEventCount(connection, "v_aggregate_total_event_counts_dated", clause);
    }

    /**
     * @param connection the database connection
     * @param fleetId the fleet identifier
     * @return the total number of events for this fleet over all airframes and event types -- every event in
     *     the database associated with this fleet.
     * @throws SQLException if the table is empty
     */
    public static int getTotalEventCount(Connection connection, int fleetId) throws SQLException {
        return getEventCount(connection, "v_fleet_total_event_counts", "fleet_id = " + fleetId);
    }

    public static int getTotalEventCountDated(
            Connection connection, int fleetId, LocalDate startDate, LocalDate endDate) throws SQLException {
        return getTotalEventCountDated(connection, fleetId, startDate, endDate, -1);
    }

    public static int getTotalEventCountDated(
            Connection connection, int fleetId, LocalDate startDate, LocalDate endDate, int airframeID)
            throws SQLException {

        String clause;
        if (airframeID < 0) clause = buildDateClause(startDate, endDate);
        else clause = buildDateAirframeClause(startDate, endDate, airframeID);

        return getEventCount(
                connection, "v_fleet_total_event_counts_dated", "fleet_id = " + fleetId + " AND " + clause);
    }

    public static class FlightCounts {

        // Maps airframeId to another map, which maps fleetId to the number of flights
        // in that fleet.
        private final Map<Integer, Map<Integer, Integer>> airframeToFleetCounts = new HashMap<>();

        // Maps fleetId to another map, which maps airframeId to the number of flights
        // of that airframe type in the specified fleet.
        private final Map<Integer, Map<Integer, Integer>> fleetToAirframeCounts = new HashMap<>();

        // Total flight counts for all fleets. Maps airframe to the number of flights
        // with that airframe.
        private final Map<Integer, Integer> aggregateCounts = new HashMap<>();

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

            aggregateCounts.merge(airframeId, flightCount, Integer::sum);
        }

        public Map<Integer, Integer> getAggregateCounts() {
            return aggregateCounts;
        }

        public Map<Integer, Integer> getFleetCounts(int fleetId) {
            return fleetToAirframeCounts.get(fleetId);
        }
    }

    private static class EventRow {
        private final String rowName;

        private int flightsWithoutError;
        private int flightsWithEvent;
        private int totalEvents;
        private double avgEvents;
        private double avgDuration;
        private double minDuration;
        private double maxDuration;
        private double avgSeverity;
        private double minSeverity;
        private double maxSeverity;

        private int aggFlightsWithoutError;
        private int aggFlightsWithEvent;
        private int aggTotalEvents;
        private double aggAvgEvents;
        private double aggAvgDuration;
        private double aggMinDuration;
        private double aggMaxDuration;
        private double aggAvgSeverity;
        private double aggMinSeverity;
        private double aggMaxSeverity;

        private static final String COLUMNS =
                "event_count, flight_count, min_duration, avg_duration, max_duration, "
                        + "min_severity, avg_severity, max_severity";

        /**
         * Creates an event row for the given fleet and event id and any additional specified conditions,
         * pulled from the supplied table names
         *
         * @param connection the database connection
         * @param rowName the name for the row
         * @param flightsWithoutError the number of flights without errors
         * @param aggFlightsWithoutError the aggregate number of flights without errors
         * @param fleetId the fleet identifier
         * @param eventId the event identifier
         * @param airframeId the airframe identifier
         * @param tableName table name for the fleet event statistics.
         * @param aggTableName table name for the aggregate event statistics
         * @param conditions extra SQL conditions
         * @throws SQLException if no row is found
         */
        EventRow(
                Connection connection,
                String rowName,
                int flightsWithoutError,
                int aggFlightsWithoutError,
                int fleetId,
                int eventId,
                int airframeId,
                String tableName,
                String aggTableName,
                String conditions)
                throws SQLException {
            this.flightsWithoutError = flightsWithoutError;
            this.aggFlightsWithoutError = aggFlightsWithoutError;

            this.rowName = rowName;
            String c = "fleet_id = " + fleetId + " AND event_definition_id = " + eventId;
            if (airframeId > 0) {
                c += " AND airframe_id = " + airframeId;
            }
            if (conditions != null) {
                c += " AND " + conditions;
            }

            String query = "SELECT " + COLUMNS + " FROM " + tableName + " WHERE " + c;

            try (PreparedStatement statement = connection.prepareStatement(query);
                    ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    this.totalEvents = results.getInt(1);
                    this.flightsWithEvent = results.getInt(2);
                    this.minDuration = results.getDouble(3);
                    this.avgDuration = results.getDouble(4);
                    this.maxDuration = results.getDouble(5);
                    this.minSeverity = results.getDouble(6);
                    this.avgSeverity = results.getDouble(7);
                    this.maxSeverity = results.getDouble(8);
                }
            }

            c = " event_definition_id = " + eventId;
            if (conditions != null) {
                c += " AND " + conditions;
            }

            query = "SELECT " + COLUMNS + " FROM " + aggTableName + " WHERE " + c;

            try (PreparedStatement statement = connection.prepareStatement(query);
                    ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    this.aggTotalEvents = results.getInt(1);
                    this.aggFlightsWithEvent = results.getInt(2);
                    this.aggMinDuration = results.getDouble(3);
                    this.aggAvgDuration = results.getDouble(4);
                    this.aggMaxDuration = results.getDouble(5);
                    this.aggMinSeverity = results.getDouble(6);
                    this.aggAvgSeverity = results.getDouble(7);
                    this.aggMaxSeverity = results.getDouble(8);
                }
            }
        }

        /**
         * @param connection the database connection
         * @param rowName the name for the row
         * @param fleetId the fleet identifier
         * @param eventDefinitionId the event definition identifier
         * @param airframeId the airframe identifier
         * @param year full 4-digit year
         * @param month month (1-12)
         * @return event row for the supplied fleet and event definition during the given month
         * @throws SQLException if no row is found
         */
        public static EventRow eventRowMonth(
                Connection connection,
                String rowName,
                int fleetId,
                int eventDefinitionId,
                int airframeId,
                int year,
                int month)
                throws SQLException {
            String tableName =
                    airframeId > 0 ? "m_fleet_airframe_monthly_event_counts" : "v_fleet_monthly_event_counts";
            String aggTableName =
                    airframeId > 0 ? "v_aggregate_airframe_monthly_event_counts" : "v_aggregate_monthly_event_counts";
            int totalFlights = FlightStatistics.getMonthFlightCount(connection, fleetId, airframeId, year, month);
            int aggTotalFlights = FlightStatistics.getAggregateMonthFlightCount(connection, airframeId, year, month);
            return new EventRow(
                    connection,
                    rowName,
                    totalFlights,
                    aggTotalFlights,
                    fleetId,
                    eventDefinitionId,
                    airframeId,
                    tableName,
                    aggTableName,
                    " year = " + year + " AND month = " + month);
        }

        /**
         * @param connection the database connection
         * @param rowName the name for the row
         * @param fleetId the fleet identifier
         * @param eventDefinitionId the event definition identifier
         * @param airframeId the airframe identifier
         * @param year full 4 digit year
         * @return event row for the specified year
         * @throws SQLException if no row is found
         */
        public static EventRow eventRowYear(
                Connection connection, String rowName, int fleetId, int eventDefinitionId, int airframeId, int year)
                throws SQLException {
            String tableName = airframeId > 0 ? "v_fleet_airframe_yearly_event_counts" : "v_fleet_yearly_event_counts";
            String aggTableName =
                    airframeId > 0 ? "v_aggregate_airframe_yearly_event_counts" : "v_aggregate_yearly_event_counts";
            int totalFlights = FlightStatistics.getYearFlightCount(connection, fleetId, airframeId, year);
            int aggTotalFlights = FlightStatistics.getAggregateYearFlightCount(connection, airframeId, year);
            return new EventRow(
                    connection,
                    rowName,
                    totalFlights,
                    aggTotalFlights,
                    fleetId,
                    eventDefinitionId,
                    airframeId,
                    tableName,
                    aggTableName,
                    " year = " + year);
        }

        /**
         * @param connection the database connection
         * @param rowName the name for the row
         * @param fleetId the fleet identifier
         * @param eventDefinitionId the event definition identifier
         * @param airframeId the airframe identifier
         * @return all-time event row for the specified event definition and fleet
         * @throws SQLException if no row is found
         */
        public static EventRow eventRowTotal(
                Connection connection, String rowName, int fleetId, int eventDefinitionId, int airframeId)
                throws SQLException {
            String tableName = airframeId > 0 ? "v_fleet_airframe_event_counts" : "v_fleet_event_counts";
            String aggTableName = airframeId > 0 ? "v_aggregate_airframe_event_counts" : "v_aggregate_event_counts";
            int totalFlights = FlightStatistics.getTotalFlightCount(connection, fleetId, airframeId);
            int aggTotalFlights = FlightStatistics.getAggregateTotalFlightCount(connection, airframeId);
            return new EventRow(
                    connection,
                    rowName,
                    totalFlights,
                    aggTotalFlights,
                    fleetId,
                    eventDefinitionId,
                    airframeId,
                    tableName,
                    aggTableName,
                    null);
        }

        public int getFlightsWithoutError() {
            return flightsWithoutError;
        }

        public int getFlightsWithEvent() {
            return flightsWithEvent;
        }

        public int getTotalEvents() {
            return totalEvents;
        }

        public double getAvgEvents() {
            return avgEvents;
        }

        public double getAvgDuration() {
            return avgDuration;
        }

        public double getMinDuration() {
            return minDuration;
        }

        public double getMaxDuration() {
            return maxDuration;
        }

        public double getAvgSeverity() {
            return avgSeverity;
        }

        public double getMinSeverity() {
            return minSeverity;
        }

        public double getMaxSeverity() {
            return maxSeverity;
        }

        public int getAggFlightsWithoutError() {
            return aggFlightsWithoutError;
        }

        public int getAggFlightsWithEvent() {
            return aggFlightsWithEvent;
        }

        public int getAggTotalEvents() {
            return aggTotalEvents;
        }

        public double getAggAvgEvents() {
            return aggAvgEvents;
        }

        public double getAggAvgDuration() {
            return aggAvgDuration;
        }

        public double getAggMinDuration() {
            return aggMinDuration;
        }

        public double getAggMaxDuration() {
            return aggMaxDuration;
        }

        public double getAggAvgSeverity() {
            return aggAvgSeverity;
        }

        public double getAggMinSeverity() {
            return aggMinSeverity;
        }

        public double getAggMaxSeverity() {
            return aggMaxSeverity;
        }

        public String getRowName() {
            return rowName;
        }
    }

    private static class AirframeStatistics {
        private final String eventName;
        private final int totalFlights;
        private final int processedFlights;
        private final String humanReadable;
        private final int eventId;
        private final List<EventRow> monthStats = new ArrayList<EventRow>();

        AirframeStatistics(Connection connection, EventDefinition eventDefinition, int fleetId) throws SQLException {
            this.eventId = eventDefinition.getId();
            this.eventName = eventDefinition.getName();
            this.humanReadable = eventDefinition.toHumanReadable();

            int airframeNameId = eventDefinition.getAirframeNameId();

            String query;

            totalFlights = FlightStatistics.getTotalFlightCount(connection, fleetId, airframeNameId);
            if (airframeNameId == 0) {
                query = "SELECT SUM(count) FROM m_fleet_airframe_event_processed_flight_count WHERE " + " fleet_id = "
                        + fleetId + " AND event_definition_id = "
                        + eventDefinition.getId();

                try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                        ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) processedFlights = resultSet.getInt(1);
                    else processedFlights = 0;
                }
            } else {
                query = "SELECT count FROM m_fleet_airframe_event_processed_flight_count WHERE " + " fleet_id = "
                        + fleetId + " AND event_definition_id = "
                        + eventDefinition.getId() + " AND airframe_id = "
                        + airframeNameId;
                try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                        ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        processedFlights = resultSet.getInt(1);
                    } else {
                        processedFlights = 0;
                    }
                }
            }

            int currentMonth = TimeUtils.getCurrentMonthUTC();
            int currentYear = TimeUtils.getCurrentYearUTC();

            OffsetDateTime lastMonth =
                    OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).minusMonths(1);
            int previousMonth = lastMonth.getMonthValue();
            int previousYear = lastMonth.getYear();

            monthStats.add(EventRow.eventRowMonth(
                    connection, "Month to Date", fleetId, eventId, airframeNameId, currentYear, currentMonth));
            monthStats.add(EventRow.eventRowMonth(
                    connection, "Previous Month", fleetId, eventId, airframeNameId, previousYear, previousMonth));
            monthStats.add(
                    EventRow.eventRowYear(connection, "Year to Date", fleetId, eventId, airframeNameId, currentYear));
            monthStats.add(
                    EventRow.eventRowYear(connection, "Previous Year", fleetId, eventId, airframeNameId, previousYear));
            monthStats.add(EventRow.eventRowTotal(connection, "Overall", fleetId, eventId, airframeNameId));
        }
    }

    public abstract static class EventCountsWithAggregateBuilder<T extends EventCountsWithAggregate> {
        protected final Map<String, Integer> flightsWithEventMap = new HashMap<>();
        protected final Map<String, Integer> totalFlightsMap = new HashMap<>();
        protected final Map<String, Integer> totalEventsMap = new HashMap<>();

        protected final Map<String, Integer> aggregateFlightsWithEventMap = new HashMap<>();
        protected final Map<String, Integer> aggregateTotalFlightsMap = new HashMap<>();
        protected final Map<String, Integer> aggregateTotalEventsMap = new HashMap<>();
        protected final TreeSet<String> keys = new TreeSet<>();

        public static int[] linearize(List<String> keys, Map<String, Integer> map) {
            int[] out = new int[keys.size()];

            for (int i = 0; i < keys.size(); i++) out[i] = map.getOrDefault(keys.get(i), 0);

            return out;
        }

        public abstract T build();

        public void update(String key, int flightsWithEvent, int totalFlights, int totalEvents) {
            keys.add(key);

            flightsWithEventMap.merge(key, flightsWithEvent, Integer::sum);
            totalFlightsMap.merge(key, totalFlights, Integer::sum);
            totalEventsMap.merge(key, totalEvents, Integer::sum);
        }

        public void updateWithTotalFlightsMax(String key, int flightsWithEvent, int totalFlights, int totalEvents) {
            keys.add(key);

            flightsWithEventMap.merge(key, flightsWithEvent, Integer::sum);
            totalFlightsMap.merge(key, totalFlights, Math::max);
            totalEventsMap.merge(key, totalEvents, Integer::sum);
        }

        public void updateAggregate(String key, int flightsWithEvent, int totalFlights, int totalEvents) {
            keys.add(key);

            aggregateFlightsWithEventMap.merge(key, flightsWithEvent, Integer::sum);
            aggregateTotalFlightsMap.merge(key, totalFlights, Integer::sum);
            aggregateTotalEventsMap.merge(key, totalEvents, Integer::sum);
        }

        public void updateAggregateWithTotalFlightsMax(
                String key, int flightsWithEvent, int totalFlights, int totalEvents) {
            keys.add(key);

            aggregateFlightsWithEventMap.merge(key, flightsWithEvent, Integer::sum);
            aggregateTotalFlightsMap.merge(key, totalFlights, Math::max);
            aggregateTotalEventsMap.merge(key, totalEvents, Integer::sum);
        }
    }

    public static class EventCountsWithAggregate {
        private final int[] flightsWithEventCounts;
        private final int[] totalFlightsCounts;
        private final int[] totalEventsCounts;

        private final int[] aggregateFlightsWithEventCounts;
        private final int[] aggregateTotalFlightsCounts;
        private final int[] aggregateTotalEventsCounts;

        public EventCountsWithAggregate(
                int[] flightsWithEventCounts,
                int[] totalFlightsCounts,
                int[] totalEventsCounts,
                int[] aggregateFlightsWithEventCounts,
                int[] aggregateTotalFlightsCounts,
                int[] aggregateTotalEventsCounts) {
            this.flightsWithEventCounts = flightsWithEventCounts;
            this.totalFlightsCounts = totalFlightsCounts;
            this.totalEventsCounts = totalEventsCounts;

            this.aggregateFlightsWithEventCounts = aggregateFlightsWithEventCounts;
            this.aggregateTotalFlightsCounts = aggregateTotalFlightsCounts;
            this.aggregateTotalEventsCounts = aggregateTotalEventsCounts;
        }

        public int[] getAggregateFlightsWithEventCounts() {
            return aggregateFlightsWithEventCounts;
        }
    }

    public static class MonthlyEventCounts extends EventCountsWithAggregate {
        private final String airframeName;
        private final String eventName;
        private final List<String> dates;

        public MonthlyEventCounts(
                String airframeName,
                String eventName,
                List<String> dates,
                int[] flightsWithEventCounts,
                int[] totalFlightsCounts,
                int[] totalEventsCounts,
                int[] aggregateFlightsWithEventCounts,
                int[] aggregateTotalFlightsCounts,
                int[] aggregateTotalEventsCounts) {
            super(
                    flightsWithEventCounts,
                    totalFlightsCounts,
                    totalEventsCounts,
                    aggregateFlightsWithEventCounts,
                    aggregateTotalFlightsCounts,
                    aggregateTotalEventsCounts);

            this.airframeName = airframeName;
            this.eventName = eventName;
            this.dates = dates;
        }
    }

    public static class MonthlyEventCountsBuilder extends EventCountsWithAggregateBuilder<MonthlyEventCounts> {
        private final String airframeName;
        private final String eventName;

        private final List<String> dates;

        public MonthlyEventCountsBuilder(
                String airframeName, String eventName, LocalDate startDate, LocalDate endDate) {
            this.airframeName = airframeName;
            this.eventName = eventName;

            // Create a date for each month between start date and end date.
            dates = Stream.iterate(startDate, date -> date.plusMonths(1))
                    .limit(ChronoUnit.MONTHS.between(startDate, endDate))
                    .map(LocalDate::toString)
                    .collect(Collectors.toList());
        }

        private int[] linearize(Map<String, Integer> map) {
            return linearize(dates, map);
        }

        @Override
        public MonthlyEventCounts build() {
            return new MonthlyEventCounts(
                    airframeName,
                    eventName,
                    dates,
                    linearize(flightsWithEventMap),
                    linearize(totalFlightsMap),
                    linearize(totalEventsMap),
                    linearize(aggregateFlightsWithEventMap),
                    linearize(aggregateTotalFlightsMap),
                    linearize(aggregateTotalEventsMap));
        }
    }

    public static class EventCounts extends EventCountsWithAggregate {
        private final String airframeName;
        private final List<String> names;

        public EventCounts(
                String airframeName,
                List<String> names,
                int[] flightsWithEventCounts,
                int[] totalFlightsCounts,
                int[] totalEventsCounts,
                int[] aggregateFlightsWithEventCounts,
                int[] aggregateTotalFlightsCounts,
                int[] aggregateTotalEventsCounts) {
            super(
                    flightsWithEventCounts,
                    totalFlightsCounts,
                    totalEventsCounts,
                    aggregateFlightsWithEventCounts,
                    aggregateTotalFlightsCounts,
                    aggregateTotalEventsCounts);

            this.names = names;
            this.airframeName = airframeName;
        }

        public List<String> getNames() {
            return names;
        }
    }

    public static class EventCountsBuilder extends EventCountsWithAggregateBuilder<EventCounts> {
        private final String airframeName;

        public EventCountsBuilder(String airframeName) {
            this.airframeName = airframeName;
        }

        public void ensureEventKey(String eventName) {
            keys.add(eventName);
        }

        public void setTotalFlights(String eventName, int totalFlights) {
            keys.add(eventName);
            totalFlightsMap.put(eventName, totalFlights);
        }

        public void setAggregateTotalFlights(String eventName, int totalFlights) {
            keys.add(eventName);
            aggregateTotalFlightsMap.put(eventName, totalFlights);
        }

        /**
         * Place Map values into lists and dead store eliminate maps
         *
         * @return EventCounts
         */
        public EventCounts build() {
            ArrayList<String> sortedKeys = new ArrayList<>(keys);
            // Should be sorted because keys is a tree set.

            return new EventCounts(
                    airframeName,
                    sortedKeys,
                    linearize(sortedKeys, flightsWithEventMap),
                    linearize(sortedKeys, totalFlightsMap),
                    linearize(sortedKeys, totalEventsMap),
                    linearize(sortedKeys, aggregateFlightsWithEventMap),
                    linearize(sortedKeys, aggregateTotalFlightsMap),
                    linearize(sortedKeys, aggregateTotalEventsMap));
        }
    }
}
