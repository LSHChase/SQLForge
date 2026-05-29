package com.company.sqloptimization.application.service;

final class ParameterizedAggMeasureColumn {

    final String name;
    final String sourceExpression;

    ParameterizedAggMeasureColumn(String name, String sourceExpression) {
        this.name = name;
        this.sourceExpression = sourceExpression;
    }
}
