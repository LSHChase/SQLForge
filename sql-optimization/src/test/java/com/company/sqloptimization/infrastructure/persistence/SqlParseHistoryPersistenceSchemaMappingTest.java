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

class SqlParseHistoryPersistenceSchemaMappingTest {

    @Test
    void shouldKeepSqlParseHistoryTableInInitSchemaAndMigration() throws IOException {
        String schema = readRepositoryFile("sql/init-schema.sql");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS sql_parse_history");
        assertContains(schema, "parse_history_id VARCHAR(128) NOT NULL");
        assertContains(schema, "source_type VARCHAR(32) NOT NULL");
        assertContains(schema, "batch_key VARCHAR(255)");
        assertContains(schema, "structure_parse_summary_json JSON");
        assertContains(schema, "access_parse_summary_json JSON");
        assertContains(schema, "idx_sql_parse_history_batch");

        String migration = readRepositoryFile("sql/migrations/V20260508_001__sql_parse_history_decoupling.sql");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS sql_parse_history");
        assertContains(migration, "SQL parse history id for this parsed SQL");
        assertContains(migration, "SQL parse history id for this parsed report SQL");

        String compatibilityMigration =
            readRepositoryFile("sql/migrations/V20260509_001__sql_parse_history_compatibility.sql");
        assertContains(compatibilityMigration, "ensure_sql_parse_history_compatibility");
        assertContains(compatibilityMigration, "binding_summary_json");
        assertContains(compatibilityMigration, "logical_object_hits_json");
        assertContains(compatibilityMigration, "issue_scenes_json");
        assertContains(compatibilityMigration, "logical_object_keys_json");
        assertContains(compatibilityMigration, "idx_sql_parse_history_trace");
    }

    @Test
    void shouldKeepSqlParseHistoryMapperAlignedWithSchema() throws IOException {
        String mapper = readMapper("mapper/SqlParseHistoryMapper.xml");
        assertContains(mapper, "FROM sql_parse_history");
        assertContains(mapper, "selectByParseHistoryId");
        assertContains(mapper, "selectByBatchKeyAndSqlFingerprint");
        assertContains(mapper, "structure_parse_summary_json");
        assertContains(mapper, "logical_object_hits_json");
        assertContains(mapper, "ORDER BY ${orderByClause}");
    }

    private static String readMapper(String resourcePath) throws IOException {
        InputStream inputStream = SqlParseHistoryPersistenceSchemaMappingTest.class
            .getClassLoader()
            .getResourceAsStream(resourcePath);
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
