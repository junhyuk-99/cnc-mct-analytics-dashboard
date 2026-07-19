package com.demo.cnc.rollup;

import com.demo.cnc.dto.DailyRollupDto;
import com.demo.cnc.dto.HourlyRollupDto;
import com.demo.cnc.dto.MonthlyRollupDto;
import com.demo.cnc.exception.BadRequestException;
import com.demo.cnc.model.CuttimeDaily;
import com.demo.cnc.model.RuntimeDaily;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.ToIntFunction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Read side of the rollup engine: serves hourly buckets and their day/month
 * roll-forward aggregates straight from the pre-aggregated summary collections,
 * so dashboard charts never scan the raw signal pool.
 */
@Service
public class RollupQueryService {

    private final MongoTemplate mongoTemplate;

    public RollupQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Hourly runtime/cutting buckets for a single KST day, ordered by machine
     * then hour.
     *
     * @param date ISO date {@code yyyy-MM-dd}
     */
    public List<HourlyRollupDto> getHourly(String date) {
        LocalDate day = parseDate(date);
        Query query = new Query(Criteria.where("year").is(day.getYear())
                .and("month").is(day.getMonthValue())
                .and("day").is(day.getDayOfMonth()));
        return aggregate(query, RuntimeDaily::getHour, CuttimeDaily::getHour).entrySet().stream()
                .map(entry -> new HourlyRollupDto(
                        entry.getKey().machineId(), entry.getKey().bucket(),
                        entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    /**
     * Per-day runtime/cutting totals for one month, aggregated up from the
     * hourly buckets. Ordered by machine then day.
     */
    public List<DailyRollupDto> getDaily(String year, String month) {
        int safeYear = parsePositiveInt(year, "year");
        int safeMonth = parsePositiveInt(month, "month");
        if (safeMonth < 1 || safeMonth > 12) {
            throw new BadRequestException("month must be between 1 and 12");
        }
        Query query = new Query(Criteria.where("year").is(safeYear).and("month").is(safeMonth));
        return aggregate(query, RuntimeDaily::getDay, CuttimeDaily::getDay).entrySet().stream()
                .map(entry -> new DailyRollupDto(
                        entry.getKey().machineId(), entry.getKey().bucket(),
                        entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    /**
     * Per-month runtime/cutting totals for one year, aggregated up from the
     * hourly buckets. Ordered by machine then month.
     */
    public List<MonthlyRollupDto> getMonthly(String year) {
        int safeYear = parsePositiveInt(year, "year");
        Query query = new Query(Criteria.where("year").is(safeYear));
        return aggregate(query, RuntimeDaily::getMonth, CuttimeDaily::getMonth).entrySet().stream()
                .map(entry -> new MonthlyRollupDto(
                        entry.getKey().machineId(), entry.getKey().bucket(),
                        entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    /**
     * Merges runtime and cutting buckets matched by {@code query} into
     * {@code (machineId, bucket) -> [runSeconds, cutSeconds]}, where the bucket
     * index (hour, day or month) is chosen by the extractors.
     */
    private SortedMap<BucketKey, double[]> aggregate(
            Query query,
            ToIntFunction<RuntimeDaily> runBucket,
            ToIntFunction<CuttimeDaily> cutBucket
    ) {
        SortedMap<BucketKey, double[]> merged = new TreeMap<>();
        for (RuntimeDaily row : mongoTemplate.find(query, RuntimeDaily.class)) {
            merged.computeIfAbsent(new BucketKey(row.getMachineId(), runBucket.applyAsInt(row)),
                    key -> new double[2])[0] += row.getRunTime();
        }
        for (CuttimeDaily row : mongoTemplate.find(query, CuttimeDaily.class)) {
            merged.computeIfAbsent(new BucketKey(row.getMachineId(), cutBucket.applyAsInt(row)),
                    key -> new double[2])[1] += row.getCutTime();
        }
        return merged;
    }

    private LocalDate parseDate(String date) {
        if (!StringUtils.hasText(date)) {
            throw new BadRequestException("date is required (yyyy-MM-dd)");
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("date must be an ISO date (yyyy-MM-dd)");
        }
    }

    private int parsePositiveInt(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(name + " is required");
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new BadRequestException(name + " must be positive");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new BadRequestException(name + " must be a number");
        }
    }

    private record BucketKey(String machineId, int bucket) implements Comparable<BucketKey> {

        @Override
        public int compareTo(BucketKey other) {
            int byMachine = machineId.compareTo(other.machineId);
            return byMachine != 0 ? byMachine : Integer.compare(bucket, other.bucket);
        }
    }
}
