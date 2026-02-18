package org.ngafid.core.airports;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RunwayTest {
    @BeforeAll
    static void setup() {
        String csvData = "0,XYZ,999,test,0.0,0.0\n";
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

    @Test
    void testConstructorNoCoordinates() {
        Runway runway = new Runway("123", "RWY1");
        assertEquals("123", runway.getSiteNumber());
        assertEquals("RWY1", runway.getName());
        assertFalse(runway.hasCoordinates());
        assertTrue(Double.isNaN(runway.getLat1()));
        assertTrue(Double.isNaN(runway.getLon1()));
        assertTrue(Double.isNaN(runway.getLat2()));
        assertTrue(Double.isNaN(runway.getLon2()));
        assertTrue(runway.toString().contains("RWY1"));
    }

    @Test
    void testConstructorWithCoordinates() {
        Runway runway = new Runway("456", "RWY2", 10.0, 20.0, 11.0, 21.0);
        assertEquals("456", runway.getSiteNumber());
        assertEquals("RWY2", runway.getName());
        assertTrue(runway.hasCoordinates());
        assertEquals(10.0, runway.getLat1());
        assertEquals(20.0, runway.getLon1());
        assertEquals(11.0, runway.getLat2());
        assertEquals(21.0, runway.getLon2());
        assertTrue(runway.toString().contains("RWY2"));
    }

    @Test
    void testGetDistanceFt() {
        Runway runway = new Runway("789", "RWY3", 0.0, 0.0, 0.0, 1.0);
        double dist = runway.getDistanceFt(1.0, 0.5);
        assertTrue(dist > 0);
    }
}
