package org.ngafid.core.airports;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AirportsTest {
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
        double dist = Airports.shortestDistanceBetweenLineAndPointFt(0.0, 0.5, 0.0, 0.0, 0.0, 1.0);
        assertTrue(dist > 0);
    }

    @Test
    void testGetAirportsMap() {
        // This test assumes Airports.getAirport returns null for unknown codes
        List<String> codes = List.of("AAA", "BBB");
        Map<String, Airport> map = Airports.getAirports(codes);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("AAA"));
        assertTrue(map.containsKey("BBB"));
    }

    @Test
    void testHasRunwayInfoFalse() {
        assertFalse(Airports.hasRunwayInfo("NONEXISTENT"));
    }

    @Test
    void testGetNearestAirportWithinNull() {
        MutableDouble dist = new MutableDouble(Double.MAX_VALUE);
        Airport nearest = Airports.getNearestAirportWithin(0.0, 0.0, 1.0, dist);
        assertNull(nearest);
        assertEquals(Double.MAX_VALUE, dist.doubleValue());
    }
}
