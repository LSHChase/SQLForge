package com.company.sqlforge.common.jdbcagent;

public class JdbcAgentExecutionMetadata {

    private String effectiveMode;
    private boolean fallbackApplied;
    private boolean rewriteApplied;
    private String routeHint;
    private String rewriteEvidence;
    private boolean auditReported;
    private String auditFailureReason;
    private String platformFailureReason;
    private String lightParseStatus;
    private JdbcAgentObservation observation;

    public String getEffectiveMode() {
        return effectiveMode;
    }

    public void setEffectiveMode(String effectiveMode) {
        this.effectiveMode = effectiveMode;
    }

    public boolean isFallbackApplied() {
        return fallbackApplied;
    }

    public void setFallbackApplied(boolean fallbackApplied) {
        this.fallbackApplied = fallbackApplied;
    }

    public boolean isRewriteApplied() {
        return rewriteApplied;
    }

    public void setRewriteApplied(boolean rewriteApplied) {
        this.rewriteApplied = rewriteApplied;
    }

    public String getRouteHint() {
        return routeHint;
    }

    public void setRouteHint(String routeHint) {
        this.routeHint = routeHint;
    }

    public String getRewriteEvidence() {
        return rewriteEvidence;
    }

    public void setRewriteEvidence(String rewriteEvidence) {
        this.rewriteEvidence = rewriteEvidence;
    }

    public boolean isAuditReported() {
        return auditReported;
    }

    public void setAuditReported(boolean auditReported) {
        this.auditReported = auditReported;
    }

    public String getAuditFailureReason() {
        return auditFailureReason;
    }

    public void setAuditFailureReason(String auditFailureReason) {
        this.auditFailureReason = auditFailureReason;
    }

    public String getPlatformFailureReason() {
        return platformFailureReason;
    }

    public void setPlatformFailureReason(String platformFailureReason) {
        this.platformFailureReason = platformFailureReason;
    }

    public String getLightParseStatus() {
        return lightParseStatus;
    }

    public void setLightParseStatus(String lightParseStatus) {
        this.lightParseStatus = lightParseStatus;
    }

    public JdbcAgentObservation getObservation() {
        return observation;
    }

    public void setObservation(JdbcAgentObservation observation) {
        this.observation = observation;
    }
}
