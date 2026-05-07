package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.application.controller.dto.GovernanceQueryHistoryExportRequest;
import com.company.governance.application.controller.vo.GovernanceTraceDetailVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryDetailVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryExportVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryPageVO;
import com.company.governance.application.controller.vo.GovernanceTraceLookupPageVO;
import com.company.governance.application.controller.vo.GovernanceTraceSummaryVO;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.governance.domain.trace.entity.ExportRecord;
import com.company.governance.domain.trace.entity.GovernanceQueryHistoryProjection;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.domain.trace.entity.TraceLookupHitRecord;
import com.company.governance.infrastructure.benchmarkengine.GovernanceBenchmarkEngineClient;
import com.company.governance.infrastructure.persistence.mapper.AuditLogMapper;
import com.company.governance.infrastructure.persistence.mapper.ExportRecordMapper;
import com.company.governance.infrastructure.persistence.mapper.GovernanceHistoryLookupIndexMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactBatchOperationRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactBatchOperationResponse;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactBatchOperationTarget;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationResponse;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import com.company.sqlforge.common.security.SensitiveDataCryptoProperties;
import com.company.sqlforge.common.security.SensitiveDataCryptoService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GovernanceHistoryApplicationServiceTest {

    private static final String TEST_BASE64_KEY = "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";

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
        SensitiveDataCryptoProperties cryptoProperties = new SensitiveDataCryptoProperties();
        cryptoProperties.setBase64Key(TEST_BASE64_KEY);
        SensitiveDataCryptoService cryptoService = new SensitiveDataCryptoService(cryptoProperties);
        GovernanceHistoryApplicationService service = new GovernanceHistoryApplicationService(
            auditLogMapper,
            null,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic,
            null,
            null,
            cryptoService
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
                "{\"resultStatus\":\"PARTIAL\",\"targetEngine\":\"HIVE\",\"degraded\":true,\"errorCode\":\"12000\","
                    + "\"cacheHit\":false,\"cacheGovernanceStatus\":\"BYPASSED\","
                    + "\"cacheGovernanceEvidence\":\"policyId=cache-policy-001;status=BYPASSED;schemaVersion=schema-v1;"
                    + "riskCode=SCHEMA_VERSION_MISSING;evictionReason=SCHEMA_VERSION_MISMATCH;evictedEntryCount=1;"
                    + "maxEntriesPerPolicy=2;policyCachedEntryCount=0;ttlSeconds=60\"}")
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
        assertEquals("BYPASSED", detail.getCacheGovernanceSurface().get("cacheGovernanceStatus"));
        assertEquals("SCHEMA_VERSION_MISMATCH", ((Map) detail.getCacheGovernanceSurface().get("cacheGovernanceEvidence")).get("evictionReason"));
        assertEquals("2", ((Map) detail.getCacheGovernanceSurface().get("cacheGovernanceEvidence")).get("maxEntriesPerPolicy"));
        assertEquals(1, detail.getAuditEvents().size());
        assertEquals(1, detail.getQueryHistories().size());
        assertEquals("RPT_SALES_DAILY", detail.getQueryHistories().get(0).getReportCode());
        assertEquals("PROD", detail.getQueryHistories().get(0).getStageCode());
        assertEquals(LocalDate.parse("2026-04-25"), detail.getQueryHistories().get(0).getBizDate());
        assertEquals("PAGE", detail.getQueryHistories().get(0).getAccessChannel());
        assertEquals("POSITIONAL", detail.getQueryHistories().get(0).getBindingMode());
        assertEquals(Boolean.TRUE, detail.getQueryHistories().get(0).getParameterizedSqlFlag());
        assertEquals("TABLE:sales.orders", detail.getQueryHistories().get(0).getLogicalObjectHits().get(1).getObjectKey());
        assertEquals("route-001", detail.getQueryHistories().get(0).getRouteSummary().get("ruleId"));
    }

    @Test
    void shouldReturnQueryHistoryPageWithFiltersAndClassificationSummary() {
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
        when(queryHistoryMapper.selectHistoryPage(
            "tenant-a",
            "QUERY_EXECUTION",
            "RPT_SALES_DAILY",
            "hetu_main",
            "PROD",
            LocalDate.parse("2026-04-25"),
            LocalDate.parse("2026-04-24"),
            LocalDate.parse("2026-04-25"),
            "PARTIAL",
            Boolean.TRUE,
            Boolean.TRUE,
            Boolean.FALSE,
            Boolean.TRUE,
            "BUSINESS_VIEW",
            "PAGE",
            "HETU",
            "analyst-001",
            LocalDateTime.parse("2026-04-25T00:00:00"),
            LocalDateTime.parse("2026-04-25T23:59:59"),
            "qh.submitted_at DESC, qh.history_id DESC",
            0,
            3
        )).thenReturn(Arrays.asList(
            buildHistoryProjection("history-001", "trace-001", "PARTIAL", "PAGE"),
            buildHistoryProjection("history-002", "trace-002", "SUCCEEDED", "API"),
            buildHistoryProjection("history-003", "trace-003", "FAILED", "API")
        ));
        when(queryHistoryMapper.countHistoryPage(
            "tenant-a",
            "QUERY_EXECUTION",
            "RPT_SALES_DAILY",
            "hetu_main",
            "PROD",
            LocalDate.parse("2026-04-25"),
            LocalDate.parse("2026-04-24"),
            LocalDate.parse("2026-04-25"),
            "PARTIAL",
            Boolean.TRUE,
            Boolean.TRUE,
            Boolean.FALSE,
            Boolean.TRUE,
            "BUSINESS_VIEW",
            "PAGE",
            "HETU",
            "analyst-001",
            LocalDateTime.parse("2026-04-25T00:00:00"),
            LocalDateTime.parse("2026-04-25T23:59:59")
        )).thenReturn(Integer.valueOf(3));

        GovernanceQueryHistoryPageVO page = service.findQueryHistoryPage(
            "tenant-a",
            "query_execution",
            "RPT_SALES_DAILY",
            "hetu_main",
            "PROD",
            "2026-04-25",
            "2026-04-24",
            "2026-04-25",
            "PARTIAL",
            Boolean.TRUE,
            Boolean.TRUE,
            Boolean.FALSE,
            Boolean.TRUE,
            "BUSINESS_VIEW",
            "PAGE",
            "HETU",
            "analyst-001",
            "2026-04-25T00:00:00",
            "2026-04-25T23:59:59",
            "submittedAt",
            "DESC",
            Integer.valueOf(1),
            Integer.valueOf(2)
        );

        assertEquals(2, page.getItems().size());
        assertEquals(Boolean.TRUE, page.getHasMore());
        assertEquals(Integer.valueOf(3), page.getTotalCount());
        assertEquals("history-001", page.getItems().get(0).getHistoryId());
        assertEquals("PARTIAL", page.getItems().get(0).getResultStatus());
        assertEquals(Collections.singletonList("BUSINESS_VIEW"), page.getItems().get(0).getLogicalObjectTypes());
        assertEquals("BUSINESS_VIEW:vw_sales_daily", page.getItems().get(0).getLogicalObjectHits().get(0).getObjectKey());
        assertEquals(Integer.valueOf(1), ((Map<String, Integer>) page.getClassificationSummary().get("statusCounts")).get("PARTIAL"));
    }

    @Test
    void shouldReturnQueryHistoryDetailWithTraceDrillThrough() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        SensitiveDataCryptoProperties cryptoProperties = new SensitiveDataCryptoProperties();
        cryptoProperties.setBase64Key(TEST_BASE64_KEY);
        SensitiveDataCryptoService cryptoService = new SensitiveDataCryptoService(cryptoProperties);
        GovernanceHistoryApplicationService service = new GovernanceHistoryApplicationService(
            auditLogMapper,
            null,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic,
            null,
            null,
            cryptoService
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
        when(queryHistoryMapper.selectHistoryDetail("tenant-a", "history-001"))
            .thenReturn(buildHistoryProjection("history-001", "trace-query", "PARTIAL", "PAGE"));
        when(queryHistoryMapper.selectById("history-001"))
            .thenReturn(buildHistoryRecord("history-001", "result-history-001", "tenant-a"));
        when(auditLogMapper.selectByTraceId("tenant-a", "trace-query", 10)).thenReturn(Collections.singletonList(
            buildAudit("trace-query", "QUERY_EXECUTION", "PARTIAL", "QUERY", "fp-001",
                LocalDateTime.parse("2026-04-22T10:00:00"),
                "{\"serviceCode\":\"QUERY_EXECUTION\",\"sqlFingerprint\":\"fp-001\"}",
                "{\"resultStatus\":\"PARTIAL\",\"targetEngine\":\"HIVE\"}")
        ));
        when(queryHistoryMapper.selectByTraceId("tenant-a", "trace-query", 10)).thenReturn(Collections.singletonList(
            buildHistory("trace-query", "history-001", "QUERY_EXECUTION", "fp-001", LocalDateTime.parse("2026-04-22T09:58:00"))
        ));
        when(exportRecordMapper.selectByTraceId("tenant-a", "trace-query", 10)).thenReturn(Collections.emptyList());

        GovernanceQueryHistoryDetailVO detail = service.findQueryHistoryDetail("tenant-a", "history-001");

        assertEquals("history-001", detail.getHistoryId());
        assertEquals("RPT_SALES_DAILY", detail.getReportCode());
        assertEquals("SELECT * FROM sales.orders", detail.getSqlText());
        assertEquals("SELECT * FROM sales.orders WHERE dt = ?", detail.getSqlTemplateText());
        assertEquals("SELECT * FROM sales.orders WHERE dt = '2026-04-25'", detail.getBoundSqlText());
        assertEquals("POSITIONAL", detail.getSqlState().get("bindingMode"));
        assertEquals("RESOLVED", detail.getQueryDateSummary().get("queryDateStatus"));
        assertEquals("HETU", detail.getExecutionSummary().get("targetEngine"));
        assertEquals("BUSINESS_VIEW", detail.getLogicalObjectHits().get(0).getObjectType());
        assertEquals("BUSINESS_VIEW:vw_sales_daily", detail.getLogicalObjectHits().get(0).getObjectKey());
        assertNotNull(detail.getTraceDetail());
        assertEquals(1, detail.getAuditRefs().size());
        assertEquals("trace-query", detail.getTraceDetail().getTraceId());
    }

    @Test
    void shouldExportQueryHistoryWithAuditLinkAndInlinePayload() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceProtectedPersistenceService protectedPersistenceService = mock(GovernanceProtectedPersistenceService.class);
        SensitiveDataCryptoProperties cryptoProperties = new SensitiveDataCryptoProperties();
        cryptoProperties.setBase64Key(TEST_BASE64_KEY);
        SensitiveDataCryptoService cryptoService = new SensitiveDataCryptoService(cryptoProperties);
        GovernanceHistoryApplicationService service = new GovernanceHistoryApplicationService(
            auditLogMapper,
            null,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic,
            null,
            protectedPersistenceService,
            cryptoService
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
        when(queryHistoryMapper.selectHistoryDetail("tenant-a", "history-001"))
            .thenReturn(buildHistoryProjection("history-001", "trace-query", "PARTIAL", "PAGE"));
        when(queryHistoryMapper.selectById("history-001"))
            .thenReturn(buildHistoryRecord("history-001", "result-history-001", "tenant-a"));
        when(auditLogMapper.selectByTraceId("tenant-a", "trace-query", 10)).thenReturn(Collections.singletonList(
            buildAudit("trace-query", "QUERY_EXECUTION", "PARTIAL", "QUERY", "fp-001",
                LocalDateTime.parse("2026-04-22T10:00:00"),
                "{\"serviceCode\":\"QUERY_EXECUTION\",\"sqlFingerprint\":\"fp-001\"}",
                "{\"resultStatus\":\"PARTIAL\",\"targetEngine\":\"HIVE\"}")
        ));
        when(queryHistoryMapper.selectByTraceId("tenant-a", "trace-query", 10)).thenReturn(Collections.singletonList(
            buildHistory("trace-query", "history-001", "QUERY_EXECUTION", "fp-001", LocalDateTime.parse("2026-04-22T09:58:00"))
        ));
        when(exportRecordMapper.selectByTraceId("tenant-a", "trace-query", 10)).thenReturn(Collections.emptyList());

        GovernanceQueryHistoryExportRequest request = new GovernanceQueryHistoryExportRequest();
        request.setHistoryId("history-001");
        request.setExportFormat("SQL_TEXT");
        request.setIncludeTraceDetail(Boolean.TRUE);
        request.setExportReason("forensics");

        GovernanceQueryHistoryExportVO response = service.exportQueryHistory("tenant-a", request);

        assertEquals("history-001", response.getHistoryId());
        assertEquals("SQL_TEXT", response.getExportFormat());
        assertEquals("GENERATED", response.getExportStatus());
        assertEquals("text/sql", response.getContentType());
        assertEquals(Boolean.TRUE, response.getPayload().contains("SELECT * FROM sales.orders"));
        assertEquals(Boolean.TRUE, response.getPayload().contains("logical_object_keys=BUSINESS_VIEW:vw_sales_daily"));
        verify(protectedPersistenceService).saveExportRecord(any(ExportRecord.class));
        verify(protectedPersistenceService).saveAuditLog(any(AuditLogRecord.class));
    }

    @Test
    void shouldNormalizeLegacyLogicalObjectEvidenceIntoSharedSurface() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        SensitiveDataCryptoProperties cryptoProperties = new SensitiveDataCryptoProperties();
        cryptoProperties.setBase64Key(TEST_BASE64_KEY);
        SensitiveDataCryptoService cryptoService = new SensitiveDataCryptoService(cryptoProperties);
        GovernanceHistoryApplicationService service = new GovernanceHistoryApplicationService(
            auditLogMapper,
            null,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic,
            null,
            null,
            cryptoService
        );

        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-request-001",
            "header",
            100L,
            200L
        );
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "governance-tenant-config")).thenReturn(true);
        GovernanceQueryHistoryProjection row = buildHistoryProjection("history-legacy", "trace-legacy", "SUCCESS", "PAGE");
        row.setLogicalObjectHits("[\"vw_sales_daily\",\"sales.orders\"]");
        when(queryHistoryMapper.selectHistoryDetail("tenant-a", "history-legacy")).thenReturn(row);
        when(queryHistoryMapper.selectById("history-legacy")).thenReturn(buildHistoryRecord("history-legacy", "result-history-legacy", "tenant-a"));
        when(auditLogMapper.selectByTraceId("tenant-a", "trace-legacy", 10)).thenReturn(Collections.emptyList());
        when(queryHistoryMapper.selectByTraceId("tenant-a", "trace-legacy", 10)).thenReturn(Collections.emptyList());
        when(exportRecordMapper.selectByTraceId("tenant-a", "trace-legacy", 10)).thenReturn(Collections.emptyList());

        GovernanceQueryHistoryDetailVO detail = service.findQueryHistoryDetail("tenant-a", "history-legacy");

        assertEquals(2, detail.getLogicalObjectHits().size());
        LogicalObjectSurface first = detail.getLogicalObjectHits().get(0);
        LogicalObjectSurface second = detail.getLogicalObjectHits().get(1);
        assertEquals("DB_VIEW", first.getObjectType());
        assertEquals("DB_VIEW:vw_sales_daily", first.getObjectKey());
        assertEquals("TABLE", second.getObjectType());
        assertEquals("TABLE:sales.orders", second.getObjectKey());
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
                + "\"compensationApplied\":true,\"compensationStrategy\":\"LATEST_SUCCESS_REPLAY\","
                + "\"engines\":{\"HETU\":{\"compensationApplied\":true,\"compensationStrategy\":\"LATEST_SUCCESS_REPLAY\","
                + "\"compensationSourceEngine\":\"HIVE\",\"compensationSourceWorkloadDigest\":\"digest-001\","
                + "\"cacheHit\":true,\"cacheGovernanceStatus\":\"HIT\","
                + "\"cacheGovernanceEvidence\":{\"policyId\":\"cache-policy-001\",\"schemaVersion\":\"schema-v1\","
                + "\"evictionReason\":\"CAPACITY_EVICTED\",\"maxEntriesPerPolicy\":2,"
                + "\"policyCachedEntryCount\":2,\"ttlSeconds\":60}}}}}}"
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
        assertEquals("HIT", ((Map) ((Map) detail.getCacheGovernanceSurface().get("engines")).get("HETU")).get("cacheGovernanceStatus"));
        Map cacheEvidence = (Map) ((Map) ((Map) detail.getCacheGovernanceSurface().get("engines")).get("HETU")).get("cacheGovernanceEvidence");
        assertEquals("CAPACITY_EVICTED", cacheEvidence.get("evictionReason"));
        assertEquals(Integer.valueOf(2), cacheEvidence.get("maxEntriesPerPolicy"));
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
    void shouldOrchestrateBatchRetentionAcrossTargetsAndKeepPartialFailureVisible() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceBenchmarkEngineClient governanceBenchmarkEngineClient = mock(GovernanceBenchmarkEngineClient.class);
        GovernanceHistoryApplicationService service = new GovernanceHistoryApplicationService(
            auditLogMapper,
            null,
            queryHistoryMapper,
            exportRecordMapper,
            tenantAccessLogic,
            governanceBenchmarkEngineClient
        );

        RequestContext.set(
            "tenant-a",
            "tenant-admin-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-batch-001",
            "header",
            100L,
            200L
        );
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "governance-tenant-config")).thenReturn(true);

        GovernanceBenchmarkArtifactOperationResponse success = new GovernanceBenchmarkArtifactOperationResponse();
        success.setTenantId("tenant-a");
        success.setReportId("report-001");
        success.setArtifactKey("json-export");
        success.setOperationType("CLEANUP_ARTIFACT");
        success.setOperationStatus("CLEANUP_COMPLETED");
        when(governanceBenchmarkEngineClient.operateArtifact(org.mockito.ArgumentMatchers.argThat(request ->
            request != null
                && "report-001".equals(request.getReportId())
                && "json-export".equals(request.getArtifactKey())
        ))).thenReturn(success);
        when(governanceBenchmarkEngineClient.operateArtifact(org.mockito.ArgumentMatchers.argThat(request ->
            request != null
                && "report-002".equals(request.getReportId())
                && "raw-data".equals(request.getArtifactKey())
        ))).thenThrow(new BizException(
            ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
            org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
            "Benchmark-engine artifact operation route is unavailable"
        ));

        GovernanceBenchmarkArtifactBatchOperationRequest request = new GovernanceBenchmarkArtifactBatchOperationRequest();
        request.setTenantId("tenant-a");
        request.setOperationType("EXECUTE_RETENTION_BATCH");
        request.setOperationReason("retention-window");
        request.setTargets(Arrays.asList(
            target("report-001", "json-export"),
            target("report-001", "json-export"),
            target("report-002", "raw-data")
        ));

        GovernanceBenchmarkArtifactBatchOperationResponse response = service.operateArtifactBatch(request);

        assertEquals("artifact-batch-request-001", response.getBatchId());
        assertEquals("PARTIAL_FAILURE", response.getOperationStatus());
        assertEquals(Integer.valueOf(3), response.getTotalItems());
        assertEquals(Integer.valueOf(1), response.getSucceededItems());
        assertEquals(Integer.valueOf(1), response.getFailedItems());
        assertEquals(Integer.valueOf(1), response.getSkippedItems());
        assertEquals(Boolean.TRUE, response.getPartialFailure());
        assertEquals("MIRROR_LIVE_EVIDENCE_EXTERNAL_WRITE_PROVIDER", response.getBatchOperationSurface().get("cleanupScope"));
        assertEquals("trace-batch-001", response.getBatchOperationSurface().get("traceId"));
        assertEquals("EXECUTE_RETENTION_BATCH", response.getItems().get(0).getOrchestrationType());
        assertEquals("SKIPPED_DUPLICATE", response.getItems().get(1).getOperationStatus());
        assertEquals("FAILED", response.getItems().get(2).getOperationStatus());
        assertEquals("Benchmark-engine artifact operation route is unavailable", response.getItems().get(2).getErrorMessage());
        verify(governanceBenchmarkEngineClient, org.mockito.Mockito.times(2)).operateArtifact(org.mockito.ArgumentMatchers.any());
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

    private GovernanceBenchmarkArtifactBatchOperationTarget target(String reportId, String artifactKey) {
        GovernanceBenchmarkArtifactBatchOperationTarget target = new GovernanceBenchmarkArtifactBatchOperationTarget();
        target.setReportId(reportId);
        target.setArtifactKey(artifactKey);
        return target;
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
        record.setDatasourceCode("hetu_main");
        record.setReportCode("RPT_SALES_DAILY");
        record.setStageCode("PROD");
        record.setBizDate(LocalDate.parse("2026-04-25"));
        record.setQueryDateStart(LocalDate.parse("2026-04-24"));
        record.setQueryDateEnd(LocalDate.parse("2026-04-25"));
        record.setQueryDateStatus("RESOLVED");
        record.setAccessChannel("PAGE");
        record.setParameterizedSqlFlag(Boolean.TRUE);
        record.setBindingMode("POSITIONAL");
        record.setBindingRenderStatus("SUCCESS");
        record.setSqlTemplateFingerprint("tmpl-fp");
        record.setBoundSqlFingerprint("bound-fp");
        record.setCommentContext("{\"report_code\":\"RPT_SALES_DAILY\",\"stage\":\"PROD\",\"biz_date\":\"2026-04-25\"}");
        record.setBindingSummary("{\"bindingMode\":\"POSITIONAL\",\"parameterizedSqlFlag\":true}");
        record.setLogicalObjectHits("[\"vw_sales_daily\",\"sales.orders\"]");
        record.setRouteSummary("{\"ruleId\":\"route-001\",\"selectedEngine\":\"HETU\"}");
        record.setCacheSummary("{\"cacheHit\":false}");
        record.setRequestId("request-" + traceId);
        record.setQueryContext("{\"workloadSource\":\"STANDARD\"}");
        record.setCreateTime(createTime);
        record.setSubmittedAt(createTime);
        return record;
    }

    private GovernanceQueryHistoryProjection buildHistoryProjection(String historyId,
                                                                    String traceId,
                                                                    String resultStatus,
                                                                    String accessChannel) {
        GovernanceQueryHistoryProjection row = new GovernanceQueryHistoryProjection();
        row.setHistoryId(historyId);
        row.setResultId("result-" + historyId);
        row.setTraceId(traceId);
        row.setHistoryType("QUERY_EXECUTION");
        row.setReportCode("RPT_SALES_DAILY");
        row.setDatasourceCode("hetu_main");
        row.setDatasourceType("HETU");
        row.setStageCode("PROD");
        row.setBizDate(LocalDate.parse("2026-04-25"));
        row.setQueryDateStart(LocalDate.parse("2026-04-24"));
        row.setQueryDateEnd(LocalDate.parse("2026-04-25"));
        row.setQueryDateStatus("RESOLVED");
        row.setAccessChannel(accessChannel);
        row.setParameterizedSqlFlag(Boolean.TRUE);
        row.setBindingMode("POSITIONAL");
        row.setBindingRenderStatus("SUCCESS");
        row.setSqlFingerprint("fp-001");
        row.setSqlTemplateFingerprint("tmpl-fp");
        row.setBoundSqlFingerprint("bound-fp");
        row.setCommentContext("{\"report_code\":\"RPT_SALES_DAILY\"}");
        row.setBindingSummary("{\"bindingMode\":\"POSITIONAL\"}");
        row.setLogicalObjectHits("[{\"objectType\":\"BUSINESS_VIEW\",\"objectKey\":\"BUSINESS_VIEW:vw_sales_daily\",\"objectName\":\"vw_sales_daily\"}]");
        row.setRouteSummary("{\"selectedEngine\":\"HETU\",\"ruleId\":\"route-001\"}");
        row.setCacheSummary("{\"cacheHit\":true}");
        row.setQueryContext("{\"structureParseSummary\":{\"syntaxStatus\":\"VALID\"},\"accessParseSummary\":{\"serviceStatus\":\"AVAILABLE\"}}");
        row.setSubmittedBy("analyst-001");
        row.setSubmittedAt(LocalDateTime.parse("2026-04-25T12:00:00"));
        row.setCreateTime(LocalDateTime.parse("2026-04-25T12:00:00"));
        row.setResultStatus(resultStatus);
        row.setTargetEngine("HETU");
        row.setReturnedRowCount(Long.valueOf(10L));
        row.setCacheHit(Boolean.TRUE);
        row.setRewriteApplied(Boolean.TRUE);
        row.setAccelerationApplied(Boolean.FALSE);
        row.setHitTableSummary("[\"sales.orders\"]");
        row.setResultSummary("{\"cacheSummary\":{\"cacheHit\":true},\"routeSummary\":{\"selectedEngine\":\"HETU\"}}");
        row.setErrorCode("12000");
        row.setErrorMessage("degraded");
        row.setStartedAt(LocalDateTime.parse("2026-04-25T12:00:00"));
        row.setFinishedAt(LocalDateTime.parse("2026-04-25T12:00:02"));
        return row;
    }

    private QueryHistoryRecord buildHistoryRecord(String historyId, String resultId, String tenantId) {
        SensitiveDataCryptoProperties cryptoProperties = new SensitiveDataCryptoProperties();
        cryptoProperties.setBase64Key(TEST_BASE64_KEY);
        SensitiveDataCryptoService cryptoService = new SensitiveDataCryptoService(cryptoProperties);
        QueryHistoryRecord record = new QueryHistoryRecord();
        record.setHistoryId(historyId);
        record.setResultId(resultId);
        record.setTenantId(tenantId);
        record.setSqlTextCipher(cryptoService.encryptBytes("SELECT * FROM sales.orders".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        record.setSqlTemplateCipher(cryptoService.encryptBytes("SELECT * FROM sales.orders WHERE dt = ?".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        record.setBoundSqlTextCipher(cryptoService.encryptBytes("SELECT * FROM sales.orders WHERE dt = '2026-04-25'".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
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
