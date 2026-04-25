package com.company.sqlforge.common.governance;

import java.util.List;

public class GovernanceBenchmarkArtifactBatchOperationRequest {

    private String tenantId;
    private String operationType;
    private String operationReason;
    private String cleanupScope;
    private String batchId;
    private List<GovernanceBenchmarkArtifactBatchOperationTarget> targets;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public List<GovernanceBenchmarkArtifactBatchOperationTarget> getTargets() {
        return targets;
    }

    public void setTargets(List<GovernanceBenchmarkArtifactBatchOperationTarget> targets) {
        this.targets = targets;
    }
}
