package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
            .createdBy("user-001")
            .createdAt(Instant.parse("2026-05-10T00:00:00Z"))
            .build();

        RecommendationDiffVO diff = service.buildRecommendationDiff(recommendation);

        assertEquals("物化视图预计算", diff.getRuleDiff().get(0).get("titleZh"));
        assertEquals("仅候选，需外部协同，不自动执行", diff.getRuleDiff().get(0).get("statusZh"));
        assertEquals("GENERATED", diff.getAccelerationArtifact().get("artifactStatus"));
        assertEquals("PARAMETERIZED_AGG_MV", diff.getAccelerationArtifact().get("mvType"));
        assertTrue(((List<?>) diff.getAccelerationArtifact().get("grain")).contains("customer_id"));
        assertFalse(((List<?>) diff.getAccelerationArtifact().get("measures")).isEmpty());
        String mvName = String.valueOf(diff.getAccelerationArtifact().get("mvName"));
        String ddlSql = String.valueOf(diff.getAccelerationArtifact().get("ddlSql"));
        String rewriteSql = String.valueOf(diff.getAccelerationArtifact().get("rewriteSql"));
        assertTrue(ddlSql.contains("CREATE MATERIALIZED VIEW " + mvName));
        assertTrue(rewriteSql.contains("FROM " + mvName));
        assertTrue(rewriteSql.contains("SUM(total_amount) AS total_amount"));
        assertFalse(rewriteSql.contains("SELECT * FROM " + mvName));
        assertEquals("GENERATED", diff.getDiffSummary().get("accelerationArtifactStatus"));
    }

    @Test
    void shouldPreferPersistedArtifactSnapshotAndRejectExactQueryMv() {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("rule", "PRECOMPUTE_MV");
        snapshot.put("mvType", "PREJOIN_MV");
        snapshot.put("artifactStatus", "REVIEW_REQUIRED");
        snapshot.put("mvName", "persisted_mv_snapshot");
        snapshot.put("grain", Collections.singletonList("dt"));
        snapshot.put("dimensions", Collections.singletonList("dt"));
        snapshot.put("measures", Collections.<Map<String, Object>>emptyList());
        snapshot.put("joinGraph", Collections.<Map<String, Object>>emptyList());
        snapshot.put("coverage", map("coversProjection", Boolean.TRUE));
        snapshot.put("blockingReasons", Collections.<Map<String, Object>>emptyList());
        snapshot.put("reviewWarnings", Collections.singletonList(map("code", "ROW_AMPLIFICATION_METADATA_MISSING")));
        AccelerationRecommendation persisted = mvRecommendation().toBuilder()
            .accelerationArtifact(snapshot)
            .build();

        RecommendationDiffVO diff = service.buildRecommendationDiff(persisted);

        assertEquals("persisted_mv_snapshot", diff.getAccelerationArtifact().get("mvName"));
        assertEquals("PREJOIN_MV", diff.getAccelerationArtifact().get("mvType"));
        assertEquals("REVIEW_REQUIRED", diff.getAccelerationArtifact().get("artifactStatus"));
        assertEquals(
            "ROW_AMPLIFICATION_METADATA_MISSING",
            ((Map<?, ?>) ((List<?>) diff.getAccelerationArtifact().get("reviewWarnings")).get(0)).get("code")
        );
        assertEquals("REVIEW_REQUIRED", diff.getDiffSummary().get("accelerationArtifactStatus"));

        Map<String, Object> exactQuery = new LinkedHashMap<String, Object>();
        exactQuery.put("mvType", "EXACT_QUERY_MV");
        exactQuery.put("artifactStatus", "GENERATED");
        exactQuery.put("ddlSql", "CREATE MATERIALIZED VIEW mv_exact AS SELECT * FROM orders");
        exactQuery.put("refreshSql", "REFRESH MATERIALIZED VIEW mv_exact");
        exactQuery.put("validationSql", "SELECT 1");
        exactQuery.put("rollbackSql", "DROP MATERIALIZED VIEW mv_exact");
        exactQuery.put("rewriteSql", "SELECT * FROM mv_exact");
        RecommendationDiffVO exactDiff = service.buildRecommendationDiff(
            mvRecommendation().toBuilder().accelerationArtifact(exactQuery).build()
        );

        assertNull(exactDiff.getAccelerationArtifact());
        assertFalse(exactDiff.getDiffSummary().containsKey("accelerationArtifactStatus"));
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

    @Test
    void shouldSampleVeryLargeMultiLineSqlDiffWithoutExactMatrix() {
        AccelerationRecommendation recommendation = recommendation(
            largeProjectionSql("dim"),
            largeProjectionSql("mv_dim"),
            Collections.singletonList(rule("LARGE_SQL_REWRITE_DISPLAY", "L1", "APPLIED_TO_CANDIDATE_SQL")),
            Collections.<Map<String, Object>>emptyList(),
            Collections.<Map<String, Object>>emptyList(),
            true
        );

        RecommendationDiffVO diff = service.buildRecommendationDiff(recommendation);
        Map<String, Object> hunk = diff.getTextDiff().get(0);

        assertFalse(diff.getTextDiff().isEmpty());
        assertEquals(Boolean.TRUE, diff.getDiffSummary().get("largeDiffTruncated"));
        assertEquals(Boolean.TRUE, hunk.get("largeDiffTruncated"));
        assertEquals("LINE", hunk.get("granularity"));
        assertEquals("LARGE_SQL_CONTEXT_SAMPLE", hunk.get("diffPolicy"));
        assertTrue(((Integer) hunk.get("originalUnitCount")).intValue() > 1600);
        assertTrue(((Integer) hunk.get("recommendedUnitCount")).intValue() > 1600);
        assertTrue(((Integer) hunk.get("originalOmittedUnitCount")).intValue() > 0);
        assertTrue(((Integer) hunk.get("recommendedOmittedUnitCount")).intValue() > 0);
        assertTrue(String.valueOf(hunk.get("originalText")).contains("dim_0001"));
        assertTrue(String.valueOf(hunk.get("recommendedText")).contains("mv_dim_0001"));
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
            .createdBy("user-001")
            .createdAt(Instant.parse("2026-05-10T00:00:00Z"))
            .build();
    }

    private AccelerationRecommendation mvRecommendation() {
        return AccelerationRecommendation.builder()
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
            .createdBy("user-001")
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

    private Map<String, Object> map(String key, Object value) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put(key, value);
        return entry;
    }

    private Map<String, Object> risk(String rule) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("rule", rule);
        entry.put("category", "SEMANTIC_EQUIVALENCE_RISK");
        entry.put("severity", "HIGH");
        return entry;
    }

    private String largeProjectionSql(String prefix) {
        StringBuilder builder = new StringBuilder("SELECT\n");
        for (int i = 1; i <= 1800; i++) {
            builder.append("  ")
                .append(prefix)
                .append('_')
                .append(String.format("%04d", Integer.valueOf(i)))
                .append(i == 1800 ? "\n" : ",\n");
        }
        builder.append("FROM orders\n");
        builder.append("WHERE dt = DATE '2026-05-01'");
        return builder.toString();
    }
}
