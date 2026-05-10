package com.company.sqloptimization.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccelerationCandidateRecord {

    private String candidateId;
    private String tenantId;
    private String sourceType;
    private String sourceKind;
    private String sourceId;
    private String historyId;
    private String parseHistoryId;
    private String parseTaskId;
    private String batchId;
    private String batchItemId;
    private String benchmarkTaskId;
    private String optimizationTaskId;
    private String sqlFingerprint;
    private String datasourceCode;
    private String stage;
    private String reportCode;
    private String candidateType;
    private String status;
    private BigDecimal confidence;
    private Integer priority;
    private String evidenceLevel;
    private String schemaVersion;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String sourceEvidenceJson;
    private String issueEvidenceJson;
    private String runtimeEvidenceJson;
    private String benefitEstimateJson;
    private String costEstimateJson;
    private String riskJson;

    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceKind() { return sourceKind; }
    public void setSourceKind(String sourceKind) { this.sourceKind = sourceKind; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public String getParseHistoryId() { return parseHistoryId; }
    public void setParseHistoryId(String parseHistoryId) { this.parseHistoryId = parseHistoryId; }
    public String getParseTaskId() { return parseTaskId; }
    public void setParseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getBatchItemId() { return batchItemId; }
    public void setBatchItemId(String batchItemId) { this.batchItemId = batchItemId; }
    public String getBenchmarkTaskId() { return benchmarkTaskId; }
    public void setBenchmarkTaskId(String benchmarkTaskId) { this.benchmarkTaskId = benchmarkTaskId; }
    public String getOptimizationTaskId() { return optimizationTaskId; }
    public void setOptimizationTaskId(String optimizationTaskId) { this.optimizationTaskId = optimizationTaskId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getCandidateType() { return candidateType; }
    public void setCandidateType(String candidateType) { this.candidateType = candidateType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getEvidenceLevel() { return evidenceLevel; }
    public void setEvidenceLevel(String evidenceLevel) { this.evidenceLevel = evidenceLevel; }
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getSourceEvidenceJson() { return sourceEvidenceJson; }
    public void setSourceEvidenceJson(String sourceEvidenceJson) { this.sourceEvidenceJson = sourceEvidenceJson; }
    public String getIssueEvidenceJson() { return issueEvidenceJson; }
    public void setIssueEvidenceJson(String issueEvidenceJson) { this.issueEvidenceJson = issueEvidenceJson; }
    public String getRuntimeEvidenceJson() { return runtimeEvidenceJson; }
    public void setRuntimeEvidenceJson(String runtimeEvidenceJson) { this.runtimeEvidenceJson = runtimeEvidenceJson; }
    public String getBenefitEstimateJson() { return benefitEstimateJson; }
    public void setBenefitEstimateJson(String benefitEstimateJson) { this.benefitEstimateJson = benefitEstimateJson; }
    public String getCostEstimateJson() { return costEstimateJson; }
    public void setCostEstimateJson(String costEstimateJson) { this.costEstimateJson = costEstimateJson; }
    public String getRiskJson() { return riskJson; }
    public void setRiskJson(String riskJson) { this.riskJson = riskJson; }
}
