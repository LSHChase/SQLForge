package com.company.sqlforge.common.governance;

public class GovernanceSqlRewriteDivergenceAlertRequest {

    private String tenantId;
    private String sourceType;
    private String sourceKind;
    private String sourceId;
    private String evidenceLevel;
    private String historyId;
    private String parseHistoryId;
    private String recommendationId;
    private String rewriteRecordId;
    private String validationRunId;
    private String planId;
    private String sqlFingerprint;
    private String comparisonStatus;
    private String differenceType;
    private String sampleEvidenceJson;
    private Boolean autoApplyPaused;
    private String summary;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceKind() {
        return sourceKind;
    }

    public void setSourceKind(String sourceKind) {
        this.sourceKind = sourceKind;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getEvidenceLevel() {
        return evidenceLevel;
    }

    public void setEvidenceLevel(String evidenceLevel) {
        this.evidenceLevel = evidenceLevel;
    }

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public String getParseHistoryId() {
        return parseHistoryId;
    }

    public void setParseHistoryId(String parseHistoryId) {
        this.parseHistoryId = parseHistoryId;
    }

    public String getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(String recommendationId) {
        this.recommendationId = recommendationId;
    }

    public String getRewriteRecordId() {
        return rewriteRecordId;
    }

    public void setRewriteRecordId(String rewriteRecordId) {
        this.rewriteRecordId = rewriteRecordId;
    }

    public String getValidationRunId() {
        return validationRunId;
    }

    public void setValidationRunId(String validationRunId) {
        this.validationRunId = validationRunId;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }

    public String getComparisonStatus() {
        return comparisonStatus;
    }

    public void setComparisonStatus(String comparisonStatus) {
        this.comparisonStatus = comparisonStatus;
    }

    public String getDifferenceType() {
        return differenceType;
    }

    public void setDifferenceType(String differenceType) {
        this.differenceType = differenceType;
    }

    public String getSampleEvidenceJson() {
        return sampleEvidenceJson;
    }

    public void setSampleEvidenceJson(String sampleEvidenceJson) {
        this.sampleEvidenceJson = sampleEvidenceJson;
    }

    public Boolean getAutoApplyPaused() {
        return autoApplyPaused;
    }

    public void setAutoApplyPaused(Boolean autoApplyPaused) {
        this.autoApplyPaused = autoApplyPaused;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
