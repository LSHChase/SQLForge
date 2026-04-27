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

class ParseBatchPersistenceSchemaMappingTest {

    @Test
    void shouldKeepParseBatchTableInInitSchema() throws IOException {
        String schema = readRepositoryFile("sql/init-schema.sql");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS parse_batch");
        assertContains(schema, "import_mode VARCHAR(32) NOT NULL");
        assertContains(schema, "structure_parse_only TINYINT(1) NOT NULL DEFAULT 0");
        assertContains(schema, "status_history_json JSON NOT NULL");
        assertContains(schema, "idx_parse_batch_tenant_status_created");
    }

    @Test
    void shouldProvideIncrementalMigrationForParseBatchContract() throws IOException {
        String migration = readRepositoryFile("sql/migrations/V20260426_004__parse_batch_contract.sql");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS parse_batch");
        assertContains(migration, "source_type VARCHAR(32) NOT NULL");
        assertContains(migration, "structure_parse_success_rate DECIMAL(6,2)");
    }

    @Test
    void shouldKeepParseBatchItemSchemaAndMigrationAligned() throws IOException {
        String schema = readRepositoryFile("sql/init-schema.sql");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS parse_batch_item");
        assertContains(schema, "sequence_number INT NOT NULL");
        assertContains(schema, "issue_scenes_json JSON");
        String migration = readRepositoryFile("sql/migrations/V20260426_005__parse_batch_ingestion.sql");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS parse_batch_item");
        assertContains(migration, "logical_object_keys_json JSON");
        String mapper = readMapper("mapper/ParseBatchItemMapper.xml");
        assertContains(mapper, "FROM parse_batch_item");
        assertContains(mapper, "sequence_number");
        assertContains(mapper, "access_service_status");
    }

    @Test
    void shouldKeepMapperXmlAlignedWithParseBatchTable() throws IOException {
        String mapper = readMapper("mapper/ParseBatchMapper.xml");
        assertContains(mapper, "FROM parse_batch");
        assertContains(mapper, "import_mode");
        assertContains(mapper, "source_type");
        assertContains(mapper, "status_history_json");
    }

    private static String readMapper(String resourcePath) throws IOException {
        InputStream inputStream = ParseBatchPersistenceSchemaMappingTest.class.getClassLoader().getResourceAsStream(resourcePath);
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
