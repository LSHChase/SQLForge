package com.company.governance.domain.trace.entity;

import java.time.LocalDateTime;

public class ConfigSnapshotRecord {

    private String configSnapshotId;
    private String tenantId;
    private String serviceCode;
    private String sourceConfigType;
    private String sourceConfigId;
    private String sourceVersion;
    private String snapshotStatus;
    private String snapshotReason;
    private String traceId;
    private String requestId;
    private String sagaId;
    private String snapshotPayload;
    private String createdBy;
    private LocalDateTime createTime;

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

    public String getSourceConfigType() {
        return sourceConfigType;
    }

    public void setSourceConfigType(String sourceConfigType) {
        this.sourceConfigType = sourceConfigType;
    }

    public String getSourceConfigId() {
        return sourceConfigId;
    }

    public void setSourceConfigId(String sourceConfigId) {
        this.sourceConfigId = sourceConfigId;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public void setSourceVersion(String sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    public String getSnapshotStatus() {
        return snapshotStatus;
    }

    public void setSnapshotStatus(String snapshotStatus) {
        this.snapshotStatus = snapshotStatus;
    }

    public String getSnapshotReason() {
        return snapshotReason;
    }

    public void setSnapshotReason(String snapshotReason) {
        this.snapshotReason = snapshotReason;
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

    public String getSnapshotPayload() {
        return snapshotPayload;
    }

    public void setSnapshotPayload(String snapshotPayload) {
        this.snapshotPayload = snapshotPayload;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
