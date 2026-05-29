package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.AggregateCallParser.containsMeasureFunction;
import static com.company.sqloptimization.application.service.AggregateCallParser.parseAggregateCall;
import static com.company.sqloptimization.application.service.AggregateCallParser.splitTopLevelDivision;
import static com.company.sqloptimization.application.service.GrainMeasureProfileValues.booleanValue;
import static com.company.sqloptimization.application.service.GrainMeasureProfileValues.text;
import static com.company.sqloptimization.application.service.GrainMeasureTextSupport.normalizeExpression;
import static com.company.sqloptimization.application.service.GrainMeasureTextSupport.stripAlias;
import static com.company.sqloptimization.application.service.MeasureOutcomeFactory.blockingReason;
import static com.company.sqloptimization.application.service.MeasureOutcomeFactory.deriveMeasure;
import static com.company.sqloptimization.application.service.MeasureOutcomeFactory.deriveRatioMeasure;
import static com.company.sqloptimization.application.service.MeasureOutcomeFactory.nonMergeableMeasure;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class MeasureDerivationPlanner {

    private MeasureDerivationPlanner() {
    }

    static MeasureDerivationPlan deriveMeasures(List<Map<String, Object>> projections,
                                                List<Map<String, Object>> aggregations) {
        List<Map<String, Object>> measures = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        Set<String> consumedAggregateExpressions = new LinkedHashSet<String>();
        int sequence = 1;

        for (Map<String, Object> projection : projections) {
            String alias = text(projection.get("alias"));
            String sourceExpression = stripAlias(text(projection.get("expression")), alias);
            if (!StringUtils.hasText(sourceExpression)) {
                continue;
            }
            MeasureDeriveOutcome outcome = deriveProjectionMeasure(
                sourceExpression,
                alias,
                sequence,
                consumedAggregateExpressions
            );
            if (outcome != null) {
                sequence++;
                measures.add(outcome.measure);
                blockingReasons.addAll(outcome.blockingReasons);
            }
        }

        for (Map<String, Object> aggregation : aggregations) {
            AggregateCallInfo call = parseAggregateCall(
                text(aggregation.get("expression")),
                text(aggregation.get("functionName")),
                booleanValue(aggregation.get("distinct"))
            );
            if (call == null || consumedAggregateExpressions.contains(normalizeExpression(call.expression))) {
                continue;
            }
            MeasureDeriveOutcome outcome = deriveMeasure(call, "", sequence);
            sequence++;
            measures.add(outcome.measure);
            blockingReasons.addAll(outcome.blockingReasons);
        }
        return new MeasureDerivationPlan(measures, blockingReasons);
    }

    private static MeasureDeriveOutcome deriveProjectionMeasure(String sourceExpression,
                                                                String alias,
                                                                int sequence,
                                                                Set<String> consumedAggregateExpressions) {
        RatioParts ratioParts = splitTopLevelDivision(sourceExpression);
        if (ratioParts != null) {
            MeasureDeriveOutcome outcome = deriveRatioMeasure(sourceExpression, alias, ratioParts, sequence);
            consumedAggregateExpressions.add(normalizeExpression(ratioParts.numerator));
            consumedAggregateExpressions.add(normalizeExpression(ratioParts.denominator));
            return outcome;
        }
        AggregateCallInfo call = parseAggregateCall(sourceExpression, null, false);
        if (call != null) {
            MeasureDeriveOutcome outcome = deriveMeasure(call, alias, sequence);
            consumedAggregateExpressions.add(normalizeExpression(call.expression));
            return outcome;
        }
        if (containsMeasureFunction(sourceExpression)) {
            Map<String, Object> measure = nonMergeableMeasure(
                alias,
                "COMPLEX_EXPRESSION",
                sourceExpression,
                "COMPLEX_MEASURE_EXPRESSION_NOT_MERGEABLE"
            );
            List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
            blockingReasons.add(blockingReason(
                "COMPLEX_MEASURE_EXPRESSION_NOT_MERGEABLE",
                "复杂指标表达式无法在 AMV-004 中证明可重聚合。",
                sourceExpression
            ));
            return new MeasureDeriveOutcome(measure, blockingReasons);
        }
        return null;
    }
}
