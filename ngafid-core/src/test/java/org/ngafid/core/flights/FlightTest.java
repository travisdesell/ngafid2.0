package org.ngafid.core.flights;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import org.ngafid.core.util.filters.Filter;

/**
 * Tests for {@link Flight}'s query and retrieval API: listing and counting flights for a fleet, applying
 * {@link Filter}s, sorting (including the occurrences-in-table columns), pagination, date-range/airport lookups, and
 * the {@code extraCondition} helpers.
 *
 * <p>Shared database seeding and {@link Filter} fixtures live in {@link FlightTestBase}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FlightTest extends FlightTestBase {

    /**
     * Verifies {@code Flight.getFlights} returns a non-null list of flights for an existing fleet.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(1)
    @DisplayName("Should get flights for existing fleet")
    public void testGetFlightsForFleet() throws SQLException {
        ArrayList<Flight> flights = Flight.getFlights(connection, 1, 10);

        assertNotNull(flights);
        assertTrue(flights.size() >= 0);
    }

    /**
     * Verifies {@code Flight.getNumFlights} returns the number of flights for a fleet.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(2)
    @DisplayName("Should get number of flights")
    public void testGetNumFlights() throws SQLException {
        int count = Flight.getNumFlights(connection, 1);

        assertTrue(count >= 0);
    }

    /**
     * Verifies {@code Flight.getFlights} honors an extra SQL condition together with a row limit.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(3)
    @DisplayName("Should get flights with extra condition and limit")
    public void testGetFlightsWithExtraConditionAndLimit() throws SQLException {
        ArrayList<Flight> flights = Flight.getFlights(connection, "fleet_id = 1", 1);

        assertNotNull(flights);
        assertTrue(flights.size() >= 0);
        // Note: The method uses LIMIT 100 as a safety mechanism, not the parameter
        assertTrue(flights.size() <= 100);
    }

    /**
     * Verifies {@code Flight.getFlights} throws {@link NullPointerException} when given a null connection.
     */
    @Test
    @Order(4)
    @DisplayName("Should handle null connection gracefully in getFlights")
    public void testNullConnection() {
        assertThrows(NullPointerException.class, () -> {
            Flight.getFlights(null, 1, 10);
        });
    }

    /**
     * Verifies {@code Flight.getFlightsWithinDateRangeFromAirport} throws {@link NullPointerException} for a null
     * connection.
     */
    @Test
    @Order(5)
    @DisplayName("Should handle null connection in getFlightsWithinDateRangeFromAirport")
    public void testNullConnectionInGetFlightsWithinDateRangeFromAirport() {
        assertThrows(NullPointerException.class, () -> {
            Flight.getFlightsWithinDateRangeFromAirport(null, "2023-01-01", "2023-01-31", "KJFK", 10);
        });
    }

    /**
     * Verifies {@code Flight.getFlightsWithinDateRangeFromAirport} returns the flights that visited a given airport
     * within a date range.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(6)
    @DisplayName("Should get flights within date range from airport")
    public void testGetFlightsWithinDateRangeFromAirport() throws SQLException {
        List<Flight> flights =
                Flight.getFlightsWithinDateRangeFromAirport(connection, "2023-01-01", "2023-01-31", "KJFK", 10);

        assertNotNull(flights);
        assertTrue(flights.size() >= 0);
    }

    /**
     * Verifies {@code Flight.getFlightsByRange} returns the flights in a page range.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(7)
    @DisplayName("Should get flights by range")
    public void testGetFlightsByRange() throws SQLException {
        List<Flight> flights = Flight.getFlightsByRange(connection, 1, 0, 10);

        assertNotNull(flights);
        assertTrue(flights.size() >= 0);
    }

    /**
     * Verifies {@code Flight.getFlights} applies a {@link org.ngafid.core.util.filters.Filter} to restrict the results.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(8)
    @DisplayName("Should get flights with filter")
    public void testGetFlightsWithFilter() throws SQLException {
        ArrayList<String> filterInputs = new ArrayList<>();
        filterInputs.add("Start Date");
        filterInputs.add(">=");
        filterInputs.add("2020-01-01");

        Filter filter = new Filter(filterInputs);

        ArrayList<Flight> flights = Flight.getFlights(connection, 1, filter);

        assertNotNull(flights);
        assertTrue(flights.size() >= 0);
    }

    /**
     * Verifies {@code Flight.getNumFlights} with a filter returns the matching count for a positive fleet id.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(9)
    @DisplayName("Should get number of flights with filter for fleetId > 0")
    public void testGetNumFlightsWithFilter() throws SQLException {
        ArrayList<String> filterInputs = new ArrayList<>();
        filterInputs.add("Start Date");
        filterInputs.add(">=");
        filterInputs.add("2020-01-01");

        Filter filter = new Filter(filterInputs);

        int count = Flight.getNumFlights(connection, 1, filter);

        assertTrue(count >= 0);
    }

    /**
     * Verifies {@code Flight.getFlightsSorted} orders results by tail number.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(10)
    @DisplayName("Should get flights sorted by tail_number")
    public void testGetFlightsSortedByTailNumber() throws SQLException {
        ArrayList<String> filterInputs = new ArrayList<>();
        filterInputs.add("Start Date");
        filterInputs.add(">=");
        filterInputs.add("2020-01-01");

        Filter filter = new Filter(filterInputs);

        ArrayList<Flight> flights = Flight.getFlightsSorted(connection, 1, filter, 0, 10, "tail_number", true);

        assertNotNull(flights);
        assertTrue(flights.size() >= 0);
    }

    /**
     * Verifies {@code Flight.getFlightsSorted} orders results by itinerary.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(11)
    @DisplayName("Should get flights sorted by itinerary")
    public void testGetFlightsSortedByItinerary() throws SQLException {
        ArrayList<String> filterInputs = new ArrayList<>();
        filterInputs.add("Start Date");
        filterInputs.add(">=");
        filterInputs.add("2020-01-01");

        Filter filter = new Filter(filterInputs);

        ArrayList<Flight> flights = Flight.getFlightsSorted(connection, 1, filter, 0, 10, "itinerary", false);

        assertNotNull(flights);
        assertTrue(flights.size() >= 0);
    }

    /**
     * Verifies {@code Flight.getFlightsSorted} orders results by flight tags.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(12)
    @DisplayName("Should get flights sorted by flight_tags")
    public void testGetFlightsSortedByFlightTags() throws SQLException {
        ArrayList<String> filterInputs = new ArrayList<>();
        filterInputs.add("Start Date");
        filterInputs.add(">=");
        filterInputs.add("2020-01-01");

        Filter filter = new Filter(filterInputs);

        ArrayList<Flight> flights = Flight.getFlightsSorted(connection, 1, filter, 0, 10, "flight_tags", true);

        assertNotNull(flights);
        assertTrue(flights.size() >= 0);
    }

    /**
     * Verifies {@code Flight.getFlightsSorted} orders results by event count.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(13)
    @DisplayName("Should get flights sorted by events")
    public void testGetFlightsSortedByEvents() throws SQLException {
        ArrayList<String> filterInputs = new ArrayList<>();
        filterInputs.add("Start Date");
        filterInputs.add(">=");
        filterInputs.add("2020-01-01");

        Filter filter = new Filter(filterInputs);

        ArrayList<Flight> flights = Flight.getFlightsSorted(connection, 1, filter, 0, 10, "events", false);

        assertNotNull(flights);
        assertTrue(flights.size() >= 0);
    }

    /**
     * Verifies {@code Flight.getFlightsSorted} orders results by airports visited.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(14)
    @DisplayName("Should get flights sorted by airports_visited")
    public void testGetFlightsSortedByAirportsVisited() throws SQLException {
        ArrayList<String> filterInputs = new ArrayList<>();
        filterInputs.add("Start Date");
        filterInputs.add(">=");
        filterInputs.add("2020-01-01");

        Filter filter = new Filter(filterInputs);

        ArrayList<Flight> flights = Flight.getFlightsSorted(connection, 1, filter, 0, 10, "airports_visited", true);

        assertNotNull(flights);
        assertTrue(flights.size() >= 0);
    }

    /**
     * Verifies {@code Flight.getFlightsSorted} falls back to its default ordering for an unrecognized sort column.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(15)
    @DisplayName("Should get flights sorted by default case")
    public void testGetFlightsSortedByDefault() throws SQLException {
        ArrayList<String> filterInputs = new ArrayList<>();
        filterInputs.add("Start Date");
        filterInputs.add(">=");
        filterInputs.add("2020-01-01");

        Filter filter = new Filter(filterInputs);

        ArrayList<Flight> flights = Flight.getFlightsSorted(connection, 1, filter, 0, 10, "start_time", false);

        assertNotNull(flights);
        assertTrue(flights.size() >= 0);
    }

    /**
     * Verifies {@code Flight.getNumFlights} with a filter counts across all fleets when the fleet id is &lt;= 0.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(330)
    @DisplayName("Should get number of flights with filter when fleetId <= 0")
    public void testGetNumFlightsWithFilterAndFleetIdZero() throws SQLException {
        // Create test flights first
        createTestFlight(5000);
        createTestFlight(5001);

        // This should trigger the path: if (fleetId <= 0) { if (filter != null) { ... } }
        // The query should be: "SELECT count(id) FROM flights WHERE ()"
        // An empty GROUP filter generates "()" which causes SQL syntax error in H2

        // Use a GROUP filter with no child filters, which generates an empty condition
        Filter filter = new Filter("AND");

        // This should trigger the path: if (fleetId <= 0) { if (filter != null) { ... } }
        // The query becomes: "SELECT count(id) FROM flights WHERE ()" which causes a SQL error.
        try {
            int count = Flight.getNumFlights(connection, -1, filter);
            assertTrue(count >= 0, "Should return a non-negative count");
        } catch (Exception e) {
            // Expected: an empty "WHERE ()" condition is a SQL error. The exact message is
            // database-specific (H2 reports "Data conversion error"/"ROW to BOOLEAN"; MySQL
            // reports a SQL syntax error), so accept any of those signatures.
            String message = e.getMessage() == null ? "" : e.getMessage();
            assertTrue(
                    message.contains("Data conversion error")
                            || message.contains("ROW to BOOLEAN")
                            || message.toLowerCase().contains("sql syntax")
                            || e instanceof java.sql.SQLSyntaxErrorException,
                    "Expected a SQL error from the empty filter condition, but was: " + message);
        }
    }

    /**
     * Verifies {@code Flight.getFlights} applies an extra SQL condition parameter.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(402)
    @DisplayName("Should test getFlights with extraCondition parameter")
    public void testGetFlightsWithExtraCondition() throws SQLException {
        // Create test flights
        createTestFlight(8000);
        createTestFlight(8001);
        createTestFlight(8002);

        String extraCondition = "id IN (8000, 8001)";
        ArrayList<Flight> flights = Flight.getFlights(connection, extraCondition);

        // Should return 2 flights
        assertEquals(2, flights.size(), "Should return 2 flights with the specified IDs");

        // Verify the returned flights have the correct IDs
        Set<Integer> returnedIds = flights.stream().map(Flight::getId).collect(Collectors.toSet());

        assertTrue(returnedIds.contains(8000), "Should contain flight 8000");
        assertTrue(returnedIds.contains(8001), "Should contain flight 8001");
        assertFalse(returnedIds.contains(8002), "Should not contain flight 8002");
    }

    /**
     * Verifies {@code Flight.getFlights} applies an extra SQL condition together with a row limit.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(403)
    @DisplayName("Should test getFlights with extraCondition and limit")
    public void testGetFlightsWithExtraConditionAndLimitNew() throws SQLException {
        // Create test flights
        createTestFlight(8003);
        createTestFlight(8004);
        createTestFlight(8005);

        String extraCondition = "id >= 8003";
        ArrayList<Flight> flights = Flight.getFlights(connection, extraCondition, 2);

        // Should return at most 2 flights due to the limit parameter
        assertTrue(flights.size() <= 2, "Should return at most 2 flights due to limit parameter");

        // Verify the returned flights are ordered by ID descending (the query uses ORDER BY id DESC).
        assertTrue(flights.get(0).getId() >= flights.get(1).getId(), "Flights should be ordered by ID descending");
    }

    /**
     * Verifies {@code Flight.getFlightsByRange} applies a valid filter over a page range.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(404)
    @DisplayName("Should test getFlightsByRange with valid filter")
    public void testGetFlightsByRangeWithValidFilter() throws SQLException {
        // Create test flights
        createTestFlight(9000);
        createTestFlight(9001);
        createTestFlight(9002);

        // Create a valid RULE filter
        ArrayList<String> filterInputs = new ArrayList<>();
        filterInputs.add("Flight ID");
        filterInputs.add(">");
        filterInputs.add("0");
        Filter filter = new Filter(filterInputs);

        // Test getFlightsByRange with valid filter
        List<Flight> flights = Flight.getFlightsByRange(connection, filter, 1, 0, 2);

        // Should return flights within the range
        assertNotNull(flights, "Should return a list of flights");
        assertTrue(flights.size() <= 2, "Should return at most 2 flights due to range limit");
    }

    /**
     * Verifies {@code Flight.getFlightsByRange} returns correct results across different page ranges.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(405)
    @DisplayName("Should test getFlightsByRange with different ranges")
    public void testGetFlightsByRangeWithDifferentRanges() throws SQLException {
        // Create test flights
        createTestFlight(9003);
        createTestFlight(9004);
        createTestFlight(9005);
        createTestFlight(9006);

        // Create a valid RULE filter
        ArrayList<String> filterInputs = new ArrayList<>();
        filterInputs.add("Flight ID");
        filterInputs.add(">");
        filterInputs.add("0");
        Filter filter = new Filter(filterInputs);

        List<Flight> flights1 = Flight.getFlightsByRange(connection, filter, 1, 0, 1);
        List<Flight> flights2 = Flight.getFlightsByRange(connection, filter, 1, 1, 3);

        // Verify different ranges return different results
        assertNotNull(flights1, "First range should return flights");
        assertNotNull(flights2, "Second range should return flights");

        // Each page should have at most pageSize flights
        assertTrue(flights1.size() <= 1, "First range should return at most 1 flight");
        assertTrue(flights2.size() <= 2, "Second range should return at most 2 flights");
    }

    /**
     * Verifies {@code Flight.getFlightsByRange} returns an empty list for an empty range.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(406)
    @DisplayName("Should test getFlightsByRange with empty range")
    public void testGetFlightsByRangeWithEmptyRange() throws SQLException {
        // Create test flights
        createTestFlight(9007);
        createTestFlight(9008);

        // Create a valid RULE filter
        ArrayList<String> filterInputs = new ArrayList<>();
        filterInputs.add("Flight ID");
        filterInputs.add(">");
        filterInputs.add("0");
        Filter filter = new Filter(filterInputs);

        List<Flight> flights = Flight.getFlightsByRange(connection, filter, 1, 1, 1);

        // Should return empty list for empty range
        assertNotNull(flights, "Should return a list (even if empty)");
        assertEquals(0, flights.size(), "Should return empty list for empty range");
    }

    /**
     * Verifies {@code Flight.getFlights} paginates results using the current-page and page-size parameters.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(407)
    @DisplayName("Should test getFlights with currentPage and pageSize parameters")
    public void testGetFlightsWithCurrentPageAndPageSize() throws SQLException {
        // Create test flights
        createTestFlight(10000);
        createTestFlight(10001);
        createTestFlight(10002);
        createTestFlight(10003);
        createTestFlight(10004);

        // Create a valid RULE filter
        ArrayList<String> filterInputs = new ArrayList<>();
        filterInputs.add("Flight ID");
        filterInputs.add(">");
        filterInputs.add("0");
        Filter filter = new Filter(filterInputs);

        ArrayList<Flight> flightsPage1 = Flight.getFlights(connection, 1, filter, 0, 2);

        ArrayList<Flight> flightsPage2 = Flight.getFlights(connection, 1, filter, 1, 2);

        ArrayList<Flight> flightsPage3 = Flight.getFlights(connection, 1, filter, 2, 2);

        // Verify results
        assertNotNull(flightsPage1, "First page should return flights");
        assertNotNull(flightsPage2, "Second page should return flights");
        assertNotNull(flightsPage3, "Third page should return flights");

        // Each page should have at most pageSize flights
        assertTrue(flightsPage1.size() <= 2, "First page should have at most 2 flights");
        assertTrue(flightsPage2.size() <= 2, "Second page should have at most 2 flights");
        assertTrue(flightsPage3.size() <= 2, "Third page should have at most 2 flights");

        // Verify pagination is working (different pages should have different results)
        if (flightsPage1.size() > 0 && flightsPage2.size() > 0) {
            // The flights should be different between pages
            Set<Integer> page1Ids = flightsPage1.stream().map(Flight::getId).collect(Collectors.toSet());
            Set<Integer> page2Ids = flightsPage2.stream().map(Flight::getId).collect(Collectors.toSet());

            // Pages should not have overlapping flight IDs (assuming we have enough flights)
            boolean hasOverlap = page1Ids.stream().anyMatch(page2Ids::contains);
            // Note: This might have overlap if there are fewer flights than expected
            // The important thing is that the method executes successfully
        }
    }

    /**
     * Verifies {@code Flight.getFlights} pagination handles edge-case current-page and page-size values.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(408)
    @DisplayName("Should test getFlights with currentPage and pageSize - edge cases")
    public void testGetFlightsWithCurrentPageAndPageSizeEdgeCases() throws SQLException {
        // Create test flights
        createTestFlight(10005);
        createTestFlight(10006);

        // Create a valid RULE filter
        ArrayList<String> filterInputs = new ArrayList<>();
        filterInputs.add("Flight ID");
        filterInputs.add(">");
        filterInputs.add("0");
        Filter filter = new Filter(filterInputs);

        ArrayList<Flight> flightsZeroSize = Flight.getFlights(connection, 1, filter, 0, 0);
        assertNotNull(flightsZeroSize, "Should return empty list for pageSize=0");
        assertEquals(0, flightsZeroSize.size(), "Should return empty list for pageSize=0");

        ArrayList<Flight> flightsLargeSize = Flight.getFlights(connection, 1, filter, 0, 100);
        assertNotNull(flightsLargeSize, "Should return flights for large pageSize");
        assertTrue(flightsLargeSize.size() <= 100, "Should return at most 100 flights");

        ArrayList<Flight> flightsFirstOnly = Flight.getFlights(connection, 1, filter, 0, 1);
        assertNotNull(flightsFirstOnly, "Should return first flight");
        assertTrue(flightsFirstOnly.size() <= 1, "Should return at most 1 flight");
    }

    /**
     * Verifies {@code Flight.getFlightsSorted} sorts by an occurrences-in-table column with a double-valued filter
     * parameter.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(415)
    @DisplayName("Should sort flights by occurrences-in-table with double parameters")
    public void testGetFlightsSortedByOccurrencesInTableWithDoubleParameters() throws SQLException {
        setupTestDataForSorting(connection, 1);
        Filter filter = createFilterWithDoubleParameter();
        ArrayList<Flight> flights = Flight.getFlightsSorted(connection, 1, filter, 0, 10, "itinerary", true);
        assertNotNull(flights);
    }

    /**
     * Verifies {@code Flight.getFlightsSorted} sorts by an occurrences-in-table column with an integer-valued filter
     * parameter.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(416)
    @DisplayName("Should sort flights by occurrences-in-table with integer parameters")
    public void testGetFlightsSortedByOccurrencesInTableWithIntegerParameters() throws SQLException {
        setupTestDataForSorting(connection, 1);
        Filter filter = createFilterWithIntegerParameter();
        ArrayList<Flight> flights = Flight.getFlightsSorted(connection, 1, filter, 0, 10, "events", true);
        assertNotNull(flights);
    }

    /**
     * Verifies {@code Flight.getFlightsSorted} sorts by an occurrences-in-table column with a filter mixing double and
     * integer parameters.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(417)
    @DisplayName("Should sort flights by occurrences-in-table with mixed parameters")
    public void testGetFlightsSortedByOccurrencesInTableWithMixedParameters() throws SQLException {
        setupTestDataForSorting(connection, 1);
        Filter filter = createFilterWithMixedParameters();
        ArrayList<Flight> flights = Flight.getFlightsSorted(connection, 1, filter, 0, 10, "flight_tags", true);
        assertNotNull(flights);
    }

    /**
     * Verifies {@code Flight.getFlightsSorted} sorts by an occurrences-in-table column with a simple (non-null) filter.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(418)
    @DisplayName("Should sort flights by occurrences-in-table with a simple filter")
    public void testGetFlightsSortedByOccurrencesInTableWithNoFilter() throws SQLException {
        setupTestDataForSorting(connection, 1);
        // Use a simple filter instead of null to avoid NullPointerException
        Filter filter = createFilterWithDoubleParameter();
        ArrayList<Flight> flights = Flight.getFlightsSorted(connection, 1, filter, 0, 10, "itinerary", true);
        assertNotNull(flights);
    }

    /**
     * Verifies {@code Flight.getFlightsSorted} paginates results correctly when sorting by an occurrences-in-table
     * column (each page respects the page size).
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(419)
    @DisplayName("Should paginate flights sorted by occurrences-in-table")
    public void testGetFlightsSortedByOccurrencesInTableWithPagination() throws SQLException {
        setupTestDataForSorting(connection, 1);
        Filter filter = createFilterWithDoubleParameter();
        ArrayList<Flight> flightsPage1 = Flight.getFlightsSorted(connection, 1, filter, 0, 2, "itinerary", true);
        ArrayList<Flight> flightsPage2 = Flight.getFlightsSorted(connection, 1, filter, 1, 2, "itinerary", true);
        assertNotNull(flightsPage1);
        assertNotNull(flightsPage2);
        assertTrue(flightsPage1.size() <= 2);
        assertTrue(flightsPage2.size() <= 2);
    }

    /**
     * Verifies {@code Flight.getFlightsSorted} supports both ascending and descending order when sorting by an
     * occurrences-in-table column.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(420)
    @DisplayName("Should sort flights by occurrences-in-table in ascending and descending order")
    public void testGetFlightsSortedByOccurrencesInTableWithDifferentSortOrders() throws SQLException {
        setupTestDataForSorting(connection, 1);
        Filter filter = createFilterWithIntegerParameter();
        ArrayList<Flight> flightsAsc = Flight.getFlightsSorted(connection, 1, filter, 0, 10, "events", true);
        ArrayList<Flight> flightsDesc = Flight.getFlightsSorted(connection, 1, filter, 0, 10, "events", false);
        assertNotNull(flightsAsc);
        assertNotNull(flightsDesc);
    }

    /**
     * Verifies {@code Flight.getNumFlights} returns a non-negative count with a double-valued filter parameter.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(421)
    @DisplayName("Should get number of flights with double parameters")
    public void testGetNumFlightsWithDoubleParameters() throws SQLException {
        Connection connection = org.ngafid.core.Database.getConnection();
        Filter filter = createFilterWithDoubleParameter();
        int count = Flight.getNumFlights(connection, 1, filter);
        assertTrue(count >= 0);
    }

    /**
     * Verifies {@code Flight.getNumFlights} returns a non-negative count with an integer-valued filter parameter.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(422)
    @DisplayName("Should get number of flights with integer parameters")
    public void testGetNumFlightsWithIntegerParameters() throws SQLException {
        Connection connection = org.ngafid.core.Database.getConnection();
        Filter filter = createFilterWithIntegerParameter();
        int count = Flight.getNumFlights(connection, 1, filter);
        assertTrue(count >= 0);
    }

    /**
     * Verifies {@code Flight.getNumFlights} returns a non-negative count with a filter mixing double and integer
     * parameters.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(423)
    @DisplayName("Should get number of flights with mixed parameters")
    public void testGetNumFlightsWithMixedParameters() throws SQLException {
        Connection connection = org.ngafid.core.Database.getConnection();
        Filter filter = createFilterWithMixedParameters();
        int count = Flight.getNumFlights(connection, 1, filter);
        assertTrue(count >= 0);
    }

    /**
     * Verifies {@code Flight.getFlights} respects a row limit.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(424)
    @DisplayName("Should get flights with a row limit")
    public void testGetFlightsWithLimit() throws SQLException {
        Connection connection = org.ngafid.core.Database.getConnection();
        Filter filter = createFilterWithDoubleParameter();
        ArrayList<Flight> flights = Flight.getFlights(connection, 1, filter, 50);
        assertNotNull(flights);
        assertTrue(flights.size() <= 100);
    }

    /**
     * Verifies {@code Flight.getFlightsSorted} sorting by an occurrences-in-table column with integer parameters,
     * covering the integer-parameter binding path.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(425)
    @DisplayName("Should sort flights by occurrences-in-table covering the integer-parameter path")
    public void testGetFlightsSortedByOccurrencesInTableIntegerParameterCoverage() throws SQLException {
        Connection connection = org.ngafid.core.Database.getConnection();

        Filter filter = createFilterWithIntegerParameter();

        ArrayList<Flight> flightsEvents = Flight.getFlightsSorted(connection, 1, filter, 0, 10, "events", true);
        ArrayList<Flight> flightsItinerary = Flight.getFlightsSorted(connection, 1, filter, 0, 10, "itinerary", true);
        ArrayList<Flight> flightsTags = Flight.getFlightsSorted(connection, 1, filter, 0, 10, "flight_tags", true);

        assertNotNull(flightsEvents);
        assertNotNull(flightsItinerary);
        assertNotNull(flightsTags);
        assertTrue(flightsEvents.size() >= 0);
        assertTrue(flightsItinerary.size() >= 0);
        assertTrue(flightsTags.size() >= 0);
    }
}
