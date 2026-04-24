package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.domain.trace.entity.ConfigSnapshotRecord;
import com.company.governance.domain.trace.entity.ExecutionResultRecord;
import com.company.governance.domain.trace.entity.ExportRecord;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.infrastructure.persistence.mapper.ConfigSnapshotMapper;
import com.company.governance.infrastructure.persistence.mapper.ExecutionResultMapper;
import com.company.governance.infrastructure.persistence.mapper.ExportRecordMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactTraceRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceRequest;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GovernanceBenchmarkTraceabilityApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldPersistTraceabilityChainAndReturnDeterministicIds() {
        GovernanceProtectedPersistenceService protectedPersistenceService = org.mockito.Mockito.mock(GovernanceProtectedPersistenceService.class);
        ConfigSnapshotMapper configSnapshotMapper = org.mockito.Mockito.mock(ConfigSnapshotMapper.class);
        ExecutionResultMapper executionResultMapper = org.mockito.Mockito.mock(ExecutionResultMapper.class);
        QueryHistoryMapper queryHistoryMapper = org.mockito.Mockito.mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = org.mockito.Mockito.mock(ExportRecordMapper.class);
        GovernanceBenchmarkTraceabilityApplicationService service =
            new GovernanceBenchmarkTraceabilityApplicationService(
                protectedPersistenceService,
                configSnapshotMapper,
                executionResultMapper,
                queryHistoryMapper,
                exportRecordMapper
            );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        when(configSnapshotMapper.selectById("cfg-benchmark-report-001")).thenReturn(null);
        when(executionResultMapper.selectById("result-benchmark-report-001")).thenReturn(null);
        when(queryHistoryMapper.selectById("history-benchmark-report-001")).thenReturn(null);
        when(exportRecordMapper.selectById("export-benchmark-report-001-pdf-export")).thenReturn(null);
        when(exportRecordMapper.selectById("export-benchmark-report-001-raw-data")).thenReturn(null);

        GovernanceBenchmarkReportTraceRequest request = new GovernanceBenchmarkReportTraceRequest();
        request.setReportId("report-001");
        request.setTaskId("task-001");
        request.setTaskType("BASELINE");
        request.setSqlFingerprint("fp-001");
        request.setSqlText("SELECT * FROM orders");
        request.setResultStatus("SUCCEEDED");
        request.setReadonlyRequired("true");
        request.setShadowEnvironmentMode("REQUIRED");
        request.setDesensitizationRequirement("REQUIRED");
        request.setGeneratedAt("2026-04-24T10:00:00Z");
        request.setStartedAt("2026-04-24T09:59:50Z");
        request.setFinishedAt("2026-04-24T10:00:05Z");
        request.setReportQueryPath("/api/benchmark-engine/reports/report-001");
        request.setRawDataDownloadPath("/api/benchmark-engine/reports/report-001/raw-data");
        request.setExecutionSummaryJson("{\"executionMode\":\"REPO_CLOSED_ISOLATED_EXECUTOR\"}");
        request.setTargetEngines(Arrays.asList("HETU", "HIVE"));
        request.setArtifacts(Arrays.asList(
            artifact("pdf-export", "REPORT_EXPORT", "PDF", "file:/tmp/report-001.pdf"),
            artifact("raw-data", "RAW_DATA_SNAPSHOT", "JSON", "file:/tmp/report-001-raw.json")
        ));

        com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceResponse response =
            service.writeBenchmarkReportTrace(request);

        assertEquals("cfg-benchmark-report-001", response.getConfigSnapshotId());
        assertEquals("result-benchmark-report-001", response.getResultId());
        assertEquals("history-benchmark-report-001", response.getHistoryId());
        assertEquals(2, response.getArtifacts().size());
        assertEquals("export-benchmark-report-001-pdf-export", response.getArtifacts().get(0).getExportId());
        assertEquals("DATABASE_TRACE_EXPORT_ORCHESTRATION_BASELINE", response.getImplementationStage());

        ArgumentCaptor<ConfigSnapshotRecord> configCaptor = ArgumentCaptor.forClass(ConfigSnapshotRecord.class);
        verify(protectedPersistenceService).saveConfigSnapshot(configCaptor.capture());
        assertNotNull(configCaptor.getValue().getSnapshotPayload());

        ArgumentCaptor<ExecutionResultRecord> resultCaptor = ArgumentCaptor.forClass(ExecutionResultRecord.class);
        verify(protectedPersistenceService).saveExecutionResult(resultCaptor.capture());
        assertEquals("task-001", resultCaptor.getValue().getTaskId());

        ArgumentCaptor<QueryHistoryRecord> historyCaptor = ArgumentCaptor.forClass(QueryHistoryRecord.class);
        verify(protectedPersistenceService).saveQueryHistoryWithSqlText(historyCaptor.capture(), org.mockito.Mockito.eq("SELECT * FROM orders"));
        assertEquals("BENCHMARK_REPORT_EXPORT", historyCaptor.getValue().getHistoryType());

        ArgumentCaptor<ExportRecord> exportCaptor = ArgumentCaptor.forClass(ExportRecord.class);
        verify(protectedPersistenceService, times(2)).saveExportRecord(exportCaptor.capture());
        assertEquals("AVAILABLE", exportCaptor.getAllValues().get(0).getExportStatus());
    }

    @Test
    void shouldReuseExistingIdsWithoutDuplicatingRecords() {
        GovernanceProtectedPersistenceService protectedPersistenceService = org.mockito.Mockito.mock(GovernanceProtectedPersistenceService.class);
        ConfigSnapshotMapper configSnapshotMapper = org.mockito.Mockito.mock(ConfigSnapshotMapper.class);
        ExecutionResultMapper executionResultMapper = org.mockito.Mockito.mock(ExecutionResultMapper.class);
        QueryHistoryMapper queryHistoryMapper = org.mockito.Mockito.mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = org.mockito.Mockito.mock(ExportRecordMapper.class);
        GovernanceBenchmarkTraceabilityApplicationService service =
            new GovernanceBenchmarkTraceabilityApplicationService(
                protectedPersistenceService,
                configSnapshotMapper,
                executionResultMapper,
                queryHistoryMapper,
                exportRecordMapper
            );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        when(configSnapshotMapper.selectById("cfg-benchmark-report-002")).thenReturn(new ConfigSnapshotRecord());
        when(executionResultMapper.selectById("result-benchmark-report-002")).thenReturn(new ExecutionResultRecord());
        when(queryHistoryMapper.selectById("history-benchmark-report-002")).thenReturn(new QueryHistoryRecord());
        when(exportRecordMapper.selectById("export-benchmark-report-002-json-export")).thenReturn(new ExportRecord());

        GovernanceBenchmarkReportTraceRequest request = new GovernanceBenchmarkReportTraceRequest();
        request.setReportId("report-002");
        request.setTaskId("task-002");
        request.setTaskType("BASELINE");
        request.setSqlFingerprint("fp-002");
        request.setSqlText("SELECT 1");
        request.setResultStatus("SUCCEEDED");
        request.setGeneratedAt("2026-04-24T10:00:00Z");
        request.setStartedAt("2026-04-24T09:59:50Z");
        request.setFinishedAt("2026-04-24T10:00:05Z");
        request.setArtifacts(Arrays.asList(artifact("json-export", "REPORT_EXPORT", "JSON", "file:/tmp/report-002.json")));

        com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceResponse response =
            service.writeBenchmarkReportTrace(request);

        assertEquals("export-benchmark-report-002-json-export", response.getArtifacts().get(0).getExportId());
        verify(protectedPersistenceService, times(0)).saveConfigSnapshot(any(ConfigSnapshotRecord.class));
        verify(protectedPersistenceService, times(0)).saveExecutionResult(any(ExecutionResultRecord.class));
        verify(protectedPersistenceService, times(0)).saveQueryHistoryWithSqlText(any(QueryHistoryRecord.class), any(String.class));
        verify(protectedPersistenceService, times(0)).saveExportRecord(any(ExportRecord.class));
    }

    private GovernanceBenchmarkArtifactTraceRequest artifact(String key, String kind, String format, String storageUri) {
        GovernanceBenchmarkArtifactTraceRequest artifact = new GovernanceBenchmarkArtifactTraceRequest();
        artifact.setArtifactKey(key);
        artifact.setArtifactKind(kind);
        artifact.setExportFormat(format);
        artifact.setMediaType("application/json");
        artifact.setFileName(key + ".json");
        artifact.setContentLength(Integer.valueOf(128));
        artifact.setChecksumSha256("abc123");
        artifact.setStorageType("LOCAL_FILE");
        artifact.setStorageUri(storageUri);
        return artifact;
    }
}
