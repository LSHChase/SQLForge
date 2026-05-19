package com.company.sqloptimization.application.controller.vo;

import java.util.List;
import java.util.Map;

public class RecommendationDiffVO {

    private String recommendationId;
    private String tenantId;
    private String sourceType;
    private String sourceKind;
    private String sourceId;
    private String evidenceLevel;
    private String sqlFingerprint;
    private String originalSql;
    private String recommendedSql;
    private List<Map<String, Object>> textDiff;
    private List<Map<String, Object>> ruleDiff;
    private Map<String, Object> accelerationArtifact;
    private Map<String, Object> astSummaryDiff;
    private Map<String, Object> diffSummary;
    private String diffStatus;
    private String contractStage;
    private String implementationStage;

    public String getRecommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceKind() { return sourceKind; }
    public void setSourceKind(String sourceKind) { this.sourceKind = sourceKind; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getEvidenceLevel() { return evidenceLevel; }
    public void setEvidenceLevel(String evidenceLevel) { this.evidenceLevel = evidenceLevel; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getOriginalSql() { return originalSql; }
    public void setOriginalSql(String originalSql) { this.originalSql = originalSql; }
    public String getRecommendedSql() { return recommendedSql; }
    public void setRecommendedSql(String recommendedSql) { this.recommendedSql = recommendedSql; }
    public List<Map<String, Object>> getTextDiff() { return textDiff; }
    public void setTextDiff(List<Map<String, Object>> textDiff) { this.textDiff = textDiff; }
    public List<Map<String, Object>> getRuleDiff() { return ruleDiff; }
    public void setRuleDiff(List<Map<String, Object>> ruleDiff) { this.ruleDiff = ruleDiff; }
    public Map<String, Object> getAccelerationArtifact() { return accelerationArtifact; }
    public void setAccelerationArtifact(Map<String, Object> accelerationArtifact) { this.accelerationArtifact = accelerationArtifact; }
    public Map<String, Object> getAstSummaryDiff() { return astSummaryDiff; }
    public void setAstSummaryDiff(Map<String, Object> astSummaryDiff) { this.astSummaryDiff = astSummaryDiff; }
    public Map<String, Object> getDiffSummary() { return diffSummary; }
    public void setDiffSummary(Map<String, Object> diffSummary) { this.diffSummary = diffSummary; }
    public String getDiffStatus() { return diffStatus; }
    public void setDiffStatus(String diffStatus) { this.diffStatus = diffStatus; }
    public String getContractStage() { return contractStage; }
    public void setContractStage(String contractStage) { this.contractStage = contractStage; }
    public String getImplementationStage() { return implementationStage; }
    public void setImplementationStage(String implementationStage) { this.implementationStage = implementationStage; }
}
