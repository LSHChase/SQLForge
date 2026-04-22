package com.company.queryexecution.infrastructure.adapter;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import org.springframework.stereotype.Component;

@Component
public class NoopHetuClientOperator implements HetuClientOperator {

    @Override
    public QueryExecutionStep execute(String actualSql, QueryExecuteRequest request, boolean degradedPath) {
        throw new IllegalStateException("Hetu client mode is not configured");
    }
}
