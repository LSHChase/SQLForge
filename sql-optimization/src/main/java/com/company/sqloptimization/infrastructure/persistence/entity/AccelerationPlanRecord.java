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
    private String reviewNote;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String rejectedBy;
    private LocalDateTime rejectedAt;
    private Integer lastErrorCode;
    private String lastErrorMessage;
    private String runtimeBindingJson;
    private LocalDateTime runtimeBindingAt;
    private String runtimeBindingBy;
    private String verificationEvidenceJson;
    private LocalDateTime verifiedAt;
    private String verifiedBy;
    private String rollbackEvidenceJson;
    private LocalDateTime rolledBackAt;
    private String rolledBackBy;
    private String statusHistoryJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSourceTaskId() {
        return sourceTaskId;
    }

    public void setSourceTaskId(String sourceTaskId) {
        this.sourceTaskId = sourceTaskId;
    }

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
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

    public String getSelectedSuggestionTypesJson() {
        return selectedSuggestionTypesJson;
    }

    public void setSelectedSuggestionTypesJson(String selectedSuggestionTypesJson) {
        this.selectedSuggestionTypesJson = selectedSuggestionTypesJson;
    }

    public String getPlanStatus() {
        return planStatus;
    }

    public void setPlanStatus(String planStatus) {
        this.planStatus = planStatus;
    }

    public String getPlanSummary() {
        return planSummary;
    }

    public void setPlanSummary(String planSummary) {
        this.planSummary = planSummary;
    }

    public String getPrimaryRecommendation() {
        return primaryRecommendation;
    }

    public void setPrimaryRecommendation(String primaryRecommendation) {
        this.primaryRecommendation = primaryRecommendation;
    }

    public String getPlanPayloadJson() {
        return planPayloadJson;
    }

    public void setPlanPayloadJson(String planPayloadJson) {
        this.planPayloadJson = planPayloadJson;
    }

    public String getBenefitsJson() {
        return benefitsJson;
    }

    public void setBenefitsJson(String benefitsJson) {
        this.benefitsJson = benefitsJson;
    }

    public String getCostsJson() {
        return costsJson;
    }

    public void setCostsJson(String costsJson) {
        this.costsJson = costsJson;
    }

    public String getRisksJson() {
        return risksJson;
    }

    public void setRisksJson(String risksJson) {
        this.risksJson = risksJson;
    }

    public String getConfigSnapshotId() {
        return configSnapshotId;
    }

    public void setConfigSnapshotId(String configSnapshotId) {
        this.configSnapshotId = configSnapshotId;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectedBy() {
        return rejectedBy;
    }

    public void setRejectedBy(String rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public Integer getLastErrorCode() {
        return lastErrorCode;
    }

    public void setLastErrorCode(Integer lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public String getRuntimeBindingJson() {
        return runtimeBindingJson;
    }

    public void setRuntimeBindingJson(String runtimeBindingJson) {
        this.runtimeBindingJson = runtimeBindingJson;
    }

    public LocalDateTime getRuntimeBindingAt() {
        return runtimeBindingAt;
    }

    public void setRuntimeBindingAt(LocalDateTime runtimeBindingAt) {
        this.runtimeBindingAt = runtimeBindingAt;
    }

    public String getRuntimeBindingBy() {
        return runtimeBindingBy;
    }

    public void setRuntimeBindingBy(String runtimeBindingBy) {
        this.runtimeBindingBy = runtimeBindingBy;
    }

    public String getVerificationEvidenceJson() {
        return verificationEvidenceJson;
    }

    public void setVerificationEvidenceJson(String verificationEvidenceJson) {
        this.verificationEvidenceJson = verificationEvidenceJson;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getRollbackEvidenceJson() {
        return rollbackEvidenceJson;
    }

    public void setRollbackEvidenceJson(String rollbackEvidenceJson) {
        this.rollbackEvidenceJson = rollbackEvidenceJson;
    }

    public LocalDateTime getRolledBackAt() {
        return rolledBackAt;
    }

    public void setRolledBackAt(LocalDateTime rolledBackAt) {
        this.rolledBackAt = rolledBackAt;
    }

    public String getRolledBackBy() {
        return rolledBackBy;
    }

    public void setRolledBackBy(String rolledBackBy) {
        this.rolledBackBy = rolledBackBy;
    }

    public String getStatusHistoryJson() {
        return statusHistoryJson;
    }

    public void setStatusHistoryJson(String statusHistoryJson) {
        this.statusHistoryJson = statusHistoryJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
