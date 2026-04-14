package com.sqlforge.backend.repository;

import java.util.List;
import java.util.Map;

public interface WorkflowBaselineRepository {

    Map<String, Object> get(String fingerprint);

    List<Map<String, Object>> findAll();

    void save(String fingerprint, String tenantId, String decision, Map<String, Object> summary);
}
