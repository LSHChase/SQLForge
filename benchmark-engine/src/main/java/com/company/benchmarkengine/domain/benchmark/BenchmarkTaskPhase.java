package com.company.benchmarkengine.domain.benchmark;

public enum BenchmarkTaskPhase {

    SUBMITTED,
    BASELINE_PREPARING,
    SHADOW_VALIDATING,
    WARMING_UP,
    EXECUTING,
    THRESHOLD_EVALUATING,
    REPORTING,
    FINISHED
}
