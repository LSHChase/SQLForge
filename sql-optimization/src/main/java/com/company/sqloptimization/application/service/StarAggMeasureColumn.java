package com.company.sqloptimization.application.service;

final class StarAggMeasureColumn {

    final String name;
    final String sourceExpression;

    StarAggMeasureColumn(String name, String sourceExpression) {
        this.name = name;
        this.sourceExpression = sourceExpression;
    }
}
