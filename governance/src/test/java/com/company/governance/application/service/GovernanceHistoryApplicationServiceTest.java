package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.governance.application.controller.vo.GovernanceTraceDetailVO;
import com.company.governance.application.controller.vo.GovernanceTraceLookupPageVO;
import com.company.governance.application.controller.vo.GovernanceTraceSummaryVO;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.governance.domain.trace.entity.ExportRecord;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.domain.trace.entity.TraceLookupHitRecord;
import com.company.governance.infrastructure.persistence.mapper.AuditLogMapper;
import com.company.governance.infrastructure.persistence.mapper.ExportRecordMapper;
import com.company.governance.infrastructure.persistence.mapper.GovernanceHistoryLookupIndexMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import com.company.sqlforge.common.exception.BizException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GovernanceHistoryApplicationServiceTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        RequestContext.clear();
    }

    @Test
    void shouldAggregateRecentTracesAcrossAuditAndTraceTables() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceHistoryApplicationService service = new GovernanceHistoryApplicationService(
            auditLogMapper,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic
        );

        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("TENANT_ADMIN", "OPERATOR"),
            "request-001",
            "trace-request-001",
            "header",
            100L,
            200L
        );
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "governance-tenant-config")).thenReturn(true);
        when(auditLogMapper.selectRecentBusinessByTenant("tenant-a", 50)).thenReturn(Arrays.asList(
            buildAudit("trace-query", "QUERY_EXECUTION", "PARTIAL", "QUERY", "fp-001",
                LocalDateTime.parse("2026-04-22T10:00:00"),
                "{\"serviceCode\":\"QUERY_EXECUTION\",\"sqlFingerprint\":\"fp-001\"}",
                "{\"resultStatus\":\"PARTIAL\",\"targetEngine\":\"HIVE\",\"degraded\":true,\"errorCode\":\"12000\"}"),
            buildAudit("trace-opt", "SQL_OPTIMIZATION", "FAILED", "TASK", "task-001",
                LocalDateTime.parse("2026-04-22T09:59:00"),
                "{\"serviceCode\":\"SQL_OPTIMIZATION\",\"taskId\":\"task-001\"}",
                "{\"resultStatus\":\"FAILED\",\"taskId\":\"task-001\",\"errorCode\":\"13000\"}")
        ));
        when(queryHistoryMapper.selectRecentByTenant("tenant-a", 50)).thenReturn(Collections.singletonList(
            buildHistory("trace-query", "history-001", "QUERY_EXECUTION", "fp-001", LocalDateTime.parse("2026-04-22T09:58:00"))
        ));
        when(exportRecordMapper.selectRecentByTenant("tenant-a", 50)).thenReturn(Collections.singletonList(
            buildExport("trace-opt", "export-001", "FAILED", LocalDateTime.parse("2026-04-22T09:57:00"))
        ));

        List<GovernanceTraceSummaryVO> traces = service.findRecentTraces("tenant-a", Integer.valueOf(10));

        assertEquals(2, traces.size());
        assertEquals("trace-query", traces.get(0).getTraceId());
        assertEquals("PARTIAL", traces.get(0).getLatestStatus());
        assertEquals(Integer.valueOf(1), traces.get(0).getQueryHistoryCount());
        assertEquals(Boolean.TRUE, traces.get(0).getDegraded());
        assertEquals("trace-opt", traces.get(1).getTraceId());
        assertEquals(Integer.valueOf(1), traces.get(1).getExportRecordCount());
        assertEquals("task-001", traces.get(1).getTaskId());
    }

    @Test
    void shouldReturnTraceDetailWithAuditEventsAndLinkedRecords() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceHistoryApplicationService service = new GovernanceHistoryApplicationService(
            auditLogMapper,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic
        );

        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("OPERATOR"),
            "request-001",
            "trace-request-001",
            "header",
            100L,
            200L
        );
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "governance-tenant-config")).thenReturn(true);
        when(auditLogMapper.selectByTraceId("tenant-a", "trace-query", 5)).thenReturn(Collections.singletonList(
            buildAudit("trace-query", "QUERY_EXECUTION", "PARTIAL", "QUERY", "fp-001",
                LocalDateTime.parse("2026-04-22T10:00:00"),
                "{\"serviceCode\":\"QUERY_EXECUTION\",\"sqlFingerprint\":\"fp-001\"}",
                "{\"resultStatus\":\"PARTIAL\",\"targetEngine\":\"HIVE\",\"degraded\":true,\"errorCode\":\"12000\"}")
        ));
        when(queryHistoryMapper.selectByTraceId("tenant-a", "trace-query", 5)).thenReturn(Collections.singletonList(
            buildHistory("trace-query", "history-001", "QUERY_EXECUTION", "fp-001", LocalDateTime.parse("2026-04-22T09:58:00"))
        ));
        when(exportRecordMapper.selectByTraceId("tenant-a", "trace-query", 5)).thenReturn(Collections.emptyList());

        GovernanceTraceDetailVO detail = service.findTraceDetail("tenant-a", "trace-query", Integer.valueOf(5));

        assertEquals("trace-query", detail.getTraceId());
        assertEquals("PARTIAL", detail.getLatestStatus());
        assertEquals(Integer.valueOf(1), detail.getAuditEventCount());
        assertEquals(Integer.valueOf(1), detail.getQueryHistoryCount());
        assertEquals("fp-001", detail.getSqlFingerprint());
        assertEquals("HIVE", detail.getTargetEngine());
        assertEquals(Boolean.TRUE, detail.getDegraded());
        assertEquals(1, detail.getAuditEvents().size());
        assertEquals(1, detail.getQueryHistories().size());
    }

    @Test
    void shouldExposeCompensationReplayAndArtifactRecoverySurfacesInTraceDetail() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceHistoryApplicationService service = new GovernanceHistoryApplicationService(
            auditLogMapper,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic
        );

        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("OPERATOR"),
            "request-001",
            "trace-request-001",
            "header",
            100L,
            200L
        );
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "governance-tenant-config")).thenReturn(true);

        AuditLogRecord audit = buildAudit(
            "trace-benchmark",
            "BENCHMARK_ENGINE",
            "SUCCESS",
            "REPORT",
            "report-001",
            LocalDateTime.parse("2026-04-22T10:00:00"),
            "{\"serviceCode\":\"BENCHMARK_ENGINE\",\"reportId\":\"report-001\"}",
            "{\"resultStatus\":\"SUCCESS\",\"reportId\":\"report-001\",\"artifactStorageType\":\"ENVIRONMENT_OBJECT_STORAGE\","
                + "\"artifactStorageEvidence\":\"providerMode=PRIMARY_PLUS_RECOVERY_PROVIDER;primaryProvider=PRIMARY_HTTP;"
                + "primaryProviderContract=HTTP_PUT_GET_DELETE;recoveryProvider=RECOVERY_HTTP;"
                + "recoveryProviderContract=HTTP_PUT_GET_DELETE;recoveryOrder=REPO_LOCAL_MIRROR,PRIMARY_PROVIDER,RECOVERY_PROVIDER,REPORT_SNAPSHOT;"
                + "cleanupScope=MIRROR_LIVE_EVIDENCE_EXTERNAL_WRITE_PROVIDER;providerWriteStatus=VERIFIED;providerRecoveryStatus=VERIFIED;"
                + "recoveryProviderWriteStatus=VERIFIED;recoveryProviderRecoveryStatus=VERIFIED;providerObjectUrl=http://primary/object;"
                + "recoveryProviderObjectUrl=http://recovery/object\",\"artifactRecoveryStatus\":\"STORED\","
                + "\"artifactStorageRecoverySource\":\"RECOVERY_PROVIDER\",\"artifactStorageReadStatus\":\"RECOVERED_FROM_RECOVERY_PROVIDER\"}"
        );
        QueryHistoryRecord history = buildHistory(
            "trace-benchmark",
            "history-001",
            "BENCHMARK_ENGINE",
            "fp-001",
            LocalDateTime.parse("2026-04-22T09:58:00")
        );
        history.setQueryContext(
            "{\"reportId\":\"report-001\",\"workloadSource\":\"COMPENSATED_REPLAY\",\"backfillApplied\":true,"
                + "\"workloadEvidence\":{\"executionMode\":\"QUERY_EXECUTION_WORKLOAD_ORCHESTRATED_REPLAY\","
                + "\"queryExecution\":{\"implementationStage\":\"BENCHMARK_WORKLOAD_ORCHESTRATION_BASELINE\","
                + "\"engines\":{\"HETU\":{\"compensationApplied\":true,\"compensationStrategy\":\"LATEST_SUCCESS_REPLAY\","
                + "\"compensationSourceEngine\":\"HIVE\",\"compensationSourceWorkloadDigest\":\"digest-001\"}}}}}"
        );
        ExportRecord export = buildExport(
            "trace-benchmark",
            "export-001",
            "SUCCESS",
            LocalDateTime.parse("2026-04-22T09:59:00")
        );
        export.setExportOptions(
            "{\"reportId\":\"report-001\",\"storageType\":\"ENVIRONMENT_OBJECT_STORAGE\","
                + "\"storageEvidence\":{\"providerMode\":\"PRIMARY_PLUS_RECOVERY_PROVIDER\",\"primaryProvider\":\"PRIMARY_HTTP\","
                + "\"primaryProviderContract\":\"HTTP_PUT_GET_DELETE\",\"recoveryProvider\":\"RECOVERY_HTTP\","
                + "\"recoveryProviderContract\":\"HTTP_PUT_GET_DELETE\",\"recoveryOrder\":\"REPO_LOCAL_MIRROR,PRIMARY_PROVIDER,RECOVERY_PROVIDER,REPORT_SNAPSHOT\","
                + "\"cleanupScope\":\"MIRROR_LIVE_EVIDENCE_EXTERNAL_WRITE_PROVIDER\",\"providerWriteStatus\":\"VERIFIED\","
                + "\"providerRecoveryStatus\":\"VERIFIED\",\"recoveryProviderWriteStatus\":\"VERIFIED\","
                + "\"recoveryProviderRecoveryStatus\":\"VERIFIED\"}}"
        );

        when(auditLogMapper.selectByTraceId("tenant-a", "trace-benchmark", 5)).thenReturn(Collections.singletonList(audit));
        when(queryHistoryMapper.selectByTraceId("tenant-a", "trace-benchmark", 5)).thenReturn(Collections.singletonList(history));
        when(exportRecordMapper.selectByTraceId("tenant-a", "trace-benchmark", 5)).thenReturn(Collections.singletonList(export));

        GovernanceTraceDetailVO detail = service.findTraceDetail("tenant-a", "trace-benchmark", Integer.valueOf(5));

        assertEquals("PRIMARY_PLUS_RECOVERY_PROVIDER", detail.getArtifactStorageContract().get("providerMode"));
        assertEquals("RECOVERY_PROVIDER", detail.getArtifactRecoverySurface().get("storageRecoverySource"));
        assertEquals("RECOVERED_FROM_RECOVERY_PROVIDER", detail.getArtifactRecoverySurface().get("storageReadStatus"));
        assertEquals(Boolean.TRUE, detail.getCompensationReplayEvidence().get("compensationApplied"));
        assertEquals("LATEST_SUCCESS_REPLAY", detail.getCompensationReplayEvidence().get("compensationStrategy"));
        assertEquals("HIVE", detail.getCompensationReplayEvidence().get("compensationSourceEngine"));
        assertEquals("digest-001", detail.getCompensationReplayEvidence().get("compensationSourceWorkloadDigest"));
        assertEquals("PRIMARY_PLUS_RECOVERY_PROVIDER", detail.getExportRecords().get(0).getExportOptions().get("storageEvidence") instanceof Map
            ? ((Map) detail.getExportRecords().get(0).getExportOptions().get("storageEvidence")).get("providerMode")
            : null);
        assertEquals("COMPENSATED_REPLAY", detail.getQueryHistories().get(0).getQueryContext().get("workloadSource"));
    }

    @Test
    void shouldExposeArtifactOperationSurfaceInTraceDetail() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceHistoryApplicationService service = new GovernanceHistoryApplicationService(
            auditLogMapper,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic
        );

        RequestContext.set(
            "tenant-a",
            "tenant-admin-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-request-001",
            "header",
            100L,
            200L
        );
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "governance-tenant-config")).thenReturn(true);
        when(auditLogMapper.selectByTraceId("tenant-a", "trace-artifact-op", 5)).thenReturn(Collections.singletonList(
            buildAudit(
                "trace-artifact-op",
                "BENCHMARK_ENGINE",
                "SUCCESS",
                "BENCHMARK_ENGINE_REPORT",
                "report-001",
                LocalDateTime.parse("2026-04-24T10:00:00"),
                "{\"serviceCode\":\"BENCHMARK_ENGINE\",\"reportId\":\"report-001\",\"artifactKey\":\"json-export\"}",
                "{\"resultStatus\":\"SUCCESS\",\"reportId\":\"report-001\",\"artifactStorageType\":\"ENVIRONMENT_OBJECT_STORAGE\","
                    + "\"artifactStorageEvidence\":\"providerMode=PRIMARY_PROVIDER_ONLY;primaryProvider=PRIMARY_HTTP;providerHeadStatus=VERIFIED;providerRequestId=request-primary;liveEvidenceStatus=PROVIDER_LIVE_EVIDENCE_VERIFIED\","
                    + "\"artifactOperationSurface\":{\"operationType\":\"RECOVER_ARTIFACT\",\"operationStatus\":\"RECOVERY_COMPLETED\","
                    + "\"storageRecoverySource\":\"EXTERNAL_WRITE\",\"storageReadStatus\":\"RECOVERED_FROM_EXTERNAL_WRITE\","
                    + "\"cleanupScope\":\"MIRROR_ONLY\",\"providerHeadStatus\":\"VERIFIED\",\"providerRequestId\":\"request-primary\"}}"
            )
        ));
        when(queryHistoryMapper.selectByTraceId("tenant-a", "trace-artifact-op", 5)).thenReturn(Collections.emptyList());
        when(exportRecordMapper.selectByTraceId("tenant-a", "trace-artifact-op", 5)).thenReturn(Collections.emptyList());

        GovernanceTraceDetailVO detail = service.findTraceDetail("tenant-a", "trace-artifact-op", Integer.valueOf(5));

        assertEquals("RECOVER_ARTIFACT", detail.getArtifactOperationSurface().get("operationType"));
        assertEquals("RECOVERY_COMPLETED", detail.getArtifactOperationSurface().get("operationStatus"));
        assertEquals("EXTERNAL_WRITE", detail.getArtifactOperationSurface().get("storageRecoverySource"));
        assertEquals("VERIFIED", detail.getArtifactOperationSurface().get("providerHeadStatus"));
        assertEquals("request-primary", detail.getArtifactOperationSurface().get("providerRequestId"));
    }

    @Test
    void shouldScanPastGovernanceAuthNoiseWhenListingRecentTraces() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceHistoryApplicationService service = new GovernanceHistoryApplicationService(
            auditLogMapper,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic
        );

        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("TENANT_ADMIN", "OPERATOR"),
            "request-001",
            "trace-request-001",
            "header",
            100L,
            200L
        );
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "governance-tenant-config")).thenReturn(true);

        List<AuditLogRecord> recentAudits = new ArrayList<AuditLogRecord>();
        recentAudits.add(buildAudit("trace-auth-1", "GOVERNANCE", "SUCCESS", "AUTH_REQUEST", "/api/governance/history/traces",
            LocalDateTime.parse("2026-04-22T10:06:00"),
            "{\"serviceCode\":\"GOVERNANCE\"}",
            "{\"resultStatus\":\"SUCCESS\"}"));
        recentAudits.add(buildAudit("trace-auth-2", "GOVERNANCE", "SUCCESS", "AUTH_REQUEST", "/api/governance/history/traces",
            LocalDateTime.parse("2026-04-22T10:05:00"),
            "{\"serviceCode\":\"GOVERNANCE\"}",
            "{\"resultStatus\":\"SUCCESS\"}"));
        recentAudits.add(buildAudit("trace-auth-3", "GOVERNANCE", "FAILED", "AUTH_REQUEST", "/api/governance/history/traces",
            LocalDateTime.parse("2026-04-22T10:04:00"),
            "{\"serviceCode\":\"GOVERNANCE\"}",
            "{\"resultStatus\":\"FAILED\"}"));
        recentAudits.add(buildAudit("trace-auth-4", "GOVERNANCE", "SUCCESS", "AUTH_REQUEST", "/api/governance/history/traces",
            LocalDateTime.parse("2026-04-22T10:03:00"),
            "{\"serviceCode\":\"GOVERNANCE\"}",
            "{\"resultStatus\":\"SUCCESS\"}"));
        recentAudits.add(buildAudit("trace-auth-5", "GOVERNANCE", "SUCCESS", "AUTH_REQUEST", "/api/governance/history/traces",
            LocalDateTime.parse("2026-04-22T10:02:00"),
            "{\"serviceCode\":\"GOVERNANCE\"}",
            "{\"resultStatus\":\"SUCCESS\"}"));
        recentAudits.add(buildAudit("trace-query", "QUERY_EXECUTION", "PARTIAL", "QUERY", "fp-001",
            LocalDateTime.parse("2026-04-22T10:01:00"),
            "{\"serviceCode\":\"QUERY_EXECUTION\",\"sqlFingerprint\":\"fp-001\"}",
            "{\"resultStatus\":\"PARTIAL\",\"targetEngine\":\"HIVE\",\"degraded\":true,\"errorCode\":\"12000\"}"));
        recentAudits.add(buildAudit("trace-opt", "SQL_OPTIMIZATION", "FAILED", "TASK", "task-001",
            LocalDateTime.parse("2026-04-22T10:00:00"),
            "{\"serviceCode\":\"SQL_OPTIMIZATION\",\"taskId\":\"task-001\"}",
            "{\"resultStatus\":\"FAILED\",\"taskId\":\"task-001\",\"errorCode\":\"13000\"}"));

        when(auditLogMapper.selectRecentBusinessByTenant("tenant-a", 10)).thenReturn(recentAudits);
        when(queryHistoryMapper.selectRecentByTenant("tenant-a", 10)).thenReturn(Collections.emptyList());
        when(exportRecordMapper.selectRecentByTenant("tenant-a", 10)).thenReturn(Collections.emptyList());

        List<GovernanceTraceSummaryVO> traces = service.findRecentTraces("tenant-a", Integer.valueOf(2));

        assertEquals(2, traces.size());
        assertEquals("trace-query", traces.get(0).getTraceId());
        assertEquals("trace-opt", traces.get(1).getTraceId());
    }

    @Test
    void shouldCollapseRecentTracesByBusinessResource() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceHistoryApplicationService service = new GovernanceHistoryApplicationService(
            auditLogMapper,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic
        );

        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("TENANT_ADMIN", "OPERATOR"),
            "request-001",
            "trace-request-001",
            "header",
            100L,
            200L
        );
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "governance-tenant-config")).thenReturn(true);

        when(auditLogMapper.selectRecentBusinessByTenant("tenant-a", 15)).thenReturn(Arrays.asList(
            buildAudit("trace-benchmark-failed", "BENCHMARK_ENGINE", "FAILED", "BENCHMARK_ENGINE_TASK", "task-benchmark-failed",
                LocalDateTime.parse("2026-04-22T10:05:00"),
                "{\"serviceCode\":\"BENCHMARK_ENGINE\",\"taskId\":\"task-benchmark-failed\"}",
                "{\"resultStatus\":\"FAILED\",\"taskId\":\"task-benchmark-failed\",\"errorCode\":\"14000\"}"),
            buildAudit("trace-benchmark-running", "BENCHMARK_ENGINE", "RUNNING", "BENCHMARK_ENGINE_TASK", "task-benchmark-failed",
                LocalDateTime.parse("2026-04-22T10:04:00"),
                "{\"serviceCode\":\"BENCHMARK_ENGINE\",\"taskId\":\"task-benchmark-failed\"}",
                "{\"resultStatus\":\"RUNNING\",\"taskId\":\"task-benchmark-failed\"}"),
            buildAudit("trace-opt-failed", "SQL_OPTIMIZATION", "FAILED", "SQL_OPTIMIZATION_TASK", "task-optimization-failed",
                LocalDateTime.parse("2026-04-22T10:03:00"),
                "{\"serviceCode\":\"SQL_OPTIMIZATION\",\"taskId\":\"task-optimization-failed\"}",
                "{\"resultStatus\":\"FAILED\",\"taskId\":\"task-optimization-failed\",\"errorCode\":\"13000\"}"),
            buildAudit("trace-opt-queued", "SQL_OPTIMIZATION", "QUEUED", "SQL_OPTIMIZATION_TASK", "task-optimization-failed",
                LocalDateTime.parse("2026-04-22T10:02:00"),
                "{\"serviceCode\":\"SQL_OPTIMIZATION\",\"taskId\":\"task-optimization-failed\"}",
                "{\"resultStatus\":\"QUEUED\",\"taskId\":\"task-optimization-failed\"}"),
            buildAudit("trace-query-partial", "QUERY_EXECUTION", "PARTIAL", "QUERY", "fingerprint-001",
                LocalDateTime.parse("2026-04-22T10:01:00"),
                "{\"serviceCode\":\"QUERY_EXECUTION\",\"sqlFingerprint\":\"fingerprint-001\"}",
                "{\"resultStatus\":\"PARTIAL\",\"targetEngine\":\"HIVE\",\"degraded\":true,\"errorCode\":\"12000\"}"),
            buildAudit("trace-query-success", "QUERY_EXECUTION", "SUCCESS", "QUERY", "fingerprint-001",
                LocalDateTime.parse("2026-04-22T10:00:00"),
                "{\"serviceCode\":\"QUERY_EXECUTION\",\"sqlFingerprint\":\"fingerprint-001\"}",
                "{\"resultStatus\":\"SUCCESS\",\"targetEngine\":\"HETU\",\"degraded\":false}")
        ));
        when(queryHistoryMapper.selectRecentByTenant("tenant-a", 15)).thenReturn(Collections.emptyList());
        when(exportRecordMapper.selectRecentByTenant("tenant-a", 15)).thenReturn(Collections.emptyList());

        List<GovernanceTraceSummaryVO> traces = service.findRecentTraces("tenant-a", Integer.valueOf(3));

        assertEquals(3, traces.size());
        assertEquals("trace-benchmark-failed", traces.get(0).getTraceId());
        assertEquals("trace-opt-failed", traces.get(1).getTraceId());
        assertEquals("trace-query-partial", traces.get(2).getTraceId());
    }

    @Test
    void shouldLookupTracesByTaskAndReportWithoutCollapsingCompensationEvidence() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceHistoryApplicationService service = new GovernanceHistoryApplicationService(
            auditLogMapper,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic
        );

        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("TENANT_ADMIN", "OPERATOR"),
            "request-001",
            "trace-request-001",
            "header",
            100L,
            200L
        );
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "governance-tenant-config")).thenReturn(true);
        when(auditLogMapper.selectTraceHitsByTargetId(
            "tenant-a",
            "task-001",
            Arrays.asList("TASK", "SQL_OPTIMIZATION_TASK", "BENCHMARK_ENGINE_TASK"),
            null,
            null,
            11
        )).thenReturn(Arrays.asList(
            buildTraceHit("SMOKE-FORCE-AUDIT-FALLBACK-task-001", "2026-04-22T10:06:00", 1002L),
            buildTraceHit("trace-opt-failed", "2026-04-22T10:05:00", 1001L)
        ));
        when(auditLogMapper.selectTraceHitsByTargetId(
            "tenant-a",
            "report-001",
            Arrays.asList("REPORT", "BENCHMARK_ENGINE_REPORT"),
            null,
            null,
            11
        )).thenReturn(Collections.singletonList(
            buildTraceHit("trace-benchmark-report", "2026-04-22T10:04:00", 1000L)
        ));
        when(auditLogMapper.selectByTraceIds("tenant-a", Arrays.asList(
            "SMOKE-FORCE-AUDIT-FALLBACK-task-001",
            "trace-opt-failed"
        ))).thenReturn(Arrays.asList(
            buildAudit("SMOKE-FORCE-AUDIT-FALLBACK-task-001", "SQL_OPTIMIZATION", "FAILED",
                "SQL_OPTIMIZATION_TASK", "task-001",
                LocalDateTime.parse("2026-04-22T10:06:00"),
                "{\"serviceCode\":\"SQL_OPTIMIZATION\",\"taskId\":\"task-001\"}",
                "{\"resultStatus\":\"FAILED\",\"taskId\":\"task-001\",\"currentPhase\":\"COMPENSATED\",\"errorCode\":\"13000\"}"),
            buildAudit("trace-opt-failed", "SQL_OPTIMIZATION", "FAILED",
                "SQL_OPTIMIZATION_TASK", "task-001",
                LocalDateTime.parse("2026-04-22T10:05:00"),
                "{\"serviceCode\":\"SQL_OPTIMIZATION\",\"taskId\":\"task-001\"}",
                "{\"resultStatus\":\"FAILED\",\"taskId\":\"task-001\",\"errorCode\":\"13000\"}")
        ));
        when(auditLogMapper.selectByTraceIds("tenant-a", Collections.singletonList("trace-benchmark-report"))).thenReturn(
            Collections.singletonList(
                buildAudit("trace-benchmark-report", "BENCHMARK_ENGINE", "SUCCESS",
                    "REPORT", "report-001",
                    LocalDateTime.parse("2026-04-22T10:04:00"),
                    "{\"serviceCode\":\"BENCHMARK_ENGINE\",\"reportId\":\"report-001\"}",
                    "{\"resultStatus\":\"SUCCESS\",\"reportId\":\"report-001\"}")
            )
        );
        when(queryHistoryMapper.selectByTraceIds("tenant-a", Arrays.asList(
            "SMOKE-FORCE-AUDIT-FALLBACK-task-001",
            "trace-opt-failed"
        ))).thenReturn(Collections.emptyList());
        when(queryHistoryMapper.selectByTraceIds("tenant-a", Collections.singletonList("trace-benchmark-report")))
            .thenReturn(Collections.emptyList());
        when(exportRecordMapper.selectByTraceIds("tenant-a", Arrays.asList(
            "SMOKE-FORCE-AUDIT-FALLBACK-task-001",
            "trace-opt-failed"
        ))).thenReturn(Collections.emptyList());
        when(exportRecordMapper.selectByTraceIds("tenant-a", Collections.singletonList("trace-benchmark-report"))).thenReturn(Collections.singletonList(
            buildExport("trace-benchmark-report", "report-001", "SUCCESS", LocalDateTime.parse("2026-04-22T10:03:00"))
        ));

        GovernanceTraceLookupPageVO taskMatches = service.lookupTraces(
            "tenant-a",
            null,
            "task-001",
            null,
            null,
            null,
            null,
            Integer.valueOf(10)
        );
        GovernanceTraceLookupPageVO reportMatches = service.lookupTraces(
            "tenant-a",
            null,
            null,
            "report-001",
            null,
            null,
            null,
            Integer.valueOf(10)
        );

        assertEquals(2, taskMatches.getItems().size());
        assertEquals("SMOKE-FORCE-AUDIT-FALLBACK-task-001", taskMatches.getItems().get(0).getTraceId());
        assertEquals("trace-opt-failed", taskMatches.getItems().get(1).getTraceId());
        assertEquals(Boolean.FALSE, taskMatches.getHasMore());
        assertEquals(1, reportMatches.getItems().size());
        assertEquals("trace-benchmark-report", reportMatches.getItems().get(0).getTraceId());
        assertEquals("report-001", reportMatches.getItems().get(0).getReportId());
    }

    @Test
    void shouldPageIndexedTaskLookupAcrossOlderTraceHits() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceHistoryApplicationService service = new GovernanceHistoryApplicationService(
            auditLogMapper,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic
        );

        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("TENANT_ADMIN", "OPERATOR"),
            "request-001",
            "trace-request-001",
            "header",
            100L,
            200L
        );
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "governance-tenant-config")).thenReturn(true);
        when(auditLogMapper.selectTraceHitsByTargetId(
            "tenant-a",
            "task-001",
            Arrays.asList("TASK", "SQL_OPTIMIZATION_TASK", "BENCHMARK_ENGINE_TASK"),
            null,
            null,
            2
        )).thenReturn(Arrays.asList(
            buildTraceHit("trace-newest", "2026-04-22T10:06:00", 1002L),
            buildTraceHit("trace-older", "2026-04-22T10:05:00", 1001L)
        ));
        when(auditLogMapper.selectTraceHitsByTargetId(
            "tenant-a",
            "task-001",
            Arrays.asList("TASK", "SQL_OPTIMIZATION_TASK", "BENCHMARK_ENGINE_TASK"),
            LocalDateTime.parse("2026-04-22T10:06:00"),
            Long.valueOf(1002L),
            2
        )).thenReturn(Arrays.asList(
            buildTraceHit("trace-older", "2026-04-22T10:05:00", 1001L),
            buildTraceHit("trace-oldest", "2026-04-22T10:04:00", 1000L)
        ));
        when(auditLogMapper.selectTraceHitsByTargetId(
            "tenant-a",
            "task-001",
            Arrays.asList("TASK", "SQL_OPTIMIZATION_TASK", "BENCHMARK_ENGINE_TASK"),
            LocalDateTime.parse("2026-04-22T10:05:00"),
            Long.valueOf(1001L),
            2
        )).thenReturn(Collections.singletonList(
            buildTraceHit("trace-oldest", "2026-04-22T10:04:00", 1000L)
        ));
        when(auditLogMapper.selectByTraceIds("tenant-a", Arrays.asList("trace-newest", "trace-older"))).thenReturn(Arrays.asList(
            buildAudit("trace-newest", "SQL_OPTIMIZATION", "FAILED", "SQL_OPTIMIZATION_TASK", "task-001",
                LocalDateTime.parse("2026-04-22T10:06:00"),
                "{\"serviceCode\":\"SQL_OPTIMIZATION\",\"taskId\":\"task-001\"}",
                "{\"resultStatus\":\"FAILED\",\"taskId\":\"task-001\",\"errorCode\":\"13000\"}"),
            buildAudit("trace-older", "SQL_OPTIMIZATION", "FAILED", "SQL_OPTIMIZATION_TASK", "task-001",
                LocalDateTime.parse("2026-04-22T10:05:00"),
                "{\"serviceCode\":\"SQL_OPTIMIZATION\",\"taskId\":\"task-001\"}",
                "{\"resultStatus\":\"FAILED\",\"taskId\":\"task-001\",\"errorCode\":\"13000\"}")
        ));
        when(auditLogMapper.selectByTraceIds("tenant-a", Arrays.asList("trace-older", "trace-oldest"))).thenReturn(Arrays.asList(
            buildAudit("trace-older", "SQL_OPTIMIZATION", "FAILED", "SQL_OPTIMIZATION_TASK", "task-001",
                LocalDateTime.parse("2026-04-22T10:05:00"),
                "{\"serviceCode\":\"SQL_OPTIMIZATION\",\"taskId\":\"task-001\"}",
                "{\"resultStatus\":\"FAILED\",\"taskId\":\"task-001\",\"errorCode\":\"13000\"}"),
            buildAudit("trace-oldest", "SQL_OPTIMIZATION", "FAILED", "SQL_OPTIMIZATION_TASK", "task-001",
                LocalDateTime.parse("2026-04-22T10:04:00"),
                "{\"serviceCode\":\"SQL_OPTIMIZATION\",\"taskId\":\"task-001\"}",
                "{\"resultStatus\":\"FAILED\",\"taskId\":\"task-001\",\"errorCode\":\"13000\"}")
        ));
        when(auditLogMapper.selectByTraceIds("tenant-a", Collections.singletonList("trace-oldest"))).thenReturn(Collections.singletonList(
            buildAudit("trace-oldest", "SQL_OPTIMIZATION", "FAILED", "SQL_OPTIMIZATION_TASK", "task-001",
                LocalDateTime.parse("2026-04-22T10:04:00"),
                "{\"serviceCode\":\"SQL_OPTIMIZATION\",\"taskId\":\"task-001\"}",
                "{\"resultStatus\":\"FAILED\",\"taskId\":\"task-001\",\"errorCode\":\"13000\"}")
        ));
        when(queryHistoryMapper.selectByTraceIds("tenant-a", Arrays.asList("trace-newest", "trace-older")))
            .thenReturn(Collections.emptyList());
        when(queryHistoryMapper.selectByTraceIds("tenant-a", Arrays.asList("trace-older", "trace-oldest")))
            .thenReturn(Collections.emptyList());
        when(queryHistoryMapper.selectByTraceIds("tenant-a", Collections.singletonList("trace-oldest")))
            .thenReturn(Collections.emptyList());
        when(exportRecordMapper.selectByTraceIds("tenant-a", Arrays.asList("trace-newest", "trace-older")))
            .thenReturn(Collections.emptyList());
        when(exportRecordMapper.selectByTraceIds("tenant-a", Arrays.asList("trace-older", "trace-oldest")))
            .thenReturn(Collections.emptyList());
        when(exportRecordMapper.selectByTraceIds("tenant-a", Collections.singletonList("trace-oldest")))
            .thenReturn(Collections.emptyList());

        GovernanceTraceLookupPageVO firstPage = service.lookupTraces(
            "tenant-a",
            null,
            "task-001",
            null,
            null,
            null,
            null,
            Integer.valueOf(1)
        );
        GovernanceTraceLookupPageVO secondPage = service.lookupTraces(
            "tenant-a",
            null,
            "task-001",
            null,
            null,
            null,
            firstPage.getNextCursor(),
            Integer.valueOf(1)
        );
        GovernanceTraceLookupPageVO thirdPage = service.lookupTraces(
            "tenant-a",
            null,
            "task-001",
            null,
            null,
            null,
            secondPage.getNextCursor(),
            Integer.valueOf(1)
        );

        assertEquals(1, firstPage.getItems().size());
        assertEquals("trace-newest", firstPage.getItems().get(0).getTraceId());
        assertEquals(Boolean.TRUE, firstPage.getHasMore());
        assertEquals(1, secondPage.getItems().size());
        assertEquals("trace-older", secondPage.getItems().get(0).getTraceId());
        assertEquals(Boolean.TRUE, secondPage.getHasMore());
        assertEquals(1, thirdPage.getItems().size());
        assertEquals("trace-oldest", thirdPage.getItems().get(0).getTraceId());
        assertEquals(Boolean.FALSE, thirdPage.getHasMore());
    }

    @Test
    void shouldLookupIndexedHistoryWithinExplicitWindow() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        GovernanceHistoryLookupIndexMapper governanceHistoryLookupIndexMapper = mock(GovernanceHistoryLookupIndexMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceHistoryApplicationService service = new GovernanceHistoryApplicationService(
            auditLogMapper,
            governanceHistoryLookupIndexMapper,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic
        );

        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("TENANT_ADMIN", "OPERATOR"),
            "request-001",
            "trace-request-001",
            "header",
            100L,
            200L
        );
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "governance-tenant-config")).thenReturn(true);
        when(governanceHistoryLookupIndexMapper.selectTraceHitsByLookupId(
            "tenant-a",
            "TASK",
            "task-001",
            LocalDateTime.parse("2026-04-01T00:00:00"),
            LocalDateTime.parse("2026-04-30T23:59:59"),
            null,
            null,
            2
        )).thenReturn(Collections.singletonList(
            buildTraceHit("trace-windowed", "2026-04-22T10:06:00", 1002L)
        ));
        when(auditLogMapper.selectByTraceIds("tenant-a", Collections.singletonList("trace-windowed"))).thenReturn(Collections.singletonList(
            buildAudit("trace-windowed", "SQL_OPTIMIZATION", "FAILED", "SQL_OPTIMIZATION_TASK", "task-001",
                LocalDateTime.parse("2026-04-22T10:06:00"),
                "{\"serviceCode\":\"SQL_OPTIMIZATION\",\"taskId\":\"task-001\"}",
                "{\"resultStatus\":\"FAILED\",\"taskId\":\"task-001\",\"errorCode\":\"13000\"}")
        ));
        when(queryHistoryMapper.selectByTraceIds("tenant-a", Collections.singletonList("trace-windowed")))
            .thenReturn(Collections.emptyList());
        when(exportRecordMapper.selectByTraceIds("tenant-a", Collections.singletonList("trace-windowed")))
            .thenReturn(Collections.emptyList());

        GovernanceTraceLookupPageVO page = service.lookupTraces(
            "tenant-a",
            null,
            "task-001",
            null,
            "2026-04-01T00:00:00",
            "2026-04-30T23:59:59",
            null,
            Integer.valueOf(1)
        );

        assertEquals(1, page.getItems().size());
        assertEquals("trace-windowed", page.getItems().get(0).getTraceId());
        assertEquals(Boolean.FALSE, page.getHasMore());
    }

    @Test
    void shouldRejectLookupWithoutTraceTaskOrReportId() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceHistoryApplicationService service = new GovernanceHistoryApplicationService(
            auditLogMapper,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic
        );

        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("TENANT_ADMIN", "OPERATOR"),
            "request-001",
            "trace-request-001",
            "header",
            100L,
            200L
        );
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "governance-tenant-config")).thenReturn(true);

        BizException exception = assertThrows(
            BizException.class,
            () -> service.lookupTraces("tenant-a", null, null, null, null, null, null, Integer.valueOf(10))
        );

        assertEquals(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, exception.getCode());
    }

    @Test
    void shouldRejectCrossTenantHistoryReadWithoutPlatformAdmin() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceHistoryApplicationService service = new GovernanceHistoryApplicationService(
            auditLogMapper,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic
        );

        RequestContext.set(
            "tenant-a",
            "tenant-admin-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-request-001",
            "header",
            100L,
            200L
        );

        BizException exception = assertThrows(
            BizException.class,
            () -> service.findRecentTraces("tenant-b", Integer.valueOf(5))
        );

        assertEquals(ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED, exception.getCode());
    }

    private AuditLogRecord buildAudit(String traceId,
                                      String serviceCode,
                                      String status,
                                      String targetType,
                                      String targetId,
                                      LocalDateTime createTime,
                                      String requestParams,
                                      String responseSummary) {
        AuditLogRecord record = new AuditLogRecord();
        record.setTraceId(traceId);
        record.setRequestId("request-" + traceId);
        record.setServiceCode(serviceCode);
        record.setOperationType("TRACE_TEST");
        record.setTargetType(targetType);
        record.setTargetId(targetId);
        record.setStatus(status);
        record.setCostMs(Long.valueOf(12L));
        record.setCreateTime(createTime);
        record.setRequestParams(requestParams);
        record.setResponseSummary(responseSummary);
        return record;
    }

    private QueryHistoryRecord buildHistory(String traceId,
                                            String historyId,
                                            String historyType,
                                            String sqlFingerprint,
                                            LocalDateTime createTime) {
        QueryHistoryRecord record = new QueryHistoryRecord();
        record.setTraceId(traceId);
        record.setHistoryId(historyId);
        record.setHistoryType(historyType);
        record.setSqlFingerprint(sqlFingerprint);
        record.setRequestId("request-" + traceId);
        record.setCreateTime(createTime);
        record.setSubmittedAt(createTime);
        return record;
    }

    private ExportRecord buildExport(String traceId,
                                     String exportId,
                                     String exportStatus,
                                     LocalDateTime createTime) {
        ExportRecord record = new ExportRecord();
        record.setTraceId(traceId);
        record.setExportId(exportId);
        record.setExportStatus(exportStatus);
        record.setRequestId("request-" + traceId);
        record.setCreateTime(createTime);
        record.setFinishedAt(createTime);
        return record;
    }

    private TraceLookupHitRecord buildTraceHit(String traceId, String lastSeenAt, Long lastAuditId) {
        TraceLookupHitRecord record = new TraceLookupHitRecord();
        record.setTraceId(traceId);
        record.setLastSeenAt(LocalDateTime.parse(lastSeenAt));
        record.setLastAuditId(lastAuditId);
        return record;
    }
}
