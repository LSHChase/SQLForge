package com.company.sqlforge.common.jdbcagent;

import com.company.sqlforge.common.openaccess.SqlForgeQueryResponse;

public class JdbcAgentExecutionResult<T> {

    private final JdbcAgentDirectResult<T> directResult;
    private final SqlForgeQueryResponse governedResponse;
    private final JdbcAgentExecutionMetadata metadata;

    public JdbcAgentExecutionResult(JdbcAgentDirectResult<T> directResult,
                                    SqlForgeQueryResponse governedResponse,
                                    JdbcAgentExecutionMetadata metadata) {
        this.directResult = directResult;
        this.governedResponse = governedResponse;
        this.metadata = metadata;
    }

    public JdbcAgentDirectResult<T> getDirectResult() {
        return directResult;
    }

    public SqlForgeQueryResponse getGovernedResponse() {
        return governedResponse;
    }

    public JdbcAgentExecutionMetadata getMetadata() {
        return metadata;
    }
}
