package com.demo.cnc.rollup;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Rollup aggregation body.
 *
 * <p>Converts the cumulative counter readings in {@code machine_signal_pool}
 * ({@code RunTime*}/{@code CutTime*}) into hourly deltas and upserts them into
 * {@code runtime_daily} / {@code cuttime_daily}.
 *
 * <p>Aggregation unit: {@code machineId + baseDate(KST 00:00) + hour(KST 0-23)}.
 *
 * <p>The counter-to-delta conversion applies explicit correction rules so that
 * counter resets, clock gaps and unrealistic jumps do not inflate the summary
 * (see {@link #resolveDelta}). This is a portfolio reconstruction of a
 * production rollup pipeline; all inputs are synthetic.
 */
@Repository
public class RollupRepository {

    public static final String SOURCE_COLLECTION = "machine_signal_pool";
    public static final String RUNTIME_COLLECTION = "runtime_daily";
    public static final String CUTTIME_COLLECTION = "cuttime_daily";

    private static final Pattern RUN_PREFIX = Pattern.compile("^RunTime", Pattern.CASE_INSENSITIVE);
    private static final Pattern CUT_PREFIX = Pattern.compile("^CutTime", Pattern.CASE_INSENSITIVE);

    private static final ZoneId PROJECT_ZONE = ZoneId.of("Asia/Seoul");

    private static final Logger logger = LoggerFactory.getLogger(RollupRepository.class);

    private final MongoTemplate mongoTemplate;

    public RollupRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Rolls up {@code [startDate, endDateExclusive)} (KST day boundaries) for the
     * given machines: clears existing target buckets in range, recomputes hourly
     * deltas from the source pool, and bulk-upserts them.
     */
    public void rollupRange(LocalDate startDate, LocalDate endDateExclusive, List<String> machineIds) {
        if (startDate == null || endDateExclusive == null
                || !startDate.isBefore(endDateExclusive) || CollectionUtils.isEmpty(machineIds)) {
            return;
        }

        Instant startUtc = startDate.atStartOfDay(PROJECT_ZONE).toInstant();
        Instant endUtc = endDateExclusive.atStartOfDay(PROJECT_ZONE).toInstant();
        Instant now = Instant.now();

        clearRollupRange(startDate, endDateExclusive, machineIds);

        List<SignalKey> signalKeys = findSignalKeys(startUtc, endUtc, machineIds);
        if (CollectionUtils.isEmpty(signalKeys)) {
            return;
        }

        Map<HourBucket, BucketValue> buckets = new LinkedHashMap<>();
        for (SignalKey signalKey : signalKeys) {
            List<SourcePoint> rowsInRange = findRowsInRange(signalKey, startUtc, endUtc);
            SourcePoint prev = null;
            if (!CollectionUtils.isEmpty(rowsInRange) || logger.isDebugEnabled()) {
                prev = findPrevSeed(signalKey, startUtc);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("[Rollup][SourceProbe] machineId={}, signalName={}, rowsInRange={}, prevSeed={}",
                        signalKey.machineId(), signalKey.signalName(), rowsInRange.size(), prev != null);
            }
            if (CollectionUtils.isEmpty(rowsInRange)) {
                continue;
            }

            for (SourcePoint cur : rowsInRange) {
                double delta = resolveDelta(prev, cur);
                if (delta > 0.0d) {
                    LocalDate baseDate = cur.endDate().atZone(PROJECT_ZONE).toLocalDate();
                    int hour = cur.endDate().atZone(PROJECT_ZONE).getHour();
                    HourBucket bucket = new HourBucket(signalKey.machineId(), baseDate, hour);
                    BucketValue value = buckets.computeIfAbsent(bucket, key -> new BucketValue());
                    if (signalKey.signalType() == SignalType.RUN) {
                        value.runTime += delta;
                    } else if (signalKey.signalType() == SignalType.CUT) {
                        value.cutTime += delta;
                    }
                }
                prev = cur;
            }
        }

        if (buckets.isEmpty()) {
            return;
        }

        BulkOperations runtimeOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, RUNTIME_COLLECTION);
        BulkOperations cutOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CUTTIME_COLLECTION);
        int runtimeCount = 0;
        int cutCount = 0;

        for (Map.Entry<HourBucket, BucketValue> entry : buckets.entrySet()) {
            HourBucket bucket = entry.getKey();
            BucketValue value = entry.getValue();
            if (bucket == null || value == null || !StringUtils.hasText(bucket.machineId()) || bucket.baseDate() == null) {
                continue;
            }

            int year = bucket.baseDate().getYear();
            int month = bucket.baseDate().getMonthValue();
            int day = bucket.baseDate().getDayOfMonth();
            int hour = bucket.hour();
            Date baseDate = Date.from(bucket.baseDate().atStartOfDay(PROJECT_ZONE).toInstant());

            if (value.runTime > 0.0d) {
                Update update = new Update()
                        .set("machineId", bucket.machineId())
                        .set("baseDate", baseDate)
                        .set("year", year)
                        .set("month", month)
                        .set("day", day)
                        .set("hour", hour)
                        .set("runTime", value.runTime)
                        .set("updatedAt", Date.from(now))
                        .setOnInsert("createdAt", Date.from(now));
                runtimeOps.upsert(buildUpsertQuery(bucket.machineId(), baseDate, hour), update);
                runtimeCount++;
            }

            if (value.cutTime > 0.0d) {
                Update update = new Update()
                        .set("machineId", bucket.machineId())
                        .set("baseDate", baseDate)
                        .set("year", year)
                        .set("month", month)
                        .set("day", day)
                        .set("hour", hour)
                        .set("cutTime", value.cutTime)
                        .set("updatedAt", Date.from(now))
                        .setOnInsert("createdAt", Date.from(now));
                cutOps.upsert(buildUpsertQuery(bucket.machineId(), baseDate, hour), update);
                cutCount++;
            }
        }

        if (runtimeCount > 0) {
            runtimeOps.execute();
        }
        if (cutCount > 0) {
            cutOps.execute();
        }
        logger.info("[Rollup] Upserted buckets runtime={}, cut={} for range {} to {}",
                runtimeCount, cutCount, startDate, endDateExclusive);
    }

    private Query buildUpsertQuery(String machineId, Date baseDate, int hour) {
        return new Query(Criteria.where("machineId").is(machineId)
                .and("baseDate").is(baseDate)
                .and("hour").is(hour));
    }

    private void clearRollupRange(LocalDate startDate, LocalDate endDateExclusive, List<String> machineIds) {
        Date startBaseDate = Date.from(startDate.atStartOfDay(PROJECT_ZONE).toInstant());
        Date endBaseDate = Date.from(endDateExclusive.atStartOfDay(PROJECT_ZONE).toInstant());
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("baseDate").gte(startBaseDate).lt(endBaseDate),
                Criteria.where("machineId").in(machineIds)
        ));
        mongoTemplate.remove(query, RUNTIME_COLLECTION);
        mongoTemplate.remove(query, CUTTIME_COLLECTION);
    }

    private List<SignalKey> findSignalKeys(Instant startUtc, Instant endUtc, List<String> machineIds) {
        Document match = new Document("$and", List.of(
                new Document("endDate", new Document("$gte", Date.from(startUtc)).append("$lt", Date.from(endUtc))),
                new Document("$or", List.of(
                        new Document("signalName", RUN_PREFIX),
                        new Document("signalName", CUT_PREFIX))),
                new Document("machineId", new Document("$in", machineIds))
        ));

        List<AggregationOperation> ops = new ArrayList<>();
        ops.add(raw(new Document("$match", match)));
        ops.add(raw(new Document("$group", new Document("_id", new Document()
                .append("machineId", "$machineId")
                .append("signalName", "$signalName")))));
        ops.add(raw(new Document("$project", new Document()
                .append("_id", 0)
                .append("machineId", "$_id.machineId")
                .append("signalName", "$_id.signalName"))));
        ops.add(raw(new Document("$sort", new Document("machineId", 1).append("signalName", 1))));

        List<Document> docs = mongoTemplate.aggregate(
                Aggregation.newAggregation(ops), SOURCE_COLLECTION, Document.class).getMappedResults();
        if (CollectionUtils.isEmpty(docs)) {
            return List.of();
        }

        List<SignalKey> signalKeys = new ArrayList<>();
        for (Document doc : docs) {
            String machineId = doc.getString("machineId");
            String signalName = doc.getString("signalName");
            SignalType signalType = resolveSignalType(signalName);
            if (StringUtils.hasText(machineId) && StringUtils.hasText(signalName) && signalType != null) {
                signalKeys.add(new SignalKey(machineId, signalName, signalType));
            }
        }
        return signalKeys;
    }

    private List<SourcePoint> findRowsInRange(SignalKey signalKey, Instant startUtc, Instant endUtc) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("machineId").is(signalKey.machineId()),
                Criteria.where("signalName").is(signalKey.signalName()),
                Criteria.where("endDate").gte(Date.from(startUtc)).lt(Date.from(endUtc))
        )).with(Sort.by(Sort.Direction.ASC, "endDate"));
        query.fields().include("endDate").include("value");

        List<Document> docs = mongoTemplate.find(query, Document.class, SOURCE_COLLECTION);
        List<SourcePoint> rows = new ArrayList<>(docs.size());
        for (Document doc : docs) {
            SourcePoint point = toSourcePoint(doc);
            if (point != null) {
                rows.add(point);
            }
        }
        return rows;
    }

    private SourcePoint findPrevSeed(SignalKey signalKey, Instant startUtc) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("machineId").is(signalKey.machineId()),
                Criteria.where("signalName").is(signalKey.signalName()),
                Criteria.where("endDate").lt(Date.from(startUtc))
        )).with(Sort.by(Sort.Direction.DESC, "endDate")).limit(1);
        query.fields().include("endDate").include("value");

        return toSourcePoint(mongoTemplate.findOne(query, Document.class, SOURCE_COLLECTION));
    }

    private SourcePoint toSourcePoint(Document doc) {
        if (doc == null) {
            return null;
        }
        Date endDate = doc.getDate("endDate");
        Double value = toDouble(doc.get("value"));
        if (endDate == null || value == null) {
            return null;
        }
        return new SourcePoint(endDate.toInstant(), value);
    }

    /** Delegates to {@link DeltaRule}; see that class for the correction rules. */
    private double resolveDelta(SourcePoint prev, SourcePoint cur) {
        if (prev == null || cur == null) {
            return 0.0d;
        }
        return DeltaRule.resolve(
                prev.value(), prev.endDate().toEpochMilli(),
                cur.value(), cur.endDate().toEpochMilli());
    }

    private SignalType resolveSignalType(String signalName) {
        if (!StringUtils.hasText(signalName)) {
            return null;
        }
        if (RUN_PREFIX.matcher(signalName).find()) {
            return SignalType.RUN;
        }
        if (CUT_PREFIX.matcher(signalName).find()) {
            return SignalType.CUT;
        }
        return null;
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof CharSequence sequence) {
            String text = sequence.toString().trim();
            if (text.isEmpty()) {
                return null;
            }
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private AggregationOperation raw(Document document) {
        return context -> document;
    }

    private enum SignalType {
        RUN,
        CUT
    }

    private record SignalKey(String machineId, String signalName, SignalType signalType) {
    }

    private record SourcePoint(Instant endDate, Double value) {
    }

    private record HourBucket(String machineId, LocalDate baseDate, int hour) {
    }

    private static final class BucketValue {
        private double runTime;
        private double cutTime;
    }
}
