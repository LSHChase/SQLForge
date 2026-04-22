package com.company.sqloptimization.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class OptimizationTaskPersistenceSchemaMappingTest {

    @Test
    void shouldKeepOptimizationTaskTableInInitSchema() throws IOException {
        String schema = readRepositoryFile("sql/init-schema.sql");

        assertContains(schema, "CREATE TABLE IF NOT EXISTS optimization_task");
        assertContains(schema, "requested_suggestion_types_json JSON");
        assertContains(schema, "status_history_json JSON NOT NULL");
        assertContains(schema, "idx_optimization_task_status_submitted");
    }

    @Test
    void shouldProvideIncrementalMigrationForOptimizationTaskPersistence() throws IOException {
        String migration = readRepositoryFile("sql/migrations/V20260422_014__sql_optimization_task_persistence.sql");

        assertContains(migration, "CREATE TABLE IF NOT EXISTS optimization_task");
        assertContains(migration, "current_phase VARCHAR(32) NOT NULL");
        assertContains(migration, "status_history_json JSON NOT NULL");
    }

    @Test
    void shouldKeepMapperXmlAlignedWithOptimizationTaskTable() throws IOException {
        String mapper = readMapper("mapper/OptimizationTaskMapper.xml");

        assertContains(mapper, "FROM optimization_task");
        assertContains(mapper, "requested_suggestion_types_json");
        assertContains(mapper, "status_history_json");
        assertContains(mapper, "WHERE status = 'QUEUED'");
    }

    private static String readMapper(String resourcePath) throws IOException {
        InputStream inputStream = OptimizationTaskPersistenceSchemaMappingTest.class.getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(inputStream, "missing mapper resource: " + resourcePath);
        try (InputStream stream = inputStream) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int readLength = stream.read(buffer);
            while (readLength != -1) {
                outputStream.write(buffer, 0, readLength);
                readLength = stream.read(buffer);
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String readRepositoryFile(String relativePath) throws IOException {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.exists(candidate)) {
                return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
            }
            current = current.getParent();
        }
        throw new IOException("Unable to locate repository file: " + relativePath);
    }

    private static void assertContains(String content, String expected) {
        assertTrue(content.contains(expected), "missing expected fragment: " + expected);
    }
}
