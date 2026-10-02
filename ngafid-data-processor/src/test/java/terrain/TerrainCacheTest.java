package terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.ngafid.processor.terrain.TerrainCache.getAltitudeFt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ngafid.processor.terrain.TerrainCache;

public class TerrainCacheTest {
    private void altitudeTest(double latitude, double longitude, double expectedAltitude) {
        try {
            ///  msl is altitude doubled because we run max(0, msl - fileAltitudeFt)
            double actual = getAltitudeFt(expectedAltitude * 2, latitude, longitude);
            assertEquals(expectedAltitude, actual, 30);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Verifies that the SRTM tile file name is built with the correct hemisphere prefixes (N/S, E/W) and
     * zero-padded degrees for all four latitude/longitude quadrants.
     */
    @Test
    @DisplayName("Should build the SRTM tile filename for all lat/lon quadrants")
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

    /**
     * Verifies that {@code getAltitudeFt} returns Albany, NY's known ground elevation (~267 ft) within tolerance.
     */
    @Test
    @DisplayName("Should return Albany's ground altitude within tolerance")
    public void testGetAlbanyAltitudeFt() {
        altitudeTest(42.74871, -73.80550, 267.0);
    }

    /**
     * Verifies that {@code getAltitudeFt} returns Grand Forks, ND's known ground elevation (~838 ft) within
     * tolerance.
     */
    @Test
    @DisplayName("Should return Grand Forks' ground altitude within tolerance")
    public void testGetGrandForksAltitudeFt() {
        // Grand Forks
        altitudeTest(47.94286, -97.17658, 838.0);
    }

    /**
     * Verifies that {@code getAltitudeFt} returns Denver, CO's known high-elevation ground altitude (~5373 ft)
     * within tolerance.
     */
    @Test
    @DisplayName("Should return Denver's high-elevation ground altitude within tolerance")
    public void testGetDenverAltitudeFt() {
        altitudeTest(39.85610, -104.67374, 5373.0);
    }

    /**
     * Verifies that {@code getAltitudeFt} returns Rochester, NY's known ground elevation (~542 ft) within
     * tolerance.
     */
    @Test
    @DisplayName("Should return Rochester's ground altitude within tolerance")
    public void testGetRochesterAltitudeFt() {
        altitudeTest(43.12252, -77.66657, 542.0);
    }

    /**
     * Verifies that {@code getAltitudeFt} returns Phoenix, AZ's known ground elevation (~1124 ft) within
     * tolerance.
     */
    @Test
    @DisplayName("Should return Phoenix's ground altitude within tolerance")
    public void testGetPhoenixAltitudeFt() {
        altitudeTest(33.43727, -112.00779, 1124.0);
    }
}
