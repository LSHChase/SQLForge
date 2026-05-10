package com.company.sqloptimization.domain.governance;

public enum GovernanceSourceKind {
    STRUCTURE_PARSE,
    COMBINED_PARSE,
    PARSE_BATCH,
    REPORT_BATCH,
    END_OF_DAY_SLOW_SQL,
    QUERY_HISTORY,
    SLOW_SQL,
    HIGH_P99,
    HIGH_SCAN,
    BENCHMARK_REGRESSION,
    MANUAL
}
