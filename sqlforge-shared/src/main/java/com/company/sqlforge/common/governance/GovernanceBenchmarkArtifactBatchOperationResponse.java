package com.company.sqlforge.common.governance;

import java.util.List;
import java.util.Map;

public class GovernanceBenchmarkArtifactBatchOperationResponse {

    private String tenantId;
    private String batchId;
    private String operationType;
    private String operationStatus;
    private Integer totalItems;
    private Integer succeededItems;
    private Integer failedItems;
    private Integer skippedItems;
    private Boolean partialFailure;
    private List<GovernanceBenchmarkArtifactOperationResponse> items;
    private Map<String, Object> batchOperationSurface;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperationStatus() {
        return operationStatus;
    }

    public void setOperationStatus(String operationStatus) {
        this.operationStatus = operationStatus;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public Integer getSucceededItems() {
        return succeededItems;
    }

    public void setSucceededItems(Integer succeededItems) {
        this.succeededItems = succeededItems;
    }

    public Integer getFailedItems() {
        return failedItems;
    }

    public void setFailedItems(Integer failedItems) {
        this.failedItems = failedItems;
    }

    public Integer getSkippedItems() {
        return skippedItems;
    }

    public void setSkippedItems(Integer skippedItems) {
        this.skippedItems = skippedItems;
    }

    public Boolean getPartialFailure() {
        return partialFailure;
    }

    public void setPartialFailure(Boolean partialFailure) {
        this.partialFailure = partialFailure;
    }

    public List<GovernanceBenchmarkArtifactOperationResponse> getItems() {
        return items;
    }

    public void setItems(List<GovernanceBenchmarkArtifactOperationResponse> items) {
        this.items = items;
    }

    public Map<String, Object> getBatchOperationSurface() {
        return batchOperationSurface;
    }

    public void setBatchOperationSurface(Map<String, Object> batchOperationSurface) {
        this.batchOperationSurface = batchOperationSurface;
    }
}
