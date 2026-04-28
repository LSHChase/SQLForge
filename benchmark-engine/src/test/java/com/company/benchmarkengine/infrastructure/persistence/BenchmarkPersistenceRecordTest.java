package com.company.benchmarkengine.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkReportRecord;
import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkTaskRecord;
import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkTestSetCaseRecord;
import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkTestSetRecord;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class BenchmarkPersistenceRecordTest {

    @Test
    void shouldExposeBenchmarkTaskRecordBeanContract() {
        BenchmarkTaskRecord record = new BenchmarkTaskRecord();
        LocalDateTime now = LocalDateTime.of(2026, 4, 22, 9, 30, 0);
        record.setTaskId("task-001");
        record.setTenantId("tenant-a");
        record.setTaskType("COMPARISON");
        record.setSqlText("SELECT * FROM orders");
        record.setSqlFingerprint("fp-001");
        record.setPriority("NORMAL");
        record.setTargetEnginesJson("[\"HETU\",\"HIVE\"]");
        record.setConcurrency(Integer.valueOf(16));
        record.setDurationSeconds(Integer.valueOf(300));
        record.setRampUpSeconds(Integer.valueOf(30));
        record.setDatasetSizeLabel("TEN_GB");
        record.setTemplateId("comparison-dual-engine");
        record.setTemplateType("CROSS_ENGINE_COMPARISON");
        record.setTemplateVersion("v2026.04");
        record.setTestSetId("set-route-comparison");
        record.setTestSetSource("RECOMMENDATION_GENERATION");
        record.setTestSetLabelsJson("[{\"type\":\"DOMAIN\",\"value\":\"ROUTE_GOVERNANCE\"}]");
        record.setTestSetSourceRefsJson("[{\"type\":\"RECOMMENDATION\",\"referenceId\":\"rec-001\"}]");
        record.setReadonlyRequired(Boolean.TRUE);
        record.setShadowEnvironmentMode("REQUIRED");
        record.setDesensitizationRequirement("REQUIRED");
        record.setThresholdsJson("[{\"metric\":\"QPS\"}]");
        record.setStatus("QUEUED");
        record.setCurrentPhase("SUBMITTED");
        record.setProgressPercent(Integer.valueOf(5));
        record.setReportId("report-001");
        record.setErrorCode(Integer.valueOf(14000));
        record.setErrorMessage("worker failed");
        record.setErrorSuggestedAction("retry");
        record.setErrorRetryable(Boolean.TRUE);
        record.setStatusHistoryJson("[{\"note\":\"TASK_SUBMITTED\"}]");
        record.setSubmittedAt(now);
        record.setStartedAt(now.plusSeconds(5));
        record.setFinishedAt(now.plusMinutes(1));
        record.setCreateTime(now.plusMinutes(2));
        record.setUpdateTime(now.plusMinutes(3));

        assertEquals("task-001", record.getTaskId());
        assertEquals("tenant-a", record.getTenantId());
        assertEquals("COMPARISON", record.getTaskType());
        assertEquals("SELECT * FROM orders", record.getSqlText());
        assertEquals("fp-001", record.getSqlFingerprint());
        assertEquals("NORMAL", record.getPriority());
        assertEquals("[\"HETU\",\"HIVE\"]", record.getTargetEnginesJson());
        assertEquals(Integer.valueOf(16), record.getConcurrency());
        assertEquals(Integer.valueOf(300), record.getDurationSeconds());
        assertEquals(Integer.valueOf(30), record.getRampUpSeconds());
        assertEquals("TEN_GB", record.getDatasetSizeLabel());
        assertEquals("comparison-dual-engine", record.getTemplateId());
        assertEquals("CROSS_ENGINE_COMPARISON", record.getTemplateType());
        assertEquals("v2026.04", record.getTemplateVersion());
        assertEquals("set-route-comparison", record.getTestSetId());
        assertEquals("RECOMMENDATION_GENERATION", record.getTestSetSource());
        assertEquals("[{\"type\":\"DOMAIN\",\"value\":\"ROUTE_GOVERNANCE\"}]", record.getTestSetLabelsJson());
        assertEquals("[{\"type\":\"RECOMMENDATION\",\"referenceId\":\"rec-001\"}]", record.getTestSetSourceRefsJson());
        assertEquals(Boolean.TRUE, record.getReadonlyRequired());
        assertEquals("REQUIRED", record.getShadowEnvironmentMode());
        assertEquals("REQUIRED", record.getDesensitizationRequirement());
        assertEquals("[{\"metric\":\"QPS\"}]", record.getThresholdsJson());
        assertEquals("QUEUED", record.getStatus());
        assertEquals("SUBMITTED", record.getCurrentPhase());
        assertEquals(Integer.valueOf(5), record.getProgressPercent());
        assertEquals("report-001", record.getReportId());
        assertEquals(Integer.valueOf(14000), record.getErrorCode());
        assertEquals("worker failed", record.getErrorMessage());
        assertEquals("retry", record.getErrorSuggestedAction());
        assertEquals(Boolean.TRUE, record.getErrorRetryable());
        assertEquals("[{\"note\":\"TASK_SUBMITTED\"}]", record.getStatusHistoryJson());
        assertEquals(now, record.getSubmittedAt());
        assertEquals(now.plusSeconds(5), record.getStartedAt());
        assertEquals(now.plusMinutes(1), record.getFinishedAt());
        assertEquals(now.plusMinutes(2), record.getCreateTime());
        assertEquals(now.plusMinutes(3), record.getUpdateTime());
    }

    @Test
    void shouldExposeBenchmarkReportRecordBeanContract() {
        BenchmarkReportRecord record = new BenchmarkReportRecord();
        LocalDateTime now = LocalDateTime.of(2026, 4, 22, 10, 0, 0);
        record.setReportId("report-001");
        record.setTaskId("task-001");
        record.setTenantId("tenant-a");
        record.setTaskType("REGRESSION_GUARD");
        record.setSqlFingerprint("fp-001");
        record.setGeneratedAt(now);
        record.setVerdict("FAIL");
        record.setEngineProfilesJson("[{\"engine\":\"HETU\"}]");
        record.setThresholdAssessmentsJson("[{\"metric\":\"P99_LATENCY_MS\"}]");
        record.setRecommendationsJson("[{\"category\":\"REGRESSION_GATE\"}]");
        record.setCreateTime(now.plusMinutes(1));
        record.setUpdateTime(now.plusMinutes(2));

        assertEquals("report-001", record.getReportId());
        assertEquals("task-001", record.getTaskId());
        assertEquals("tenant-a", record.getTenantId());
        assertEquals("REGRESSION_GUARD", record.getTaskType());
        assertEquals("fp-001", record.getSqlFingerprint());
        assertEquals(now, record.getGeneratedAt());
        assertEquals("FAIL", record.getVerdict());
        assertEquals("[{\"engine\":\"HETU\"}]", record.getEngineProfilesJson());
        assertEquals("[{\"metric\":\"P99_LATENCY_MS\"}]", record.getThresholdAssessmentsJson());
        assertEquals("[{\"category\":\"REGRESSION_GATE\"}]", record.getRecommendationsJson());
        assertEquals(now.plusMinutes(1), record.getCreateTime());
        assertEquals(now.plusMinutes(2), record.getUpdateTime());
    }

    @Test
    void shouldExposeBenchmarkTestSetRecordBeanContract() {
        BenchmarkTestSetRecord record = new BenchmarkTestSetRecord();
        LocalDateTime now = LocalDateTime.of(2026, 4, 22, 11, 0, 0);
        record.setTestSetId("set-001");
        record.setTenantId("tenant-a");
        record.setTestSetName("route-governance-import");
        record.setTemplateId("comparison-dual-engine");
        record.setTemplateType("CROSS_ENGINE_COMPARISON");
        record.setTemplateVersion("v2026.04");
        record.setTestSetSource("BATCH_IMPORT");
        record.setStatus("PARTIAL_READY");
        record.setTotalCases(Integer.valueOf(2));
        record.setAcceptedCases(Integer.valueOf(1));
        record.setRejectedCases(Integer.valueOf(1));
        record.setFileType("CSV");
        record.setFileName("comparison.csv");
        record.setImportBatchId("import-001");
        record.setFieldMappingsJson("[{\"field\":\"SQL_TEXT\",\"columnName\":\"sql_text\"}]");
        record.setTestSetLabelsJson("[{\"type\":\"SCENARIO\",\"value\":\"COMPARISON\"}]");
        record.setTestSetSourceRefsJson("[{\"type\":\"IMPORT_BATCH\",\"referenceId\":\"import-001\"}]");
        record.setCreatedBy("operator-001");
        record.setCreatedAt(now);
        record.setUpdatedAt(now.plusMinutes(1));
        record.setCreateTime(now.plusMinutes(2));
        record.setUpdateTime(now.plusMinutes(3));

        assertEquals("set-001", record.getTestSetId());
        assertEquals("tenant-a", record.getTenantId());
        assertEquals("route-governance-import", record.getTestSetName());
        assertEquals("comparison-dual-engine", record.getTemplateId());
        assertEquals("CROSS_ENGINE_COMPARISON", record.getTemplateType());
        assertEquals("v2026.04", record.getTemplateVersion());
        assertEquals("BATCH_IMPORT", record.getTestSetSource());
        assertEquals("PARTIAL_READY", record.getStatus());
        assertEquals(Integer.valueOf(2), record.getTotalCases());
        assertEquals(Integer.valueOf(1), record.getAcceptedCases());
        assertEquals(Integer.valueOf(1), record.getRejectedCases());
        assertEquals("CSV", record.getFileType());
        assertEquals("comparison.csv", record.getFileName());
        assertEquals("import-001", record.getImportBatchId());
        assertEquals("[{\"field\":\"SQL_TEXT\",\"columnName\":\"sql_text\"}]", record.getFieldMappingsJson());
        assertEquals("[{\"type\":\"SCENARIO\",\"value\":\"COMPARISON\"}]", record.getTestSetLabelsJson());
        assertEquals("[{\"type\":\"IMPORT_BATCH\",\"referenceId\":\"import-001\"}]", record.getTestSetSourceRefsJson());
        assertEquals("operator-001", record.getCreatedBy());
        assertEquals(now, record.getCreatedAt());
        assertEquals(now.plusMinutes(1), record.getUpdatedAt());
        assertEquals(now.plusMinutes(2), record.getCreateTime());
        assertEquals(now.plusMinutes(3), record.getUpdateTime());
    }

    @Test
    void shouldExposeBenchmarkTestSetCaseRecordBeanContract() {
        BenchmarkTestSetCaseRecord record = new BenchmarkTestSetCaseRecord();
        LocalDateTime now = LocalDateTime.of(2026, 4, 22, 12, 0, 0);
        record.setCaseId("case-001");
        record.setTestSetId("set-001");
        record.setSequenceNumber(Integer.valueOf(1));
        record.setSourceLineNumber(Integer.valueOf(2));
        record.setCaseName("primary");
        record.setSqlText("SELECT * FROM orders");
        record.setSqlFingerprint("fp-001");
        record.setDatasourceCode("ds-a");
        record.setReportCode("report-001");
        record.setTagsJson("[\"comparison\",\"route\"]");
        record.setBindParametersJson("{\"bizDate\":\"2026-04-22\"}");
        record.setStatus("ACCEPTED");
        record.setRejectionReason("none");
        record.setRawCaseDataJson("{\"sql_text\":\"SELECT * FROM orders\"}");
        record.setCreateTime(now.plusMinutes(1));
        record.setUpdateTime(now.plusMinutes(2));

        assertEquals("case-001", record.getCaseId());
        assertEquals("set-001", record.getTestSetId());
        assertEquals(Integer.valueOf(1), record.getSequenceNumber());
        assertEquals(Integer.valueOf(2), record.getSourceLineNumber());
        assertEquals("primary", record.getCaseName());
        assertEquals("SELECT * FROM orders", record.getSqlText());
        assertEquals("fp-001", record.getSqlFingerprint());
        assertEquals("ds-a", record.getDatasourceCode());
        assertEquals("report-001", record.getReportCode());
        assertEquals("[\"comparison\",\"route\"]", record.getTagsJson());
        assertEquals("{\"bizDate\":\"2026-04-22\"}", record.getBindParametersJson());
        assertEquals("ACCEPTED", record.getStatus());
        assertEquals("none", record.getRejectionReason());
        assertEquals("{\"sql_text\":\"SELECT * FROM orders\"}", record.getRawCaseDataJson());
        assertEquals(now.plusMinutes(1), record.getCreateTime());
        assertEquals(now.plusMinutes(2), record.getUpdateTime());
    }
}
