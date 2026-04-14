package com.sqlforge.backend.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlforge.backend.model.ConnectionActivity;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileConnectionActivityRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndFindByConnectionIdShouldPersistActivities() {
        Path storageFile = tempDir.resolve("connection-activity.json");
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        FileConnectionActivityRepository repository =
            new FileConnectionActivityRepository(objectMapper, storageFile.toString());

        repository.load();
        repository.save(
            new ConnectionActivity(
                "activity-1",
                "conn-1",
                "Primary Trino",
                "probe",
                "connected",
                "probe succeeded",
                Instant.parse("2026-04-14T00:00:00Z")
            )
        );

        List<ConnectionActivity> activities = repository.findByConnectionId("conn-1", 20);

        assertEquals(1, activities.size());
        assertEquals("probe", activities.get(0).getActionType());
        assertEquals("connected", activities.get(0).getActionStatus());
    }
}
