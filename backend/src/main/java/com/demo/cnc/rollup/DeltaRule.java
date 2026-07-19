package com.demo.cnc.rollup;

/**
 * Converts two consecutive cumulative counter readings into a bounded time delta.
 *
 * <p>Raw shop-floor counters ({@code RunTime}/{@code CutTime}) are monotonically
 * increasing, but real data contains counter resets, out-of-order points, long
 * idle gaps and spurious jumps. Feeding those straight into a sum would inflate
 * the dashboard. This rule keeps the aggregation honest:
 *
 * <ol>
 *   <li>{@code rawDelta <= 0} &rarr; 0 (counter reset / non-increasing)</li>
 *   <li>{@code gapSec <= 0} &rarr; 0 (bad ordering / duplicate timestamp)</li>
 *   <li>{@code gapSec > MAX_GAP_SEC} &rarr; 0 (long gap treated as downtime)</li>
 *   <li>otherwise cap the step to {@code min(gapSec, MAX_STEP_SEC)} seconds</li>
 * </ol>
 *
 * <p>Pure and side-effect free so the correction logic can be unit tested without
 * a database.
 */
public final class DeltaRule {

    /** A single reading may contribute at most this many seconds of run/cut time. */
    public static final double MAX_STEP_SEC = 120.0d;

    /** Gaps longer than this between readings are dropped (treated as downtime). */
    public static final double MAX_GAP_SEC = 600.0d;

    private DeltaRule() {
    }

    /**
     * @param prevValue   previous cumulative counter value
     * @param prevEpochMs previous reading timestamp (epoch millis)
     * @param curValue    current cumulative counter value
     * @param curEpochMs  current reading timestamp (epoch millis)
     * @return the corrected, non-negative delta in seconds
     */
    public static double resolve(double prevValue, long prevEpochMs, double curValue, long curEpochMs) {
        double rawDelta = curValue - prevValue;
        if (rawDelta <= 0.0d) {
            return 0.0d;
        }
        double gapSec = (curEpochMs - prevEpochMs) / 1000.0d;
        if (gapSec <= 0.0d) {
            return 0.0d;
        }
        if (gapSec > MAX_GAP_SEC) {
            return 0.0d;
        }
        double maxAllowedStep = Math.min(gapSec, MAX_STEP_SEC);
        return Math.min(rawDelta, maxAllowedStep);
    }
}
