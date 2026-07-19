package com.demo.cnc.rollup;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures the indexes the rollup engine relies on exist at startup.
 *
 * <ul>
 *   <li>Source pool: {@code (machineId, signalName, endDate)} to make the
 *       per-signal range scan efficient.</li>
 *   <li>Target buckets: a unique {@code (machineId, baseDate, hour)} index that
 *       both speeds up the upsert lookup and guards against duplicate buckets.</li>
 * </ul>
 */
@Component
public class RollupIndexInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(RollupIndexInitializer.class);

    private static final String IDX_SOURCE = "idx_signal_machineId_signalName_endDate";
    private static final String IDX_TARGET_UNIQUE = "ux_rollup_machineId_baseDate_hour";

    private static final Document KEY_SOURCE = new Document("machineId", 1)
            .append("signalName", 1)
            .append("endDate", 1);
    private static final Document KEY_TARGET_UNIQUE = new Document("machineId", 1)
            .append("baseDate", 1)
            .append("hour", 1);

    private final MongoTemplate mongoTemplate;

    public RollupIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureIndex(RollupRepository.SOURCE_COLLECTION, IDX_SOURCE, KEY_SOURCE, false);
        ensureIndex(RollupRepository.RUNTIME_COLLECTION, IDX_TARGET_UNIQUE, KEY_TARGET_UNIQUE, true);
        ensureIndex(RollupRepository.CUTTIME_COLLECTION, IDX_TARGET_UNIQUE, KEY_TARGET_UNIQUE, true);
    }

    private void ensureIndex(String collectionName, String name, Document key, boolean unique) {
        MongoCollection<Document> collection = mongoTemplate.getCollection(collectionName);

        Map<String, Document> existingByName = new LinkedHashMap<>();
        for (Document doc : collection.listIndexes()) {
            String existingName = doc.getString("name");
            if (existingName != null) {
                existingByName.put(existingName, doc);
            }
        }

        Document existing = existingByName.get(name);
        if (existing != null) {
            boolean keyMatches = key.equals(existing.get("key", Document.class));
            boolean uniqueMatches = unique == Boolean.TRUE.equals(existing.getBoolean("unique"));
            if (keyMatches && uniqueMatches) {
                return;
            }
            logger.warn("[Rollup] Replacing index {} on {} due to key/unique mismatch.", name, collectionName);
            collection.dropIndex(name);
        }

        IndexOptions options = new IndexOptions().name(name).background(true).unique(unique);
        collection.createIndex(key, options);
        logger.info("[Rollup] Ensured index {} on {}", name, collectionName);
    }
}
