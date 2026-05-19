package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqloptimization.application.controller.vo.RecommendationDiffVO;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationType;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SqlDiffApplicationServiceTest {

    private final SqlDiffApplicationService service =
        new SqlDiffApplicationService(new SqlOptimizationPipelineService());

    @Test
    void shouldReturnReadyTextAndRuleDiffForCountRewrite() {
        AccelerationRecommendation recommendation = recommendation(
            "SELECT COUNT(1) FROM orders",
            "SELECT COUNT(*) FROM orders",
            Collections.singletonList(rule("COUNT_ONE_TO_COUNT_STAR", "L0", "APPLIED_TO_CANDIDATE_SQL")),
            Collections.<Map<String, Object>>emptyList(),
            Collections.<Map<String, Object>>emptyList(),
            false
        );

        RecommendationDiffVO diff = service.buildRecommendationDiff(recommendation);

        assertEquals("READY", diff.getDiffStatus());
        assertFalse(diff.getTextDiff().isEmpty());
        assertEquals("REPLACE", diff.getTextDiff().get(0).get("type"));
        assertEquals("COUNT_ONE_TO_COUNT_STAR", diff.getRuleDiff().get(0).get("rule"));
        assertEquals("HUNK_REFERENCED", diff.getRuleDiff().get(0).get("highlightStatus"));
        assertEquals("BOTH_PARSED", diff.getAstSummaryDiff().get("parseStatus"));
        assertEquals("DISPLAY_ONLY_NOT_SEMANTIC_PROOF", diff.getDiffSummary().get("evidenceBoundary"));
        assertEquals(Boolean.FALSE, diff.getDiffSummary().get("autoApplyAllowed"));
    }

    @Test
    void shouldExposePrecomputeMvArtifactAndChineseRuleExplanation() {
        AccelerationRecommendation recommendation = AccelerationRecommendation.builder()
            .recommendationId("recommendation-mv")
            .tenantId("tenant-a")
            .recommendationType(RecommendationType.ACCELERATION)
            .sourceType(GovernanceSourceType.QUERY)
            .sourceKind(GovernanceSourceKind.QUERY_HISTORY)
            .sourceId("history-001")
            .evidenceLevel(EvidenceLevel.RUNTIME_HISTORY)
            .sqlFingerprint("fingerprint-001")
            .sourceSqlText("SELECT customer_id, SUM(amount) AS total_amount FROM orders GROUP BY customer_id")
            .recommendedSqlText("SELECT customer_id, SUM(amount) AS total_amount FROM orders GROUP BY customer_id")
            .targetEngine("HETU")
            .targetDatasource("datasource-a")
            .reportCode("sales-daily")
            .ruleChain(Collections.singletonList(rule("PRECOMPUTE_MV", "L2", "PULL_ONLY_CANDIDATE")))
            .manualReviewRequired(Boolean.TRUE)
            .autoApplyAllowed(false)
            .createdBy("operator-001")
            .createdAt(Instant.parse("2026-05-10T00:00:00Z"))
            .build();

        RecommendationDiffVO diff = service.buildRecommendationDiff(recommendation);

        assertEquals("物化视图预计算", diff.getRuleDiff().get(0).get("titleZh"));
        assertEquals("仅候选，需外部协同，不自动执行", diff.getRuleDiff().get(0).get("statusZh"));
        assertEquals("GENERATED", diff.getAccelerationArtifact().get("artifactStatus"));
        assertEquals("PARAMETERIZED_AGG_MV", diff.getAccelerationArtifact().get("mvType"));
        assertTrue(((List<?>) diff.getAccelerationArtifact().get("grain")).contains("customer_id"));
        assertFalse(((List<?>) diff.getAccelerationArtifact().get("measures")).isEmpty());
        assertTrue(String.valueOf(diff.getAccelerationArtifact().get("ddlSql")).contains("CREATE MATERIALIZED VIEW mv_sales_daily"));
        assertTrue(String.valueOf(diff.getAccelerationArtifact().get("rewriteSql")).contains("SELECT * FROM mv_sales_daily"));
        assertEquals("GENERATED", diff.getDiffSummary().get("accelerationArtifactStatus"));
    }

    @Test
    void shouldExposeAstSummaryChangesForGroupAndOrderRewrite() {
        AccelerationRecommendation recommendation = recommendation(
            "SELECT status, COUNT(*) FROM orders GROUP BY status, status ORDER BY status, status",
            "SELECT status, COUNT(*) FROM orders GROUP BY status ORDER BY status",
            Collections.singletonList(rule("DUPLICATE_GROUP_ORDER_KEY", "L0", "APPLIED_TO_CANDIDATE_SQL")),
            Collections.<Map<String, Object>>emptyList(),
            Collections.<Map<String, Object>>emptyList(),
            false
        );

        RecommendationDiffVO diff = service.buildRecommendationDiff(recommendation);
        List<?> categories = (List<?>) diff.getDiffSummary().get("astChangeCategories");

        assertEquals("READY", diff.getDiffStatus());
        assertTrue(categories.contains("GROUP"));
        assertTrue(categories.contains("ORDER"));
    }

    @Test
    void shouldKeepManualReviewRuleAsSummaryOnlyEvidence() {
        AccelerationRecommendation recommendation = recommendation(
            "SELECT * FROM orders WHERE YEAR(order_date) = 2026",
            "SELECT * FROM orders WHERE YEAR(order_date) = 2026",
            Collections.<Map<String, Object>>emptyList(),
            Collections.singletonList(rule("FUNCTION_PREDICATE_TO_RANGE", "L1", "NOT_APPLIED")),
            Collections.singletonList(risk("FUNCTION_PREDICATE_TO_RANGE")),
            true
        );

        RecommendationDiffVO diff = service.buildRecommendationDiff(recommendation);

        assertEquals("READY", diff.getDiffStatus());
        assertTrue(diff.getTextDiff().isEmpty());
        assertEquals(2, diff.getRuleDiff().size());
        assertEquals("SUMMARY_ONLY", diff.getRuleDiff().get(0).get("highlightStatus"));
        assertEquals(Boolean.TRUE, diff.getRuleDiff().get(0).get("manualReviewRequired"));
        assertEquals(Boolean.TRUE, diff.getDiffSummary().get("manualReviewRequired"));
    }

    @Test
    void shouldReturnTextDiffWhenAstSummaryCannotParseOneSide() {
        AccelerationRecommendation recommendation = recommendation(
            "SELECT FROM",
            "SELECT customer_id FROM orders",
            Collections.<Map<String, Object>>emptyList(),
            Collections.<Map<String, Object>>emptyList(),
            Collections.<Map<String, Object>>emptyList(),
            true
        );

        RecommendationDiffVO diff = service.buildRecommendationDiff(recommendation);

        assertEquals("TEXT_DIFF_READY_AST_WARNING", diff.getDiffStatus());
        assertFalse(diff.getTextDiff().isEmpty());
        assertEquals("ORIGINAL_PARSE_FAILED", diff.getAstSummaryDiff().get("parseStatus"));
        assertEquals(Boolean.TRUE, diff.getDiffSummary().get("textDiffReady"));
        assertEquals(Boolean.FALSE, diff.getDiffSummary().get("astSummaryReady"));
    }

    private AccelerationRecommendation recommendation(String originalSql,
                                                       String recommendedSql,
                                                       List<Map<String, Object>> ruleChain,
                                                       List<Map<String, Object>> unappliedRules,
                                                       List<Map<String, Object>> semanticRisks,
                                                       boolean manualReviewRequired) {
        return AccelerationRecommendation.builder()
            .recommendationId("recommendation-001")
            .tenantId("tenant-a")
            .recommendationType(RecommendationType.REWRITE)
            .sourceSqlId("source-sql-001")
            .historyId("history-001")
            .sourceType(GovernanceSourceType.QUERY)
            .sourceKind(GovernanceSourceKind.QUERY_HISTORY)
            .sourceId("history-001")
            .evidenceLevel(EvidenceLevel.RUNTIME_HISTORY)
            .sqlFingerprint("fingerprint-001")
            .sourceSqlText(originalSql)
            .recommendedSqlText(recommendedSql)
            .ruleChain(ruleChain)
            .unappliedRules(unappliedRules)
            .semanticRisks(semanticRisks)
            .manualReviewRequired(Boolean.valueOf(manualReviewRequired))
            .autoApplyAllowed(false)
            .createdBy("operator-001")
            .createdAt(Instant.parse("2026-05-10T00:00:00Z"))
            .build();
    }

    private Map<String, Object> rule(String rule, String level, String status) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("rule", rule);
        entry.put("level", level);
        entry.put("status", status);
        return entry;
    }

    private Map<String, Object> risk(String rule) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("rule", rule);
        entry.put("category", "SEMANTIC_EQUIVALENCE_RISK");
        entry.put("severity", "HIGH");
        return entry;
    }
}
