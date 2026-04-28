package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationTaskSuggestion;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

        assertTrue(suggestion.getSummary().contains("2 table(s)"));
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
        assertTrue(suggestion.getSummary().contains("acceleration recommendation"));
    }

    @Test
    void shouldAnalyzeQueryShapeWithTrinoParserAdapter() {
        SqlOptimizationPipelineService trinoService = new SqlOptimizationPipelineService();
        ReflectionTestUtils.setField(trinoService, "parserStrategy", "TRINO");

        SqlOptimizationPipelineService.ParsedSqlProfile profile = trinoService.analyze(
            "SELECT customer_id, sum(amount) OVER (PARTITION BY customer_id) total_amount "
                + "FROM hive.sales.orders WHERE dt >= DATE '2026-04-01' LIMIT 10",
            DataSourceTypeEnum.HETU
        );

        assertEquals("TRINO", profile.getParserEngine());
        assertEquals(1, profile.getTables().size());
        assertEquals(1, profile.getWindowFunctionCount());
        assertTrue(profile.isLimitPresent());
    }
}
