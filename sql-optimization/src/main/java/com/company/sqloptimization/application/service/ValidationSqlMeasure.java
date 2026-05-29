package com.company.sqloptimization.application.service;

import org.springframework.util.StringUtils;

final class ValidationSqlMeasure {

    final String name;
    final String measureType;

    ValidationSqlMeasure(String name, String measureType) {
        this.name = name;
        this.measureType = StringUtils.hasText(measureType) ? measureType : "SUM";
    }
}
