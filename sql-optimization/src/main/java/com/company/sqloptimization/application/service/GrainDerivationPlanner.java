package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.GrainMeasureFunctionPolicy.TIME_ROLLUP_FUNCTIONS;
import static com.company.sqloptimization.application.service.GrainMeasureProfileValues.mapList;
import static com.company.sqloptimization.application.service.GrainMeasureProfileValues.stringList;
import static com.company.sqloptimization.application.service.GrainMeasureProfileValues.text;
import static com.company.sqloptimization.application.service.GrainMeasureProfileValues.upperText;
import static com.company.sqloptimization.application.service.GrainMeasureTextSupport.addText;
import static com.company.sqloptimization.application.service.GrainMeasureTextSupport.addTextList;
import static com.company.sqloptimization.application.service.GrainMeasureTextSupport.leftPredicateField;
import static com.company.sqloptimization.application.service.GrainMeasureTextSupport.normalizeName;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GrainDerivationPlanner {

    private GrainDerivationPlanner() {
    }

    static GrainDerivationPlan deriveGrain(Map<String, Object> advancedStructureProfile,
                                           L2PredicateClassifier.PredicateClassificationResult predicateClassification) {
        List<String> grain = new ArrayList<String>();
        Set<String> seen = new LinkedHashSet<String>();
        boolean hasTimeRollup = false;

        for (Map<String, Object> item : mapList(advancedStructureProfile.get("groupBy"))) {
            String expression = text(item.get("expression"));
            addText(grain, seen, expression);
            if (isStableTimeRollup(expression)) {
                hasTimeRollup = true;
                addTextList(grain, seen, stringList(item.get("sourceColumns")));
            }
        }
        if (predicateClassification != null) {
            addPredicateFields(grain, seen, predicateClassification.getExternalizedPredicates());
            addPredicateFields(grain, seen, predicateClassification.getSecurityPredicates());
        }
        for (Map<String, Object> item : mapList(advancedStructureProfile.get("timeFunctions"))) {
            String functionName = upperText(item.get("functionName"));
            if (TIME_ROLLUP_FUNCTIONS.contains(functionName)) {
                hasTimeRollup = true;
                addTextList(grain, seen, stringList(item.get("sourceColumns")));
            }
        }
        return new GrainDerivationPlan(grain, hasTimeRollup);
    }

    static GrainDerivationPlan withDistinctMeasureKeys(GrainDerivationPlan grainDerivation,
                                                       List<Map<String, Object>> measures) {
        List<String> grain = new ArrayList<String>(grainDerivation.grain);
        Set<String> seen = new LinkedHashSet<String>();
        for (String item : grain) {
            seen.add(normalizeName(item));
        }
        for (Map<String, Object> measure : measures) {
            if (!"COUNT_DISTINCT".equals(text(measure.get("measureType")))) {
                continue;
            }
            addText(grain, seen, text(measure.get("distinctArgument")));
        }
        return new GrainDerivationPlan(grain, grainDerivation.hasTimeRollup);
    }

    private static void addPredicateFields(List<String> grain,
                                           Set<String> seen,
                                           List<Map<String, Object>> predicates) {
        for (Map<String, Object> predicate : predicates) {
            List<String> sourceColumns = stringList(predicate.get("sourceColumns"));
            if (sourceColumns.isEmpty()) {
                addText(grain, seen, leftPredicateField(text(predicate.get("expression"))));
            } else {
                addTextList(grain, seen, sourceColumns);
            }
        }
    }

    private static boolean isStableTimeRollup(String expression) {
        String upper = upperText(expression);
        for (String functionName : TIME_ROLLUP_FUNCTIONS) {
            if (upper.startsWith(functionName + "(") || upper.contains(functionName + "(")) {
                return true;
            }
        }
        return false;
    }
}
