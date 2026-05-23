package com.company.governance.infrastructure.persistence.entity;

import java.time.Instant;

public class RedisRuleSourceRecord {

    private String sourceId;
    private String tenantId;
    private String sourceName;
    private String redisEndpoints;
    private String redisNamespace;
    private String keyPattern;
    private String authMode;
    private String credentialRef;
    private Boolean bypassOnUnavailable;
    private Boolean enabled;
    private Instant createTime;
    private Instant updateTime;

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
    public Boolean getBypassOnUnavailable() { return bypassOnUnavailable; }
    public void setBypassOnUnavailable(Boolean bypassOnUnavailable) { this.bypassOnUnavailable = bypassOnUnavailable; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Instant getCreateTime() { return createTime; }
    public void setCreateTime(Instant createTime) { this.createTime = createTime; }
    public Instant getUpdateTime() { return updateTime; }
    public void setUpdateTime(Instant updateTime) { this.updateTime = updateTime; }
}
