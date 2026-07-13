package org.ngafid.core.flights;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VortexRingStateTest {

    @Test
    void bestWeightUsesTwentyFivePercentCargoCapacity() {
        var spec = new VortexRingState.HelicopterSpec("TEST", 5_000.0, 2_000.0, 1_500.0, 400.0);
        assertEquals(2_750.0, VortexRingState.bestWeightLbs(spec), 1e-9);
    }

    @Test
    void bestWeightFallsBackToEmptyWeightWhenMinimumFlyingWeightMissing() {
        var spec = new VortexRingState.HelicopterSpec("TEST", 5_000.0, Double.NaN, 1_500.0, 400.0);
        assertEquals(2_375.0, VortexRingState.bestWeightLbs(spec), 1e-9);
    }

    @Test
    void normalizesTrueAirspeedAndVerticalSpeedByHoverInducedVelocity() {
        var spec = new VortexRingState.HelicopterSpec("TEST", 5_000.0, 2_000.0, Double.NaN, 400.0);
        var normalized = VortexRingState.normalize(spec, 10.0, -900.0, 1.225, Double.NaN).orElseThrow();

        assertTrue(normalized.vhMps() > 0.0);
        assertTrue(normalized.vxOverVh() > 0.0);
        assertTrue(normalized.vzOverVh() < 0.0);
    }

    @Test
    void johnsonBoundaryClassifiesDocumentedEventRegion() {
        assertTrue(VortexRingState.isJohnsonEvent(0.0, -0.5));
        assertTrue(VortexRingState.isJohnsonEvent(0.5, -1.0));
        assertFalse(VortexRingState.isJohnsonEvent(0.96, -1.0));
        assertFalse(VortexRingState.isJohnsonEvent(0.5, -0.2));
    }

    @Test
    void defaultBoundaryLoadsRaisePolygonResource() {
        assertTrue(VortexRingState.defaultBoundary().pointCount() > 1_000);
    }

    @Test
    void classifierReturnsEventBeforeRisk() {
        var inputs = new VortexRingState.NormalizedInputs(10.0, 0.0, -0.5);
        assertEquals(VortexRingState.EVENT, VortexRingState.classify(inputs));
    }

    @Test
    void classifierReturnsRiskNearPolygonBoundary() {
        var inputs = new VortexRingState.NormalizedInputs(10.0, 1.05, -0.65);
        assertEquals(VortexRingState.RISK, VortexRingState.classify(inputs));
    }

    @Test
    void classifierReturnsNoRiskAwayFromBoundary() {
        var inputs = new VortexRingState.NormalizedInputs(10.0, 3.0, 0.0);
        assertEquals(VortexRingState.NO_RISK, VortexRingState.classify(inputs));
    }

    @Test
    void normalizeRejectsMissingRotorDiameter() {
        var spec = new VortexRingState.HelicopterSpec("TEST", 5_000.0, 2_000.0, Double.NaN, Double.NaN);
        assertTrue(VortexRingState.normalize(spec, 10.0, -900.0, 1.225, Double.NaN).isEmpty());
    }
}