package com.demo.cnc.controller;

import com.demo.cnc.dto.ApiResponse;
import com.demo.cnc.dto.MachineDto;
import com.demo.cnc.dto.MachineHistoryPageDto;
import com.demo.cnc.service.MachineHistoryService;
import com.demo.cnc.service.MachineService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/machines")
public class MachineController {

    private final MachineService machineService;
    private final MachineHistoryService machineHistoryService;

    public MachineController(MachineService machineService, MachineHistoryService machineHistoryService) {
        this.machineService = machineService;
        this.machineHistoryService = machineHistoryService;
    }

    @GetMapping
    public ApiResponse<List<MachineDto>> getMachines() {
        return ApiResponse.ok(machineService.getEnabledMachines());
    }

    /** Paged status-event timeline for one machine, newest first. */
    @GetMapping("/history")
    public ApiResponse<MachineHistoryPageDto> getHistory(
            @RequestParam String machineId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.ok(machineHistoryService.getHistory(machineId, from, to, page, size));
    }
}
