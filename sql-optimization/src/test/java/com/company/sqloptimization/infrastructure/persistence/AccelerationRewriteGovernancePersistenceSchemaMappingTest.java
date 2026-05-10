package com.company.sqloptimization.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

class AccelerationRewriteGovernancePersistenceSchemaMappingTest {

    @Test
    void shouldKeepAccelerationRewriteTablesInInitSchema() throws IOException {
        String schema = readRepositoryFile("sql/init-schema.sql");

        assertContains(schema, "CREATE TABLE IF NOT EXISTS acceleration_candidate");
        assertContains(schema, "source_type VARCHAR(32) NOT NULL");
        assertContains(schema, "report_code VARCHAR(128) DEFAULT NULL");
        assertContains(schema, "source_evidence_json JSON DEFAULT NULL");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS sql_rewrite_record");
        assertContains(schema, "original_sql_text MEDIUMTEXT NOT NULL");
        assertContains(schema, "rule_chain_json JSON DEFAULT NULL");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS rewrite_validation_run");
        assertContains(schema, "auto_apply_paused TINYINT(1) NOT NULL DEFAULT 0");
        assertContains(schema, "original_result_digest_json JSON DEFAULT NULL");
    }

    @Test
    void shouldProvideIncrementalMigrationWithoutPhysicalForeignKeys() throws IOException {
        String migration = readRepositoryFile(
            "sql/migrations/V20260510_001__acceleration_rewrite_governance_persistence.sql"
        );

        assertContains(migration, "CREATE TABLE IF NOT EXISTS acceleration_candidate");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS sql_rewrite_record");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS rewrite_validation_run");
        assertContains(migration, "idx_acc_candidate_source");
        assertContains(migration, "idx_rewrite_record_source");
        assertContains(migration, "idx_validation_run_rewrite_started");
        assertFalse(migration.toUpperCase().contains("FOREIGN KEY"), "migration must not add physical foreign keys");
    }

    @Test
    void shouldKeepMapperXmlAlignedWithAccelerationRewriteTables() throws IOException {
        String candidateMapper = readMapper("mapper/AccelerationCandidateMapper.xml");
        String rewriteMapper = readMapper("mapper/SqlRewriteRecordMapper.xml");
        String validationMapper = readMapper("mapper/RewriteValidationRunMapper.xml");

        assertContains(candidateMapper, "FROM acceleration_candidate");
        assertContains(candidateMapper, "source_evidence_json");
        assertContains(candidateMapper, "ORDER BY created_at DESC");
        assertContains(rewriteMapper, "FROM sql_rewrite_record");
        assertContains(rewriteMapper, "rule_chain_json");
        assertContains(rewriteMapper, "last_validation_run_id");
        assertContains(rewriteMapper, "selectByTenantIdAndHistoryId");
        assertContains(rewriteMapper, "AND history_id = #{historyId}");
        assertContains(validationMapper, "FROM rewrite_validation_run");
        assertContains(validationMapper, "auto_apply_paused");
        assertContains(validationMapper, "ORDER BY started_at DESC");
        assertFalse(candidateMapper.contains("${"), "candidate mapper must use bound parameters");
        assertFalse(rewriteMapper.contains("${"), "rewrite mapper must use bound parameters");
        assertFalse(validationMapper.contains("${"), "validation mapper must use bound parameters");
    }

    private static String readMapper(String resourcePath) throws IOException {
        InputStream inputStream =
            AccelerationRewriteGovernancePersistenceSchemaMappingTest.class.getClassLoader()
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
