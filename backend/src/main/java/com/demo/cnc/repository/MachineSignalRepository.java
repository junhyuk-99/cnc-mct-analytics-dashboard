package com.demo.cnc.repository;

import com.demo.cnc.model.MachineSignal;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MachineSignalRepository extends MongoRepository<MachineSignal, String> {
}
