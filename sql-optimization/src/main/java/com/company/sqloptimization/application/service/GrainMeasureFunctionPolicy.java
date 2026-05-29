package com.company.sqloptimization.application.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

final class GrainMeasureFunctionPolicy {

    static final Set<String> DIRECT_MERGEABLE_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("SUM", "COUNT", "MIN", "MAX"));
    static final Set<String> BLOCKED_PERCENTILE_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("APPROX_PERCENTILE", "PERCENTILE_CONT", "PERCENTILE_DISC", "QUANTILE"));
    static final Set<String> BLOCKED_COMPLEX_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("APPROX_DISTINCT", "GROUP_CONCAT", "STRING_AGG", "LISTAGG"));
    static final Set<String> TIME_ROLLUP_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("DATE_TRUNC", "TRUNC", "DATE_FORMAT", "YEAR", "MONTH", "DAY"));

    private GrainMeasureFunctionPolicy() {
    }
}
