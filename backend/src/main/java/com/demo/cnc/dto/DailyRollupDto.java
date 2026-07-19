package com.demo.cnc.dto;

/**
 * One day's rollup totals for a machine within a month, aggregated up from the
 * hourly {@code runtime_daily} / {@code cuttime_daily} buckets.
 */
public record DailyRollupDto(
        String machineId,
        int day,
        double runTimeSeconds,
        double cutTimeSeconds
) {
}
