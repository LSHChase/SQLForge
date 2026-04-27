package com.company.governance.application.controller.vo;

import java.time.Instant;

public class DispatchPolicyVO {

    private String policyId;
    private String tenantId;
    private String policyName;
    private String dispatchType;
    private String targetEngine;
    private String targetDatasource;
    private String ackMode;
    private Integer pullWindowSeconds;
    private Integer maxBatchSize;
    private String retryStrategy;
    private boolean enabled;
    private String enforcementStatus;
    private String executionBoundary;
    private Instant updatedAt;
    private String contractStage;
    private String implementationStage;

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public String getDispatchType() { return dispatchType; }
    public void setDispatchType(String dispatchType) { this.dispatchType = dispatchType; }
    public String getTargetEngine() { return targetEngine; }
    public void setTargetEngine(String targetEngine) { this.targetEngine = targetEngine; }
    public String getTargetDatasource() { return targetDatasource; }
    public void setTargetDatasource(String targetDatasource) { this.targetDatasource = targetDatasource; }
    public String getAckMode() { return ackMode; }
    public void setAckMode(String ackMode) { this.ackMode = ackMode; }
    public Integer getPullWindowSeconds() { return pullWindowSeconds; }
    public void setPullWindowSeconds(Integer pullWindowSeconds) { this.pullWindowSeconds = pullWindowSeconds; }
    public Integer getMaxBatchSize() { return maxBatchSize; }
    public void setMaxBatchSize(Integer maxBatchSize) { this.maxBatchSize = maxBatchSize; }
    public String getRetryStrategy() { return retryStrategy; }
    public void setRetryStrategy(String retryStrategy) { this.retryStrategy = retryStrategy; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getEnforcementStatus() { return enforcementStatus; }
    public void setEnforcementStatus(String enforcementStatus) { this.enforcementStatus = enforcementStatus; }
    public String getExecutionBoundary() { return executionBoundary; }
    public void setExecutionBoundary(String executionBoundary) { this.executionBoundary = executionBoundary; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getContractStage() { return contractStage; }
    public void setContractStage(String contractStage) { this.contractStage = contractStage; }
    public String getImplementationStage() { return implementationStage; }
    public void setImplementationStage(String implementationStage) { this.implementationStage = implementationStage; }
}
