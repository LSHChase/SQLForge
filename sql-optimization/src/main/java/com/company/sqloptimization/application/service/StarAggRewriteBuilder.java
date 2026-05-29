package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggProfileValues.mapList;
import static com.company.sqloptimization.application.service.StarAggProfileValues.stringList;
import static com.company.sqloptimization.application.service.StarAggProfileValues.text;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.normalizeExpression;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.unqualifiedName;
import static com.company.sqloptimization.application.service.StarAggSqlTextSupport.containsAggregateToken;
import static com.company.sqloptimization.application.service.StarAggSqlTextSupport.leftPredicateField;
import static com.company.sqloptimization.application.service.StarAggSqlTextSupport.replaceIdentifier;
import static com.company.sqloptimization.application.service.StarAggSqlTextSupport.stripAlias;
import static com.company.sqloptimization.application.service.StarAggSqlTextSupport.withAlias;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class StarAggRewriteBuilder {

    private StarAggRewriteBuilder() {
    }

    static String rewriteSql(String mvName,
                             Map<String, Object> advancedStructureProfile,
                             L2PredicateClassifier.PredicateClassificationResult classification,
                             List<Map<String, Object>> measures,
                             StarAggDimensionPlan dimensionPlan) {
        List<String> selectItems = rewriteSelectItems(advancedStructureProfile, measures, dimensionPlan);
        List<String> wherePredicates = rewriteFilterPredicates(classification, dimensionPlan);
        List<String> groupByItems = rewriteGroupByItems(mapList(advancedStructureProfile.get("groupBy")), dimensionPlan);
        List<String> havingPredicates = rewriteHavingPredicates(classification, measures, dimensionPlan);
        return renderRewriteSql(mvName, selectItems, wherePredicates, groupByItems, havingPredicates);
    }

    private static List<String> rewriteSelectItems(Map<String, Object> advancedStructureProfile,
                                                   List<Map<String, Object>> measures,
                                                   StarAggDimensionPlan dimensionPlan) {
        List<String> selectItems = new ArrayList<String>();
        Set<String> selectedDimensions = new LinkedHashSet<String>();
        for (Map<String, Object> projection : mapList(advancedStructureProfile.get("projections"))) {
            if (isMeasureProjection(projection, measures)) {
                continue;
            }
            StarAggDimensionSpec dimension = dimensionPlan.find(text(projection.get("expression")));
            if (dimension == null) {
                dimension = dimensionPlan.findBySources(stringList(projection.get("sourceColumns")));
            }
            if (dimension != null && !selectedDimensions.contains(dimension.outputName)) {
                selectItems.add(withAlias(dimension.outputName, text(projection.get("alias"))));
                selectedDimensions.add(dimension.outputName);
            }
        }
        for (Map<String, Object> measure : measures) {
            String rewriteExpression = text(measure.get("rewriteExpression"));
            String name = text(measure.get("name"));
            if (StringUtils.hasText(rewriteExpression) && StringUtils.hasText(name)) {
                selectItems.add(rewriteExpression + " AS " + name);
            }
        }
        return selectItems;
    }

    private static boolean isMeasureProjection(Map<String, Object> projection, List<Map<String, Object>> measures) {
        String expression = normalizeExpression(stripAlias(text(projection.get("expression")), text(projection.get("alias"))));
        if ("AGGREGATION".equals(text(projection.get("expressionType")))) {
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
        L2PredicateClassifier.PredicateClassificationResult classification,
        StarAggDimensionPlan dimensionPlan) {
        List<String> predicates = new ArrayList<String>();
        if (classification != null) {
            addRewriteFilterPredicates(predicates, classification.getExternalizedPredicates(), dimensionPlan);
            addRewriteFilterPredicates(predicates, classification.getSecurityPredicates(), dimensionPlan);
        }
        return predicates;
    }

    private static void addRewriteFilterPredicates(List<String> target,
                                                   List<Map<String, Object>> predicates,
                                                   StarAggDimensionPlan dimensionPlan) {
        for (Map<String, Object> predicate : predicates) {
            if ("WHERE".equalsIgnoreCase(text(predicate.get("clause")))) {
                target.add(StarAggRewriteSupport.rewriteColumns(text(predicate.get("expression")), dimensionPlan));
            }
        }
    }

    private static List<String> rewriteGroupByItems(List<Map<String, Object>> groupBy,
                                                    StarAggDimensionPlan dimensionPlan) {
        List<String> items = new ArrayList<String>();
        Set<String> seen = new LinkedHashSet<String>();
        for (Map<String, Object> groupByItem : groupBy) {
            StarAggDimensionSpec dimension = dimensionPlan.find(text(groupByItem.get("expression")));
            if (dimension == null) {
                dimension = dimensionPlan.findBySources(stringList(groupByItem.get("sourceColumns")));
            }
            if (dimension != null && !seen.contains(dimension.outputName)) {
                items.add(dimension.outputName);
                seen.add(dimension.outputName);
            }
        }
        return items;
    }

    private static List<String> rewriteHavingPredicates(
        L2PredicateClassifier.PredicateClassificationResult classification,
        List<Map<String, Object>> measures,
        StarAggDimensionPlan dimensionPlan) {
        List<String> predicates = new ArrayList<String>();
        if (classification == null) {
            return predicates;
        }
        for (Map<String, Object> predicate : classification.getRetainedPredicates()) {
            if ("HAVING".equalsIgnoreCase(text(predicate.get("clause")))) {
                String expression = rewriteMeasureExpressions(text(predicate.get("expression")), measures);
                predicates.add(StarAggRewriteSupport.rewriteColumns(expression, dimensionPlan));
            }
        }
        return predicates;
    }

    private static String rewriteMeasureExpressions(String expression, List<Map<String, Object>> measures) {
        String result = expression;
        List<StarAggMeasureReplacement> replacements = new ArrayList<StarAggMeasureReplacement>();
        for (Map<String, Object> measure : measures) {
            addMeasureReplacement(replacements, text(measure.get("sourceExpression")), text(measure.get("rewriteExpression")));
            addMeasureReplacement(replacements, text(measure.get("name")), text(measure.get("rewriteExpression")));
            for (Map<String, Object> component : mapList(measure.get("components"))) {
                addMeasureReplacement(replacements, text(component.get("sourceExpression")), text(component.get("rewriteExpression")));
                addMeasureReplacement(replacements, text(component.get("name")), text(component.get("rewriteExpression")));
            }
        }
        Collections.sort(replacements, new Comparator<StarAggMeasureReplacement>() {
            @Override
            public int compare(StarAggMeasureReplacement left, StarAggMeasureReplacement right) {
                return Integer.compare(right.sourceExpression.length(), left.sourceExpression.length());
            }
        });
        boolean exactExpressionApplied = false;
        for (StarAggMeasureReplacement replacement : replacements) {
            if (replacement.sourceExpression.indexOf('(') >= 0) {
                String next = result.replace(replacement.sourceExpression, replacement.rewriteExpression);
                exactExpressionApplied = exactExpressionApplied || !next.equals(result);
                result = next;
            }
        }
        if (!exactExpressionApplied) {
            for (StarAggMeasureReplacement replacement : replacements) {
                if (replacement.sourceExpression.indexOf('(') < 0) {
                    result = replaceIdentifier(result, replacement.sourceExpression, replacement.rewriteExpression);
                }
            }
        }
        return result;
    }

    private static void addMeasureReplacement(List<StarAggMeasureReplacement> replacements,
                                              String sourceExpression,
                                              String rewriteExpression) {
        if (StringUtils.hasText(sourceExpression) && StringUtils.hasText(rewriteExpression)) {
            replacements.add(new StarAggMeasureReplacement(sourceExpression, rewriteExpression));
        }
    }

    private static String renderRewriteSql(String mvName,
                                           List<String> selectItems,
                                           List<String> wherePredicates,
                                           List<String> groupByItems,
                                           List<String> havingPredicates) {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT\n");
        for (int i = 0; i < selectItems.size(); i++) {
            builder.append("  ").append(selectItems.get(i)).append(i == selectItems.size() - 1 ? "\n" : ",\n");
        }
        builder.append("FROM ").append(mvName).append('\n');
        if (!wherePredicates.isEmpty()) {
            builder.append("WHERE ").append(String.join("\n  AND ", wherePredicates)).append('\n');
        }
        if (!groupByItems.isEmpty()) {
            builder.append("GROUP BY ").append(String.join(", ", groupByItems)).append('\n');
        }
        if (!havingPredicates.isEmpty()) {
            builder.append("HAVING ").append(String.join("\n  AND ", havingPredicates)).append('\n');
        }
        return builder.append(';').toString();
    }
}
