package com.company.sqlforge.common.openaccess;

import java.util.List;

public class SqlForgeQueryMetadata {

    private String targetEngine;
    private String actualSql;
    private Long elapsedMs;
    private Long scannedRows;
    private boolean cacheHit;
    private String cacheGovernanceStatus;
    private String cacheGovernanceEvidence;
    private boolean accelerationApplied;
    private String executionMode;
    private List<String> attemptedModes;
    private Integer rowCount;
    private String routeProfile;
    private List<String> routeOrder;
    private String routeEvidenceSource;
    private String routeVerificationStatus;

    public String getTargetEngine() {
        return targetEngine;
    }

    public void setTargetEngine(String targetEngine) {
        this.targetEngine = targetEngine;
    }

    public String getActualSql() {
        return actualSql;
    }

    public void setActualSql(String actualSql) {
        this.actualSql = actualSql;
    }

    public Long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public Long getScannedRows() {
        return scannedRows;
    }

    public void setScannedRows(Long scannedRows) {
        this.scannedRows = scannedRows;
    }

    public boolean isCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    public String getCacheGovernanceStatus() {
        return cacheGovernanceStatus;
    }

    public void setCacheGovernanceStatus(String cacheGovernanceStatus) {
        this.cacheGovernanceStatus = cacheGovernanceStatus;
    }

    public String getCacheGovernanceEvidence() {
        return cacheGovernanceEvidence;
    }

    public void setCacheGovernanceEvidence(String cacheGovernanceEvidence) {
        this.cacheGovernanceEvidence = cacheGovernanceEvidence;
    }

    public boolean isAccelerationApplied() {
        return accelerationApplied;
    }

    public void setAccelerationApplied(boolean accelerationApplied) {
        this.accelerationApplied = accelerationApplied;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }

    public List<String> getAttemptedModes() {
        return attemptedModes;
    }

    public void setAttemptedModes(List<String> attemptedModes) {
        this.attemptedModes = attemptedModes;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public String getRouteProfile() {
        return routeProfile;
    }

    public void setRouteProfile(String routeProfile) {
        this.routeProfile = routeProfile;
    }

    public List<String> getRouteOrder() {
        return routeOrder;
    }

    public void setRouteOrder(List<String> routeOrder) {
        this.routeOrder = routeOrder;
    }

    public String getRouteEvidenceSource() {
        return routeEvidenceSource;
    }

    public void setRouteEvidenceSource(String routeEvidenceSource) {
        this.routeEvidenceSource = routeEvidenceSource;
    }

    public String getRouteVerificationStatus() {
        return routeVerificationStatus;
    }

    public void setRouteVerificationStatus(String routeVerificationStatus) {
        this.routeVerificationStatus = routeVerificationStatus;
    }
}
