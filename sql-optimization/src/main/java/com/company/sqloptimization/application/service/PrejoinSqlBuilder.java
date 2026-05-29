package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.PrejoinProfileValues.text;
import static com.company.sqloptimization.application.service.PrejoinSqlTextSupport.trimTrailingSemicolon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

final class PrejoinSqlBuilder {

    private PrejoinSqlBuilder() {
    }

    static String selectSql(List<String> selectItems, String fromClause, List<String> retainedWherePredicates) {
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
        builder.append(';');
        return builder.toString();
    }

    static String fromClause(String sourceSql, List<Map<String, Object>> baseTables, PrejoinJoinPlan joinPlan) {
        String extractedFromClause = extractFromClause(sourceSql);
        if (StringUtils.hasText(extractedFromClause)) {
            return extractedFromClause;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(tableWithAlias(baseTables.get(0)));
        for (PrejoinJoinClause joinClause : joinPlan.joinClauses) {
            builder.append('\n');
            builder.append("JOIN ");
            builder.append(tableWithAlias(joinClause));
            builder.append(" ON ");
            builder.append(joinClause.condition);
        }
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

    private static String tableWithAlias(PrejoinJoinClause joinClause) {
        if (!joinClause.rightTable.isEmpty()) {
            return tableWithAlias(joinClause.rightTable);
        }
        String right = text(joinClause.join.get("right"));
        String alias = text(joinClause.join.get("rightAlias"));
        if (StringUtils.hasText(alias) && !alias.equalsIgnoreCase(right)) {
            return right + " " + alias;
        }
        return right;
    }

    private static String tableWithAlias(Map<String, Object> table) {
        String tableName = text(table.get("tableName"));
        String alias = text(table.get("alias"));
        if (StringUtils.hasText(alias) && !alias.equalsIgnoreCase(tableName)) {
            return tableName + " " + alias;
        }
        return tableName;
    }
}
