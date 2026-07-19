package com.demo.cnc.repository;

import com.demo.cnc.model.RuntimeDaily;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RuntimeDailyRepository extends MongoRepository<RuntimeDaily, String> {
}
