package com.company.governance.domain.dispatchpolicy;

import java.time.Instant;

public class DispatchPolicyConfig {

    private final String policyId;
    private final String tenantId;
    private final String policyName;
    private final String dispatchType;
    private final String targetEngine;
    private final String targetDatasource;
    private final String ackMode;
    private final int pullWindowSeconds;
    private final int maxBatchSize;
    private final String retryStrategy;
    private final boolean enabled;
    private final Instant updatedAt;

    public DispatchPolicyConfig(String policyId,
                                String tenantId,
                                String policyName,
                                String dispatchType,
                                String targetEngine,
                                String targetDatasource,
                                String ackMode,
                                int pullWindowSeconds,
                                int maxBatchSize,
                                String retryStrategy,
                                boolean enabled,
                                Instant updatedAt) {
        this.policyId = policyId;
        this.tenantId = tenantId;
        this.policyName = policyName;
        this.dispatchType = dispatchType;
        this.targetEngine = targetEngine;
        this.targetDatasource = targetDatasource;
        this.ackMode = ackMode;
        this.pullWindowSeconds = pullWindowSeconds;
        this.maxBatchSize = maxBatchSize;
        this.retryStrategy = retryStrategy;
        this.enabled = enabled;
        this.updatedAt = updatedAt;
    }

    public String getPolicyId() { return policyId; }
    public String getTenantId() { return tenantId; }
    public String getPolicyName() { return policyName; }
    public String getDispatchType() { return dispatchType; }
    public String getTargetEngine() { return targetEngine; }
    public String getTargetDatasource() { return targetDatasource; }
    public String getAckMode() { return ackMode; }
    public int getPullWindowSeconds() { return pullWindowSeconds; }
    public int getMaxBatchSize() { return maxBatchSize; }
    public String getRetryStrategy() { return retryStrategy; }
    public boolean isEnabled() { return enabled; }
    public Instant getUpdatedAt() { return updatedAt; }
}
