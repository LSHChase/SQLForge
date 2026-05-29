package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.ValidationSqlText.join;
import static com.company.sqloptimization.application.service.ValidationSqlText.sqlLiteral;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ValidationSqlChecks {

    private ValidationSqlChecks() {
    }

    static String rowCountCheck(String checkName,
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

    static String measureDiffCheck(ValidationSqlMeasure measure) {
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

    static String groupKeyDiffCheck(List<ValidationSqlColumn> groupKeys) {
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

    static String groupMeasureDiffCheck(ValidationSqlMeasure measure, List<ValidationSqlColumn> groupKeys) {
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

    static String groupAggregateExpression(ValidationSqlMeasure measure) {
        return aggregateFunction(measure) + "(" + measure.name + ")";
    }

    private static String measureAggregateExpression(ValidationSqlMeasure measure) {
        return aggregateFunction(measure) + "(" + measure.name + ")";
    }

    private static String aggregateFunction(ValidationSqlMeasure measure) {
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

    private static boolean numericMeasure(ValidationSqlMeasure measure) {
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

    private static String groupJoinCondition(List<ValidationSqlColumn> groupKeys) {
        List<String> conditions = new ArrayList<String>();
        for (ValidationSqlColumn groupKey : groupKeys) {
            conditions.add(nullSafeEquality("o." + groupKey.name, "r." + groupKey.name));
        }
        return join(conditions, " AND ");
    }

    private static String columnNames(List<ValidationSqlColumn> columns) {
        List<String> names = new ArrayList<String>();
        for (ValidationSqlColumn column : columns) {
            names.add(column.name);
        }
        return join(names, ",");
    }
}
