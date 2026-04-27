package com.company.sqlforge.common.audit;

/**
 * Minimum cross-service audit contract.
 */
public final class AuditEvent {

    private final String occurredAt;
    private final String tenantId;
    private final String userId;
    private final String serviceCode;
    private final String operationCode;
    private final String resourceType;
    private final String resourceId;
    private final String resultStatus;
    private final long elapsedMs;
    private final String traceId;
    private final String requestId;
    private final String accessChannel;
    private final String authSource;
    private final String sourceIp;
    private final String userAgent;

    public AuditEvent(String occurredAt,
                      String tenantId,
                      String userId,
                      String serviceCode,
                      String operationCode,
                      String resourceType,
                      String resourceId,
                      String resultStatus,
                      long elapsedMs,
                      String traceId,
                      String requestId,
                      String accessChannel,
                      String authSource,
                      String sourceIp,
                      String userAgent) {
        this.occurredAt = occurredAt;
        this.tenantId = tenantId;
        this.userId = userId;
        this.serviceCode = serviceCode;
        this.operationCode = operationCode;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.resultStatus = resultStatus;
        this.elapsedMs = elapsedMs;
        this.traceId = traceId;
        this.requestId = requestId;
        this.accessChannel = accessChannel;
        this.authSource = authSource;
        this.sourceIp = sourceIp;
        this.userAgent = userAgent;
    }

    public String getOccurredAt() {
        return occurredAt;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public String getServiceCode() {
        return serviceCode;
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

    public String getTraceId() {
        return traceId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getAccessChannel() {
        return accessChannel;
    }

    public String getAuthSource() {
        return authSource;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
