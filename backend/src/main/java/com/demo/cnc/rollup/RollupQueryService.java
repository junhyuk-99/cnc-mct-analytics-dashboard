package com.demo.cnc.rollup;

import com.demo.cnc.dto.HourlyRollupDto;
import com.demo.cnc.exception.BadRequestException;
import com.demo.cnc.model.CuttimeDaily;
import com.demo.cnc.model.RuntimeDaily;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Read side of the rollup engine: serves hourly buckets straight from the
 * pre-aggregated summary collections, so dashboard charts never scan the raw
 * signal pool.
 */
@Service
public class RollupQueryService {

    private final MongoTemplate mongoTemplate;

    public RollupQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Returns the hourly runtime/cutting buckets for a single KST day, ordered
     * by machine then hour.
     *
     * @param date ISO date {@code yyyy-MM-dd}
     */
    public List<HourlyRollupDto> getHourly(String date) {
        LocalDate day = parseDate(date);
        Query query = new Query(Criteria.where("year").is(day.getYear())
                .and("month").is(day.getMonthValue())
                .and("day").is(day.getDayOfMonth()));

        // Key: "machineId|hour" preserving encounter order.
        Map<String, double[]> merged = new LinkedHashMap<>();
        for (RuntimeDaily r : mongoTemplate.find(query, RuntimeDaily.class)) {
            merged.computeIfAbsent(key(r.getMachineId(), r.getHour()), k -> new double[2])[0] += r.getRunTime();
        }
        for (CuttimeDaily c : mongoTemplate.find(query, CuttimeDaily.class)) {
            merged.computeIfAbsent(key(c.getMachineId(), c.getHour()), k -> new double[2])[1] += c.getCutTime();
        }

        List<HourlyRollupDto> result = new ArrayList<>(merged.size());
        for (Map.Entry<String, double[]> entry : merged.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            result.add(new HourlyRollupDto(parts[0], Integer.parseInt(parts[1]),
                    entry.getValue()[0], entry.getValue()[1]));
        }
        result.sort((a, b) -> {
            int byMachine = a.machineId().compareTo(b.machineId());
            return byMachine != 0 ? byMachine : Integer.compare(a.hour(), b.hour());
        });
        return result;
    }

    private String key(String machineId, int hour) {
        return machineId + "|" + hour;
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
}
