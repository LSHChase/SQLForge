package com.company.governance.application.controller.dto;

public class DispatchPolicyUpsertRequest {

    private String tenantId;
    private String policyName;
    private String dispatchType;
    private String targetEngine;
    private String targetDatasource;
    private String ackMode;
    private Integer pullWindowSeconds;
    private Integer maxBatchSize;
    private String retryStrategy;
    private Boolean enabled;

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
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
