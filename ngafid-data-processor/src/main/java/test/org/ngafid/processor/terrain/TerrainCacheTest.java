package test.org.ngafid.processor.terrain..terrain;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.ngafid.common.terrain.TerrainCache.getAltitudeFt;

public class TerrainCacheTest {
    private void altitudeTest(double latitude, double longitude, double expectedAltitude) {
        try {
            ///  msl is altitude doubled because we run max(0, msl - fileAltitudeFt)
            double actual = getAltitudeFt(expectedAltitude * 2, latitude, longitude);
            assertEquals(expectedAltitude, actual, 30);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetFilenameFromLatLon() {
        // Northeast
        String actual = TerrainCache.getFilenameFromLatLon(90, 90);
        assertEquals("N90E090.hgt", actual);

        // Southeast
        actual = TerrainCache.getFilenameFromLatLon(-90, 90);
        assertEquals("S90E090.hgt", actual);

        // Northwest
        actual = TerrainCache.getFilenameFromLatLon(90, -90);
        assertEquals("N90W090.hgt", actual);

        // Southwest
        actual = TerrainCache.getFilenameFromLatLon(-90, -90);
        assertEquals("S90W090.hgt", actual);
    }

    @Test
    public void testGetAlbanyAltitudeFt() {
        altitudeTest(42.74871, -73.80550, 267.0);
    }

    @Test
    public void testGetGrandForksAltitudeFt() {
        // Grand Forks
        altitudeTest(47.94286, -97.17658, 838.0);
    }

    @Test
    public void testGetDenverAltitudeFt() {
        altitudeTest(39.85610, -104.67374, 5373.0);
    }

    @Test
    public void testGetRochesterAltitudeFt() {
        altitudeTest(43.12252, -77.66657, 542.0);
    }

    @Test
    public void testGetPhoenixAltitudeFt() {
        altitudeTest(33.43727, -112.00779, 1124.0);
    }
}
