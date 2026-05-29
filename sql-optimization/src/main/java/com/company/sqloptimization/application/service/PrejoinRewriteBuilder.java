package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.PrejoinColumnRewriteSupport.rewriteColumns;
import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.normalizeName;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.mapList;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.text;
import static com.company.sqloptimization.application.service.PrejoinSqlTextSupport.stripAlias;
import static com.company.sqloptimization.application.service.PrejoinSqlTextSupport.withAlias;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class PrejoinRewriteBuilder {

    private PrejoinRewriteBuilder() {
    }

    static String rewriteSql(String mvName,
                             Map<String, Object> advancedStructureProfile,
                             L2PredicateClassifier.PredicateClassificationResult predicateClassification,
                             PrejoinColumnPlan columnPlan) {
        List<String> selectItems = rewriteSelectItems(advancedStructureProfile, columnPlan);
        List<String> wherePredicates = rewriteWherePredicates(predicateClassification, columnPlan);
        List<String> groupByItems = rewriteGroupByItems(advancedStructureProfile, columnPlan);
        List<String> havingPredicates = rewriteHavingPredicates(predicateClassification, columnPlan);

        StringBuilder builder = new StringBuilder();
        builder.append("SELECT\n");
        for (int i = 0; i < selectItems.size(); i++) {
            builder.append("  ");
            builder.append(selectItems.get(i));
            builder.append(i == selectItems.size() - 1 ? "\n" : ",\n");
        }
        builder.append("FROM ").append(mvName).append('\n');
        if (!wherePredicates.isEmpty()) {
            builder.append("WHERE ");
            builder.append(String.join("\n  AND ", wherePredicates));
            builder.append('\n');
        }
        if (!groupByItems.isEmpty()) {
            builder.append("GROUP BY ");
            builder.append(String.join(", ", groupByItems));
            builder.append('\n');
        }
        if (!havingPredicates.isEmpty()) {
            builder.append("HAVING ");
            builder.append(String.join("\n  AND ", havingPredicates));
            builder.append('\n');
        }
        builder.append(';');
        return builder.toString();
    }

    private static List<String> rewriteSelectItems(Map<String, Object> advancedStructureProfile,
                                                   PrejoinColumnPlan columnPlan) {
        List<String> selectItems = new ArrayList<String>();
        for (Map<String, Object> projection : mapList(advancedStructureProfile.get("projections"))) {
            String alias = text(projection.get("alias"));
            String expression = stripAlias(text(projection.get("expression")), alias);
            if (!StringUtils.hasText(expression)) {
                continue;
            }
            selectItems.add(withAlias(rewriteColumns(expression, columnPlan), alias));
        }
        if (selectItems.isEmpty()) {
            selectItems.add("COUNT(*) AS row_count");
        }
        return selectItems;
    }

    private static List<String> rewriteWherePredicates(
        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
        PrejoinColumnPlan columnPlan) {
        List<String> predicates = new ArrayList<String>();
        if (predicateClassification == null) {
            return predicates;
        }
        addRewritePredicates(predicates, predicateClassification.getExternalizedPredicates(), "WHERE", columnPlan);
        addRewritePredicates(predicates, predicateClassification.getSecurityPredicates(), "WHERE", columnPlan);
        return predicates;
    }

    private static List<String> rewriteGroupByItems(Map<String, Object> advancedStructureProfile,
                                                    PrejoinColumnPlan columnPlan) {
        List<String> items = new ArrayList<String>();
        Set<String> seen = new LinkedHashSet<String>();
        for (Map<String, Object> groupBy : mapList(advancedStructureProfile.get("groupBy"))) {
            String rewritten = rewriteColumns(text(groupBy.get("expression")), columnPlan);
            if (StringUtils.hasText(rewritten) && !seen.contains(normalizeName(rewritten))) {
                items.add(rewritten);
                seen.add(normalizeName(rewritten));
            }
        }
        return items;
    }

    private static List<String> rewriteHavingPredicates(
        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
        PrejoinColumnPlan columnPlan) {
        List<String> predicates = new ArrayList<String>();
        if (predicateClassification == null) {
            return predicates;
        }
        addRewritePredicates(predicates, predicateClassification.getExternalizedPredicates(), "HAVING", columnPlan);
        addRewritePredicates(predicates, predicateClassification.getSecurityPredicates(), "HAVING", columnPlan);
        addRewritePredicates(predicates, predicateClassification.getRetainedPredicates(), "HAVING", columnPlan);
        return predicates;
    }

    private static void addRewritePredicates(List<String> target,
                                             List<Map<String, Object>> predicates,
                                             String clause,
                                             PrejoinColumnPlan columnPlan) {
        for (Map<String, Object> predicate : predicates) {
            if (clause.equalsIgnoreCase(text(predicate.get("clause")))) {
                target.add(rewriteColumns(text(predicate.get("expression")), columnPlan));
            }
        }
    }
}
