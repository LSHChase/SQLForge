package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.ParameterizedAggTextSupport.normalizeExpression;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.util.StringUtils;

final class ParameterizedAggDimensionSpec {

    final String sourceExpression;
    final String outputName;
    final Set<String> references;

    ParameterizedAggDimensionSpec(String sourceExpression, String outputName, Set<String> references) {
        this.sourceExpression = sourceExpression;
        this.outputName = outputName;
        this.references = new LinkedHashSet<String>();
        for (String reference : references) {
            if (StringUtils.hasText(reference)) {
                this.references.add(reference.trim());
            }
        }
    }

    String ddlSelectItem() {
        if (sourceExpression.equals(outputName)) {
            return sourceExpression;
        }
        return sourceExpression + " AS " + outputName;
    }

    boolean matches(String normalizedExpression) {
        if (normalizeExpression(sourceExpression).equals(normalizedExpression)
            || normalizeExpression(outputName).equals(normalizedExpression)) {
            return true;
        }
        for (String reference : references) {
            if (normalizeExpression(reference).equals(normalizedExpression)) {
                return true;
            }
        }
        return false;
    }
}
