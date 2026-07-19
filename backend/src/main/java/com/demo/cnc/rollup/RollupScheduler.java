package com.demo.cnc.rollup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic trigger for the rollup engine.
 *
 * <p>The bean is only created when {@code rollup.enabled=true}, so the schedule
 * can be turned off entirely via configuration. On each cron tick it rolls up
 * the most recent {@code lookback-days} window.
 */
@Component
@ConditionalOnProperty(name = "rollup.enabled", havingValue = "true")
public class RollupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RollupScheduler.class);

    private final RollupService rollupService;

    @Value("${rollup.lookback-days:2}")
    private int lookbackDays;

    public RollupScheduler(RollupService rollupService) {
        this.rollupService = rollupService;
    }

    @Scheduled(cron = "${rollup.cron:0 */5 * * * *}", zone = "Asia/Seoul")
    public void rollupRecentWindow() {
        logger.info("[Rollup] Scheduler fired | lookbackDays={}", lookbackDays);
        rollupService.rollupRecentDays(lookbackDays);
    }
}
