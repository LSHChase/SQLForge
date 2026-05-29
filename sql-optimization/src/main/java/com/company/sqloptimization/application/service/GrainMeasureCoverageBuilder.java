package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.GrainMeasureProfileValues.stringList;
import static com.company.sqloptimization.application.service.GrainMeasureProfileValues.text;
import static com.company.sqloptimization.application.service.GrainMeasureTextSupport.leftPredicateField;
import static com.company.sqloptimization.application.service.GrainMeasureTextSupport.normalizeName;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GrainMeasureCoverageBuilder {

    private GrainMeasureCoverageBuilder() {
    }

    static LinkedHashMap<String, Object> coverage(
        GrainDerivationPlan grainDerivation,
        MeasureDerivationPlan measureDerivation,
        L2PredicateClassifier.PredicateClassificationResult predicateClassification) {
        LinkedHashMap<String, Object> coverage = new LinkedHashMap<String, Object>();
        coverage.put("coversProjection", Boolean.valueOf(measureDerivation.blockingReasons.isEmpty()));
        coverage.put("coversFilters", Boolean.valueOf(predicateClassification == null
            || !predicateClassification.hasBlockedPredicates()));
        coverage.put("coversGrouping", Boolean.TRUE);
        coverage.put("coversMeasures", Boolean.valueOf(measureDerivation.blockingReasons.isEmpty()
            && !measureDerivation.measures.isEmpty()));
        coverage.put("coversSecurity", Boolean.valueOf(securityCovered(
            grainDerivation.grain,
            predicateClassification == null
                ? Collections.<Map<String, Object>>emptyList()
                : predicateClassification.getSecurityPredicates()
        )));
        return coverage;
    }

    private static boolean securityCovered(List<String> grain, List<Map<String, Object>> securityPredicates) {
        if (securityPredicates == null || securityPredicates.isEmpty()) {
            return true;
        }
        Set<String> normalizedGrain = new LinkedHashSet<String>();
        for (String item : grain) {
            normalizedGrain.add(normalizeName(item));
        }
        for (Map<String, Object> predicate : securityPredicates) {
            List<String> columns = stringList(predicate.get("sourceColumns"));
            if (columns.isEmpty()) {
                columns = Collections.singletonList(leftPredicateField(text(predicate.get("expression"))));
            }
            for (String column : columns) {
                if (!normalizedGrain.contains(normalizeName(column))) {
                    return false;
                }
            }
        }
        return true;
    }
}
