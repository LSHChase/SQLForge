package com.company.sqlforge.common.jdbcagent;

import java.time.Instant;
import org.springframework.util.StringUtils;

public class JdbcAgentRedisRuleMetadata {

    private String tenantId;
    private String sqlFingerprint;
    private String runtimeBindingId;
    private String datasourceCode;
    private String originalSqlText;
    private String rewriteProgramJson;
    private String templateFamilyFingerprint;
    private String status;
    private Long ruleVersion;
    private String runtimeRuleVersion;
    private String updatedAt;
    private String expiresAt;
    private String syncStatus;

    public boolean isActiveAt(Instant now) {
        if (!"ACTIVE".equals(status)) {
            return false;
        }
        return !isExpiredAt(now);
    }

    public boolean isExpiredAt(Instant now) {
        if (!StringUtils.hasText(expiresAt)) {
            return false;
        }
        try {
            Instant expiry = Instant.parse(expiresAt.trim());
            Instant reference = now == null ? Instant.now() : now;
            return !expiry.isAfter(reference);
        } catch (RuntimeException ex) {
            return true;
        }
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getRuntimeBindingId() { return runtimeBindingId; }
    public void setRuntimeBindingId(String runtimeBindingId) { this.runtimeBindingId = runtimeBindingId; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getOriginalSqlText() { return originalSqlText; }
    public void setOriginalSqlText(String originalSqlText) { this.originalSqlText = originalSqlText; }
    public String getRewriteProgramJson() { return rewriteProgramJson; }
    public void setRewriteProgramJson(String rewriteProgramJson) { this.rewriteProgramJson = rewriteProgramJson; }
    public String getTemplateFamilyFingerprint() { return templateFamilyFingerprint; }
    public void setTemplateFamilyFingerprint(String templateFamilyFingerprint) { this.templateFamilyFingerprint = templateFamilyFingerprint; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(Long ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getRuntimeRuleVersion() { return runtimeRuleVersion; }
    public void setRuntimeRuleVersion(String runtimeRuleVersion) { this.runtimeRuleVersion = runtimeRuleVersion; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
