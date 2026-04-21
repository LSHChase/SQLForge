package com.company.sqloptimization.domain.task;

/**
 * Internal processing phases shared by parse, rewrite, and acceleration-suggestion tasks.
 */
public enum OptimizationTaskPhase {
    SUBMITTED,
    DEEP_PARSING,
    SQL_REWRITING,
    COST_ESTIMATING,
    ACCELERATION_PLANNING,
    RESULT_ASSEMBLING,
    FINISHED
}
