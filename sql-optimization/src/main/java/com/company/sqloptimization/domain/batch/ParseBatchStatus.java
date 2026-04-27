package com.company.sqloptimization.domain.batch;

public enum ParseBatchStatus {
    UPLOADED,
    VALIDATING,
    READY,
    RUNNING_STRUCTURE,
    RUNNING_ACCESS,
    PARTIAL_COMPLETED,
    COMPLETED,
    FAILED
}
