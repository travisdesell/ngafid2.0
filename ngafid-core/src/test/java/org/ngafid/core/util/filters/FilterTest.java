package org.ngafid.core.util.filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void checkOperatorAcceptsSupportedComparators() {
        Filter filter = filterOf("Flight ID", ">=", "1");

        for (String operator : List.of("<=", "<", "=", ">", ">=")) {
            assertEquals(operator, filter.checkOperator(operator));
        }

        assertNull(filter.checkOperator("LIKE"));
    }

    @Test
    void checkSeriesOpAcceptsSupportedStatistics() {
        Filter filter = filterOf("Parameter", "min", "Altitude", ">=", "1");

        for (String statistic : List.of("min", "avg", "max")) {
            assertEquals(statistic, filter.checkSeriesOp(statistic));
        }

        assertNull(filter.checkSeriesOp("median"));
    }

    @Test
    void dateAndTimeHelpersNormalizeUiInputs() {
        String convertedDateTime = Filter.getOffsetDateTime("2026-03-09T12:34", EASTERN_TIME);
        String convertedTime = Filter.getOffsetTime("12:34:56", EASTERN_TIME);

        assertEquals("2026-03-09 17:34:00", convertedDateTime);
        assertEquals("17:34:56", convertedTime);

        Filter filter = filterOf("Duration", ">=", "1", "2", "3");
        assertEquals("00", filter.timePad(""));
        assertEquals("07", filter.timePad("7"));
        assertEquals("12", filter.timePad("12"));
    }

    @Test
    void airframeRuleSupportsIsAndIsNot() {
        ArrayList<Object> parameters = new ArrayList<>();
        String isQuery = filterOf("Airframe", "is", "C172S").getRuleQuery(5, parameters);

        assertEquals("flights.airframe_id = (SELECT id FROM airframes WHERE fleet_id = ? AND airframe = ?)", isQuery);
        assertParameters(parameters, 5, "C172S");

        parameters.clear();
        String isNotQuery = filterOf("Airframe", "is not", "C172S").getRuleQuery(5, parameters);

        assertEquals("flights.airframe_id != (SELECT id FROM airframes WHERE fleet_id = ? AND airframe = ?)", isNotQuery);
        assertParameters(parameters, 5, "C172S");
    }

    @Test
    void tailNumberAndSystemIdRulesSupportBothConditions() {
        ArrayList<Object> parameters = new ArrayList<>();
        String tailIsQuery = filterOf("Tail Number", "is", "N12345").getRuleQuery(8, parameters);

        assertEquals("flights.system_id in (SELECT system_id FROM tails WHERE fleet_id = ? AND tail = ?)", tailIsQuery);
        assertParameters(parameters, 8, "N12345");

        parameters.clear();
        String tailIsNotQuery = filterOf("Tail Number", "is not", "N12345").getRuleQuery(8, parameters);

        assertEquals("flights.system_id not in (SELECT system_id FROM tails WHERE fleet_id = ? AND tail = ?)", tailIsNotQuery);
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

    @Test
    void durationAndFlightIdRulesBuildExpectedQueries() {
        ArrayList<Object> parameters = new ArrayList<>();
        String durationQuery = filterOf("Duration", "<=", "1", "2", "3").getRuleQuery(1, parameters);

        assertEquals("TIMEDIFF(flights.end_time, flights.start_time) <= ?", durationQuery);
        assertParameters(parameters, "01:02:03");

        parameters.clear();
        String flightIdQuery = filterOf("Flight ID", ">", "100").getRuleQuery(12, parameters);

        assertEquals("flights.fleet_id = ? AND flights.id > ?", flightIdQuery);
        assertParameters(parameters, 12, "100");
    }

    @Test
    void startAndEndDateTimeRulesConvertBrowserDateTimeValues() {
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

    @Test
    void startAndEndDateRulesTargetCorrectColumns() {
        ArrayList<Object> parameters = new ArrayList<>();
        String startDateQuery = filterOf("Start Date", "=", "2026-03-09").getRuleQuery(1, parameters);

        assertEquals("DATE(flights.start_time) = ?", startDateQuery);
        assertParameters(parameters, "2026-03-09");

        parameters.clear();
        String endDateQuery = filterOf("End Date", "<", "2026-03-10").getRuleQuery(1, parameters);

        assertEquals("DATE(flights.end_time) < ?", endDateQuery);
        assertParameters(parameters, "2026-03-10");
    }

    @Test
    void startAndEndTimeRulesTargetCorrectColumns() {
        ArrayList<Object> parameters = new ArrayList<>();
        String startTimeQuery = filterOf("Start Time", "<", "12:34:56", EASTERN_TIME).getRuleQuery(1, parameters);

        assertEquals("TIME(flights.start_time) < ?", startTimeQuery);
        assertParameters(parameters, "17:34:56");

        parameters.clear();
        String endTimeQuery = filterOf("End Time", ">", "12:34:56", EASTERN_TIME).getRuleQuery(1, parameters);

        assertEquals("TIME(flights.end_time) > ?", endTimeQuery);
        assertParameters(parameters, "17:34:56");
    }

    @Test
    void parameterRuleSupportsAllStatistics() {
        for (String statistic : List.of("min", "avg", "max")) {
            ArrayList<Object> parameters = new ArrayList<>();
            String query = filterOf("Parameter", statistic, "Altitude", ">=", "42.5").getRuleQuery(2, parameters);

            assertContainsAll(
                    query,
                    "EXISTS (SELECT id FROM double_series",
                    "double_series.name_id = (SELECT id FROM double_series_names WHERE name = ?)",
                    "double_series." + statistic + " >= ?");
            assertParameters(parameters, "Altitude", "42.5");
        }
    }

    @Test
    void airportAndRunwayRulesSupportVisitedAndNotVisited() {
        ArrayList<Object> parameters = new ArrayList<>();
        String airportVisitedQuery = filterOf("Airport", "GFK - Grand Forks", "visited").getRuleQuery(1, parameters);

        assertContainsAll(airportVisitedQuery, "EXISTS", "itinerary.airport = ?");
        assertParameters(parameters, "GFK");

        parameters.clear();
        String airportNotVisitedQuery = filterOf("Airport", "GFK - Grand Forks", "not visited")
                .getRuleQuery(1, parameters);

        assertContainsAll(airportNotVisitedQuery, "NOT EXISTS", "itinerary.airport = ?");
        assertParameters(parameters, "GFK");

        parameters.clear();
        String runwayVisitedQuery = filterOf("Runway", "GFK - 35L", "visited").getRuleQuery(1, parameters);

        assertContainsAll(runwayVisitedQuery, "EXISTS", "itinerary.airport = ?", "itinerary.runway = ?");
        assertParameters(parameters, "GFK", "35L");

        parameters.clear();
        String runwayNotVisitedQuery = filterOf("Runway", "GFK - 35L", "not visited").getRuleQuery(1, parameters);

        assertContainsAll(runwayNotVisitedQuery, "NOT EXISTS", "itinerary.airport = ?", "itinerary.runway = ?");
        assertParameters(parameters, "GFK", "35L");
    }

    @Test
    void eventCountRuleSupportsGenericAndAirframeSpecificEvents() {
        ArrayList<Object> parameters = new ArrayList<>();
        String genericQuery = filterOf("Event Count", "Example Event", ">=", "1").getRuleQuery(7, parameters);

        assertContainsAll(genericQuery, "SELECT COUNT(*) FROM events e", "e.event_definition_id IN", "ed.airframe_id = 0", ")) >= ?");
        assertParameters(parameters, "Example Event", 7, 1);

        parameters.clear();
        String airframeQuery = filterOf("Event Count", "Example Event - C172S", "<=", "2").getRuleQuery(7, parameters);

        assertContainsAll(
                airframeQuery,
                "SELECT COUNT(*) FROM events e",
                "e.event_definition_id IN",
                "JOIN airframes a ON a.id = ed.airframe_id",
                "a.airframe = ?",
                ")) <= ?");
        assertParameters(parameters, "Example Event", "C172S", 7, 7, 2);
    }

    @Test
    void eventSeverityRuleSupportsGenericAndAirframeSpecificEvents() {
        ArrayList<Object> parameters = new ArrayList<>();
        String genericQuery = filterOf("Event Severity", "Example Event", "=", "2.5").getRuleQuery(11, parameters);

        assertContainsAll(genericQuery, "events.flight_id", "events.event_definition_id IN", "events.severity = ?");
        assertParameters(parameters, "Example Event", 11, "2.5");

        parameters.clear();
        String airframeQuery = filterOf("Event Severity", "Example Event - C172S", ">=", "2.5")
                .getRuleQuery(11, parameters);

        assertContainsAll(
                airframeQuery,
                "JOIN airframes a ON a.id = ed.airframe_id",
                "a.airframe = ?",
                "a.fleet_id = ?",
                "events.severity >= ?");
        assertParameters(parameters, "Example Event", "C172S", 11, 11, "2.5");
    }

    @Test
    void eventDurationRuleSupportsGenericAndAirframeSpecificEvents() {
        ArrayList<Object> parameters = new ArrayList<>();
        String genericQuery = filterOf("Event Duration", "Example Event", ">", "10").getRuleQuery(4, parameters);

        assertContainsAll(genericQuery, "events.event_definition_id IN", "((events.end_line - events.start_line) + 1) > ?");
        assertParameters(parameters, "Example Event", 4, "10");

        parameters.clear();
        String airframeQuery = filterOf("Event Duration", "Example Event - C172S", "<", "10")
                .getRuleQuery(4, parameters);

        assertContainsAll(
                airframeQuery,
                "JOIN airframes a ON a.id = ed.airframe_id",
                "a.airframe = ?",
                "((events.end_line - events.start_line) + 1) < ?");
        assertParameters(parameters, "Example Event", "C172S", 4, 4, "10");
    }

    @Test
    void tagRuleSupportsAssociatedAndNotAssociated() {
        ArrayList<Object> parameters = new ArrayList<>();
        String associatedQuery = filterOf("Tag", "Checkride", "Is Associated").getRuleQuery(3, parameters);

        assertContainsAll(associatedQuery, "EXISTS", "flight_tags WHERE fleet_id = ? AND name = ?");
        assertParameters(parameters, 3, "Checkride");

        parameters.clear();
        String notAssociatedQuery = filterOf("Tag", "Checkride", "Is Not Associated").getRuleQuery(3, parameters);

        assertContainsAll(notAssociatedQuery, "NOT EXISTS", "flight_tags WHERE fleet_id = ? AND name = ?");
        assertParameters(parameters, 3, "Checkride");
    }

    @Test
    void groupToQueryStringCombinesChildRulesAndPreservesParameterOrder() {
        Filter group = new Filter("OR");
        group.addFilter(filterOf("Start Date", ">=", "2026-03-09"));
        group.addFilter(filterOf("Flight ID", "<", "100"));

        ArrayList<Object> parameters = new ArrayList<>();
        String query = group.toQueryString(13, parameters);

        assertContainsAll(query, "(DATE(flights.start_time) >= ?)", " OR ", "(flights.fleet_id = ? AND flights.id < ?)");
        assertParameters(parameters, "2026-03-09", 13, "100");
    }
}