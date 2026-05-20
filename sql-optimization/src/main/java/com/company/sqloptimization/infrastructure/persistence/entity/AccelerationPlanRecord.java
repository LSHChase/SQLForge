package com.company.sqloptimization.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class AccelerationPlanRecord {

    private String planId;
    private String tenantId;
    private String sourceTaskId;
    private String sqlText;
    private String sqlFingerprint;
    private String datasourceType;
    private String selectedSuggestionTypesJson;
    private String planStatus;
    private String planSummary;
    private String primaryRecommendation;
    private String planPayloadJson;
    private String benefitsJson;
    private String costsJson;
    private String risksJson;
    private String configSnapshotId;
    private String resultId;
    private String historyId;
    private Integer lastErrorCode;
    private String lastErrorMessage;
    private String activationEvidenceJson;
    private LocalDateTime activatedAt;
    private String activatedBy;
    private String pauseEvidenceJson;
    private LocalDateTime pausedAt;
    private String pausedBy;
    private String statusHistoryJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSourceTaskId() { return sourceTaskId; }
    public void setSourceTaskId(String sourceTaskId) { this.sourceTaskId = sourceTaskId; }
    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getDatasourceType() { return datasourceType; }
    public void setDatasourceType(String datasourceType) { this.datasourceType = datasourceType; }
    public String getSelectedSuggestionTypesJson() { return selectedSuggestionTypesJson; }
    public void setSelectedSuggestionTypesJson(String selectedSuggestionTypesJson) { this.selectedSuggestionTypesJson = selectedSuggestionTypesJson; }
    public String getPlanStatus() { return planStatus; }
    public void setPlanStatus(String planStatus) { this.planStatus = planStatus; }
    public String getPlanSummary() { return planSummary; }
    public void setPlanSummary(String planSummary) { this.planSummary = planSummary; }
    public String getPrimaryRecommendation() { return primaryRecommendation; }
    public void setPrimaryRecommendation(String primaryRecommendation) { this.primaryRecommendation = primaryRecommendation; }
    public String getPlanPayloadJson() { return planPayloadJson; }
    public void setPlanPayloadJson(String planPayloadJson) { this.planPayloadJson = planPayloadJson; }
    public String getBenefitsJson() { return benefitsJson; }
    public void setBenefitsJson(String benefitsJson) { this.benefitsJson = benefitsJson; }
    public String getCostsJson() { return costsJson; }
    public void setCostsJson(String costsJson) { this.costsJson = costsJson; }
    public String getRisksJson() { return risksJson; }
    public void setRisksJson(String risksJson) { this.risksJson = risksJson; }
    public String getConfigSnapshotId() { return configSnapshotId; }
    public void setConfigSnapshotId(String configSnapshotId) { this.configSnapshotId = configSnapshotId; }
    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public Integer getLastErrorCode() { return lastErrorCode; }
    public void setLastErrorCode(Integer lastErrorCode) { this.lastErrorCode = lastErrorCode; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
    public String getActivationEvidenceJson() { return activationEvidenceJson; }
    public void setActivationEvidenceJson(String activationEvidenceJson) { this.activationEvidenceJson = activationEvidenceJson; }
    public LocalDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(LocalDateTime activatedAt) { this.activatedAt = activatedAt; }
    public String getActivatedBy() { return activatedBy; }
    public void setActivatedBy(String activatedBy) { this.activatedBy = activatedBy; }
    public String getPauseEvidenceJson() { return pauseEvidenceJson; }
    public void setPauseEvidenceJson(String pauseEvidenceJson) { this.pauseEvidenceJson = pauseEvidenceJson; }
    public LocalDateTime getPausedAt() { return pausedAt; }
    public void setPausedAt(LocalDateTime pausedAt) { this.pausedAt = pausedAt; }
    public String getPausedBy() { return pausedBy; }
    public void setPausedBy(String pausedBy) { this.pausedBy = pausedBy; }
    public String getStatusHistoryJson() { return statusHistoryJson; }
    public void setStatusHistoryJson(String statusHistoryJson) { this.statusHistoryJson = statusHistoryJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
