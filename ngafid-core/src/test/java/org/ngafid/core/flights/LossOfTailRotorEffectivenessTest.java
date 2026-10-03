package org.ngafid.core.flights;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LossOfTailRotorEffectivenessTest {

    /**
     * Verifies {@code classify} returns {@code NO_RISK} for a positive pedal margin.
     */
    @Test
    @DisplayName("Should classify a positive pedal margin as no risk")
    public void classifierReturnsNoRiskForPositivePedalMargin() {
        assertEquals(LossOfTailRotorEffectiveness.NO_RISK, LossOfTailRotorEffectiveness.classify(1.0, 0.0));
    }

    /**
     * Verifies {@code classify} returns {@code RISK} for a negative pedal margin when the yaw rate is below the event
     * threshold.
     */
    @Test
    @DisplayName("Should classify a negative pedal margin without high yaw as risk")
    public void classifierReturnsRiskForNegativePedalMarginWithoutHighYaw() {
        assertEquals(LossOfTailRotorEffectiveness.RISK, LossOfTailRotorEffectiveness.classify(-0.1, 20.0));
    }

    /**
     * Verifies {@code classify} returns {@code EVENT} once the yaw rate reaches the event threshold with a negative
     * pedal margin.
     */
    @Test
    @DisplayName("Should classify as event at the yaw-rate threshold")
    public void classifierReturnsEventBeforeRiskAtYawThreshold() {
        assertEquals(LossOfTailRotorEffectiveness.EVENT, LossOfTailRotorEffectiveness.classify(-0.1, 60.0));
    }

    /**
     * Verifies {@code classify} returns {@code EVENT} for a negative pedal margin with a yaw rate above the threshold.
     */
    @Test
    @DisplayName("Should classify as event above the yaw-rate threshold")
    public void classifierReturnsEventAboveYawThreshold() {
        assertEquals(LossOfTailRotorEffectiveness.EVENT, LossOfTailRotorEffectiveness.classify(-0.1, 80.0));
    }

    /**
     * Verifies {@code classify} returns {@code UNSUPPORTED} when either the pedal margin or yaw rate is NaN.
     */
    @Test
    @DisplayName("Should classify missing values as unsupported")
    public void classifierReturnsUnsupportedForMissingValues() {
        assertEquals(LossOfTailRotorEffectiveness.UNSUPPORTED, LossOfTailRotorEffectiveness.classify(Double.NaN, 80.0));
        assertEquals(LossOfTailRotorEffectiveness.UNSUPPORTED, LossOfTailRotorEffectiveness.classify(-0.1, Double.NaN));
    }

    /**
     * Verifies {@code isInsideModelEnvelope} treats the documented mu/sigma boundary values as inside and points just
     * outside them as outside.
     */
    @Test
    @DisplayName("Should include the documented model-envelope boundaries")
    public void modelEnvelopeIncludesDocumentedBoundaries() {
        assertFalse(LossOfTailRotorEffectiveness.isInsideModelEnvelope(0.049999, 0.05));
        assertTrue(LossOfTailRotorEffectiveness.isInsideModelEnvelope(0.05, 0.0));
        assertTrue(LossOfTailRotorEffectiveness.isInsideModelEnvelope(0.1277, 0.1233));
        assertFalse(LossOfTailRotorEffectiveness.isInsideModelEnvelope(0.127701, 0.05));
        assertFalse(LossOfTailRotorEffectiveness.isInsideModelEnvelope(0.06, -0.000001));
        assertFalse(LossOfTailRotorEffectiveness.isInsideModelEnvelope(0.06, 0.123301));
    }

    /**
     * Verifies {@code relativeWindDirectionBodyFrameDeg} wraps the body-frame relative wind direction into the [0, 360)
     * range across several heading/wind combinations.
     */
    @Test
    @DisplayName("Should wrap relative wind direction into the 0-360 range")
    public void relativeWindWrapsIntoZeroTo360Range() {
        assertEquals(0.0, LossOfTailRotorEffectiveness.relativeWindDirectionBodyFrameDeg(0.0, 0.0), 1e-9);
        assertEquals(90.0, LossOfTailRotorEffectiveness.relativeWindDirectionBodyFrameDeg(0.0, 90.0), 1e-9);
        assertEquals(270.0, LossOfTailRotorEffectiveness.relativeWindDirectionBodyFrameDeg(90.0, 0.0), 1e-9);
        assertEquals(20.0, LossOfTailRotorEffectiveness.relativeWindDirectionBodyFrameDeg(350.0, 10.0), 1e-9);
        assertEquals(340.0, LossOfTailRotorEffectiveness.relativeWindDirectionBodyFrameDeg(10.0, 350.0), 1e-9);
    }

    /**
     * Verifies {@code normalize} produces normalized inputs (relative wind, MRCT sigma, advance ratio mu, yaw rate)
     * from a database-backed helicopter spec with a valid blade chord.
     */
    @Test
    @DisplayName("Should normalize database-backed helicopter spec inputs")
    public void normalizesDbBackedHelicopterSpecInputs() {
        var spec = new LossOfTailRotorEffectiveness.HelicopterSpec(
                "TEST", 2_500.0, Double.NaN, 1_510.0, 4, 396.0, 10.0, 408.0);
        var normalized = LossOfTailRotorEffectiveness.normalize(spec, 90.0, 1.225, 20.0, 10.0, Double.NaN)
                .orElseThrow();
        assertEquals(90.0, normalized.relativeWindDeg(), 1e-9);
        assertTrue(normalized.mrctSigma() > 0.0);
        assertTrue(normalized.mu() > 0.0);
        assertEquals(10.0, normalized.yawRateDps(), 1e-9);
    }

    /**
     * Verifies {@code bestWeightLbs} prefers the midpoint of max and minimum flying weight over the empty weight when
     * selecting a representative weight.
     */
    @Test
    @DisplayName("Should prefer minimum flying weight over empty weight for best weight")
    public void bestWeightPrefersMinFlyingWeightOverEmptyWeight() {
        var spec = new LossOfTailRotorEffectiveness.HelicopterSpec(
                "TEST", 5_000.0, 2_000.0, 1_500.0, 4, 400.0, 10.0, 400.0);

        assertEquals(4_250.0, LossOfTailRotorEffectiveness.bestWeightLbs(spec), 1e-9);
    }

    /**
     * Verifies {@code normalize} returns empty when a database spec is missing the required tail-rotor blade chord.
     */
    @Test
    @DisplayName("Should reject database specs missing the required blade chord")
    public void normalizeRejectsDatabaseSpecsMissingRequiredBladeChord() {
        var spec = new LossOfTailRotorEffectiveness.HelicopterSpec(
                "SA319", 4_960.0, Double.NaN, 2_474.0, 3, 434.4, Double.NaN, 420.0);

        assertTrue(LossOfTailRotorEffectiveness.normalize(spec, 90.0, 1.225, 20.0, 10.0, Double.NaN)
                .isEmpty());
    }
}
