package com.company.governance.application.controller.vo;

import java.time.Instant;
import java.util.List;

public class MetadataSnapshotVO {

    private String snapshotId;
    private String tenantId;
    private String datasourceCode;
    private String objectType;
    private String objectKey;
    private String objectName;
    private String catalogName;
    private String schemaName;
    private String freshnessStatus;
    private String slaStatus;
    private String queryabilityStatus;
    private String evidenceStatus;
    private Instant latestRefreshTime;
    private Instant expectedSlaTime;
    private Instant snapshotTime;
    private String evidenceSource;
    private Integer columnCount;
    private Integer partitionCount;
    private Long rowCount;
    private Long storageBytes;
    private String requestId;
    private String traceId;
    private String executionId;
    private String historyId;
    private String parseTaskId;
    private String reportCode;
    private String sqlFingerprint;
    private Integer upstreamCount;
    private Integer downstreamCount;
    private List<MetadataLineageVO> upstreamRefs;
    private List<MetadataLineageVO> downstreamRefs;
    private String contractStage;
    private String implementationStage;

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public void setDatasourceCode(String datasourceCode) {
        this.datasourceCode = datasourceCode;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getFreshnessStatus() {
        return freshnessStatus;
    }

    public void setFreshnessStatus(String freshnessStatus) {
        this.freshnessStatus = freshnessStatus;
    }

    public String getSlaStatus() {
        return slaStatus;
    }

    public void setSlaStatus(String slaStatus) {
        this.slaStatus = slaStatus;
    }

    public String getQueryabilityStatus() {
        return queryabilityStatus;
    }

    public void setQueryabilityStatus(String queryabilityStatus) {
        this.queryabilityStatus = queryabilityStatus;
    }

    public String getEvidenceStatus() {
        return evidenceStatus;
    }

    public void setEvidenceStatus(String evidenceStatus) {
        this.evidenceStatus = evidenceStatus;
    }

    public Instant getLatestRefreshTime() {
        return latestRefreshTime;
    }

    public void setLatestRefreshTime(Instant latestRefreshTime) {
        this.latestRefreshTime = latestRefreshTime;
    }

    public Instant getExpectedSlaTime() {
        return expectedSlaTime;
    }

    public void setExpectedSlaTime(Instant expectedSlaTime) {
        this.expectedSlaTime = expectedSlaTime;
    }

    public Instant getSnapshotTime() {
        return snapshotTime;
    }

    public void setSnapshotTime(Instant snapshotTime) {
        this.snapshotTime = snapshotTime;
    }

    public String getEvidenceSource() {
        return evidenceSource;
    }

    public void setEvidenceSource(String evidenceSource) {
        this.evidenceSource = evidenceSource;
    }

    public Integer getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(Integer columnCount) {
        this.columnCount = columnCount;
    }

    public Integer getPartitionCount() {
        return partitionCount;
    }

    public void setPartitionCount(Integer partitionCount) {
        this.partitionCount = partitionCount;
    }

    public Long getRowCount() {
        return rowCount;
    }

    public void setRowCount(Long rowCount) {
        this.rowCount = rowCount;
    }

    public Long getStorageBytes() {
        return storageBytes;
    }

    public void setStorageBytes(Long storageBytes) {
        this.storageBytes = storageBytes;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public String getParseTaskId() {
        return parseTaskId;
    }

    public void setParseTaskId(String parseTaskId) {
        this.parseTaskId = parseTaskId;
    }

    public String getReportCode() {
        return reportCode;
    }

    public void setReportCode(String reportCode) {
        this.reportCode = reportCode;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }

    public Integer getUpstreamCount() {
        return upstreamCount;
    }

    public void setUpstreamCount(Integer upstreamCount) {
        this.upstreamCount = upstreamCount;
    }

    public Integer getDownstreamCount() {
        return downstreamCount;
    }

    public void setDownstreamCount(Integer downstreamCount) {
        this.downstreamCount = downstreamCount;
    }

    public List<MetadataLineageVO> getUpstreamRefs() {
        return upstreamRefs;
    }

    public void setUpstreamRefs(List<MetadataLineageVO> upstreamRefs) {
        this.upstreamRefs = upstreamRefs;
    }

    public List<MetadataLineageVO> getDownstreamRefs() {
        return downstreamRefs;
    }

    public void setDownstreamRefs(List<MetadataLineageVO> downstreamRefs) {
        this.downstreamRefs = downstreamRefs;
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
