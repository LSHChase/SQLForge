package com.company.sqloptimization.application.service;

import java.util.Set;

final class StarAggDimensionContext {

    final String sourceExpression;
    final Set<String> references;
    final boolean groupByDimension;
    final boolean externalized;
    final boolean security;

    StarAggDimensionContext(String sourceExpression,
                            Set<String> references,
                            boolean groupByDimension,
                            boolean externalized,
                            boolean security) {
        this.sourceExpression = sourceExpression;
        this.references = references;
        this.groupByDimension = groupByDimension;
        this.externalized = externalized;
        this.security = security;
    }
}
