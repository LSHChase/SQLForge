package com.company.sqlforge.common.governance;

import java.util.List;

public class GovernanceBenchmarkReportTraceResponse {

    private String configSnapshotId;
    private String resultId;
    private String historyId;
    private String traceId;
    private String requestId;
    private String sagaId;
    private List<GovernanceBenchmarkArtifactTraceResponse> artifacts;
    private String contractStage;
    private String implementationStage;

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

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public List<GovernanceBenchmarkArtifactTraceResponse> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<GovernanceBenchmarkArtifactTraceResponse> artifacts) {
        this.artifacts = artifacts;
    }

    public String getContractStage() {
        return contractStage;
    }

    public void setContractStage(String contractStage) {
        this.contractStage = contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }

    public void setImplementationStage(String implementationStage) {
        this.implementationStage = implementationStage;
    }
}
