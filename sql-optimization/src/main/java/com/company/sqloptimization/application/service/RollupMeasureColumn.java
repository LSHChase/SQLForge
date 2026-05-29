package com.company.sqloptimization.application.service;

final class RollupMeasureColumn {

    final String name;
    final String sourceExpression;

    RollupMeasureColumn(String name, String sourceExpression) {
        this.name = name;
        this.sourceExpression = sourceExpression;
    }
}
