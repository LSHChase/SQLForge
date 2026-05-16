package com.company.sqloptimization.domain.task;

/**
 * 解析、改写与加速建议任务共享的内部处理阶段。
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
