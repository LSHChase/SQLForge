package com.company.sqlforge.common.jdbcagent;

public class JdbcAgentDirectExecution {

    private final JdbcAgentSqlRequest request;
    private final JdbcAgentObservation observation;
    private final String sqlText;
    private final String routeHint;
    private final boolean rewritten;
    private final String rewriteEvidence;

    public JdbcAgentDirectExecution(JdbcAgentSqlRequest request,
                                    JdbcAgentObservation observation,
                                    String sqlText,
                                    String routeHint,
                                    boolean rewritten,
                                    String rewriteEvidence) {
        this.request = request;
        this.observation = observation;
        this.sqlText = sqlText;
        this.routeHint = routeHint;
        this.rewritten = rewritten;
        this.rewriteEvidence = rewriteEvidence;
    }

    public JdbcAgentSqlRequest getRequest() {
        return request;
    }

    public JdbcAgentObservation getObservation() {
        return observation;
    }

    public String getSqlText() {
        return sqlText;
    }

    public String getRouteHint() {
        return routeHint;
    }

    public boolean isRewritten() {
        return rewritten;
    }

    public String getRewriteEvidence() {
        return rewriteEvidence;
    }
}
