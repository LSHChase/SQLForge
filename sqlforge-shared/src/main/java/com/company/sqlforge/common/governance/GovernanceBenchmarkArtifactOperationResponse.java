package com.company.sqlforge.common.governance;

import java.util.Map;

public class GovernanceBenchmarkArtifactOperationResponse {

    private String tenantId;
    private String reportId;
    private String artifactKey;
    private String operationType;
    private String orchestrationType;
    private String batchId;
    private Integer batchIndex;
    private Integer batchSize;
    private String operationStatus;
    private String storageType;
    private String storageUri;
    private String storageEvidence;
    private String artifactRecoveryStatus;
    private String storageRecoverySource;
    private String storageReadStatus;
    private Integer errorCode;
    private String errorMessage;
    private Map<String, Object> artifactOperationSurface;

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

    public String getOrchestrationType() {
        return orchestrationType;
    }

    public void setOrchestrationType(String orchestrationType) {
        this.orchestrationType = orchestrationType;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public Integer getBatchIndex() {
        return batchIndex;
    }

    public void setBatchIndex(Integer batchIndex) {
        this.batchIndex = batchIndex;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public String getOperationStatus() {
        return operationStatus;
    }

    public void setOperationStatus(String operationStatus) {
        this.operationStatus = operationStatus;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getStorageUri() {
        return storageUri;
    }

    public void setStorageUri(String storageUri) {
        this.storageUri = storageUri;
    }

    public String getStorageEvidence() {
        return storageEvidence;
    }

    public void setStorageEvidence(String storageEvidence) {
        this.storageEvidence = storageEvidence;
    }

    public String getArtifactRecoveryStatus() {
        return artifactRecoveryStatus;
    }

    public void setArtifactRecoveryStatus(String artifactRecoveryStatus) {
        this.artifactRecoveryStatus = artifactRecoveryStatus;
    }

    public String getStorageRecoverySource() {
        return storageRecoverySource;
    }

    public void setStorageRecoverySource(String storageRecoverySource) {
        this.storageRecoverySource = storageRecoverySource;
    }

    public String getStorageReadStatus() {
        return storageReadStatus;
    }

    public void setStorageReadStatus(String storageReadStatus) {
        this.storageReadStatus = storageReadStatus;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, Object> getArtifactOperationSurface() {
        return artifactOperationSurface;
    }

    public void setArtifactOperationSurface(Map<String, Object> artifactOperationSurface) {
        this.artifactOperationSurface = artifactOperationSurface;
    }
}
