package com.company.sqlforge.common.queryexecution;

import java.util.List;
import java.util.Map;

public class QueryExecutionResultDigestResponse {

    private String tenantId;
    private String validationRunId;
    private String rewriteRecordId;
    private String sqlFingerprint;
    private String status;
    private String targetEngine;
    private Map<String, Object> resultDigest;
    private List<Map<String, Object>> limitedSample;
    private Map<String, Object> executionEvidence;
    private String errorCode;
    private String errorMessage;
    private String contractStage;
    private String implementationStage;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getValidationRunId() { return validationRunId; }
    public void setValidationRunId(String validationRunId) { this.validationRunId = validationRunId; }
    public String getRewriteRecordId() { return rewriteRecordId; }
    public void setRewriteRecordId(String rewriteRecordId) { this.rewriteRecordId = rewriteRecordId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTargetEngine() { return targetEngine; }
    public void setTargetEngine(String targetEngine) { this.targetEngine = targetEngine; }
    public Map<String, Object> getResultDigest() { return resultDigest; }
    public void setResultDigest(Map<String, Object> resultDigest) { this.resultDigest = resultDigest; }
    public List<Map<String, Object>> getLimitedSample() { return limitedSample; }
    public void setLimitedSample(List<Map<String, Object>> limitedSample) { this.limitedSample = limitedSample; }
    public Map<String, Object> getExecutionEvidence() { return executionEvidence; }
    public void setExecutionEvidence(Map<String, Object> executionEvidence) { this.executionEvidence = executionEvidence; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getContractStage() { return contractStage; }
    public void setContractStage(String contractStage) { this.contractStage = contractStage; }
    public String getImplementationStage() { return implementationStage; }
    public void setImplementationStage(String implementationStage) { this.implementationStage = implementationStage; }
}
