package com.demo.cnc.dto;

/**
 * One hourly rollup bucket for a machine, read back from the pre-aggregated
 * {@code runtime_daily} / {@code cuttime_daily} summary collections.
 */
public record HourlyRollupDto(
        String machineId,
        int hour,
        double runTimeSeconds,
        double cutTimeSeconds
) {
}
