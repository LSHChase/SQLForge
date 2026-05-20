package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.governance.domain.trace.entity.ConfigSnapshotRecord;
import com.company.governance.domain.trace.entity.ExecutionResultRecord;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.infrastructure.persistence.mapper.ConfigSnapshotMapper;
import com.company.governance.infrastructure.persistence.mapper.ExecutionResultMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteResponse;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GovernanceQueryExecutionHistoryApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldPersistExecutionResultQueryHistoryAndAuditInOneWrite() {
        GovernanceProtectedPersistenceService protectedPersistenceService =
            mock(GovernanceProtectedPersistenceService.class);
        GovernanceQueryExecutionHistoryApplicationService service =
            new GovernanceQueryExecutionHistoryApplicationService(
                protectedPersistenceService,
                mock(ConfigSnapshotMapper.class),
                mock(ExecutionResultMapper.class),
                mock(QueryHistoryMapper.class)
            );
        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            1L,
            System.currentTimeMillis() + 60000L
        );
        doAnswer(invocation -> {
            AuditLogRecord record = invocation.getArgument(0);
            record.setId(Long.valueOf(9001L));
            return Integer.valueOf(1);
        }).when(protectedPersistenceService).saveAuditLog(any(AuditLogRecord.class));

        GovernanceQueryExecutionHistoryWriteResponse response =
            service.writeQueryExecutionHistory(sampleRequest());

        assertEquals("cfg-qe-001", response.getConfigSnapshotId());
        assertEquals("result-qe-001", response.getResultId());
        assertEquals("history-qe-001", response.getHistoryId());
        assertEquals(Long.valueOf(9001L), response.getAuditId());
        assertEquals("trace-001", response.getTraceId());
        assertEquals("request-001", response.getRequestId());

        ArgumentCaptor<ConfigSnapshotRecord> snapshotCaptor =
            ArgumentCaptor.forClass(ConfigSnapshotRecord.class);
        ArgumentCaptor<ExecutionResultRecord> resultCaptor =
            ArgumentCaptor.forClass(ExecutionResultRecord.class);
        ArgumentCaptor<QueryHistoryRecord> historyCaptor =
            ArgumentCaptor.forClass(QueryHistoryRecord.class);
        ArgumentCaptor<AuditLogRecord> auditCaptor =
            ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(protectedPersistenceService).saveConfigSnapshot(snapshotCaptor.capture());
        verify(protectedPersistenceService).saveExecutionResult(resultCaptor.capture());
        verify(protectedPersistenceService).saveQueryHistoryWithSqlSurfaces(
            historyCaptor.capture(),
            org.mockito.Mockito.eq("SELECT * FROM orders WHERE query_date = '2026-04-27'"),
            org.mockito.Mockito.eq("SELECT * FROM orders WHERE query_date = '2026-04-27'"),
            org.mockito.Mockito.eq("SELECT * FROM orders WHERE query_date = '2026-04-27'")
        );
        verify(protectedPersistenceService).saveAuditLog(auditCaptor.capture());

        ConfigSnapshotRecord snapshot = snapshotCaptor.getValue();
        assertEquals("tenant-a", snapshot.getTenantId());
        assertEquals("QUERY_EXECUTION", snapshot.getServiceCode());
        assertEquals("QUERY_EXECUTION", snapshot.getSourceConfigType());
        assertNotNull(snapshot.getSnapshotPayload());

        ExecutionResultRecord result = resultCaptor.getValue();
        assertEquals("result-qe-001", result.getResultId());
        assertEquals("cfg-qe-001", result.getConfigSnapshotId());
        assertEquals("SUCCESS", result.getResultStatus());
        assertEquals("HETU", result.getTargetEngine());
        assertEquals(Long.valueOf(1L), result.getReturnedRowCount());
        assertEquals(Boolean.FALSE, result.getCacheHit());
        assertEquals(Boolean.FALSE, result.getRewriteApplied());
        assertEquals(Boolean.FALSE, result.getAccelerationApplied());
        assertEquals("{\"selectedEngine\":\"HETU\"}", result.getRouteSummary());
        assertEquals("{\"cacheHit\":false}", result.getCacheSummary());

        QueryHistoryRecord history = historyCaptor.getValue();
        assertEquals("history-qe-001", history.getHistoryId());
        assertEquals("result-qe-001", history.getResultId());
        assertEquals("QUERY_EXECUTION", history.getHistoryType());
        assertEquals("fp-001", history.getSqlFingerprint());
        assertEquals("hetu_main", history.getDatasourceCode());
        assertEquals("HETU", history.getDatasourceType());
        assertEquals("API", history.getAccessChannel());
        assertEquals("2026-04-27", history.getQueryDateStart().toString());
        assertEquals("2026-04-27", history.getQueryDateEnd().toString());
        assertEquals("RESOLVED", history.getQueryDateStatus());
        assertEquals("operator-001", history.getSubmittedBy());

        AuditLogRecord audit = auditCaptor.getValue();
        assertEquals("QUERY_EXECUTION", audit.getServiceCode());
        assertEquals("QUERY_EXECUTE_SYNC", audit.getOperationType());
        assertEquals("QUERY_EXECUTION_QUERY", audit.getTargetType());
        assertEquals("fp-001", audit.getTargetId());
        assertEquals("result-qe-001", audit.getResultId());
        assertEquals("history-qe-001", audit.getHistoryId());
        assertEquals("SUCCESS", audit.getStatus());
        assertEquals(Long.valueOf(42L), audit.getCostMs());
    }

    @Test
    void shouldPersistStructuredRewriteAuditFieldsForQueryHistory() {
        GovernanceProtectedPersistenceService protectedPersistenceService =
            mock(GovernanceProtectedPersistenceService.class);
        GovernanceQueryExecutionHistoryApplicationService service =
            new GovernanceQueryExecutionHistoryApplicationService(
                protectedPersistenceService,
                mock(ConfigSnapshotMapper.class),
                mock(ExecutionResultMapper.class),
                mock(QueryHistoryMapper.class)
            );
        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            1L,
            System.currentTimeMillis() + 60000L
        );
        doAnswer(invocation -> {
            AuditLogRecord record = invocation.getArgument(0);
            record.setId(Long.valueOf(9002L));
            return Integer.valueOf(1);
        }).when(protectedPersistenceService).saveAuditLog(any(AuditLogRecord.class));
        GovernanceQueryExecutionHistoryWriteRequest request = sampleRequest();
        request.setRewriteApplied(Boolean.TRUE);
        request.setSqlTemplate("SELECT * FROM orders");
        request.setBoundSql("SELECT id FROM orders");
        request.setBindingSummary("{\"rewriteApplied\":true,\"rewriteRecordId\":\"rewrite-001\","
            + "\"runtimeBindingId\":\"rwb-001\",\"ruleVersion\":3,"
            + "\"runtimeRuleVersion\":\"runtime-rewrite-v3\","
            + "\"rewriteActivationStatusSnapshot\":\"ACTIVE\"}");
        request.setRewriteRecordId("rewrite-001");
        request.setRuntimeBindingId("rwb-001");
        request.setRewriteRuleVersion(Long.valueOf(3L));
        request.setRuntimeRuleVersion("runtime-rewrite-v3");
        request.setRuntimeRewriteStatus("ACTIVE");
        request.setRewriteActivationStatusSnapshot("ACTIVE");

        service.writeQueryExecutionHistory(request);

        ArgumentCaptor<QueryHistoryRecord> historyCaptor =
            ArgumentCaptor.forClass(QueryHistoryRecord.class);
        ArgumentCaptor<ExecutionResultRecord> resultCaptor =
            ArgumentCaptor.forClass(ExecutionResultRecord.class);
        verify(protectedPersistenceService).saveExecutionResult(resultCaptor.capture());
        verify(protectedPersistenceService).saveQueryHistoryWithSqlSurfaces(
            historyCaptor.capture(),
            org.mockito.Mockito.eq("SELECT * FROM orders WHERE query_date = '2026-04-27'"),
            org.mockito.Mockito.eq("SELECT * FROM orders"),
            org.mockito.Mockito.eq("SELECT id FROM orders")
        );

        ExecutionResultRecord result = resultCaptor.getValue();
        assertEquals(Boolean.TRUE, result.getRewriteApplied());
        assertEquals(Boolean.TRUE, result.getResultSummary().contains("\"rewriteRecordId\":\"rewrite-001\""));

        QueryHistoryRecord history = historyCaptor.getValue();
        assertEquals("rewrite-001", history.getRewriteRecordId());
        assertEquals("rwb-001", history.getRuntimeBindingId());
        assertEquals(Long.valueOf(3L), history.getRewriteRuleVersion());
        assertEquals("runtime-rewrite-v3", history.getRuntimeRuleVersion());
        assertEquals("ACTIVE", history.getRuntimeRewriteStatus());
        assertEquals("ACTIVE", history.getRewriteActivationStatusSnapshot());
    }

    private GovernanceQueryExecutionHistoryWriteRequest sampleRequest() {
        GovernanceQueryExecutionHistoryWriteRequest request =
            new GovernanceQueryExecutionHistoryWriteRequest();
        request.setConfigSnapshotId("cfg-qe-001");
        request.setResultId("result-qe-001");
        request.setHistoryId("history-qe-001");
        request.setTenantId("tenant-a");
        request.setSqlText("SELECT * FROM orders WHERE query_date = '2026-04-27'");
        request.setSqlTemplate("SELECT * FROM orders WHERE query_date = '2026-04-27'");
        request.setBoundSql("SELECT * FROM orders WHERE query_date = '2026-04-27'");
        request.setSqlFingerprint("fp-001");
        request.setDatasourceCode("hetu_main");
        request.setDatasourceType("HETU");
        request.setHistoryType("QUERY_EXECUTION");
        request.setResultStatus("SUCCESS");
        request.setTargetEngine("HETU");
        request.setReturnedRowCount(Long.valueOf(1L));
        request.setCacheHit(Boolean.FALSE);
        request.setRewriteApplied(Boolean.FALSE);
        request.setAccelerationApplied(Boolean.FALSE);
        request.setAccessChannel("API");
        request.setCommentContext("{\"report_code\":\"RPT_ORDER\",\"stage\":\"PROD\"}");
        request.setQueryDateSummary(
            "{\"queryDateStart\":\"2026-04-27\",\"queryDateEnd\":\"2026-04-27\",\"queryDateStatus\":\"RESOLVED\"}"
        );
        request.setBindingSummary("{\"parameterizedSqlFlag\":false,\"bindingMode\":\"NONE\"}");
        request.setLogicalObjectHits("[{\"objectType\":\"TABLE\",\"objectKey\":\"TABLE:orders\"}]");
        request.setRouteSummary("{\"selectedEngine\":\"HETU\"}");
        request.setCacheSummary("{\"cacheHit\":false}");
        request.setQueryContext(
            "{\"queryDateStart\":\"2026-04-27\",\"queryDateEnd\":\"2026-04-27\","
                + "\"queryDateStatus\":\"RESOLVED\"}"
        );
        request.setTraceId("trace-001");
        request.setRequestId("request-001");
        request.setSagaId("query-execution-001");
        request.setSubmittedBy("operator-001");
        request.setStartedAt("2026-05-08T00:00:00Z");
        request.setFinishedAt("2026-05-08T00:00:01Z");
        request.setElapsedMs(Long.valueOf(42L));
        return request;
    }
}
