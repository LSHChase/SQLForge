package com.company.sqloptimization.application.service;

final class RollupParsedTimeExpression {

    final String targetGrain;
    final String sourceExpression;
    final String expressionKind;

    RollupParsedTimeExpression(String targetGrain, String sourceExpression, String expressionKind) {
        this.targetGrain = targetGrain;
        this.sourceExpression = sourceExpression;
        this.expressionKind = expressionKind;
    }
}
