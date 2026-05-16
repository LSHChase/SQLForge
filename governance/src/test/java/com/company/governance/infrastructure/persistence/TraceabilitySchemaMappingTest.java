package com.company.governance.infrastructure.persistence;

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

class TraceabilitySchemaMappingTest {

    @Test
    void shouldKeepCoreTraceabilityChainInInitSchema() throws IOException {
        String schema = readRepositoryFile("sql/init-schema.sql");

        assertContains(schema, "CREATE TABLE IF NOT EXISTS config_snapshot");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS execution_result");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS query_history");
        assertContains(schema, "access_channel VARCHAR(32) DEFAULT NULL");
        assertContains(schema, "target_engine VARCHAR(64) DEFAULT NULL");
        assertContains(schema, "cache_summary JSON DEFAULT NULL");
        assertContains(schema, "sql_template_cipher MEDIUMBLOB DEFAULT NULL");
        assertContains(schema, "bound_sql_text_cipher MEDIUMBLOB DEFAULT NULL");
        assertContains(schema, "report_code VARCHAR(128) DEFAULT NULL");
        assertContains(schema, "query_date_status VARCHAR(32) DEFAULT NULL");
        assertContains(schema, "binding_render_status VARCHAR(16) DEFAULT NULL");
        assertContains(schema, "comment_context JSON DEFAULT NULL");
        assertContains(schema, "rewrite_record_id VARCHAR(64) DEFAULT NULL");
        assertContains(schema, "runtime_binding_id VARCHAR(64) DEFAULT NULL");
        assertContains(schema, "rewrite_publish_status_snapshot VARCHAR(32) DEFAULT NULL");
        assertContains(schema, "idx_query_history_rewrite_record");
        assertContains(schema, "logical_object_hits JSON DEFAULT NULL");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS business_logical_view");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS logical_object_mapping");
        assertContains(schema, "target_object_key VARCHAR(255) NOT NULL");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS database_view_ref");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS database_view_dependency");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS export_record");
        assertContains(schema, "config_snapshot_id VARCHAR(64) DEFAULT NULL");
        assertContains(schema, "result_id VARCHAR(64) DEFAULT NULL");
        assertContains(schema, "history_id VARCHAR(64) DEFAULT NULL");
        assertContains(schema, "export_id VARCHAR(64) DEFAULT NULL");
        assertNotContains(schema, "CONSTRAINT fk_execution_result_config_snapshot");
        assertNotContains(schema, "CONSTRAINT fk_query_history_result");
        assertNotContains(schema, "CONSTRAINT fk_export_record_history");
        assertNotContains(schema, "CONSTRAINT fk_export_record_result");
        assertNotContains(schema, "CONSTRAINT fk_audit_log_config_snapshot");
        assertNotContains(schema, "CONSTRAINT fk_audit_log_result");
        assertNotContains(schema, "CONSTRAINT fk_audit_log_history");
        assertNotContains(schema, "CONSTRAINT fk_audit_log_export");
        assertNotContains(schema, "FOREIGN KEY");
        assertContains(schema, "sensitive_flag TINYINT(1) NOT NULL DEFAULT 0");
        assertContains(schema, "value_ciphertext TEXT DEFAULT NULL");
        assertContains(schema, "encryption_key_id VARCHAR(64) DEFAULT NULL");
    }

    @Test
    void shouldProvideIncrementalMigrationForCoreTraceabilityChain() throws IOException {
        String migration = readRepositoryFile("sql/migrations/V20260421_011__core_traceability_chain.sql");

        assertContains(migration, "ALTER TABLE audit_log");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS config_snapshot");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS execution_result");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS query_history");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS export_record");
    }

    @Test
    void shouldProvideIncrementalMigrationForSensitiveFieldEncryptionBaseline() throws IOException {
        String migration = readRepositoryFile("sql/migrations/V20260421_013__sensitive_data_encryption_baseline.sql");

        assertContains(migration, "ALTER TABLE system_config");
        assertContains(migration, "ADD COLUMN sensitive_flag");
        assertContains(migration, "ADD COLUMN value_ciphertext");
        assertContains(migration, "ADD COLUMN encryption_key_id");
    }

    @Test
    void shouldProvideIncrementalMigrationForExpandedHistoryAndExecutionEvidence() throws IOException {
        String migration = readRepositoryFile("sql/migrations/V20260426_001__query_history_execution_result_expansion.sql");

        assertContains(migration, "ALTER TABLE execution_result");
        assertContains(migration, "ADD COLUMN access_channel VARCHAR(32) DEFAULT NULL");
        assertContains(migration, "ADD COLUMN target_engine VARCHAR(64) DEFAULT NULL");
        assertContains(migration, "ADD COLUMN cache_summary JSON DEFAULT NULL");
        assertContains(migration, "ALTER TABLE query_history");
        assertContains(migration, "ADD COLUMN sql_template_cipher MEDIUMBLOB DEFAULT NULL");
        assertContains(migration, "ADD COLUMN report_code VARCHAR(128) DEFAULT NULL");
        assertContains(migration, "ADD COLUMN query_date_status VARCHAR(32) DEFAULT NULL");
        assertContains(migration, "ADD COLUMN binding_render_status VARCHAR(16) DEFAULT NULL");
        assertContains(migration, "ADD COLUMN comment_context JSON DEFAULT NULL");
        assertContains(migration, "ADD COLUMN logical_object_hits JSON DEFAULT NULL");
    }

    @Test
    void shouldProvideIncrementalMigrationForQueryHistoryRewriteAuditFields() throws IOException {
        String migration = readRepositoryFile("sql/migrations/V20260512_001__query_history_rewrite_audit_fields.sql");

        assertContains(migration, "ALTER TABLE query_history");
        assertContains(migration, "ADD COLUMN rewrite_record_id VARCHAR(64) DEFAULT NULL");
        assertContains(migration, "ADD COLUMN runtime_binding_id VARCHAR(64) DEFAULT NULL");
        assertContains(migration, "ADD COLUMN rewrite_rule_version BIGINT DEFAULT NULL");
        assertContains(migration, "ADD COLUMN runtime_rule_version VARCHAR(64) DEFAULT NULL");
        assertContains(migration, "ADD COLUMN rewrite_publish_status_snapshot VARCHAR(32) DEFAULT NULL");
        assertContains(migration, "ADD KEY idx_query_history_rewrite_record");
    }

    @Test
    void shouldProvideIncrementalMigrationForBusinessLogicalViewCatalog() throws IOException {
        String migration = readRepositoryFile("sql/migrations/V20260426_002__business_logical_view_catalog.sql");

        assertContains(migration, "CREATE TABLE IF NOT EXISTS business_logical_view");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS logical_object_mapping");
        assertContains(migration, "target_object_type VARCHAR(32) NOT NULL");
        assertContains(migration, "target_object_key VARCHAR(255) NOT NULL");
    }

    @Test
    void shouldProvideIncrementalMigrationForDatabaseViewCatalog() throws IOException {
        String migration = readRepositoryFile("sql/migrations/V20260426_003__database_view_catalog.sql");

        assertContains(migration, "CREATE TABLE IF NOT EXISTS database_view_ref");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS database_view_dependency");
        assertContains(migration, "dependency_object_key VARCHAR(255) NOT NULL");
    }

    @Test
    void shouldProvideIncrementalMigrationToDropLegacyForeignKeys() throws IOException {
        String migration = readRepositoryFile("sql/migrations/V20260423_017__drop_traceability_foreign_keys.sql");

        assertContains(migration, "drop_foreign_key_if_exists");
        assertContains(migration, "CALL drop_foreign_key_if_exists('execution_result', 'fk_execution_result_config_snapshot')");
        assertContains(migration, "CALL drop_foreign_key_if_exists('query_history', 'fk_query_history_result')");
        assertContains(migration, "CALL drop_foreign_key_if_exists('export_record', 'fk_export_record_history')");
        assertContains(migration, "CALL drop_foreign_key_if_exists('export_record', 'fk_export_record_result')");
        assertContains(migration, "CALL drop_foreign_key_if_exists('audit_log', 'fk_audit_log_export')");
    }

    @Test
    void shouldKeepMapperXmlAlignedWithCoreTraceabilityTables() throws IOException {
        assertContains(readMapper("mapper/ConfigSnapshotMapper.xml"), "FROM config_snapshot");
        assertContains(readMapper("mapper/ConfigSnapshotMapper.xml"), "config_snapshot_id");
        assertContains(readMapper("mapper/ExecutionResultMapper.xml"), "FROM execution_result");
        assertContains(readMapper("mapper/ExecutionResultMapper.xml"), "config_snapshot_id");
        assertContains(readMapper("mapper/ExecutionResultMapper.xml"), "access_channel");
        assertContains(readMapper("mapper/ExecutionResultMapper.xml"), "route_summary");
        assertContains(readMapper("mapper/BusinessLogicalViewMapper.xml"), "FROM business_logical_view");
        assertContains(readMapper("mapper/BusinessLogicalViewMapper.xml"), "view_code");
        assertContains(readMapper("mapper/LogicalObjectMappingMapper.xml"), "FROM logical_object_mapping");
        assertContains(readMapper("mapper/LogicalObjectMappingMapper.xml"), "target_object_key");
        assertContains(readMapper("mapper/DatabaseViewMapper.xml"), "FROM database_view_ref");
        assertContains(readMapper("mapper/DatabaseViewDependencyMapper.xml"), "FROM database_view_dependency");
        assertContains(readMapper("mapper/QueryHistoryMapper.xml"), "FROM query_history");
        assertContains(readMapper("mapper/QueryHistoryMapper.xml"), "result_id");
        assertContains(readMapper("mapper/QueryHistoryMapper.xml"), "sql_template_cipher");
        assertContains(readMapper("mapper/QueryHistoryMapper.xml"), "comment_context");
        assertContains(readMapper("mapper/QueryHistoryMapper.xml"), "binding_render_status");
        assertContains(readMapper("mapper/QueryHistoryMapper.xml"), "rewrite_record_id");
        assertContains(readMapper("mapper/QueryHistoryMapper.xml"), "rewrite_publish_status_snapshot");
        assertContains(readMapper("mapper/QueryHistoryMapper.xml"), "COALESCE(er.rewrite_applied, 0)");
        assertContains(readMapper("mapper/QueryHistoryMapper.xml"), "qh.history_type = #{historyType}");
        assertContains(readMapper("mapper/QueryHistoryMapper.xml"), "trace_id = #{traceId}");
        assertContains(readMapper("mapper/QueryHistoryMapper.xml"), "updateById");
        assertContains(readMapper("mapper/ExportRecordMapper.xml"), "FROM export_record");
        assertContains(readMapper("mapper/ExportRecordMapper.xml"), "history_id");
        assertContains(readMapper("mapper/ExportRecordMapper.xml"), "trace_id = #{traceId}");
        assertContains(readMapper("mapper/AuditLogMapper.xml"), "FROM audit_log");
        assertContains(readMapper("mapper/AuditLogMapper.xml"), "config_snapshot_id");
        assertContains(readMapper("mapper/AuditLogMapper.xml"), "result_id");
        assertContains(readMapper("mapper/AuditLogMapper.xml"), "history_id");
        assertContains(readMapper("mapper/AuditLogMapper.xml"), "export_id");
        assertContains(readMapper("mapper/AuditLogMapper.xml"), "trace_id = #{traceId}");
        assertContains(readMapper("mapper/SystemConfigMapper.xml"), "FROM system_config");
        assertContains(readMapper("mapper/SystemConfigMapper.xml"), "value_ciphertext");
        assertContains(readMapper("mapper/SystemConfigMapper.xml"), "encryption_key_id");
    }

    private static String readMapper(String resourcePath) throws IOException {
        InputStream inputStream = TraceabilitySchemaMappingTest.class.getClassLoader().getResourceAsStream(resourcePath);
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

    private static void assertNotContains(String content, String unexpected) {
        assertTrue(!content.contains(unexpected), "出现了不应存在的片段：" + unexpected);
    }
}
