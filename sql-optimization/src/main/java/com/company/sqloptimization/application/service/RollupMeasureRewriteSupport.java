package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RollupIdentifierSupport.replaceIdentifier;
import static com.company.sqloptimization.application.service.RollupProfileValues.mapList;
import static com.company.sqloptimization.application.service.RollupProfileValues.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class RollupMeasureRewriteSupport {

    private RollupMeasureRewriteSupport() {
    }

    static String rewriteMeasureExpressions(String expression, List<Map<String, Object>> measures) {
        String result = expression;
        List<RollupMeasureReplacement> replacements = new ArrayList<RollupMeasureReplacement>();
        for (Map<String, Object> measure : measures) {
            addMeasureReplacement(replacements, text(measure.get("sourceExpression")), text(measure.get("rewriteExpression")));
            addMeasureReplacement(replacements, text(measure.get("name")), text(measure.get("rewriteExpression")));
            for (Map<String, Object> component : mapList(measure.get("components"))) {
                addMeasureReplacement(
                    replacements,
                    text(component.get("sourceExpression")),
                    text(component.get("rewriteExpression"))
                );
                addMeasureReplacement(replacements, text(component.get("name")), text(component.get("rewriteExpression")));
            }
        }
        Collections.sort(replacements, new Comparator<RollupMeasureReplacement>() {
            @Override
            public int compare(RollupMeasureReplacement left, RollupMeasureReplacement right) {
                return Integer.compare(right.sourceExpression.length(), left.sourceExpression.length());
            }
        });
        boolean exactExpressionApplied = false;
        for (RollupMeasureReplacement replacement : replacements) {
            if (replacement.sourceExpression.indexOf('(') >= 0) {
                String next = result.replace(replacement.sourceExpression, replacement.rewriteExpression);
                exactExpressionApplied = exactExpressionApplied || !next.equals(result);
                result = next;
            }
        }
        if (!exactExpressionApplied) {
            for (RollupMeasureReplacement replacement : replacements) {
                if (replacement.sourceExpression.indexOf('(') < 0) {
                    result = replaceIdentifier(result, replacement.sourceExpression, replacement.rewriteExpression);
                }
            }
        }
        return result;
    }

    private static void addMeasureReplacement(List<RollupMeasureReplacement> replacements,
                                              String sourceExpression,
                                              String rewriteExpression) {
        if (StringUtils.hasText(sourceExpression) && StringUtils.hasText(rewriteExpression)) {
            replacements.add(new RollupMeasureReplacement(sourceExpression, rewriteExpression));
        }
    }
}
