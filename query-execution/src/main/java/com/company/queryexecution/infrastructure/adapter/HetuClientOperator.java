package com.company.queryexecution.infrastructure.adapter;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.domain.query.QueryExecutionStep;

public interface HetuClientOperator {

    QueryExecutionStep execute(String actualSql, QueryExecuteRequest request, boolean degradedPath);
}
