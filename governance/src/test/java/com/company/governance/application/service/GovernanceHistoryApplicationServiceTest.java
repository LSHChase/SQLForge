package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.governance.application.controller.vo.GovernanceTraceDetailVO;
import com.company.governance.application.controller.vo.GovernanceTraceSummaryVO;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.governance.domain.trace.entity.ExportRecord;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.infrastructure.persistence.mapper.AuditLogMapper;
import com.company.governance.infrastructure.persistence.mapper.ExportRecordMapper;
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
}
