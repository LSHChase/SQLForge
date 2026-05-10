package com.company.sqloptimization.application.controller.vo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class AccelerationCandidateVO {

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
    private Instant createdAt;
    private Instant updatedAt;
    private Map<String, Object> sourceEvidence;
    private Map<String, Object> issueEvidence;
    private Map<String, Object> runtimeEvidence;
    private Map<String, Object> benefitEstimate;
    private Map<String, Object> costEstimate;
    private Map<String, Object> risk;
    private String contractStage;
    private String implementationStage;

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
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Map<String, Object> getSourceEvidence() { return sourceEvidence; }
    public void setSourceEvidence(Map<String, Object> sourceEvidence) { this.sourceEvidence = sourceEvidence; }
    public Map<String, Object> getIssueEvidence() { return issueEvidence; }
    public void setIssueEvidence(Map<String, Object> issueEvidence) { this.issueEvidence = issueEvidence; }
    public Map<String, Object> getRuntimeEvidence() { return runtimeEvidence; }
    public void setRuntimeEvidence(Map<String, Object> runtimeEvidence) { this.runtimeEvidence = runtimeEvidence; }
    public Map<String, Object> getBenefitEstimate() { return benefitEstimate; }
    public void setBenefitEstimate(Map<String, Object> benefitEstimate) { this.benefitEstimate = benefitEstimate; }
    public Map<String, Object> getCostEstimate() { return costEstimate; }
    public void setCostEstimate(Map<String, Object> costEstimate) { this.costEstimate = costEstimate; }
    public Map<String, Object> getRisk() { return risk; }
    public void setRisk(Map<String, Object> risk) { this.risk = risk; }
    public String getContractStage() { return contractStage; }
    public void setContractStage(String contractStage) { this.contractStage = contractStage; }
    public String getImplementationStage() { return implementationStage; }
    public void setImplementationStage(String implementationStage) { this.implementationStage = implementationStage; }
}
