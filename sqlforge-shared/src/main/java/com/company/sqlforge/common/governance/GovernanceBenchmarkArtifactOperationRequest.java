package com.company.sqlforge.common.governance;

public class GovernanceBenchmarkArtifactOperationRequest {

    private String tenantId;
    private String reportId;
    private String artifactKey;
    private String operationType;
    private String operationReason;
    private String cleanupScope;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getArtifactKey() {
        return artifactKey;
    }

    public void setArtifactKey(String artifactKey) {
        this.artifactKey = artifactKey;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperationReason() {
        return operationReason;
    }

    public void setOperationReason(String operationReason) {
        this.operationReason = operationReason;
    }

    public String getCleanupScope() {
        return cleanupScope;
    }

    public void setCleanupScope(String cleanupScope) {
        this.cleanupScope = cleanupScope;
    }
}
