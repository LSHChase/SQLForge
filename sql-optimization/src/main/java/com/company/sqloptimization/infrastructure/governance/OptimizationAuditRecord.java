package com.company.sqloptimization.infrastructure.governance;

public class OptimizationAuditRecord {

    private final String operationCode;
    private final String resourceType;
    private final String resourceId;
    private final String resultStatus;
    private final long elapsedMs;
    private final String requestParams;
    private final String responseSummary;
    private final String sagaId;
    private final String configSnapshotId;
    private final String resultId;
    private final String historyId;

    public OptimizationAuditRecord(String operationCode,
                                   String resourceType,
                                   String resourceId,
                                   String resultStatus,
                                   long elapsedMs,
                                   String requestParams,
                                   String responseSummary) {
        this(
            operationCode,
            resourceType,
            resourceId,
            resultStatus,
            elapsedMs,
            requestParams,
            responseSummary,
            null,
            null,
            null,
            null
        );
    }

    public OptimizationAuditRecord(String operationCode,
                                   String resourceType,
                                   String resourceId,
                                   String resultStatus,
                                   long elapsedMs,
                                   String requestParams,
                                   String responseSummary,
                                   String sagaId,
                                   String configSnapshotId,
                                   String resultId,
                                   String historyId) {
        this.operationCode = operationCode;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.resultStatus = resultStatus;
        this.elapsedMs = elapsedMs;
        this.requestParams = requestParams;
        this.responseSummary = responseSummary;
        this.sagaId = sagaId;
        this.configSnapshotId = configSnapshotId;
        this.resultId = resultId;
        this.historyId = historyId;
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

    public String getRequestParams() {
        return requestParams;
    }

    public String getResponseSummary() {
        return responseSummary;
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
}
