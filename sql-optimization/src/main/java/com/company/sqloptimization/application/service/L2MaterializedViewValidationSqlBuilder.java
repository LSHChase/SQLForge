package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class L2MaterializedViewValidationSqlBuilder {

    private static final Pattern SAFE_IDENTIFIER_PATTERN =
        Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Set<String> AGGREGATION_MV_TYPES =
        new LinkedHashSet<String>(Arrays.asList(
            L2GrainMeasureDeriver.MV_TYPE_PARAMETERIZED_AGG,
            L2GrainMeasureDeriver.MV_TYPE_PREJOIN,
            L2GrainMeasureDeriver.MV_TYPE_STAR_AGG,
            L2GrainMeasureDeriver.MV_TYPE_ROLLUP
        ));

    private L2MaterializedViewValidationSqlBuilder() {
    }

    static ValidationSqlResult build(ValidationInput input) {
        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        if (input == null) {
            blockingReasons.add(reason(
                "VALIDATION_INPUT_REQUIRED",
                "缺少验证 SQL 生成输入，不能输出可激活物化视图产物。"
            ));
            return ValidationSqlResult.blocked(blockingReasons);
        }

        String sourceSql = trimTrailingSemicolon(input.sourceSql);
        String rewriteSql = trimTrailingSemicolon(input.rewriteSql);
        if (!StringUtils.hasText(sourceSql)) {
            blockingReasons.add(reason(
                "VALIDATION_SOURCE_SQL_REQUIRED",
                "缺少原 SQL，不能生成 original_result 验证 CTE。"
            ));
        }
        if (!StringUtils.hasText(rewriteSql)) {
            blockingReasons.add(reason(
                "VALIDATION_REWRITE_SQL_REQUIRED",
                "缺少 MV rewrite SQL，不能生成 rewrite_result 验证 CTE。"
            ));
        }
        if (!StringUtils.hasText(input.mvType)) {
            blockingReasons.add(reason(
                "VALIDATION_MV_TYPE_REQUIRED",
                "缺少物化视图类型，不能选择类型专属验证检查。"
            ));
        }

        boolean commonSubgraph = L2GrainMeasureDeriver.MV_TYPE_COMMON_SUBGRAPH.equals(input.mvType);
        List<ValidationColumn> groupKeys = commonSubgraph
            ? Collections.<ValidationColumn>emptyList()
            : groupKeyColumns(input.advancedStructureProfile, blockingReasons);
        List<ValidationMeasure> measures = commonSubgraph
            ? Collections.<ValidationMeasure>emptyList()
            : measureColumns(input.measures, blockingReasons);
        if (AGGREGATION_MV_TYPES.contains(input.mvType) && measures.isEmpty() && groupKeys.isEmpty()) {
            blockingReasons.add(reason(
                "VALIDATION_MEASURE_REQUIRED",
                "缺少可对比指标字段或分组键，不能生成聚合物化视图结果验证 SQL。"
            ));
        }
        List<String> commonSubgraphOutputColumns = commonSubgraphOutputColumns(input, blockingReasons);

        if (!blockingReasons.isEmpty()) {
            return ValidationSqlResult.blocked(blockingReasons);
        }

        StringBuilder sql = new StringBuilder();
        List<Cte> ctes = new ArrayList<Cte>();
        ctes.add(new Cte("original_result", sourceSql));
        ctes.add(new Cte("rewrite_result", rewriteSql));
        if (!groupKeys.isEmpty()) {
            ctes.add(new Cte("original_group", groupedResultSql(
                "original_result",
                groupKeys,
                measures,
                "_validation_original_marker"
            )));
            ctes.add(new Cte("rewrite_group", groupedResultSql(
                "rewrite_result",
                groupKeys,
                measures,
                "_validation_rewrite_marker"
            )));
        }
        if (commonSubgraph) {
            ctes.add(new Cte("common_subgraph_result", trimTrailingSemicolon(input.commonSubgraphSql)));
            ctes.add(new Cte("mv_subgraph_result", mvSubgraphResultSql(input.mvName, commonSubgraphOutputColumns)));
        }

        sql.append("WITH ");
        for (int i = 0; i < ctes.size(); i++) {
            if (i > 0) {
                sql.append(",\n");
            }
            Cte cte = ctes.get(i);
            sql.append(cte.name).append(" AS (\n").append(cte.sql).append("\n)");
        }
        sql.append("\n");

        List<String> checks = new ArrayList<String>();
        checks.add(rowCountCheck(
            "ROW_COUNT_CHECK",
            "final_result",
            "original_result",
            "rewrite_result",
            "original_result row count compared with rewrite_result row count"
        ));
        if (L2GrainMeasureDeriver.MV_TYPE_PREJOIN.equals(input.mvType)
            || L2GrainMeasureDeriver.MV_TYPE_STAR_AGG.equals(input.mvType)) {
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
        for (ValidationMeasure measure : measures) {
            checks.add(measureDiffCheck(measure));
        }
        if (!groupKeys.isEmpty()) {
            checks.add(groupKeyDiffCheck(groupKeys));
            for (ValidationMeasure measure : measures) {
                checks.add(groupMeasureDiffCheck(measure, groupKeys));
            }
        }

        for (int i = 0; i < checks.size(); i++) {
            if (i > 0) {
                sql.append("\nUNION ALL\n");
            }
            sql.append(checks.get(i));
        }
        sql.append(";");
        return ValidationSqlResult.generated(sql.toString());
    }

    private static List<String> commonSubgraphOutputColumns(ValidationInput input,
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
            blockingReasons.add(reason(
                "VALIDATION_MV_NAME_REQUIRED",
                "缺少可安全引用的 MV 名称，不能生成公共子图输出对比。"
            ));
        }
        List<String> columns = new ArrayList<String>();
        Set<String> seen = new LinkedHashSet<String>();
        for (String outputColumn : input.commonSubgraphOutputColumns) {
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
            blockingReasons.add(reason(
                "VALIDATION_COMMON_SUBGRAPH_OUTPUT_REQUIRED",
                "缺少公共子图输出字段，不能生成公共子图输出差异检查。"
            ));
        }
        return columns;
    }

    private static List<ValidationColumn> groupKeyColumns(Map<String, Object> advancedStructureProfile,
                                                          List<Map<String, Object>> blockingReasons) {
        List<ValidationColumn> columns = new ArrayList<ValidationColumn>();
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
                    "GROUP BY 字段无法映射为 original_result/rewrite_result 的输出列，"
                        + "不能生成分组差异检查。"
                );
                reason.put("expression", expression);
                blockingReasons.add(reason);
                continue;
            }
            if (seen.add(outputName.toUpperCase(Locale.ROOT))) {
                columns.add(new ValidationColumn(outputName));
            }
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

    private static List<ValidationMeasure> measureColumns(List<Map<String, Object>> sourceMeasures,
                                                          List<Map<String, Object>> blockingReasons) {
        List<ValidationMeasure> measures = new ArrayList<ValidationMeasure>();
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
                measures.add(new ValidationMeasure(name, text(measure.get("measureType"))));
            }
        }
        return measures;
    }

    private static String groupedResultSql(String sourceRelation,
                                           List<ValidationColumn> groupKeys,
                                           List<ValidationMeasure> measures,
                                           String markerName) {
        List<String> selectItems = new ArrayList<String>();
        List<String> groupItems = new ArrayList<String>();
        for (ValidationColumn groupKey : groupKeys) {
            selectItems.add(groupKey.name);
            groupItems.add(groupKey.name);
        }
        for (ValidationMeasure measure : measures) {
            selectItems.add(groupAggregateExpression(measure) + " AS " + measure.name);
        }
        selectItems.add("1 AS " + markerName);
        return "SELECT\n       " + join(selectItems, ",\n       ")
            + "\nFROM " + sourceRelation
            + "\nGROUP BY " + join(groupItems, ", ");
    }

    private static String mvSubgraphResultSql(String mvName, List<String> outputColumns) {
        return "SELECT " + join(outputColumns, ", ") + "\nFROM " + mvName;
    }

    private static String sqlIdentifier(String value) {
        String cleaned = cleanOutputIdentifier(value);
        if (isSafeIdentifier(cleaned)) {
            return cleaned;
        }
        return "\"" + cleaned.replace("\"", "\"\"") + "\"";
    }

    private static String rowCountCheck(String checkName,
                                        String checkTarget,
                                        String originalRelation,
                                        String rewriteRelation,
                                        String detail) {
        String originalCount = "(SELECT COUNT(*) FROM " + originalRelation + ")";
        String rewriteCount = "(SELECT COUNT(*) FROM " + rewriteRelation + ")";
        return "SELECT " + sqlLiteral(checkName) + " AS check_name,\n"
            + "       " + sqlLiteral(checkTarget) + " AS check_target,\n"
            + "       CAST(" + originalCount + " AS VARCHAR) AS original_value,\n"
            + "       CAST(" + rewriteCount + " AS VARCHAR) AS rewrite_value,\n"
            + "       CAST((" + originalCount + " - " + rewriteCount + ") AS VARCHAR) AS diff_value,\n"
            + "       " + sqlLiteral(detail) + " AS detail";
    }

    private static String measureDiffCheck(ValidationMeasure measure) {
        String originalAggregate = "(SELECT " + measureAggregateExpression(measure) + " FROM original_result)";
        String rewriteAggregate = "(SELECT " + measureAggregateExpression(measure) + " FROM rewrite_result)";
        String diffExpression = numericMeasure(measure)
            ? "CAST((COALESCE(" + originalAggregate + ", 0) - COALESCE(" + rewriteAggregate
                + ", 0)) AS VARCHAR)"
            : equalityDiffExpression(originalAggregate, rewriteAggregate);
        return "SELECT 'MEASURE_DIFF' AS check_name,\n"
            + "       " + sqlLiteral(measure.name) + " AS check_target,\n"
            + "       CAST(" + originalAggregate + " AS VARCHAR) AS original_value,\n"
            + "       CAST(" + rewriteAggregate + " AS VARCHAR) AS rewrite_value,\n"
            + "       " + diffExpression + " AS diff_value,\n"
            + "       'global measure aggregate difference' AS detail";
    }

    private static String groupKeyDiffCheck(List<ValidationColumn> groupKeys) {
        String joinCondition = groupJoinCondition(groupKeys);
        return "SELECT 'GROUP_KEY_DIFF' AS check_name,\n"
            + "       " + sqlLiteral(columnNames(groupKeys)) + " AS check_target,\n"
            + "       CAST(COALESCE(SUM(CASE WHEN r._validation_rewrite_marker IS NULL THEN 1 ELSE 0 END), 0) "
            + "AS VARCHAR) AS original_value,\n"
            + "       CAST(COALESCE(SUM(CASE WHEN o._validation_original_marker IS NULL THEN 1 ELSE 0 END), 0) "
            + "AS VARCHAR) AS rewrite_value,\n"
            + "       CAST(COUNT(*) AS VARCHAR) AS diff_value,\n"
            + "       'original-only and rewrite-only group counts' AS detail\n"
            + "FROM original_group o\n"
            + "FULL OUTER JOIN rewrite_group r ON " + joinCondition + "\n"
            + "WHERE o._validation_original_marker IS NULL OR r._validation_rewrite_marker IS NULL";
    }

    private static String groupMeasureDiffCheck(ValidationMeasure measure, List<ValidationColumn> groupKeys) {
        String joinCondition = groupJoinCondition(groupKeys);
        String equality = nullSafeEquality("o." + measure.name, "r." + measure.name);
        return "SELECT 'GROUP_MEASURE_DIFF' AS check_name,\n"
            + "       " + sqlLiteral(measure.name) + " AS check_target,\n"
            + "       CAST(COUNT(*) AS VARCHAR) AS original_value,\n"
            + "       CAST(0 AS VARCHAR) AS rewrite_value,\n"
            + "       CAST(COUNT(*) AS VARCHAR) AS diff_value,\n"
            + "       " + sqlLiteral("mismatched grouped measure rows on " + columnNames(groupKeys))
            + " AS detail\n"
            + "FROM original_group o\n"
            + "FULL OUTER JOIN rewrite_group r ON " + joinCondition + "\n"
            + "WHERE o._validation_original_marker IS NULL\n"
            + "   OR r._validation_rewrite_marker IS NULL\n"
            + "   OR NOT (" + equality + ")";
    }

    private static String groupJoinCondition(List<ValidationColumn> groupKeys) {
        List<String> conditions = new ArrayList<String>();
        for (ValidationColumn groupKey : groupKeys) {
            conditions.add(nullSafeEquality("o." + groupKey.name, "r." + groupKey.name));
        }
        return join(conditions, " AND ");
    }

    private static String measureAggregateExpression(ValidationMeasure measure) {
        return aggregateFunction(measure) + "(" + measure.name + ")";
    }

    private static String groupAggregateExpression(ValidationMeasure measure) {
        return aggregateFunction(measure) + "(" + measure.name + ")";
    }

    private static String aggregateFunction(ValidationMeasure measure) {
        String measureType = measure.measureType.toUpperCase(Locale.ROOT);
        if ("MIN".equals(measureType)) {
            return "MIN";
        }
        if ("MAX".equals(measureType)) {
            return "MAX";
        }
        if ("AVG".equals(measureType) || "RATIO".equals(measureType)) {
            return "MAX";
        }
        return "SUM";
    }

    private static boolean numericMeasure(ValidationMeasure measure) {
        String measureType = measure.measureType.toUpperCase(Locale.ROOT);
        return "SUM".equals(measureType)
            || "COUNT".equals(measureType)
            || "AVG".equals(measureType)
            || "RATIO".equals(measureType)
            || "NUMERATOR".equals(measureType)
            || "DENOMINATOR".equals(measureType)
            || measureType.contains("COMPONENT");
    }

    private static String equalityDiffExpression(String originalExpression, String rewriteExpression) {
        return "CASE WHEN " + nullSafeEquality(originalExpression, rewriteExpression)
            + " THEN '0' ELSE 'VALUE_MISMATCH' END";
    }

    private static String nullSafeEquality(String leftExpression, String rightExpression) {
        return "((" + leftExpression + " = " + rightExpression + ") OR ("
            + leftExpression + " IS NULL AND " + rightExpression + " IS NULL))";
    }

    private static String columnNames(List<ValidationColumn> columns) {
        List<String> names = new ArrayList<String>();
        for (ValidationColumn column : columns) {
            names.add(column.name);
        }
        return join(names, ",");
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

    private static String stripAlias(String expression, String alias) {
        if (!StringUtils.hasText(expression) || !StringUtils.hasText(alias)) {
            return expression;
        }
        String suffix = " AS " + alias;
        if (expression.toUpperCase(Locale.ROOT).endsWith(suffix.toUpperCase(Locale.ROOT))) {
            return expression.substring(0, expression.length() - suffix.length()).trim();
        }
        return expression;
    }

    private static boolean isSimpleColumnExpression(String expression) {
        if (!StringUtils.hasText(expression)) {
            return false;
        }
        return expression.trim().matches("(?i)[A-Z_][A-Z0-9_$]*(\\.[A-Z_][A-Z0-9_$]*)?");
    }

    private static String unqualifiedName(String expression) {
        if (!StringUtils.hasText(expression)) {
            return "";
        }
        String trimmed = expression.trim();
        int dotIndex = trimmed.lastIndexOf('.');
        return dotIndex >= 0 ? trimmed.substring(dotIndex + 1) : trimmed;
    }

    private static String normalizeExpression(String value) {
        return text(value).replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private static String cleanOutputIdentifier(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String result = value.trim();
        while (result.startsWith("\"") && result.endsWith("\"") && result.length() > 1) {
            result = result.substring(1, result.length() - 1);
        }
        while (result.startsWith("`") && result.endsWith("`") && result.length() > 1) {
            result = result.substring(1, result.length() - 1);
        }
        return result;
    }

    private static boolean isSafeIdentifier(String value) {
        return StringUtils.hasText(value) && SAFE_IDENTIFIER_PATTERN.matcher(value).matches();
    }

    private static String sqlLiteral(String value) {
        return "'" + text(value).replace("'", "''") + "'";
    }

    private static String join(List<String> values, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?>)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (Object item : (Iterable<?>) value) {
            if (item != null && StringUtils.hasText(String.valueOf(item))) {
                result.add(String.valueOf(item).trim());
            }
        }
        return result;
    }

    private static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof Iterable<?>)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : (Iterable<?>) value) {
            if (item instanceof Map<?, ?>) {
                result.add(copyMap((Map<?, ?>) item));
            }
        }
        return result;
    }

    private static Map<String, Object> copyMap(Map<?, ?> source) {
        LinkedHashMap<String, Object> target = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                target.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return target;
    }

    private static List<Map<String, Object>> immutableMapList(List<Map<String, Object>> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : source) {
            result.add(new LinkedHashMap<String, Object>(item));
        }
        return Collections.unmodifiableList(result);
    }

    private static String trimTrailingSemicolon(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "";
        }
        String trimmed = sql.trim();
        while (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static Map<String, Object> reason(String code, String description) {
        LinkedHashMap<String, Object> reason = new LinkedHashMap<String, Object>();
        reason.put("code", code);
        reason.put("description", description);
        return reason;
    }

    static final class ValidationInput {

        private final String mvType;
        private final String sourceSql;
        private final String rewriteSql;
        private final String mvName;
        private final Map<String, Object> advancedStructureProfile;
        private final List<Map<String, Object>> measures;
        private final String commonSubgraphSql;
        private final List<String> commonSubgraphOutputColumns;

        ValidationInput(String mvType,
                        String sourceSql,
                        String rewriteSql,
                        String mvName,
                        Map<String, Object> advancedStructureProfile,
                        List<Map<String, Object>> measures,
                        String commonSubgraphSql,
                        List<String> commonSubgraphOutputColumns) {
            this.mvType = mvType;
            this.sourceSql = sourceSql;
            this.rewriteSql = rewriteSql;
            this.mvName = mvName;
            this.advancedStructureProfile = advancedStructureProfile == null
                ? Collections.<String, Object>emptyMap()
                : new LinkedHashMap<String, Object>(advancedStructureProfile);
            this.measures = immutableMapList(measures);
            this.commonSubgraphSql = commonSubgraphSql;
            this.commonSubgraphOutputColumns = commonSubgraphOutputColumns == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(commonSubgraphOutputColumns));
        }
    }

    static final class ValidationSqlResult {

        private final String validationSql;
        private final List<Map<String, Object>> blockingReasons;

        private ValidationSqlResult(String validationSql, List<Map<String, Object>> blockingReasons) {
            this.validationSql = validationSql;
            this.blockingReasons = immutableMapList(blockingReasons);
        }

        private static ValidationSqlResult generated(String validationSql) {
            return new ValidationSqlResult(validationSql, Collections.<Map<String, Object>>emptyList());
        }

        private static ValidationSqlResult blocked(List<Map<String, Object>> blockingReasons) {
            return new ValidationSqlResult(null, blockingReasons);
        }

        boolean isGenerated() {
            return StringUtils.hasText(validationSql) && blockingReasons.isEmpty();
        }

        String getValidationSql() {
            return validationSql;
        }

        List<Map<String, Object>> getBlockingReasons() {
            return blockingReasons;
        }
    }

    private static final class Cte {

        private final String name;
        private final String sql;

        private Cte(String name, String sql) {
            this.name = name;
            this.sql = sql;
        }
    }

    private static final class ValidationColumn {

        private final String name;

        private ValidationColumn(String name) {
            this.name = name;
        }
    }

    private static final class ValidationMeasure {

        private final String name;
        private final String measureType;

        private ValidationMeasure(String name, String measureType) {
            this.name = name;
            this.measureType = StringUtils.hasText(measureType) ? measureType : "SUM";
        }
    }
}
