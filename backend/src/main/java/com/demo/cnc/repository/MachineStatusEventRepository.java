package com.demo.cnc.repository;

import com.demo.cnc.model.MachineStatusEvent;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MachineStatusEventRepository extends MongoRepository<MachineStatusEvent, String> {
    List<MachineStatusEvent> findByWorkDateBetween(LocalDate from, LocalDate to);

    Page<MachineStatusEvent> findByMachineIdAndWorkDateBetween(
            String machineId,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );
}
