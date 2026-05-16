package com.company.sqloptimization.domain.task;

/**
 * SQL 优化服务负责的受支持异步优化工作负载。
 */
public enum OptimizationTaskType {
    PARSE,
    REWRITE,
    ACCELERATION_SUGGESTION
}
