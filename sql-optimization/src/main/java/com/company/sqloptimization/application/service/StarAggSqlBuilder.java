package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggProfileValues.text;
import static com.company.sqloptimization.application.service.StarAggSqlTextSupport.tableWithAlias;
import static com.company.sqloptimization.application.service.StarAggSqlTextSupport.trimTrailingSemicolon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

final class StarAggSqlBuilder {

    private StarAggSqlBuilder() {
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

    static String selectSql(List<String> dimensionSelectItems,
                            List<String> measureSelectItems,
                            String fromClause,
                            List<String> retainedWherePredicates,
                            StarAggDimensionPlan dimensionPlan) {
        List<String> selectItems = new ArrayList<String>();
        selectItems.addAll(dimensionSelectItems);
        selectItems.addAll(measureSelectItems);
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
        builder.append("GROUP BY ");
        builder.append(String.join(", ", dimensionPlan.ddlGroupByItems()));
        builder.append('\n');
        builder.append(';');
        return builder.toString();
    }

    static String fromClause(String sourceSql, List<Map<String, Object>> baseTables) {
        String extracted = extractFromClause(sourceSql);
        if (StringUtils.hasText(extracted)) {
            return extracted;
        }
        if (baseTables.isEmpty()) {
            return "";
        }
        return tableWithAlias(baseTables.get(0));
    }

    private static String extractFromClause(String sourceSql) {
        if (!StringUtils.hasText(sourceSql)) {
            return "";
        }
        String normalized = trimTrailingSemicolon(sourceSql).replaceAll("\\s+", " ").trim();
        String upper = normalized.toUpperCase(Locale.ROOT);
        int fromIndex = upper.indexOf(" FROM ");
        if (fromIndex < 0) {
            return "";
        }
        int start = fromIndex + " FROM ".length();
        int end = normalized.length();
        for (String marker : Arrays.asList(" WHERE ", " GROUP BY ", " HAVING ", " ORDER BY ", " LIMIT ")) {
            int markerIndex = upper.indexOf(marker, start);
            if (markerIndex >= 0 && markerIndex < end) {
                end = markerIndex;
            }
        }
        return normalized.substring(start, end).trim();
    }
}
