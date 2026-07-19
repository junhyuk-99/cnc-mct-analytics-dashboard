package com.demo.cnc.controller;

import com.demo.cnc.dto.ApiResponse;
import com.demo.cnc.dto.PreAlarmIndicatorDto;
import com.demo.cnc.dto.PreAlarmSummaryDto;
import com.demo.cnc.service.PreAlarmService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pre-alarm view: aggregated alarm landscape plus threshold-based
 * early-warning indicators.
 */
@RestController
@RequestMapping("/api/prealarm")
public class PreAlarmController {

    private final PreAlarmService preAlarmService;

    public PreAlarmController(PreAlarmService preAlarmService) {
        this.preAlarmService = preAlarmService;
    }

    @GetMapping("/summary")
    public ApiResponse<PreAlarmSummaryDto> getSummary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return ApiResponse.ok(preAlarmService.getSummary(from, to));
    }

    @GetMapping("/indicators")
    public ApiResponse<List<PreAlarmIndicatorDto>> getIndicators(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return ApiResponse.ok(preAlarmService.getIndicators(from, to));
    }
}
