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
        assertContains(schema, "CREATE TABLE IF NOT EXISTS export_record");
        assertContains(schema, "config_snapshot_id VARCHAR(64) DEFAULT NULL");
        assertContains(schema, "result_id VARCHAR(64) DEFAULT NULL");
        assertContains(schema, "history_id VARCHAR(64) DEFAULT NULL");
        assertContains(schema, "export_id VARCHAR(64) DEFAULT NULL");
        assertContains(schema, "CONSTRAINT fk_execution_result_config_snapshot");
        assertContains(schema, "CONSTRAINT fk_query_history_result");
        assertContains(schema, "CONSTRAINT fk_export_record_history");
        assertContains(schema, "CONSTRAINT fk_export_record_result");
        assertContains(schema, "CONSTRAINT fk_audit_log_config_snapshot");
        assertContains(schema, "CONSTRAINT fk_audit_log_result");
        assertContains(schema, "CONSTRAINT fk_audit_log_history");
        assertContains(schema, "CONSTRAINT fk_audit_log_export");
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
        assertContains(migration, "ADD CONSTRAINT fk_audit_log_export");
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
    void shouldKeepMapperXmlAlignedWithCoreTraceabilityTables() throws IOException {
        assertContains(readMapper("mapper/ConfigSnapshotMapper.xml"), "FROM config_snapshot");
        assertContains(readMapper("mapper/ConfigSnapshotMapper.xml"), "config_snapshot_id");
        assertContains(readMapper("mapper/ExecutionResultMapper.xml"), "FROM execution_result");
        assertContains(readMapper("mapper/ExecutionResultMapper.xml"), "config_snapshot_id");
        assertContains(readMapper("mapper/QueryHistoryMapper.xml"), "FROM query_history");
        assertContains(readMapper("mapper/QueryHistoryMapper.xml"), "result_id");
        assertContains(readMapper("mapper/QueryHistoryMapper.xml"), "trace_id = #{traceId}");
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
