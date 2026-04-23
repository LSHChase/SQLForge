package com.company.benchmarkengine.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkReportRecord;
import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkTaskRecord;
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
}
