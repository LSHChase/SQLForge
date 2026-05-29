package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RollupColumnRewriteSupport.findDimension;
import static com.company.sqloptimization.application.service.RollupColumnRewriteSupport.findDimensionBySources;
import static com.company.sqloptimization.application.service.RollupColumnRewriteSupport.rewriteColumns;
import static com.company.sqloptimization.application.service.RollupIdentifierSupport.normalizeExpression;
import static com.company.sqloptimization.application.service.RollupIdentifierSupport.withAlias;
import static com.company.sqloptimization.application.service.RollupMeasureRewriteSupport.rewriteMeasureExpressions;
import static com.company.sqloptimization.application.service.RollupProfileValues.mapList;
import static com.company.sqloptimization.application.service.RollupProfileValues.stringList;
import static com.company.sqloptimization.application.service.RollupProfileValues.text;
import static com.company.sqloptimization.application.service.RollupSqlTextSupport.containsAggregateToken;
import static com.company.sqloptimization.application.service.RollupSqlTextSupport.stripAlias;
import static com.company.sqloptimization.application.service.RollupTimeReferenceRewriter.rewriteTimeReferences;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class RollupRewriteBuilder {

    private RollupRewriteBuilder() {
    }

    static String rewriteSql(String mvName,
                             Map<String, Object> advancedStructureProfile,
                             L2PredicateClassifier.PredicateClassificationResult predicateClassification,
                             List<Map<String, Object>> measures,
                             RollupPlan rollupPlan,
                             List<RollupDimensionSpec> dimensions) {
        List<String> selectItems = rewriteSelectItems(advancedStructureProfile, measures, rollupPlan, dimensions);
        List<String> wherePredicates = rewriteFilterPredicates(predicateClassification, rollupPlan, dimensions);
        List<String> groupByItems = rewriteGroupByItems(
            mapList(advancedStructureProfile.get("groupBy")),
            rollupPlan,
            dimensions
        );
        List<String> havingPredicates = rewriteHavingPredicates(predicateClassification, measures, rollupPlan, dimensions);

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
                                                   List<Map<String, Object>> measures,
                                                   RollupPlan rollupPlan,
                                                   List<RollupDimensionSpec> dimensions) {
        List<String> selectItems = new ArrayList<String>();
        Set<String> selectedDimensions = new LinkedHashSet<String>();
        boolean selectedTime = false;
        for (Map<String, Object> projection : mapList(advancedStructureProfile.get("projections"))) {
            if (isMeasureProjection(projection, measures)) {
                continue;
            }
            String alias = text(projection.get("alias"));
            String expression = stripAlias(text(projection.get("expression")), alias);
            if (rollupPlan.matchesTimeReference(expression)) {
                if (!selectedTime) {
                    selectItems.add(withAlias(rollupPlan.rewriteRollupExpression, alias));
                    selectedTime = true;
                }
                continue;
            }
            RollupDimensionSpec dimension = findDimension(expression, dimensions);
            if (dimension == null) {
                dimension = findDimensionBySources(stringList(projection.get("sourceColumns")), dimensions);
            }
            if (dimension == null || selectedDimensions.contains(dimension.outputName)) {
                continue;
            }
            selectItems.add(withAlias(dimension.outputName, alias));
            selectedDimensions.add(dimension.outputName);
        }
        if (!selectedTime) {
            selectItems.add(rollupPlan.rewriteRollupExpression);
        }
        addMeasureSelectItems(selectItems, measures);
        return selectItems;
    }

    private static void addMeasureSelectItems(List<String> selectItems, List<Map<String, Object>> measures) {
        for (Map<String, Object> measure : measures) {
            String rewriteExpression = text(measure.get("rewriteExpression"));
            String name = text(measure.get("name"));
            if (StringUtils.hasText(rewriteExpression) && StringUtils.hasText(name)) {
                selectItems.add(rewriteExpression + " AS " + name);
            }
        }
    }

    private static boolean isMeasureProjection(Map<String, Object> projection, List<Map<String, Object>> measures) {
        String expression = normalizeExpression(stripAlias(text(projection.get("expression")), text(projection.get("alias"))));
        String expressionType = text(projection.get("expressionType"));
        if ("AGGREGATION".equals(expressionType)) {
            return true;
        }
        for (Map<String, Object> measure : measures) {
            if (expression.equals(normalizeExpression(text(measure.get("sourceExpression"))))) {
                return true;
            }
        }
        return containsAggregateToken(expression);
    }

    private static List<String> rewriteFilterPredicates(
        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
        RollupPlan rollupPlan,
        List<RollupDimensionSpec> dimensions) {
        List<String> predicates = new ArrayList<String>();
        if (predicateClassification == null) {
            return predicates;
        }
        addRewriteFilterPredicates(predicates, predicateClassification.getExternalizedPredicates(), rollupPlan, dimensions);
        addRewriteFilterPredicates(predicates, predicateClassification.getSecurityPredicates(), rollupPlan, dimensions);
        return predicates;
    }

    private static void addRewriteFilterPredicates(List<String> target,
                                                   List<Map<String, Object>> predicates,
                                                   RollupPlan rollupPlan,
                                                   List<RollupDimensionSpec> dimensions) {
        for (Map<String, Object> predicate : predicates) {
            if ("WHERE".equalsIgnoreCase(text(predicate.get("clause")))) {
                String expression = rewriteTimeReferences(text(predicate.get("expression")), rollupPlan);
                target.add(rewriteColumns(expression, dimensions));
            }
        }
    }

    private static List<String> rewriteGroupByItems(List<Map<String, Object>> groupBy,
                                                    RollupPlan rollupPlan,
                                                    List<RollupDimensionSpec> dimensions) {
        List<String> items = new ArrayList<String>();
        Set<String> seen = new LinkedHashSet<String>();
        for (Map<String, Object> groupByItem : groupBy) {
            String expression = text(groupByItem.get("expression"));
            if (rollupPlan.matchesTimeReference(expression)) {
                if (!seen.contains(rollupPlan.rewriteRollupExpression)) {
                    items.add(rollupPlan.rewriteRollupExpression);
                    seen.add(rollupPlan.rewriteRollupExpression);
                }
                continue;
            }
            RollupDimensionSpec dimension = findDimension(expression, dimensions);
            if (dimension == null) {
                dimension = findDimensionBySources(stringList(groupByItem.get("sourceColumns")), dimensions);
            }
            if (dimension != null && !seen.contains(dimension.outputName)) {
                items.add(dimension.outputName);
                seen.add(dimension.outputName);
            }
        }
        return items;
    }

    private static List<String> rewriteHavingPredicates(
        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
        List<Map<String, Object>> measures,
        RollupPlan rollupPlan,
        List<RollupDimensionSpec> dimensions) {
        List<String> predicates = new ArrayList<String>();
        if (predicateClassification == null) {
            return predicates;
        }
        for (Map<String, Object> predicate : predicateClassification.getRetainedPredicates()) {
            if ("HAVING".equalsIgnoreCase(text(predicate.get("clause")))) {
                String expression = rewriteMeasureExpressions(text(predicate.get("expression")), measures);
                expression = rewriteTimeReferences(expression, rollupPlan);
                predicates.add(rewriteColumns(expression, dimensions));
            }
        }
        return predicates;
    }
}
