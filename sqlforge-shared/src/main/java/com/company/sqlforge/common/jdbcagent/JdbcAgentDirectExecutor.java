package com.company.sqlforge.common.jdbcagent;

public interface JdbcAgentDirectExecutor<T> {

    JdbcAgentDirectResult<T> execute(JdbcAgentDirectExecution execution) throws Exception;
}
