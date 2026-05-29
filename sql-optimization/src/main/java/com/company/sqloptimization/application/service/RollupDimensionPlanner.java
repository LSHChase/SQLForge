package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RollupIdentifierSupport.columnName;
import static com.company.sqloptimization.application.service.RollupIdentifierSupport.normalizeExpression;
import static com.company.sqloptimization.application.service.RollupIdentifierSupport.uniqueName;
import static com.company.sqloptimization.application.service.RollupProfileValues.stringList;
import static com.company.sqloptimization.application.service.RollupProfileValues.text;
import static com.company.sqloptimization.application.service.RollupSqlTextSupport.leftPredicateField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class RollupDimensionPlanner {

    private RollupDimensionPlanner() {
    }

    static List<RollupDimensionSpec> dimensionSpecs(List<Map<String, Object>> groupBy,
                                                    L2PredicateClassifier.PredicateClassificationResult
                                                        predicateClassification,
                                                    RollupPlan rollupPlan) {
        List<RollupDimensionSpec> result = new ArrayList<RollupDimensionSpec>();
        Set<String> usedNames = new LinkedHashSet<String>();
        usedNames.add(rollupPlan.mvTimeColumn);
        Set<String> seen = new LinkedHashSet<String>();
        seen.add(normalizeExpression(rollupPlan.queryTimeExpression));
        seen.add(normalizeExpression(rollupPlan.sourceColumn));
        for (Map<String, Object> groupByItem : groupBy) {
            String expression = text(groupByItem.get("expression"));
            if (!StringUtils.hasText(expression) || rollupPlan.matchesTimeReference(expression)) {
                continue;
            }
            addDimension(result, seen, usedNames, expression, stringList(groupByItem.get("sourceColumns")), rollupPlan);
        }
        if (predicateClassification != null) {
            addPredicateDimensions(result, seen, usedNames, predicateClassification.getExternalizedPredicates(), rollupPlan);
            addPredicateDimensions(result, seen, usedNames, predicateClassification.getSecurityPredicates(), rollupPlan);
        }
        return result;
    }

    static List<String> dimensionExpressions(List<RollupDimensionSpec> dimensions) {
        List<String> expressions = new ArrayList<String>();
        for (RollupDimensionSpec dimension : dimensions) {
            expressions.add(dimension.sourceExpression);
        }
        return expressions;
    }

    private static void addPredicateDimensions(List<RollupDimensionSpec> result,
                                               Set<String> seen,
                                               Set<String> usedNames,
                                               List<Map<String, Object>> predicates,
                                               RollupPlan rollupPlan) {
        for (Map<String, Object> predicate : predicates) {
            if (!"WHERE".equalsIgnoreCase(text(predicate.get("clause")))) {
                continue;
            }
            List<String> sourceColumns = stringList(predicate.get("sourceColumns"));
            if (sourceColumns.isEmpty()) {
                sourceColumns = Collections.singletonList(leftPredicateField(text(predicate.get("expression"))));
            }
            for (String sourceColumn : sourceColumns) {
                addDimension(result, seen, usedNames, sourceColumn, Collections.singletonList(sourceColumn), rollupPlan);
            }
        }
    }

    private static void addDimension(List<RollupDimensionSpec> result,
                                     Set<String> seen,
                                     Set<String> usedNames,
                                     String expression,
                                     List<String> sourceColumns,
                                     RollupPlan rollupPlan) {
        if (!StringUtils.hasText(expression) || rollupPlan.matchesTimeReference(expression)) {
            return;
        }
        String normalizedExpression = normalizeExpression(expression);
        if (!StringUtils.hasText(normalizedExpression) || seen.contains(normalizedExpression)) {
            return;
        }
        LinkedHashSet<String> references = new LinkedHashSet<String>();
        references.add(expression.trim());
        for (String sourceColumn : sourceColumns) {
            if (StringUtils.hasText(sourceColumn) && !rollupPlan.matchesTimeReference(sourceColumn)) {
                references.add(sourceColumn.trim());
            }
        }
        String outputName = uniqueName(columnName(expression), usedNames);
        result.add(new RollupDimensionSpec(expression.trim(), outputName, references));
        seen.add(normalizedExpression);
        for (String reference : references) {
            seen.add(normalizeExpression(reference));
        }
    }
}
