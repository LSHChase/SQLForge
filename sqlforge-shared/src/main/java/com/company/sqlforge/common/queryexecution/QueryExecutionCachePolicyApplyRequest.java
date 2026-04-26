package com.company.sqlforge.common.queryexecution;

public class QueryExecutionCachePolicyApplyRequest {

    private String tenantId;
    private String policyId;
    private String sqlFingerprint;
    private String datasourceType;
    private String schemaVersion;
    private String sourcePlanId;
    private String policyReason;
    private Integer maxEntries;
    private Long ttlSeconds;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }

    public String getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(String datasourceType) {
        this.datasourceType = datasourceType;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getSourcePlanId() {
        return sourcePlanId;
    }

    public void setSourcePlanId(String sourcePlanId) {
        this.sourcePlanId = sourcePlanId;
    }

    public String getPolicyReason() {
        return policyReason;
    }

    public void setPolicyReason(String policyReason) {
        this.policyReason = policyReason;
    }

    public Integer getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(Integer maxEntries) {
        this.maxEntries = maxEntries;
    }

    public Long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(Long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}
