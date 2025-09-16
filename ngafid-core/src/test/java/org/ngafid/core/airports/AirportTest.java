package org.ngafid.core.airports;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirportTest {
    @Test
    void testConstructorAndToString() {
        Airport airport = new Airport("ABC", "123", "small", 10.0, 20.0);
        assertEquals("ABC", airport.iataCode);
        assertEquals("123", airport.siteNumber);
        assertEquals("small", airport.type);
        assertEquals(10.0, airport.latitude);
        assertEquals(20.0, airport.longitude);
        assertTrue(airport.toString().contains("ABC"));
    }

    @Test
    void testRunwayManagement() {
        Airport airport = new Airport("DEF", "456", "medium", 30.0, 40.0);
        Runway runway = new Runway("456", "RWY1", 30.1, 40.1, 30.2, 40.2);
        airport.addRunway(runway);
        assertEquals(1, airport.getNumberRunways());
        assertTrue(airport.hasRunways());
        assertEquals(runway, airport.getRunway("RWY1"));
        assertTrue(airport.getRunways().contains(runway));
    }

    @Test
    void testGetNearestRunwayWithin() {
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

    @Test
    void testNoRunways() {
        Airport airport = new Airport("JKL", "101", "none", 0.0, 0.0);
        assertFalse(airport.hasRunways());
        assertEquals(0, airport.getNumberRunways());
        assertNull(airport.getRunway("NONEXISTENT"));
    }
}

