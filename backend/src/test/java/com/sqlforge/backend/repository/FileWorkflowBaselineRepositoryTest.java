package com.sqlforge.backend.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileWorkflowBaselineRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndReloadShouldPersistWorkflowBaseline() throws Exception {
        Path storageFile = tempDir.resolve("workflow-baselines.json");
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        FileWorkflowBaselineRepository repository = new FileWorkflowBaselineRepository(objectMapper, storageFile.toString());

        repository.load();
        repository.save("fp-1", "tenant-a", "approved", summary(1250.0d, 1100.0d));

        FileWorkflowBaselineRepository reloaded = new FileWorkflowBaselineRepository(objectMapper, storageFile.toString());
        reloaded.load();

        Map<String, Object> baseline = reloaded.get("fp-1");
        String persistedJson = new String(Files.readAllBytes(storageFile), StandardCharsets.UTF_8);

        assertEquals("tenant-a", baseline.get("tenantId"));
        assertEquals("approved", baseline.get("decision"));
        assertTrue(persistedJson.contains("\"fp-1\""));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> summary(double p99Ms, double averageLatencyMs) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("p99Ms", Double.valueOf(p99Ms));
        summary.put("averageLatencyMs", Double.valueOf(averageLatencyMs));
        return summary;
    }
}
