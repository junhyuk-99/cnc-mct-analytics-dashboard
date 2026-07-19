package com.demo.cnc.dto;

import java.util.List;

/**
 * Aggregated alarm landscape for the pre-alarm view: totals by severity level,
 * the most frequent alarm codes, and per-machine counts.
 */
public record PreAlarmSummaryDto(
        long total,
        long infoCount,
        long warningCount,
        long criticalCount,
        List<CodeCount> topCodes,
        List<MachineCount> byMachine
) {

    public record CodeCount(String alarmCode, String severity, long count) {
    }

    public record MachineCount(String machineId, long count, long criticalCount) {
    }
}
