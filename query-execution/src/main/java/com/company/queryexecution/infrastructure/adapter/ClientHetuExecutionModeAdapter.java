package com.company.queryexecution.infrastructure.adapter;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.domain.query.QueryExecutionAccessMode;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import org.springframework.stereotype.Component;

@Component
public class ClientHetuExecutionModeAdapter implements HetuExecutionModeAdapter {

    private final QueryExecutionHetuProperties properties;
    private final HetuClientOperator hetuClientOperator;

    public ClientHetuExecutionModeAdapter(QueryExecutionHetuProperties properties,
                                          HetuClientOperator hetuClientOperator) {
        this.properties = properties;
        this.hetuClientOperator = hetuClientOperator;
    }

    @Override
    public QueryExecutionAccessMode getMode() {
        return QueryExecutionAccessMode.CLIENT;
    }

    @Override
    public QueryExecutionStep execute(String actualSql, QueryExecuteRequest request, boolean degradedPath) {
        if (!properties.getClient().isEnabled()) {
            throw new IllegalStateException("Hetu client mode is disabled");
        }
        QueryExecutionStep step = hetuClientOperator.execute(actualSql, request, degradedPath);
        return new QueryExecutionStep(
            step.getTargetEngine(),
            step.getRows(),
            step.getElapsedMs(),
            step.getScannedRows(),
            step.isCacheHit(),
            step.isAccelerationApplied(),
            QueryExecutionAccessMode.CLIENT.name(),
            java.util.Collections.singletonList(QueryExecutionAccessMode.CLIENT.name())
        );
    }
}
