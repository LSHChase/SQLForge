package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.ValidationSqlChecks.groupKeyDiffCheck;
import static com.company.sqloptimization.application.service.ValidationSqlChecks.groupMeasureDiffCheck;
import static com.company.sqloptimization.application.service.ValidationSqlChecks.measureDiffCheck;
import static com.company.sqloptimization.application.service.ValidationSqlChecks.rowCountCheck;
import static com.company.sqloptimization.application.service.ValidationSqlProfileColumns.commonSubgraphOutputColumns;
import static com.company.sqloptimization.application.service.ValidationSqlProfileColumns.groupKeyColumns;
import static com.company.sqloptimization.application.service.ValidationSqlProfileColumns.measureColumns;
import static com.company.sqloptimization.application.service.ValidationSqlRenderer.groupedResultSql;
import static com.company.sqloptimization.application.service.ValidationSqlRenderer.mvSubgraphResultSql;
import static com.company.sqloptimization.application.service.ValidationSqlRenderer.render;
import static com.company.sqloptimization.application.service.ValidationSqlText.trimTrailingSemicolon;
import static com.company.sqloptimization.application.service.ValidationSqlValues.reason;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class ValidationSqlCoordinator {

    private static final Set<String> AGGREGATION_MV_TYPES =
        new LinkedHashSet<String>(Arrays.asList(
            L2GrainMeasureDeriver.MV_TYPE_PARAMETERIZED_AGG,
            L2GrainMeasureDeriver.MV_TYPE_PREJOIN,
            L2GrainMeasureDeriver.MV_TYPE_STAR_AGG,
            L2GrainMeasureDeriver.MV_TYPE_ROLLUP
        ));

    private ValidationSqlCoordinator() {
    }

    static L2MaterializedViewValidationSqlBuilder.ValidationSqlResult build(
        L2MaterializedViewValidationSqlBuilder.ValidationInput input) {
        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        if (input == null) {
            blockingReasons.add(reason("VALIDATION_INPUT_REQUIRED", "缺少验证 SQL 生成输入，不能输出可激活物化视图产物。"));
            return L2MaterializedViewValidationSqlBuilder.ValidationSqlResult.blocked(blockingReasons);
        }

        String sourceSql = trimTrailingSemicolon(input.sourceSql);
        String rewriteSql = trimTrailingSemicolon(input.rewriteSql);
        validateRequiredInput(input, sourceSql, rewriteSql, blockingReasons);

        boolean commonSubgraph = L2GrainMeasureDeriver.MV_TYPE_COMMON_SUBGRAPH.equals(input.mvType);
        List<ValidationSqlColumn> groupKeys = commonSubgraph
            ? java.util.Collections.<ValidationSqlColumn>emptyList()
            : groupKeyColumns(input.advancedStructureProfile, blockingReasons);
        List<ValidationSqlMeasure> measures = commonSubgraph
            ? java.util.Collections.<ValidationSqlMeasure>emptyList()
            : measureColumns(input.measures, blockingReasons);
        if (AGGREGATION_MV_TYPES.contains(input.mvType) && measures.isEmpty() && groupKeys.isEmpty()) {
            blockingReasons.add(reason(
                "VALIDATION_MEASURE_REQUIRED",
                "缺少可对比指标字段或分组键，不能生成聚合物化视图结果验证 SQL。"
            ));
        }
        List<String> commonSubgraphOutputColumns = commonSubgraphOutputColumns(input, blockingReasons);
        if (!blockingReasons.isEmpty()) {
            return L2MaterializedViewValidationSqlBuilder.ValidationSqlResult.blocked(blockingReasons);
        }

        return L2MaterializedViewValidationSqlBuilder.ValidationSqlResult.generated(render(
            ctes(input, sourceSql, rewriteSql, commonSubgraph, groupKeys, measures, commonSubgraphOutputColumns),
            checks(input.mvType, commonSubgraph, groupKeys, measures)
        ));
    }

    private static void validateRequiredInput(L2MaterializedViewValidationSqlBuilder.ValidationInput input,
                                              String sourceSql,
                                              String rewriteSql,
                                              List<Map<String, Object>> blockingReasons) {
        if (!StringUtils.hasText(sourceSql)) {
            blockingReasons.add(reason("VALIDATION_SOURCE_SQL_REQUIRED", "缺少原 SQL，不能生成 original_result 验证 CTE。"));
        }
        if (!StringUtils.hasText(rewriteSql)) {
            blockingReasons.add(reason("VALIDATION_REWRITE_SQL_REQUIRED", "缺少 MV rewrite SQL，不能生成 rewrite_result 验证 CTE。"));
        }
        if (!StringUtils.hasText(input.mvType)) {
            blockingReasons.add(reason("VALIDATION_MV_TYPE_REQUIRED", "缺少物化视图类型，不能选择类型专属验证检查。"));
        }
    }

    private static List<ValidationSqlCte> ctes(
        L2MaterializedViewValidationSqlBuilder.ValidationInput input,
        String sourceSql,
        String rewriteSql,
        boolean commonSubgraph,
        List<ValidationSqlColumn> groupKeys,
        List<ValidationSqlMeasure> measures,
        List<String> commonSubgraphOutputColumns) {
        List<ValidationSqlCte> ctes = new ArrayList<ValidationSqlCte>();
        ctes.add(new ValidationSqlCte("original_result", sourceSql));
        ctes.add(new ValidationSqlCte("rewrite_result", rewriteSql));
        if (!groupKeys.isEmpty()) {
            ctes.add(new ValidationSqlCte("original_group", groupedResultSql(
                "original_result", groupKeys, measures, "_validation_original_marker"
            )));
            ctes.add(new ValidationSqlCte("rewrite_group", groupedResultSql(
                "rewrite_result", groupKeys, measures, "_validation_rewrite_marker"
            )));
        }
        if (commonSubgraph) {
            ctes.add(new ValidationSqlCte("common_subgraph_result", trimTrailingSemicolon(input.commonSubgraphSql)));
            ctes.add(new ValidationSqlCte("mv_subgraph_result", mvSubgraphResultSql(input.mvName, commonSubgraphOutputColumns)));
        }
        return ctes;
    }

    private static List<String> checks(String mvType,
                                       boolean commonSubgraph,
                                       List<ValidationSqlColumn> groupKeys,
                                       List<ValidationSqlMeasure> measures) {
        List<String> checks = new ArrayList<String>();
        checks.add(rowCountCheck(
            "ROW_COUNT_CHECK",
            "final_result",
            "original_result",
            "rewrite_result",
            "original_result row count compared with rewrite_result row count"
        ));
        addJoinAndCommonSubgraphChecks(checks, mvType, commonSubgraph);
        for (ValidationSqlMeasure measure : measures) {
            checks.add(measureDiffCheck(measure));
        }
        if (!groupKeys.isEmpty()) {
            checks.add(groupKeyDiffCheck(groupKeys));
            for (ValidationSqlMeasure measure : measures) {
                checks.add(groupMeasureDiffCheck(measure, groupKeys));
            }
        }
        return checks;
    }

    private static void addJoinAndCommonSubgraphChecks(List<String> checks, String mvType, boolean commonSubgraph) {
        if (L2GrainMeasureDeriver.MV_TYPE_PREJOIN.equals(mvType)
            || L2GrainMeasureDeriver.MV_TYPE_STAR_AGG.equals(mvType)) {
            checks.add(rowCountCheck(
                "JOIN_ROW_COUNT_CHECK",
                "post_join_result",
                "original_result",
                "rewrite_result",
                "post-join original result row count compared with MV rewrite result row count"
            ));
        }
        if (commonSubgraph) {
            checks.add(rowCountCheck(
                "COMMON_SUBGRAPH_OUTPUT_CHECK",
                "common_subgraph_output",
                "common_subgraph_result",
                "mv_subgraph_result",
                "selected common subgraph output compared with materialized view output"
            ));
            checks.add(rowCountCheck(
                "UPPER_REWRITE_RESULT_CHECK",
                "upper_query_result",
                "original_result",
                "rewrite_result",
                "upper query result compared after replacing the common subgraph with the MV"
            ));
        }
    }
}
