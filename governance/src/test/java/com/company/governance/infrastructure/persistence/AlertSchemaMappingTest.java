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

class AlertSchemaMappingTest {

    @Test
    void shouldKeepAlertTablesInInitSchema() throws IOException {
        String schema = readRepositoryFile("sql/init-schema.sql");

        assertContains(schema, "CREATE TABLE IF NOT EXISTS alert_policy");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS alert_event");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS alert_notification_log");
        assertContains(schema, "dedupe_window_seconds INT NOT NULL DEFAULT 900");
        assertContains(schema, "notify_status VARCHAR(32) NOT NULL DEFAULT 'SIMULATED_PENDING_NOTIFY'");
        assertContains(schema, "delivery_status VARCHAR(32) NOT NULL COMMENT '投递状态：SIMULATED_SENT/DEDUPE_SUPPRESSED/SIMULATED_FAILED'");
        assertContains(schema, "alert_status VARCHAR(16) NOT NULL DEFAULT 'OPEN'");
        assertContains(schema, "idx_alert_event_tenant_dedupe_created");
        assertContains(schema, "idx_alert_notification_tenant_dedupe_created");
    }

    @Test
    void shouldProvideIncrementalMigrationForAlertBaseline() throws IOException {
        String migration = readRepositoryFile("sql/migrations/V20260427_001__governance_alert_baseline.sql");

        assertContains(migration, "CREATE TABLE IF NOT EXISTS alert_policy");
        assertContains(migration, "CREATE TABLE IF NOT EXISTS alert_event");
        assertContains(migration, "TENANT_ALERT_TYPE_TARGET");
        assertContains(migration, "SIMULATED_PENDING_NOTIFY/SIMULATED_NOTIFIED/SIMULATED_NOTIFY_FAILED");
        String notificationMigration = readRepositoryFile("sql/migrations/V20260427_002__governance_alert_notification_log.sql");
        assertContains(notificationMigration, "CREATE TABLE IF NOT EXISTS alert_notification_log");
        assertContains(notificationMigration, "SIMULATED_SENT/DEDUPE_SUPPRESSED/SIMULATED_FAILED");
    }

    @Test
    void shouldKeepAlertMapperXmlAlignedWithSchema() throws IOException {
        String eventMapper = readMapper("mapper/AlertEventMapper.xml");
        assertContains(eventMapper, "FROM alert_event");
        assertContains(eventMapper, "selectOpenByTenantId");
        assertContains(eventMapper, "selectByTenantIdAndDedupeKey");
        assertContains(eventMapper, "notify_status");
        String policyMapper = readMapper("mapper/AlertPolicyMapper.xml");
        assertContains(policyMapper, "FROM alert_policy");
        assertContains(policyMapper, "selectEnabledByTenantId");
        assertContains(policyMapper, "dedupe_strategy");
        assertContains(policyMapper, "initial_notify_status");
        String notificationMapper = readMapper("mapper/AlertNotificationLogMapper.xml");
        assertContains(notificationMapper, "FROM alert_notification_log");
        assertContains(notificationMapper, "selectByAlertId");
        assertContains(notificationMapper, "delivery_status");
        assertContains(notificationMapper, "source_alert_id");
    }

    private static String readMapper(String resourcePath) throws IOException {
        InputStream inputStream = AlertSchemaMappingTest.class.getClassLoader().getResourceAsStream(resourcePath);
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
