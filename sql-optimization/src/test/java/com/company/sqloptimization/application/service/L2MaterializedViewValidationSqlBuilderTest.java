package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class L2MaterializedViewValidationSqlBuilderTest {

    private final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    @Test
    void shouldBuildAggregationValidationSqlWithRowMeasureAndGroupChecks() {
        String sourceSql = "SELECT customer_id, SUM(amount) AS total_amount FROM orders GROUP BY customer_id";
        Map<String, Object> profile = advancedProfile(sourceSql);
        L2GrainMeasureDeriver.DerivationResult derivation = derivation(profile);

        L2MaterializedViewValidationSqlBuilder.ValidationSqlResult result =
            L2MaterializedViewValidationSqlBuilder.build(
                new L2MaterializedViewValidationSqlBuilder.ValidationInput(
                    L2GrainMeasureDeriver.MV_TYPE_PARAMETERIZED_AGG,
                    sourceSql,
                    "SELECT customer_id, SUM(total_amount) AS total_amount FROM mv_sales GROUP BY customer_id",
                    "mv_sales",
                    profile,
                    derivation.getMeasures(),
                    null,
                    Collections.<String>emptyList()
                )
            );

        assertTrue(result.isGenerated(), String.valueOf(result.getBlockingReasons()));
        String validationSql = result.getValidationSql();
        assertTrue(validationSql.contains("original_result"));
        assertTrue(validationSql.contains("rewrite_result"));
        assertTrue(validationSql.contains("ROW_COUNT_CHECK"));
        assertTrue(validationSql.contains("MEASURE_DIFF"));
        assertTrue(validationSql.contains("GROUP_MEASURE_DIFF"));
        assertTrue(validationSql.contains("GROUP_KEY_DIFF"));
    }

    @Test
    void shouldBuildJoinAndCommonSubgraphSpecificChecks() {
        String joinSql = "SELECT c.customer_level, SUM(o.amount) AS total_amount "
            + "FROM orders o JOIN customers c ON o.customer_id = c.customer_id GROUP BY c.customer_level";
        Map<String, Object> joinProfile = advancedProfile(joinSql);
        L2GrainMeasureDeriver.DerivationResult joinDerivation = derivation(joinProfile);

        L2MaterializedViewValidationSqlBuilder.ValidationSqlResult joinResult =
            L2MaterializedViewValidationSqlBuilder.build(
                new L2MaterializedViewValidationSqlBuilder.ValidationInput(
                    L2GrainMeasureDeriver.MV_TYPE_PREJOIN,
                    joinSql,
                    "SELECT customer_level, SUM(amount) AS total_amount FROM mv_prejoin GROUP BY customer_level",
                    "mv_prejoin",
                    joinProfile,
                    joinDerivation.getMeasures(),
                    null,
                    Collections.<String>emptyList()
                )
            );

        assertTrue(joinResult.isGenerated(), String.valueOf(joinResult.getBlockingReasons()));
        assertTrue(joinResult.getValidationSql().contains("JOIN_ROW_COUNT_CHECK"));

        String commonSql = "WITH recent_orders AS (SELECT customer_id, amount, region FROM orders "
            + "WHERE status = 'PAID') SELECT customer_id, SUM(amount) AS total_amount "
            + "FROM recent_orders WHERE region = 'CN' GROUP BY customer_id";
        Map<String, Object> commonProfile = advancedProfile(commonSql);
        L2GrainMeasureDeriver.DerivationResult commonDerivation = derivation(commonProfile);
        L2MaterializedViewValidationSqlBuilder.ValidationSqlResult commonResult =
            L2MaterializedViewValidationSqlBuilder.build(
                new L2MaterializedViewValidationSqlBuilder.ValidationInput(
                    L2GrainMeasureDeriver.MV_TYPE_COMMON_SUBGRAPH,
                    commonSql,
                    "SELECT customer_id, SUM(amount) AS total_amount FROM mv_common "
                        + "WHERE region = 'CN' GROUP BY customer_id",
                    "mv_common",
                    commonProfile,
                    commonDerivation.getMeasures(),
                    "SELECT customer_id, amount, region FROM orders WHERE status = 'PAID'",
                    asList("customer_id", "amount", "region")
                )
            );

        assertTrue(commonResult.isGenerated(), String.valueOf(commonResult.getBlockingReasons()));
        assertTrue(commonResult.getValidationSql().contains("COMMON_SUBGRAPH_OUTPUT_CHECK"));
        assertTrue(commonResult.getValidationSql().contains("UPPER_REWRITE_RESULT_CHECK"));
    }

    @Test
    void shouldBlockWhenValidationFieldCannotBeResolved() {
        String sourceSql = "SELECT customer_id, SUM(amount) AS total_amount FROM orders GROUP BY customer_id";
        Map<String, Object> badMeasure = new LinkedHashMap<String, Object>();
        badMeasure.put("name", "SUM(amount)");
        badMeasure.put("measureType", "SUM");
        badMeasure.put("mergeable", Boolean.TRUE);

        L2MaterializedViewValidationSqlBuilder.ValidationSqlResult result =
            L2MaterializedViewValidationSqlBuilder.build(
                new L2MaterializedViewValidationSqlBuilder.ValidationInput(
                    L2GrainMeasureDeriver.MV_TYPE_PARAMETERIZED_AGG,
                    sourceSql,
                    "SELECT customer_id, SUM(total_amount) AS total_amount FROM mv_sales GROUP BY customer_id",
                    "mv_sales",
                    advancedProfile(sourceSql),
                    Collections.singletonList(badMeasure),
                    null,
                    Collections.<String>emptyList()
                )
            );

        assertFalse(result.isGenerated());
        assertTrue(hasReason(result.getBlockingReasons(), "VALIDATION_MEASURE_COLUMN_UNRESOLVED"));
    }

    private Map<String, Object> advancedProfile(String sql) {
        return service.analyze(sql, DataSourceTypeEnum.HETU).toAdvancedStructureProfile();
    }

    private L2GrainMeasureDeriver.DerivationResult derivation(Map<String, Object> advancedProfile) {
        return L2GrainMeasureDeriver.derive(
            advancedProfile,
            L2PredicateClassifier.classify(advancedProfile)
        );
    }

    private static List<String> asList(String first, String second, String third) {
        return java.util.Arrays.asList(first, second, third);
    }

    private static boolean hasReason(List<Map<String, Object>> reasons, String code) {
        for (Map<String, Object> reason : reasons) {
            if (code.equals(reason.get("code"))) {
                return true;
            }
        }
        return false;
    }
}
