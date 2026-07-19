package com.demo.cnc.service;

import com.demo.cnc.dto.MachineHistoryPageDto;
import com.demo.cnc.exception.BadRequestException;
import com.demo.cnc.model.MachineStatusEvent;
import com.demo.cnc.repository.MachineStatusEventRepository;
import com.demo.cnc.service.DateRangeParser.DateRange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Paged status-event history for a single machine — the machine history
 * timeline view. Events are returned newest first.
 */
@Service
public class MachineHistoryService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final MachineStatusEventRepository statusEventRepository;

    public MachineHistoryService(MachineStatusEventRepository statusEventRepository) {
        this.statusEventRepository = statusEventRepository;
    }

    public MachineHistoryPageDto getHistory(String machineId, String from, String to, Integer page, Integer size) {
        if (machineId == null || machineId.isBlank()) {
            throw new BadRequestException("machineId is required");
        }
        DateRange range = DateRangeParser.parse(from, to);
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        Page<MachineStatusEvent> result = statusEventRepository.findByMachineIdAndWorkDateBetween(
                machineId.trim(),
                range.from(),
                range.to(),
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "startedAt"))
        );

        return new MachineHistoryPageDto(
                result.getContent().stream().map(this::toItem).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private MachineHistoryPageDto.Item toItem(MachineStatusEvent event) {
        return new MachineHistoryPageDto.Item(
                event.getEventId(),
                event.getMachineId(),
                event.getStatus(),
                event.getStartedAt(),
                event.getEndedAt(),
                event.getDurationSeconds(),
                event.getWorkDate()
        );
    }
}
