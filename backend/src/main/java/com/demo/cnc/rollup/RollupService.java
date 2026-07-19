package com.demo.cnc.rollup;

import com.demo.cnc.model.Machine;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Rollup orchestration: resolves the target machines, computes the KST window,
 * and delegates the aggregation to {@link RollupRepository}.
 */
@Service
public class RollupService {

    public static final int DEFAULT_LOOKBACK_DAYS = 2;
    private static final ZoneId PROJECT_ZONE = ZoneId.of("Asia/Seoul");
    private static final Logger logger = LoggerFactory.getLogger(RollupService.class);

    private final RollupRepository repository;
    private final MongoTemplate mongoTemplate;

    public RollupService(RollupRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    public void rollupRecentDays() {
        rollupRecentDays(DEFAULT_LOOKBACK_DAYS);
    }

    public void rollupRecentDays(int lookbackDays) {
        logger.info("[Rollup] mongoDb={} | lookbackDays={}", mongoTemplate.getDb().getName(), lookbackDays);
        rollupRecentDays(lookbackDays, resolveMachineIds());
    }

    public void rollupRecentDays(int lookbackDays, List<String> machineIds) {
        int windowDays = Math.max(1, lookbackDays);
        ZonedDateTime executedAtKst = ZonedDateTime.now(PROJECT_ZONE);
        LocalDate endDateExclusive = executedAtKst.toLocalDate().plusDays(1);
        LocalDate startDate = endDateExclusive.minusDays(windowDays);

        logger.info("[Rollup] Scheduled window executedAtKst={}, lookbackDays={}, startDate={}, endDateExclusive={}",
                executedAtKst, windowDays, startDate, endDateExclusive);

        rollupRange(startDate, endDateExclusive, machineIds);
    }

    /**
     * Rolls up an explicit KST date range (end exclusive). Used by both the
     * scheduler and the CLI backfill runner.
     */
    public void rollupRange(LocalDate startDate, LocalDate endDateExclusive, List<String> machineIds) {
        if (startDate == null || endDateExclusive == null || !startDate.isBefore(endDateExclusive)) {
            logger.warn("[Rollup] Skipping rollup: invalid range startDate={}, endDateExclusive={}",
                    startDate, endDateExclusive);
            return;
        }

        List<String> resolved = CollectionUtils.isEmpty(machineIds) ? resolveMachineIds() : machineIds;
        if (CollectionUtils.isEmpty(resolved)) {
            logger.warn("[Rollup] Skipping rollup: no machines found for range {} to {}", startDate, endDateExclusive);
            return;
        }

        logger.info("[Rollup] Rolling up range {} to {} (end exclusive), machineCount={}, machines={}",
                startDate, endDateExclusive, resolved.size(), resolved);

        repository.rollupRange(startDate, endDateExclusive, resolved);
    }

    private List<String> resolveMachineIds() {
        List<Machine> machines = mongoTemplate.find(
                new Query(Criteria.where("enabled").is(true)), Machine.class);
        return machines.stream()
                .map(Machine::getMachineId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }
}
