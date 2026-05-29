package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggReferenceSupport.normalizeExpression;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

final class StarAggDimensionSpec {

    final String sourceExpression;
    final String outputName;
    final Set<String> references;
    final StarAggRelationSpec relation;
    final boolean groupByDimension;
    final boolean externalized;
    final boolean security;
    final String role;

    StarAggDimensionSpec(String sourceExpression,
                         String outputName,
                         Set<String> references,
                         StarAggRelationSpec relation,
                         boolean groupByDimension,
                         boolean externalized,
                         boolean security,
                         String role) {
        this.sourceExpression = sourceExpression;
        this.outputName = outputName;
        this.references = Collections.unmodifiableSet(new LinkedHashSet<String>(references));
        this.relation = relation;
        this.groupByDimension = groupByDimension;
        this.externalized = externalized;
        this.security = security;
        this.role = role;
    }

    boolean matches(String expression) {
        String normalized = normalizeExpression(expression);
        if (normalizeExpression(sourceExpression).equals(normalized)
            || normalizeExpression(outputName).equals(normalized)) {
            return true;
        }
        for (String reference : references) {
            if (normalizeExpression(reference).equals(normalized)) {
                return true;
            }
        }
        return false;
    }
}
