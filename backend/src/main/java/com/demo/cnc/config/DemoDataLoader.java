package com.demo.cnc.config;

import com.demo.cnc.model.AlarmEvent;
import com.demo.cnc.model.DailySummary;
import com.demo.cnc.model.Machine;
import com.demo.cnc.model.MachineSignal;
import com.demo.cnc.model.MachineStatusEvent;
import com.demo.cnc.model.RuntimeCuttimeEvent;
import com.demo.cnc.repository.AlarmEventRepository;
import com.demo.cnc.repository.DailySummaryRepository;
import com.demo.cnc.repository.MachineRepository;
import com.demo.cnc.repository.MachineSignalRepository;
import com.demo.cnc.repository.MachineStatusEventRepository;
import com.demo.cnc.repository.RuntimeCuttimeEventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoDataLoader implements CommandLineRunner {

    private final ObjectMapper objectMapper;
    private final MachineRepository machineRepository;
    private final MachineStatusEventRepository statusEventRepository;
    private final RuntimeCuttimeEventRepository runtimeCuttimeEventRepository;
    private final AlarmEventRepository alarmEventRepository;
    private final DailySummaryRepository dailySummaryRepository;
    private final MachineSignalRepository machineSignalRepository;

    public DemoDataLoader(
            ObjectMapper objectMapper,
            MachineRepository machineRepository,
            MachineStatusEventRepository statusEventRepository,
            RuntimeCuttimeEventRepository runtimeCuttimeEventRepository,
            AlarmEventRepository alarmEventRepository,
            DailySummaryRepository dailySummaryRepository,
            MachineSignalRepository machineSignalRepository
    ) {
        this.objectMapper = objectMapper;
        this.machineRepository = machineRepository;
        this.statusEventRepository = statusEventRepository;
        this.runtimeCuttimeEventRepository = runtimeCuttimeEventRepository;
        this.alarmEventRepository = alarmEventRepository;
        this.dailySummaryRepository = dailySummaryRepository;
        this.machineSignalRepository = machineSignalRepository;
    }

    @Override
    public void run(String... args) throws IOException {
        Path sampleDataPath = findSampleDataPath();

        // Demo-only seed loader: imports synthetic sample-data JSON when Mongo collections are empty.
        if (machineRepository.count() == 0) {
            machineRepository.saveAll(read(sampleDataPath.resolve("machines.json"), new TypeReference<List<Machine>>() {}));
        }
        if (statusEventRepository.count() == 0) {
            statusEventRepository.saveAll(read(sampleDataPath.resolve("status-history.json"), new TypeReference<List<MachineStatusEvent>>() {}));
        }
        if (runtimeCuttimeEventRepository.count() == 0) {
            runtimeCuttimeEventRepository.saveAll(read(sampleDataPath.resolve("runtime-cuttime.json"), new TypeReference<List<RuntimeCuttimeEvent>>() {}));
        }
        if (alarmEventRepository.count() == 0) {
            alarmEventRepository.saveAll(read(sampleDataPath.resolve("alarm-history.json"), new TypeReference<List<AlarmEvent>>() {}));
        }
        if (dailySummaryRepository.count() == 0) {
            dailySummaryRepository.saveAll(read(sampleDataPath.resolve("daily-summary.json"), new TypeReference<List<DailySummary>>() {}));
        }
        if (machineSignalRepository.count() == 0) {
            machineSignalRepository.saveAll(read(sampleDataPath.resolve("machine-signal-pool.json"), new TypeReference<List<MachineSignal>>() {}));
        }
    }

    private Path findSampleDataPath() {
        List<Path> candidates = List.of(Path.of("sample-data"), Path.of("..", "sample-data"));
        return candidates.stream()
                .filter(Files::isDirectory)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("sample-data directory was not found"));
    }

    private <T> List<T> read(Path path, TypeReference<List<T>> typeReference) throws IOException {
        return objectMapper.readValue(path.toFile(), typeReference);
    }
}
