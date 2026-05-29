package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RewriteCoverageFields.allFieldsCovered;
import static com.company.sqloptimization.application.service.RewriteCoverageFields.containsCoverageField;
import static com.company.sqloptimization.application.service.RewriteCoverageProfileValues.mapList;
import static com.company.sqloptimization.application.service.RewriteCoverageProfileValues.stringList;
import static com.company.sqloptimization.application.service.RewriteCoverageProfileValues.text;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.normalizeExpression;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.stripAlias;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class RewriteCoverageMeasures {

    private static final Set<String> AGGREGATE_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("SUM", "COUNT", "MIN", "MAX", "AVG"));

    private RewriteCoverageMeasures() {
    }

    static boolean coversMeasures(List<Map<String, Object>> aggregations,
                                  List<Map<String, Object>> measures,
                                  Set<String> coverageFields) {
        if (aggregations.isEmpty()) {
            return true;
        }
        for (Map<String, Object> aggregation : aggregations) {
            String expression = text(aggregation.get("expression"));
            if (measureCoversExpression(expression, "", measures, coverageFields)) {
                continue;
            }
            if (allFieldsCovered(stringList(aggregation.get("sourceColumns")), coverageFields)) {
                continue;
            }
            return false;
        }
        return true;
    }

    static boolean aggregationCovered(String expression,
                                      String alias,
                                      List<String> sourceColumns,
                                      List<Map<String, Object>> measures,
                                      Set<String> coverageFields) {
        if (measureCoversExpression(expression, alias, measures, coverageFields)) {
            return true;
        }
        if (org.springframework.util.StringUtils.hasText(alias) && containsCoverageField(coverageFields, alias)) {
            return true;
        }
        return allFieldsCovered(sourceColumns, coverageFields);
    }

    static boolean isAggregationProjection(Map<String, Object> projection, String expression) {
        if ("AGGREGATION".equalsIgnoreCase(text(projection.get("expressionType")))) {
            return true;
        }
        String upper = normalizeExpression(expression);
        for (String functionName : AGGREGATE_FUNCTIONS) {
            if (upper.contains(functionName + "(")) {
                return true;
            }
        }
        return false;
    }

    static boolean isCountAny(String expression) {
        String normalized = normalizeExpression(expression);
        return normalized.startsWith("COUNT(*)")
            || normalized.startsWith("COUNT(1)");
    }

    private static boolean measureCoversExpression(String expression,
                                                   String alias,
                                                   List<Map<String, Object>> measures,
                                                   Set<String> coverageFields) {
        String normalizedExpression = normalizeExpression(stripAlias(expression, alias));
        for (Map<String, Object> measure : measures) {
            if (normalizedExpression.equals(normalizeExpression(text(measure.get("sourceExpression"))))) {
                List<Map<String, Object>> components = mapList(measure.get("components"));
                return components.isEmpty()
                    ? containsCoverageField(coverageFields, text(measure.get("name")))
                    : componentsCovered(components, coverageFields);
            }
            if (org.springframework.util.StringUtils.hasText(alias)
                && alias.equalsIgnoreCase(text(measure.get("name")))
                && containsCoverageField(coverageFields, text(measure.get("name")))) {
                return true;
            }
            for (Map<String, Object> component : mapList(measure.get("components"))) {
                if (normalizedExpression.equals(normalizeExpression(text(component.get("sourceExpression"))))) {
                    return containsCoverageField(coverageFields, text(component.get("name")));
                }
            }
        }
        return false;
    }

    private static boolean componentsCovered(List<Map<String, Object>> components, Set<String> coverageFields) {
        if (components.isEmpty()) {
            return false;
        }
        for (Map<String, Object> component : components) {
            if (!containsCoverageField(coverageFields, text(component.get("name")))) {
                return false;
            }
        }
        return true;
    }
}
