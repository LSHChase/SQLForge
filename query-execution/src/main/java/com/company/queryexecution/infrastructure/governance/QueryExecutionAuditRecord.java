package com.company.queryexecution.infrastructure.governance;

public class QueryExecutionAuditRecord {

    private final String operationCode;
    private final String resourceType;
    private final String resourceId;
    private final String resultStatus;
    private final long elapsedMs;
    private final String requestParams;
    private final String responseSummary;

    public QueryExecutionAuditRecord(String operationCode,
                                     String resourceType,
                                     String resourceId,
                                     String resultStatus,
                                     long elapsedMs,
                                     String requestParams,
                                     String responseSummary) {
        this.operationCode = operationCode;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.resultStatus = resultStatus;
        this.elapsedMs = elapsedMs;
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

    public String getRequestParams() {
        return requestParams;
    }

    public String getResponseSummary() {
        return responseSummary;
    }
}
