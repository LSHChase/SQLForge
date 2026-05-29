package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RollupIdentifierSupport.replaceIdentifier;
import static com.company.sqloptimization.application.service.RollupIdentifierSupport.unqualifiedName;

import org.springframework.util.StringUtils;

final class RollupTimeReferenceRewriter {

    private RollupTimeReferenceRewriter() {
    }

    static String rewriteTimeReferences(String expression, RollupPlan rollupPlan) {
        String result = expression;
        if (!StringUtils.hasText(result) || rollupPlan == null) {
            return result;
        }
        if (StringUtils.hasText(rollupPlan.queryTimeExpression)) {
            result = result.replace(rollupPlan.queryTimeExpression, rollupPlan.rewriteRollupExpression);
        }
        result = replaceIdentifier(result, rollupPlan.sourceColumn, rollupPlan.mvTimeColumn);
        String unqualified = unqualifiedName(rollupPlan.sourceColumn);
        if (!unqualified.equals(rollupPlan.sourceColumn)) {
            result = replaceIdentifier(result, unqualified, rollupPlan.mvTimeColumn);
        }
        return result;
    }
}
