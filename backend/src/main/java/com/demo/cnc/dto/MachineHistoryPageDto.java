package com.demo.cnc.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * One page of a machine's status-event history, newest first.
 */
public record MachineHistoryPageDto(
        List<Item> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public record Item(
            String eventId,
            String machineId,
            String status,
            Instant startedAt,
            Instant endedAt,
            long durationSeconds,
            LocalDate workDate
    ) {
    }
}
