package org.ngafid.core.airports;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AirportTest {
    /**
     * Seeds the shared {@link Airports} lookup maps with one synthetic airport (with two runways) before any test runs,
     * providing a stable fixture for the airport/runway tests.
     */
    @BeforeAll
    public static void setup() {
        String csvData = "0,GHI,789,large,50.0,60.0\n";
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
            Runway runway1 = new Runway(siteNumber, "RWY1", 50.1, 60.1, 50.2, 60.2);
            Runway runway2 = new Runway(siteNumber, "RWY2", 51.0, 61.0, 51.1, 61.1);
            airport.addRunway(runway1);
            airport.addRunway(runway2);
            iata.put(iataCode, airport);
            site.put(siteNumber, airport);
            geo.computeIfAbsent(airport.getGeoHash(), k -> new ArrayList<>()).add(airport);
        }
        Airports.injectTestData(iata, site, geo);
    }

    /**
     * Verifies the {@link Airport} constructor stores the IATA code, site number, type, and coordinates, and that
     * {@code toString} includes the IATA code.
     */
    @Test
    @DisplayName("Should construct an airport and expose its fields via toString")
    public void testConstructorAndToString() {
        Airport airport = new Airport("ABC", "123", "small", 10.0, 20.0);
        assertEquals("ABC", airport.getIataCode());
        assertEquals("123", airport.getSiteNumber());
        assertEquals("small", airport.getType());
        assertEquals(10.0, airport.getLatitude());
        assertEquals(20.0, airport.getLongitude());
        assertTrue(airport.toString().contains("ABC"));
    }

    /**
     * Verifies adding a runway to an {@link Airport} updates the runway count and {@code hasRunways}, and that the
     * runway is found by name and in the runway collection.
     */
    @Test
    @DisplayName("Should add and look up runways on an airport")
    public void testRunwayManagement() {
        Airport airport = new Airport("DEF", "456", "medium", 30.0, 40.0);
        Runway runway = new Runway("456", "RWY1", 30.1, 40.1, 30.2, 40.2);
        airport.addRunway(runway);
        assertEquals(1, airport.getNumberRunways());
        assertTrue(airport.hasRunways());
        assertEquals(runway, airport.getRunway("RWY1"));
        assertTrue(airport.getRunways().contains(runway));
    }

    /**
     * Verifies {@link Airport#getNearestRunwayWithin} returns the closest runway within the distance limit and writes
     * its distance into the out-parameter.
     */
    @Test
    @DisplayName("Should find the nearest runway within a distance limit")
    public void testGetNearestRunwayWithin() {
        Airport airport = new Airport("GHI", "789", "large", 50.0, 60.0);
        Runway runway1 = new Runway("789", "RWY1", 50.1, 60.1, 50.2, 60.2);
        Runway runway2 = new Runway("789", "RWY2", 51.0, 61.0, 51.1, 61.1);
        airport.addRunway(runway1);
        airport.addRunway(runway2);
        MutableDouble dist = new MutableDouble(Double.MAX_VALUE);
        Runway nearest = airport.getNearestRunwayWithin(50.15, 60.15, 10000.0, dist);
        assertNotNull(nearest);
        assertEquals("RWY1", nearest.getName());
        assertTrue(dist.doubleValue() < 10000.0);
    }

    /**
     * Verifies an airport with no runways reports {@code hasRunways() == false}, a zero count, and null for an unknown
     * runway name.
     */
    @Test
    @DisplayName("Should report no runways for an airport without any")
    public void testNoRunways() {
        Airport airport = new Airport("JKL", "101", "none", 0.0, 0.0);
        assertFalse(airport.hasRunways());
        assertEquals(0, airport.getNumberRunways());
        assertNull(airport.getRunway("NONEXISTENT"));
    }
}
