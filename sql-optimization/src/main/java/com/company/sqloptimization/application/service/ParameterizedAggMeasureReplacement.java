package com.company.sqloptimization.application.service;

final class ParameterizedAggMeasureReplacement {

    final String sourceExpression;
    final String rewriteExpression;

    ParameterizedAggMeasureReplacement(String sourceExpression, String rewriteExpression) {
        this.sourceExpression = sourceExpression;
        this.rewriteExpression = rewriteExpression;
    }
}
