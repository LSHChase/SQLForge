package com.company.governance.application.controller.vo;

import java.time.LocalDateTime;
import java.util.Map;

public class GovernanceTraceSummaryBaseVO {

    private String traceId;
    private String requestId;
    private String serviceCode;
    private String operationType;
    private String resourceType;
    private String resourceId;
    private String latestStatus;
    private LocalDateTime lastSeenAt;
    private Integer auditEventCount;
    private Integer nonSuccessEventCount;
    private Integer queryHistoryCount;
    private Integer exportRecordCount;
    private String taskId;
    private String reportId;
    private String sqlFingerprint;
    private String errorCode;
    private String targetEngine;
    private Boolean degraded;
    private Map<String, Object> compensationReplayEvidence;
    private Map<String, Object> cacheGovernanceSurface;
    private Map<String, Object> artifactStorageContract;
    private Map<String, Object> artifactRecoverySurface;
    private Map<String, Object> artifactOperationSurface;

    public GovernanceTraceSummaryBaseVO() {
    }

    public GovernanceTraceSummaryBaseVO(String traceId,
                                    String requestId,
                                    String serviceCode,
                                    String operationType,
                                    String resourceType,
                                    String resourceId,
                                    String latestStatus,
                                    LocalDateTime lastSeenAt,
                                    Integer auditEventCount,
                                    Integer nonSuccessEventCount,
                                    Integer queryHistoryCount,
                                    Integer exportRecordCount,
                                    String taskId,
                                    String reportId,
                                    String sqlFingerprint,
                                    String errorCode,
                                    String targetEngine,
                                    Boolean degraded) {
        this(
            traceId,
            requestId,
            serviceCode,
            operationType,
            resourceType,
            resourceId,
            latestStatus,
            lastSeenAt,
            auditEventCount,
            nonSuccessEventCount,
            queryHistoryCount,
            exportRecordCount,
            taskId,
            reportId,
            sqlFingerprint,
            errorCode,
            targetEngine,
            degraded,
            null,
            null,
            null,
            null,
            null
        );
    }

    public GovernanceTraceSummaryBaseVO(String traceId,
                                    String requestId,
                                    String serviceCode,
                                    String operationType,
                                    String resourceType,
                                    String resourceId,
                                    String latestStatus,
                                    LocalDateTime lastSeenAt,
                                    Integer auditEventCount,
                                    Integer nonSuccessEventCount,
                                    Integer queryHistoryCount,
                                    Integer exportRecordCount,
                                    String taskId,
                                    String reportId,
                                    String sqlFingerprint,
                                    String errorCode,
                                    String targetEngine,
                                    Boolean degraded,
                                    Map<String, Object> compensationReplayEvidence,
                                    Map<String, Object> cacheGovernanceSurface,
                                    Map<String, Object> artifactStorageContract,
                                    Map<String, Object> artifactRecoverySurface,
                                    Map<String, Object> artifactOperationSurface) {
        this.traceId = traceId;
        this.requestId = requestId;
        this.serviceCode = serviceCode;
        this.operationType = operationType;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.latestStatus = latestStatus;
        this.lastSeenAt = lastSeenAt;
        this.auditEventCount = auditEventCount;
        this.nonSuccessEventCount = nonSuccessEventCount;
        this.queryHistoryCount = queryHistoryCount;
        this.exportRecordCount = exportRecordCount;
        this.taskId = taskId;
        this.reportId = reportId;
        this.sqlFingerprint = sqlFingerprint;
        this.errorCode = errorCode;
        this.targetEngine = targetEngine;
        this.degraded = degraded;
        this.compensationReplayEvidence = compensationReplayEvidence;
        this.cacheGovernanceSurface = cacheGovernanceSurface;
        this.artifactStorageContract = artifactStorageContract;
        this.artifactRecoverySurface = artifactRecoverySurface;
        this.artifactOperationSurface = artifactOperationSurface;
    }

    public String getTraceId() { return traceId; }

    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getRequestId() { return requestId; }

    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getServiceCode() { return serviceCode; }

    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }

    public String getOperationType() { return operationType; }

    public void setOperationType(String operationType) { this.operationType = operationType; }

    public String getResourceType() { return resourceType; }

    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceId() { return resourceId; }

    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getLatestStatus() { return latestStatus; }

    public void setLatestStatus(String latestStatus) { this.latestStatus = latestStatus; }

    public LocalDateTime getLastSeenAt() { return lastSeenAt; }

    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public Integer getAuditEventCount() { return auditEventCount; }

    public void setAuditEventCount(Integer auditEventCount) { this.auditEventCount = auditEventCount; }

    public Integer getNonSuccessEventCount() { return nonSuccessEventCount; }

    public void setNonSuccessEventCount(Integer nonSuccessEventCount) { this.nonSuccessEventCount = nonSuccessEventCount; }

    public Integer getQueryHistoryCount() { return queryHistoryCount; }

    public void setQueryHistoryCount(Integer queryHistoryCount) { this.queryHistoryCount = queryHistoryCount; }

    public Integer getExportRecordCount() { return exportRecordCount; }

    public void setExportRecordCount(Integer exportRecordCount) { this.exportRecordCount = exportRecordCount; }

    public String getTaskId() { return taskId; }

    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getReportId() { return reportId; }

    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getSqlFingerprint() { return sqlFingerprint; }

    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }

    public String getErrorCode() { return errorCode; }

    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getTargetEngine() { return targetEngine; }

    public void setTargetEngine(String targetEngine) { this.targetEngine = targetEngine; }

    public Boolean getDegraded() { return degraded; }

    public void setDegraded(Boolean degraded) { this.degraded = degraded; }

    public Map<String, Object> getCompensationReplayEvidence() { return compensationReplayEvidence; }

    public void setCompensationReplayEvidence(Map<String, Object> compensationReplayEvidence) { this.compensationReplayEvidence = compensationReplayEvidence; }

    public Map<String, Object> getCacheGovernanceSurface() { return cacheGovernanceSurface; }

    public void setCacheGovernanceSurface(Map<String, Object> cacheGovernanceSurface) { this.cacheGovernanceSurface = cacheGovernanceSurface; }

    public Map<String, Object> getArtifactStorageContract() { return artifactStorageContract; }

    public void setArtifactStorageContract(Map<String, Object> artifactStorageContract) { this.artifactStorageContract = artifactStorageContract; }

    public Map<String, Object> getArtifactRecoverySurface() { return artifactRecoverySurface; }

    public void setArtifactRecoverySurface(Map<String, Object> artifactRecoverySurface) { this.artifactRecoverySurface = artifactRecoverySurface; }

    public Map<String, Object> getArtifactOperationSurface() { return artifactOperationSurface; }

    public void setArtifactOperationSurface(Map<String, Object> artifactOperationSurface) { this.artifactOperationSurface = artifactOperationSurface; }
}
