package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskContextDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.domain.benchmark.BenchmarkExecutionSummary;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifactKind;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdVerdict;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BenchmarkGovernanceTraceServiceTest {

    @Test
    void shouldBuildStructuredWorkloadAndStorageEvidenceRequest() {
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        when(governanceCapabilityClient.writeBenchmarkReportTrace(any())).thenReturn(new GovernanceBenchmarkReportTraceResponse());
        BenchmarkGovernanceTraceService service = new BenchmarkGovernanceTraceService(governanceCapabilityClient);
        BenchmarkTaskModelApplicationService modelService = new BenchmarkTaskModelApplicationService();

        BenchmarkTask task = modelService.createQueuedTask(baseRequest(), "benchmark-task-026", Instant.parse("2026-04-24T00:00:00Z"));
        task.markRunning(Instant.parse("2026-04-24T00:00:01Z"));
        task.advancePhase(com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase.WARMING_UP, 30, "WARMING_UP");
        task.advancePhase(com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase.EXECUTING, 60, "EXECUTING");
        task.advancePhase(com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase.THRESHOLD_EVALUATING, 90, "THRESHOLDS");
        task.advancePhase(com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase.REPORTING, 96, "REPORTING");
        task.markSucceeded("report-benchmark-task-026", Instant.parse("2026-04-24T00:00:05Z"));

        BenchmarkExecutionSummary executionSummary = new BenchmarkExecutionSummary(
            "QUERY_EXECUTION_WORKLOAD_ORCHESTRATED_REPLAY",
            "QUERY_EXECUTION_CAPTURED_WITH_ISOLATED_REPLAY",
            Integer.valueOf(4),
            Long.valueOf(400L),
            "qe-digest-001",
            Arrays.asList(
                "workloadSource=QUERY_EXECUTION_SYNC",
                "backfillApplied=false",
                "queryExecutionWorkloadSource=QUERY_EXECUTION_SYNC",
                "queryExecutionImplementationStage=BENCHMARK_WORKLOAD_ORCHESTRATION_BASELINE",
                "queryExecution[HETU]=QUERY_EXECUTION_SYNC,mode=CLIENT,elapsedMs=42,scannedRows=1024,cacheHit=true,"
                    + "cacheGovernanceStatus=HIT,cacheGovernanceEvidence=policyId:cache-policy-001|status:HIT|schemaVersion:schema-v1"
            )
        );
        BenchmarkReport report = new BenchmarkReport(
            "report-benchmark-task-026",
            task.getTaskId(),
            BenchmarkTaskType.BASELINE,
            "tenant-a",
            "fp-026",
            Instant.parse("2026-04-24T00:00:06Z"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            executionSummary,
            Collections.emptyList()
        );
        BenchmarkReportResponse reportResponse = new BenchmarkReportResponse(
            report.getReportId(),
            task.getTaskId(),
            BenchmarkTaskType.BASELINE,
            "fp-026",
            BenchmarkThresholdVerdict.PASS,
            report.getGeneratedAt(),
            Collections.singletonList(DataSourceTypeEnum.HETU),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "JSON",
            Arrays.asList("JSON", "PDF", "HTML"),
            "/api/benchmark-engine/reports/" + report.getReportId(),
            "/api/benchmark-engine/reports/" + report.getReportId() + "/raw-data",
            "LONG_TERM_BASELINE",
            "EXTERNALIZED_ARTIFACT_GOVERNANCE_TRACE_BASELINE"
        );
        BenchmarkReportArtifact artifact = new BenchmarkReportArtifact(
            "pdf-export",
            BenchmarkReportArtifactKind.REPORT_EXPORT,
            BenchmarkReportFormat.PDF,
            "report-026.pdf",
            "application/pdf",
            Integer.valueOf(128),
            "checksum-026",
            "ENVIRONMENT_OBJECT_STORAGE",
            "env-obj://benchmark-bucket/tenant-a/report-benchmark-task-026/report-026.pdf",
            "mode=repo-local-mirror+external-write-verified;externalWriteStatus=VERIFIED;recoveryVerificationStatus=VERIFIED",
            null,
            Integer.valueOf(180),
            "GOVERNANCE_TENANT_CONFIG_RETENTION_DAYS",
            "2026-10-21T00:00:00Z",
            null
        );

        service.registerTrace(task, report, reportResponse, null, Collections.singletonList(artifact));

        ArgumentCaptor<GovernanceBenchmarkReportTraceRequest> captor =
            ArgumentCaptor.forClass(GovernanceBenchmarkReportTraceRequest.class);
        org.mockito.Mockito.verify(governanceCapabilityClient).writeBenchmarkReportTrace(captor.capture());
        GovernanceBenchmarkReportTraceRequest request = captor.getValue();
        assertEquals("qe-digest-001", request.getWorkloadDigest());
        assertEquals("QUERY_EXECUTION_SYNC", request.getWorkloadSource());
        assertFalse(request.getBackfillApplied().booleanValue());
        Map workloadEvidence = JsonUtils.fromJson(request.getWorkloadEvidenceJson(), Map.class);
        assertEquals("QUERY_EXECUTION_WORKLOAD_ORCHESTRATED_REPLAY", workloadEvidence.get("executionMode"));
        Map queryExecution = (Map) workloadEvidence.get("queryExecution");
        assertNotNull(queryExecution);
        Map engines = (Map) queryExecution.get("engines");
        assertNotNull(engines.get("HETU"));
        assertEquals("HIT", ((Map) engines.get("HETU")).get("cacheGovernanceStatus"));
        assertEquals("VERIFIED", request.getArtifacts().get(0).getStorageEvidence().split(";")[1].split("=")[1]);
        assertEquals(Integer.valueOf(180), request.getArtifacts().get(0).getRetentionDays());
    }

    private BenchmarkTaskSubmitRequest baseRequest() {
        BenchmarkTaskSubmitRequest request = new BenchmarkTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(BenchmarkTaskType.BASELINE);
        request.setSqlText("SELECT * FROM orders");
        request.setSqlFingerprint("fp-026");
        request.setTaskContext(new BenchmarkTaskContextDTO());
        return request;
    }
}
