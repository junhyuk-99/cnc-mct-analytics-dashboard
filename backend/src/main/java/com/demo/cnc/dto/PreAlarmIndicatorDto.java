package com.demo.cnc.dto;

/**
 * One computed early-warning indicator: a machine metric that crossed a
 * configured pre-alarm threshold over the selected window.
 */
public record PreAlarmIndicatorDto(
        String machineId,
        String rule,
        double metricValue,
        double threshold,
        String severity,
        String message
) {
}
