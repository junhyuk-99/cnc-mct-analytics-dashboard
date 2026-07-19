package com.demo.cnc.service;

import com.demo.cnc.dto.PreAlarmIndicatorDto;
import com.demo.cnc.dto.PreAlarmSummaryDto;
import com.demo.cnc.model.AlarmEvent;
import com.demo.cnc.model.Machine;
import com.demo.cnc.model.RuntimeCuttimeEvent;
import com.demo.cnc.repository.AlarmEventRepository;
import com.demo.cnc.repository.MachineRepository;
import com.demo.cnc.repository.RuntimeCuttimeEventRepository;
import com.demo.cnc.service.DateRangeParser.DateRange;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Pre-alarm view: the aggregated alarm landscape plus computed early-warning
 * indicators. Instead of waiting for CRITICAL alarms, threshold rules over the
 * selected window surface machines that are trending toward trouble —
 * utilization decay, cutting-ratio decay, and rising alarm frequency.
 *
 * <p>Thresholds are configuration (prealarm.*), so the rules can be tuned
 * without code changes.
 */
@Service
public class PreAlarmService {

    public static final String SEVERITY_WATCH = "WATCH";
    public static final String SEVERITY_WARNING = "WARNING";

    private static final int TOP_CODE_LIMIT = 5;

    private final AlarmEventRepository alarmEventRepository;
    private final RuntimeCuttimeEventRepository runtimeCuttimeEventRepository;
    private final MachineRepository machineRepository;

    @Value("${prealarm.utilization-watch:70.0}")
    private double utilizationWatch;

    @Value("${prealarm.utilization-warning:55.0}")
    private double utilizationWarning;

    @Value("${prealarm.cutting-ratio-watch:70.0}")
    private double cuttingRatioWatch;

    @Value("${prealarm.cutting-ratio-warning:60.0}")
    private double cuttingRatioWarning;

    @Value("${prealarm.alarm-count-watch:15}")
    private long alarmCountWatch;

    @Value("${prealarm.critical-count-warning:6}")
    private long criticalCountWarning;

    public PreAlarmService(
            AlarmEventRepository alarmEventRepository,
            RuntimeCuttimeEventRepository runtimeCuttimeEventRepository,
            MachineRepository machineRepository
    ) {
        this.alarmEventRepository = alarmEventRepository;
        this.runtimeCuttimeEventRepository = runtimeCuttimeEventRepository;
        this.machineRepository = machineRepository;
    }

    public PreAlarmSummaryDto getSummary(String from, String to) {
        DateRange range = DateRangeParser.parse(from, to);
        List<AlarmEvent> alarms = alarmEventRepository.findByWorkDateBetweenOrderByOccurredAtDesc(
                range.from(), range.to());

        long info = 0;
        long warning = 0;
        long critical = 0;
        Map<String, long[]> byCode = new LinkedHashMap<>();
        Map<String, String> codeSeverity = new LinkedHashMap<>();
        Map<String, long[]> byMachine = new LinkedHashMap<>();

        for (AlarmEvent alarm : alarms) {
            switch (alarm.getSeverity()) {
                case "CRITICAL" -> critical++;
                case "WARNING" -> warning++;
                default -> info++;
            }
            byCode.computeIfAbsent(alarm.getAlarmCode(), key -> new long[1])[0]++;
            codeSeverity.putIfAbsent(alarm.getAlarmCode(), alarm.getSeverity());
            long[] machineCounts = byMachine.computeIfAbsent(alarm.getMachineId(), key -> new long[2]);
            machineCounts[0]++;
            if ("CRITICAL".equals(alarm.getSeverity())) {
                machineCounts[1]++;
            }
        }

        List<PreAlarmSummaryDto.CodeCount> topCodes = byCode.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<String, long[]> entry) -> entry.getValue()[0]).reversed())
                .limit(TOP_CODE_LIMIT)
                .map(entry -> new PreAlarmSummaryDto.CodeCount(
                        entry.getKey(), codeSeverity.get(entry.getKey()), entry.getValue()[0]))
                .toList();

        List<PreAlarmSummaryDto.MachineCount> machineCounts = byMachine.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<String, long[]> entry) -> entry.getValue()[0]).reversed())
                .map(entry -> new PreAlarmSummaryDto.MachineCount(
                        entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();

        return new PreAlarmSummaryDto(alarms.size(), info, warning, critical, topCodes, machineCounts);
    }

    public List<PreAlarmIndicatorDto> getIndicators(String from, String to) {
        DateRange range = DateRangeParser.parse(from, to);
        List<Machine> machines = machineRepository.findByEnabledTrueOrderByMachineTypeAscMachineIdAsc();
        List<RuntimeCuttimeEvent> metrics = runtimeCuttimeEventRepository.findByWorkDateBetween(
                range.from(), range.to());
        List<AlarmEvent> alarms = alarmEventRepository.findByWorkDateBetweenOrderByOccurredAtDesc(
                range.from(), range.to());

        Map<String, MetricAccumulator> byMachine = new LinkedHashMap<>();
        for (RuntimeCuttimeEvent metric : metrics) {
            MetricAccumulator acc = byMachine.computeIfAbsent(metric.getMachineId(), key -> new MetricAccumulator());
            acc.runtimeSeconds += metric.getRuntimeSeconds();
            acc.cuttimeSeconds += metric.getCuttimeSeconds();
            acc.workDates.add(metric.getWorkDate().toString());
        }
        for (AlarmEvent alarm : alarms) {
            MetricAccumulator acc = byMachine.computeIfAbsent(alarm.getMachineId(), key -> new MetricAccumulator());
            acc.alarmCount++;
            if ("CRITICAL".equals(alarm.getSeverity())) {
                acc.criticalCount++;
            }
        }

        List<PreAlarmIndicatorDto> indicators = new ArrayList<>();
        for (Machine machine : machines) {
            MetricAccumulator acc = byMachine.get(machine.getMachineId());
            if (acc == null) {
                continue;
            }

            long plannedSeconds = machine.getPlannedDailySeconds() * Math.max(1, acc.workDates.size());
            double utilization = plannedSeconds > 0 ? acc.runtimeSeconds * 100.0 / plannedSeconds : 0.0;
            addThresholdIndicator(indicators, machine.getMachineId(), "LOW_UTILIZATION", utilization,
                    utilizationWarning, utilizationWatch,
                    "Average utilization %.1f%% is below the %s threshold %.1f%%");

            double cuttingRatio = acc.runtimeSeconds > 0 ? acc.cuttimeSeconds * 100.0 / acc.runtimeSeconds : 0.0;
            addThresholdIndicator(indicators, machine.getMachineId(), "LOW_CUTTING_RATIO", cuttingRatio,
                    cuttingRatioWarning, cuttingRatioWatch,
                    "Average cutting ratio %.1f%% is below the %s threshold %.1f%%");

            if (acc.criticalCount >= criticalCountWarning) {
                indicators.add(new PreAlarmIndicatorDto(machine.getMachineId(), "CRITICAL_ALARM_FREQUENCY",
                        acc.criticalCount, criticalCountWarning, SEVERITY_WARNING,
                        String.format("%d critical alarms in the window reached the warning threshold %d",
                                acc.criticalCount, criticalCountWarning)));
            } else if (acc.alarmCount >= alarmCountWatch) {
                indicators.add(new PreAlarmIndicatorDto(machine.getMachineId(), "ALARM_FREQUENCY",
                        acc.alarmCount, alarmCountWatch, SEVERITY_WATCH,
                        String.format("%d alarms in the window reached the watch threshold %d",
                                acc.alarmCount, alarmCountWatch)));
            }
        }

        indicators.sort(Comparator
                .comparing((PreAlarmIndicatorDto indicator) -> SEVERITY_WARNING.equals(indicator.severity()) ? 0 : 1)
                .thenComparing(PreAlarmIndicatorDto::machineId));
        return indicators;
    }

    /** Emits a WARNING or WATCH indicator when {@code value} is below a "lower is worse" threshold pair. */
    private void addThresholdIndicator(
            List<PreAlarmIndicatorDto> indicators,
            String machineId,
            String rule,
            double value,
            double warningThreshold,
            double watchThreshold,
            String messageTemplate
    ) {
        if (value < warningThreshold) {
            indicators.add(new PreAlarmIndicatorDto(machineId, rule, round1(value), warningThreshold,
                    SEVERITY_WARNING, String.format(messageTemplate, value, "warning", warningThreshold)));
        } else if (value < watchThreshold) {
            indicators.add(new PreAlarmIndicatorDto(machineId, rule, round1(value), watchThreshold,
                    SEVERITY_WATCH, String.format(messageTemplate, value, "watch", watchThreshold)));
        }
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static final class MetricAccumulator {
        private long runtimeSeconds;
        private long cuttimeSeconds;
        private long alarmCount;
        private long criticalCount;
        private final Set<String> workDates = new HashSet<>();
    }
}
