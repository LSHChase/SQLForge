package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.ParameterizedAggDimensionPlanner.dimensionExpressions;
import static com.company.sqloptimization.application.service.ParameterizedAggProfileValues.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class ParameterizedAggSqlBuilder {

    private ParameterizedAggSqlBuilder() {
    }

    static String selectSql(List<String> selectItems,
                            String fromClause,
                            List<String> retainedWherePredicates,
                            List<ParameterizedAggDimensionSpec> dimensions) {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT\n");
        for (int i = 0; i < selectItems.size(); i++) {
            builder.append("  ");
            builder.append(selectItems.get(i));
            builder.append(i == selectItems.size() - 1 ? "\n" : ",\n");
        }
        builder.append("FROM ").append(fromClause).append('\n');
        if (!retainedWherePredicates.isEmpty()) {
            builder.append("WHERE ");
            builder.append(String.join("\n  AND ", retainedWherePredicates));
            builder.append('\n');
        }
        if (!dimensions.isEmpty()) {
            builder.append("GROUP BY ");
            builder.append(String.join(", ", dimensionExpressions(dimensions)));
            builder.append('\n');
        }
        builder.append(';');
        return builder.toString();
    }

    static List<String> retainedWherePredicates(
        L2PredicateClassifier.PredicateClassificationResult predicateClassification) {
        List<String> predicates = new ArrayList<String>();
        if (predicateClassification == null) {
            return predicates;
        }
        for (Map<String, Object> predicate : predicateClassification.getRetainedPredicates()) {
            if ("WHERE".equalsIgnoreCase(text(predicate.get("clause")))) {
                predicates.add(text(predicate.get("expression")));
            }
        }
        return predicates;
    }

    static String baseFromClause(List<Map<String, Object>> tables) {
        List<Map<String, Object>> baseTables = baseTables(tables);
        if (baseTables.isEmpty()) {
            return "";
        }
        List<String> relations = new ArrayList<String>();
        for (Map<String, Object> table : baseTables) {
            String tableName = text(table.get("tableName"));
            String alias = text(table.get("alias"));
            if (!StringUtils.hasText(tableName)) {
                continue;
            }
            if (StringUtils.hasText(alias) && !alias.equalsIgnoreCase(tableName)) {
                relations.add(tableName + " " + alias);
            } else {
                relations.add(tableName);
            }
        }
        return String.join(", ", relations);
    }

    static List<Map<String, Object>> baseTables(List<Map<String, Object>> tables) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> table : tables) {
            String sourceType = text(table.get("sourceType"));
            if (!StringUtils.hasText(sourceType) || "BASE_TABLE".equals(sourceType)) {
                result.add(table);
            }
        }
        return result;
    }
}
