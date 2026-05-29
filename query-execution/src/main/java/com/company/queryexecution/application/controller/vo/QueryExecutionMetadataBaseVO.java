package com.company.queryexecution.application.controller.vo;

import java.util.Collections;
import java.util.List;

public class QueryExecutionMetadataBaseVO {

    private final String targetEngine;
    private final String actualSql;
    private final long elapsedMs;
    private final long scannedRows;
    private final boolean cacheHit;
    private final boolean accelerationApplied;
    private final String executionMode;
    private final List<String> attemptedModes;
    private final int rowCount;
    private final String routeProfile;
    private final List<String> routeOrder;
    private final String routeEvidenceSource;
    private final String routeVerificationStatus;
    private final String cacheGovernanceStatus;
    private final String cacheGovernanceEvidence;
    private final String originalSql;
    private final boolean rewriteApplied;
    private final String rewriteRecordId;
    private final String runtimeBindingId;
    private final Long ruleVersion;
    private final String runtimeRuleVersion;
    private final String runtimeRewriteStatus;
    private final String rewriteActivationStatusSnapshot;
    private final String rewriteFallbackReason;

    QueryExecutionMetadataBaseVO(String targetEngine, String actualSql, long elapsedMs, long scannedRows,
                                 boolean cacheHit, boolean accelerationApplied, String executionMode,
                                 List<String> attemptedModes, int rowCount, String routeProfile,
                                 List<String> routeOrder, String routeEvidenceSource,
                                 String routeVerificationStatus, String cacheGovernanceStatus,
                                 String cacheGovernanceEvidence, String originalSql, boolean rewriteApplied,
                                 String rewriteRecordId, String runtimeBindingId, Long ruleVersion,
                                 String runtimeRuleVersion, String runtimeRewriteStatus,
                                 String rewriteActivationStatusSnapshot, String rewriteFallbackReason) {
        this.targetEngine = targetEngine;
        this.actualSql = actualSql;
        this.elapsedMs = elapsedMs;
        this.scannedRows = scannedRows;
        this.cacheHit = cacheHit;
        this.accelerationApplied = accelerationApplied;
        this.executionMode = executionMode;
        this.attemptedModes = attemptedModes == null ? Collections.<String>emptyList() : attemptedModes;
        this.rowCount = rowCount;
        this.routeProfile = routeProfile;
        this.routeOrder = routeOrder == null ? Collections.<String>emptyList() : routeOrder;
        this.routeEvidenceSource = routeEvidenceSource;
        this.routeVerificationStatus = routeVerificationStatus;
        this.cacheGovernanceStatus = cacheGovernanceStatus;
        this.cacheGovernanceEvidence = cacheGovernanceEvidence;
        this.originalSql = originalSql == null ? actualSql : originalSql;
        this.rewriteApplied = rewriteApplied;
        this.rewriteRecordId = rewriteRecordId;
        this.runtimeBindingId = runtimeBindingId;
        this.ruleVersion = ruleVersion;
        this.runtimeRuleVersion = runtimeRuleVersion;
        this.runtimeRewriteStatus = runtimeRewriteStatus;
        this.rewriteActivationStatusSnapshot = rewriteActivationStatusSnapshot;
        this.rewriteFallbackReason = rewriteFallbackReason;
    }

    public String getTargetEngine() { return targetEngine; }
    public String getActualSql() { return actualSql; }
    public long getElapsedMs() { return elapsedMs; }
    public long getScannedRows() { return scannedRows; }
    public boolean isCacheHit() { return cacheHit; }
    public boolean isAccelerationApplied() { return accelerationApplied; }
    public String getExecutionMode() { return executionMode; }
    public List<String> getAttemptedModes() { return attemptedModes; }
    public int getRowCount() { return rowCount; }
    public String getRouteProfile() { return routeProfile; }
    public List<String> getRouteOrder() { return routeOrder; }
    public String getRouteEvidenceSource() { return routeEvidenceSource; }
    public String getRouteVerificationStatus() { return routeVerificationStatus; }
    public String getCacheGovernanceStatus() { return cacheGovernanceStatus; }
    public String getCacheGovernanceEvidence() { return cacheGovernanceEvidence; }
    public String getOriginalSql() { return originalSql; }
    public boolean isRewriteApplied() { return rewriteApplied; }
    public String getRewriteRecordId() { return rewriteRecordId; }
    public String getRuntimeBindingId() { return runtimeBindingId; }
    public Long getRuleVersion() { return ruleVersion; }
    public String getRuntimeRuleVersion() { return runtimeRuleVersion; }
    public String getRuntimeRewriteStatus() { return runtimeRewriteStatus; }
    public String getRewriteActivationStatusSnapshot() { return rewriteActivationStatusSnapshot; }
    public String getRewriteFallbackReason() { return rewriteFallbackReason; }
}
