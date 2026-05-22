package com.company.sqlforge.common.queryexecution;

public class RuntimeRewriteBindingActivationRequest {

    private String tenantId;
    private String rewriteRecordId;
    private String recommendationId;
    private String sourceType;
    private String sourceKind;
    private String sourceId;
    private String sqlFingerprint;
    private String originalSqlDigest;
    private String originalSqlText;
    private String recommendedSqlText;
    private String rewriteProgramJson;
    private String templateFamilyFingerprint;
    private String datasourceCode;
    private String activatedBy;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
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
    public String getOriginalSqlText() { return originalSqlText; }
    public void setOriginalSqlText(String originalSqlText) { this.originalSqlText = originalSqlText; }
    public String getRecommendedSqlText() { return recommendedSqlText; }
    public void setRecommendedSqlText(String recommendedSqlText) { this.recommendedSqlText = recommendedSqlText; }
    public String getRewriteProgramJson() { return rewriteProgramJson; }
    public void setRewriteProgramJson(String rewriteProgramJson) { this.rewriteProgramJson = rewriteProgramJson; }
    public String getTemplateFamilyFingerprint() { return templateFamilyFingerprint; }
    public void setTemplateFamilyFingerprint(String templateFamilyFingerprint) { this.templateFamilyFingerprint = templateFamilyFingerprint; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getActivatedBy() { return activatedBy; }
    public void setActivatedBy(String activatedBy) { this.activatedBy = activatedBy; }
}
