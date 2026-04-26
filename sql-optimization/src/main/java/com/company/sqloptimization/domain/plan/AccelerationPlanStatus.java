package com.company.sqloptimization.domain.plan;

public enum AccelerationPlanStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    APPLY_FAILED,
    APPLIED,
    VERIFY_FAILED,
    VERIFIED,
    ROLLBACK_FAILED,
    ROLLED_BACK
}
