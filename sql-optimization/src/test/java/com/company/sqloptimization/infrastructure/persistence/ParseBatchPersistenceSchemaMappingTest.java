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
        assertContains(mapper, "selectAll");
        assertContains(mapper, "ORDER BY batch_id ASC, sequence_number ASC");
    }

    @Test
    void shouldKeepReportBatchSchemaAndMigrationAligned() throws IOException {
        String schema = readRepositoryFile("sql/init-schema.sql");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS report_batch");
        assertContains(schema, "report_code_field VARCHAR(128) NOT NULL");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS report_batch_item");
        assertContains(schema, "source_file_line VARCHAR(512)");
        String migration = readRepositoryFile("sql/migrations/V20260426_006__report_batch_catalog.sql");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS report_batch");
        assertContains(migration, "report_batch_item");
        String mapper = readMapper("mapper/ReportBatchMapper.xml");
        assertContains(mapper, "FROM report_batch");
        String itemMapper = readMapper("mapper/ReportBatchItemMapper.xml");
        assertContains(itemMapper, "FROM report_batch_item");
        assertContains(itemMapper, "structure_syntax_status");
    }

    @Test
    void shouldKeepAccelerationRecommendationSchemaAndMigrationAligned() throws IOException {
        String schema = readRepositoryFile("sql/init-schema.sql");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS acceleration_recommendation");
        assertContains(schema, "recommendation_type VARCHAR(32) NOT NULL");
        assertContains(schema, "recommended_sql_text MEDIUMTEXT NOT NULL");
        assertContains(schema, "history_id VARCHAR(64)");
        assertContains(schema, "parse_task_id VARCHAR(64)");
        assertContains(schema, "route_decision_id VARCHAR(64)");
        assertContains(schema, "benefit_level VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN'");
        assertContains(schema, "risk_level VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN'");
        assertContains(schema, "no executed state in SQLForge");
        String migration = readRepositoryFile("sql/migrations/V20260426_007__acceleration_recommendation_catalog.sql");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS acceleration_recommendation");
        assertContains(migration, "idx_acc_reco_tenant_type_created");
        String mapper = readMapper("mapper/AccelerationRecommendationMapper.xml");
        assertContains(mapper, "FROM acceleration_recommendation");
        assertContains(mapper, "selectByTenantId");
        assertContains(mapper, "requires_dispatch");
        String traceMigration = readRepositoryFile("sql/migrations/V20260426_009__recommendation_traceability_keys.sql");
        assertContains(traceMigration, "ADD COLUMN history_id");
        assertContains(traceMigration, "idx_acc_reco_parse_task");
    }

    @Test
    void shouldKeepDispatchEventSchemaAndMigrationAligned() throws IOException {
        String schema = readRepositoryFile("sql/init-schema.sql");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS dispatch_event");
        assertContains(schema, "dispatch_payload_json JSON NOT NULL");
        assertContains(schema, "status_history_json JSON NOT NULL");
        assertContains(schema, "CREATED/PUBLISHED/PULLED/ACKED/FAILED");
        String migration = readRepositoryFile("sql/migrations/V20260426_008__dispatch_event_state_machine.sql");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS dispatch_event");
        assertContains(migration, "idx_dispatch_event_tenant_status_created");
        String mapper = readMapper("mapper/DispatchEventMapper.xml");
        assertContains(mapper, "FROM dispatch_event");
        assertContains(mapper, "selectByTenantIdAndStatus");
        assertContains(mapper, "selectByTenantIdAndRecommendationId");
        assertContains(mapper, "status_history_json");
    }

    @Test
    void shouldKeepMapperXmlAlignedWithParseBatchTable() throws IOException {
        String mapper = readMapper("mapper/ParseBatchMapper.xml");
        assertContains(mapper, "FROM parse_batch");
        assertContains(mapper, "import_mode");
        assertContains(mapper, "source_type");
        assertContains(mapper, "status_history_json");
        assertContains(mapper, "selectAll");
    }

    @Test
    void shouldKeepReportBatchMappersAlignedWithHistoryQueries() throws IOException {
        String mapper = readMapper("mapper/ReportBatchMapper.xml");
        assertContains(mapper, "FROM report_batch");
        assertContains(mapper, "selectAll");
        String itemMapper = readMapper("mapper/ReportBatchItemMapper.xml");
        assertContains(itemMapper, "FROM report_batch_item");
        assertContains(itemMapper, "selectAll");
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
