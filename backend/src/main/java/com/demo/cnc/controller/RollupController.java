package com.demo.cnc.controller;

import com.demo.cnc.dto.ApiResponse;
import com.demo.cnc.dto.DailyRollupDto;
import com.demo.cnc.dto.HourlyRollupDto;
import com.demo.cnc.dto.MonthlyRollupDto;
import com.demo.cnc.rollup.RollupQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read API over the rollup summary collections.
 */
@RestController
@RequestMapping("/api/rollup")
public class RollupController {

    private final RollupQueryService rollupQueryService;

    public RollupController(RollupQueryService rollupQueryService) {
        this.rollupQueryService = rollupQueryService;
    }

    /**
     * Hourly runtime/cutting buckets for a single day.
     *
     * @param date ISO date {@code yyyy-MM-dd}
     */
    @GetMapping("/hourly")
    public ApiResponse<List<HourlyRollupDto>> getHourly(@RequestParam String date) {
        return ApiResponse.ok(rollupQueryService.getHourly(date));
    }

    /** Per-day totals for one month, rolled forward from the hourly buckets. */
    @GetMapping("/daily")
    public ApiResponse<List<DailyRollupDto>> getDaily(
            @RequestParam String year,
            @RequestParam String month
    ) {
        return ApiResponse.ok(rollupQueryService.getDaily(year, month));
    }

    /** Per-month totals for one year, rolled forward from the hourly buckets. */
    @GetMapping("/monthly")
    public ApiResponse<List<MonthlyRollupDto>> getMonthly(@RequestParam String year) {
        return ApiResponse.ok(rollupQueryService.getMonthly(year));
    }
}
