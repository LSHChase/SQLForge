package com.company.sqloptimization.application.service;

final class RollupMeasureReplacement {

    final String sourceExpression;
    final String rewriteExpression;

    RollupMeasureReplacement(String sourceExpression, String rewriteExpression) {
        this.sourceExpression = sourceExpression;
        this.rewriteExpression = rewriteExpression;
    }
}
