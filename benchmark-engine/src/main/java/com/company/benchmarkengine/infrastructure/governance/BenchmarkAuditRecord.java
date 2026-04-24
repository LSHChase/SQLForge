package com.company.benchmarkengine.infrastructure.governance;

public class BenchmarkAuditRecord {

    private final String operationCode;
    private final String resourceType;
    private final String resourceId;
    private final String resultStatus;
    private final long elapsedMs;
    private final String sagaId;
    private final String configSnapshotId;
    private final String resultId;
    private final String historyId;
    private final String exportId;
    private final String requestParams;
    private final String responseSummary;

    public BenchmarkAuditRecord(String operationCode,
                                String resourceType,
                                String resourceId,
                                String resultStatus,
                                long elapsedMs,
                                String sagaId,
                                String configSnapshotId,
                                String resultId,
                                String historyId,
                                String exportId,
                                String requestParams,
                                String responseSummary) {
        this.operationCode = operationCode;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.resultStatus = resultStatus;
        this.elapsedMs = elapsedMs;
        this.sagaId = sagaId;
        this.configSnapshotId = configSnapshotId;
        this.resultId = resultId;
        this.historyId = historyId;
        this.exportId = exportId;
        this.requestParams = requestParams;
        this.responseSummary = responseSummary;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public String getSagaId() {
        return sagaId;
    }

    public String getConfigSnapshotId() {
        return configSnapshotId;
    }

    public String getResultId() {
        return resultId;
    }

    public String getHistoryId() {
        return historyId;
    }

    public String getExportId() {
        return exportId;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public String getResponseSummary() {
        return responseSummary;
    }
}
