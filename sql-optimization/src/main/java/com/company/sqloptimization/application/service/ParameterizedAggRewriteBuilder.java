package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.ParameterizedAggDimensionPlanner.findDimension;
import static com.company.sqloptimization.application.service.ParameterizedAggDimensionPlanner.findDimensionBySources;
import static com.company.sqloptimization.application.service.ParameterizedAggProfileValues.mapList;
import static com.company.sqloptimization.application.service.ParameterizedAggProfileValues.stringList;
import static com.company.sqloptimization.application.service.ParameterizedAggProfileValues.text;
import static com.company.sqloptimization.application.service.ParameterizedAggTextSupport.containsAggregateToken;
import static com.company.sqloptimization.application.service.ParameterizedAggTextSupport.normalizeExpression;
import static com.company.sqloptimization.application.service.ParameterizedAggTextSupport.replaceIdentifier;
import static com.company.sqloptimization.application.service.ParameterizedAggTextSupport.stripAlias;
import static com.company.sqloptimization.application.service.ParameterizedAggTextSupport.withAlias;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class ParameterizedAggRewriteBuilder {

    private ParameterizedAggRewriteBuilder() {
    }

    static String rewriteSql(String mvName,
                             Map<String, Object> advancedStructureProfile,
                             L2PredicateClassifier.PredicateClassificationResult predicateClassification,
                             List<Map<String, Object>> measures,
                             List<ParameterizedAggDimensionSpec> dimensions) {
        List<String> selectItems = rewriteSelectItems(advancedStructureProfile, measures, dimensions);
        List<String> wherePredicates = rewriteFilterPredicates(predicateClassification, dimensions);
        List<String> groupByItems = rewriteGroupByItems(mapList(advancedStructureProfile.get("groupBy")), dimensions);
        List<String> havingPredicates = rewriteHavingPredicates(predicateClassification, measures, dimensions);
        return renderRewriteSql(mvName, selectItems, wherePredicates, groupByItems, havingPredicates);
    }

    private static List<String> rewriteSelectItems(Map<String, Object> advancedStructureProfile,
                                                   List<Map<String, Object>> measures,
                                                   List<ParameterizedAggDimensionSpec> dimensions) {
        List<String> selectItems = new ArrayList<String>();
        Set<String> selectedDimensions = new LinkedHashSet<String>();
        for (Map<String, Object> projection : mapList(advancedStructureProfile.get("projections"))) {
            if (isMeasureProjection(projection, measures)) {
                continue;
            }
            ParameterizedAggDimensionSpec dimension = findDimension(text(projection.get("expression")), dimensions);
            if (dimension == null) {
                dimension = findDimensionBySources(stringList(projection.get("sourceColumns")), dimensions);
            }
            if (dimension != null && !selectedDimensions.contains(dimension.outputName)) {
                selectItems.add(withAlias(dimension.outputName, text(projection.get("alias"))));
                selectedDimensions.add(dimension.outputName);
            }
        }
        if (selectItems.isEmpty()) {
            addFallbackGroupByItems(selectItems, selectedDimensions, advancedStructureProfile, dimensions);
        }
        addMeasureRewriteItems(selectItems, measures);
        return selectItems;
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
        List<ParameterizedAggDimensionSpec> dimensions) {
        List<String> predicates = new ArrayList<String>();
        if (predicateClassification == null) {
            return predicates;
        }
        addRewriteFilterPredicates(predicates, predicateClassification.getExternalizedPredicates(), dimensions);
        addRewriteFilterPredicates(predicates, predicateClassification.getSecurityPredicates(), dimensions);
        return predicates;
    }

    private static void addRewriteFilterPredicates(List<String> target,
                                                   List<Map<String, Object>> predicates,
                                                   List<ParameterizedAggDimensionSpec> dimensions) {
        for (Map<String, Object> predicate : predicates) {
            if ("WHERE".equalsIgnoreCase(text(predicate.get("clause")))) {
                target.add(ParameterizedAggRewriteSupport.rewriteColumns(text(predicate.get("expression")), dimensions));
            }
        }
    }

    private static List<String> rewriteGroupByItems(List<Map<String, Object>> groupBy,
                                                    List<ParameterizedAggDimensionSpec> dimensions) {
        List<String> items = new ArrayList<String>();
        Set<String> seen = new LinkedHashSet<String>();
        for (Map<String, Object> groupByItem : groupBy) {
            ParameterizedAggDimensionSpec dimension = findDimension(text(groupByItem.get("expression")), dimensions);
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
        List<ParameterizedAggDimensionSpec> dimensions) {
        List<String> predicates = new ArrayList<String>();
        if (predicateClassification == null) {
            return predicates;
        }
        for (Map<String, Object> predicate : predicateClassification.getRetainedPredicates()) {
            if ("HAVING".equalsIgnoreCase(text(predicate.get("clause")))) {
                String expression = rewriteMeasureExpressions(text(predicate.get("expression")), measures);
                predicates.add(ParameterizedAggRewriteSupport.rewriteColumns(expression, dimensions));
            }
        }
        return predicates;
    }

    private static String rewriteMeasureExpressions(String expression, List<Map<String, Object>> measures) {
        String result = expression;
        List<ParameterizedAggMeasureReplacement> replacements = new ArrayList<ParameterizedAggMeasureReplacement>();
        for (Map<String, Object> measure : measures) {
            addMeasureReplacement(replacements, text(measure.get("sourceExpression")), text(measure.get("rewriteExpression")));
            addMeasureReplacement(replacements, text(measure.get("name")), text(measure.get("rewriteExpression")));
            for (Map<String, Object> component : mapList(measure.get("components"))) {
                addMeasureReplacement(replacements, text(component.get("sourceExpression")), text(component.get("rewriteExpression")));
                addMeasureReplacement(replacements, text(component.get("name")), text(component.get("rewriteExpression")));
            }
        }
        Collections.sort(replacements, new Comparator<ParameterizedAggMeasureReplacement>() {
            @Override
            public int compare(ParameterizedAggMeasureReplacement left, ParameterizedAggMeasureReplacement right) {
                return Integer.compare(right.sourceExpression.length(), left.sourceExpression.length());
            }
        });
        return applyMeasureReplacements(result, replacements);
    }

    private static String applyMeasureReplacements(String result, List<ParameterizedAggMeasureReplacement> replacements) {
        boolean exactExpressionApplied = false;
        for (ParameterizedAggMeasureReplacement replacement : replacements) {
            if (replacement.sourceExpression.indexOf('(') >= 0) {
                String next = result.replace(replacement.sourceExpression, replacement.rewriteExpression);
                exactExpressionApplied = exactExpressionApplied || !next.equals(result);
                result = next;
            }
        }
        if (!exactExpressionApplied) {
            for (ParameterizedAggMeasureReplacement replacement : replacements) {
                if (replacement.sourceExpression.indexOf('(') < 0) {
                    result = replaceIdentifier(result, replacement.sourceExpression, replacement.rewriteExpression);
                }
            }
        }
        return result;
    }

    private static void addFallbackGroupByItems(List<String> selectItems,
                                                Set<String> selectedDimensions,
                                                Map<String, Object> advancedStructureProfile,
                                                List<ParameterizedAggDimensionSpec> dimensions) {
        for (Map<String, Object> groupBy : mapList(advancedStructureProfile.get("groupBy"))) {
            ParameterizedAggDimensionSpec dimension = findDimension(text(groupBy.get("expression")), dimensions);
            if (dimension == null) {
                dimension = findDimensionBySources(stringList(groupBy.get("sourceColumns")), dimensions);
            }
            if (dimension != null && !selectedDimensions.contains(dimension.outputName)) {
                selectItems.add(dimension.outputName);
                selectedDimensions.add(dimension.outputName);
            }
        }
    }

    private static void addMeasureRewriteItems(List<String> selectItems, List<Map<String, Object>> measures) {
        for (Map<String, Object> measure : measures) {
            String rewriteExpression = text(measure.get("rewriteExpression"));
            String name = text(measure.get("name"));
            if (StringUtils.hasText(rewriteExpression) && StringUtils.hasText(name)) {
                selectItems.add(rewriteExpression + " AS " + name);
            }
        }
    }

    private static void addMeasureReplacement(List<ParameterizedAggMeasureReplacement> replacements,
                                              String sourceExpression,
                                              String rewriteExpression) {
        if (StringUtils.hasText(sourceExpression) && StringUtils.hasText(rewriteExpression)) {
            replacements.add(new ParameterizedAggMeasureReplacement(sourceExpression, rewriteExpression));
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
