package com.company.queryexecution.domain.query;

/**
 * 在线查询路径的容错策略。
 */
public enum FaultToleranceStrategy {
    RETRY_THEN_FALLBACK,
    FAIL_FAST,
    FALLBACK_IMMEDIATE
}
