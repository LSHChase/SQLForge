package com.company.governance.domain.redissource;

import java.time.Instant;

public class RedisRuleSourceConfig {

    private final String sourceId;
    private final String tenantId;
    private final String sourceName;
    private final String redisEndpoints;
    private final String redisNamespace;
    private final String keyPattern;
    private final String authMode;
    private final String credentialRef;
    private final boolean bypassOnUnavailable;
    private final boolean enabled;
    private final Instant updatedAt;

    public RedisRuleSourceConfig(String sourceId,
                                 String tenantId,
                                 String sourceName,
                                 String redisEndpoints,
                                 String redisNamespace,
                                 String keyPattern,
                                 String authMode,
                                 String credentialRef,
                                 boolean bypassOnUnavailable,
                                 boolean enabled,
                                 Instant updatedAt) {
        this.sourceId = sourceId;
        this.tenantId = tenantId;
        this.sourceName = sourceName;
        this.redisEndpoints = redisEndpoints;
        this.redisNamespace = redisNamespace;
        this.keyPattern = keyPattern;
        this.authMode = authMode;
        this.credentialRef = credentialRef;
        this.bypassOnUnavailable = bypassOnUnavailable;
        this.enabled = enabled;
        this.updatedAt = updatedAt;
    }

    public String getSourceId() { return sourceId; }
    public String getTenantId() { return tenantId; }
    public String getSourceName() { return sourceName; }
    public String getRedisEndpoints() { return redisEndpoints; }
    public String getRedisNamespace() { return redisNamespace; }
    public String getKeyPattern() { return keyPattern; }
    public String getAuthMode() { return authMode; }
    public String getCredentialRef() { return credentialRef; }
    public boolean isBypassOnUnavailable() { return bypassOnUnavailable; }
    public boolean isEnabled() { return enabled; }
    public Instant getUpdatedAt() { return updatedAt; }
}
