package com.sqlforge.backend.repository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryWorkflowBaselineRepository {

    private final Map<String, Map<String, Object>> baselines = new ConcurrentHashMap<String, Map<String, Object>>();

    public Map<String, Object> get(String fingerprint) {
        return baselines.get(fingerprint);
    }

    public void save(String fingerprint, String tenantId, String decision, Map<String, Object> summary) {
        Map<String, Object> record = new LinkedHashMap<String, Object>();
        record.put("fingerprint", fingerprint);
        record.put("tenantId", tenantId);
        record.put("decision", decision);
        record.put("summary", summary);
        record.put("updatedAt", Instant.now().toString());
        baselines.put(fingerprint, record);
    }
}
