package com.company.queryexecution.infrastructure.adapter;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.domain.query.QueryExecutionAccessMode;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            throw new IllegalStateException("Hetu client 模式已禁用");
        }
        QueryExecutionStep step = hetuClientOperator.execute(actualSql, request, degradedPath);
        return new QueryExecutionStep(
            step.getTargetEngine(),
            sanitizeRows(step.getRows(), degradedPath),
            step.getElapsedMs(),
            step.getScannedRows(),
            step.isCacheHit(),
            step.isAccelerationApplied(),
            QueryExecutionAccessMode.CLIENT.name(),
            java.util.Collections.singletonList(QueryExecutionAccessMode.CLIENT.name())
        );
    }

    private List<Map<String, Object>> sanitizeRows(List<Map<String, Object>> rows, boolean degradedPath) {
        if (rows == null) {
            return java.util.Collections.<Map<String, Object>>emptyList();
        }
        List<Map<String, Object>> sanitized = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> copy = new LinkedHashMap<String, Object>(row);
            copy.put("engine", "HETU");
            copy.put("mode", degradedPath ? "FALLBACK" : "PRIMARY");
            copy.put("executionMode", QueryExecutionAccessMode.CLIENT.name());
            sanitized.add(copy);
        }
        return sanitized;
    }
}
