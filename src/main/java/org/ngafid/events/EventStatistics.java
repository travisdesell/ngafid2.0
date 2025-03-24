package org.ngafid.events;

import org.ngafid.common.TimeUtils;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.FlightStatistics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        ArrayList<EventDefinition> eventDefinitions = EventDefinition.getAll(connection,
                "airframe_id = ? AND (fleet_id = 0 OR fleet_id = ?)", new Object[]{airframeNameId, fleetId});

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

    public static Map<String, EventCounts> getEventCounts(Connection connection, int fleetId, LocalDate startDate,
                                                          LocalDate endDate) throws SQLException {
        if (startDate == null)
            startDate = LocalDate.of(0, 1, 1);

        if (endDate == null)
            endDate = LocalDate.now();

        int startYear = startDate.getYear();
        int startMonth = startDate.getMonthValue();
        int endYear = endDate.getYear();
        int endMonth = endDate.getMonthValue();

        String query = "SELECT fleet_id, event_definition_id, airframe_id, SUM(event_count) as event_count, SUM(flight_count) as flight_count FROM m_fleet_airframe_monthly_event_counts " +
                " WHERE year >= " + startYear + " AND year <= " + endYear + " AND month >= " + startMonth + " AND month <= " + endMonth +
                " GROUP BY fleet_id, event_definition_id, airframe_id";

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet result = statement.executeQuery()) {
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
                                    entry -> entry.getValue().build()));
        }
    }

    public static Map<String, Map<String, MonthlyEventCounts>> getMonthlyEventCounts(Connection connection, int fleetId,
                                                                                     LocalDate startDateNullable,
                                                                                     LocalDate endDateNullable)
            throws SQLException {
        final LocalDate startDate = startDateNullable == null ? LocalDate.of(0, 1, 1) : startDateNullable;
        final LocalDate endDate = endDateNullable == null ? LocalDate.now() : endDateNullable;

        int startYear = startDate.getYear();
        int startMonth = startDate.getMonthValue();
        int endYear = endDate.getYear();
        int endMonth = endDate.getMonthValue();

        String query = "SELECT year, month, fleet_id, event_definition_id, airframe_id, SUM(event_count) as event_count, SUM(flight_count) as flight_count FROM m_fleet_airframe_monthly_event_counts " +
                " WHERE year >= " + startYear + " AND year <= " + endYear + " AND month >= " + startMonth + " AND month <= " + endMonth +
                " GROUP BY fleet_id, event_definition_id, airframe_id, year, month";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
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
        if (startDate == null)
            startDate = LocalDate.of(0, 1, 1);

        if (endDate == null)
            endDate = LocalDate.now();

        String query = "SELECT COUNT(DISTINCT id) as flight_count, airframe_id, fleet_id FROM flights WHERE " +
                "start_time BETWEEN ? AND ? GROUP BY flights.airframe_id, flights.fleet_id ";

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
        if (condition != null)
            query += " WHERE " + condition;

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet results = statement.executeQuery()) {
            if (results.next()) {
                return results.getInt(1);
            }

            throw new SQLException("Failed to read from table " + tableName + ":" + statement);
        }
    }

    /**
     * @return the total number of events over all fleets, airframes, and event types so far for the current year.
     * @throws SQLException if the table is empty
     */
    public static int getAggregateCurrentYearEventCount(Connection connection) throws SQLException {
        return getEventCount(connection, "v_fleet_yearly_event_counts", " year = " + TimeUtils.getCurrentYearUTC());
    }

    /**
     * @return the total number of events for the specified fleet over all airframes and event types so far for the current year.
     * @throws SQLException if the table is empty
     */
    public static int getCurrentYearEventCount(Connection connection, int fleetId) throws SQLException {
        return getEventCount(connection, "v_fleet_yearly_event_counts", "fleet_id = " + fleetId + " AND year = " + TimeUtils.getCurrentYearUTC());
    }

    /**
     * @return the total number of events over all fleets, airframes, and event types so far for the current month.
     * @throws SQLException if the table is empty
     */
    public static int getAggregateCurrentMonthEventCount(Connection connection) throws SQLException {
        return getEventCount(connection, "v_aggregate_monthly_event_counts", " year = " + TimeUtils.getCurrentYearUTC() + " AND month = " + TimeUtils.getCurrentMonthUTC());
    }

    /**
     * @return the total number of events for the specified fleet over all airframes and event types so far for the current month.
     * @throws SQLException if the table is empty
     */
    public static int getCurrentMonthEventCount(Connection connection, int fleetId) throws SQLException {
        return getEventCount(connection, "v_fleet_monthly_event_counts", "fleet_id = " + fleetId + " AND year = " + TimeUtils.getCurrentYearUTC() + " AND month = " + TimeUtils.getCurrentMonthUTC());
    }

    /**
     * @return the total number of events over all fleets, airframes, and event types -- every event in the database.
     * @throws SQLException if the table is empty
     */
    public static int getAggregateTotalEventCount(Connection connection) throws SQLException {
        return getEventCount(connection, "v_aggregate_total_event_count", null);
    }

    /**
     * @return the total number of events for this fleet over all airframes and event types -- every event in the database associated with this fleet.
     * @throws SQLException if the table is empty
     */
    public static int getTotalEventCount(Connection connection, int fleetId) throws SQLException {
        return getEventCount(connection, "v_fleet_total_event_counts", "fleet_id = " + fleetId);
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

    private static class EventRow {
        public final String rowName;

        public int flightsWithoutError;
        public int flightsWithEvent;
        public int totalEvents;
        public double avgEvents;
        public double avgDuration;
        public double minDuration;
        public double maxDuration;
        public double avgSeverity;
        public double minSeverity;
        public double maxSeverity;

        public int aggFlightsWithoutError;
        public int aggFlightsWithEvent;
        public int aggTotalEvents;
        public double aggAvgEvents;
        public double aggAvgDuration;
        public double aggMinDuration;
        public double aggMaxDuration;
        public double aggAvgSeverity;
        public double aggMinSeverity;
        public double aggMaxSeverity;

        private static final String COLUMNS = "event_count, flight_count, min_duration, avg_duration, max_duration, min_severity, avg_severity, max_severity";

        /**
         * Creates an event row for the given fleet and event id and any additional specified conditions, pulled from the supplied table names
         *
         * @param tableName    table name for the fleet event statistics.
         * @param aggTableName table name for the aggregate event statistics
         * @param conditions   extra SQL conditions
         * @throws SQLException if no row is found
         */
        EventRow(Connection connection, String rowName, int flightsWithoutError, int aggFlightsWithoutError, int fleetId, int eventId, int airframeId,
                 String tableName, String aggTableName, String conditions) throws SQLException {
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
         * @param year  full 4-digit year
         * @param month month (1-12)
         * @return event row for the supplied fleet and event definition during the given month
         */
        public static EventRow eventRowMonth(Connection connection, String rowName, int fleetId, int eventDefinitionId, int airframeId, int year, int month) throws SQLException {
            String tableName = airframeId > 0 ? "m_fleet_airframe_monthly_event_counts" : "v_fleet_monthly_event_counts";
            String aggTableName = airframeId > 0 ? "v_aggregate_airframe_monthly_event_counts" : "v_aggregate_monthly_event_counts";
            int totalFlights = FlightStatistics.getMonthFlightCount(connection, fleetId, airframeId, year, month);
            int aggTotalFlights = FlightStatistics.getAggregateMonthFlightCount(connection, airframeId, year, month);
            return new EventRow(connection, rowName, totalFlights, aggTotalFlights, fleetId, eventDefinitionId, airframeId, tableName, aggTableName, " year = " + year + " AND month = " + month);
        }

        /**
         * @param year full 4 digit year
         * @return event row for the specified year
         */
        public static EventRow eventRowYear(Connection connection, String rowName, int fleetId, int eventDefinitionId, int airframeId, int year) throws SQLException {
            String tableName = airframeId > 0 ? "v_fleet_airframe_yearly_event_counts" : "v_fleet_yearly_event_counts";
            String aggTableName = airframeId > 0 ? "v_aggregate_airframe_yearly_event_counts" : "v_aggregate_yearly_event_counts";
            int totalFlights = FlightStatistics.getYearFlightCount(connection, fleetId, airframeId, year);
            int aggTotalFlights = FlightStatistics.getAggregateYearFlightCount(connection, airframeId, year);
            return new EventRow(connection, rowName, totalFlights, aggTotalFlights, fleetId, eventDefinitionId, airframeId, tableName, aggTableName, " year = " + year);
        }

        /**
         * @return all-time event row for the specified event definition and fleet
         */
        public static EventRow eventRowTotal(Connection connection, String rowName, int fleetId, int eventDefinitionId, int airframeId) throws SQLException {
            String tableName = airframeId > 0 ? "v_fleet_airframe_event_counts" : "v_fleet_event_counts";
            String aggTableName = airframeId > 0 ? "v_aggregate_airframe_event_counts" : "v_aggregate_event_counts";
            int totalFlights = FlightStatistics.getTotalFlightCount(connection, fleetId, airframeId);
            int aggTotalFlights = FlightStatistics.getAggregateTotalFlightCount(connection, airframeId);
            return new EventRow(connection, rowName, totalFlights, aggTotalFlights, fleetId, eventDefinitionId, airframeId, tableName, aggTableName, null);
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
                query = "SELECT SUM(count) FROM m_fleet_airframe_event_processed_flight_count WHERE " +
                        " fleet_id = " + fleetId +
                        " AND event_definition_id = " + eventDefinition.getId();

                try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                     ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next())
                        processedFlights = resultSet.getInt(1);
                    else
                        processedFlights = 0;
                }
            } else {
                query = "SELECT count FROM m_fleet_airframe_event_processed_flight_count WHERE " +
                        " fleet_id = " + fleetId +
                        " AND event_definition_id = " + eventDefinition.getId() +
                        " AND airframe_id = " + airframeNameId;
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

            OffsetDateTime lastMonth = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).minusMonths(1);
            int previousMonth = lastMonth.getMonthValue();
            int previousYear = lastMonth.getYear();

            monthStats.add(EventRow.eventRowMonth(connection, "Month to Date", fleetId, eventId, airframeNameId, currentYear, currentMonth));
            monthStats.add(EventRow.eventRowMonth(connection, "Previous Month", fleetId, eventId, airframeNameId, previousYear, previousMonth));
            monthStats.add(EventRow.eventRowYear(connection, "Year to Date", fleetId, eventId, airframeNameId, currentYear));
            monthStats.add(EventRow.eventRowYear(connection, "Previous Year", fleetId, eventId, airframeNameId, previousYear));
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

            for (int i = 0; i < keys.size(); i++)
                out[i] = map.getOrDefault(keys.get(i), 0);

            return out;
        }

        public abstract T build();

        public void update(String key, int flightsWithEvent, int totalFlights, int totalEvents) {
            keys.add(key);

            flightsWithEventMap.merge(key, flightsWithEvent, Integer::sum);
            totalFlightsMap.merge(key, totalFlights, Integer::sum);
            totalEventsMap.merge(key, totalEvents, Integer::sum);

            System.out.println(flightsWithEvent + " : " + totalFlights + " : " + totalFlights);
        }

        public void updateAggregate(String key, int flightsWithEvent, int totalFlights, int totalEvents) {
            keys.add(key);

            aggregateFlightsWithEventMap.merge(key, flightsWithEvent, Integer::sum);
            aggregateTotalFlightsMap.merge(key, totalFlights, Integer::sum);
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

        public EventCountsWithAggregate(int[] flightsWithEventCounts, int[] totalFlightsCounts,
                                        int[] totalEventsCounts, int[] aggregateFlightsWithEventCounts,
                                        int[] aggregateTotalFlightsCounts, int[] aggregateTotalEventsCounts) {
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
        private final String airframeName;
        private final String eventName;

        private final List<String> dates;

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

        private int[] linearize(Map<String, Integer> map) {
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
        private final String airframeName;
        private final List<String> names;

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

        public List<String> getNames() {
            return names;
        }
    }

    public static class EventCountsBuilder extends EventCountsWithAggregateBuilder<EventCounts> {
        private final String airframeName;

        public EventCountsBuilder(String airframeName) {
            this.airframeName = airframeName;
        }

        public void setAggregateTotalFlights(String eventName, int totalFlights) {
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
                    airframeName, sortedKeys,
                    linearize(sortedKeys, flightsWithEventMap), linearize(sortedKeys, totalFlightsMap),
                    linearize(sortedKeys, totalEventsMap),
                    linearize(sortedKeys, aggregateFlightsWithEventMap),
                    linearize(sortedKeys, aggregateTotalFlightsMap),
                    linearize(sortedKeys, aggregateTotalEventsMap));
        }
    }

}
