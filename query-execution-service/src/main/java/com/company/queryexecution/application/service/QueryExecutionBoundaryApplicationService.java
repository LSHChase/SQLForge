package com.company.queryexecution.application.service;

import com.company.queryexecution.domain.boundary.QueryExecutionBoundaryDefinition;
import org.springframework.stereotype.Service;

/**
 * Provides the baseline boundary definition for the query execution service.
 */
@Service
public class QueryExecutionBoundaryApplicationService {

    public QueryExecutionBoundaryDefinition describeBoundary() {
        return QueryExecutionBoundaryDefinition.baseline();
    }
}
