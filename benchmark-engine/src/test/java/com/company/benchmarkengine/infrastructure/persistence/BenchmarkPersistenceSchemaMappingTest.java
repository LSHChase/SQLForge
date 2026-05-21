package com.company.benchmarkengine.infrastructure.persistence;

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

class BenchmarkPersistenceSchemaMappingTest {

    @Test
    void shouldKeepBenchmarkSchemaFieldsInInitSchema() throws IOException {
        String schema = readRepositoryFile("sql/init-schema.sql");

        assertContains(schema, "CREATE TABLE IF NOT EXISTS benchmark_task");
        assertContains(schema, "scale_target_json JSON DEFAULT NULL");
        assertContains(schema, "template_id VARCHAR(64) DEFAULT NULL");
        assertContains(schema, "test_set_source_refs_json JSON DEFAULT NULL");
        assertContains(schema, "CREATE TABLE IF NOT EXISTS benchmark_task_report");
        assertContains(schema, "execution_summary_json JSON DEFAULT NULL");
        assertContains(schema, "regression_summary_json JSON DEFAULT NULL");
        assertContains(schema, "alert_linkages_json JSON DEFAULT NULL");
        assertContains(schema, "export_artifacts_json JSON DEFAULT NULL");
    }

    @Test
    void shouldProvideIncrementalMigrationForBenchmarkScaleTarget() throws IOException {
        String migration = readRepositoryFile("sql/migrations/V20260518_002__benchmark_task_scale_target.sql");

        assertContains(migration, "information_schema.columns");
        assertContains(migration, "column_name = 'scale_target_json'");
        assertContains(migration, "ADD COLUMN scale_target_json JSON DEFAULT NULL");
    }

    @Test
    void shouldKeepMapperXmlAlignedWithBenchmarkSchema() throws IOException {
        assertContains(readMapper("mapper/BenchmarkTaskMapper.xml"), "scale_target_json");
        assertContains(readMapper("mapper/BenchmarkTaskMapper.xml"), "test_set_source_refs_json");
        assertContains(readMapper("mapper/BenchmarkReportMapper.xml"), "execution_summary_json");
        assertContains(readMapper("mapper/BenchmarkReportMapper.xml"), "regression_summary_json");
        assertContains(readMapper("mapper/BenchmarkReportMapper.xml"), "alert_linkages_json");
        assertContains(readMapper("mapper/BenchmarkReportMapper.xml"), "export_artifacts_json");
    }

    private static String readMapper(String resourcePath) throws IOException {
        InputStream inputStream =
            BenchmarkPersistenceSchemaMappingTest.class.getClassLoader().getResourceAsStream(resourcePath);
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
