package com.company.sqloptimization.domain.task;

/**
 * Supported async optimization workloads owned by the SQL optimization service.
 */
public enum OptimizationTaskType {
    PARSE,
    REWRITE,
    ACCELERATION_SUGGESTION
}
