package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.parse.SqlParserMode;
import com.company.sqloptimization.domain.rewrite.ir.RelationalOperator;
import com.company.sqloptimization.domain.rewrite.ir.RewriteCoreIrSnapshot;
import com.company.sqloptimization.domain.rewrite.ir.RewriteIrConflict;
import com.company.sqloptimization.domain.rewrite.ir.RewriteIrLayer;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDag;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDagIssue;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockEdge;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockNode;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteCandidate;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlan;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteRuleType;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceCheck;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceCheckType;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceReport;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceStatus;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationTaskSuggestion;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        assertTrue(suggestion.getArtifacts().get(0).getContent().contains("\"advancedStructureProfile\""));
        assertTrue(suggestion.getArtifacts().get(0).getContent().contains("\"joinGraph\""));
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
            "SELECT o.customer_id, SUM(o.amount) FROM orders o "
                + "JOIN customer_profile c ON o.customer_id = c.customer_id "
                + "WHERE o.order_date >= DATE '2026-04-01' GROUP BY o.customer_id",
            DataSourceTypeEnum.HETU
        );

        SqlOptimizationPipelineService.RecommendationRuleOutputModel model =
            service.buildRecommendationRuleOutputModel(profile);

        assertTrue(containsRule(model.getRuleChain(), "PRECOMPUTE_MV"));
        assertTrue(containsRule(model.getRuleChain(), "PARTITION_PRUNING"));
        assertTrue(containsRule(model.getRuleChain(), "BUCKET_JOIN"));
        assertEquals("物化视图预计算", rule(model.getRuleChain(), "PRECOMPUTE_MV").get("titleZh"));
        assertEquals("仅候选，需外部协同，不自动执行", rule(model.getRuleChain(), "PRECOMPUTE_MV").get("statusZh"));
        assertTrue(containsPrecondition(model.getPreconditions(), "RUNTIME_REUSE_AND_REFRESH_POLICY_REQUIRED"));
        assertTrue(containsPrecondition(model.getPreconditions(), "JOIN_KEY_DISTRIBUTION_REQUIRED"));
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
            Arrays.asList(AccelerationSuggestionType.PRECOMPUTE, AccelerationSuggestionType.PARTITION),
            DataSourceTypeEnum.HETU,
            "datasource-a",
            "fingerprint-001",
            "report-sales"
        );

        String accelerationPlan = suggestion.getArtifacts().get(0).getContent();
        String signalProfile = suggestion.getArtifacts().get(1).getContent();
        assertTrue(accelerationPlan.contains("PRECOMPUTE"));
        assertTrue(accelerationPlan.contains("PARTITION"));
        assertTrue(signalProfile.contains("\"advancedStructureProfile\""));
        assertTrue(signalProfile.contains("\"aggregations\""));
        assertTrue(suggestion.getArtifacts().get(3).getContent().contains("\"artifactStatus\":\"GENERATED\""));
        assertTrue(suggestion.getArtifacts().get(3).getContent().contains("CREATE MATERIALIZED VIEW mv_report_sales"));
        assertTrue(suggestion.getArtifacts().get(3).getContent().contains("FROM mv_report_sales"));
        assertTrue(suggestion.getArtifacts().get(3).getContent().contains("SUM(sum_amount) AS sum_amount"));
        assertFalse(suggestion.getArtifacts().get(3).getContent().contains("SELECT * FROM mv_report_sales"));
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
    void shouldBuildFiveLayerRewriteCoreIrSnapshot() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "WITH recent_orders AS ("
                + "SELECT customer_id, amount, dt FROM orders WHERE dt >= DATE '2026-05-01'"
                + ") SELECT c.customer_level, SUM(r.amount) AS total_amount "
                + "FROM recent_orders r JOIN customers c ON r.customer_id = c.customer_id "
                + "WHERE r.dt <= DATE '2026-05-31' GROUP BY c.customer_level",
            DataSourceTypeEnum.HETU
        );

        RewriteCoreIrSnapshot snapshot = service.buildRewriteCoreIr(profile);

        assertEquals(RewriteCoreIrSnapshot.SCHEMA_VERSION, snapshot.getSchemaVersion());
        assertTrue(snapshot.containsLayer(RewriteIrLayer.L1_AST));
        assertTrue(snapshot.containsLayer(RewriteIrLayer.L2_TABLE_REFERENCE));
        assertTrue(snapshot.containsLayer(RewriteIrLayer.L3_QUERY_BLOCK));
        assertTrue(snapshot.containsLayer(RewriteIrLayer.L4_RELATIONAL_ALGEBRA));
        assertTrue(snapshot.containsLayer(RewriteIrLayer.L5_BUSINESS_INTENT));
        assertEquals("JSQLPARSER_EXPRESSION", snapshot.getAst().getDialectNodeKind());
        assertFalse(snapshot.getTableReferences().isEmpty());
        assertFalse(snapshot.getQueryBlocks().isEmpty());
        assertTrue(containsOperator(snapshot, RelationalOperator.TABLE_SCAN));
        assertTrue(containsOperator(snapshot, RelationalOperator.JOIN));
        assertTrue(containsOperator(snapshot, RelationalOperator.SIGMA));
        assertTrue(containsOperator(snapshot, RelationalOperator.GAMMA));
        assertTrue(containsOperator(snapshot, RelationalOperator.PI));
        assertFalse(snapshot.getBusinessIntent().getTimeAnchors().isEmpty());
        assertFalse(snapshot.getBusinessIntent().getMeasures().isEmpty());
        assertFalse(snapshot.getBusinessIntent().getDimensions().isEmpty());
        assertFalse(snapshot.getBusinessIntent().getFilters().isEmpty());
        assertTrue(containsConflict(snapshot, "IR_LAYER_RULE_LEVEL_NAME_OVERLAP"));
        assertEquals("NO_FRONTEND_PAGE_CHANGE", snapshot.getAttributes().get("pageImpact"));
    }

    @Test
    void shouldDecomposeRepeatedCorrelatedSubqueriesIntoQueryBlockDag() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT c.customer_id, "
                + "(SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.customer_id "
                + "AND o.dt = DATE '2026-05-01') AS order_count_a, "
                + "(SELECT COUNT(*) FROM orders x WHERE x.customer_id = c.customer_id "
                + "AND x.dt = DATE '2026-05-31') AS order_count_b "
                + "FROM customers c WHERE c.status = 'ACTIVE'",
            DataSourceTypeEnum.HETU
        );

        QueryBlockDag dag = service.buildQueryBlockDag(profile);

        assertEquals(QueryBlockDag.SCHEMA_VERSION, dag.getSchemaVersion());
        assertEquals("APACHE_CALCITE", dag.getParserEngine());
        assertEquals("AVAILABLE", dag.getAttributes().get("decompositionStatus"));
        assertTrue(dag.getBlocks().size() >= 3, "blocks=" + dag.getBlocks().size());
        assertFalse(dag.getTopologicalOrder().isEmpty());
        assertTrue(dag.hasDuplicateStructuralBlocks(), dag.getStructuralHashGroups().toString());
        assertTrue(dag.getDuplicateStructuralGroups().get(0).getBlockIds().size() >= 2);
        assertTrue(containsDagEdge(dag, QueryBlockEdge.LATERAL_JOIN_PROMOTION));
        assertTrue(containsDagIssue(dag, "QBDAG_CORRELATED_SUBQUERY_CYCLE_BROKEN"));
        assertTrue(hasExternalReference(dag, "c.customer_id"));
    }

    @Test
    void shouldExposeQueryBlockDagThroughRewriteCoreIrSnapshot() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT a.customer_id, a.total_amount, b.total_amount AS total_amount_b "
                + "FROM (SELECT customer_id, SUM(amount) AS total_amount FROM orders "
                + "WHERE dt = DATE '2026-05-01' GROUP BY customer_id) a "
                + "JOIN (SELECT customer_id, SUM(amount) AS total_amount FROM orders "
                + "WHERE dt = DATE '2026-05-31' GROUP BY customer_id) b "
                + "ON a.customer_id = b.customer_id",
            DataSourceTypeEnum.HETU
        );

        RewriteCoreIrSnapshot snapshot = service.buildRewriteCoreIr(profile);

        assertNotNull(snapshot.getQueryBlockDag());
        assertTrue(snapshot.getQueryBlockDag().hasDuplicateStructuralBlocks());
        assertEquals("NO_FRONTEND_PAGE_CHANGE", snapshot.getQueryBlockDag().getAttributes().get("pageImpact"));
        assertEquals(
            Integer.valueOf(snapshot.getQueryBlockDag().getDuplicateStructuralGroups().size()),
            snapshot.getAttributes().get("duplicateStructuralGroupCount")
        );
        assertTrue(hasEquivalentQueryBlock(snapshot));
    }

    @Test
    void shouldGenerateRelationalRewriteCandidatesForRepeatedAggregateBlocks() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            repeatedAggregateLeftJoinSql(),
            DataSourceTypeEnum.HETU
        );

        RelationalRewritePlan plan = service.buildRelationalRewritePlan(profile);

        assertEquals(RelationalRewritePlan.SCHEMA_VERSION, plan.getSchemaVersion());
        assertTrue(plan.hasCandidate(RelationalRewriteRuleType.CSE_ELIMINATION));
        assertTrue(plan.hasCandidate(RelationalRewriteRuleType.VERTICAL_FOLDING));
        assertTrue(plan.hasCandidate(RelationalRewriteRuleType.HORIZONTAL_UNNESTING));
        assertEquals("NO_FRONTEND_PAGE_CHANGE", plan.getAttributes().get("pageImpact"));
        assertEquals(Boolean.FALSE, plan.getAttributes().get("autoApplyAllowed"));

        RelationalRewriteCandidate cse = firstCandidate(plan, RelationalRewriteRuleType.CSE_ELIMINATION);
        assertTrue(cse.isManualReviewRequired());
        assertFalse(cse.isAutoApplyAllowed());
        assertTrue(cse.getSourceBlockIds().size() >= 3, cse.getSourceBlockIds().toString());
        assertFalse(cse.getCompensationPredicates().isEmpty());
        assertTrue(cse.getReplacementForm().contains("pushCompensationPredicatesAtReference=true"));

        RelationalRewriteCandidate vertical = firstCandidate(plan, RelationalRewriteRuleType.VERTICAL_FOLDING);
        assertTrue(vertical.getReplacementForm().contains("VERTICAL_FOLD"));
        assertTrue(vertical.getPreconditions().contains("COUNT_DISTINCT_ARGUMENT_EQUIVALENCE_REQUIRED"));
        assertTrue(vertical.getSemanticRisks().contains("OVERLAPPING_PREDICATES_MUST_REMAIN_INSIDE_CASE_EXPRESSION"));

        RelationalRewriteCandidate horizontal = firstCandidate(plan, RelationalRewriteRuleType.HORIZONTAL_UNNESTING);
        assertTrue(horizontal.getReplacementForm().contains("HORIZONTAL_UNNEST"));
        assertEquals("AGGREGATION_PUSHDOWN_GROUP_BY_EXTENSION", horizontal.getAttributes().get("unnestingMode"));
    }

    @Test
    void shouldExposeRelationalRewritePlanThroughRewriteCoreIrSnapshot() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            repeatedAggregateLeftJoinSql(),
            DataSourceTypeEnum.HETU
        );

        RewriteCoreIrSnapshot snapshot = service.buildRewriteCoreIr(profile);

        assertNotNull(snapshot.getRelationalRewritePlan());
        assertTrue(snapshot.getRelationalRewritePlan().hasCandidate(RelationalRewriteRuleType.CSE_ELIMINATION));
        assertTrue(snapshot.getRelationalRewritePlan().hasCandidate(RelationalRewriteRuleType.VERTICAL_FOLDING));
        assertTrue(snapshot.getRelationalRewritePlan().hasCandidate(RelationalRewriteRuleType.HORIZONTAL_UNNESTING));
        assertEquals(
            Integer.valueOf(snapshot.getRelationalRewritePlan().getCandidates().size()),
            snapshot.getAttributes().get("relationalRewriteCandidateCount")
        );
        assertEquals("CANDIDATE_GENERATED", snapshot.getAttributes().get("relationalRewritePlanStatus"));
    }

    @Test
    void shouldGenerateSemanticEquivalenceReportForRelationalRewriteCandidates() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            repeatedAggregateLeftJoinSql(),
            DataSourceTypeEnum.HETU
        );

        SemanticEquivalenceReport report = service.verifySemanticEquivalence(profile);

        assertEquals(SemanticEquivalenceReport.SCHEMA_VERSION, report.getSchemaVersion());
        assertEquals(SemanticEquivalenceStatus.NEEDS_CONSTRAINTS, report.getStatus());
        assertTrue(report.hasCheck(SemanticEquivalenceCheckType.CONSTRAINT_BASED_EQUIVALENCE));
        assertTrue(report.hasCheck(SemanticEquivalenceCheckType.STATISTICAL_AGGREGATION_EQUIVALENCE));
        assertEquals("NO_FRONTEND_PAGE_CHANGE", report.getAttributes().get("pageImpact"));
        assertEquals(Boolean.FALSE, report.getAttributes().get("autoApplyAllowed"));
        assertFalse(report.getUnverifiedCandidateIds().isEmpty());

        SemanticEquivalenceCheck constraint = firstSemanticCheck(
            report,
            SemanticEquivalenceCheckType.CONSTRAINT_BASED_EQUIVALENCE,
            RelationalRewriteRuleType.CSE_ELIMINATION
        );
        assertTrue(constraint.getDifferenceExpression().contains("DELTA_Q1_MINUS_Q2"));
        assertTrue(constraint.getReverseDifferenceExpression().contains("DELTA_Q2_MINUS_Q1"));
        assertTrue(constraint.getProofObligations().contains("DELTA_Q1_MINUS_Q2_EMPTY"));
        assertTrue(constraint.getProofObligations().contains("COMPENSATION_PREDICATES_APPLIED_AT_REFERENCE"));
        assertTrue(constraint.getNullSemantics().contains("USE_IS_NOT_DISTINCT_FROM_FOR_NULLABLE_COLUMN_COMPARISON"));

        SemanticEquivalenceCheck aggregation = firstSemanticCheck(
            report,
            SemanticEquivalenceCheckType.STATISTICAL_AGGREGATION_EQUIVALENCE,
            RelationalRewriteRuleType.VERTICAL_FOLDING
        );
        assertEquals(SemanticEquivalenceStatus.CONDITIONALLY_PROVED, aggregation.getStatus());
        assertTrue(aggregation.getPreconditions().contains("COUNT_DISTINCT_CASE_FILTER_EQUIVALENCE"));
        assertTrue(aggregation.getPreconditions().contains("CASE_FALSE_BRANCH_RETURNS_NULL"));
        assertTrue(aggregation.getNullSemantics().contains("COUNT_DISTINCT_IGNORES_NULL"));
        assertTrue(String.valueOf(aggregation.getAttributes().get("equivalenceLaw")).contains("COUNT(DISTINCT CASE"));
    }

    @Test
    void shouldExposeSemanticEquivalenceReportThroughRewriteCoreIrSnapshot() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            repeatedAggregateLeftJoinSql(),
            DataSourceTypeEnum.HETU
        );

        RewriteCoreIrSnapshot snapshot = service.buildRewriteCoreIr(profile);

        assertNotNull(snapshot.getSemanticEquivalenceReport());
        assertTrue(snapshot.getSemanticEquivalenceReport().hasCheck(
            SemanticEquivalenceCheckType.CONSTRAINT_BASED_EQUIVALENCE
        ));
        assertTrue(snapshot.getSemanticEquivalenceReport().hasCheck(
            SemanticEquivalenceCheckType.STATISTICAL_AGGREGATION_EQUIVALENCE
        ));
        assertEquals(
            Integer.valueOf(snapshot.getSemanticEquivalenceReport().getChecks().size()),
            snapshot.getAttributes().get("semanticEquivalenceCheckCount")
        );
        assertEquals(
            snapshot.getSemanticEquivalenceReport().getStatus().name(),
            snapshot.getAttributes().get("semanticEquivalenceStatus")
        );
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

    @Test
    void shouldAnalyzeYonghongProductionReportSqlAndRecommendGovernedRewriteShapes() throws Exception {
        String sql = readRepositorySqlFixture("docs/test01.sql");

        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(sql, DataSourceTypeEnum.HETU);
        SqlOptimizationPipelineService.RecommendationRuleOutputModel model =
            service.buildRecommendationRuleOutputModel(profile);
        OptimizationTaskSuggestion rewriteSuggestion = service.buildRewriteSuggestion(profile);
        Map<String, Object> accelerationArtifact = L2AccelerationArtifactBuilder.buildForPrecomputeCandidate(
            new L2AccelerationArtifactBuilder.AccelerationRecommendationInput(
                sql,
                "HETU",
                "datasource-yonghong",
                "fingerprint-yonghong-million-customer-growth",
                "SZ_0000003772",
                "million-customer-growth",
                null
            ),
            profile
        );

        assertEquals("JSQLPARSER", profile.getParserEngine());
        assertTrue(containsText(profile.getTables(), "BIM_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM"), profile.getTables().toString());
        assertTrue(profile.getSubqueryCount() >= 20, "subqueryCount=" + profile.getSubqueryCount());
        assertTrue(profile.getNestedSubqueryDepth() >= 3, "nestedSubqueryDepth=" + profile.getNestedSubqueryDepth());
        assertTrue(profile.getRepeatedTableScanCount() >= 5, "repeatedTableScanCount=" + profile.getRepeatedTableScanCount());
        assertTrue(profile.getOrPredicateCount() >= 1, "orPredicateCount=" + profile.getOrPredicateCount());
        assertTrue(containsText(profile.getDatePredicateColumns(), "DTE"), profile.getDatePredicateColumns().toString());
        assertTrue(profile.getAggregateFunctions().contains("COUNT"), profile.getAggregateFunctions().toString());
        assertTrue(profile.getAggregateFunctionCount() >= 10, "aggregateFunctionCount=" + profile.getAggregateFunctionCount());
        assertTrue(profile.getWarnings().contains("REPEATED_TABLE_SCAN_RISK"), profile.getWarnings().toString());
        assertTrue(profile.getWarnings().contains("OR_PREDICATE_INDEX_RISK"), profile.getWarnings().toString());
        assertTrue(profile.getWarnings().contains("AGGREGATION_COMPLEXITY_RISK"), profile.getWarnings().toString());

        Map<String, Object> advancedProfile = profile.toAdvancedStructureProfile();
        assertEquals("AVAILABLE", advancedProfile.get("profileStatus"));
        assertTrue(String.valueOf(advancedProfile.get("projections")).contains("机构编码__第二层时点机构号"));
        assertTrue(String.valueOf(advancedProfile.get("projections")).contains("Sum_增速100"));
        assertTrue(String.valueOf(advancedProfile.get("predicates")).contains("WHERE_OR_"));

        assertTrue(containsRule(model.getUnappliedRules(), "OR_TO_UNION_ALL"));
        assertTrue(containsRule(model.getUnappliedRules(), "MULTI_COUNT_DISTINCT_DECOMPOSITION"));
        assertTrue(containsRule(model.getRuleChain(), "PRECOMPUTE_MV"));
        assertTrue(containsRule(model.getRuleChain(), "PARTITION_PRUNING"));
        assertTrue(containsRule(model.getRuleChain(), "REPORT_SQL_MERGE"));
        assertTrue(containsRule(model.getRuleChain(), L2SnapshotAggregateReportMvCandidateGenerator.RULE));
        assertFalse(model.isAutoApplyAllowed());

        String rewriteCandidateSql = rewriteSuggestion.getArtifacts().get(0).getContent();
        assertTrue(rewriteCandidateSql.contains("raw_customer_snapshot"), rewriteCandidateSql);
        assertTrue(rewriteCandidateSql.contains("report_customer_snapshot"), rewriteCandidateSql);
        assertTrue(rewriteCandidateSql.contains("base_100_anchor"), rewriteCandidateSql);
        assertTrue(rewriteCandidateSql.contains("metric_by_org"), rewriteCandidateSql);
        assertTrue(rewriteCandidateSql.contains("growth_by_org"), rewriteCandidateSql);
        assertFalse(rewriteCandidateSql.contains("GROUPING SETS"), rewriteCandidateSql);
        assertTrue(rewriteCandidateSql.contains("UNION ALL"), rewriteCandidateSql);
        assertTrue(rewriteCandidateSql.contains("'深圳市分行' AS report_org_label"), rewriteCandidateSql);
        assertTrue(rewriteCandidateSql.contains("branch_org_name AS report_org_label"), rewriteCandidateSql);
        assertTrue(rewriteCandidateSql.contains("深圳市分行营业部"), rewriteCandidateSql);
        assertTrue(rewriteCandidateSql.contains("snapshot_aum"), rewriteCandidateSql);
        assertTrue(rewriteCandidateSql.contains("base_aum < 1000000 AND current_aum >= 1000000"), rewriteCandidateSql);
        assertTrue(rewriteCandidateSql.contains("a.report_org_name AS \"机构编码__第二层机构简称\""), rewriteCandidateSql);
        assertTrue(rewriteCandidateSql.contains("\"Sum_增量100\""), rewriteCandidateSql);

        assertNotNull(accelerationArtifact);
        assertEquals("PARAMETERIZED_AGG_MV", accelerationArtifact.get("mvType"));
        assertEquals("REVIEW_REQUIRED", accelerationArtifact.get("artifactStatus"));
        assertFalse("EXACT_QUERY_MV".equals(accelerationArtifact.get("mvType")));
        assertEquals("PULL_ONLY_NOT_EXECUTED_BY_SQLFORGE", accelerationArtifact.get("governanceBoundary"));
        assertTrue(maps(accelerationArtifact.get("blockingReasons")).isEmpty());
        assertTrue(String.valueOf(accelerationArtifact.get("ddlSql")).contains("CREATE MATERIALIZED VIEW"));
        assertTrue(String.valueOf(accelerationArtifact.get("ddlSql")).contains("snapshot_aum"));
        assertTrue(String.valueOf(accelerationArtifact.get("ddlSql")).contains("report_org_name"));
        assertTrue(String.valueOf(accelerationArtifact.get("ddlSql")).contains("report_org_label"));
        assertFalse(String.valueOf(accelerationArtifact.get("ddlSql")).contains("GROUPING SETS"));
        assertTrue(String.valueOf(accelerationArtifact.get("ddlSql")).contains("UNION ALL"));
        assertTrue(String.valueOf(accelerationArtifact.get("rewriteSql")).contains("FROM "
            + accelerationArtifact.get("mvName")));
        assertTrue(String.valueOf(accelerationArtifact.get("rewriteSql")).contains("base_100_anchor"));
        assertTrue(String.valueOf(accelerationArtifact.get("rewriteSql")).contains("metric_by_org"));
        assertTrue(String.valueOf(accelerationArtifact.get("rewriteSql")).contains("growth_by_org"));
        assertFalse(String.valueOf(accelerationArtifact.get("rewriteSql"))
            .contains("BIM_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM"));
        assertTrue(String.valueOf(accelerationArtifact.get("validationSql")).contains("RESULT_SET_EXCEPT_DIFF"));
        assertTrue(String.valueOf(accelerationArtifact.get("validationSql")).contains("METRIC_SUM_DIFF"));
        assertTrue(String.valueOf(accelerationArtifact.get("validationSql")).contains("KEY_CARDINALITY_DIFF"));
        assertTrue(String.valueOf(accelerationArtifact.get("validationSql")).contains("ANCHOR_KEY_SET_DIFF"));
        assertTrue(String.valueOf(accelerationArtifact.get("validationSql")).contains("ORG_LABEL_SET_DIFF"));
        assertTrue(String.valueOf(accelerationArtifact.get("validationSql")).contains("METRIC_BY_KEY_DIFF"));
        List<Map<String, Object>> validationMethods = maps(accelerationArtifact.get("validationMethods"));
        assertTrue(validationMethods.size() >= 6, validationMethods.toString());
        assertTrue(hasCode(validationMethods, "RESULT_SET_EXCEPT_DIFF"));
        assertTrue(hasCode(validationMethods, "METRIC_SUM_DIFF"));
        assertTrue(hasCode(validationMethods, "ORG_LABEL_SET_DIFF"));
        assertTrue(hasCode(validationMethods, "METRIC_BY_KEY_DIFF"));
        assertTrue(hasCode(validationMethods, "PLAN_SHAPE_SCAN_REDUCTION"));
        assertTrue(String.valueOf(accelerationArtifact.get("rewriteEvidence")).contains("BASE_100_SUB34_EQUIVALENT"));
        assertTrue(String.valueOf(accelerationArtifact.get("rewriteEvidence")).contains("orgLabelLineage"));
    }

    @Test
    void shouldRecommendReportSnapshotRewriteForEquivalentNamingVariants() {
        String sql = reportSnapshotNamingVariantSql();

        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(sql, DataSourceTypeEnum.HETU);
        OptimizationTaskSuggestion rewriteSuggestion = service.buildRewriteSuggestion(profile);
        SqlOptimizationPipelineService.RecommendationRuleOutputModel model =
            service.buildRecommendationRuleOutputModel(profile);
        Map<String, Object> accelerationArtifact = L2AccelerationArtifactBuilder.buildForPrecomputeCandidate(
            new L2AccelerationArtifactBuilder.AccelerationRecommendationInput(
                sql,
                "HETU",
                "datasource-generic-report",
                "fingerprint-generic-report",
                "GENERIC_REPORT",
                "generic-report",
                null
            ),
            profile
        );

        assertTrue(profile.getRepeatedTableScanCount() >= 4, "repeatedTableScanCount=" + profile.getRepeatedTableScanCount());
        assertTrue(containsRule(model.getRuleChain(), L2SnapshotAggregateReportMvCandidateGenerator.RULE));
        String rewriteCandidateSql = rewriteSuggestion.getArtifacts().get(0).getContent();
        assertTrue(rewriteCandidateSql.contains("rpt_customer_asset_snapshot"), rewriteCandidateSql);
        assertTrue(rewriteCandidateSql.contains("FACT_CUSTOMER_ASSET__ORG_CODE_L2 AS org_level2_no"), rewriteCandidateSql);
        assertTrue(rewriteCandidateSql.contains("FACT_CUSTOMER_ASSET__ORG_SHORT_NAME_L3 AS branch_org_name"),
            rewriteCandidateSql);
        assertTrue(rewriteCandidateSql.contains("'深圳分行' AS report_org_label"), rewriteCandidateSql);
        assertTrue(rewriteCandidateSql.contains("FACT_CUSTOMER_ASSET__BIZ_DATE AS snapshot_date"), rewriteCandidateSql);
        assertFalse(rewriteCandidateSql.contains("GROUPING SETS"), rewriteCandidateSql);

        assertNotNull(accelerationArtifact);
        assertEquals("PARAMETERIZED_AGG_MV", accelerationArtifact.get("mvType"));
        assertTrue(String.valueOf(accelerationArtifact.get("ddlSql"))
            .contains("FACT_CUSTOMER_ASSET__ORG_CODE_L2 AS org_no_2"));
        assertTrue(String.valueOf(accelerationArtifact.get("ddlSql")).contains("UNION ALL"));
        assertFalse(String.valueOf(accelerationArtifact.get("ddlSql")).contains("GROUPING SETS"));
        assertTrue(String.valueOf(accelerationArtifact.get("rewriteEvidence")).contains("SZ001"));
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

    private String reportSnapshotNamingVariantSql() {
        return "SELECT b.org_code_l2, b.org_name_l2, b.org_label, b.base_100, c.current_100, "
            + "x.base_600, y.current_600, n.new_100\n"
            + "FROM (\n"
            + "  SELECT FACT_CUSTOMER_ASSET__ORG_CODE_L2 AS org_code_l2,\n"
            + "    FACT_CUSTOMER_ASSET__ORG_NAME_L2 AS org_name_l2,\n"
            + "    '深圳分行' AS org_label,\n"
            + "    COUNT(DISTINCT FACT_CUSTOMER_ASSET__CUSTOMER_ID) AS base_100\n"
            + "  FROM rpt_customer_asset_snapshot\n"
            + "  WHERE (FACT_CUSTOMER_ASSET__ORG_CODE_L2 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L3 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L4 = 'SZ001')\n"
            + "    AND FACT_CUSTOMER_ASSET__ORG_LEVEL = 4\n"
            + "    AND FACT_CUSTOMER_ASSET__BIZ_DATE = '2026-04-30'\n"
            + "    AND FACT_CUSTOMER_ASSET__AVG_BALANCE >= 1000000\n"
            + "  GROUP BY FACT_CUSTOMER_ASSET__ORG_CODE_L2, FACT_CUSTOMER_ASSET__ORG_NAME_L2\n"
            + ") b\n"
            + "LEFT JOIN (\n"
            + "  SELECT FACT_CUSTOMER_ASSET__ORG_CODE_L2 AS org_code_l2,\n"
            + "    COUNT(DISTINCT FACT_CUSTOMER_ASSET__CUSTOMER_ID) AS current_100\n"
            + "  FROM rpt_customer_asset_snapshot\n"
            + "  WHERE (FACT_CUSTOMER_ASSET__ORG_CODE_L2 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L3 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L4 = 'SZ001')\n"
            + "    AND FACT_CUSTOMER_ASSET__ORG_LEVEL = 4\n"
            + "    AND FACT_CUSTOMER_ASSET__BIZ_DATE = '2026-05-31'\n"
            + "    AND FACT_CUSTOMER_ASSET__AVG_BALANCE >= 1000000\n"
            + "  GROUP BY FACT_CUSTOMER_ASSET__ORG_CODE_L2\n"
            + ") c ON b.org_code_l2 = c.org_code_l2\n"
            + "LEFT JOIN (\n"
            + "  SELECT FACT_CUSTOMER_ASSET__ORG_CODE_L2 AS org_code_l2,\n"
            + "    COUNT(DISTINCT FACT_CUSTOMER_ASSET__CUSTOMER_ID) AS base_600\n"
            + "  FROM rpt_customer_asset_snapshot\n"
            + "  WHERE (FACT_CUSTOMER_ASSET__ORG_CODE_L2 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L3 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L4 = 'SZ001')\n"
            + "    AND FACT_CUSTOMER_ASSET__ORG_LEVEL = 4\n"
            + "    AND FACT_CUSTOMER_ASSET__BIZ_DATE = '2026-04-30'\n"
            + "    AND FACT_CUSTOMER_ASSET__AVG_BALANCE >= 6000000\n"
            + "  GROUP BY FACT_CUSTOMER_ASSET__ORG_CODE_L2\n"
            + ") x ON b.org_code_l2 = x.org_code_l2\n"
            + "LEFT JOIN (\n"
            + "  SELECT FACT_CUSTOMER_ASSET__ORG_CODE_L2 AS org_code_l2,\n"
            + "    COUNT(DISTINCT FACT_CUSTOMER_ASSET__CUSTOMER_ID) AS current_600\n"
            + "  FROM rpt_customer_asset_snapshot\n"
            + "  WHERE (FACT_CUSTOMER_ASSET__ORG_CODE_L2 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L3 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L4 = 'SZ001')\n"
            + "    AND FACT_CUSTOMER_ASSET__ORG_LEVEL = 4\n"
            + "    AND FACT_CUSTOMER_ASSET__BIZ_DATE = '2026-05-31'\n"
            + "    AND FACT_CUSTOMER_ASSET__AVG_BALANCE >= 6000000\n"
            + "  GROUP BY FACT_CUSTOMER_ASSET__ORG_CODE_L2\n"
            + ") y ON b.org_code_l2 = y.org_code_l2\n"
            + "LEFT JOIN (\n"
            + "  SELECT FACT_CUSTOMER_ASSET__ORG_CODE_L3 AS org_code_l3,\n"
            + "    FACT_CUSTOMER_ASSET__ORG_SHORT_NAME_L3 AS branch_name,\n"
            + "    COUNT(DISTINCT FACT_CUSTOMER_ASSET__CUSTOMER_ID) AS new_100\n"
            + "  FROM rpt_customer_asset_snapshot\n"
            + "  WHERE (FACT_CUSTOMER_ASSET__ORG_CODE_L2 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L3 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L4 = 'SZ001')\n"
            + "    AND FACT_CUSTOMER_ASSET__ORG_LEVEL = 4\n"
            + "    AND FACT_CUSTOMER_ASSET__BIZ_DATE = '2026-05-31'\n"
            + "    AND FACT_CUSTOMER_ASSET__AVG_BALANCE >= 1000000\n"
            + "  GROUP BY FACT_CUSTOMER_ASSET__ORG_CODE_L3, FACT_CUSTOMER_ASSET__ORG_SHORT_NAME_L3\n"
            + ") n ON b.org_code_l2 = n.org_code_l3";
    }

    private boolean containsRule(List<Map<String, Object>> entries, String rule) {
        for (Map<String, Object> entry : entries) {
            if (rule.equals(entry.get("rule"))) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> rule(List<Map<String, Object>> entries, String rule) {
        for (Map<String, Object> entry : entries) {
            if (rule.equals(entry.get("rule"))) {
                return entry;
            }
        }
        throw new AssertionError("Missing rule " + rule);
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

    private boolean containsText(Iterable<String> values, String expectedText) {
        for (String value : values) {
            if (value != null && value.contains(expectedText)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCode(List<Map<String, Object>> entries, String code) {
        for (Map<String, Object> entry : entries) {
            if (code.equals(entry.get("code"))) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?>)) {
            return java.util.Collections.emptyList();
        }
        return (List<Map<String, Object>>) value;
    }

    private String readRepositorySqlFixture(String relativePath) throws Exception {
        Path root = Paths.get("").toAbsolutePath();
        Path fixture = root.resolve(relativePath);
        if (!Files.exists(fixture)) {
            fixture = root.resolve("..").resolve(relativePath).normalize();
        }
        assertTrue(Files.exists(fixture), "缺少 SQL fixture：" + fixture);
        return new String(Files.readAllBytes(fixture), StandardCharsets.UTF_8);
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

    private boolean containsOperator(RewriteCoreIrSnapshot snapshot, RelationalOperator operator) {
        for (com.company.sqloptimization.domain.rewrite.ir.RelationalAlgebraNode node : snapshot.getRelationalAlgebra()) {
            if (operator == node.getOperator()) {
                return true;
            }
        }
        return false;
    }

    private boolean containsConflict(RewriteCoreIrSnapshot snapshot, String conflictCode) {
        for (RewriteIrConflict conflict : snapshot.getArchitectureConflicts()) {
            if (conflictCode.equals(conflict.getConflictCode())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsDagEdge(QueryBlockDag dag, String edgeType) {
        for (QueryBlockEdge edge : dag.getEdges()) {
            if (edgeType.equals(edge.getEdgeType())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsDagIssue(QueryBlockDag dag, String issueCode) {
        for (QueryBlockDagIssue issue : dag.getIssues()) {
            if (issueCode.equals(issue.getCode())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExternalReference(QueryBlockDag dag, String expected) {
        for (QueryBlockNode block : dag.getBlocks()) {
            for (String reference : block.getExternalReferences()) {
                if (expected.equalsIgnoreCase(reference)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasEquivalentQueryBlock(RewriteCoreIrSnapshot snapshot) {
        for (com.company.sqloptimization.domain.rewrite.ir.QueryBlockIr block : snapshot.getQueryBlocks()) {
            if (Boolean.TRUE.equals(block.getAttributes().get("equivalentToRepresentative"))) {
                return true;
            }
        }
        return false;
    }

    private RelationalRewriteCandidate firstCandidate(RelationalRewritePlan plan, RelationalRewriteRuleType ruleType) {
        List<RelationalRewriteCandidate> candidates = plan.candidatesOf(ruleType);
        assertFalse(candidates.isEmpty(), "缺少关系代数改写候选：" + ruleType);
        return candidates.get(0);
    }

    private SemanticEquivalenceCheck firstSemanticCheck(SemanticEquivalenceReport report,
                                                        SemanticEquivalenceCheckType checkType,
                                                        RelationalRewriteRuleType ruleType) {
        for (SemanticEquivalenceCheck check : report.getChecks()) {
            if (checkType == check.getCheckType() && ruleType == check.getRuleType()) {
                return check;
            }
        }
        assertNotNull(null, "缺少语义等价验证：" + checkType + " / " + ruleType);
        return null;
    }

    private String repeatedAggregateLeftJoinSql() {
        return "SELECT b.customer_id, s1.base_100, s2.current_100, s3.base_600 "
            + "FROM customers b "
            + "LEFT JOIN ("
            + "  SELECT customer_id, COUNT(DISTINCT customer_id) AS base_100 "
            + "  FROM customer_snapshot "
            + "  WHERE dt = DATE '2026-04-30' AND avg_balance >= 1000000 "
            + "  GROUP BY customer_id"
            + ") s1 ON b.customer_id = s1.customer_id "
            + "LEFT JOIN ("
            + "  SELECT customer_id, COUNT(DISTINCT customer_id) AS current_100 "
            + "  FROM customer_snapshot "
            + "  WHERE dt = DATE '2026-05-31' AND avg_balance >= 1000000 "
            + "  GROUP BY customer_id"
            + ") s2 ON b.customer_id = s2.customer_id "
            + "LEFT JOIN ("
            + "  SELECT customer_id, COUNT(DISTINCT customer_id) AS base_600 "
            + "  FROM customer_snapshot "
            + "  WHERE dt = DATE '2026-04-30' AND avg_balance >= 6000000 "
            + "  GROUP BY customer_id"
            + ") s3 ON b.customer_id = s3.customer_id";
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
