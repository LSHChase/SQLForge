package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.queryexecution.QueryExecutionMaterializedViewCreateRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionMaterializedViewCreateResponse;
import com.company.sqloptimization.application.controller.dto.MaterializedViewCreateRequest;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewriteActivationStatus;
import com.company.sqloptimization.domain.governance.RewriteRecordStatus;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.BenefitLevel;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationType;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RiskLevel;
import com.company.sqloptimization.domain.rewrite.SqlRewriteRecord;
import com.company.sqloptimization.infrastructure.queryexecution.QueryExecutionMaterializedViewClient;
import com.company.sqloptimization.infrastructure.repository.InMemoryAccelerationRecommendationRepository;
import com.company.sqloptimization.infrastructure.repository.InMemorySqlRewriteRecordRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MaterializedViewCreateApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldCreateGeneratedMvAndWriteRewriteEvidence() {
        setTenant("tenant-a");
        InMemoryAccelerationRecommendationRepository recommendationRepository =
            new InMemoryAccelerationRecommendationRepository();
        InMemorySqlRewriteRecordRepository rewriteRecordRepository = new InMemorySqlRewriteRecordRepository();
        recommendationRepository.save(recommendation("rec-001", "tenant-a", "GENERATED", Collections.emptyMap()));
        rewriteRecordRepository.saveRecord(rewriteRecord("rewrite-001", "tenant-a", "rec-001"));
        RecordingMaterializedViewClient client = new RecordingMaterializedViewClient("SUCCESS", "SUCCESS", "SUCCESS");
        MaterializedViewCreateApplicationService service =
            new MaterializedViewCreateApplicationService(recommendationRepository, rewriteRecordRepository, client);

        QueryExecutionMaterializedViewCreateResponse response =
            service.create("rec-001", request("tenant-a", "rewrite-001"));

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("tenant-a", client.lastRequest.getTenantId());
        assertEquals("rec-001", client.lastRequest.getRecommendationId());
        assertEquals("rewrite-001", client.lastRequest.getRewriteRecordId());
        assertEquals("mv_orders_customer", client.lastRequest.getMvName());
        assertEquals("CREATE MATERIALIZED VIEW mv_orders_customer AS SELECT 1", client.lastRequest.getDdlSql());
        assertEquals("REFRESH MATERIALIZED VIEW mv_orders_customer", client.lastRequest.getRefreshSql());
        Map<String, Object> evidence = evidence(rewriteRecordRepository, "rewrite-001");
        assertEquals("SUCCESS", evidence.get("status"));
        assertEquals("SUCCESS", evidence.get("ddlStatus"));
        assertEquals("SUCCESS", evidence.get("refreshStatus"));
        assertEquals("PARAMETERIZED_AGG_MV", evidence.get("mvType"));
    }

    @Test
    void shouldAllowReviewRequiredArtifactAfterButtonConsent() {
        setTenant("tenant-a");
        InMemoryAccelerationRecommendationRepository recommendationRepository =
            new InMemoryAccelerationRecommendationRepository();
        InMemorySqlRewriteRecordRepository rewriteRecordRepository = new InMemorySqlRewriteRecordRepository();
        recommendationRepository.save(recommendation("rec-001", "tenant-a", "REVIEW_REQUIRED", Collections.emptyMap()));
        RecordingMaterializedViewClient client =
            new RecordingMaterializedViewClient("PARTIAL_SUCCESS", "SUCCESS", "FAILED");
        MaterializedViewCreateApplicationService service =
            new MaterializedViewCreateApplicationService(recommendationRepository, rewriteRecordRepository, client);

        QueryExecutionMaterializedViewCreateResponse response =
            service.create("rec-001", request("tenant-a", null));

        assertEquals("PARTIAL_SUCCESS", response.getStatus());
        assertEquals("REVIEW_REQUIRED", recommendationRepository
            .findByRecommendationId("rec-001")
            .getAccelerationArtifact()
            .get("artifactStatus"));
        assertEquals("manual consent", client.lastRequest.getReason());
    }

    @Test
    void shouldCreateMvForRewriteRecommendationWhenArtifactIsGenerated() {
        setTenant("tenant-a");
        InMemoryAccelerationRecommendationRepository recommendationRepository =
            new InMemoryAccelerationRecommendationRepository();
        InMemorySqlRewriteRecordRepository rewriteRecordRepository = new InMemorySqlRewriteRecordRepository();
        recommendationRepository.save(recommendation("rec-rewrite", "tenant-a", "GENERATED", Collections.emptyMap())
            .toBuilder()
            .recommendationType(RecommendationType.REWRITE)
            .targetEngine("AUTO")
            .build());
        RecordingMaterializedViewClient client = new RecordingMaterializedViewClient("SUCCESS", "SUCCESS", "SUCCESS");
        MaterializedViewCreateApplicationService service =
            new MaterializedViewCreateApplicationService(recommendationRepository, rewriteRecordRepository, client);

        QueryExecutionMaterializedViewCreateResponse response =
            service.create("rec-rewrite", request("tenant-a", null));

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("rec-rewrite", client.lastRequest.getRecommendationId());
        assertEquals("HETU", client.lastRequest.getTargetEngine());
        assertEquals("hetu_main", client.lastRequest.getTargetDatasource());
        assertEquals("CREATE MATERIALIZED VIEW mv_orders_customer AS SELECT 1", client.lastRequest.getDdlSql());
    }

    @Test
    void shouldRejectBlockedOrMissingSqlArtifact() {
        setTenant("tenant-a");
        InMemoryAccelerationRecommendationRepository recommendationRepository =
            new InMemoryAccelerationRecommendationRepository();
        InMemorySqlRewriteRecordRepository rewriteRecordRepository = new InMemorySqlRewriteRecordRepository();
        recommendationRepository.save(recommendation(
            "rec-blocked",
            "tenant-a",
            "BLOCKED",
            Collections.<String, Object>singletonMap("blockingReasons", Collections.singletonList(reason()))
        ));
        recommendationRepository.save(recommendation(
            "rec-missing-sql",
            "tenant-a",
            "GENERATED",
            Collections.<String, Object>singletonMap("refreshSql", "")
        ));
        MaterializedViewCreateApplicationService service =
            new MaterializedViewCreateApplicationService(
                recommendationRepository,
                rewriteRecordRepository,
                new RecordingMaterializedViewClient("SUCCESS", "SUCCESS", "SUCCESS")
            );

        assertThrows(BizException.class, () -> service.create("rec-blocked", request("tenant-a", null)));
        assertThrows(BizException.class, () -> service.create("rec-missing-sql", request("tenant-a", null)));
    }

    @Test
    void shouldRejectTenantMismatchAndWrongRewriteRecord() {
        setTenant("tenant-a");
        InMemoryAccelerationRecommendationRepository recommendationRepository =
            new InMemoryAccelerationRecommendationRepository();
        InMemorySqlRewriteRecordRepository rewriteRecordRepository = new InMemorySqlRewriteRecordRepository();
        recommendationRepository.save(recommendation("rec-001", "tenant-a", "GENERATED", Collections.emptyMap()));
        rewriteRecordRepository.saveRecord(rewriteRecord("rewrite-cross-tenant", "tenant-b", "rec-001"));
        rewriteRecordRepository.saveRecord(rewriteRecord("rewrite-wrong-rec", "tenant-a", "rec-other"));
        MaterializedViewCreateApplicationService service =
            new MaterializedViewCreateApplicationService(
                recommendationRepository,
                rewriteRecordRepository,
                new RecordingMaterializedViewClient("SUCCESS", "SUCCESS", "SUCCESS")
            );

        assertThrows(AccessDeniedException.class, () -> service.create(
            "rec-001",
            request("tenant-a", "rewrite-cross-tenant")
        ));
        assertThrows(BizException.class, () -> service.create(
            "rec-001",
            request("tenant-a", "rewrite-wrong-rec")
        ));
        assertThrows(AccessDeniedException.class, () -> service.create("rec-001", request("tenant-b", null)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> evidence(InMemorySqlRewriteRecordRepository repository, String rewriteRecordId) {
        Object value = repository.findRecordById(rewriteRecordId).getTraceRefs().get("materializedViewCreateEvidence");
        assertNotNull(value);
        return (Map<String, Object>) value;
    }

    private MaterializedViewCreateRequest request(String tenantId, String rewriteRecordId) {
        MaterializedViewCreateRequest request = new MaterializedViewCreateRequest();
        request.setTenantId(tenantId);
        request.setRewriteRecordId(rewriteRecordId);
        request.setReason("manual consent");
        return request;
    }

    private AccelerationRecommendation recommendation(String recommendationId,
                                                       String tenantId,
                                                       String artifactStatus,
                                                       Map<String, Object> overrides) {
        return AccelerationRecommendation.builder()
            .recommendationId(recommendationId)
            .tenantId(tenantId)
            .recommendationType(RecommendationType.ACCELERATION)
            .recommendedSqlText("SELECT * FROM mv_orders_customer")
            .targetEngine("HETU")
            .targetDatasource("hetu_main")
            .benefitLevel(BenefitLevel.HIGH)
            .riskLevel(RiskLevel.MEDIUM)
            .accelerationArtifact(artifact(artifactStatus, overrides))
            .createdAt(Instant.parse("2026-05-24T10:00:00Z"))
            .updatedAt(Instant.parse("2026-05-24T10:00:00Z"))
            .build();
    }

    private Map<String, Object> artifact(String artifactStatus, Map<String, Object> overrides) {
        Map<String, Object> artifact = new LinkedHashMap<String, Object>();
        artifact.put("rule", "PRECOMPUTE_MV");
        artifact.put("mvType", "PARAMETERIZED_AGG_MV");
        artifact.put("artifactStatus", artifactStatus);
        artifact.put("mvName", "mv_orders_customer");
        artifact.put("targetEngine", "HETU");
        artifact.put("targetDatasource", "hetu_main");
        artifact.put("ddlSql", "CREATE MATERIALIZED VIEW mv_orders_customer AS SELECT 1");
        artifact.put("refreshSql", "REFRESH MATERIALIZED VIEW mv_orders_customer");
        artifact.put("validationSql", "SELECT COUNT(*) FROM mv_orders_customer");
        artifact.put("rollbackSql", "DROP MATERIALIZED VIEW mv_orders_customer");
        artifact.put("rewriteSql", "SELECT * FROM mv_orders_customer");
        artifact.put("blockingReasons", Collections.emptyList());
        artifact.put("reviewWarnings", Collections.emptyList());
        artifact.putAll(overrides);
        return artifact;
    }

    private Map<String, Object> reason() {
        Map<String, Object> reason = new LinkedHashMap<String, Object>();
        reason.put("code", "UNSAFE");
        reason.put("description", "unsafe mv");
        return reason;
    }

    private SqlRewriteRecord rewriteRecord(String rewriteRecordId, String tenantId, String recommendationId) {
        return SqlRewriteRecord.builder()
            .rewriteRecordId(rewriteRecordId)
            .tenantId(tenantId)
            .recommendationId(recommendationId)
            .sourceType(GovernanceSourceType.PARSE)
            .sourceKind(GovernanceSourceKind.STRUCTURE_PARSE)
            .sourceId("parse-001")
            .evidenceLevel(EvidenceLevel.STATIC_PARSE)
            .sqlFingerprint("fp-001")
            .datasourceCode("hetu_main")
            .status(RewriteRecordStatus.DRAFT)
            .validationStatus(RewriteValidationStatus.NOT_VALIDATED)
            .activationStatus(RewriteActivationStatus.INACTIVE)
            .originalSqlText("SELECT 1")
            .recommendedSqlText("SELECT * FROM mv_orders_customer")
            .createdBy("user-001")
            .createdAt(Instant.parse("2026-05-24T10:00:00Z"))
            .updatedAt(Instant.parse("2026-05-24T10:00:00Z"))
            .traceRefs(Collections.<String, Object>emptyMap())
            .build();
    }

    private void setTenant(String tenantId) {
        RequestContext.set(
            tenantId,
            "user-001",
            "request-001",
            "trace-001",
            "header",
            1L,
            2L
        );
    }

    private static final class RecordingMaterializedViewClient implements QueryExecutionMaterializedViewClient {

        private final String status;
        private final String ddlStatus;
        private final String refreshStatus;
        private QueryExecutionMaterializedViewCreateRequest lastRequest;

        private RecordingMaterializedViewClient(String status, String ddlStatus, String refreshStatus) {
            this.status = status;
            this.ddlStatus = ddlStatus;
            this.refreshStatus = refreshStatus;
        }

        @Override
        public QueryExecutionMaterializedViewCreateResponse create(QueryExecutionMaterializedViewCreateRequest request) {
            this.lastRequest = request;
            QueryExecutionMaterializedViewCreateResponse response =
                new QueryExecutionMaterializedViewCreateResponse();
            response.setRecommendationId(request.getRecommendationId());
            response.setRewriteRecordId(request.getRewriteRecordId());
            response.setMvName(request.getMvName());
            response.setTargetEngine(request.getTargetEngine());
            response.setTargetDatasource(request.getTargetDatasource());
            response.setStatus(status);
            response.setDdlStatus(ddlStatus);
            response.setRefreshStatus(refreshStatus);
            response.setRuntimeSummary("runtime summary");
            response.setRuntimeDetailsJson("{\"detail\":\"ok\"}");
            return response;
        }
    }
}
