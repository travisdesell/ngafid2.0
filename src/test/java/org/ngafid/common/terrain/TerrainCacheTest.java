package org.ngafid.common.terrain;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.ngafid.common.terrain.TerrainCache.getAltitudeFt;

public class TerrainCacheTest {
    private void altitudeTest(double msl, double latitude, double longitude, double expectedAltitude) {
        try {
            double actual = getAltitudeFt(msl, latitude, longitude);
            System.out.println("Expected: " + expectedAltitude + ", Actual: " + actual);
            assertEquals(expectedAltitude, actual, 0.1);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetFilenameFromLatLon() {
        // Northeast
        String actual = TerrainCache.getFilenameFromLatLon(90, 90);
        assertEquals("N090E090.hgt", actual);

        // Southeast
        actual = TerrainCache.getFilenameFromLatLon(-90, 90);
        assertEquals("S090E090.hgt", actual);

        // Northwest
        actual = TerrainCache.getFilenameFromLatLon(90, -90);
        assertEquals("N090W090.hgt", actual);

        // Southwest
        actual = TerrainCache.getFilenameFromLatLon(-90, -90);
        assertEquals("S090W090.hgt", actual);
    }

    @Test
    public void testGetAlbanyAltitudeFt() {
        altitudeTest(0, 42.74871, -73.80550, 267.0);
    }

    @Test
    public void testGetGrandForksAltitudeFt() {
        // Grand Forks
        altitudeTest(0, 47.94286, -97.17658, 838.0);
    }

    @Test
    public void testGetDenverAltitudeFt() {
        altitudeTest(0, 39.85610, -104.67374, 5373.0);
    }

    @Test
    public void testGetRochesterAltitudeFt() {
        altitudeTest(0, 43.12252, -77.66657, 542.0);
    }

    @Test
    public void testGetPhoenixAltitudeFt() {
        altitudeTest(0, 33.43727, -112.00779, 1124.0);
    }
}
