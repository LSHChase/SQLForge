package com.company.sqloptimization.domain.recommendation;

import java.util.List;

public class AccelerationRecommendationFilter {

    private String tenantId;
    private String recommendationType;
    private String status;
    private String benefitLevel;
    private String riskLevel;
    private String validationStatus;
    private Boolean requiresDispatch;
    private Boolean manualReviewRequired;
    private String sourceType;
    private String sourceKind;
    private List<String> sourceKinds;
    private String sourceId;
    private String historyId;
    private String parseTaskId;
    private String batchId;
    private String reportCode;
    private String orderByClause;
    private int offset;
    private int limit;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBenefitLevel() { return benefitLevel; }
    public void setBenefitLevel(String benefitLevel) { this.benefitLevel = benefitLevel; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public Boolean getRequiresDispatch() { return requiresDispatch; }
    public void setRequiresDispatch(Boolean requiresDispatch) { this.requiresDispatch = requiresDispatch; }
    public Boolean getManualReviewRequired() { return manualReviewRequired; }
    public void setManualReviewRequired(Boolean manualReviewRequired) { this.manualReviewRequired = manualReviewRequired; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceKind() { return sourceKind; }
    public void setSourceKind(String sourceKind) { this.sourceKind = sourceKind; }
    public List<String> getSourceKinds() { return sourceKinds; }
    public void setSourceKinds(List<String> sourceKinds) { this.sourceKinds = sourceKinds; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getParseTaskId() { return parseTaskId; }
    public void setParseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getOrderByClause() { return orderByClause; }
    public void setOrderByClause(String orderByClause) { this.orderByClause = orderByClause; }
    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
