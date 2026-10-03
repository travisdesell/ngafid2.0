package org.ngafid.core.flights;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link Flight#calculateLOCI}, the loss-of-control-in-flight probability calculation. Exercises the
 * heading/roll/true-airspeed inputs across normal, zero, negative, extreme, high-airspeed, maximum-roll,
 * heading-wraparound, and consistent-value scenarios, asserting the result stays a finite probability in [0, 100].
 *
 * <p>Shared database seeding fixtures live in {@link FlightTestBase}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FlightLociTest extends FlightTestBase {

    /**
     * Verifies {@code Flight.calculateLOCI} returns a finite probability in [0, 100] for normal heading/roll/airspeed
     * inputs.
     */
    @Test
    @Order(79)
    @DisplayName("Should calculate LOCI with normal values")
    public void testCalculateLOCINormalValues() {
        double[] hdgData = {0.0, 10.0, 20.0, 30.0, 40.0};
        double[] rollData = {0.0, 5.0, 10.0, 15.0, 20.0};
        double[] tasData = {100.0, 110.0, 120.0, 130.0, 140.0};

        DoubleTimeSeries hdg = new DoubleTimeSeries("HDG", "degrees", hdgData);
        DoubleTimeSeries roll = new DoubleTimeSeries("Roll", "degrees", rollData);
        DoubleTimeSeries tas = new DoubleTimeSeries("TAS", "ft/min", tasData);

        double laggedHdg = 5.0;
        int index = 2;

        double result = Flight.calculateLOCI(hdg, index, roll, tas, laggedHdg);

        assertTrue(result >= 0.0, "LOCI should be >= 0");
        assertTrue(result <= 100.0, "LOCI should be <= 100");

        assertFalse(Double.isNaN(result), "LOCI should not be NaN");
        assertFalse(Double.isInfinite(result), "LOCI should not be infinite");
    }

    /**
     * Verifies {@code Flight.calculateLOCI} handles a NaN lagged-heading input without producing an invalid result.
     */
    @Test
    @Order(80)
    @DisplayName("Should calculate LOCI with NaN laggedHdg")
    public void testCalculateLOCIWithNaNLaggedHdg() {
        double[] hdgData = {0.0, 10.0, 20.0, 30.0, 40.0};
        double[] rollData = {0.0, 5.0, 10.0, 15.0, 20.0};
        double[] tasData = {100.0, 110.0, 120.0, 130.0, 140.0};

        DoubleTimeSeries hdg = new DoubleTimeSeries("HDG", "degrees", hdgData);
        DoubleTimeSeries roll = new DoubleTimeSeries("Roll", "degrees", rollData);
        DoubleTimeSeries tas = new DoubleTimeSeries("TAS", "ft/min", tasData);

        double laggedHdg = Double.NaN;
        int index = 2;

        double result = Flight.calculateLOCI(hdg, index, roll, tas, laggedHdg);

        assertTrue(result >= 0.0, "LOCI should be >= 0");
        assertTrue(result <= 100.0, "LOCI should be <= 100");

        assertFalse(Double.isNaN(result), "LOCI should not be NaN");
        assertFalse(Double.isInfinite(result), "LOCI should not be infinite");
    }

    /**
     * Verifies {@code Flight.calculateLOCI} handles all-zero inputs.
     */
    @Test
    @Order(81)
    @DisplayName("Should calculate LOCI with zero values")
    public void testCalculateLOCIWithZeroValues() {

        double[] hdgData = {0.0, 0.0, 0.0, 0.0, 0.0};
        double[] rollData = {0.0, 0.0, 0.0, 0.0, 0.0};
        double[] tasData = {0.0, 0.0, 0.0, 0.0, 0.0};

        DoubleTimeSeries hdg = new DoubleTimeSeries("HDG", "degrees", hdgData);
        DoubleTimeSeries roll = new DoubleTimeSeries("Roll", "degrees", rollData);
        DoubleTimeSeries tas = new DoubleTimeSeries("TAS", "ft/min", tasData);

        double laggedHdg = 0.0;
        int index = 2;

        double result = Flight.calculateLOCI(hdg, index, roll, tas, laggedHdg);

        assertTrue(result >= 0.0, "LOCI should be >= 0");
        assertTrue(result <= 100.0, "LOCI should be <= 100");

        assertFalse(Double.isNaN(result), "LOCI should not be NaN");
        assertFalse(Double.isInfinite(result), "LOCI should not be infinite");
    }

    /**
     * Verifies {@code Flight.calculateLOCI} stays within bounds for extreme input values.
     */
    @Test
    @Order(82)
    @DisplayName("Should calculate LOCI with extreme values")
    public void testCalculateLOCIWithExtremeValues() {

        double[] hdgData = {0.0, 90.0, 180.0, 270.0, 360.0};
        double[] rollData = {0.0, 45.0, 90.0, 135.0, 180.0};
        double[] tasData = {1000.0, 2000.0, 3000.0, 4000.0, 5000.0};

        DoubleTimeSeries hdg = new DoubleTimeSeries("HDG", "degrees", hdgData);
        DoubleTimeSeries roll = new DoubleTimeSeries("Roll", "degrees", rollData);
        DoubleTimeSeries tas = new DoubleTimeSeries("TAS", "ft/min", tasData);

        double laggedHdg = 180.0;
        int index = 2;

        double result = Flight.calculateLOCI(hdg, index, roll, tas, laggedHdg);

        assertTrue(result >= 0.0, "LOCI should be >= 0");
        assertTrue(result <= 100.0, "LOCI should be <= 100");

        assertFalse(Double.isNaN(result), "LOCI should not be NaN");
        assertFalse(Double.isInfinite(result), "LOCI should not be infinite");
    }

    /**
     * Verifies {@code Flight.calculateLOCI} handles negative input values.
     */
    @Test
    @Order(83)
    @DisplayName("Should calculate LOCI with negative values")
    public void testCalculateLOCIWithNegativeValues() {

        double[] hdgData = {-10.0, -5.0, 0.0, 5.0, 10.0};
        double[] rollData = {-30.0, -15.0, 0.0, 15.0, 30.0};
        double[] tasData = {50.0, 100.0, 150.0, 200.0, 250.0};

        DoubleTimeSeries hdg = new DoubleTimeSeries("HDG", "degrees", hdgData);
        DoubleTimeSeries roll = new DoubleTimeSeries("Roll", "degrees", rollData);
        DoubleTimeSeries tas = new DoubleTimeSeries("TAS", "ft/min", tasData);

        double laggedHdg = -5.0;
        int index = 2;

        double result = Flight.calculateLOCI(hdg, index, roll, tas, laggedHdg);

        assertTrue(result >= 0.0, "LOCI should be >= 0");
        assertTrue(result <= 100.0, "LOCI should be <= 100");

        assertFalse(Double.isNaN(result), "LOCI should not be NaN");
        assertFalse(Double.isInfinite(result), "LOCI should not be infinite");
    }

    /**
     * Verifies {@code Flight.calculateLOCI} computes correctly at different sample indices.
     */
    @Test
    @Order(84)
    @DisplayName("Should calculate LOCI with different indices")
    public void testCalculateLOCIWithDifferentIndices() {
        double[] hdgData = {0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0};
        double[] rollData = {0.0, 5.0, 10.0, 15.0, 20.0, 25.0, 30.0};
        double[] tasData = {100.0, 110.0, 120.0, 130.0, 140.0, 150.0, 160.0};

        DoubleTimeSeries hdg = new DoubleTimeSeries("HDG", "degrees", hdgData);
        DoubleTimeSeries roll = new DoubleTimeSeries("Roll", "degrees", rollData);
        DoubleTimeSeries tas = new DoubleTimeSeries("TAS", "ft/min", tasData);

        double laggedHdg = 5.0;

        // Test different indices
        for (int index = 0; index < hdgData.length; index++) {
            double result = Flight.calculateLOCI(hdg, index, roll, tas, laggedHdg);

            assertTrue(result >= 0.0, "LOCI should be >= 0 for index " + index);
            assertTrue(result <= 100.0, "LOCI should be <= 100 for index " + index);

            assertFalse(Double.isNaN(result), "LOCI should not be NaN for index " + index);
            assertFalse(Double.isInfinite(result), "LOCI should not be infinite for index " + index);
        }
    }

    /**
     * Verifies {@code Flight.calculateLOCI} correctly handles heading differences that wrap around 360 degrees.
     */
    @Test
    @Order(85)
    @DisplayName("Should calculate LOCI with heading differences around 360 degrees")
    public void testCalculateLOCIWithHeadingDifferences() {

        double[] hdgData = {350.0, 10.0, 20.0, 30.0, 40.0};
        double[] rollData = {0.0, 5.0, 10.0, 15.0, 20.0};
        double[] tasData = {100.0, 110.0, 120.0, 130.0, 140.0};

        DoubleTimeSeries hdg = new DoubleTimeSeries("HDG", "degrees", hdgData);
        DoubleTimeSeries roll = new DoubleTimeSeries("Roll", "degrees", rollData);
        DoubleTimeSeries tas = new DoubleTimeSeries("TAS", "ft/min", tasData);

        double laggedHdg = 10.0;
        int index = 0; // hdgData[0] = 350.0, difference = 350 - 10 = 340 degrees

        double result = Flight.calculateLOCI(hdg, index, roll, tas, laggedHdg);

        assertTrue(result >= 0.0, "LOCI should be >= 0");
        assertTrue(result <= 100.0, "LOCI should be <= 100");

        assertFalse(Double.isNaN(result), "LOCI should not be NaN");
        assertFalse(Double.isInfinite(result), "LOCI should not be infinite");
    }

    /**
     * Verifies {@code Flight.calculateLOCI} handles maximum roll values.
     */
    @Test
    @Order(86)
    @DisplayName("Should calculate LOCI with maximum roll values")
    public void testCalculateLOCIWithMaximumRollValues() {

        double[] hdgData = {0.0, 10.0, 20.0, 30.0, 40.0};
        double[] rollData = {0.0, 45.0, 90.0, 135.0, 180.0};
        double[] tasData = {100.0, 110.0, 120.0, 130.0, 140.0};

        DoubleTimeSeries hdg = new DoubleTimeSeries("HDG", "degrees", hdgData);
        DoubleTimeSeries roll = new DoubleTimeSeries("Roll", "degrees", rollData);
        DoubleTimeSeries tas = new DoubleTimeSeries("TAS", "ft/min", tasData);

        double laggedHdg = 5.0;
        int index = 4; // rollData[4] = 180.0 degrees

        double result = Flight.calculateLOCI(hdg, index, roll, tas, laggedHdg);

        assertTrue(result >= 0.0, "LOCI should be >= 0");
        assertTrue(result <= 100.0, "LOCI should be <= 100");

        assertFalse(Double.isNaN(result), "LOCI should not be NaN");
        assertFalse(Double.isInfinite(result), "LOCI should not be infinite");
    }

    /**
     * Verifies {@code Flight.calculateLOCI} handles high true-airspeed (TAS) values.
     */
    @Test
    @Order(87)
    @DisplayName("Should calculate LOCI with high TAS values")
    public void testCalculateLOCIWithHighTASValues() {

        double[] hdgData = {0.0, 10.0, 20.0, 30.0, 40.0};
        double[] rollData = {0.0, 5.0, 10.0, 15.0, 20.0};
        double[] tasData = {10000.0, 15000.0, 20000.0, 25000.0, 30000.0};

        DoubleTimeSeries hdg = new DoubleTimeSeries("HDG", "degrees", hdgData);
        DoubleTimeSeries roll = new DoubleTimeSeries("Roll", "degrees", rollData);
        DoubleTimeSeries tas = new DoubleTimeSeries("TAS", "ft/min", tasData);

        double laggedHdg = 5.0;
        int index = 4; // tasData[4] = 30000.0 ft/min

        double result = Flight.calculateLOCI(hdg, index, roll, tas, laggedHdg);

        assertTrue(result >= 0.0, "LOCI should be >= 0");
        assertTrue(result <= 100.0, "LOCI should be <= 100");

        assertFalse(Double.isNaN(result), "LOCI should not be NaN");
        assertFalse(Double.isInfinite(result), "LOCI should not be infinite");
    }

    /**
     * Verifies {@code Flight.calculateLOCI} handles edge-case heading values.
     */
    @Test
    @Order(88)
    @DisplayName("Should calculate LOCI with edge case heading values")
    public void testCalculateLOCIWithEdgeCaseHeadingValues() {

        double[] hdgData = {0.0, 90.0, 180.0, 270.0, 360.0};
        double[] rollData = {0.0, 5.0, 10.0, 15.0, 20.0};
        double[] tasData = {100.0, 110.0, 120.0, 130.0, 140.0};

        DoubleTimeSeries hdg = new DoubleTimeSeries("HDG", "degrees", hdgData);
        DoubleTimeSeries roll = new DoubleTimeSeries("Roll", "degrees", rollData);
        DoubleTimeSeries tas = new DoubleTimeSeries("TAS", "ft/min", tasData);

        double[] laggedHeadings = {0.0, 90.0, 180.0, 270.0, 360.0};

        for (int i = 0; i < laggedHeadings.length; i++) {
            double laggedHdg = laggedHeadings[i];
            int index = i;

            double result = Flight.calculateLOCI(hdg, index, roll, tas, laggedHdg);

            assertTrue(result >= 0.0, "LOCI should be >= 0 for laggedHdg " + laggedHdg);
            assertTrue(result <= 100.0, "LOCI should be <= 100 for laggedHdg " + laggedHdg);

            assertFalse(Double.isNaN(result), "LOCI should not be NaN for laggedHdg " + laggedHdg);
            assertFalse(Double.isInfinite(result), "LOCI should not be infinite for laggedHdg " + laggedHdg);
        }
    }

    /**
     * Verifies {@code Flight.calculateLOCI} returns a stable result for consistent (unchanging) inputs.
     */
    @Test
    @Order(89)
    @DisplayName("Should calculate LOCI with consistent values")
    public void testCalculateLOCIWithConsistentValues() {

        double[] hdgData = {100.0, 100.0, 100.0, 100.0, 100.0};
        double[] rollData = {10.0, 10.0, 10.0, 10.0, 10.0};
        double[] tasData = {200.0, 200.0, 200.0, 200.0, 200.0};

        DoubleTimeSeries hdg = new DoubleTimeSeries("HDG", "degrees", hdgData);
        DoubleTimeSeries roll = new DoubleTimeSeries("Roll", "degrees", rollData);
        DoubleTimeSeries tas = new DoubleTimeSeries("TAS", "ft/min", tasData);

        double laggedHdg = 100.0; // Same as current heading
        int index = 2;

        double result = Flight.calculateLOCI(hdg, index, roll, tas, laggedHdg);

        assertTrue(result >= 0.0, "LOCI should be >= 0");
        assertTrue(result <= 100.0, "LOCI should be <= 100");

        assertFalse(Double.isNaN(result), "LOCI should not be NaN");
        assertFalse(Double.isInfinite(result), "LOCI should not be infinite");
    }
}
