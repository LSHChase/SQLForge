package com.company.sqloptimization.application.service;

final class StarAggMeasureReplacement {

    final String sourceExpression;
    final String rewriteExpression;

    StarAggMeasureReplacement(String sourceExpression, String rewriteExpression) {
        this.sourceExpression = sourceExpression;
        this.rewriteExpression = rewriteExpression;
    }
}
