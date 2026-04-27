package com.company.sqloptimization.application.service.report;

public class ReportSqlResolveResult {

    private final String sqlText;
    private final String sourceType;
    private final String resolverStatus;
    private final String fallbackReason;

    public ReportSqlResolveResult(String sqlText, String sourceType, String resolverStatus, String fallbackReason) {
        this.sqlText = sqlText;
        this.sourceType = sourceType;
        this.resolverStatus = resolverStatus;
        this.fallbackReason = fallbackReason;
    }

    public String getSqlText() {
        return sqlText;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getResolverStatus() {
        return resolverStatus;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }
}
