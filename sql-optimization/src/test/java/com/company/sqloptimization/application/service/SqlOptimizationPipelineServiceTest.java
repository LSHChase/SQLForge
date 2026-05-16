package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.parse.SqlParserMode;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationTaskSuggestion;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SqlOptimizationPipelineServiceTest {

    private final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    @Test
    void shouldBuildRealParseArtifactsFromSqlAst() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT * FROM orders o JOIN customer_profile c ON o.customer_id = c.customer_id "
                + "WHERE order_date >= DATE '2026-04-01' ORDER BY order_date",
            DataSourceTypeEnum.HETU
        );

        OptimizationTaskSuggestion suggestion = service.buildParseSuggestion(profile);

        assertTrue(suggestion.getSummary().contains("2 张表"));
        assertEquals("AST_PROFILE", suggestion.getArtifacts().get(0).getCategory());
        assertTrue(suggestion.getArtifacts().get(0).getContent().contains("\"joinCount\":1"));
        assertEquals("SELECT_STAR", suggestion.getRisks().get(0).getCategory());
    }

    @Test
    void shouldRewriteSqlWithSafeAstRules() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT COUNT(1), status FROM orders WHERE tenant_id = 1 AND tenant_id = 1 "
                + "GROUP BY status, status ORDER BY status, status",
            DataSourceTypeEnum.HETU
        );

        OptimizationTaskSuggestion suggestion = service.buildRewriteSuggestion(profile);
        String rewrittenSql = suggestion.getArtifacts().get(0).getContent();
        String ruleTrace = suggestion.getArtifacts().get(1).getContent();

        assertTrue(rewrittenSql.contains("GROUP BY status"));
        assertTrue(!rewrittenSql.contains("GROUP BY status, status"));
        assertTrue(!rewrittenSql.contains("ORDER BY status, status"));
        assertTrue(ruleTrace.contains("COUNT_LITERAL_TO_COUNT_STAR"));
        assertTrue(ruleTrace.contains("DEDUPLICATE_WHERE_PREDICATES"));
        assertTrue(ruleTrace.contains("DEDUPLICATE_GROUP_BY_KEYS"));
        assertTrue(ruleTrace.contains("DEDUPLICATE_ORDER_BY_KEYS"));
    }

    @Test
    void shouldNotRewriteCountNullToCountStar() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT COUNT(NULL) FROM orders",
            DataSourceTypeEnum.HETU
        );

        OptimizationTaskSuggestion suggestion = service.buildRewriteSuggestion(profile);
        String rewrittenSql = suggestion.getArtifacts().get(0).getContent().toUpperCase();

        assertTrue(service.deriveRewriteCandidateRules(profile).isEmpty());
        assertTrue(rewrittenSql.contains("COUNT(NULL)"));
        assertFalse(rewrittenSql.contains("COUNT(*)"));
    }

    @Test
    void shouldPreserveOrderDirectionWhenDeduplicatingGroupAndOrderKeys() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT status FROM orders GROUP BY status, status ORDER BY status ASC, status DESC, status ASC",
            DataSourceTypeEnum.HETU
        );

        OptimizationTaskSuggestion suggestion = service.buildRewriteSuggestion(profile);
        String rewrittenSql = suggestion.getArtifacts().get(0).getContent().toUpperCase();
        String ruleTrace = suggestion.getArtifacts().get(1).getContent();

        assertTrue(rewrittenSql.contains("GROUP BY STATUS"));
        assertFalse(rewrittenSql.contains("GROUP BY STATUS, STATUS"));
        assertTrue(rewrittenSql.contains("STATUS DESC"));
        assertFalse(rewrittenSql.contains("ORDER BY STATUS, STATUS DESC, STATUS"));
        assertTrue(ruleTrace.contains("DEDUPLICATE_GROUP_BY_KEYS"));
        assertTrue(ruleTrace.contains("DEDUPLICATE_ORDER_BY_KEYS"));
    }

    @Test
    void shouldBuildLayeredRecommendationRuleOutputModel() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT COUNT(1), * FROM orders "
                + "WHERE YEAR(order_date) = 2026 OR status LIKE '%paid' "
                + "GROUP BY status, status ORDER BY status, status",
            DataSourceTypeEnum.HETU
        );

        SqlOptimizationPipelineService.RecommendationRuleOutputModel model =
            service.buildRecommendationRuleOutputModel(profile);

        assertTrue(containsRule(model.getRuleChain(), "COUNT_ONE_TO_COUNT_STAR"));
        assertTrue(containsRule(model.getRuleChain(), "DUPLICATE_GROUP_ORDER_KEY"));
        assertTrue(containsRule(model.getUnappliedRules(), "SELECT_STAR_EXPANSION"));
        assertTrue(containsRule(model.getUnappliedRules(), "OR_TO_UNION_ALL"));
        assertTrue(containsRule(model.getUnappliedRules(), "FUNCTION_PREDICATE_TO_RANGE"));
        assertEquals("NOT_REAL_EXECUTION_GAIN", model.getExpectedBenefit().get("claimBoundary"));
        assertEquals("RESULT_DIFF_REQUIRED", model.getEstimatedCost().get("validation"));
        assertEquals("RESULT_DIFF_THEN_MANUAL_REVIEW", model.getValidationMethod());
        assertTrue(model.isManualReviewRequired());
        assertFalse(model.isAutoApplyAllowed());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldKeepSelectStarAndFunctionPredicateAsManualReviewCandidates() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT * FROM orders WHERE YEAR(order_date) = 2026",
            DataSourceTypeEnum.HETU
        );

        OptimizationTaskSuggestion suggestion = service.buildRewriteSuggestion(profile);
        String rewrittenSql = suggestion.getArtifacts().get(0).getContent().toUpperCase();
        SqlOptimizationPipelineService.RecommendationRuleOutputModel model =
            service.buildRecommendationRuleOutputModel(profile);
        Map<String, Object> selectStarRule = findRule(model.getUnappliedRules(), "SELECT_STAR_EXPANSION");
        Map<String, Object> functionRule = findRule(model.getUnappliedRules(), "FUNCTION_PREDICATE_TO_RANGE");
        Map<String, Object> selectStarEvidence = (Map<String, Object>) selectStarRule.get("evidence");
        Map<String, Object> functionEvidence = (Map<String, Object>) functionRule.get("evidence");

        assertTrue(rewrittenSql.contains("SELECT * FROM ORDERS"));
        assertFalse(rewrittenSql.contains("ORDER_DATE >="));
        assertEquals(Boolean.TRUE, selectStarRule.get("manualReviewRequired"));
        assertEquals(Boolean.TRUE, functionRule.get("manualReviewRequired"));
        assertEquals("MANUAL_REVIEW_ONLY", selectStarEvidence.get("rewritePolicy"));
        assertTrue(((List<?>) selectStarEvidence.get("starItems")).contains("*"));
        assertEquals("MANUAL_REVIEW_ONLY", functionEvidence.get("rewritePolicy"));
        assertTrue(((List<?>) functionEvidence.get("predicateSamples")).contains("YEAR(order_date) = 2026"));
        assertTrue(model.isManualReviewRequired());
        assertFalse(model.isAutoApplyAllowed());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldKeepRepeatedSubqueryAsManualReviewCteCandidate() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT c.customer_id, "
                + "(SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.customer_id) AS order_count_a, "
                + "(SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.customer_id) AS order_count_b "
                + "FROM customers c",
            DataSourceTypeEnum.HETU
        );

        OptimizationTaskSuggestion suggestion = service.buildRewriteSuggestion(profile);
        String rewrittenSql = suggestion.getArtifacts().get(0).getContent().toUpperCase();
        SqlOptimizationPipelineService.RecommendationRuleOutputModel model =
            service.buildRecommendationRuleOutputModel(profile);
        Map<String, Object> cteRule = findRule(model.getUnappliedRules(), "REPEATED_SUBQUERY_TO_CTE");
        Map<String, Object> evidence = (Map<String, Object>) cteRule.get("evidence");

        assertFalse(rewrittenSql.startsWith("WITH "));
        assertEquals(Boolean.TRUE, cteRule.get("manualReviewRequired"));
        assertEquals("MANUAL_REVIEW_ONLY", evidence.get("rewritePolicy"));
        assertEquals(Integer.valueOf(1), evidence.get("repeatedSubqueryCount"));
        assertTrue(model.isManualReviewRequired());
        assertFalse(model.isAutoApplyAllowed());
    }

    @Test
    void shouldMarkL2PhysicalRecommendationsAsPullOnlyCandidates() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT customer_id, SUM(amount) FROM orders "
                + "WHERE order_date >= DATE '2026-04-01' GROUP BY customer_id",
            DataSourceTypeEnum.HETU
        );

        SqlOptimizationPipelineService.RecommendationRuleOutputModel model =
            service.buildRecommendationRuleOutputModel(profile);

        assertTrue(containsRule(model.getRuleChain(), "PRECOMPUTE_MV"));
        assertTrue(containsRule(model.getRuleChain(), "PARTITION_PRUNING"));
        assertTrue(containsPrecondition(model.getPreconditions(), "RUNTIME_REUSE_AND_REFRESH_POLICY_REQUIRED"));
        assertFalse(model.isAutoApplyAllowed());
    }

    @Test
    void shouldDeriveAccelerationRecommendationsFromQueryShape() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT customer_id, SUM(amount) FROM orders "
                + "WHERE order_date >= DATE '2026-04-01' GROUP BY customer_id",
            DataSourceTypeEnum.HETU
        );

        OptimizationTaskSuggestion suggestion = service.buildAccelerationSuggestion(
            profile,
            Arrays.asList(AccelerationSuggestionType.PRECOMPUTE, AccelerationSuggestionType.PARTITION)
        );

        String accelerationPlan = suggestion.getArtifacts().get(0).getContent();
        assertTrue(accelerationPlan.contains("PRECOMPUTE"));
        assertTrue(accelerationPlan.contains("PARTITION"));
        assertTrue(suggestion.getSummary().contains("加速推荐"));
    }

    @Test
    void shouldAnalyzeQueryShapeWithApacheCalciteParserAdapter() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "WITH recent_orders AS ("
                + "SELECT customer_id, amount, dt FROM hive.sales.orders WHERE dt >= DATE '2026-04-01'"
                + ") SELECT customer_id, SUM(amount) AS total_amount "
                + "FROM recent_orders WHERE dt <= DATE '2026-04-30' GROUP BY customer_id LIMIT 10",
            DataSourceTypeEnum.HETU,
            SqlParserMode.APACHE_CALCITE
        );

        assertEquals("APACHE_CALCITE", profile.getParserEngine());
        assertTrue(profile.getTables().contains("hive.sales.orders"));
        assertTrue(profile.getPredicateCount() >= 2);
        assertEquals(1, profile.getAggregateFunctions().size());
        assertTrue(profile.getGroupByCount() >= 1);
        assertTrue(profile.isLimitPresent());
        assertTrue(service.deriveRewriteCandidateRules(profile).isEmpty());
    }

    @Test
    void shouldAnalyzeSqlWithDashLineCommentsWithoutTreatingStringLiteralAsComment() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "-- report_code=RPT_COMMENTED\n"
                + "SELECT customer_id, '--not-a-comment' AS marker FROM orders -- table comment\n"
                + "WHERE dt = DATE '2026-04-01' -- date filter\n"
                + "AND status = 'PAID'",
            DataSourceTypeEnum.HETU
        );

        assertEquals("JSQLPARSER", profile.getParserEngine());
        assertEquals(1, profile.getTables().size());
        assertEquals("orders", profile.getTables().get(0));
        assertTrue(profile.getPredicateCount() >= 2);
    }

    @Test
    void shouldNotFlagRepeatedScanOrExpressionForTablePrefixedColumns() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT orders_status, orders_amount, orders_count "
                + "FROM orders "
                + "WHERE dt = DATE '2026-04-01' "
                + "GROUP BY orders_status, orders_amount, orders_count "
                + "ORDER BY orders_status "
                + "LIMIT 50",
            DataSourceTypeEnum.HETU
        );

        assertEquals(1, profile.getTables().size());
        assertEquals("orders", profile.getTables().get(0));
        assertEquals(0, profile.getRepeatedTableScanCount());
        assertEquals(0, profile.getRepeatedExpressionCount());
        assertFalse(profile.getWarnings().contains("REPEATED_TABLE_SCAN_RISK"));
        assertFalse(profile.getWarnings().contains("REPEATED_EXPRESSION_COMPUTE"));
    }

    @Test
    void shouldExposeOrderGroupDuplicateAndGroupWithoutAggregateSignals() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT status FROM orders "
                + "WHERE dt = DATE '2026-04-01' "
                + "GROUP BY status, status "
                + "ORDER BY status, status, customer_id",
            DataSourceTypeEnum.HETU
        );

        assertEquals(3, profile.getOrderByExpressionCount());
        assertEquals(1, profile.getDuplicateOrderByKeyCount());
        assertEquals(1, profile.getDuplicateGroupByKeyCount());
        assertTrue(profile.isGroupByWithoutAggregate());
        assertTrue(profile.getWarnings().contains("ORDER_BY_COMPLEXITY_RISK"));
        assertTrue(profile.getWarnings().contains("GROUP_BY_WITHOUT_AGGREGATE_RISK"));
        assertTrue(profile.getWarnings().contains("DUPLICATE_GROUP_OR_ORDER_KEY_RISK"));
    }

    @Test
    void shouldExposeAggregationStringAndRepeatedSubquerySignals() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT c.customer_id, "
                + "(SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.customer_id) AS order_count_a, "
                + "(SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.customer_id) AS order_count_b, "
                + "GROUP_CONCAT(CONCAT(c.customer_name, ':', c.status)) AS customer_labels, "
                + "COUNT(*), SUM(c.amount), AVG(c.amount) "
                + "FROM customers c "
                + "WHERE c.dt = DATE '2026-04-01' "
                + "GROUP BY c.customer_id "
                + "ORDER BY c.customer_id",
            DataSourceTypeEnum.HETU
        );

        assertTrue(profile.getAggregateFunctionCount() >= 5);
        assertTrue(profile.getStringConcatenationCount() >= 1);
        assertEquals(1, profile.getLargeStringAggregateCount());
        assertTrue(profile.getRepeatedSubqueryCount() >= 1);
        assertTrue(profile.getWarnings().contains("AGGREGATION_COMPLEXITY_RISK"));
        assertTrue(profile.getWarnings().contains("LARGE_STRING_RESULT_RISK"));
        assertTrue(profile.getWarnings().contains("REPEATED_SUBQUERY_RISK"));
    }

    @Test
    void shouldExtractComplexAntiPatternSignalsFromNestedSql() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            complexAntiPatternSql(),
            DataSourceTypeEnum.HETU
        );

        assertEquals("JSQLPARSER", profile.getParserEngine());
        assertEquals(5, profile.getTables().size());
        assertTrue(profile.getSubqueryCount() >= 9);
        assertEquals(3, profile.getScalarSubqueryCount());
        assertTrue(profile.getNestedSubqueryDepth() >= 3);
        assertTrue(profile.getCorrelatedSubqueryCount() >= 6);
        assertTrue(profile.getOrPredicateCount() >= 1);
        assertTrue(profile.getFunctionWrappedPredicateCount() >= 1);
        assertTrue(profile.getLeadingWildcardLikeCount() >= 1);
        assertTrue(profile.getRandomOrderCount() >= 1);
        assertTrue(profile.getNotExistsCount() >= 1);
        assertTrue(profile.getRepeatedTableScanCount() > 0);
        assertTrue(profile.getWarnings().contains("SCALAR_SUBQUERY_IN_SELECT"));
        assertTrue(profile.getWarnings().contains("NESTED_SUBQUERY_RISK"));
        assertTrue(profile.getWarnings().contains("CORRELATED_SUBQUERY_RISK"));
        assertTrue(profile.getWarnings().contains("FUNCTION_WRAPPED_PREDICATE"));
        assertTrue(profile.getWarnings().contains("NOT_EXISTS_ANTI_JOIN_RISK"));
        assertTrue(profile.getWarnings().contains("LEADING_WILDCARD_LIKE_RISK"));
        assertTrue(profile.getWarnings().contains("ORDER_BY_RANDOM_RISK"));
        assertTrue(profile.getWarnings().contains("REPEATED_TABLE_SCAN_RISK"));
        assertTrue(profile.getWarnings().contains("COMPLEX_QUERY_GRAPH_RISK"));
    }

    private String complexAntiPatternSql() {
        return "-- complex anti-pattern query\n"
            + "SELECT c.customer_id, c.customer_name, c.state,\n"
            + "(SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.customer_id) AS total_orders,\n"
            + "(SELECT SUM(order_amount) FROM orders o WHERE o.customer_id = c.customer_id) AS total_spent,\n"
            + "(SELECT GROUP_CONCAT(product_name) FROM order_items oi JOIN products p ON oi.product_id = p.product_id "
            + "WHERE oi.customer_id = c.customer_id) AS all_products\n"
            + "FROM customers c\n"
            + "WHERE c.is_active = 1 AND c.customer_id IN (\n"
            + "SELECT o1.customer_id FROM orders o1 WHERE YEAR(o1.order_date) = 2025\n"
            + "AND NOT EXISTS (SELECT 1 FROM customer_tags ct WHERE ct.customer_id = o1.customer_id AND ct.tag_name = 'VIP')\n"
            + "AND o1.order_amount > (SELECT AVG(o2.order_amount) FROM orders o2 "
            + "WHERE o2.state = (SELECT state FROM customers WHERE customer_id = o1.customer_id))\n"
            + "AND EXISTS (SELECT 1 FROM order_items oi2 WHERE oi2.order_id = o1.order_id "
            + "AND oi2.product_id IN (SELECT product_id FROM products WHERE category LIKE '%电子%')))\n"
            + "OR c.customer_id IN (SELECT customer_id FROM orders WHERE order_amount > 10000)\n"
            + "ORDER BY RAND() LIMIT 10";
    }

    private boolean containsRule(List<Map<String, Object>> entries, String rule) {
        for (Map<String, Object> entry : entries) {
            if (rule.equals(entry.get("rule"))) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> findRule(List<Map<String, Object>> entries, String rule) {
        for (Map<String, Object> entry : entries) {
            if (rule.equals(entry.get("rule"))) {
                return entry;
            }
        }
        assertNotNull(null, "Expected rule " + rule);
        return null;
    }

    private boolean containsPrecondition(List<Map<String, Object>> entries, String code) {
        for (Map<String, Object> entry : entries) {
            if (code.equals(entry.get("code"))) {
                return true;
            }
        }
        return false;
    }
}
