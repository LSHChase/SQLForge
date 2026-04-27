package com.company.sqloptimization.application.controller.dto;

import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.BenefitLevel;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationStatus;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationType;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RiskLevel;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class AccelerationRecommendationCreateRequest {

    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotNull(message = "recommendationType is required")
    private RecommendationType recommendationType;

    private String sourceSqlId;
    private String historyId;
    private String parseTaskId;
    private String batchId;
    private String routeDecisionId;
    private String alertId;
    private String sqlFingerprint;
    private String sourceSqlText;

    @NotBlank(message = "recommendedSqlText is required")
    private String recommendedSqlText;

    private String targetEngine;
    private String targetDatasource;
    private String reportCode;
    private String logicalObjectKey;
    private String summary;
    private String reason;
    private String expectedGain;
    private BenefitLevel benefitLevel;
    private RiskLevel riskLevel;
    private String riskSummary;
    private Boolean requiresDispatch;
    private RecommendationStatus status;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public RecommendationType getRecommendationType() { return recommendationType; }
    public void setRecommendationType(RecommendationType recommendationType) { this.recommendationType = recommendationType; }
    public String getSourceSqlId() { return sourceSqlId; }
    public void setSourceSqlId(String sourceSqlId) { this.sourceSqlId = sourceSqlId; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getParseTaskId() { return parseTaskId; }
    public void setParseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getRouteDecisionId() { return routeDecisionId; }
    public void setRouteDecisionId(String routeDecisionId) { this.routeDecisionId = routeDecisionId; }
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getSourceSqlText() { return sourceSqlText; }
    public void setSourceSqlText(String sourceSqlText) { this.sourceSqlText = sourceSqlText; }
    public String getRecommendedSqlText() { return recommendedSqlText; }
    public void setRecommendedSqlText(String recommendedSqlText) { this.recommendedSqlText = recommendedSqlText; }
    public String getTargetEngine() { return targetEngine; }
    public void setTargetEngine(String targetEngine) { this.targetEngine = targetEngine; }
    public String getTargetDatasource() { return targetDatasource; }
    public void setTargetDatasource(String targetDatasource) { this.targetDatasource = targetDatasource; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getLogicalObjectKey() { return logicalObjectKey; }
    public void setLogicalObjectKey(String logicalObjectKey) { this.logicalObjectKey = logicalObjectKey; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getExpectedGain() { return expectedGain; }
    public void setExpectedGain(String expectedGain) { this.expectedGain = expectedGain; }
    public BenefitLevel getBenefitLevel() { return benefitLevel; }
    public void setBenefitLevel(BenefitLevel benefitLevel) { this.benefitLevel = benefitLevel; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public String getRiskSummary() { return riskSummary; }
    public void setRiskSummary(String riskSummary) { this.riskSummary = riskSummary; }
    public Boolean getRequiresDispatch() { return requiresDispatch; }
    public void setRequiresDispatch(Boolean requiresDispatch) { this.requiresDispatch = requiresDispatch; }
    public RecommendationStatus getStatus() { return status; }
    public void setStatus(RecommendationStatus status) { this.status = status; }
}
