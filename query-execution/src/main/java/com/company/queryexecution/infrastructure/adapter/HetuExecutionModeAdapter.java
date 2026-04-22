package com.company.queryexecution.infrastructure.adapter;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.domain.query.QueryExecutionAccessMode;
import com.company.queryexecution.domain.query.QueryExecutionStep;

public interface HetuExecutionModeAdapter {

    QueryExecutionAccessMode getMode();

    QueryExecutionStep execute(String actualSql, QueryExecuteRequest request, boolean degradedPath);
}
