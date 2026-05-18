package com.company.sqloptimization.domain.trial;

public enum RewriteTrialStatus {
    NOT_REQUESTED,
    QUEUED,
    RUNNING,
    CANDIDATE_GENERATED,
    NO_SAFE_REWRITE,
    FAILED,
    RECOMMENDED,
    RECORD_CREATED
}
