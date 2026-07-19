package com.demo.cnc.rollup;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * One-shot backfill runner for the rollup engine.
 *
 * <p>Enabled with {@code rollup.backfill.enabled=true}, it aggregates an explicit
 * date range once at startup and then does nothing on subsequent ticks. This is
 * the path used to (re)build the summary buckets for a historical window — for
 * the portfolio it lets you roll up the whole synthetic sample range in one run.
 *
 * <p>Example:
 * <pre>
 *   --rollup.backfill.enabled=true \
 *   --rollup.backfill.from=2026-01-01 \
 *   --rollup.backfill.to=2026-01-31 \
 *   --rollup.backfill.machineIds=CNC-DEMO-01,MCT-DEMO-01   # optional
 * </pre>
 * {@code to} is exclusive. Runs after {@link RollupIndexInitializer}.
 */
@Component
@Order(100)
@ConditionalOnProperty(name = "rollup.backfill.enabled", havingValue = "true")
public class RollupBackfillRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(RollupBackfillRunner.class);

    private final RollupService rollupService;

    @Value("${rollup.backfill.from:}")
    private String from;

    @Value("${rollup.backfill.to:}")
    private String to;

    @Value("${rollup.backfill.machineIds:}")
    private String machineIds;

    public RollupBackfillRunner(RollupService rollupService) {
        this.rollupService = rollupService;
    }

    @Override
    public void run(ApplicationArguments args) {
        logger.info("[Rollup] Backfill requested: from={}, to={}, machineIds={}", from, to, machineIds);

        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);
        if (fromDate == null || toDate == null) {
            logger.warn("[Rollup] Backfill skipped: from/to must be valid ISO dates (yyyy-MM-dd).");
            return;
        }
        if (!fromDate.isBefore(toDate)) {
            logger.warn("[Rollup] Backfill skipped: from ({}) must be before to ({}).", fromDate, toDate);
            return;
        }

        rollupService.rollupRange(fromDate, toDate, parseMachineIds(machineIds));
    }

    private LocalDate parseDate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            logger.warn("[Rollup] Backfill skipped: unparseable date '{}'.", raw);
            return null;
        }
    }

    private List<String> parseMachineIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }
}
