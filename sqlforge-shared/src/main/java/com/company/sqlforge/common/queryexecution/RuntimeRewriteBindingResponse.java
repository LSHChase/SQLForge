package com.company.sqlforge.common.queryexecution;

public class RuntimeRewriteBindingResponse {

    private String tenantId;
    private String runtimeBindingId;
    private String rewriteRecordId;
    private String recommendationId;
    private String sourceType;
    private String sourceKind;
    private String sourceId;
    private String sqlFingerprint;
    private String originalSqlDigest;
    private String recommendedSqlText;
    private String datasourceCode;
    private String status;
    private boolean active;
    private Long ruleVersion;
    private String runtimeRuleVersion;
    private String runtimeSummary;
    private String runtimeDetailsJson;
    private String contractStage;
    private String implementationStage;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getRuntimeBindingId() { return runtimeBindingId; }
    public void setRuntimeBindingId(String runtimeBindingId) { this.runtimeBindingId = runtimeBindingId; }
    public String getRewriteRecordId() { return rewriteRecordId; }
    public void setRewriteRecordId(String rewriteRecordId) { this.rewriteRecordId = rewriteRecordId; }
    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceKind() { return sourceKind; }
    public void setSourceKind(String sourceKind) { this.sourceKind = sourceKind; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getOriginalSqlDigest() { return originalSqlDigest; }
    public void setOriginalSqlDigest(String originalSqlDigest) { this.originalSqlDigest = originalSqlDigest; }
    public String getRecommendedSqlText() { return recommendedSqlText; }
    public void setRecommendedSqlText(String recommendedSqlText) { this.recommendedSqlText = recommendedSqlText; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Long getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(Long ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getRuntimeRuleVersion() { return runtimeRuleVersion; }
    public void setRuntimeRuleVersion(String runtimeRuleVersion) { this.runtimeRuleVersion = runtimeRuleVersion; }
    public String getRuntimeSummary() { return runtimeSummary; }
    public void setRuntimeSummary(String runtimeSummary) { this.runtimeSummary = runtimeSummary; }
    public String getRuntimeDetailsJson() { return runtimeDetailsJson; }
    public void setRuntimeDetailsJson(String runtimeDetailsJson) { this.runtimeDetailsJson = runtimeDetailsJson; }
    public String getContractStage() { return contractStage; }
    public void setContractStage(String contractStage) { this.contractStage = contractStage; }
    public String getImplementationStage() { return implementationStage; }
    public void setImplementationStage(String implementationStage) { this.implementationStage = implementationStage; }
}
