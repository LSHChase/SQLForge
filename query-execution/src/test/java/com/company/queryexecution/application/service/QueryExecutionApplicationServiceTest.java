package com.company.queryexecution.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.queryexecution.application.controller.dto.QueryContextDTO;
import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.config.QueryExecutionRewriteProperties;
import com.company.queryexecution.domain.query.AccelerationPreference;
import com.company.queryexecution.domain.query.FaultToleranceStrategy;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.queryexecution.domain.query.QueryExecutionStatus;
import com.company.queryexecution.infrastructure.adapter.DeterministicQueryExecutionAdapter;
import com.company.queryexecution.infrastructure.adapter.HetuExecutionUnavailableException;
import com.company.queryexecution.infrastructure.adapter.QueryExecutionAdapter;
import com.company.queryexecution.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanActivationRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionCachePolicyApplyRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResolveRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.RequestMetadataContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteCandidate;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveResponse;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteResponse;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class QueryExecutionApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
        RequestMetadataContext.clear();
    }

    @Test
    void shouldExecuteSynchronouslyForReadonlyHetuQuery(CapturedOutput output) {
        setRequestContext("tenant-a");
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        QueryExecutionApplicationService service =
            newService(new DeterministicQueryExecutionAdapter(), mockGovernanceClient(), meterRegistry);
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders");
        request.setAccelerationPreference(AccelerationPreference.PREFER_ACCELERATED);

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        assertEquals("HETU", response.getMetadata().getTargetEngine());
        assertEquals("HETU_REAL_INTEGRATION", response.getImplementationStage());
        assertFalse(response.isDegraded());
        assertNull(response.getError());
        assertEquals(1, response.getRows().size());
        assertFalse(response.getMetadata().isAccelerationApplied());
        assertEquals("SIMULATED", response.getMetadata().getExecutionMode());
        assertEquals(1, response.getMetadata().getAttemptedModes().size());
        assertEquals("SIMULATED", response.getMetadata().getAttemptedModes().get(0));
        assertEquals(1, response.getMetadata().getRowCount());
        assertTrue(output.getOut().contains("operation=QUERY_EXECUTE_SYNC"));
        assertTrue(output.getOut().contains("status=START"));
        assertTrue(output.getOut().contains("to=PRIMARY_ROUTE_SELECTED"));
        assertTrue(output.getOut().contains("status=END resultStatus=SUCCESS"));
        assertFalse(output.getOut().contains("SELECT * FROM orders"));
        assertEquals(1.0D, meterRegistry.get("sqlforge.query.execution.requests").tags(
            "requested_datasource", "HETU",
            "target_engine", "HETU",
            "result_status", "SUCCESS",
            "degraded", "false",
            "execution_mode", "SIMULATED",
            "fault_tolerance", "FAIL_FAST"
        ).counter().count());
        assertEquals(1L, meterRegistry.get("sqlforge.query.execution.latency").timer().count());
        assertEquals(1.0D, meterRegistry.get("sqlforge.query.execution.mode.hits").tags(
            "requested_datasource", "HETU",
            "target_engine", "HETU",
            "mode", "SIMULATED"
        ).counter().count());
    }

    @Test
    void shouldExecuteSynchronouslyForReadonlyHetuQueryWithLeadingComments() {
        setRequestContext("tenant-a");
        QueryExecutionApplicationService service =
            newService(new DeterministicQueryExecutionAdapter(), mockGovernanceClient(), new SimpleMeterRegistry());
        QueryExecuteRequest request = baseRequest(
            "--report_code=RPT_SQL_QUERY\n--stage=PROD\nSELECT * FROM orders WHERE query_date = '2026-04-27'"
        );

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        assertNull(response.getError());
        assertEquals("SIMULATED", response.getMetadata().getExecutionMode());
        assertEquals("RPT_SQL_QUERY", response.getCommentContext().get("report_code"));
        assertEquals("PROD", response.getCommentContext().get("stage"));
        assertEquals("RESOLVED", response.getQueryDateSummary().get("queryDateStatus"));
        assertEquals("2026-04-27", response.getQueryDateSummary().get("queryDateStart"));
        assertEquals("SUCCESS", response.getBindingSummary().get("bindingRenderStatus"));
        assertEquals("TABLE:orders", response.getLogicalObjectHits().get(0).getObjectKey());
        assertEquals("HETU", response.getRouteSummary().get("selectedEngine"));
        assertEquals(Boolean.FALSE, response.getCacheSummary().get("cacheHit"));
        assertEquals("SELECT", response.getLightweightParseSummary().get("sqlType"));
        assertEquals("VALID", response.getLightweightParseSummary().get("syntaxStatus"));
    }

    @Test
    void shouldExposePartialBindingAndQueryDateSummariesForParameterizedSql() {
        setRequestContext("tenant-a");
        QueryExecutionApplicationService service =
            newService(new DeterministicQueryExecutionAdapter(), mockGovernanceClient(), new SimpleMeterRegistry());
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders WHERE query_date = :query_date LIMIT :limit");

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        assertEquals(Boolean.TRUE, response.getBindingSummary().get("parameterizedSqlFlag"));
        assertEquals("NAMED", response.getBindingSummary().get("bindingMode"));
        assertEquals("PARTIAL", response.getBindingSummary().get("bindingRenderStatus"));
        assertEquals("PARTIAL", response.getQueryDateSummary().get("queryDateStatus"));
        assertEquals(Collections.singletonList("query_date"), response.getQueryDateSummary().get("queryDateFields"));
        assertEquals("MODERATE", response.getLightweightParseSummary().get("complexityLevel"));
        assertEquals(Boolean.TRUE, ((java.util.List<?>) response.getLightweightParseSummary().get("rewriteCandidates"))
            .contains("RESOLVE_QUERY_DATE_BINDINGS"));
    }

    @Test
    void shouldApplyAccelerationOnlyWhenActiveBindingExists() {
        setRequestContext("tenant-a");
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        QueryExecutionAccelerationRuntimeService runtimeService = new QueryExecutionAccelerationRuntimeService();
        QueryExecutionAccelerationPlanActivationRequest activationRequest = new QueryExecutionAccelerationPlanActivationRequest();
        activationRequest.setTenantId("tenant-a");
        activationRequest.setPlanId("plan-001");
        activationRequest.setSqlFingerprint(com.company.sqlforge.common.utils.SqlFingerprintUtils.fingerprint("SELECT * FROM orders"));
        activationRequest.setDatasourceType("HETU");
        activationRequest.setSelectedSuggestionTypes(java.util.Collections.singletonList("PRECOMPUTE"));
        activationRequest.setPlanSummary("active plan");
        activationRequest.setPrimaryRecommendation("use active runtime config");
        runtimeService.activate(activationRequest);

        QueryExecutionApplicationService service = new QueryExecutionApplicationService(
            new DeterministicQueryExecutionAdapter(),
            governanceCapabilityClient,
            QueryExecutionMetricsRecorder.noop(),
            runtimeService
        );
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders");
        request.setAccelerationPreference(AccelerationPreference.PREFER_ACCELERATED);

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        assertTrue(response.getMetadata().isAccelerationApplied());
    }

    @Test
    void shouldApplyActiveRuntimeRewriteBindingBeforeExecution() {
        setRequestContext("tenant-a");
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        QueryExecutionRuntimeRewriteBindingService rewriteBindingService =
            mock(QueryExecutionRuntimeRewriteBindingService.class);
        String originalSql = "SELECT * FROM orders";
        String recommendedSql = "SELECT id FROM orders";
        String originalFingerprint = SqlFingerprintUtils.fingerprint(originalSql);
        when(rewriteBindingService.resolveActive(any()))
            .thenReturn(activeRuntimeRewriteResponse(originalFingerprint, recommendedSql));
        RecordingQueryExecutionAdapter adapter = new RecordingQueryExecutionAdapter();
        QueryExecutionApplicationService service =
            newService(adapter, governanceCapabilityClient, rewriteBindingService, developmentDirectSuccessProperties());

        QueryExecuteResponse response = service.executeSynchronously(baseRequest(originalSql));

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        assertNull(adapter.actualSql);
        assertEquals(originalSql, response.getMetadata().getOriginalSql());
        assertEquals(recommendedSql, response.getMetadata().getActualSql());
        assertEquals("DEV_REWRITE_DIRECT_SUCCESS", response.getMetadata().getExecutionMode());
        assertEquals("DEV_RUNTIME_REWRITE_SHORT_CIRCUIT", response.getMetadata().getRouteProfile());
        assertTrue(response.getMetadata().isRewriteApplied());
        assertEquals(1, response.getRows().size());
        assertEquals(Boolean.TRUE, response.getRows().get(0).get("rewriteApplied"));
        assertEquals("rewrite-001", response.getMetadata().getRewriteRecordId());
        assertEquals("rwb-001", response.getMetadata().getRuntimeBindingId());
        assertEquals(Long.valueOf(3L), response.getMetadata().getRuleVersion());
        assertEquals("runtime-rewrite-v3", response.getMetadata().getRuntimeRuleVersion());
        assertEquals(originalFingerprint, response.getSqlFingerprint());
        assertEquals(Boolean.TRUE, response.getBindingSummary().get("rewriteApplied"));
        assertEquals("rwb-001", response.getBindingSummary().get("runtimeBindingId"));
        assertEquals(SqlFingerprintUtils.fingerprint(recommendedSql),
            response.getBindingSummary().get("actualSqlFingerprint"));

        ArgumentCaptor<RuntimeRewriteBindingResolveRequest> resolveCaptor =
            ArgumentCaptor.forClass(RuntimeRewriteBindingResolveRequest.class);
        verify(rewriteBindingService).resolveActive(resolveCaptor.capture());
        assertEquals("tenant-a", resolveCaptor.getValue().getTenantId());
        assertEquals(originalFingerprint, resolveCaptor.getValue().getSqlFingerprint());
        assertEquals(originalSql, resolveCaptor.getValue().getSqlText());
        assertNull(resolveCaptor.getValue().getDatasourceCode());

        ArgumentCaptor<GovernanceQueryExecutionHistoryWriteRequest> historyCaptor =
            ArgumentCaptor.forClass(GovernanceQueryExecutionHistoryWriteRequest.class);
        verify(governanceCapabilityClient).writeQueryExecutionHistory(historyCaptor.capture());
        GovernanceQueryExecutionHistoryWriteRequest historyRequest = historyCaptor.getValue();
        assertEquals(Boolean.TRUE, historyRequest.getRewriteApplied());
        assertEquals(originalSql, historyRequest.getSqlTemplate());
        assertEquals(recommendedSql, historyRequest.getBoundSql());
        assertEquals("rewrite-001", historyRequest.getRewriteRecordId());
        assertEquals("rwb-001", historyRequest.getRuntimeBindingId());
        assertEquals(Long.valueOf(3L), historyRequest.getRewriteRuleVersion());
        assertEquals("runtime-rewrite-v3", historyRequest.getRuntimeRuleVersion());
        assertEquals("ACTIVE", historyRequest.getRuntimeRewriteStatus());
        assertEquals("ACTIVE", historyRequest.getRewriteActivationStatusSnapshot());
        assertTrue(historyRequest.getBindingSummary().contains("\"runtimeBindingId\":\"rwb-001\""));
        assertTrue(historyRequest.getBindingSummary().contains("\"rewriteActivationStatusSnapshot\":\"ACTIVE\""));
        assertTrue(historyRequest.getQueryContext().contains("\"runtimeRuleVersion\":\"runtime-rewrite-v3\""));
    }

    @Test
    void shouldExecuteRecommendedSqlWhenDevelopmentRewriteDirectSuccessIsDisabled() {
        setRequestContext("tenant-a");
        QueryExecutionRuntimeRewriteBindingService rewriteBindingService =
            mock(QueryExecutionRuntimeRewriteBindingService.class);
        String originalSql = "SELECT * FROM orders";
        String recommendedSql = "SELECT id FROM orders";
        when(rewriteBindingService.resolveActive(any()))
            .thenReturn(activeRuntimeRewriteResponse(SqlFingerprintUtils.fingerprint(originalSql), recommendedSql));
        RecordingQueryExecutionAdapter adapter = new RecordingQueryExecutionAdapter();
        QueryExecutionApplicationService service =
            newService(adapter, mockGovernanceClient(), rewriteBindingService);

        QueryExecuteResponse response = service.executeSynchronously(baseRequest(originalSql));

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        assertEquals(recommendedSql, adapter.actualSql);
        assertEquals(recommendedSql, response.getMetadata().getActualSql());
        assertEquals("SIMULATED", response.getMetadata().getExecutionMode());
        assertTrue(response.getMetadata().isRewriteApplied());
        assertEquals("rwb-001", response.getMetadata().getRuntimeBindingId());
    }

    @Test
    void shouldKeepOriginalSqlWhenRuntimeRewriteBindingIsMissing() {
        setRequestContext("tenant-a");
        QueryExecutionRuntimeRewriteBindingService rewriteBindingService =
            mock(QueryExecutionRuntimeRewriteBindingService.class);
        when(rewriteBindingService.resolveActive(any())).thenReturn(missingRuntimeRewriteResponse());
        RecordingQueryExecutionAdapter adapter = new RecordingQueryExecutionAdapter();
        QueryExecutionApplicationService service =
            newService(adapter, mockGovernanceClient(), rewriteBindingService);

        QueryExecuteResponse response = service.executeSynchronously(baseRequest("SELECT * FROM orders"));

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        assertEquals("SELECT * FROM orders", adapter.actualSql);
        assertFalse(response.getMetadata().isRewriteApplied());
        assertEquals(Boolean.FALSE, response.getBindingSummary().get("rewriteApplied"));
        assertEquals("MISSING", response.getBindingSummary().get("runtimeRewriteStatus"));
    }

    @Test
    void shouldResolveRuntimeRewriteBindingWithEffectiveBiViewCatalogAndKeepRawMetadata() {
        setRequestContext("tenant-a");
        QueryExecutionRuntimeRewriteBindingService rewriteBindingService =
            mock(QueryExecutionRuntimeRewriteBindingService.class);
        when(rewriteBindingService.resolveActive(any())).thenReturn(missingRuntimeRewriteResponse());
        RecordingQueryExecutionAdapter adapter = new RecordingQueryExecutionAdapter();
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        QueryExecutionApplicationService service =
            newService(adapter, governanceCapabilityClient, rewriteBindingService);
        String rawSql = "SELECT * FROM BI_SALES_V.orders WHERE dt = DATE '2026-04-01'";
        String effectiveSql = "SELECT * FROM BI_SALES_HETU.orders WHERE dt = DATE '2026-04-01'";
        String effectiveFingerprint = SqlFingerprintUtils.fingerprint(effectiveSql);

        QueryExecuteResponse response = service.executeSynchronously(baseRequest(rawSql));

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        assertEquals(effectiveSql, adapter.actualSql);
        assertEquals(rawSql, response.getMetadata().getOriginalSql());
        assertEquals(effectiveSql, response.getMetadata().getActualSql());
        assertFalse(response.getMetadata().isRewriteApplied());
        assertEquals(effectiveFingerprint, response.getSqlFingerprint());

        ArgumentCaptor<RuntimeRewriteBindingResolveRequest> resolveCaptor =
            ArgumentCaptor.forClass(RuntimeRewriteBindingResolveRequest.class);
        verify(rewriteBindingService).resolveActive(resolveCaptor.capture());
        assertEquals(effectiveSql, resolveCaptor.getValue().getSqlText());
        assertEquals(effectiveFingerprint, resolveCaptor.getValue().getSqlFingerprint());

        ArgumentCaptor<GovernanceQueryExecutionHistoryWriteRequest> historyCaptor =
            ArgumentCaptor.forClass(GovernanceQueryExecutionHistoryWriteRequest.class);
        verify(governanceCapabilityClient).writeQueryExecutionHistory(historyCaptor.capture());
        assertEquals(rawSql, historyCaptor.getValue().getSqlText());
        assertEquals(effectiveSql, historyCaptor.getValue().getBoundSql());
        assertEquals(Boolean.FALSE, historyCaptor.getValue().getRewriteApplied());
    }

    @Test
    void shouldPassDatasourceCodeAndResolvedEngineToExecutionAdapter() {
        setRequestContext("tenant-a");
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        GovernanceJdbcRouteCandidate candidate = new GovernanceJdbcRouteCandidate();
        candidate.setEngineType("HETU");
        candidate.setDatasourceCode("hetu_main");
        candidate.setConnectionMode("JDBC");
        candidate.setEnabled(true);
        candidate.setHealthStatus("HEALTHY");
        GovernanceJdbcRouteResolveResponse routeResponse = new GovernanceJdbcRouteResolveResponse();
        routeResponse.setCandidates(Collections.singletonList(candidate));
        when(governanceCapabilityClient.resolveJdbcRoute(any())).thenReturn(routeResponse);
        RecordingQueryExecutionAdapter adapter = new RecordingQueryExecutionAdapter();
        QueryExecutionApplicationService service =
            newService(adapter, governanceCapabilityClient, (QueryExecutionRuntimeRewriteBindingService) null);
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders");
        request.setDatasourceType(DataSourceTypeEnum.AUTO);
        request.setDatasourceCode("hetu_main");

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        assertEquals(DataSourceTypeEnum.HETU, adapter.targetEngine);
        assertEquals(DataSourceTypeEnum.HETU, adapter.request.getDatasourceType());
        assertEquals("hetu_main", adapter.request.getDatasourceCode());
    }

    @Test
    void shouldPassDatasourceEvidenceWhenResolvingRuntimeRewriteBinding() {
        setRequestContext("tenant-a");
        QueryExecutionRuntimeRewriteBindingService rewriteBindingService =
            mock(QueryExecutionRuntimeRewriteBindingService.class);
        when(rewriteBindingService.resolveActive(any())).thenReturn(missingRuntimeRewriteResponse());
        QueryExecutionApplicationService service =
            newService(new RecordingQueryExecutionAdapter(), mockGovernanceClient(), rewriteBindingService);
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders");
        QueryContextDTO queryContext = new QueryContextDTO();
        queryContext.setDatabaseName("hetu_reporting");
        request.setQueryContext(queryContext);

        service.executeSynchronously(request);

        ArgumentCaptor<RuntimeRewriteBindingResolveRequest> resolveCaptor =
            ArgumentCaptor.forClass(RuntimeRewriteBindingResolveRequest.class);
        verify(rewriteBindingService).resolveActive(resolveCaptor.capture());
        assertEquals("hetu_reporting", resolveCaptor.getValue().getDatasourceCode());
    }

    @Test
    void shouldFallbackToOriginalSqlWhenRecommendedRewriteSqlIsNotReadonly() {
        setRequestContext("tenant-a");
        QueryExecutionRuntimeRewriteBindingService rewriteBindingService =
            mock(QueryExecutionRuntimeRewriteBindingService.class);
        String originalSql = "SELECT * FROM orders";
        when(rewriteBindingService.resolveActive(any())).thenReturn(
            activeRuntimeRewriteResponse(SqlFingerprintUtils.fingerprint(originalSql), "DELETE FROM orders")
        );
        RecordingQueryExecutionAdapter adapter = new RecordingQueryExecutionAdapter();
        QueryExecutionApplicationService service =
            newService(adapter, mockGovernanceClient(), rewriteBindingService);

        QueryExecuteResponse response = service.executeSynchronously(baseRequest(originalSql));

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        assertEquals(originalSql, adapter.actualSql);
        assertFalse(response.getMetadata().isRewriteApplied());
        assertEquals("RECOMMENDED_SQL_NOT_READONLY", response.getMetadata().getRewriteFallbackReason());
        assertEquals("rwb-001", response.getMetadata().getRuntimeBindingId());
    }

    @Test
    void shouldFallbackToOriginalSqlWhenRuntimeRewriteLookupFails() {
        setRequestContext("tenant-a");
        QueryExecutionRuntimeRewriteBindingService rewriteBindingService =
            mock(QueryExecutionRuntimeRewriteBindingService.class);
        when(rewriteBindingService.resolveActive(any()))
            .thenThrow(new IllegalStateException("runtime binding store 不可用"));
        RecordingQueryExecutionAdapter adapter = new RecordingQueryExecutionAdapter();
        QueryExecutionApplicationService service =
            newService(adapter, mockGovernanceClient(), rewriteBindingService);

        QueryExecuteResponse response = service.executeSynchronously(baseRequest("SELECT * FROM orders"));

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        assertEquals("SELECT * FROM orders", adapter.actualSql);
        assertFalse(response.getMetadata().isRewriteApplied());
        assertEquals("RUNTIME_REWRITE_RESOLVE_FAILED", response.getMetadata().getRewriteFallbackReason());
    }

    @Test
    void shouldReturnGovernedCacheHitAfterBackfillUnderSameSchemaVersion() {
        setRequestContext("tenant-a");
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        QueryExecutionCacheGovernanceRuntimeService cacheRuntimeService = new QueryExecutionCacheGovernanceRuntimeService();
        QueryExecutionCachePolicyApplyRequest applyRequest = new QueryExecutionCachePolicyApplyRequest();
        applyRequest.setTenantId("tenant-a");
        applyRequest.setPolicyId("cache-policy-001");
        applyRequest.setSqlFingerprint(com.company.sqlforge.common.utils.SqlFingerprintUtils.fingerprint("SELECT * FROM orders"));
        applyRequest.setDatasourceType("HETU");
        applyRequest.setSchemaVersion("schema-v1");
        applyRequest.setPolicyReason("approved governed cache");
        cacheRuntimeService.apply(applyRequest);

        QueryExecutionApplicationService service = new QueryExecutionApplicationService(
            new DeterministicQueryExecutionAdapter(),
            governanceCapabilityClient,
            new QueryExecutionMetricsRecorder(meterRegistry),
            new QueryExecutionAccelerationRuntimeService(),
            cacheRuntimeService
        );

        QueryExecuteRequest firstRequest = baseRequest("SELECT * FROM orders");
        firstRequest.setQueryContext(schemaVersionContext("schema-v1"));
        QueryExecuteResponse firstResponse = service.executeSynchronously(firstRequest);

        QueryExecuteRequest secondRequest = baseRequest("SELECT * FROM orders");
        secondRequest.setQueryContext(schemaVersionContext("schema-v1"));
        QueryExecuteResponse secondResponse = service.executeSynchronously(secondRequest);

        assertEquals("BACKFILLED", firstResponse.getMetadata().getCacheGovernanceStatus());
        assertFalse(firstResponse.getMetadata().isCacheHit());
        assertEquals("HIT", secondResponse.getMetadata().getCacheGovernanceStatus());
        assertTrue(secondResponse.getMetadata().isCacheHit());
        assertTrue(secondResponse.getMetadata().getCacheGovernanceEvidence().contains("schemaVersion=schema-v1"));
        assertEquals(1.0D, meterRegistry.get("sqlforge.query.execution.cache.governance").tags(
            "target_engine", "HETU",
            "status", "BACKFILLED",
            "event", "BACKFILL",
            "risk_code", "NONE",
            "eviction_reason", "NONE"
        ).counter().count());
        assertEquals(1.0D, meterRegistry.get("sqlforge.query.execution.cache.governance").tags(
            "target_engine", "HETU",
            "status", "BACKFILLED",
            "event", "MISS",
            "risk_code", "NONE",
            "eviction_reason", "NONE"
        ).counter().count());
        assertEquals(1.0D, meterRegistry.get("sqlforge.query.execution.cache.governance").tags(
            "target_engine", "HETU",
            "status", "HIT",
            "event", "HIT",
            "risk_code", "NONE",
            "eviction_reason", "NONE"
        ).counter().count());
    }

    @Test
    void shouldBypassThenInvalidateGovernedCacheWhenSchemaVersionChanges() {
        setRequestContext("tenant-a");
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        QueryExecutionCacheGovernanceRuntimeService cacheRuntimeService = new QueryExecutionCacheGovernanceRuntimeService();
        QueryExecutionCachePolicyApplyRequest applyRequest = new QueryExecutionCachePolicyApplyRequest();
        applyRequest.setTenantId("tenant-a");
        applyRequest.setPolicyId("cache-policy-001");
        applyRequest.setSqlFingerprint(com.company.sqlforge.common.utils.SqlFingerprintUtils.fingerprint("SELECT * FROM orders"));
        applyRequest.setDatasourceType("HETU");
        applyRequest.setSchemaVersion("schema-v1");
        cacheRuntimeService.apply(applyRequest);

        QueryExecutionApplicationService service = new QueryExecutionApplicationService(
            new DeterministicQueryExecutionAdapter(),
            governanceCapabilityClient,
            QueryExecutionMetricsRecorder.noop(),
            new QueryExecutionAccelerationRuntimeService(),
            cacheRuntimeService
        );

        QueryExecuteRequest missingSchemaRequest = baseRequest("SELECT * FROM orders");
        missingSchemaRequest.setQueryContext(new QueryContextDTO());
        QueryExecuteResponse bypassResponse = service.executeSynchronously(missingSchemaRequest);

        QueryExecuteRequest versionOneRequest = baseRequest("SELECT * FROM orders");
        versionOneRequest.setQueryContext(schemaVersionContext("schema-v1"));
        QueryExecuteResponse firstVersionedResponse = service.executeSynchronously(versionOneRequest);

        QueryExecuteRequest versionTwoRequest = baseRequest("SELECT * FROM orders");
        versionTwoRequest.setQueryContext(schemaVersionContext("schema-v2"));
        QueryExecuteResponse invalidatedResponse = service.executeSynchronously(versionTwoRequest);

        assertEquals("BYPASSED", bypassResponse.getMetadata().getCacheGovernanceStatus());
        assertTrue(bypassResponse.getMetadata().getCacheGovernanceEvidence().contains("riskCode=SCHEMA_VERSION_MISSING"));
        assertEquals("BACKFILLED", firstVersionedResponse.getMetadata().getCacheGovernanceStatus());
        assertEquals("BACKFILLED", invalidatedResponse.getMetadata().getCacheGovernanceStatus());
        assertTrue(invalidatedResponse.getMetadata().getCacheGovernanceEvidence().contains("riskCode=SCHEMA_VERSION_MISMATCH"));
        assertTrue(invalidatedResponse.getMetadata().getCacheGovernanceEvidence().contains("schemaVersion=schema-v2"));
    }

    @Test
    void shouldReturnTimeoutWhenFailFastThresholdIsExceeded() {
        setRequestContext("tenant-a");
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new DeterministicQueryExecutionAdapter(), mockGovernanceClient());
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders");
        request.setQueryContext(timeoutContext(30L));
        request.setFaultToleranceStrategy(FaultToleranceStrategy.FAIL_FAST);

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.TIMEOUT, response.getStatus());
        assertNotNull(response.getError());
        assertEquals(ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ENGINE_TIMEOUT, response.getError().getCode());
        assertEquals(1, response.getRetryPath().size());
        assertEquals("HETU", response.getRetryPath().get(0).getEngine());
        assertEquals("TIMEOUT", response.getRetryPath().get(0).getResultStatus());
        assertEquals("LOCAL_TIMEOUT_ROLLBACK_MARKED", response.getRetryPath().get(0).getLocalRecoveryMarker());
        assertEquals("CLOSE_PRIMARY_ATTEMPT_CONTEXT", response.getRetryPath().get(0).getLocalRecoveryAction());
    }

    @Test
    void shouldRejectNonReadonlySqlBeforeExecution() {
        setRequestContext("tenant-a");
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new DeterministicQueryExecutionAdapter(), mockGovernanceClient());
        QueryExecuteRequest request = baseRequest("DELETE FROM orders");

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.FAILED, response.getStatus());
        assertNotNull(response.getError());
        assertEquals(ErrorCodeConstants.QUERY_EXECUTION_RISK_REJECTED, response.getError().getCode());
        assertTrue(response.getRows().isEmpty());
        assertFalse(response.isDegraded());
    }

    @Test
    void shouldFallbackToHiveWhenRetryThenFallbackIsEnabled() {
        setRequestContext("tenant-a");
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        QueryExecutionApplicationService service =
            newService(new DeterministicQueryExecutionAdapter(), mockGovernanceClient(), meterRegistry);
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders");
        request.setQueryContext(timeoutContext(30L));
        request.setFaultToleranceStrategy(FaultToleranceStrategy.RETRY_THEN_FALLBACK);

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.PARTIAL, response.getStatus());
        assertTrue(response.isDegraded());
        assertEquals("HIVE", response.getMetadata().getTargetEngine());
        assertEquals(2, response.getRetryPath().size());
        assertEquals("HETU", response.getRetryPath().get(0).getEngine());
        assertEquals("HIVE", response.getRetryPath().get(1).getEngine());
        assertEquals("LOCAL_TIMEOUT_ROLLBACK_MARKED", response.getRetryPath().get(0).getLocalRecoveryMarker());
        assertEquals("LOCAL_FALLBACK_COMPENSATION_MARKED", response.getRetryPath().get(1).getLocalRecoveryMarker());
        assertEquals("RECORD_DEGRADED_RESULT", response.getRetryPath().get(1).getLocalRecoveryAction());
        assertEquals("HIVE_FALLBACK", response.getMetadata().getExecutionMode());
        assertEquals(1, response.getMetadata().getAttemptedModes().size());
        assertEquals("HIVE_FALLBACK", response.getMetadata().getAttemptedModes().get(0));
        assertEquals(1, response.getMetadata().getRowCount());
        assertNull(response.getError());
        assertEquals(1.0D, meterRegistry.get("sqlforge.query.execution.fallbacks").tags(
            "requested_datasource", "HETU",
            "target_engine", "HIVE",
            "fault_tolerance", "RETRY_THEN_FALLBACK",
            "execution_mode", "HIVE_FALLBACK"
        ).counter().count());
        assertEquals(1.0D, meterRegistry.get("sqlforge.query.execution.timeouts").tags(
            "requested_datasource", "HETU",
            "target_engine", "HIVE",
            "fault_tolerance", "RETRY_THEN_FALLBACK"
        ).counter().count());
    }

    @Test
    void shouldReturnStructuredRouteUnavailableWhenHetuChainIsNotAvailable() {
        setRequestContext("tenant-a");
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        QueryExecutionApplicationService service =
            newService(new QueryExecutionAdapter() {
                @Override
                public QueryExecutionStep execute(DataSourceTypeEnum targetEngine,
                                                  String actualSql,
                                                  QueryExecuteRequest request,
                                                  boolean degradedPath) {
                    throw new HetuExecutionUnavailableException(
                        "当前环境已禁用 Hetu 执行链路",
                        java.util.Collections.singletonList("CHAIN_DISABLED"),
                        "REPO_CLOSED_BASELINE",
                        java.util.Arrays.asList("JDBC", "REST", "CLIENT"),
                        "REPO_CLOSED_CONFIGURATION",
                        "PENDING_ENV_WINDOW"
                    );
                }
            }, mockGovernanceClient(), meterRegistry);

        QueryExecuteResponse response = service.executeSynchronously(baseRequest("SELECT * FROM orders"));

        assertEquals(QueryExecutionStatus.FAILED, response.getStatus());
        assertEquals(ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ROUTE_UNAVAILABLE, response.getError().getCode());
        assertEquals(java.util.Collections.singletonList("CHAIN_DISABLED"), response.getMetadata().getAttemptedModes());
        assertEquals("REPO_CLOSED_BASELINE", response.getMetadata().getRouteProfile());
        assertEquals(java.util.Arrays.asList("JDBC", "REST", "CLIENT"), response.getMetadata().getRouteOrder());
        assertEquals("REPO_CLOSED_CONFIGURATION", response.getMetadata().getRouteEvidenceSource());
        assertEquals(1, response.getRetryPath().size());
        assertEquals("LOCAL_PRIMARY_ROUTE_FAILURE_MARKED", response.getRetryPath().get(0).getLocalRecoveryMarker());
        assertEquals(1.0D, meterRegistry.get("sqlforge.query.execution.route_不可用").tags(
            "requested_datasource", "HETU",
            "target_engine", "HETU",
            "fault_tolerance", "FAIL_FAST"
        ).counter().count());
        assertEquals(1.0D, meterRegistry.get("sqlforge.query.execution.mode.attempts").tags(
            "requested_datasource", "HETU",
            "mode", "CHAIN",
            "outcome", "DISABLED"
        ).counter().count());
    }

    @Test
    void shouldFallbackToHiveWhenHetuModeChainFailsAndFallbackIsEnabled() {
        setRequestContext("tenant-a");
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new QueryExecutionAdapter() {
                @Override
                public QueryExecutionStep execute(DataSourceTypeEnum targetEngine,
                                                  String actualSql,
                                                  QueryExecuteRequest request,
                                                  boolean degradedPath) {
                    if (targetEngine == DataSourceTypeEnum.HIVE) {
                        return new DeterministicQueryExecutionAdapter().execute(targetEngine, actualSql, request, degradedPath);
                    }
                    throw new HetuExecutionUnavailableException(
                        "已校准的 Hetu 执行模式均未成功，attemptedModes=[JDBC, JDBC:FAILED_EXECUTION, REST, REST:FAILED_EXECUTION]",
                        java.util.Arrays.asList("JDBC", "JDBC:FAILED_EXECUTION", "REST", "REST:FAILED_EXECUTION"),
                        "REPO_CLOSED_BASELINE",
                        java.util.Arrays.asList("JDBC", "REST"),
                        "REPO_CLOSED_CONFIGURATION",
                        "PENDING_ENV_WINDOW"
                    );
                }
            }, mockGovernanceClient());
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders");
        request.setFaultToleranceStrategy(FaultToleranceStrategy.RETRY_THEN_FALLBACK);

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.PARTIAL, response.getStatus());
        assertTrue(response.isDegraded());
        assertEquals("HIVE", response.getMetadata().getTargetEngine());
        assertEquals(2, response.getRetryPath().size());
        assertEquals("LOCAL_PRIMARY_ROUTE_FAILURE_MARKED", response.getRetryPath().get(0).getLocalRecoveryMarker());
        assertEquals("LOCAL_FALLBACK_COMPENSATION_MARKED", response.getRetryPath().get(1).getLocalRecoveryMarker());
    }

    @Test
    void shouldLogExceptionWhenExecutionAdapterFails(CapturedOutput output) {
        setRequestContext("tenant-a");
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new QueryExecutionAdapter() {
                @Override
                public QueryExecutionStep execute(DataSourceTypeEnum targetEngine,
                                                  String actualSql,
                                                  QueryExecuteRequest request,
                                                  boolean degradedPath) {
                    throw new IllegalStateException("simulated adapter failure");
                }
            }, governanceCapabilityClient);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.executeSynchronously(
            baseRequest("SELECT * FROM orders")
        ));

        assertEquals("simulated adapter failure", ex.getMessage());
        assertTrue(output.getOut().contains("status=FAILED phase=EXCEPTION"));
        assertTrue(output.getOut().contains("reason=simulated adapter failure"));
        verify(governanceCapabilityClient).writeQueryExecutionHistory(any());
    }

    @Test
    void shouldCallGovernanceChecksAndAuditOnSuccessfulExecution() {
        setRequestContext("tenant-a");
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new DeterministicQueryExecutionAdapter(), governanceCapabilityClient);

        QueryExecuteResponse response = service.executeSynchronously(baseRequest("SELECT * FROM orders"));

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        verify(governanceCapabilityClient).assertDatasourceAccess(
            org.mockito.Mockito.eq("tenant-a"),
            org.mockito.Mockito.eq(DataSourceTypeEnum.HETU),
            org.mockito.Mockito.eq("QUERY_EXECUTION_QUERY"),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.Mockito.eq("QUERY_EXECUTE_SYNC")
        );
        verify(governanceCapabilityClient).writeQueryExecutionHistory(any());
    }

    @Test
    void shouldNotFailQueryWhenHistoryWriteFails(CapturedOutput output) {
        setRequestContext("tenant-a");
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        when(governanceCapabilityClient.writeQueryExecutionHistory(any()))
            .thenThrow(new IllegalStateException("history route down"));
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new DeterministicQueryExecutionAdapter(), governanceCapabilityClient);

        QueryExecuteResponse response = service.executeSynchronously(baseRequest("SELECT * FROM orders"));

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        assertEquals("HETU", response.getMetadata().getTargetEngine());
        assertTrue(output.getOut().contains("status=HISTORY_WRITE_ASYNC_FAILED"));
        assertTrue(output.getOut().contains("history route down"));
        verify(governanceCapabilityClient).writeQueryExecutionHistory(any());
    }

    @Test
    void shouldIncludeHistoryProjectionFieldsInGovernanceWriteRequest() {
        setRequestContext("tenant-a");
        RequestMetadataContext.set("127.0.0.1", "JUnit", "api");
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new DeterministicQueryExecutionAdapter(), governanceCapabilityClient);

        service.executeSynchronously(baseRequest("SELECT * FROM orders"));

        ArgumentCaptor<GovernanceQueryExecutionHistoryWriteRequest> captor =
            ArgumentCaptor.forClass(GovernanceQueryExecutionHistoryWriteRequest.class);
        verify(governanceCapabilityClient).writeQueryExecutionHistory(captor.capture());
        GovernanceQueryExecutionHistoryWriteRequest historyRequest = captor.getValue();
        assertEquals("QUERY_EXECUTION", historyRequest.getHistoryType());
        assertEquals("SUCCESS", historyRequest.getResultStatus());
        assertEquals("HETU", historyRequest.getTargetEngine());
        assertEquals(Long.valueOf(1L), historyRequest.getReturnedRowCount());
        assertEquals(Boolean.FALSE, historyRequest.getCacheHit());
        assertEquals(Boolean.FALSE, historyRequest.getRewriteApplied());
        assertEquals(Boolean.FALSE, historyRequest.getAccelerationApplied());
        assertEquals("API", historyRequest.getAccessChannel());
        assertNotNull(historyRequest.getSqlText());
        assertNotNull(historyRequest.getSqlFingerprint());
        assertTrue(historyRequest.getRouteSummary().contains("\"selectedEngine\":\"HETU\""));
        assertTrue(historyRequest.getCacheSummary().contains("\"cacheHit\":false"));
        assertTrue(historyRequest.getQueryContext().contains("\"authSource\":\"header\""));
    }

    @Test
    void shouldPreserveSubmittedSqlTextInGovernanceHistoryWriteRequest() {
        setRequestContext("tenant-a");
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new DeterministicQueryExecutionAdapter(), governanceCapabilityClient);
        String submittedSql = "  SELECT * FROM orders /* keep original comment */;\n";

        service.executeSynchronously(baseRequest(submittedSql));

        ArgumentCaptor<GovernanceQueryExecutionHistoryWriteRequest> captor =
            ArgumentCaptor.forClass(GovernanceQueryExecutionHistoryWriteRequest.class);
        verify(governanceCapabilityClient).writeQueryExecutionHistory(captor.capture());
        GovernanceQueryExecutionHistoryWriteRequest historyRequest = captor.getValue();
        assertEquals(submittedSql, historyRequest.getSqlText());
        assertEquals("SELECT * FROM orders /* keep original comment */", historyRequest.getSqlTemplate());
        assertEquals("SELECT * FROM orders /* keep original comment */", historyRequest.getBoundSql());
    }

    @Test
    void shouldRewriteBiViewCatalogBeforeExecutionAndPreserveRawHistorySql() {
        setRequestContext("tenant-a");
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        RecordingQueryExecutionAdapter adapter = new RecordingQueryExecutionAdapter();
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(adapter, governanceCapabilityClient);
        String rawSql = "SELECT * FROM BI_SALES_V.orders WHERE dt = DATE '2026-04-01'";
        String effectiveSql = "SELECT * FROM BI_SALES_HETU.orders WHERE dt = DATE '2026-04-01'";

        QueryExecuteResponse response = service.executeSynchronously(baseRequest(rawSql));

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        assertEquals(effectiveSql, adapter.actualSql);
        assertEquals(effectiveSql, adapter.request.getSqlText());
        assertEquals(rawSql, response.getMetadata().getOriginalSql());
        assertEquals(effectiveSql, response.getMetadata().getActualSql());
        assertFalse(response.getMetadata().isRewriteApplied());
        assertEquals(SqlFingerprintUtils.fingerprint(effectiveSql), response.getSqlFingerprint());

        ArgumentCaptor<GovernanceQueryExecutionHistoryWriteRequest> captor =
            ArgumentCaptor.forClass(GovernanceQueryExecutionHistoryWriteRequest.class);
        verify(governanceCapabilityClient).writeQueryExecutionHistory(captor.capture());
        GovernanceQueryExecutionHistoryWriteRequest historyRequest = captor.getValue();
        assertEquals(rawSql, historyRequest.getSqlText());
        assertEquals(rawSql, historyRequest.getSqlTemplate());
        assertEquals(effectiveSql, historyRequest.getBoundSql());
        assertEquals(Boolean.FALSE, historyRequest.getRewriteApplied());
        assertEquals(SqlFingerprintUtils.fingerprint(effectiveSql), historyRequest.getSqlFingerprint());
    }

    @Test
    void shouldRejectSpoofedTenantBeforeLoggingOrGovernanceCall() {
        setRequestContext("tenant-a");
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new DeterministicQueryExecutionAdapter(), governanceCapabilityClient);
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders");
        request.setTenantId("tenant-b");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> service.executeSynchronously(request));

        assertEquals("请求 tenantId 与已认证租户上下文不一致", ex.getMessage());
        verify(governanceCapabilityClient, org.mockito.Mockito.never()).assertDatasourceAccess(any(), any(), any(), any(), any());
        verify(governanceCapabilityClient, org.mockito.Mockito.never()).writeAudit(any());
        verify(governanceCapabilityClient, org.mockito.Mockito.never()).writeQueryExecutionHistory(any());
    }

    private QueryExecuteRequest baseRequest(String sqlText) {
        QueryExecuteRequest request = new QueryExecuteRequest();
        request.setSqlText(sqlText);
        request.setTenantId("tenant-a");
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        return request;
    }

    private QueryContextDTO timeoutContext(Long timeoutMs) {
        QueryContextDTO queryContext = new QueryContextDTO();
        queryContext.setTimeoutMs(timeoutMs);
        return queryContext;
    }

    private QueryContextDTO schemaVersionContext(String schemaVersion) {
        QueryContextDTO queryContext = new QueryContextDTO();
        queryContext.setSchemaVersion(schemaVersion);
        return queryContext;
    }

    private GovernanceCapabilityClient mockGovernanceClient() {
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        doNothing().when(governanceCapabilityClient).assertDatasourceAccess(any(), any(), any(), any(), any());
        doNothing().when(governanceCapabilityClient).writeAudit(any());
        when(governanceCapabilityClient.writeQueryExecutionHistory(any()))
            .thenReturn(new GovernanceQueryExecutionHistoryWriteResponse());
        return governanceCapabilityClient;
    }

    private QueryExecutionApplicationService newService(QueryExecutionAdapter adapter,
                                                        GovernanceCapabilityClient governanceCapabilityClient,
                                                        SimpleMeterRegistry meterRegistry) {
        return new QueryExecutionApplicationService(
            adapter,
            governanceCapabilityClient,
            new QueryExecutionMetricsRecorder(meterRegistry),
            new QueryExecutionAccelerationRuntimeService()
        );
    }

    private QueryExecutionApplicationService newService(QueryExecutionAdapter adapter,
                                                        GovernanceCapabilityClient governanceCapabilityClient,
                                                        QueryExecutionRuntimeRewriteBindingService rewriteBindingService) {
        return new QueryExecutionApplicationService(
            adapter,
            governanceCapabilityClient,
            QueryExecutionMetricsRecorder.noop(),
            new QueryExecutionAccelerationRuntimeService(),
            new QueryExecutionCacheGovernanceRuntimeService(),
            rewriteBindingService
        );
    }

    private QueryExecutionApplicationService newService(QueryExecutionAdapter adapter,
                                                        GovernanceCapabilityClient governanceCapabilityClient,
                                                        QueryExecutionRuntimeRewriteBindingService rewriteBindingService,
                                                        QueryExecutionRewriteProperties rewriteProperties) {
        return new QueryExecutionApplicationService(
            adapter,
            governanceCapabilityClient,
            QueryExecutionMetricsRecorder.noop(),
            new QueryExecutionAccelerationRuntimeService(),
            new QueryExecutionCacheGovernanceRuntimeService(),
            rewriteBindingService,
            rewriteProperties
        );
    }

    private QueryExecutionRewriteProperties developmentDirectSuccessProperties() {
        QueryExecutionRewriteProperties properties = new QueryExecutionRewriteProperties();
        properties.setDevelopmentDirectSuccessEnabled(true);
        return properties;
    }

    private RuntimeRewriteBindingResponse activeRuntimeRewriteResponse(String sqlFingerprint, String recommendedSql) {
        RuntimeRewriteBindingResponse response = new RuntimeRewriteBindingResponse();
        response.setTenantId("tenant-a");
        response.setRuntimeBindingId("rwb-001");
        response.setRewriteRecordId("rewrite-001");
        response.setRecommendationId("recommendation-001");
        response.setSourceType("QUERY");
        response.setSourceKind("QUERY_HISTORY");
        response.setSourceId("history-001");
        response.setSqlFingerprint(sqlFingerprint);
        response.setOriginalSqlDigest("digest-original-001");
        response.setRecommendedSqlText(recommendedSql);
        response.setDatasourceCode("hetu_main");
        response.setStatus("ACTIVE");
        response.setActive(true);
        response.setRuleVersion(Long.valueOf(3L));
        response.setRuntimeRuleVersion("runtime-rewrite-v3");
        response.setRuntimeSummary("Active runtime rewrite binding was found.");
        return response;
    }

    private RuntimeRewriteBindingResponse missingRuntimeRewriteResponse() {
        RuntimeRewriteBindingResponse response = new RuntimeRewriteBindingResponse();
        response.setTenantId("tenant-a");
        response.setStatus("MISSING");
        response.setActive(false);
        response.setRuntimeSummary("No active runtime rewrite binding was found.");
        return response;
    }

    private static final class RecordingQueryExecutionAdapter implements QueryExecutionAdapter {
        private String actualSql;
        private DataSourceTypeEnum targetEngine;
        private QueryExecuteRequest request;

        @Override
        public QueryExecutionStep execute(DataSourceTypeEnum targetEngine,
                                          String actualSql,
                                          QueryExecuteRequest request,
                                          boolean degradedPath) {
            this.actualSql = actualSql;
            this.targetEngine = targetEngine;
            this.request = request;
            return new DeterministicQueryExecutionAdapter().execute(targetEngine, actualSql, request, degradedPath);
        }
    }

    private void setRequestContext(String tenantId) {
        RequestContext.set(
            tenantId,
            "user-001",
            "request-001",
            "trace-001",
            "header",
            1L,
            System.currentTimeMillis() + 60000L
        );
    }
}
