package com.demo.cnc.rollup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the counter-to-delta correction rules — the core of the rollup
 * engine — exercised without a database.
 */
class DeltaRuleTest {

    private static final long T0 = 1_735_700_000_000L; // arbitrary epoch millis
    private static final double EPS = 1e-9;

    @Test
    @DisplayName("normal 120s step within cap is counted in full")
    void normalStepCountedInFull() {
        double delta = DeltaRule.resolve(1000.0, T0, 1120.0, T0 + 120_000);
        assertEquals(120.0, delta, EPS);
    }

    @Test
    @DisplayName("counter reset (non-increasing value) yields zero")
    void counterResetDropped() {
        double delta = DeltaRule.resolve(5000.0, T0, 2000.0, T0 + 120_000);
        assertEquals(0.0, delta, EPS);
    }

    @Test
    @DisplayName("spurious jump is capped to the per-step maximum")
    void spuriousJumpCapped() {
        double delta = DeltaRule.resolve(1000.0, T0, 6120.0, T0 + 120_000);
        assertEquals(DeltaRule.MAX_STEP_SEC, delta, EPS);
    }

    @Test
    @DisplayName("gap longer than the max is treated as downtime and dropped")
    void longGapDropped() {
        long gapMs = (long) (DeltaRule.MAX_GAP_SEC + 60) * 1000L;
        double delta = DeltaRule.resolve(1000.0, T0, 1100.0, T0 + gapMs);
        assertEquals(0.0, delta, EPS);
    }

    @Test
    @DisplayName("non-positive gap (bad ordering / duplicate timestamp) yields zero")
    void nonPositiveGapDropped() {
        assertEquals(0.0, DeltaRule.resolve(1000.0, T0, 1100.0, T0), EPS);
        assertEquals(0.0, DeltaRule.resolve(1000.0, T0, 1100.0, T0 - 1000), EPS);
    }

    @Test
    @DisplayName("short gap caps the step to the gap length, not the 120s ceiling")
    void shortGapCapsToGapLength() {
        // 30s gap but counter advanced 90s worth: only 30s can be real.
        double delta = DeltaRule.resolve(1000.0, T0, 1090.0, T0 + 30_000);
        assertEquals(30.0, delta, EPS);
    }
}
