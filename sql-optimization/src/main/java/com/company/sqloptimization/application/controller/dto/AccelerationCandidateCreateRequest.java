package com.company.sqloptimization.application.controller.dto;

import com.company.sqloptimization.domain.governance.CandidateStatus;
import com.company.sqloptimization.domain.governance.CandidateType;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import java.math.BigDecimal;
import java.util.Map;
import javax.validation.constraints.NotNull;

public class AccelerationCandidateCreateRequest {

    private String tenantId;

    @NotNull(message = "sourceType 为必填项")
    private GovernanceSourceType sourceType;

    @NotNull(message = "sourceKind 为必填项")
    private GovernanceSourceKind sourceKind;

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
    private CandidateType candidateType;
    private CandidateStatus status;
    private BigDecimal confidence;
    private Integer priority;

    @NotNull(message = "evidenceLevel 为必填项")
    private EvidenceLevel evidenceLevel;

    private String schemaVersion;
    private Map<String, Object> sourceEvidence;
    private Map<String, Object> issueEvidence;
    private Map<String, Object> runtimeEvidence;
    private Map<String, Object> benefitEstimate;
    private Map<String, Object> costEstimate;
    private Map<String, Object> risk;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public GovernanceSourceType getSourceType() { return sourceType; }
    public void setSourceType(GovernanceSourceType sourceType) { this.sourceType = sourceType; }
    public GovernanceSourceKind getSourceKind() { return sourceKind; }
    public void setSourceKind(GovernanceSourceKind sourceKind) { this.sourceKind = sourceKind; }
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
    public CandidateType getCandidateType() { return candidateType; }
    public void setCandidateType(CandidateType candidateType) { this.candidateType = candidateType; }
    public CandidateStatus getStatus() { return status; }
    public void setStatus(CandidateStatus status) { this.status = status; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public EvidenceLevel getEvidenceLevel() { return evidenceLevel; }
    public void setEvidenceLevel(EvidenceLevel evidenceLevel) { this.evidenceLevel = evidenceLevel; }
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
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
}
