package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.infrastructure.repository.InMemoryAccelerationRecommendationRepository;
import com.company.sqloptimization.infrastructure.repository.InMemorySqlParseHistoryRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class L2CommonSubgraphMvCandidateGeneratorTest {

    private final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    @Test
    void shouldGenerateCommonSubgraphMvForCteReport() {
        String sql = "WITH archived_orders AS (SELECT order_id FROM order_archive), "
            + "recent_orders AS (SELECT customer_id, amount, region, dt FROM orders WHERE status = 'PAID') "
            + "SELECT customer_id, SUM(amount) AS total_amount FROM recent_orders "
            + "WHERE region = 'CN' GROUP BY customer_id";

        Map<String, Object> artifact = artifact(sql);

        assertEquals("GENERATED", artifact.get("artifactStatus"), String.valueOf(artifact));
        assertEquals("COMMON_SUBGRAPH_MV", artifact.get("mvType"));
        Map<String, Object> evidence = map(artifact.get("commonSubgraphEvidence"));
        assertEquals("CTE", evidence.get("sourceKind"));
        assertEquals("recent_orders", evidence.get("sourceName"));
        assertEquals(Integer.valueOf(1), evidence.get("referenceCount"));

        String mvName = String.valueOf(artifact.get("mvName"));
        String ddlSql = String.valueOf(artifact.get("ddlSql"));
        assertTrue(ddlSql.contains("CREATE MATERIALIZED VIEW " + mvName + " AS"));
        assertTrue(
            ddlSql.contains("SELECT customer_id, amount, region, dt FROM orders WHERE status = 'PAID'"),
            ddlSql
        );
        assertFalse(ddlSql.contains(sql));

        String rewriteSql = String.valueOf(artifact.get("rewriteSql"));
        assertTrue(rewriteSql.contains("FROM " + mvName + " recent_orders"));
        assertTrue(rewriteSql.contains("SUM(amount) AS total_amount"));
        assertTrue(rewriteSql.contains("WHERE region = 'CN'"));
        assertFalse(rewriteSql.contains("WITH archived_orders"));
        assertFalse(rewriteSql.contains("FROM orders"));

        String validationSql = String.valueOf(artifact.get("validationSql"));
        assertTrue(validationSql.contains("original_result"));
        assertTrue(validationSql.contains("rewrite_result"));
        assertTrue(validationSql.contains("ROW_COUNT_CHECK"));
        assertTrue(validationSql.contains("COMMON_SUBGRAPH_OUTPUT_CHECK"));
        assertTrue(validationSql.contains("UPPER_REWRITE_RESULT_CHECK"));
    }

    @Test
    void shouldGenerateCommonSubgraphMvForDerivedTableSource() {
        String sql = "SELECT d.customer_id, SUM(d.amount) AS total_amount "
            + "FROM (SELECT customer_id, amount, region FROM orders WHERE status = 'PAID') d "
            + "WHERE d.region = 'CN' GROUP BY d.customer_id";

        Map<String, Object> artifact = artifact(sql);

        assertEquals("GENERATED", artifact.get("artifactStatus"), String.valueOf(artifact));
        assertEquals("COMMON_SUBGRAPH_MV", artifact.get("mvType"));
        assertEquals("DERIVED_TABLE", map(artifact.get("commonSubgraphEvidence")).get("sourceKind"));
        assertTrue(String.valueOf(artifact.get("ddlSql")).contains(
            "SELECT customer_id, amount, region FROM orders WHERE status = 'PAID'"
        ));
        String rewriteSql = String.valueOf(artifact.get("rewriteSql"));
        assertTrue(rewriteSql.contains("FROM " + artifact.get("mvName") + " d"));
        assertFalse(rewriteSql.contains("FROM (SELECT"));
        assertFalse(rewriteSql.contains("FROM orders"));
    }

    @Test
    void shouldIncludeCrossSqlExactSubgraphEvidence() {
        String currentSql = "WITH recent_orders AS (SELECT customer_id, amount, region FROM orders "
            + "WHERE status = 'PAID') SELECT customer_id, SUM(amount) AS total_amount "
            + "FROM recent_orders WHERE region = 'CN' GROUP BY customer_id";
        String peerSql = "WITH paid_orders AS (SELECT customer_id, amount, region FROM orders "
            + "WHERE status = 'PAID') SELECT region, COUNT(*) AS order_count "
            + "FROM paid_orders GROUP BY region";

        Map<String, Object> artifact = artifactWithPeers(currentSql, peer(peerSql, "peer-fingerprint-002"));

        assertEquals("GENERATED", artifact.get("artifactStatus"));
        Map<String, Object> evidence = map(artifact.get("commonSubgraphEvidence"));
        assertEquals("CROSS_SQL_SHARED_SUBGRAPH", evidence.get("mode"));
        assertEquals(Integer.valueOf(2), evidence.get("referenceCount"));
        assertEquals(2, list(evidence.get("matchedSqlFingerprints")).size());
        assertTrue(String.valueOf(evidence.get("matchedSqlFingerprints")).contains("peer-fingerprint-002"));
    }

    @Test
    void applicationServiceShouldCollectBatchPeerRecommendationsForCrossSqlEvidence() {
        InMemoryAccelerationRecommendationRepository recommendationRepository =
            new InMemoryAccelerationRecommendationRepository();
        L2AccelerationArtifactApplicationService applicationService =
            new L2AccelerationArtifactApplicationService(
                service,
                recommendationRepository,
                new InMemorySqlParseHistoryRepository()
            );
        String currentSql = "WITH recent_orders AS (SELECT customer_id, amount, region FROM orders "
            + "WHERE status = 'PAID') SELECT customer_id, SUM(amount) AS total_amount "
            + "FROM recent_orders WHERE region = 'CN' GROUP BY customer_id";
        String peerSql = "WITH paid_orders AS (SELECT customer_id, amount, region FROM orders "
            + "WHERE status = 'PAID') SELECT region, COUNT(*) AS order_count "
            + "FROM paid_orders GROUP BY region";
        recommendationRepository.save(recommendation("rec-peer", peerSql, "peer-fingerprint-002"));

        Map<String, Object> artifact = applicationService.buildForRecommendation(
            recommendation("rec-current", currentSql, "current-fingerprint-001")
        );

        assertEquals("GENERATED", artifact.get("artifactStatus"));
        Map<String, Object> evidence = map(artifact.get("commonSubgraphEvidence"));
        assertEquals("CROSS_SQL_SHARED_SUBGRAPH", evidence.get("mode"));
        assertEquals(Integer.valueOf(2), evidence.get("referenceCount"));
        assertTrue(String.valueOf(evidence.get("matchedSqlFingerprints")).contains("peer-fingerprint-002"));
    }

    @Test
    void shouldBlockCorrelatedSubqueryAndRepeatedScalarSubqueryWithoutSql() {
        assertBlocked(
            artifact("SELECT c.customer_id, (SELECT SUM(o.amount) FROM orders o "
                + "WHERE o.customer_id = c.customer_id) AS total_amount FROM customers c"),
            "CORRELATED_SUBQUERY_COMMON_SUBGRAPH_UNSUPPORTED"
        );

        assertBlocked(
            artifact("SELECT customer_id, "
                + "(SELECT MAX(score) FROM score_snapshot) AS max_score, "
                + "(SELECT MAX(score) FROM score_snapshot) AS max_score_again "
                + "FROM customers"),
            "REPEATED_SUBQUERY_COMMON_SUBGRAPH_BLOCKED"
        );
    }

    @Test
    void shouldBlockRecursiveCteWindowNonDeterministicOrderLimitAndSelectStarWithoutSql() {
        assertBlocked(
            artifact("WITH RECURSIVE t(n) AS (SELECT 1 AS n) SELECT n FROM t"),
            "RECURSIVE_CTE_COMMON_SUBGRAPH_UNSUPPORTED"
        );
        assertBlocked(
            artifact("WITH ranked_orders AS (SELECT customer_id, amount, "
                + "ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY amount DESC) AS rn FROM orders) "
                + "SELECT customer_id, amount FROM ranked_orders WHERE rn = 1"),
            "WINDOW_FUNCTION_COMMON_SUBGRAPH_UNSUPPORTED"
        );
        assertBlocked(
            artifact("WITH sampled_orders AS (SELECT customer_id, amount, RAND() AS sample_key FROM orders) "
                + "SELECT customer_id, amount FROM sampled_orders WHERE sample_key > 0.5"),
            "NON_DETERMINISTIC_FUNCTION_COMMON_SUBGRAPH_UNSUPPORTED"
        );
        assertBlocked(
            artifact("WITH top_orders AS (SELECT customer_id, amount FROM orders ORDER BY amount DESC LIMIT 10) "
                + "SELECT customer_id, amount FROM top_orders"),
            "SUBGRAPH_ORDER_LIMIT_UNSUPPORTED"
        );
        assertBlocked(
            artifact("WITH recent_orders AS (SELECT * FROM orders) SELECT customer_id FROM recent_orders"),
            "EXPLICIT_PROJECTION_REQUIRED"
        );
    }

    @Test
    void shouldBlockWhenSubgraphOutputDoesNotCoverUpperQuery() {
        assertBlocked(
            artifact("WITH recent_orders AS (SELECT customer_id FROM orders) "
                + "SELECT customer_id, SUM(amount) AS total_amount FROM recent_orders GROUP BY customer_id"),
            "SUBGRAPH_OUTPUT_NOT_COVERED"
        );
    }

    private Map<String, Object> artifact(String sql) {
        return artifactWithPeers(sql);
    }

    private Map<String, Object> artifactWithPeers(
        String sql,
        L2AccelerationArtifactBuilder.CommonSubgraphPeerSql... peers
    ) {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(sql, DataSourceTypeEnum.HETU);
        List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> peerList =
            new ArrayList<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql>();
        if (peers != null) {
            for (L2AccelerationArtifactBuilder.CommonSubgraphPeerSql peer : peers) {
                peerList.add(peer);
            }
        }
        return L2AccelerationArtifactBuilder.buildForPrecomputeCandidate(
            new L2AccelerationArtifactBuilder.AccelerationRecommendationInput(
                sql,
                "HETU",
                "datasource-a",
                "fingerprint-001",
                "sales-daily",
                null,
                null,
                peerList
            ),
            profile
        );
    }

    private L2AccelerationArtifactBuilder.CommonSubgraphPeerSql peer(String sql, String fingerprint) {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(sql, DataSourceTypeEnum.HETU);
        return new L2AccelerationArtifactBuilder.CommonSubgraphPeerSql(
            sql,
            fingerprint,
            "RECOMMENDATION",
            "rec-peer-002",
            "sales-daily",
            profile.toAdvancedStructureProfile()
        );
    }

    private AccelerationRecommendation recommendation(String recommendationId, String sql, String fingerprint) {
        return AccelerationRecommendation.builder()
            .recommendationId(recommendationId)
            .tenantId("tenant-a")
            .recommendationType(AccelerationRecommendation.RecommendationType.ACCELERATION)
            .sqlFingerprint(fingerprint)
            .sourceSqlText(sql)
            .recommendedSqlText("SELECT 1")
            .targetEngine("HETU")
            .targetDatasource("datasource-a")
            .reportCode("sales-daily")
            .batchId("batch-001")
            .ruleChain(precomputeRuleChain())
            .createdBy("tester")
            .createdAt(Instant.parse("2026-05-20T00:00:00Z"))
            .build();
    }

    private List<Map<String, Object>> precomputeRuleChain() {
        LinkedHashMap<String, Object> rule = new LinkedHashMap<String, Object>();
        rule.put("rule", "PRECOMPUTE_MV");
        return Collections.<Map<String, Object>>singletonList(rule);
    }

    private static void assertBlocked(Map<String, Object> artifact, String reasonCode) {
        assertEquals("BLOCKED", artifact.get("artifactStatus"));
        assertEquals("COMMON_SUBGRAPH_MV", artifact.get("mvType"));
        assertTrue(hasReason(maps(artifact.get("blockingReasons")), reasonCode), String.valueOf(artifact));
        assertNull(artifact.get("ddlSql"));
        assertNull(artifact.get("rewriteSql"));
    }

    private static boolean hasReason(List<Map<String, Object>> reasons, String code) {
        for (Map<String, Object> reason : reasons) {
            if (code.equals(reason.get("code"))) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) {
        return (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return (List<Object>) value;
    }
}
