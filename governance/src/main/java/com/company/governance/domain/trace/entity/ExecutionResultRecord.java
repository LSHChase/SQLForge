package com.company.governance.domain.trace.entity;

import java.time.LocalDateTime;

public class ExecutionResultRecord {

    private String resultId;
    private String configSnapshotId;
    private String tenantId;
    private String serviceCode;
    private String taskId;
    private String taskType;
    private String resultStatus;
    private String traceId;
    private String requestId;
    private String sagaId;
    private String accessChannel;
    private String targetEngine;
    private Long returnedRowCount;
    private Boolean cacheHit;
    private Boolean rewriteApplied;
    private Boolean accelerationApplied;
    private String hitTableSummary;
    private String routeSummary;
    private String cacheSummary;
    private String resultSummary;
    private String resultPayload;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createTime;

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getConfigSnapshotId() {
        return configSnapshotId;
    }

    public void setConfigSnapshotId(String configSnapshotId) {
        this.configSnapshotId = configSnapshotId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
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

    public String getAccessChannel() {
        return accessChannel;
    }

    public void setAccessChannel(String accessChannel) {
        this.accessChannel = accessChannel;
    }

    public String getTargetEngine() {
        return targetEngine;
    }

    public void setTargetEngine(String targetEngine) {
        this.targetEngine = targetEngine;
    }

    public Long getReturnedRowCount() {
        return returnedRowCount;
    }

    public void setReturnedRowCount(Long returnedRowCount) {
        this.returnedRowCount = returnedRowCount;
    }

    public Boolean getCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(Boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    public Boolean getRewriteApplied() {
        return rewriteApplied;
    }

    public void setRewriteApplied(Boolean rewriteApplied) {
        this.rewriteApplied = rewriteApplied;
    }

    public Boolean getAccelerationApplied() {
        return accelerationApplied;
    }

    public void setAccelerationApplied(Boolean accelerationApplied) {
        this.accelerationApplied = accelerationApplied;
    }

    public String getHitTableSummary() {
        return hitTableSummary;
    }

    public void setHitTableSummary(String hitTableSummary) {
        this.hitTableSummary = hitTableSummary;
    }

    public String getRouteSummary() {
        return routeSummary;
    }

    public void setRouteSummary(String routeSummary) {
        this.routeSummary = routeSummary;
    }

    public String getCacheSummary() {
        return cacheSummary;
    }

    public void setCacheSummary(String cacheSummary) {
        this.cacheSummary = cacheSummary;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public String getResultPayload() {
        return resultPayload;
    }

    public void setResultPayload(String resultPayload) {
        this.resultPayload = resultPayload;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
