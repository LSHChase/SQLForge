package com.company.queryexecution.domain.query;

/**
 * Fault-tolerance strategy for the online query path.
 */
public enum FaultToleranceStrategy {
    RETRY_THEN_FALLBACK,
    FAIL_FAST,
    FALLBACK_IMMEDIATE
}
