package org.ngafid.core.util.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FilterTest {
    private static final String EASTERN_TIME = "(GMT-05:00) Eastern Time (US & Canada)";

    private static Filter filterOf(String... inputs) {
        return new Filter(new ArrayList<>(List.of(inputs)));
    }

    private static void assertContainsAll(String query, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(query.contains(fragment), "Expected query fragment not found: " + fragment + " in " + query);
        }
    }

    private static void assertParameters(ArrayList<Object> parameters, Object... expected) {
        assertEquals(Arrays.asList(expected), parameters);
    }

    /**
     * Verifies {@code checkOperator} accepts the supported comparison operators and returns null for unsupported ones.
     */
    @Test
    @DisplayName("Should accept supported comparison operators")
    public void checkOperatorAcceptsSupportedComparators() {
        Filter filter = filterOf("Flight ID", ">=", "1");

        for (String operator : List.of("<=", "<", "=", ">", ">=")) {
            assertEquals(operator, filter.checkOperator(operator));
        }

        assertNull(filter.checkOperator("LIKE"));
    }

    /**
     * Verifies {@code checkSeriesOp} accepts the supported series statistics (min/avg/max) and returns null for an
     * unsupported one.
     */
    @Test
    @DisplayName("Should accept supported series statistics")
    public void checkSeriesOpAcceptsSupportedStatistics() {
        Filter filter = filterOf("Parameter", "min", "Altitude", ">=", "1");

        for (String statistic : List.of("min", "avg", "max")) {
            assertEquals(statistic, filter.checkSeriesOp(statistic));
        }

        assertNull(filter.checkSeriesOp("median"));
    }

    /**
     * Verifies the date/time helpers convert browser-supplied local date-times and times to the stored UTC strings, and
     * that {@code timePad} zero-pads time components.
     */
    @Test
    @DisplayName("Should normalize UI date and time inputs")
    public void dateAndTimeHelpersNormalizeUiInputs() {
        String convertedDateTime = Filter.getOffsetDateTime("2026-03-09T12:34", EASTERN_TIME);
        String convertedTime = Filter.getOffsetTime("12:34:56", EASTERN_TIME);

        assertEquals("2026-03-09 17:34:00", convertedDateTime);
        assertEquals("17:34:56", convertedTime);

        Filter filter = filterOf("Duration", ">=", "1", "2", "3");
        assertEquals("00", filter.timePad(""));
        assertEquals("07", filter.timePad("7"));
        assertEquals("12", filter.timePad("12"));
    }

    /**
     * Verifies the Airframe rule builds the correct "is"/"is not" subquery against the airframes table with the fleet
     * id and airframe name as parameters.
     */
    @Test
    @DisplayName("Should build airframe rule queries for is and is-not")
    public void airframeRuleSupportsIsAndIsNot() {
        ArrayList<Object> parameters = new ArrayList<>();
        String isQuery = filterOf("Airframe", "is", "C172S").getRuleQuery(5, parameters);

        assertEquals("flights.airframe_id = (SELECT id FROM airframes WHERE fleet_id = ? AND airframe = ?)", isQuery);
        assertParameters(parameters, 5, "C172S");

        parameters.clear();
        String isNotQuery = filterOf("Airframe", "is not", "C172S").getRuleQuery(5, parameters);

        assertEquals(
                "flights.airframe_id != (SELECT id FROM airframes WHERE fleet_id = ? AND airframe = ?)", isNotQuery);
        assertParameters(parameters, 5, "C172S");
    }

    /**
     * Verifies the Tail Number and System ID rules build the correct is/is-not queries and parameter lists for both
     * conditions.
     */
    @Test
    @DisplayName("Should build tail-number and system-id rule queries for both conditions")
    public void tailNumberAndSystemIdRulesSupportBothConditions() {
        ArrayList<Object> parameters = new ArrayList<>();
        String tailIsQuery = filterOf("Tail Number", "is", "N12345").getRuleQuery(8, parameters);

        assertEquals("flights.system_id in (SELECT system_id FROM tails WHERE fleet_id = ? AND tail = ?)", tailIsQuery);
        assertParameters(parameters, 8, "N12345");

        parameters.clear();
        String tailIsNotQuery = filterOf("Tail Number", "is not", "N12345").getRuleQuery(8, parameters);

        assertEquals(
                "flights.system_id not in (SELECT system_id FROM tails WHERE fleet_id = ? AND tail = ?)",
                tailIsNotQuery);
        assertParameters(parameters, 8, "N12345");

        parameters.clear();
        String systemIsQuery = filterOf("System ID", "is", "SYS-1").getRuleQuery(9, parameters);

        assertEquals("flights.fleet_id = ? AND flights.system_id = ?", systemIsQuery);
        assertParameters(parameters, 9, "SYS-1");

        parameters.clear();
        String systemIsNotQuery = filterOf("System ID", "is not", "SYS-1").getRuleQuery(9, parameters);

        assertEquals("flights.fleet_id = ? AND flights.system_id != ?", systemIsNotQuery);
        assertParameters(parameters, 9, "SYS-1");
    }

    /**
     * Verifies the Duration rule formats its value as an HH:mm:ss string and the Flight ID rule scopes by fleet, each
     * building the expected query and parameters.
     */
    @Test
    @DisplayName("Should build duration and flight-id rule queries")
    public void durationAndFlightIdRulesBuildExpectedQueries() {
        ArrayList<Object> parameters = new ArrayList<>();
        String durationQuery = filterOf("Duration", "<=", "1", "2", "3").getRuleQuery(1, parameters);

        assertEquals("TIMEDIFF(flights.end_time, flights.start_time) <= ?", durationQuery);
        assertParameters(parameters, "01:02:03");

        parameters.clear();
        String flightIdQuery = filterOf("Flight ID", ">", "100").getRuleQuery(12, parameters);

        assertEquals("flights.fleet_id = ? AND flights.id > ?", flightIdQuery);
        assertParameters(parameters, 12, "100");
    }

    /**
     * Verifies the Start/End Date-and-Time rules convert browser date-time values to stored UTC strings and target the
     * start_time/end_time columns.
     */
    @Test
    @DisplayName("Should convert browser date-time values for start/end date-time rules")
    public void startAndEndDateTimeRulesConvertBrowserDateTimeValues() {
        ArrayList<Object> parameters = new ArrayList<>();
        String startQuery = filterOf("Start Date and Time", ">=", "2026-03-09T12:34", EASTERN_TIME)
                .getRuleQuery(1, parameters);

        assertEquals("flights.start_time >= ?", startQuery);
        assertParameters(parameters, "2026-03-09 17:34:00");

        parameters.clear();
        String endQuery = filterOf("End Date and Time", "<=", "2026-03-09T12:34", EASTERN_TIME)
                .getRuleQuery(1, parameters);

        assertEquals("flights.end_time <= ?", endQuery);
        assertParameters(parameters, "2026-03-09 17:34:00");
    }

    /**
     * Verifies the Start/End Date rules apply {@code DATE(...)} to the correct start_time/end_time columns.
     */
    @Test
    @DisplayName("Should target the correct columns for start/end date rules")
    public void startAndEndDateRulesTargetCorrectColumns() {
        ArrayList<Object> parameters = new ArrayList<>();
        String startDateQuery = filterOf("Start Date", "=", "2026-03-09").getRuleQuery(1, parameters);

        assertEquals("DATE(flights.start_time) = ?", startDateQuery);
        assertParameters(parameters, "2026-03-09");

        parameters.clear();
        String endDateQuery = filterOf("End Date", "<", "2026-03-10").getRuleQuery(1, parameters);

        assertEquals("DATE(flights.end_time) < ?", endDateQuery);
        assertParameters(parameters, "2026-03-10");
    }

    /**
     * Verifies the Start/End Time rules apply {@code TIME(...)} to the correct columns and convert the time to UTC.
     */
    @Test
    @DisplayName("Should target the correct columns for start/end time rules")
    public void startAndEndTimeRulesTargetCorrectColumns() {
        ArrayList<Object> parameters = new ArrayList<>();
        String startTimeQuery =
                filterOf("Start Time", "<", "12:34:56", EASTERN_TIME).getRuleQuery(1, parameters);

        assertEquals("TIME(flights.start_time) < ?", startTimeQuery);
        assertParameters(parameters, "17:34:56");

        parameters.clear();
        String endTimeQuery =
                filterOf("End Time", ">", "12:34:56", EASTERN_TIME).getRuleQuery(1, parameters);

        assertEquals("TIME(flights.end_time) > ?", endTimeQuery);
        assertParameters(parameters, "17:34:56");
    }

    /**
     * Verifies the Parameter rule builds a double-series EXISTS subquery using each supported statistic (min/avg/max).
     */
    @Test
    @DisplayName("Should build parameter rule queries for all statistics")
    public void parameterRuleSupportsAllStatistics() {
        for (String statistic : List.of("min", "avg", "max")) {
            ArrayList<Object> parameters = new ArrayList<>();
            String query =
                    filterOf("Parameter", statistic, "Altitude", ">=", "42.5").getRuleQuery(2, parameters);

            assertContainsAll(
                    query,
                    "EXISTS (SELECT id FROM double_series",
                    "double_series.name_id = (SELECT id FROM double_series_names WHERE name = ?)",
                    "double_series." + statistic + " >= ?");
            assertParameters(parameters, "Altitude", "42.5");
        }
    }

    /**
     * Verifies the Airport and Runway rules build EXISTS/NOT EXISTS itinerary subqueries for the visited and
     * not-visited conditions, splitting the airport/runway inputs into parameters.
     */
    @Test
    @DisplayName("Should build airport and runway rule queries for visited and not-visited")
    public void airportAndRunwayRulesSupportVisitedAndNotVisited() {
        ArrayList<Object> parameters = new ArrayList<>();
        String airportVisitedQuery =
                filterOf("Airport", "GFK - Grand Forks", "visited").getRuleQuery(1, parameters);

        assertContainsAll(airportVisitedQuery, "EXISTS", "itinerary.airport = ?");
        assertParameters(parameters, "GFK");

        parameters.clear();
        String airportNotVisitedQuery =
                filterOf("Airport", "GFK - Grand Forks", "not visited").getRuleQuery(1, parameters);

        assertContainsAll(airportNotVisitedQuery, "NOT EXISTS", "itinerary.airport = ?");
        assertParameters(parameters, "GFK");

        parameters.clear();
        String runwayVisitedQuery = filterOf("Runway", "GFK - 35L", "visited").getRuleQuery(1, parameters);

        assertContainsAll(runwayVisitedQuery, "EXISTS", "itinerary.airport = ?", "itinerary.runway = ?");
        assertParameters(parameters, "GFK", "35L");

        parameters.clear();
        String runwayNotVisitedQuery =
                filterOf("Runway", "GFK - 35L", "not visited").getRuleQuery(1, parameters);

        assertContainsAll(runwayNotVisitedQuery, "NOT EXISTS", "itinerary.airport = ?", "itinerary.runway = ?");
        assertParameters(parameters, "GFK", "35L");
    }

    /**
     * Verifies the Event Count rule builds a COUNT subquery for both generic events and airframe-specific events (the
     * latter joining the airframes table), with the expected parameter order.
     */
    @Test
    @DisplayName("Should build event-count rule queries for generic and airframe-specific events")
    public void eventCountRuleSupportsGenericAndAirframeSpecificEvents() {
        ArrayList<Object> parameters = new ArrayList<>();
        String genericQuery =
                filterOf("Event Count", "Example Event", ">=", "1").getRuleQuery(7, parameters);

        assertContainsAll(
                genericQuery,
                "SELECT COUNT(*) FROM events e",
                "e.event_definition_id IN",
                "ed.airframe_id = 0",
                ")) >= ?");
        assertParameters(parameters, "Example Event", 7, 1);

        parameters.clear();
        String airframeQuery =
                filterOf("Event Count", "Example Event - C172S", "<=", "2").getRuleQuery(7, parameters);

        assertContainsAll(
                airframeQuery,
                "SELECT COUNT(*) FROM events e",
                "e.event_definition_id IN",
                "JOIN airframes a ON a.id = ed.airframe_id",
                "a.airframe = ?",
                ")) <= ?");
        assertParameters(parameters, "Example Event", "C172S", 7, 7, 2);
    }

    /**
     * Verifies the Event Severity rule builds the expected query for both generic events and airframe-specific events
     * (joining the airframes table), with the expected parameter order.
     */
    @Test
    @DisplayName("Should build event-severity rule queries for generic and airframe-specific events")
    public void eventSeverityRuleSupportsGenericAndAirframeSpecificEvents() {
        ArrayList<Object> parameters = new ArrayList<>();
        String genericQuery =
                filterOf("Event Severity", "Example Event", "=", "2.5").getRuleQuery(11, parameters);

        assertContainsAll(genericQuery, "events.flight_id", "events.event_definition_id IN", "events.severity = ?");
        assertParameters(parameters, "Example Event", 11, "2.5");

        parameters.clear();
        String airframeQuery =
                filterOf("Event Severity", "Example Event - C172S", ">=", "2.5").getRuleQuery(11, parameters);

        assertContainsAll(
                airframeQuery,
                "JOIN airframes a ON a.id = ed.airframe_id",
                "a.airframe = ?",
                "a.fleet_id = ?",
                "events.severity >= ?");
        assertParameters(parameters, "Example Event", "C172S", 11, 11, "2.5");
    }

    /**
     * Verifies the Event Duration rule builds a line-span comparison for both generic events and airframe-specific
     * events (joining the airframes table), with the expected parameter order.
     */
    @Test
    @DisplayName("Should build event-duration rule queries for generic and airframe-specific events")
    public void eventDurationRuleSupportsGenericAndAirframeSpecificEvents() {
        ArrayList<Object> parameters = new ArrayList<>();
        String genericQuery =
                filterOf("Event Duration", "Example Event", ">", "10").getRuleQuery(4, parameters);

        assertContainsAll(
                genericQuery, "events.event_definition_id IN", "((events.end_line - events.start_line) + 1) > ?");
        assertParameters(parameters, "Example Event", 4, "10");

        parameters.clear();
        String airframeQuery =
                filterOf("Event Duration", "Example Event - C172S", "<", "10").getRuleQuery(4, parameters);

        assertContainsAll(
                airframeQuery,
                "JOIN airframes a ON a.id = ed.airframe_id",
                "a.airframe = ?",
                "((events.end_line - events.start_line) + 1) < ?");
        assertParameters(parameters, "Example Event", "C172S", 4, 4, "10");
    }

    /**
     * Verifies the Tag rule builds EXISTS/NOT EXISTS flight-tag subqueries for the associated and not-associated
     * conditions.
     */
    @Test
    @DisplayName("Should build tag rule queries for associated and not-associated")
    public void tagRuleSupportsAssociatedAndNotAssociated() {
        ArrayList<Object> parameters = new ArrayList<>();
        String associatedQuery = filterOf("Tag", "Checkride", "Is Associated").getRuleQuery(3, parameters);

        assertContainsAll(associatedQuery, "EXISTS", "flight_tags WHERE fleet_id = ? AND name = ?");
        assertParameters(parameters, 3, "Checkride");

        parameters.clear();
        String notAssociatedQuery =
                filterOf("Tag", "Checkride", "Is Not Associated").getRuleQuery(3, parameters);

        assertContainsAll(notAssociatedQuery, "NOT EXISTS", "flight_tags WHERE fleet_id = ? AND name = ?");
        assertParameters(parameters, 3, "Checkride");
    }

    /**
     * Verifies a group filter's {@code toQueryString} combines its child rules with the group operator and preserves
     * the child parameters in order.
     */
    @Test
    @DisplayName("Should combine child rules and preserve parameter order for a group")
    public void groupToQueryStringCombinesChildRulesAndPreservesParameterOrder() {
        Filter group = new Filter("OR");
        group.addFilter(filterOf("Start Date", ">=", "2026-03-09"));
        group.addFilter(filterOf("Flight ID", "<", "100"));

        ArrayList<Object> parameters = new ArrayList<>();
        String query = group.toQueryString(13, parameters);

        assertContainsAll(
                query, "(DATE(flights.start_time) >= ?)", " OR ", "(flights.fleet_id = ? AND flights.id < ?)");
        assertParameters(parameters, "2026-03-09", 13, "100");
    }
}
