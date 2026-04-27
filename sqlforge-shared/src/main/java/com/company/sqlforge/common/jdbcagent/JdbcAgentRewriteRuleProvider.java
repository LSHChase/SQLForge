package com.company.sqlforge.common.jdbcagent;

public interface JdbcAgentRewriteRuleProvider {

    JdbcAgentRewriteDecision resolve(JdbcAgentObservation observation, JdbcAgentProperties properties) throws Exception;
}
