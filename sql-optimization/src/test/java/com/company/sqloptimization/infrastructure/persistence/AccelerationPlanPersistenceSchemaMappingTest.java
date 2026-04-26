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

class AccelerationPlanPersistenceSchemaMappingTest {

    @Test
    void shouldKeepAccelerationPlanTableInInitSchema() throws IOException {
        String schema = readRepositoryFile("sql/init-schema.sql");

        assertContains(schema, "CREATE TABLE IF NOT EXISTS acceleration_plan");
        assertContains(schema, "selected_suggestion_types_json JSON NOT NULL");
        assertContains(schema, "plan_status VARCHAR(32) NOT NULL");
        assertContains(schema, "runtime_binding_json JSON");
        assertContains(schema, "verification_evidence_json JSON");
        assertContains(schema, "rollback_evidence_json JSON");
        assertContains(schema, "status_history_json JSON NOT NULL");
        assertContains(schema, "idx_acceleration_plan_status_created");
        assertContains(schema, "SQL_ACCELERATION_PLAN");
    }

    @Test
    void shouldProvideIncrementalMigrationForGovernedAccelerationPlan() throws IOException {
        String migration = readRepositoryFile("sql/migrations/V20260425_002__acceleration_plan_governance.sql");

        assertContains(migration, "CREATE TABLE IF NOT EXISTS acceleration_plan");
        assertContains(migration, "plan_status VARCHAR(32) NOT NULL");
        assertContains(migration, "runtime_binding_json JSON");
        assertContains(migration, "status_history_json JSON NOT NULL");
        assertContains(migration, "DELETE FROM governance_history_lookup_index");
        assertContains(migration, "SQL_ACCELERATION_PLAN");
        assertContains(migration, "CREATE TRIGGER trg_audit_log_lookup_index_ai");
    }

    @Test
    void shouldKeepMapperXmlAlignedWithAccelerationPlanTable() throws IOException {
        String mapper = readMapper("mapper/AccelerationPlanMapper.xml");

        assertContains(mapper, "FROM acceleration_plan");
        assertContains(mapper, "selected_suggestion_types_json");
        assertContains(mapper, "plan_status");
        assertContains(mapper, "runtime_binding_json");
        assertContains(mapper, "verification_evidence_json");
        assertContains(mapper, "rollback_evidence_json");
        assertContains(mapper, "status_history_json");
    }

    private static String readMapper(String resourcePath) throws IOException {
        InputStream inputStream = AccelerationPlanPersistenceSchemaMappingTest.class.getClassLoader().getResourceAsStream(resourcePath);
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
