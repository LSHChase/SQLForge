package com.company.sqlforge.common.jdbcagent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class JdbcAgentObservation {

    private final String originalSql;
    private final String templateSql;
    private final String boundSqlText;
    private final String sqlFingerprint;
    private final Map<String, String> commentContext;
    private final JdbcAgentQueryDateSummary queryDateSummary;
    private final boolean parameterized;
    private final int parameterCount;

    public JdbcAgentObservation(String originalSql,
                                String templateSql,
                                String boundSqlText,
                                String sqlFingerprint,
                                Map<String, String> commentContext,
                                JdbcAgentQueryDateSummary queryDateSummary,
                                boolean parameterized,
                                int parameterCount) {
        this.originalSql = originalSql;
        this.templateSql = templateSql;
        this.boundSqlText = boundSqlText;
        this.sqlFingerprint = sqlFingerprint;
        this.commentContext = Collections.unmodifiableMap(new LinkedHashMap<String, String>(
            commentContext == null ? Collections.<String, String>emptyMap() : commentContext
        ));
        this.queryDateSummary = queryDateSummary;
        this.parameterized = parameterized;
        this.parameterCount = parameterCount;
    }

    public String getOriginalSql() {
        return originalSql;
    }

    public String getTemplateSql() {
        return templateSql;
    }

    public String getBoundSqlText() {
        return boundSqlText;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public Map<String, String> getCommentContext() {
        return commentContext;
    }

    public JdbcAgentQueryDateSummary getQueryDateSummary() {
        return queryDateSummary;
    }

    public boolean isParameterized() {
        return parameterized;
    }

    public int getParameterCount() {
        return parameterCount;
    }
}
