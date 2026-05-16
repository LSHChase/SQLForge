package com.company.queryexecution.domain.query;

/**
 * 公共契约暴露的查询执行结果状态。
 */
public enum QueryExecutionStatus {
    SUCCESS,
    PARTIAL,
    FAILED,
    TIMEOUT
}
