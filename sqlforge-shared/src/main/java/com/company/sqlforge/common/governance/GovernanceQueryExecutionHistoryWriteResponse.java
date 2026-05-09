package com.company.sqlforge.common.governance;

public class GovernanceQueryExecutionHistoryWriteResponse {

    private String configSnapshotId;
    private String resultId;
    private String historyId;
    private Long auditId;
    private String traceId;
    private String requestId;
    private String sagaId;
    private String contractStage;
    private String implementationStage;

    public String getConfigSnapshotId() { return configSnapshotId; }
    public void setConfigSnapshotId(String configSnapshotId) { this.configSnapshotId = configSnapshotId; }
    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    public Long getAuditId() { return auditId; }
    public void setAuditId(Long auditId) { this.auditId = auditId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getSagaId() { return sagaId; }
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }
    public String getContractStage() { return contractStage; }
    public void setContractStage(String contractStage) { this.contractStage = contractStage; }
    public String getImplementationStage() { return implementationStage; }
    public void setImplementationStage(String implementationStage) { this.implementationStage = implementationStage; }
}
