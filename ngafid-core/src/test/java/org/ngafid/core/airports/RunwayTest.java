package org.ngafid.core.airports;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RunwayTest {
    @BeforeAll
    static void setup() {
        // Sample CSV data for airports
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
            geo.computeIfAbsent(airport.geoHash, k -> new ArrayList<>()).add(airport);
        }
        Airports.injectTestData(iata, site, geo);
    }

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
        double dist = runway.getDistanceFt(1.0, 0.5);
        assertTrue(dist > 0);
    }
}
