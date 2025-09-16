package org.ngafid.core.airports;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RunwayTest {
    @Test
    void testConstructorNoCoordinates() {
        Runway runway = new Runway("123", "RWY1");
        assertEquals("123", runway.siteNumber);
        assertEquals("RWY1", runway.name);
        assertFalse(runway.hasCoordinates);
        assertTrue(Double.isNaN(runway.lat1));
        assertTrue(Double.isNaN(runway.lon1));
        assertTrue(Double.isNaN(runway.lat2));
        assertTrue(Double.isNaN(runway.lon2));
        assertTrue(runway.toString().contains("RWY1"));
    }

    @Test
    void testConstructorWithCoordinates() {
        Runway runway = new Runway("456", "RWY2", 10.0, 20.0, 11.0, 21.0);
        assertEquals("456", runway.siteNumber);
        assertEquals("RWY2", runway.name);
        assertTrue(runway.hasCoordinates);
        assertEquals(10.0, runway.lat1);
        assertEquals(20.0, runway.lon1);
        assertEquals(11.0, runway.lat2);
        assertEquals(21.0, runway.lon2);
        assertTrue(runway.toString().contains("RWY2"));
    }

    @Test
    void testGetDistanceFt() {
        Runway runway = new Runway("789", "RWY3", 0.0, 0.0, 0.0, 1.0);
        double dist = runway.getDistanceFt(0.0, 0.5);
        assertTrue(dist > 0);
    }
}
