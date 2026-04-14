package com.sqlforge.backend.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlforge.backend.model.ConnectionDefinition;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileConnectionRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndReloadShouldPersistConnectionMetadataOnly() throws Exception {
        Path storageFile = tempDir.resolve("connections.json");
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        FileConnectionRepository repository = new FileConnectionRepository(objectMapper, storageFile.toString());

        repository.load();
        repository.save(
            new ConnectionDefinition(
                "conn-1",
                "Primary Trino",
                "trino",
                "trino.sqlforge.local",
                8443,
                "lakehouse",
                "analyst",
                true,
                "registered",
                Instant.parse("2026-04-13T00:00:00Z")
            )
        );

        FileConnectionRepository reloaded = new FileConnectionRepository(objectMapper, storageFile.toString());
        reloaded.load();

        List<ConnectionDefinition> connections = reloaded.findAll();
        String persistedJson = new String(Files.readAllBytes(storageFile), StandardCharsets.UTF_8);

        assertEquals(1, connections.size());
        assertEquals("Primary Trino", connections.get(0).getName());
        assertFalse(persistedJson.contains("password"));
    }

    @Test
    void deleteByIdShouldRemovePersistedConnection() throws Exception {
        Path storageFile = tempDir.resolve("connections-delete.json");
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        FileConnectionRepository repository = new FileConnectionRepository(objectMapper, storageFile.toString());

        repository.load();
        repository.save(
            new ConnectionDefinition(
                "conn-delete",
                "Delete Me",
                "mysql",
                "127.0.0.1",
                3306,
                "sqlforge",
                "analyst",
                false,
                "registered",
                Instant.parse("2026-04-13T00:00:00Z")
            )
        );

        boolean deleted = repository.deleteById("conn-delete");
        FileConnectionRepository reloaded = new FileConnectionRepository(objectMapper, storageFile.toString());
        reloaded.load();

        assertTrue(deleted);
        assertEquals(0, reloaded.findAll().size());
    }

    @Test
    void updateShouldReplacePersistedConnectionMetadata() throws Exception {
        Path storageFile = tempDir.resolve("connections-update.json");
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        FileConnectionRepository repository = new FileConnectionRepository(objectMapper, storageFile.toString());

        repository.load();
        repository.save(
            new ConnectionDefinition(
                "conn-update",
                "Old Name",
                "mysql",
                "127.0.0.1",
                3306,
                "sqlforge",
                "analyst",
                false,
                "registered",
                Instant.parse("2026-04-13T00:00:00Z")
            )
        );

        repository.update(
            new ConnectionDefinition(
                "conn-update",
                "New Name",
                "trino",
                "127.0.0.1",
                8080,
                "lakehouse",
                "analyst",
                true,
                "registered",
                Instant.parse("2026-04-13T00:00:00Z")
            )
        );

        FileConnectionRepository reloaded = new FileConnectionRepository(objectMapper, storageFile.toString());
        reloaded.load();

        assertEquals(1, reloaded.findAll().size());
        assertEquals("New Name", reloaded.findAll().get(0).getName());
        assertEquals("trino", reloaded.findAll().get(0).getEngineCode());
    }
}
