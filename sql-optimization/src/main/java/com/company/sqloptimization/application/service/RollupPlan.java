package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RollupIdentifierSupport.normalizeExpression;
import static com.company.sqloptimization.application.service.RollupIdentifierSupport.unqualifiedName;
import static com.company.sqloptimization.application.service.RollupProfileValues.immutableMapList;
import static com.company.sqloptimization.application.service.RollupProfileValues.text;
import static com.company.sqloptimization.application.service.RollupSqlTextSupport.stripAlias;

import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class RollupPlan {

    final String queryTimeExpression;
    final String sourceColumn;
    final String queryTargetGrain;
    final String mvTimeExpression;
    final String mvTimeColumn;
    final String rewriteRollupExpression;
    final List<Map<String, Object>> blockingReasons;

    RollupPlan(String queryTimeExpression,
               String sourceColumn,
               String queryTargetGrain,
               String mvTimeExpression,
               String mvTimeColumn,
               String rewriteRollupExpression,
               List<Map<String, Object>> blockingReasons) {
        this.queryTimeExpression = text(queryTimeExpression);
        this.sourceColumn = text(sourceColumn);
        this.queryTargetGrain = text(queryTargetGrain);
        this.mvTimeExpression = text(mvTimeExpression);
        this.mvTimeColumn = text(mvTimeColumn);
        this.rewriteRollupExpression = text(rewriteRollupExpression);
        this.blockingReasons = immutableMapList(blockingReasons);
    }

    static RollupPlan blocked(String queryTimeExpression, List<Map<String, Object>> blockingReasons) {
        return new RollupPlan(queryTimeExpression, "", "", "", "", "", blockingReasons);
    }

    boolean matchesTimeReference(String expression) {
        String normalized = normalizeExpression(stripAlias(expression, ""));
        return StringUtils.hasText(normalized)
            && (normalized.equals(normalizeExpression(queryTimeExpression))
            || normalized.equals(normalizeExpression(sourceColumn))
            || normalized.equals(normalizeExpression(unqualifiedName(sourceColumn))));
    }
}
