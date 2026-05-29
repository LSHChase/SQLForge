package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RollupDimensionPlanner.dimensionExpressions;
import static com.company.sqloptimization.application.service.RollupProfileValues.baseTables;
import static com.company.sqloptimization.application.service.RollupProfileValues.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class RollupSqlBuilder {

    private RollupSqlBuilder() {
    }

    static String selectSql(List<String> selectItems,
                            String fromClause,
                            List<String> retainedWherePredicates,
                            RollupPlan rollupPlan,
                            List<RollupDimensionSpec> dimensions) {
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
        List<String> groupByItems = new ArrayList<String>();
        groupByItems.add(rollupPlan.mvTimeExpression);
        groupByItems.addAll(dimensionExpressions(dimensions));
        builder.append("GROUP BY ");
        builder.append(String.join(", ", groupByItems));
        builder.append('\n');
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
        Map<String, Object> table = baseTables.get(0);
        String tableName = text(table.get("tableName"));
        String alias = text(table.get("alias"));
        if (StringUtils.hasText(alias) && !alias.equalsIgnoreCase(tableName)) {
            return tableName + " " + alias;
        }
        return tableName;
    }
}
