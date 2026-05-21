package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.application.service.QueryExecutionAccelerationRuntimeService;
import com.company.queryexecution.application.service.QueryExecutionApplicationService;
import com.company.queryexecution.application.service.QueryExecutionCacheGovernanceRuntimeService;
import com.company.queryexecution.application.service.QueryExecutionMetricsRecorder;
import com.company.queryexecution.application.service.QueryExecutionRuntimeRewriteBindingService;
import com.company.queryexecution.domain.query.QueryExecutionStatus;
import com.company.queryexecution.domain.rewrite.RuntimeRewriteBinding;
import com.company.queryexecution.domain.rewrite.RuntimeRewriteBindingStatus;
import com.company.queryexecution.domain.rewrite.repository.RuntimeRewriteBindingRepository;
import com.company.queryexecution.infrastructure.adapter.DeterministicQueryExecutionAdapter;
import com.company.queryexecution.infrastructure.governance.GovernanceCapabilityClient;
import com.company.queryexecution.infrastructure.governance.QueryExecutionAuditRecord;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.RequestMetadataContext;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteResponse;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingActivationRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResolveRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingStateChangeRequest;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import com.company.sqloptimization.application.controller.dto.AccelerationRecommendationCreateRequest;
import com.company.sqloptimization.application.controller.dto.RewriteValidationRunCreateRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordCreateRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordActivationActionRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordReviewRequest;
import com.company.sqloptimization.application.controller.vo.AccelerationRecommendationVO;
import com.company.sqloptimization.application.controller.vo.RewriteValidationRunVO;
import com.company.sqloptimization.application.controller.vo.SqlRewriteRecordVO;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewriteActivationStatus;
import com.company.sqloptimization.domain.governance.RewriteRecordStatus;
import com.company.sqloptimization.domain.governance.RewriteReviewStatus;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.BenefitLevel;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationStatus;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationType;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RiskLevel;
import com.company.sqloptimization.infrastructure.queryexecution.QueryExecutionResultDigestClient;
import com.company.sqloptimization.infrastructure.queryexecution.QueryExecutionRuntimeRewriteBindingClient;
import com.company.sqloptimization.infrastructure.repository.InMemoryAccelerationRecommendationRepository;
import com.company.sqloptimization.infrastructure.repository.InMemorySqlRewriteRecordRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProductionRewriteClosedLoopEndToEndTest {

    private static final String TENANT_ID = "tenant-a";
    private static final String HISTORY_ID = "history-prw-012";
    private static final String ORIGINAL_SQL =
        "SELECT * FROM orders WHERE query_date = '2026-05-12'";
    private static final String RECOMMENDED_SQL =
        "SELECT id FROM orders WHERE query_date = '2026-05-12'";
    private static final String SQL_FINGERPRINT = SqlFingerprintUtils.fingerprint(ORIGINAL_SQL);
    private static final String MV_ORIGINAL_SQL =
        "SELECT customer_id, SUM(amount) AS total_amount FROM orders GROUP BY customer_id";
    private static final String MV_REWRITE_SQL =
        "SELECT customer_id, SUM(total_amount) AS total_amount FROM mv_sales_daily GROUP BY customer_id";
    private static final String MV_SQL_FINGERPRINT = SqlFingerprintUtils.fingerprint(MV_ORIGINAL_SQL);

    @AfterEach
    void tearDown() {
        RequestContext.clear();
        RequestMetadataContext.clear();
    }

    @Test
    void shouldCloseProductionRewriteLoopThroughRuntimeBindingFlow() {
        setRequestContext("operator-001", "request-prw-012", "trace-prw-012");
        InMemoryRuntimeRewriteBindingRepository runtimeRepository = new InMemoryRuntimeRewriteBindingRepository();
        QueryExecutionRuntimeRewriteBindingService runtimeBindingService =
            new QueryExecutionRuntimeRewriteBindingService(runtimeRepository);
        RuntimeBindingClientBridge runtimeClient = new RuntimeBindingClientBridge(runtimeBindingService);
        SequencedResultDigestClient digestClient = new SequencedResultDigestClient(
            digest("schema-v1", Long.valueOf(1L), "checksum-equivalent", row("id", "1"), 100L, 1000L),
            digest("schema-v1", Long.valueOf(1L), "checksum-equivalent", row("id", "1"), 60L, 500L),
            digest("schema-v1", Long.valueOf(1L), "checksum-original", row("id", "1"), 100L, 1000L),
            digest("schema-v1", Long.valueOf(1L), "checksum-recommended", row("id", "2"), 60L, 500L)
        );
        SqlRewriteRecordApplicationService rewriteRecordService =
            new SqlRewriteRecordApplicationService(
                new InMemorySqlRewriteRecordRepository(),
                digestClient,
                new ResultDigestComparisonEngine(),
                runtimeClient
            );
        AccelerationRecommendationApplicationService recommendationService =
            new AccelerationRecommendationApplicationService(new InMemoryAccelerationRecommendationRepository());

        AccelerationRecommendationVO recommendation =
            recommendationService.createRecommendation(recommendationRequest());
        SqlRewriteRecordVO rewriteRecord =
            rewriteRecordService.createRewriteRecord(rewriteRecordRequest(recommendation));
        SqlRewriteRecordVO approved = rewriteRecordService.reviewRewriteRecord(
            rewriteRecord.getRewriteRecordId(),
            reviewRequest(RewriteReviewStatus.APPROVED, "equivalent production rewrite evidence")
        );
        RewriteValidationRunVO equivalentRun =
            rewriteRecordService.createValidationRun(approved.getRewriteRecordId(), validationRequest());
        SqlRewriteRecordVO published = rewriteRecordService.activateRewriteRecord(
            approved.getRewriteRecordId(),
            publishRequest("release approved production rewrite")
        );

        CapturingGovernanceCapabilityClient governanceClient = new CapturingGovernanceCapabilityClient();
        RecordingQueryExecutionAdapter queryAdapter = new RecordingQueryExecutionAdapter();
        QueryExecutionApplicationService queryExecutionService = new QueryExecutionApplicationService(
            queryAdapter,
            governanceClient,
            new QueryExecutionMetricsRecorder(new SimpleMeterRegistry()),
            new QueryExecutionAccelerationRuntimeService(),
            new QueryExecutionCacheGovernanceRuntimeService(),
            runtimeBindingService
        );
        QueryExecuteResponse executionResponse =
            queryExecutionService.executeSynchronously(queryRequest());

        setRequestContext("rewrite-validation-scheduler", "request-prw-012-pause", "trace-prw-012-pause");
        RewriteValidationRunVO divergedRun =
            rewriteRecordService.createValidationRun(published.getRewriteRecordId(), validationRequest());
        SqlRewriteRecordVO paused =
            rewriteRecordService.getRewriteRecord(published.getRewriteRecordId());

        assertEquals(recommendation.getRecommendationId(), rewriteRecord.getRecommendationId());
        assertEquals("APPROVED", approved.getReviewStatus());
        assertEquals("EQUIVALENT", equivalentRun.getComparisonStatus());
        assertEquals("ACTIVE", published.getActivationStatus());
        assertNotNull(published.getRuntimeBindingId());
        assertEquals("runtime-rewrite-v1", published.getRuntimeRuleVersion());
        assertEquals("ACTIVATE", lastActivationStatusAction(published));
        assertEquals("ACTIVE", lastActivationStatus(published));
        assertEquals(QueryExecutionStatus.SUCCESS, executionResponse.getStatus());
        assertEquals(RECOMMENDED_SQL, queryAdapter.getActualSql());
        assertTrue(executionResponse.getMetadata().isRewriteApplied());
        assertEquals(published.getRewriteRecordId(), executionResponse.getMetadata().getRewriteRecordId());
        assertEquals(published.getRuntimeBindingId(), executionResponse.getMetadata().getRuntimeBindingId());
        assertEquals("ACTIVE", executionResponse.getMetadata().getRewriteActivationStatusSnapshot());
        assertEquals(Boolean.TRUE, governanceClient.getLastHistoryRequest().getRewriteApplied());
        assertEquals(ORIGINAL_SQL, governanceClient.getLastHistoryRequest().getSqlTemplate());
        assertEquals(RECOMMENDED_SQL, governanceClient.getLastHistoryRequest().getBoundSql());
        assertEquals(published.getRewriteRecordId(), governanceClient.getLastHistoryRequest().getRewriteRecordId());
        assertEquals("DIVERGED", divergedRun.getComparisonStatus());
        assertEquals(Boolean.TRUE, divergedRun.getAutoApplyPaused());
        assertEquals("PAUSED", paused.getActivationStatus());
        assertEquals("AUTO_PAUSE", lastActivationStatusAction(paused));
        assertEquals("PAUSED", lastActivationStatus(paused));
        assertEquals("MISSING", runtimeBindingService.resolveActive(resolveRequest()).getStatus());
        assertEquals(4, digestClient.getRequestCount());
    }

    @Test
    void shouldCloseMaterializedViewRuntimeRewriteLoopThroughArtifactRewriteSql() {
        setRequestContext("operator-001", "request-mv-runtime", "trace-mv-runtime");
        InMemoryRuntimeRewriteBindingRepository runtimeRepository = new InMemoryRuntimeRewriteBindingRepository();
        QueryExecutionRuntimeRewriteBindingService runtimeBindingService =
            new QueryExecutionRuntimeRewriteBindingService(runtimeRepository);
        RuntimeBindingClientBridge runtimeClient = new RuntimeBindingClientBridge(runtimeBindingService);
        SequencedResultDigestClient digestClient = new SequencedResultDigestClient(
            digest("schema-mv", Long.valueOf(1L), "checksum-equivalent", row("customer_id", "1"), 120L, 1200L),
            digest("schema-mv", Long.valueOf(1L), "checksum-equivalent", row("customer_id", "1"), 50L, 200L)
        );
        SqlRewriteRecordApplicationService rewriteRecordService =
            new SqlRewriteRecordApplicationService(
                new InMemorySqlRewriteRecordRepository(),
                digestClient,
                new ResultDigestComparisonEngine(),
                runtimeClient
            );
        AccelerationRecommendationApplicationService recommendationService =
            new AccelerationRecommendationApplicationService(new InMemoryAccelerationRecommendationRepository());

        AccelerationRecommendationVO recommendation =
            recommendationService.createRecommendation(mvRecommendationRequest());
        SqlRewriteRecordVO rewriteRecord =
            rewriteRecordService.createRewriteRecord(mvRewriteRecordRequest(recommendation));
        SqlRewriteRecordVO approved = rewriteRecordService.reviewRewriteRecord(
            rewriteRecord.getRewriteRecordId(),
            reviewRequest(RewriteReviewStatus.APPROVED, "external MV DDL, refresh, and validation evidence approved")
        );
        RewriteValidationRunVO equivalentRun =
            rewriteRecordService.createValidationRun(approved.getRewriteRecordId(), validationRequest());
        SqlRewriteRecordVO published = rewriteRecordService.activateRewriteRecord(
            approved.getRewriteRecordId(),
            publishRequest("release MV runtime rewrite binding")
        );

        CapturingGovernanceCapabilityClient governanceClient =
            new CapturingGovernanceCapabilityClient(MV_SQL_FINGERPRINT);
        RecordingQueryExecutionAdapter queryAdapter = new RecordingQueryExecutionAdapter();
        QueryExecutionApplicationService queryExecutionService = new QueryExecutionApplicationService(
            queryAdapter,
            governanceClient,
            new QueryExecutionMetricsRecorder(new SimpleMeterRegistry()),
            new QueryExecutionAccelerationRuntimeService(),
            new QueryExecutionCacheGovernanceRuntimeService(),
            runtimeBindingService
        );
        QueryExecuteResponse executionResponse =
            queryExecutionService.executeSynchronously(queryRequest(MV_ORIGINAL_SQL));
        RuntimeRewriteBindingResponse activeBinding =
            runtimeBindingService.resolveActive(resolveRequest(MV_SQL_FINGERPRINT));

        assertEquals(MV_REWRITE_SQL, rewriteRecord.getRecommendedSqlText());
        assertEquals("APPROVED", approved.getReviewStatus());
        assertEquals(Boolean.TRUE, approved.getAutoApplyAllowed());
        assertEquals("EQUIVALENT", equivalentRun.getComparisonStatus());
        assertEquals("ACTIVE", published.getActivationStatus());
        assertNotNull(published.getRuntimeBindingId());
        assertEquals("ACTIVE", activeBinding.getStatus());
        assertEquals(published.getRuntimeBindingId(), activeBinding.getRuntimeBindingId());
        assertEquals(QueryExecutionStatus.SUCCESS, executionResponse.getStatus());
        assertEquals(MV_REWRITE_SQL, queryAdapter.getActualSql());
        assertTrue(executionResponse.getMetadata().isRewriteApplied());
        assertEquals(Boolean.TRUE, governanceClient.getLastHistoryRequest().getRewriteApplied());
        assertEquals(MV_ORIGINAL_SQL, governanceClient.getLastHistoryRequest().getSqlTemplate());
        assertEquals(MV_REWRITE_SQL, governanceClient.getLastHistoryRequest().getBoundSql());
        assertEquals(published.getRuntimeBindingId(), governanceClient.getLastHistoryRequest().getRuntimeBindingId());
    }

    private AccelerationRecommendationCreateRequest recommendationRequest() {
        AccelerationRecommendationCreateRequest request = new AccelerationRecommendationCreateRequest();
        request.setTenantId(TENANT_ID);
        request.setRecommendationType(RecommendationType.REWRITE);
        request.setHistoryId(HISTORY_ID);
        request.setSourceType(GovernanceSourceType.QUERY);
        request.setSourceKind(GovernanceSourceKind.QUERY_HISTORY);
        request.setSourceId(HISTORY_ID);
        request.setEvidenceLevel(EvidenceLevel.RUNTIME_HISTORY);
        request.setSqlFingerprint(SQL_FINGERPRINT);
        request.setSourceSqlText(ORIGINAL_SQL);
        request.setRecommendedSqlText(RECOMMENDED_SQL);
        request.setTargetEngine("HETU");
        request.setTargetDatasource("hetu_main");
        request.setReportCode("RPT_PRW_012");
        request.setLogicalObjectKey("TABLE:orders");
        request.setSummary("production rewrite closed-loop candidate");
        request.setReason("projection pruning");
        request.setExpectedGain("lower scanned bytes");
        request.setBenefitLevel(BenefitLevel.HIGH);
        request.setRiskLevel(RiskLevel.MEDIUM);
        request.setRiskSummary("requires human SQL rewrite approval");
        request.setRequiresDispatch(Boolean.FALSE);
        request.setStatus(RecommendationStatus.RECOMMENDED);
        request.setValidationStatus(RewriteValidationStatus.NOT_VALIDATED);
        request.setAutoApplyAllowed(Boolean.TRUE);
        request.setManualReviewRequired(Boolean.TRUE);
        return request;
    }

    private SqlRewriteRecordCreateRequest rewriteRecordRequest(AccelerationRecommendationVO recommendation) {
        SqlRewriteRecordCreateRequest request = new SqlRewriteRecordCreateRequest();
        request.setTenantId(TENANT_ID);
        request.setRecommendationId(recommendation.getRecommendationId());
        request.setSourceType(GovernanceSourceType.QUERY);
        request.setSourceKind(GovernanceSourceKind.QUERY_HISTORY);
        request.setSourceId(HISTORY_ID);
        request.setEvidenceLevel(EvidenceLevel.RUNTIME_HISTORY);
        request.setHistoryId(HISTORY_ID);
        request.setSqlFingerprint(SQL_FINGERPRINT);
        request.setDatasourceCode("hetu_main");
        request.setStatus(RewriteRecordStatus.READY);
        request.setValidationStatus(RewriteValidationStatus.NOT_VALIDATED);
        request.setActivationStatus(RewriteActivationStatus.INACTIVE);
        request.setAutoApplyAllowed(Boolean.TRUE);
        request.setManualReviewRequired(Boolean.TRUE);
        request.setValidationPolicyId("PRW_012_READONLY_DIGEST_POLICY");
        request.setOriginalSqlText(ORIGINAL_SQL);
        request.setRecommendedSqlText(RECOMMENDED_SQL);
        request.setTraceRefs(Collections.<String, Object>singletonMap("recommendationEvidence", "PRW-012"));
        return request;
    }

    private AccelerationRecommendationCreateRequest mvRecommendationRequest() {
        AccelerationRecommendationCreateRequest request = recommendationRequest();
        request.setRecommendationType(RecommendationType.ACCELERATION);
        request.setHistoryId("history-mv-runtime");
        request.setSourceId("history-mv-runtime");
        request.setSqlFingerprint(MV_SQL_FINGERPRINT);
        request.setSourceSqlText(MV_ORIGINAL_SQL);
        request.setRecommendedSqlText(MV_ORIGINAL_SQL);
        request.setReportCode("sales-daily");
        request.setSummary("MV precompute candidate");
        request.setReason("materialized view precompute");
        request.setLogicalObjectKey("sales-daily");
        request.setRuleChain(Collections.singletonList(rule("PRECOMPUTE_MV")));
        request.setAutoApplyAllowed(Boolean.FALSE);
        return request;
    }

    private SqlRewriteRecordCreateRequest mvRewriteRecordRequest(AccelerationRecommendationVO recommendation) {
        SqlRewriteRecordCreateRequest request = new SqlRewriteRecordCreateRequest();
        request.setTenantId(TENANT_ID);
        request.setRecommendationId(recommendation.getRecommendationId());
        request.setSourceType(GovernanceSourceType.QUERY);
        request.setSourceKind(GovernanceSourceKind.QUERY_HISTORY);
        request.setSourceId("history-mv-runtime");
        request.setEvidenceLevel(EvidenceLevel.RUNTIME_HISTORY);
        request.setHistoryId("history-mv-runtime");
        request.setSqlFingerprint(MV_SQL_FINGERPRINT);
        request.setDatasourceCode("hetu_main");
        request.setStatus(RewriteRecordStatus.READY);
        request.setValidationStatus(RewriteValidationStatus.NOT_VALIDATED);
        request.setActivationStatus(RewriteActivationStatus.INACTIVE);
        request.setAutoApplyAllowed(Boolean.FALSE);
        request.setManualReviewRequired(Boolean.TRUE);
        request.setValidationPolicyId("MV_RUNTIME_READONLY_DIGEST_POLICY");
        request.setOriginalSqlText(MV_ORIGINAL_SQL);
        request.setRecommendedSqlText(MV_REWRITE_SQL);
        request.setTraceRefs(Collections.<String, Object>singletonMap("accelerationArtifact", generatedMvArtifact()));
        return request;
    }

    private Map<String, Object> generatedMvArtifact() {
        Map<String, Object> artifact = new LinkedHashMap<String, Object>();
        artifact.put("rule", "PRECOMPUTE_MV");
        artifact.put("mvType", "PARAMETERIZED_AGG_MV");
        artifact.put("artifactStatus", "GENERATED");
        artifact.put("mvName", "mv_sales_daily");
        artifact.put("targetDatasource", "hetu_main");
        artifact.put("ddlSql", "CREATE MATERIALIZED VIEW mv_sales_daily AS\n"
            + "SELECT customer_id, dt, SUM(amount) AS total_amount FROM orders GROUP BY customer_id, dt");
        artifact.put("refreshSql", "REFRESH MATERIALIZED VIEW mv_sales_daily");
        artifact.put("validationSql", "WITH rewrite_result AS (" + MV_REWRITE_SQL + ") "
            + "SELECT COUNT(*) FROM rewrite_result");
        artifact.put("rewriteSql", MV_REWRITE_SQL);
        artifact.put("blockingReasons", Collections.emptyList());
        artifact.put("governanceBoundary", "PULL_ONLY_NOT_EXECUTED_BY_SQLFORGE");
        return artifact;
    }

    private Map<String, Object> rule(String ruleCode) {
        Map<String, Object> rule = new LinkedHashMap<String, Object>();
        rule.put("ruleCode", ruleCode);
        rule.put("rule", ruleCode);
        rule.put("status", "PULL_ONLY_CANDIDATE");
        return rule;
    }

    private SqlRewriteRecordReviewRequest reviewRequest(RewriteReviewStatus status, String note) {
        SqlRewriteRecordReviewRequest request = new SqlRewriteRecordReviewRequest();
        request.setTenantId(TENANT_ID);
        request.setReviewStatus(status);
        request.setReviewNote(note);
        return request;
    }

    private RewriteValidationRunCreateRequest validationRequest() {
        RewriteValidationRunCreateRequest request = new RewriteValidationRunCreateRequest();
        request.setTenantId(TENANT_ID);
        request.setTriggerReason("PRW-012 production closed-loop validation");
        return request;
    }

    private SqlRewriteRecordActivationActionRequest publishRequest(String reason) {
        SqlRewriteRecordActivationActionRequest request = new SqlRewriteRecordActivationActionRequest();
        request.setTenantId(TENANT_ID);
        request.setReason(reason);
        return request;
    }

    private QueryExecuteRequest queryRequest() {
        return queryRequest(ORIGINAL_SQL);
    }

    private QueryExecuteRequest queryRequest(String sqlText) {
        QueryExecuteRequest request = new QueryExecuteRequest();
        request.setTenantId(TENANT_ID);
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        request.setSqlText(sqlText);
        return request;
    }

    private RuntimeRewriteBindingResolveRequest resolveRequest() {
        return resolveRequest(SQL_FINGERPRINT);
    }

    private RuntimeRewriteBindingResolveRequest resolveRequest(String sqlFingerprint) {
        RuntimeRewriteBindingResolveRequest request = new RuntimeRewriteBindingResolveRequest();
        request.setTenantId(TENANT_ID);
        request.setSqlFingerprint(sqlFingerprint);
        return request;
    }

    private void setRequestContext(String userId, String requestId, String traceId) {
        RequestContext.set(
            TENANT_ID,
            userId,
            Arrays.asList("SERVICE"),
            requestId,
            traceId,
            "header",
            1L,
            2L
        );
    }

    private String lastActivationStatusAction(SqlRewriteRecordVO record) {
        return String.valueOf(lastActivationStatusTrace(record).get("action"));
    }

    private String lastActivationStatus(SqlRewriteRecordVO record) {
        return String.valueOf(lastActivationStatusTrace(record).get("activationStatus"));
    }

    private Map<?, ?> lastActivationStatusTrace(SqlRewriteRecordVO record) {
        Object pauseEvidence = record.getTraceRefs().get("pauseEvidence");
        if (pauseEvidence != null) {
            return (Map<?, ?>) pauseEvidence;
        }
        return (Map<?, ?>) record.getTraceRefs().get("activationEvidence");
    }

    private QueryExecutionResultDigestResponse digest(String schemaDigest,
                                                      Long rowCount,
                                                      String checksumDigest,
                                                      Map<String, Object> sampleRow,
                                                      long elapsedMs,
                                                      long scannedRows) {
        QueryExecutionResultDigestResponse response = new QueryExecutionResultDigestResponse();
        Map<String, Object> resultDigest = new LinkedHashMap<String, Object>();
        resultDigest.put("schemaDigest", schemaDigest);
        resultDigest.put("rowCount", rowCount);
        resultDigest.put("checksumDigest", checksumDigest);
        response.setTenantId(TENANT_ID);
        response.setStatus("SUCCESS");
        response.setTargetEngine("HETU");
        response.setResultDigest(resultDigest);
        response.setLimitedSample(Collections.singletonList(sampleRow));
        response.setExecutionEvidence(executionEvidence(elapsedMs, scannedRows));
        return response;
    }

    private Map<String, Object> executionEvidence(long elapsedMs, long scannedRows) {
        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("readonlyDigestOnly", Boolean.TRUE);
        evidence.put("elapsedMs", Long.valueOf(elapsedMs));
        evidence.put("scannedRows", Long.valueOf(scannedRows));
        return evidence;
    }

    private Map<String, Object> row(String key, String value) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put(key, value);
        return row;
    }

    private static final class RuntimeBindingClientBridge implements QueryExecutionRuntimeRewriteBindingClient {

        private final QueryExecutionRuntimeRewriteBindingService runtimeBindingService;

        private RuntimeBindingClientBridge(QueryExecutionRuntimeRewriteBindingService runtimeBindingService) {
            this.runtimeBindingService = runtimeBindingService;
        }

        @Override
        public RuntimeRewriteBindingResponse activate(RuntimeRewriteBindingActivationRequest request) {
            return runtimeBindingService.activate(request);
        }

        @Override
        public RuntimeRewriteBindingResponse pause(RuntimeRewriteBindingStateChangeRequest request) {
            return runtimeBindingService.pause(request);
        }

    }

    private static final class SequencedResultDigestClient implements QueryExecutionResultDigestClient {

        private final List<QueryExecutionResultDigestResponse> responses;
        private int requestCount;

        private SequencedResultDigestClient(QueryExecutionResultDigestResponse... responses) {
            this.responses = new ArrayList<QueryExecutionResultDigestResponse>(Arrays.asList(responses));
        }

        @Override
        public QueryExecutionResultDigestResponse executeDigest(QueryExecutionResultDigestRequest request) {
            QueryExecutionResultDigestResponse response = responses.get(requestCount);
            requestCount++;
            response.setTenantId(request.getTenantId());
            response.setValidationRunId(request.getValidationRunId());
            response.setRewriteRecordId(request.getRewriteRecordId());
            response.setSqlFingerprint(request.getSqlFingerprint());
            return response;
        }

        private int getRequestCount() {
            return requestCount;
        }
    }

    private static final class CapturingGovernanceCapabilityClient implements GovernanceCapabilityClient {

        private final String expectedSqlFingerprint;
        private GovernanceQueryExecutionHistoryWriteRequest lastHistoryRequest;

        private CapturingGovernanceCapabilityClient() {
            this(SQL_FINGERPRINT);
        }

        private CapturingGovernanceCapabilityClient(String expectedSqlFingerprint) {
            this.expectedSqlFingerprint = expectedSqlFingerprint;
        }

        @Override
        public void assertAuthorization(String tenantId,
                                        DataSourceTypeEnum datasourceType,
                                        String resourceType,
                                        String resourceId,
                                        String operationCode) {
            assertEquals(TENANT_ID, tenantId);
            assertEquals(DataSourceTypeEnum.HETU, datasourceType);
            assertEquals(expectedSqlFingerprint, resourceId);
        }

        @Override
        public void writeAudit(QueryExecutionAuditRecord auditRecord) {
        }

        @Override
        public GovernanceJdbcRouteResolveResponse resolveJdbcRoute(GovernanceJdbcRouteResolveRequest request) {
            return null;
        }

        @Override
        public GovernanceQueryExecutionHistoryWriteResponse writeQueryExecutionHistory(
            GovernanceQueryExecutionHistoryWriteRequest request
        ) {
            lastHistoryRequest = request;
            GovernanceQueryExecutionHistoryWriteResponse response = new GovernanceQueryExecutionHistoryWriteResponse();
            response.setHistoryId("history-written-prw-012");
            response.setTraceId(RequestContext.getTraceId());
            response.setRequestId(RequestContext.getRequestId());
            response.setContractStage("LONG_TERM_BASELINE");
            response.setImplementationStage("PRW_012_TEST_HISTORY_CAPTURE");
            return response;
        }

        private GovernanceQueryExecutionHistoryWriteRequest getLastHistoryRequest() {
            return lastHistoryRequest;
        }
    }

    private static final class RecordingQueryExecutionAdapter extends DeterministicQueryExecutionAdapter {

        private String actualSql;

        @Override
        public com.company.queryexecution.domain.query.QueryExecutionStep execute(
            DataSourceTypeEnum targetEngine,
            String actualSql,
            QueryExecuteRequest request,
            boolean degradedPath
        ) {
            this.actualSql = actualSql;
            return super.execute(targetEngine, actualSql, request, degradedPath);
        }

        private String getActualSql() {
            return actualSql;
        }
    }

    private static final class InMemoryRuntimeRewriteBindingRepository implements RuntimeRewriteBindingRepository {

        private final Map<String, RuntimeRewriteBinding> bindings =
            new LinkedHashMap<String, RuntimeRewriteBinding>();

        @Override
        public RuntimeRewriteBinding save(RuntimeRewriteBinding binding) {
            bindings.put(binding.getRuntimeBindingId(), binding);
            return binding;
        }

        @Override
        public RuntimeRewriteBinding findByRuntimeBindingId(String runtimeBindingId) {
            return bindings.get(runtimeBindingId);
        }

        @Override
        public RuntimeRewriteBinding findActiveByTenantIdAndSqlFingerprint(String tenantId, String sqlFingerprint) {
            for (RuntimeRewriteBinding binding : bindings.values()) {
                if (tenantId.equals(binding.getTenantId())
                    && sqlFingerprint.equals(binding.getSqlFingerprint())
                    && binding.getStatus() == RuntimeRewriteBindingStatus.ACTIVE) {
                    return binding;
                }
            }
            return null;
        }

        @Override
        public RuntimeRewriteBinding findLatestByTenantIdAndSqlFingerprint(String tenantId, String sqlFingerprint) {
            List<RuntimeRewriteBinding> matches = findByTenantIdAndSqlFingerprint(tenantId, sqlFingerprint);
            if (matches.isEmpty()) {
                return null;
            }
            matches.sort(Comparator.comparingLong(RuntimeRewriteBinding::getRuleVersion).reversed());
            return matches.get(0);
        }

        @Override
        public List<RuntimeRewriteBinding> findByTenantIdAndSqlFingerprint(String tenantId, String sqlFingerprint) {
            List<RuntimeRewriteBinding> matches = new ArrayList<RuntimeRewriteBinding>();
            for (RuntimeRewriteBinding binding : bindings.values()) {
                if (tenantId.equals(binding.getTenantId()) && sqlFingerprint.equals(binding.getSqlFingerprint())) {
                    matches.add(binding);
                }
            }
            return matches;
        }
    }
}
