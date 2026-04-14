package com.sqlforge.backend.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class FileWorkflowBaselineRepository implements WorkflowBaselineRepository {

    private final ObjectMapper objectMapper;
    private final String storageFilePath;
    private final Map<String, Map<String, Object>> baselines = new ConcurrentHashMap<String, Map<String, Object>>();

    public FileWorkflowBaselineRepository(
        ObjectMapper objectMapper,
        @Value("${sqlforge.storage.workflow-baselines-file:${java.io.tmpdir}/sqlforge/workflow-baselines.json}") String storageFilePath
    ) {
        this.objectMapper = objectMapper;
        this.storageFilePath = storageFilePath;
    }

    @PostConstruct
    public synchronized void load() {
        File storageFile = new File(storageFilePath);

        if (!storageFile.exists()) {
            return;
        }

        try {
            Map<String, Map<String, Object>> persisted = objectMapper.readValue(
                storageFile,
                new TypeReference<Map<String, Map<String, Object>>>() { }
            );
            baselines.clear();
            baselines.putAll(persisted);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read persisted workflow baselines", exception);
        }
    }

    @Override
    public synchronized Map<String, Object> get(String fingerprint) {
        return baselines.get(fingerprint);
    }

    @Override
    public synchronized void save(String fingerprint, String tenantId, String decision, Map<String, Object> summary) {
        Map<String, Object> record = new LinkedHashMap<String, Object>();
        record.put("fingerprint", fingerprint);
        record.put("tenantId", tenantId);
        record.put("decision", decision);
        record.put("summary", summary);
        record.put("updatedAt", Instant.now().toString());
        baselines.put(fingerprint, record);
        persist();
    }

    private void persist() {
        File storageFile = new File(storageFilePath);
        File parent = storageFile.getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("failed to create workflow baseline storage directory");
        }

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storageFile, baselines);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to persist workflow baselines", exception);
        }
    }
}
