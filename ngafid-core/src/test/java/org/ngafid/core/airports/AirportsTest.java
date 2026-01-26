package org.ngafid.core.airports;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AirportsTest {
    @BeforeAll
    static void setup() {
        String csvData = "0,AAA,111,test,0.0,0.0\n" +
                         "0,BBB,222,test,1.0,1.0\n" +
                         "0,CCC,333,medium,2.0,2.0\n";
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
            geo.computeIfAbsent(airport.geoHash, k -> new ArrayList<>()).add(airport);
        }
        Airports.injectTestData(iata, site, geo);
    }

    @Test
    void testCalculateDistanceInKilometer() {
        double dist = Airports.calculateDistanceInKilometer(0.0, 0.0, 0.0, 1.0);
        assertTrue(dist > 0);
    }

    @Test
    void testCalculateDistanceInMeter() {
        double dist = Airports.calculateDistanceInMeter(0.0, 0.0, 0.0, 1.0);
        assertTrue(dist > 0);
    }

    @Test
    void testCalculateDistanceInFeet() {
        double dist = Airports.calculateDistanceInFeet(0.0, 0.0, 0.0, 1.0);
        assertTrue(dist > 0);
    }

    @Test
    void testShortestDistanceBetweenLineAndPointFt() {
        double dist = Airports.shortestDistanceBetweenLineAndPointFt(1.0, 0.5, 0.0, 0.0, 0.0, 1.0);
        assertTrue(dist > 0);
    }

    @Test
    void testGetAirportsMap() {
        List<String> codes = List.of("AAA", "BBB");
        Map<String, Airport> map = Airports.getAirports(codes);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("AAA"));
        assertTrue(map.containsKey("BBB"));
        assertFalse(map.containsKey("CCC"));
    }

    @Test
    void testHasRunwayInfoFalse() {
        assertFalse(Airports.hasRunwayInfo("NONEXISTENT"));
    }

    @Test
    void testGetNearestAirportWithinNull() {
        MutableDouble dist = new MutableDouble(Double.MAX_VALUE);
        Airport nearest = Airports.getNearestAirportWithin(10.0, 10.0, 1.0, dist);
        assertNull(nearest);
        assertEquals(Double.MAX_VALUE, dist.doubleValue());
    }

    @Test
    void testGetNearestAirportWithinFindsAirport() {
        MutableDouble dist = new MutableDouble(Double.MAX_VALUE);
        // Use coordinates that match the first airport in the test data (0.0, 0.0)
        Airport nearest = Airports.getNearestAirportWithin(0.0, 0.0, 10000.0, dist);
        assertNotNull(nearest);
        assertEquals("AAA", nearest.iataCode);
        assertTrue(dist.doubleValue() < 10000.0);
    }
}
