package com.company.queryexecution.infrastructure.adapter;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;

/**
 * Infrastructure abstraction for the current deterministic synchronous execution path.
 */
public interface QueryExecutionAdapter {

    QueryExecutionStep execute(DataSourceTypeEnum targetEngine,
                               String actualSql,
                               QueryExecuteRequest request,
                               boolean degradedPath);
}
