package com.company.sqloptimization.domain.governance;

public enum RewriteValidationStatus {
    NOT_VALIDATED,
    VALIDATING,
    EQUIVALENT,
    DIVERGED,
    FAILED,
    EXPIRED
}
