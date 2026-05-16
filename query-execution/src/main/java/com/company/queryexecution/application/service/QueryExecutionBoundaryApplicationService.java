package com.company.queryexecution.application.service;

import com.company.queryexecution.domain.boundary.QueryExecutionBoundaryDefinition;
import org.springframework.stereotype.Service;

/**
 * 提供查询执行服务的基线边界定义。
 */
@Service
public class QueryExecutionBoundaryApplicationService {

    public QueryExecutionBoundaryDefinition describeBoundary() {
        return QueryExecutionBoundaryDefinition.baseline();
    }
}
