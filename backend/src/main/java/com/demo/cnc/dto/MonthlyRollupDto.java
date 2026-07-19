package com.demo.cnc.dto;

/**
 * One month's rollup totals for a machine, aggregated up from the hourly
 * {@code runtime_daily} / {@code cuttime_daily} buckets.
 */
public record MonthlyRollupDto(
        String machineId,
        int month,
        double runTimeSeconds,
        double cutTimeSeconds
) {
}
