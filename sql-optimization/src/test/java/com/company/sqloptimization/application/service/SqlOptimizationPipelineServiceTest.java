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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        Map<String, Object> productionGate = nestedMap(model.getExpectedBenefit(), "productionScaleGate");
        assertEquals("EXTERNAL_EVIDENCE_REQUIRED", productionGate.get("status"));
        assertEquals("THIRTY_PB", productionGate.get("targetDatasetSizeLabel"));
        assertEquals(Integer.valueOf(10000), productionGate.get("targetConcurrency"));
        assertEquals(Long.valueOf(10000000L), productionGate.get("targetDailyQueryVolume"));
        assertTrue(((List<?>) productionGate.get("requiredEvidence")).contains("VERIFIED_COST_BILL"));
        assertEquals("USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-EXTERNAL-ARTIFACTS-20260518",
            productionGate.get("blockedTask"));
        assertEquals("EXTERNAL_EVIDENCE_REQUIRED",
            nestedMap(model.getEstimatedCost(), "productionScaleGate").get("status"));
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

    @Test
    void shouldExposeAtLeastFiftySelectRewriteRecommendationScenarios() {
        List<Sample> samples = Arrays.asList(
            sample("count-literal", "SELECT COUNT(1) FROM orders WHERE dt = DATE '2026-05-01'", "COUNT_ONE_TO_COUNT_STAR"),
            sample("duplicate-where", "SELECT order_id FROM orders WHERE dt = DATE '2026-05-01' AND dt = DATE '2026-05-01'",
                "DEDUPLICATE_WHERE_PREDICATES"),
            sample("duplicate-having", "SELECT customer_id, COUNT(*) FROM orders GROUP BY customer_id "
                + "HAVING COUNT(*) > 1 AND COUNT(*) > 1", "DEDUPLICATE_HAVING_PREDICATES"),
            sample("duplicate-group-order", "SELECT status FROM orders GROUP BY status, status ORDER BY status, status",
                "DUPLICATE_GROUP_ORDER_KEY"),
            sample("select-star", "SELECT * FROM orders WHERE dt = DATE '2026-05-01'", "SELECT_STAR_EXPANSION"),
            sample("or-predicate", "SELECT order_id FROM orders WHERE status = 'PAID' OR channel = 'APP'", "OR_TO_UNION_ALL"),
            sample("function-predicate", "SELECT order_id FROM orders WHERE YEAR(order_date) = 2026",
                "FUNCTION_PREDICATE_TO_RANGE"),
            sample("scalar-subquery", "SELECT c.customer_id, "
                + "(SELECT MAX(o.amount) FROM orders o WHERE o.customer_id = c.customer_id) AS max_amount FROM customers c",
                "SCALAR_SUBQUERY_TO_JOIN"),
            sample("repeated-subquery", "SELECT c.customer_id, "
                + "(SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.customer_id) AS a, "
                + "(SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.customer_id) AS b FROM customers c",
                "REPEATED_SUBQUERY_TO_CTE"),
            sample("not-exists", "SELECT c.customer_id FROM customers c WHERE NOT EXISTS "
                + "(SELECT 1 FROM orders o WHERE o.customer_id = c.customer_id)", "NOT_EXISTS_TO_ANTI_JOIN"),
            sample("leading-like", "SELECT customer_id FROM customers WHERE customer_name LIKE '%vip%'",
                "LEADING_LIKE_REVIEW"),
            sample("order-random", "SELECT order_id FROM orders ORDER BY RAND() LIMIT 10", "ORDER_RANDOM_REVIEW"),
            sample("distinct", "SELECT DISTINCT customer_id FROM orders WHERE dt = DATE '2026-05-01'",
                "DISTINCT_DEDUP_REVIEW"),
            sample("group-to-distinct", "SELECT status FROM orders GROUP BY status", "GROUP_BY_TO_DISTINCT"),
            sample("having-pushdown", "SELECT customer_id, COUNT(*) FROM orders GROUP BY customer_id HAVING customer_id > 10",
                "HAVING_TO_WHERE_PUSHDOWN"),
            sample("in-subquery", "SELECT order_id FROM orders WHERE customer_id IN "
                + "(SELECT customer_id FROM customers WHERE state = 'CA')", "IN_SUBQUERY_TO_SEMI_JOIN"),
            sample("exists", "SELECT c.customer_id FROM customers c WHERE EXISTS "
                + "(SELECT 1 FROM orders o WHERE o.customer_id = c.customer_id)", "EXISTS_TO_SEMI_JOIN"),
            sample("not-in", "SELECT customer_id FROM customers WHERE customer_id NOT IN "
                + "(SELECT customer_id FROM blocked_customers)", "NOT_IN_TO_ANTI_JOIN"),
            sample("left-null", "SELECT c.customer_id FROM customers c LEFT JOIN orders o "
                + "ON c.customer_id = o.customer_id WHERE o.order_id IS NULL", "LEFT_JOIN_NULL_TO_ANTI_JOIN"),
            sample("cross-join", "SELECT c.customer_id, r.region_id FROM customers c CROSS JOIN regions r",
                "CROSS_JOIN_GUARD"),
            sample("cast-key", "SELECT o.order_id FROM orders o JOIN customers c "
                + "ON CAST(o.customer_id AS VARCHAR) = c.customer_id", "CAST_JOIN_KEY_NORMALIZE"),
            sample("implicit-cast", "SELECT order_id FROM orders WHERE order_id = '123'", "IMPLICIT_TYPE_CAST_REVIEW"),
            sample("prefix-like", "SELECT customer_id FROM customers WHERE customer_name LIKE 'vip%'",
                "LIKE_PREFIX_RANGE_REVIEW"),
            sample("regexp", "SELECT customer_id FROM customers WHERE REGEXP_LIKE(customer_name, '^vip')",
                "REGEXP_FILTER_TO_SEARCH_INDEX"),
            sample("long-in-list", "SELECT order_id FROM orders WHERE status IN "
                + "('S1','S2','S3','S4','S5','S6','S7','S8','S9','S10')", "LONG_IN_LIST_TO_TEMP_TABLE"),
            sample("window-topn", "SELECT customer_id, order_id, ROW_NUMBER() OVER "
                + "(PARTITION BY customer_id ORDER BY order_date DESC) AS rn FROM orders", "WINDOW_TOPN_REWRITE"),
            sample("union", "SELECT customer_id FROM orders_2025 UNION SELECT customer_id FROM orders_2026",
                "UNION_DEDUP_REVIEW"),
            sample("intersect", "SELECT customer_id FROM orders INTERSECT SELECT customer_id FROM customers",
                "INTERSECT_TO_SEMI_JOIN"),
            sample("except", "SELECT customer_id FROM customers EXCEPT SELECT customer_id FROM blocked_customers",
                "EXCEPT_TO_ANTI_JOIN"),
            sample("json", "SELECT JSON_EXTRACT(payload, '$.campaign') FROM events WHERE dt = DATE '2026-05-01'",
                "JSON_EXTRACT_MATERIALIZATION"),
            sample("unnest", "SELECT UNNEST(items) FROM orders WHERE dt = DATE '2026-05-01'", "UNNEST_LATERAL_REVIEW"),
            sample("null-safe", "SELECT order_id FROM orders WHERE COALESCE(status, 'UNKNOWN') = 'PAID'",
                "NULL_SAFE_EQUALITY_REVIEW"),
            sample("offset", "SELECT order_id FROM orders ORDER BY order_id LIMIT 10 OFFSET 100",
                "OFFSET_TO_KEYSET_PAGINATION"),
            sample("join-reorder", "SELECT o.order_id FROM orders o JOIN customers c ON o.customer_id = c.customer_id "
                + "JOIN regions r ON c.region_id = r.region_id", "JOIN_REORDER_BY_STATS"),
            sample("dynamic-filter", "SELECT o.order_id FROM orders o JOIN customers c "
                + "ON o.customer_id = c.customer_id WHERE c.state = 'CA'", "DYNAMIC_FILTERING_JOIN"),
            sample("star-schema-mv", "SELECT r.region_name, SUM(o.amount) FROM orders o "
                + "JOIN customers c ON o.customer_id = c.customer_id JOIN regions r ON c.region_id = r.region_id "
                + "GROUP BY r.region_name", "STAR_SCHEMA_MV"),
            sample("split-sql", complexAntiPatternSql(), "SPLIT_SQL"),
            sample("result-cache", "SELECT customer_id, SUM(amount) FROM orders "
                + "WHERE dt = DATE '2026-05-01' GROUP BY customer_id LIMIT 100", "RESULT_CACHE"),
            sample("projection-pruning", "SELECT order_id, customer_id, status, channel, amount, dt, province "
                + "FROM orders WHERE dt = DATE '2026-05-01'", "PROJECTION_PRUNING"),
            sample("topn", "SELECT order_id FROM orders WHERE dt = DATE '2026-05-01' ORDER BY amount DESC LIMIT 20",
                "TOPN_PUSHDOWN"),
            sample("case-aggregate", "SELECT customer_id, SUM(CASE WHEN status = 'PAID' THEN amount ELSE 0 END) "
                + "FROM orders GROUP BY customer_id", "PIVOT_AGGREGATE_PRECOMPUTE"),
            sample("date-grain", "SELECT DATE_TRUNC('day', order_date), SUM(amount) FROM orders "
                + "GROUP BY DATE_TRUNC('day', order_date)", "DATE_GRANULARITY_MV"),
            sample("partition-compensation", "SELECT dt, SUM(amount) FROM orders "
                + "WHERE dt >= DATE '2026-05-01' AND status = 'PAID' GROUP BY dt", "PARTITION_COMPENSATION_UNION"),
            sample("with-cte", "WITH recent_orders AS (SELECT order_id, customer_id FROM orders "
                + "WHERE dt = DATE '2026-05-01') SELECT customer_id FROM recent_orders", "CTE_MATERIALIZATION_POLICY"),
            sample("full-scan", "SELECT order_id FROM orders", "FULL_SCAN_FILTER_GUARD"),
            sample("order-without-limit", "SELECT order_id FROM orders WHERE dt = DATE '2026-05-01' ORDER BY amount DESC",
                "ORDER_BY_WITHOUT_LIMIT_GUARD"),
            sample("limit-without-order", "SELECT order_id FROM orders WHERE dt = DATE '2026-05-01' LIMIT 20",
                "LIMIT_WITHOUT_ORDER_GUARD"),
            sample("repeated-expression", "SELECT customer_id, amount * tax_rate AS tax_value FROM orders "
                + "WHERE amount * tax_rate > 100 ORDER BY amount * tax_rate", "REPEATED_EXPRESSION_TO_CTE"),
            sample("udf", "SELECT custom_score(amount) FROM orders WHERE dt = DATE '2026-05-01'",
                "UDF_EVALUATION_ISOLATION"),
            sample("string-concat", "SELECT CONCAT(first_name, last_name) FROM customers WHERE state = 'CA'",
                "STRING_CONCAT_PRECOMPUTE"),
            sample("string-aggregate", "SELECT customer_id, GROUP_CONCAT(product_name) FROM order_items "
                + "GROUP BY customer_id", "LARGE_STRING_AGGREGATE_OFFLOAD"),
            sample("window-frame", "SELECT customer_id, SUM(amount) OVER (PARTITION BY customer_id) AS total_amount "
                + "FROM orders WHERE dt = DATE '2026-05-01'", "WINDOW_FRAME_PRECOMPUTE"),
            sample("approx-distinct", "SELECT APPROX_DISTINCT(user_id) FROM events WHERE dt = DATE '2026-05-01'",
                "APPROX_DISTINCT_SKETCH_MV"),
            sample("multi-count-distinct", "SELECT COUNT(DISTINCT user_id), COUNT(DISTINCT session_id) "
                + "FROM events WHERE dt = DATE '2026-05-01'", "MULTI_COUNT_DISTINCT_DECOMPOSITION"),
            sample("not-equal", "SELECT order_id FROM orders WHERE status <> 'CANCELLED'", "NEGATION_FILTER_REVIEW"),
            sample("null-filter", "SELECT customer_id FROM customers WHERE phone IS NULL", "NULL_FILTER_INDEX_REVIEW"),
            sample("order-expression", "SELECT customer_id FROM customers WHERE state = 'CA' ORDER BY LOWER(customer_name)",
                "ORDER_BY_EXPRESSION_PRECOMPUTE"),
            sample("array-contains", "SELECT order_id FROM orders WHERE ARRAY_CONTAINS(tags, 'vip')",
                "ARRAY_CONTAINS_INDEX_REVIEW"),
            sample("range-join", "SELECT o.order_id FROM orders o JOIN promotions p "
                + "ON o.order_date BETWEEN p.start_date AND p.end_date WHERE o.dt = DATE '2026-05-01'",
                "RANGE_JOIN_BUCKETIZATION"),
            sample("case-expression", "SELECT CASE WHEN amount > 0 THEN amount ELSE 0 END "
                + "FROM orders WHERE dt = DATE '2026-05-01'", "CASE_EXPRESSION_NORMALIZATION"),
            sample("distinct-order", "SELECT DISTINCT customer_id FROM orders "
                + "WHERE dt = DATE '2026-05-01' ORDER BY customer_id", "DISTINCT_ORDER_BY_ALIGNMENT"),
            sample("correlated-subquery", "SELECT c.customer_id FROM customers c WHERE EXISTS "
                + "(SELECT 1 FROM orders o WHERE o.customer_id = c.customer_id)", "CORRELATED_SUBQUERY_DECORRELATION"),
            sample("nested-subquery", "SELECT customer_id FROM customers WHERE customer_id IN "
                + "(SELECT customer_id FROM orders WHERE amount > "
                + "(SELECT AVG(amount) FROM orders WHERE dt = DATE '2026-05-01'))", "NESTED_SUBQUERY_FLATTENING"),
            sample("percentile", "SELECT APPROX_PERCENTILE(amount, 0.95) FROM orders "
                + "WHERE dt = DATE '2026-05-01'", "PERCENTILE_SKETCH_PRECOMPUTE"),
            sample("rollup-lattice", "SELECT dt, status, SUM(amount) FROM orders "
                + "WHERE dt = DATE '2026-05-01' GROUP BY dt, status", "ROLLUP_AGGREGATE_LATTICE")
        );

        Set<String> coveredRules = new LinkedHashSet<String>();
        for (Sample sample : samples) {
            SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(sample.sql, DataSourceTypeEnum.HETU);
            SqlOptimizationPipelineService.RecommendationRuleOutputModel model =
                service.buildRecommendationRuleOutputModel(profile);
            Set<String> modelRules = allRuleNames(model);
            assertTrue(modelRules.contains(sample.expectedRule), sample.name + " missing " + sample.expectedRule);
            assertFalse(model.isAutoApplyAllowed(), sample.name + " 不允许基于静态分析自动应用");
            coveredRules.add(sample.expectedRule);
        }
        assertTrue(coveredRules.size() >= 50, "coveredRules=" + coveredRules);
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

    private Set<String> allRuleNames(SqlOptimizationPipelineService.RecommendationRuleOutputModel model) {
        Set<String> names = new LinkedHashSet<String>();
        addRuleNames(names, model.getRuleChain());
        addRuleNames(names, model.getUnappliedRules());
        return names;
    }

    private void addRuleNames(Set<String> names, List<Map<String, Object>> entries) {
        for (Map<String, Object> entry : entries) {
            Object rule = entry.get("rule");
            if (rule != null) {
                names.add(String.valueOf(rule));
            }
        }
    }

    private Sample sample(String name, String sql, String expectedRule) {
        return new Sample(name, sql, expectedRule);
    }

    private Map<String, Object> findRule(List<Map<String, Object>> entries, String rule) {
        for (Map<String, Object> entry : entries) {
            if (rule.equals(entry.get("rule"))) {
                return entry;
            }
        }
        assertNotNull(null, "缺少预期规则：" + rule);
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> value, String key) {
        Object nested = value.get(key);
        assertTrue(nested instanceof Map, "缺少嵌套 map：" + key);
        return (Map<String, Object>) nested;
    }

    private static final class Sample {
        private final String name;
        private final String sql;
        private final String expectedRule;

        private Sample(String name, String sql, String expectedRule) {
            this.name = name;
            this.sql = sql;
            this.expectedRule = expectedRule;
        }
    }
}
