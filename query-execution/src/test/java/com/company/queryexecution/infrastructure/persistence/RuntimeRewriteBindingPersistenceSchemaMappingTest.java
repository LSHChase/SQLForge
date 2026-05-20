package com.company.queryexecution.infrastructure.persistence;

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

class RuntimeRewriteBindingPersistenceSchemaMappingTest {

    @Test
    void shouldKeepRuntimeRewriteBindingTableInInitSchema() throws IOException {
        String schema = readRepositoryFile("sql/init-schema.sql");

        assertContains(schema, "CREATE TABLE IF NOT EXISTS runtime_rewrite_binding");
        assertContains(schema, "status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'");
        assertContains(schema, "rule_version BIGINT NOT NULL DEFAULT 1");
        assertContains(schema, "runtime_rule_version VARCHAR(64) NOT NULL");
        assertContains(schema, "recommended_sql_text MEDIUMTEXT NOT NULL");
        assertContains(schema, "active_binding_key VARCHAR(320) GENERATED ALWAYS AS");
        assertContains(schema, "UNIQUE KEY uk_runtime_rewrite_active_binding (active_binding_key)");
    }

    @Test
    void shouldProvideIncrementalMigrationWithoutPhysicalForeignKeys() throws IOException {
        String migration = readRepositoryFile(
            "sql/migrations/V20260511_002__query_execution_runtime_rewrite_binding.sql"
        );

        assertContains(migration, "CREATE TABLE IF NOT EXISTS runtime_rewrite_binding");
        assertContains(migration, "运行时改写绑定状态：ACTIVE/PAUSED");
        assertContains(migration, "UNIQUE KEY uk_runtime_rewrite_active_binding (active_binding_key)");
        assertContains(migration, "KEY idx_runtime_rewrite_tenant_fingerprint");
        assertFalse(migration.toUpperCase().contains("FOREIGN KEY"), "migration 不得新增物理外键约束");
    }

    @Test
    void shouldKeepMapperXmlAlignedWithRuntimeRewriteBindingTable() throws IOException {
        String mapper = readMapper("mapper/RuntimeRewriteBindingMapper.xml");

        assertContains(mapper, "FROM runtime_rewrite_binding");
        assertContains(mapper, "selectActiveByTenantIdAndSqlFingerprint");
        assertContains(mapper, "AND status = 'ACTIVE'");
        assertContains(mapper, "ORDER BY rule_version DESC, created_at DESC");
        assertContains(mapper, "runtime_rule_version");
        assertFalse(mapper.contains("${"), "runtime rewrite binding mapper 必须使用绑定参数");
    }

    private static String readMapper(String resourcePath) throws IOException {
        InputStream inputStream =
            RuntimeRewriteBindingPersistenceSchemaMappingTest.class.getClassLoader()
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
