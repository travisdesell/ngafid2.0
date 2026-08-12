package org.ngafid.core.flights;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LossOfTailRotorEffectivenessTest {

    @Test
    void classifierReturnsNoRiskForPositivePedalMargin() {
        assertEquals(LossOfTailRotorEffectiveness.NO_RISK, LossOfTailRotorEffectiveness.classify(1.0, 0.0));
    }

    @Test
    void classifierReturnsRiskForNegativePedalMarginWithoutHighYaw() {
        assertEquals(LossOfTailRotorEffectiveness.RISK, LossOfTailRotorEffectiveness.classify(-0.1, 20.0));
    }

    @Test
    void classifierReturnsEventBeforeRiskAtYawThreshold() {
        assertEquals(LossOfTailRotorEffectiveness.EVENT, LossOfTailRotorEffectiveness.classify(-0.1, 60.0));
    }

    @Test
    void classifierReturnsEventAboveYawThreshold() {
        assertEquals(LossOfTailRotorEffectiveness.EVENT, LossOfTailRotorEffectiveness.classify(-0.1, 80.0));
    }

    @Test
    void classifierReturnsUnsupportedForMissingValues() {
        assertEquals(LossOfTailRotorEffectiveness.UNSUPPORTED, LossOfTailRotorEffectiveness.classify(Double.NaN, 80.0));
        assertEquals(LossOfTailRotorEffectiveness.UNSUPPORTED, LossOfTailRotorEffectiveness.classify(-0.1, Double.NaN));
    }

    @Test
    void modelEnvelopeIncludesDocumentedBoundaries() {
        assertFalse(LossOfTailRotorEffectiveness.isInsideModelEnvelope(0.049999, 0.05));
        assertTrue(LossOfTailRotorEffectiveness.isInsideModelEnvelope(0.05, 0.0));
        assertTrue(LossOfTailRotorEffectiveness.isInsideModelEnvelope(0.1277, 0.1233));
        assertFalse(LossOfTailRotorEffectiveness.isInsideModelEnvelope(0.127701, 0.05));
        assertFalse(LossOfTailRotorEffectiveness.isInsideModelEnvelope(0.06, -0.000001));
        assertFalse(LossOfTailRotorEffectiveness.isInsideModelEnvelope(0.06, 0.123301));
    }

    @Test
    void relativeWindWrapsIntoZeroTo360Range() {
        assertEquals(0.0, LossOfTailRotorEffectiveness.relativeWindDirectionBodyFrameDeg(0.0, 0.0), 1e-9);
        assertEquals(90.0, LossOfTailRotorEffectiveness.relativeWindDirectionBodyFrameDeg(0.0, 90.0), 1e-9);
        assertEquals(270.0, LossOfTailRotorEffectiveness.relativeWindDirectionBodyFrameDeg(90.0, 0.0), 1e-9);
        assertEquals(20.0, LossOfTailRotorEffectiveness.relativeWindDirectionBodyFrameDeg(350.0, 10.0), 1e-9);
        assertEquals(340.0, LossOfTailRotorEffectiveness.relativeWindDirectionBodyFrameDeg(10.0, 350.0), 1e-9);
    }

    @Test
    void normalizesDbBackedHelicopterSpecInputs() {
        var spec = new LossOfTailRotorEffectiveness.HelicopterSpec(
                "TEST", 2_500.0, Double.NaN, 1_510.0, 4, 396.0, 10.0, 408.0);
        var normalized = LossOfTailRotorEffectiveness.normalize(spec, 90.0, 1.225, 20.0, 10.0, Double.NaN)
                .orElseThrow();
        assertEquals(90.0, normalized.relativeWindDeg(), 1e-9);
        assertTrue(normalized.mrctSigma() > 0.0);
        assertTrue(normalized.mu() > 0.0);
        assertEquals(10.0, normalized.yawRateDps(), 1e-9);
    }

    @Test
    void bestWeightPrefersMinFlyingWeightOverEmptyWeight() {
        var spec = new LossOfTailRotorEffectiveness.HelicopterSpec(
                "TEST", 5_000.0, 2_000.0, 1_500.0, 4, 400.0, 10.0, 400.0);

        assertEquals(4_250.0, LossOfTailRotorEffectiveness.bestWeightLbs(spec), 1e-9);
    }

    @Test
    void normalizeRejectsDatabaseSpecsMissingRequiredBladeChord() {
        var spec = new LossOfTailRotorEffectiveness.HelicopterSpec(
                "SA319", 4_960.0, Double.NaN, 2_474.0, 3, 434.4, Double.NaN, 420.0);

        assertTrue(LossOfTailRotorEffectiveness.normalize(spec, 90.0, 1.225, 20.0, 10.0, Double.NaN).isEmpty());
    }

}