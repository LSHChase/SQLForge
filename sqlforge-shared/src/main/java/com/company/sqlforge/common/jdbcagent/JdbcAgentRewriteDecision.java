package com.company.sqlforge.common.jdbcagent;

public class JdbcAgentRewriteDecision {

    private final boolean applied;
    private final String rewrittenSql;
    private final String routeHint;
    private final String evidence;

    public JdbcAgentRewriteDecision(boolean applied, String rewrittenSql, String routeHint, String evidence) {
        this.applied = applied;
        this.rewrittenSql = rewrittenSql;
        this.routeHint = routeHint;
        this.evidence = evidence;
    }

    public static JdbcAgentRewriteDecision passthrough(String evidence) {
        return new JdbcAgentRewriteDecision(false, null, null, evidence);
    }

    public boolean isApplied() {
        return applied;
    }

    public String getRewrittenSql() {
        return rewrittenSql;
    }

    public String getRouteHint() {
        return routeHint;
    }

    public String getEvidence() {
        return evidence;
    }
}
