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
        assertContains(schema, "review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW'");
        assertContains(schema, "publish_status VARCHAR(32) NOT NULL DEFAULT 'UNPUBLISHED'");
        assertContains(schema, "runtime_binding_id VARCHAR(64) DEFAULT NULL");
        assertContains(schema, "runtime_rule_version VARCHAR(64) DEFAULT NULL");
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
        String reviewPublishMigration = readRepositoryFile(
            "sql/migrations/V20260511_001__sql_rewrite_record_review_publish_runtime_fields.sql"
        );

        assertContains(migration, "CREATE TABLE IF NOT EXISTS acceleration_candidate");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS sql_rewrite_record");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS rewrite_validation_run");
        assertContains(migration, "idx_acc_candidate_source");
        assertContains(migration, "idx_rewrite_record_source");
        assertContains(migration, "idx_validation_run_rewrite_started");
        assertFalse(migration.toUpperCase().contains("FOREIGN KEY"), "migration 不得新增物理外键约束");
        assertContains(reviewPublishMigration, "ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW'");
        assertContains(reviewPublishMigration, "ADD COLUMN publish_status VARCHAR(32) NOT NULL DEFAULT 'UNPUBLISHED'");
        assertContains(reviewPublishMigration, "ADD COLUMN runtime_binding_id VARCHAR(64) DEFAULT NULL");
        assertContains(reviewPublishMigration, "ADD COLUMN runtime_rule_version VARCHAR(64) DEFAULT NULL");
        assertFalse(
            reviewPublishMigration.toUpperCase().contains("FOREIGN KEY"),
            "review/publish migration 不得新增物理外键约束"
        );
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
        assertContains(rewriteMapper, "review_status");
        assertContains(rewriteMapper, "publish_status");
        assertContains(rewriteMapper, "runtime_binding_id");
        assertContains(rewriteMapper, "runtime_rule_version");
        assertContains(rewriteMapper, "last_validation_run_id");
        assertContains(rewriteMapper, "selectByTenantIdAndHistoryId");
        assertContains(rewriteMapper, "AND history_id = #{historyId}");
        assertContains(validationMapper, "FROM rewrite_validation_run");
        assertContains(validationMapper, "auto_apply_paused");
        assertContains(validationMapper, "ORDER BY started_at DESC");
        assertFalse(candidateMapper.contains("${"), "candidate mapper 必须使用绑定参数");
        assertFalse(rewriteMapper.contains("${"), "rewrite mapper 必须使用绑定参数");
        assertFalse(validationMapper.contains("${"), "validation mapper 必须使用绑定参数");
    }

    private static String readMapper(String resourcePath) throws IOException {
        InputStream inputStream =
            AccelerationRewriteGovernancePersistenceSchemaMappingTest.class.getClassLoader()
                .getResourceAsStream(resourcePath);
        assertNotNull(inputStream, "缺少 mapper 资源：" + resourcePath);
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
        assertTrue(content.contains(expected), "缺少预期片段：" + expected);
    }
}
