package com.company.sqlforge.common.rewrite;

public class RuntimeSqlRewriteTemplateResult {

    private final boolean applied;
    private final String rewrittenSql;
    private final String matchMode;
    private final String failureReason;
    private final String programJson;

    private RuntimeSqlRewriteTemplateResult(boolean applied,
                                            String rewrittenSql,
                                            String matchMode,
                                            String failureReason,
                                            String programJson) {
        this.applied = applied;
        this.rewrittenSql = rewrittenSql;
        this.matchMode = matchMode;
        this.failureReason = failureReason;
        this.programJson = programJson;
    }

    public static RuntimeSqlRewriteTemplateResult applied(String rewrittenSql,
                                                          String matchMode,
                                                          String programJson) {
        return new RuntimeSqlRewriteTemplateResult(true, rewrittenSql, matchMode, null, programJson);
    }

    public static RuntimeSqlRewriteTemplateResult notApplied(String failureReason) {
        return new RuntimeSqlRewriteTemplateResult(false, null, null, failureReason, null);
    }

    public boolean isApplied() {
        return applied;
    }

    public String getRewrittenSql() {
        return rewrittenSql;
    }

    public String getMatchMode() {
        return matchMode;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getProgramJson() {
        return programJson;
    }
}
