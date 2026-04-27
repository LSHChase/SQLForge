package com.company.sqlforge.common.jdbcagent;

public class NoopJdbcAgentRewriteRuleProvider implements JdbcAgentRewriteRuleProvider {

    @Override
    public JdbcAgentRewriteDecision resolve(JdbcAgentObservation observation, JdbcAgentProperties properties) {
        return JdbcAgentRewriteDecision.passthrough("NO_RULE_MATCH");
    }
}
