package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.ParameterizedAggProfileValues.stringList;
import static com.company.sqloptimization.application.service.ParameterizedAggProfileValues.text;
import static com.company.sqloptimization.application.service.ParameterizedAggTextSupport.columnName;
import static com.company.sqloptimization.application.service.ParameterizedAggTextSupport.normalizeExpression;
import static com.company.sqloptimization.application.service.ParameterizedAggTextSupport.sameExpression;
import static com.company.sqloptimization.application.service.ParameterizedAggTextSupport.uniqueName;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class ParameterizedAggDimensionPlanner {

    private ParameterizedAggDimensionPlanner() {
    }

    static List<ParameterizedAggDimensionSpec> dimensionSpecs(List<String> dimensions,
                                                              List<Map<String, Object>> groupBy) {
        List<ParameterizedAggDimensionSpec> result = new ArrayList<ParameterizedAggDimensionSpec>();
        Set<String> usedNames = new LinkedHashSet<String>();
        for (String dimension : dimensions) {
            if (!StringUtils.hasText(dimension)) {
                continue;
            }
            LinkedHashSet<String> references = new LinkedHashSet<String>();
            references.add(dimension.trim());
            for (Map<String, Object> groupByItem : groupBy) {
                if (sameExpression(dimension, text(groupByItem.get("expression")))
                    || stringList(groupByItem.get("sourceColumns")).contains(dimension)) {
                    references.addAll(stringList(groupByItem.get("sourceColumns")));
                    references.add(text(groupByItem.get("expression")));
                }
            }
            String outputName = uniqueName(columnName(dimension), usedNames);
            result.add(new ParameterizedAggDimensionSpec(dimension.trim(), outputName, references));
        }
        return result;
    }

    static List<String> dimensionExpressions(List<ParameterizedAggDimensionSpec> dimensions) {
        List<String> expressions = new ArrayList<String>();
        for (ParameterizedAggDimensionSpec dimension : dimensions) {
            expressions.add(dimension.sourceExpression);
        }
        return expressions;
    }

    static ParameterizedAggDimensionSpec findDimension(String expression,
                                                       List<ParameterizedAggDimensionSpec> dimensions) {
        String normalized = normalizeExpression(expression);
        for (ParameterizedAggDimensionSpec dimension : dimensions) {
            if (dimension.matches(normalized)) {
                return dimension;
            }
        }
        return null;
    }

    static ParameterizedAggDimensionSpec findDimensionBySources(
        List<String> sourceColumns,
        List<ParameterizedAggDimensionSpec> dimensions) {
        for (String sourceColumn : sourceColumns) {
            ParameterizedAggDimensionSpec dimension = findDimension(sourceColumn, dimensions);
            if (dimension != null) {
                return dimension;
            }
        }
        return null;
    }
}
