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
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingPublishRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResolveRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingStateChangeRequest;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import com.company.sqloptimization.application.controller.dto.AccelerationRecommendationCreateRequest;
import com.company.sqloptimization.application.controller.dto.RewriteValidationRunCreateRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordCreateRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordPublishActionRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordReviewRequest;
import com.company.sqloptimization.application.controller.vo.AccelerationRecommendationVO;
import com.company.sqloptimization.application.controller.vo.RewriteValidationRunVO;
import com.company.sqloptimization.application.controller.vo.SqlRewriteRecordVO;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewritePublishStatus;
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
        SqlRewriteRecordVO published = rewriteRecordService.publishRewriteRecord(
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
        assertEquals("PUBLISHED", published.getPublishStatus());
        assertNotNull(published.getRuntimeBindingId());
        assertEquals("runtime-rewrite-v1", published.getRuntimeRuleVersion());
        assertEquals("PUBLISH", lastPublishStatusAction(published));
        assertEquals("PUBLISHED", lastPublishStatus(published));
        assertEquals(QueryExecutionStatus.SUCCESS, executionResponse.getStatus());
        assertEquals(RECOMMENDED_SQL, queryAdapter.getActualSql());
        assertTrue(executionResponse.getMetadata().isRewriteApplied());
        assertEquals(published.getRewriteRecordId(), executionResponse.getMetadata().getRewriteRecordId());
        assertEquals(published.getRuntimeBindingId(), executionResponse.getMetadata().getRuntimeBindingId());
        assertEquals("PUBLISHED", executionResponse.getMetadata().getRewritePublishStatusSnapshot());
        assertEquals(Boolean.TRUE, governanceClient.getLastHistoryRequest().getRewriteApplied());
        assertEquals(ORIGINAL_SQL, governanceClient.getLastHistoryRequest().getSqlTemplate());
        assertEquals(RECOMMENDED_SQL, governanceClient.getLastHistoryRequest().getBoundSql());
        assertEquals(published.getRewriteRecordId(), governanceClient.getLastHistoryRequest().getRewriteRecordId());
        assertEquals("DIVERGED", divergedRun.getComparisonStatus());
        assertEquals(Boolean.TRUE, divergedRun.getAutoApplyPaused());
        assertEquals("PAUSED", paused.getPublishStatus());
        assertEquals("AUTO_PAUSE", lastPublishStatusAction(paused));
        assertEquals("PAUSED", lastPublishStatus(paused));
        assertEquals("MISSING", runtimeBindingService.resolveActive(resolveRequest()).getStatus());
        assertEquals(4, digestClient.getRequestCount());
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
        request.setStatus(RewriteRecordStatus.APPLIED);
        request.setValidationStatus(RewriteValidationStatus.NOT_VALIDATED);
        request.setPublishStatus(RewritePublishStatus.UNPUBLISHED);
        request.setAutoApplyAllowed(Boolean.TRUE);
        request.setManualReviewRequired(Boolean.TRUE);
        request.setValidationPolicyId("PRW_012_READONLY_DIGEST_POLICY");
        request.setOriginalSqlText(ORIGINAL_SQL);
        request.setRecommendedSqlText(RECOMMENDED_SQL);
        request.setTraceRefs(Collections.<String, Object>singletonMap("recommendationEvidence", "PRW-012"));
        return request;
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

    private SqlRewriteRecordPublishActionRequest publishRequest(String reason) {
        SqlRewriteRecordPublishActionRequest request = new SqlRewriteRecordPublishActionRequest();
        request.setTenantId(TENANT_ID);
        request.setReason(reason);
        return request;
    }

    private QueryExecuteRequest queryRequest() {
        QueryExecuteRequest request = new QueryExecuteRequest();
        request.setTenantId(TENANT_ID);
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        request.setSqlText(ORIGINAL_SQL);
        return request;
    }

    private RuntimeRewriteBindingResolveRequest resolveRequest() {
        RuntimeRewriteBindingResolveRequest request = new RuntimeRewriteBindingResolveRequest();
        request.setTenantId(TENANT_ID);
        request.setSqlFingerprint(SQL_FINGERPRINT);
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

    private String lastPublishStatusAction(SqlRewriteRecordVO record) {
        return String.valueOf(lastPublishStatusTrace(record).get("action"));
    }

    private String lastPublishStatus(SqlRewriteRecordVO record) {
        return String.valueOf(lastPublishStatusTrace(record).get("publishStatus"));
    }

    private Map<?, ?> lastPublishStatusTrace(SqlRewriteRecordVO record) {
        return (Map<?, ?>) record.getTraceRefs().get("lastPublishStatusTrace");
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
        public RuntimeRewriteBindingResponse publish(RuntimeRewriteBindingPublishRequest request) {
            return runtimeBindingService.publish(request);
        }

        @Override
        public RuntimeRewriteBindingResponse pause(RuntimeRewriteBindingStateChangeRequest request) {
            return runtimeBindingService.pause(request);
        }

        @Override
        public RuntimeRewriteBindingResponse unpublish(RuntimeRewriteBindingStateChangeRequest request) {
            return runtimeBindingService.unpublish(request);
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

        private GovernanceQueryExecutionHistoryWriteRequest lastHistoryRequest;

        @Override
        public void assertAuthorization(String tenantId,
                                        DataSourceTypeEnum datasourceType,
                                        String resourceType,
                                        String resourceId,
                                        String operationCode) {
            assertEquals(TENANT_ID, tenantId);
            assertEquals(DataSourceTypeEnum.HETU, datasourceType);
            assertEquals(SQL_FINGERPRINT, resourceId);
        }

        @Override
        public void writeAudit(QueryExecutionAuditRecord auditRecord) {
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
