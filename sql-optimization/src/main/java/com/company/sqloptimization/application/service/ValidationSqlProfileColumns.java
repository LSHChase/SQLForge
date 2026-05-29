package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.ValidationSqlText.cleanOutputIdentifier;
import static com.company.sqloptimization.application.service.ValidationSqlText.isSafeIdentifier;
import static com.company.sqloptimization.application.service.ValidationSqlText.isSimpleColumnExpression;
import static com.company.sqloptimization.application.service.ValidationSqlText.normalizeExpression;
import static com.company.sqloptimization.application.service.ValidationSqlText.sqlIdentifier;
import static com.company.sqloptimization.application.service.ValidationSqlText.stripAlias;
import static com.company.sqloptimization.application.service.ValidationSqlText.unqualifiedName;
import static com.company.sqloptimization.application.service.ValidationSqlValues.booleanValue;
import static com.company.sqloptimization.application.service.ValidationSqlValues.mapList;
import static com.company.sqloptimization.application.service.ValidationSqlValues.reason;
import static com.company.sqloptimization.application.service.ValidationSqlValues.stringList;
import static com.company.sqloptimization.application.service.ValidationSqlValues.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class ValidationSqlProfileColumns {

    private ValidationSqlProfileColumns() {
    }

    static List<String> commonSubgraphOutputColumns(
        L2MaterializedViewValidationSqlBuilder.ValidationInput input,
        List<Map<String, Object>> blockingReasons) {
        if (!L2GrainMeasureDeriver.MV_TYPE_COMMON_SUBGRAPH.equals(input.mvType)) {
            return Collections.emptyList();
        }
        if (!StringUtils.hasText(input.commonSubgraphSql)) {
            blockingReasons.add(reason(
                "VALIDATION_COMMON_SUBGRAPH_SQL_REQUIRED",
                "缺少公共子图 SQL，不能生成 common_subgraph_result 验证 CTE。"
            ));
        }
        if (!StringUtils.hasText(input.mvName) || !isSafeIdentifier(input.mvName)) {
            blockingReasons.add(reason("VALIDATION_MV_NAME_REQUIRED", "缺少可安全引用的 MV 名称，不能生成公共子图输出对比。"));
        }
        return sanitizedCommonSubgraphColumns(input.commonSubgraphOutputColumns, blockingReasons);
    }

    static List<ValidationSqlColumn> groupKeyColumns(Map<String, Object> advancedStructureProfile,
                                                     List<Map<String, Object>> blockingReasons) {
        List<ValidationSqlColumn> columns = new ArrayList<ValidationSqlColumn>();
        Set<String> seen = new LinkedHashSet<String>();
        List<Map<String, Object>> projections = mapList(
            advancedStructureProfile == null ? null : advancedStructureProfile.get("projections")
        );
        for (Map<String, Object> groupBy : mapList(
            advancedStructureProfile == null ? null : advancedStructureProfile.get("groupBy")
        )) {
            String expression = text(groupBy.get("expression"));
            String outputName = outputNameForExpression(groupBy, projections);
            if (!StringUtils.hasText(outputName) || !isSafeIdentifier(outputName)) {
                Map<String, Object> reason = reason(
                    "VALIDATION_GROUP_KEY_COLUMN_UNRESOLVED",
                    "GROUP BY 字段无法映射为 original_result/rewrite_result 的输出列，不能生成分组差异检查。"
                );
                reason.put("expression", expression);
                blockingReasons.add(reason);
                continue;
            }
            if (seen.add(outputName.toUpperCase(Locale.ROOT))) {
                columns.add(new ValidationSqlColumn(outputName));
            }
        }
        return columns;
    }

    static List<ValidationSqlMeasure> measureColumns(List<Map<String, Object>> sourceMeasures,
                                                     List<Map<String, Object>> blockingReasons) {
        List<ValidationSqlMeasure> measures = new ArrayList<ValidationSqlMeasure>();
        Set<String> seen = new LinkedHashSet<String>();
        for (Map<String, Object> measure : sourceMeasures == null
            ? Collections.<Map<String, Object>>emptyList()
            : sourceMeasures) {
            if (!booleanValue(measure.get("mergeable"))) {
                continue;
            }
            String name = cleanOutputIdentifier(text(measure.get("name")));
            if (!StringUtils.hasText(name) || !isSafeIdentifier(name)) {
                Map<String, Object> reason = reason(
                    "VALIDATION_MEASURE_COLUMN_UNRESOLVED",
                    "指标字段无法安全引用，不能生成指标差异检查。"
                );
                reason.put("measureName", text(measure.get("name")));
                blockingReasons.add(reason);
                continue;
            }
            if (seen.add(name.toUpperCase(Locale.ROOT))) {
                measures.add(new ValidationSqlMeasure(name, text(measure.get("measureType"))));
            }
        }
        return measures;
    }

    private static List<String> sanitizedCommonSubgraphColumns(List<String> outputColumns,
                                                               List<Map<String, Object>> blockingReasons) {
        List<String> columns = new ArrayList<String>();
        Set<String> seen = new LinkedHashSet<String>();
        for (String outputColumn : outputColumns) {
            String column = cleanOutputIdentifier(outputColumn);
            if (!StringUtils.hasText(column)) {
                Map<String, Object> reason = reason(
                    "VALIDATION_COMMON_SUBGRAPH_OUTPUT_UNRESOLVED",
                    "公共子图输出字段无法安全引用，不能生成公共子图输出差异检查。"
                );
                reason.put("outputColumn", outputColumn);
                blockingReasons.add(reason);
            } else if (seen.add(column.toUpperCase(Locale.ROOT))) {
                columns.add(sqlIdentifier(column));
            }
        }
        if (columns.isEmpty()) {
            blockingReasons.add(reason("VALIDATION_COMMON_SUBGRAPH_OUTPUT_REQUIRED", "缺少公共子图输出字段，不能生成公共子图输出差异检查。"));
        }
        return columns;
    }

    private static String outputNameForExpression(Map<String, Object> groupBy, List<Map<String, Object>> projections) {
        String expression = normalizeExpression(text(groupBy.get("expression")));
        List<String> sourceColumns = stringList(groupBy.get("sourceColumns"));
        for (Map<String, Object> projection : projections) {
            String projectionExpression = normalizeExpression(stripAlias(
                text(projection.get("expression")),
                text(projection.get("alias"))
            ));
            String alias = cleanOutputIdentifier(text(projection.get("alias")));
            if (StringUtils.hasText(alias)
                && (expression.equals(projectionExpression)
                    || sameSourceColumns(sourceColumns, stringList(projection.get("sourceColumns"))))) {
                return alias;
            }
        }
        if (isSimpleColumnExpression(text(groupBy.get("expression")))) {
            return cleanOutputIdentifier(unqualifiedName(text(groupBy.get("expression"))));
        }
        return "";
    }

    private static boolean sameSourceColumns(List<String> left, List<String> right) {
        if (left.isEmpty() || right.isEmpty() || left.size() != right.size()) {
            return false;
        }
        Set<String> normalizedLeft = new LinkedHashSet<String>();
        for (String item : left) {
            normalizedLeft.add(normalizeExpression(item));
        }
        Set<String> normalizedRight = new LinkedHashSet<String>();
        for (String item : right) {
            normalizedRight.add(normalizeExpression(item));
        }
        return normalizedLeft.equals(normalizedRight);
    }
}
