package com.company.sqlforge.common.openaccess;

public enum SqlForgeFaultToleranceStrategy {
    RETRY_THEN_FALLBACK,
    FAIL_FAST,
    FALLBACK_IMMEDIATE
}
