package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggProfileValues.stringList;
import static com.company.sqloptimization.application.service.StarAggProfileValues.text;
import static com.company.sqloptimization.application.service.StarAggSqlTextSupport.leftPredicateField;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class StarAggSecurityCoverage {

    private StarAggSecurityCoverage() {
    }

    static boolean securityPredicatesCovered(
        L2PredicateClassifier.PredicateClassificationResult classification,
        List<StarAggDimensionSpec> dimensions) {
        if (classification == null || classification.getSecurityPredicates().isEmpty()) {
            return true;
        }
        for (Map<String, Object> predicate : classification.getSecurityPredicates()) {
            List<String> sourceColumns = stringList(predicate.get("sourceColumns"));
            if (sourceColumns.isEmpty()) {
                sourceColumns = Collections.singletonList(leftPredicateField(text(predicate.get("expression"))));
            }
            for (String sourceColumn : sourceColumns) {
                if (!coveredByDimension(sourceColumn, dimensions)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean coveredByDimension(String sourceColumn, List<StarAggDimensionSpec> dimensions) {
        for (StarAggDimensionSpec dimension : dimensions) {
            if (dimension.matches(sourceColumn)) {
                return true;
            }
        }
        return false;
    }
}
