package org.ngafid.core.airports;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AirportsTest {
    /**
     * Seeds the shared {@link Airports} lookup maps with three synthetic airports before any test runs, providing a
     * stable fixture for the distance and lookup tests.
     */
    @BeforeAll
    public static void setup() {
        String csvData = "0,AAA,111,test,0.0,0.0\n" + "0,BBB,222,test,1.0,1.0\n" + "0,CCC,333,medium,2.0,2.0\n";
        Map<String, Airport> iata = new HashMap<>();
        Map<String, Airport> site = new HashMap<>();
        Map<String, ArrayList<Airport>> geo = new HashMap<>();
        for (String line : csvData.split("\\n")) {
            String[] values = line.split(",");
            String iataCode = values[1];
            String siteNumber = values[2];
            String type = values[3];
            double latitude = Double.parseDouble(values[4]);
            double longitude = Double.parseDouble(values[5]);
            Airport airport = new Airport(iataCode, siteNumber, type, latitude, longitude);
            iata.put(iataCode, airport);
            site.put(siteNumber, airport);
            geo.computeIfAbsent(airport.getGeoHash(), k -> new ArrayList<>()).add(airport);
        }
        Airports.injectTestData(iata, site, geo);
    }

    /**
     * Verifies {@link Airports#calculateDistanceInKilometer} returns a positive distance between two distinct points.
     */
    @Test
    @DisplayName("Should calculate distance between points in kilometers")
    public void testCalculateDistanceInKilometer() {
        double dist = Airports.calculateDistanceInKilometer(0.0, 0.0, 0.0, 1.0);
        assertTrue(dist > 0);
    }

    /**
     * Verifies {@link Airports#calculateDistanceInMeter} returns a positive distance between two distinct points.
     */
    @Test
    @DisplayName("Should calculate distance between points in meters")
    public void testCalculateDistanceInMeter() {
        double dist = Airports.calculateDistanceInMeter(0.0, 0.0, 0.0, 1.0);
        assertTrue(dist > 0);
    }

    /**
     * Verifies {@link Airports#calculateDistanceInFeet} returns a positive distance between two distinct points.
     */
    @Test
    @DisplayName("Should calculate distance between points in feet")
    public void testCalculateDistanceInFeet() {
        double dist = Airports.calculateDistanceInFeet(0.0, 0.0, 0.0, 1.0);
        assertTrue(dist > 0);
    }

    /**
     * Verifies {@link Airports#shortestDistanceBetweenLineAndPointFt} returns a positive perpendicular distance from a
     * point to a line segment.
     */
    @Test
    @DisplayName("Should calculate shortest distance between a line and a point")
    public void testShortestDistanceBetweenLineAndPointFt() {
        double dist = Airports.shortestDistanceBetweenLineAndPointFt(1.0, 0.5, 0.0, 0.0, 0.0, 1.0);
        assertTrue(dist > 0);
    }

    /**
     * Verifies {@link Airports#getAirports} returns only the airports for the requested IATA codes.
     */
    @Test
    @DisplayName("Should return a map of airports for the requested codes")
    public void testGetAirportsMap() {
        List<String> codes = List.of("AAA", "BBB");
        Map<String, Airport> map = Airports.getAirports(codes);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("AAA"));
        assertTrue(map.containsKey("BBB"));
        assertFalse(map.containsKey("CCC"));
    }

    /**
     * Verifies {@link Airports#hasRunwayInfo} returns false for an unknown airport code.
     */
    @Test
    @DisplayName("Should report no runway info for an unknown airport")
    public void testHasRunwayInfoFalse() {
        assertFalse(Airports.hasRunwayInfo("NONEXISTENT"));
    }

    /**
     * Verifies {@link Airports#getNearestAirportWithin} returns null (and leaves the distance out-parameter unchanged)
     * when no airport is within range.
     */
    @Test
    @DisplayName("Should return null when no airport is within range")
    public void testGetNearestAirportWithinNull() {
        MutableDouble dist = new MutableDouble(Double.MAX_VALUE);
        Airport nearest = Airports.getNearestAirportWithin(10.0, 10.0, 1.0, dist);
        assertNull(nearest);
        assertEquals(Double.MAX_VALUE, dist.doubleValue());
    }

    /**
     * Verifies {@link Airports#getNearestAirportWithin} returns the nearest airport and sets the distance out-parameter
     * when one is within range.
     */
    @Test
    @DisplayName("Should find the nearest airport within range")
    public void testGetNearestAirportWithinFindsAirport() {
        MutableDouble dist = new MutableDouble(Double.MAX_VALUE);
        // Use coordinates that match the first airport in the test data (0.0, 0.0)
        Airport nearest = Airports.getNearestAirportWithin(0.0, 0.0, 10000.0, dist);
        assertNotNull(nearest);
        assertEquals("AAA", nearest.getIataCode());
        assertTrue(dist.doubleValue() < 10000.0);
    }
}
