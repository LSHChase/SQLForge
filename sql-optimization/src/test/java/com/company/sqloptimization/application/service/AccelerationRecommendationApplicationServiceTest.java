package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqloptimization.application.controller.dto.AccelerationRecommendationCreateRequest;
import com.company.sqloptimization.application.controller.vo.AccelerationRecommendationVO;
import com.company.sqloptimization.application.controller.vo.RecommendationDiffVO;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.BenefitLevel;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationStatus;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationType;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RiskLevel;
import com.company.sqloptimization.infrastructure.repository.InMemoryAccelerationRecommendationRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AccelerationRecommendationApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldCreateReadonlyRecommendationWithBenefitAndRiskModel() {
        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            1L,
            2L
        );
        AccelerationRecommendationApplicationService service =
            new AccelerationRecommendationApplicationService(new InMemoryAccelerationRecommendationRepository());

        AccelerationRecommendationCreateRequest request = new AccelerationRecommendationCreateRequest();
        request.setTenantId("tenant-a");
        request.setRecommendationType(RecommendationType.CREATE_TABLE);
        request.setSourceSqlId("parse-item-001");
        request.setHistoryId("history-001");
        request.setParseTaskId("parse-task-001");
        request.setBatchId("batch-001");
        request.setRouteDecisionId("route-001");
        request.setAlertId("alert-001");
        request.setSqlFingerprint("fp-001");
        request.setSourceSqlText("SELECT COUNT(1) FROM sales");
        request.setRecommendedSqlText("CREATE TABLE agg_sales AS SELECT * FROM sales");
        request.setTargetEngine("trino");
        request.setTargetDatasource("hetu_main");
        request.setReportCode("RPT_SALES_DAILY");
        request.setLogicalObjectKey("BUSINESS_VIEW:sales_daily");
        request.setExpectedGain("Reduce repeated report runtime by precomputing daily sales");
        request.setBenefitLevel(BenefitLevel.HIGH);
        request.setRiskLevel(RiskLevel.MEDIUM);
        request.setRequiresDispatch(Boolean.TRUE);
        request.setSourceType(GovernanceSourceType.PARSE);
        request.setSourceKind(GovernanceSourceKind.STRUCTURE_PARSE);
        request.setSourceId("parse-task-001");
        request.setEvidenceLevel(EvidenceLevel.STATIC_PARSE);
        request.setRuleChain(Collections.singletonList(rule("COUNT_ONE_TO_COUNT_STAR", "L0")));
        request.setUnappliedRules(Collections.singletonList(rule("SELECT_STAR_EXPANSION", "L1")));
        request.setPreconditions(Collections.singletonList(precondition("SELECT_STAR_EXPANSION")));
        request.setSemanticRisks(Collections.singletonList(risk("SELECT_STAR_EXPANSION")));
        request.setExpectedBenefit(map("claimBoundary", "NOT_REAL_EXECUTION_GAIN"));
        request.setEstimatedCost(map("validation", "RESULT_DIFF_REQUIRED"));
        request.setConfidence(Integer.valueOf(64));
        request.setValidationMethod("RESULT_DIFF_THEN_MANUAL_REVIEW");
        request.setAutoApplyAllowed(Boolean.FALSE);
        request.setManualReviewRequired(Boolean.TRUE);

        AccelerationRecommendationVO created = service.createRecommendation(request);
        List<AccelerationRecommendationVO> list = service.listRecommendations();
        AccelerationRecommendationVO detail = service.getRecommendation(created.getRecommendationId());
        RecommendationDiffVO diff = service.getRecommendationDiff(created.getRecommendationId());

        assertEquals("CREATE_TABLE", created.getRecommendationType());
        assertEquals("HIGH", created.getBenefitLevel());
        assertEquals("MEDIUM", created.getRiskLevel());
        assertEquals("history-001", created.getHistoryId());
        assertEquals("parse-task-001", created.getParseTaskId());
        assertEquals("batch-001", created.getBatchId());
        assertEquals("route-001", created.getRouteDecisionId());
        assertEquals("alert-001", created.getAlertId());
        assertEquals(Boolean.TRUE, created.getRequiresDispatch());
        assertEquals("RECOMMENDED", created.getStatus());
        assertEquals("PARSE", created.getSourceType());
        assertEquals("STRUCTURE_PARSE", created.getSourceKind());
        assertEquals("parse-task-001", created.getSourceId());
        assertEquals("STATIC_PARSE", created.getEvidenceLevel());
        assertEquals("SQL_RECOMMENDATION_RULE_MODEL_V1", created.getSchemaVersion());
        assertEquals("COUNT_ONE_TO_COUNT_STAR", created.getRuleChain().get(0).get("rule"));
        assertEquals("SELECT_STAR_EXPANSION", created.getUnappliedRules().get(0).get("rule"));
        assertEquals("NOT_REAL_EXECUTION_GAIN", created.getExpectedBenefit().get("claimBoundary"));
        assertEquals("RESULT_DIFF_REQUIRED", created.getEstimatedCost().get("validation"));
        assertEquals(Integer.valueOf(64), created.getConfidence());
        assertEquals("RESULT_DIFF_THEN_MANUAL_REVIEW", created.getValidationMethod());
        assertEquals("NOT_VALIDATED", created.getValidationStatus());
        assertEquals(Boolean.FALSE, created.getAutoApplyAllowed());
        assertEquals(Boolean.TRUE, created.getManualReviewRequired());
        assertEquals(1, list.size());
        assertEquals(created.getRecommendationId(), detail.getRecommendationId());
        assertEquals("TEXT_DIFF_READY_AST_WARNING", diff.getDiffStatus());
        assertEquals(Boolean.TRUE, diff.getDiffSummary().get("textDiffReady"));
        assertEquals("DISPLAY_ONLY_NOT_SEMANTIC_PROOF", diff.getDiffSummary().get("evidenceBoundary"));
        assertFalse(enumContainsExecuted(), "RecommendationStatus must not expose an executed state");
    }

    @Test
    void shouldRejectCrossTenantRecommendationCreation() {
        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            1L,
            2L
        );
        AccelerationRecommendationApplicationService service =
            new AccelerationRecommendationApplicationService(new InMemoryAccelerationRecommendationRepository());
        AccelerationRecommendationCreateRequest request = new AccelerationRecommendationCreateRequest();
        request.setTenantId("tenant-b");
        request.setRecommendationType(RecommendationType.REWRITE);
        request.setRecommendedSqlText("SELECT 1");

        assertThrows(AccessDeniedException.class, () -> service.createRecommendation(request));
    }

    private boolean enumContainsExecuted() {
        for (RecommendationStatus status : RecommendationStatus.values()) {
            if ("EXECUTED".equals(status.name())) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> rule(String rule, String level) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("rule", rule);
        entry.put("level", level);
        return entry;
    }

    private Map<String, Object> precondition(String rule) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("rule", rule);
        entry.put("code", "METADATA_REQUIRED");
        return entry;
    }

    private Map<String, Object> risk(String rule) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("rule", rule);
        entry.put("severity", "HIGH");
        return entry;
    }

    private Map<String, Object> map(String key, Object value) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put(key, value);
        return entry;
    }
}
