package com.company.governance.application.controller.vo;

import java.time.Instant;

public class RedisRuleSourceVO {

    private String sourceId;
    private String tenantId;
    private String sourceName;
    private String redisEndpoints;
    private String redisNamespace;
    private String keyPattern;
    private String authMode;
    private String credentialRef;
    private boolean bypassOnUnavailable;
    private boolean enabled;
    private String healthStatus;
    private String unavailableReason;
    private String activationMode;
    private Instant updatedAt;
    private String contractStage;
    private String implementationStage;

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getRedisEndpoints() { return redisEndpoints; }
    public void setRedisEndpoints(String redisEndpoints) { this.redisEndpoints = redisEndpoints; }
    public String getRedisNamespace() { return redisNamespace; }
    public void setRedisNamespace(String redisNamespace) { this.redisNamespace = redisNamespace; }
    public String getKeyPattern() { return keyPattern; }
    public void setKeyPattern(String keyPattern) { this.keyPattern = keyPattern; }
    public String getAuthMode() { return authMode; }
    public void setAuthMode(String authMode) { this.authMode = authMode; }
    public String getCredentialRef() { return credentialRef; }
    public void setCredentialRef(String credentialRef) { this.credentialRef = credentialRef; }
    public boolean isBypassOnUnavailable() { return bypassOnUnavailable; }
    public void setBypassOnUnavailable(boolean bypassOnUnavailable) { this.bypassOnUnavailable = bypassOnUnavailable; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }
    public String getUnavailableReason() { return unavailableReason; }
    public void setUnavailableReason(String unavailableReason) { this.unavailableReason = unavailableReason; }
    public String getActivationMode() { return activationMode; }
    public void setActivationMode(String activationMode) { this.activationMode = activationMode; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getContractStage() { return contractStage; }
    public void setContractStage(String contractStage) { this.contractStage = contractStage; }
    public String getImplementationStage() { return implementationStage; }
    public void setImplementationStage(String implementationStage) { this.implementationStage = implementationStage; }
}
