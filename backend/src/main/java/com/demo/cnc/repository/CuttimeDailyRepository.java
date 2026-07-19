package com.demo.cnc.repository;

import com.demo.cnc.model.CuttimeDaily;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CuttimeDailyRepository extends MongoRepository<CuttimeDaily, String> {
}
