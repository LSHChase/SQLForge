package com.company.queryexecution.domain.query;

/**
 * Query execution result status exposed by the public contract.
 */
public enum QueryExecutionStatus {
    SUCCESS,
    PARTIAL,
    FAILED,
    TIMEOUT
}
