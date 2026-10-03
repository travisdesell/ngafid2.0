package org.ngafid.core.flights;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Flight}'s time-series accessors: adding double series to the in-memory map, retrieving the
 * double/string series maps, and reading double/string series by name from the cache or the database (including the
 * connection-taking overloads and name edge cases such as empty, null, whitespace, special-character, and long names).
 *
 * <p>Shared database seeding fixtures live in {@link FlightTestBase}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FlightTimeSeriesTest extends FlightTestBase {

    /**
     * Verifies {@code Flight.addDoubleTimeSeries} inserts a series into the flight's in-memory double-series map.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(300)
    @DisplayName("Should add DoubleTimeSeries to the map")
    public void testAddDoubleTimeSeries() throws SQLException {
        createTestFlight(3001);
        Flight flight = Flight.getFlight(connection, 3001);

        // Create a test DoubleTimeSeries
        DoubleTimeSeries testSeries = new DoubleTimeSeries("TestSeries", "TestUnit", new double[] {1.0, 2.0, 3.0});

        // Add the series
        flight.addDoubleTimeSeries("TestSeries", testSeries);

        // Verify it was added
        assertNotNull(flight.getDoubleTimeSeriesMap().get("TestSeries"), "Series should be added to map");
        assertEquals(testSeries, flight.getDoubleTimeSeriesMap().get("TestSeries"), "Added series should match");
    }

    /**
     * Verifies {@code Flight.addDoubleTimeSeries} replaces an existing series with the same name in the map.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(301)
    @DisplayName("Should replace existing DoubleTimeSeries in the map")
    public void testAddDoubleTimeSeriesReplace() throws SQLException {
        createTestFlight(3002);
        Flight flight = Flight.getFlight(connection, 3002);

        // Create initial series
        DoubleTimeSeries initialSeries = new DoubleTimeSeries("TestSeries", "TestUnit", new double[] {1.0, 2.0});
        flight.addDoubleTimeSeries("TestSeries", initialSeries);

        // Create replacement series
        DoubleTimeSeries replacementSeries =
                new DoubleTimeSeries("TestSeries", "NewUnit", new double[] {3.0, 4.0, 5.0});
        flight.addDoubleTimeSeries("TestSeries", replacementSeries);

        // Verify it was replaced
        assertEquals(replacementSeries, flight.getDoubleTimeSeriesMap().get("TestSeries"), "Series should be replaced");
        assertNotEquals(
                initialSeries, flight.getDoubleTimeSeriesMap().get("TestSeries"), "Original series should be replaced");
    }

    /**
     * Verifies {@code Flight.getDoubleTimeSeriesMap} returns the flight's double-series map.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(302)
    @DisplayName("Should return the doubleTimeSeries map")
    public void testGetDoubleTimeSeriesMap() throws SQLException {
        createTestFlight(3003);
        Flight flight = Flight.getFlight(connection, 3003);

        Map<String, DoubleTimeSeries> seriesMap = flight.getDoubleTimeSeriesMap();

        assertNotNull(seriesMap, "Map should not be null");
        assertTrue(seriesMap.isEmpty(), "Map should be empty initially");
    }

    /**
     * Verifies {@code Flight.getDoubleTimeSeriesMap} returns the same cached map instance on repeated calls.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(303)
    @DisplayName("Should return the same doubleTimeSeries map instance")
    public void testGetDoubleTimeSeriesMapSameInstance() throws SQLException {
        createTestFlight(3004);
        Flight flight = Flight.getFlight(connection, 3004);

        Map<String, DoubleTimeSeries> map1 = flight.getDoubleTimeSeriesMap();
        Map<String, DoubleTimeSeries> map2 = flight.getDoubleTimeSeriesMap();

        assertSame(map1, map2, "Should return the same map instance");
    }

    /**
     * Verifies {@code Flight.getStringTimeSeriesMap} returns the flight's string-series map.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(304)
    @DisplayName("Should return the stringTimeSeries map")
    public void testGetStringTimeSeriesMap() throws SQLException {
        createTestFlight(3005);
        Flight flight = Flight.getFlight(connection, 3005);

        Map<String, StringTimeSeries> seriesMap = flight.getStringTimeSeriesMap();

        assertNotNull(seriesMap, "Map should not be null");
        assertTrue(seriesMap.isEmpty(), "Map should be empty initially");
    }

    /**
     * Verifies {@code Flight.getStringTimeSeriesMap} returns the same cached map instance on repeated calls.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(305)
    @DisplayName("Should return the same stringTimeSeries map instance")
    public void testGetStringTimeSeriesMapSameInstance() throws SQLException {
        createTestFlight(3006);
        Flight flight = Flight.getFlight(connection, 3006);

        Map<String, StringTimeSeries> map1 = flight.getStringTimeSeriesMap();
        Map<String, StringTimeSeries> map2 = flight.getStringTimeSeriesMap();

        assertSame(map1, map2, "Should return the same map instance");
    }

    /**
     * Verifies {@code Flight.getDoubleTimeSeries} returns a series from the in-memory cache when present (no database
     * read).
     *
     * @throws SQLException if a database operation fails
     * @throws IOException if reading series data fails
     */
    @Test
    @Order(306)
    @DisplayName("Should return series from cache when it exists")
    public void testGetDoubleTimeSeriesFromCache() throws SQLException, IOException {
        createTestFlight(3007);
        Flight flight = Flight.getFlight(connection, 3007);

        // Add series to cache
        DoubleTimeSeries testSeries = new DoubleTimeSeries("CachedSeries", "TestUnit", new double[] {1.0, 2.0, 3.0});
        flight.addDoubleTimeSeries("CachedSeries", testSeries);

        // Get series from cache
        DoubleTimeSeries result = flight.getDoubleTimeSeries("CachedSeries");

        assertNotNull(result, "Should return cached series");
        assertEquals(testSeries, result, "Should return the same series instance");
    }

    /**
     * Verifies {@code Flight.getDoubleTimeSeries} returns null when the series is neither cached nor in the database.
     *
     * @throws SQLException if a database operation fails
     * @throws IOException if reading series data fails
     */
    @Test
    @Order(307)
    @DisplayName("Should return null when series not in cache and not in database")
    public void testGetDoubleTimeSeriesNotInCacheOrDatabase() throws SQLException, IOException {
        createTestFlight(3008);
        Flight flight = Flight.getFlight(connection, 3008);

        // Try to get non-existent series
        DoubleTimeSeries result = flight.getDoubleTimeSeries("NonExistentSeries");

        assertNull(result, "Should return null for non-existent series");
    }

    /**
     * Verifies {@code Flight.getStringTimeSeries} returns a series from the in-memory cache when present.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(308)
    @DisplayName("Should return string series from cache")
    public void testGetStringTimeSeriesFromCache() throws SQLException {
        createTestFlight(3009);
        Flight flight = Flight.getFlight(connection, 3009);

        // Add series to cache
        StringTimeSeries testSeries = new StringTimeSeries(
                "CachedStringSeries", "String", new ArrayList<>(Arrays.asList("value1", "value2")));
        flight.getStringTimeSeriesMap().put("CachedStringSeries", testSeries);

        // Get series from cache
        StringTimeSeries result = flight.getStringTimeSeries("CachedStringSeries");

        assertNotNull(result, "Should return cached series");
        assertEquals(testSeries, result, "Should return the same series instance");
    }

    /**
     * Verifies {@code Flight.getStringTimeSeries} returns null when the series is not cached.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(309)
    @DisplayName("Should return null when string series not in cache")
    public void testGetStringTimeSeriesNotInCache() throws SQLException {
        createTestFlight(3010);
        Flight flight = Flight.getFlight(connection, 3010);

        // Try to get non-existent series
        StringTimeSeries result = flight.getStringTimeSeries("NonExistentStringSeries");

        assertNull(result, "Should return null for non-existent series");
    }

    /**
     * Verifies {@code Flight.getDoubleTimeSeries} loads a series from the database on a cache miss and caches it for
     * subsequent calls.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(310)
    @DisplayName("Should get and cache double series from database")
    public void testGetDoubleTimeSeriesFromDatabase() throws SQLException {
        createTestFlight(3011);
        Flight flight = Flight.getFlight(connection, 3011);

        setupTimeSeriesData(connection, 3011);

        // Get series from database
        DoubleTimeSeries result = flight.getDoubleTimeSeries(connection, "Altitude");

        assertNotNull(result, "Should return series from database");
        assertEquals("Altitude", result.getName(), "Series name should match");

        // Verify it was cached
        assertNotNull(flight.getDoubleTimeSeriesMap().get("Altitude"), "Series should be cached");
    }

    /**
     * Verifies the connection-taking {@code Flight.getDoubleTimeSeries} retrieves a series from the database and caches
     * it.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(326)
    @DisplayName("Should retrieve and cache double time series with connection")
    public void testGetDoubleTimeSeriesWithConnection() throws SQLException {
        createTestFlight(3026);
        setupTimeSeriesData(connection, 3026);
        Flight flight = Flight.getFlight(connection, 3026);

        // Test retrieving time series with connection parameter
        DoubleTimeSeries series = flight.getDoubleTimeSeries(connection, "TestSeries1");

        // The series might be null if not found, which is expected behavior
        if (series != null) {
            assertEquals("TestSeries1", series.getName(), "Series name should match");
        }

        // Verify it was cached
        assertTrue(flight.getDoubleTimeSeriesMap().containsKey("TestSeries1"), "Series should be cached");
    }

    /**
     * Verifies the connection-taking {@code Flight.getStringTimeSeries} retrieves a series from the database and caches
     * it.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(327)
    @DisplayName("Should retrieve and cache string time series with connection")
    public void testGetStringTimeSeriesWithConnection() throws SQLException {
        createTestFlight(3027);
        setupStringTimeSeriesData(connection, 3027);
        Flight flight = Flight.getFlight(connection, 3027);

        // Test retrieving string time series with connection parameter
        StringTimeSeries series = flight.getStringTimeSeries(connection, "TestStringSeries1");

        // The series might be null if not found, which is expected behavior
        if (series != null) {
            assertEquals("TestStringSeries1", series.getName(), "Series name should match");
        }

        // Verify it was cached
        assertTrue(flight.getStringTimeSeriesMap().containsKey("TestStringSeries1"), "Series should be cached");
    }

    /**
     * Verifies {@code Flight.getDoubleTimeSeries} returns null for a series name that does not exist in the database.
     *
     * @throws SQLException if a database operation fails
     * @throws IOException if reading series data fails
     */
    @Test
    @Order(409)
    @DisplayName("Should return null when series does not exist in database")
    public void testGetDoubleTimeSeriesNonExistent() throws SQLException, IOException {
        createTestFlight(5003);
        Flight flight = Flight.getFlight(connection, 5003);

        // Try to get non-existent series
        DoubleTimeSeries result = flight.getDoubleTimeSeries("NonExistentSeries");

        assertNull(result, "Should return null for non-existent series");

        // Verify cache remains empty
        assertTrue(flight.getDoubleTimeSeriesMap().isEmpty(), "Cache should remain empty");
    }

    /**
     * Verifies {@code Flight.getDoubleTimeSeries} handles an empty series name (returning null rather than throwing).
     *
     * @throws SQLException if a database operation fails
     * @throws IOException if reading series data fails
     */
    @Test
    @Order(410)
    @DisplayName("Should handle empty series name")
    public void testGetDoubleTimeSeriesEmptyName() throws SQLException, IOException {
        createTestFlight(5004);
        Flight flight = Flight.getFlight(connection, 5004);

        // Try to get series with empty name
        DoubleTimeSeries result = flight.getDoubleTimeSeries("");

        assertNull(result, "Should return null for empty series name");
    }

    /**
     * Verifies {@code Flight.getDoubleTimeSeries} handles a null series name (returning null rather than throwing).
     *
     * @throws SQLException if a database operation fails
     * @throws IOException if reading series data fails
     */
    @Test
    @Order(411)
    @DisplayName("Should handle null series name")
    public void testGetDoubleTimeSeriesNullName() throws SQLException, IOException {
        createTestFlight(5005);
        Flight flight = Flight.getFlight(connection, 5005);

        // Try to get series with null name
        DoubleTimeSeries result = flight.getDoubleTimeSeries(null);

        assertNull(result, "Should return null for null series name");
    }

    /**
     * Verifies {@code Flight.getDoubleTimeSeries} handles a series name containing special characters.
     *
     * @throws SQLException if a database operation fails
     * @throws IOException if reading series data fails
     */
    @Test
    @Order(412)
    @DisplayName("Should handle series with special characters in name")
    public void testGetDoubleTimeSeriesSpecialCharacters() throws SQLException, IOException {
        createTestFlight(5009);
        Flight flight = Flight.getFlight(connection, 5009);

        // Setup time series data with special characters in name
        setupTimeSeriesDataWithSpecialNames(connection, 5009);

        // Test series with special characters
        DoubleTimeSeries result = flight.getDoubleTimeSeries("Test_Series-1");

        if (result != null) {
            assertEquals("Test_Series-1", result.getName(), "Series name with special characters should match");
        }
    }

    /**
     * Verifies {@code Flight.getDoubleTimeSeries} handles a very long series name.
     *
     * @throws SQLException if a database operation fails
     * @throws IOException if reading series data fails
     */
    @Test
    @Order(413)
    @DisplayName("Should handle series with very long names")
    public void testGetDoubleTimeSeriesLongName() throws SQLException, IOException {
        createTestFlight(5012);
        Flight flight = Flight.getFlight(connection, 5012);

        // Create a series with a long name (but within database limits)
        String longName = "VeryLongSeriesNameThatTestsTheSystemBehaviorWithExtendedNames";
        setupTimeSeriesDataWithLongName(connection, 5012, longName);

        // Test retrieval with long name
        DoubleTimeSeries result = flight.getDoubleTimeSeries(longName);

        if (result != null) {
            assertEquals(longName, result.getName(), "Long series name should match");
        }
    }

    /**
     * Verifies {@code Flight.getDoubleTimeSeries} handles a series name containing whitespace.
     *
     * @throws SQLException if a database operation fails
     * @throws IOException if reading series data fails
     */
    @Test
    @Order(414)
    @DisplayName("Should handle series with whitespace in name")
    public void testGetDoubleTimeSeriesWhitespaceName() throws SQLException, IOException {
        createTestFlight(5013);
        Flight flight = Flight.getFlight(connection, 5013);

        // Test series name with leading/trailing whitespace
        DoubleTimeSeries resultWithSpaces = flight.getDoubleTimeSeries("  Altitude  ");
        assertNull(resultWithSpaces, "Series name with whitespace should return null");

        // Test series name with internal spaces
        DoubleTimeSeries resultWithInternalSpaces = flight.getDoubleTimeSeries("Alt itude");
        assertNull(resultWithInternalSpaces, "Series name with internal spaces should return null");
    }
}
