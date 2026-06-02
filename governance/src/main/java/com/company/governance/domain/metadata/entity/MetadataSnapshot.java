package com.company.governance.domain.metadata.entity;

import java.time.Instant;
import java.util.List;

public class MetadataSnapshot {

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
    private List<MetadataLineageRef> upstreamRefs;
    private List<MetadataLineageRef> downstreamRefs;

    public MetadataSnapshot() {
    }

    public MetadataSnapshot(String snapshotId,
                            String tenantId,
                            String datasourceCode,
                            String objectType,
                            String objectKey,
                            String objectName,
                            String catalogName,
                            String schemaName,
                            String freshnessStatus,
                            String slaStatus,
                            String queryabilityStatus,
                            String evidenceStatus,
                            Instant latestRefreshTime,
                            Instant expectedSlaTime,
                            Instant snapshotTime,
                            String evidenceSource,
                            Integer columnCount,
                            Integer partitionCount,
                            Long rowCount,
                            Long storageBytes,
                            String requestId,
                            String traceId,
                            String executionId,
                            String historyId,
                            String parseTaskId,
                            String reportCode,
                            String sqlFingerprint,
                            List<MetadataLineageRef> upstreamRefs,
                            List<MetadataLineageRef> downstreamRefs) {
        this.snapshotId = snapshotId;
        this.tenantId = tenantId;
        this.datasourceCode = datasourceCode;
        this.objectType = objectType;
        this.objectKey = objectKey;
        this.objectName = objectName;
        this.catalogName = catalogName;
        this.schemaName = schemaName;
        this.freshnessStatus = freshnessStatus;
        this.slaStatus = slaStatus;
        this.queryabilityStatus = queryabilityStatus;
        this.evidenceStatus = evidenceStatus;
        this.latestRefreshTime = latestRefreshTime;
        this.expectedSlaTime = expectedSlaTime;
        this.snapshotTime = snapshotTime;
        this.evidenceSource = evidenceSource;
        this.columnCount = columnCount;
        this.partitionCount = partitionCount;
        this.rowCount = rowCount;
        this.storageBytes = storageBytes;
        this.requestId = requestId;
        this.traceId = traceId;
        this.executionId = executionId;
        this.historyId = historyId;
        this.parseTaskId = parseTaskId;
        this.reportCode = reportCode;
        this.sqlFingerprint = sqlFingerprint;
        this.upstreamRefs = upstreamRefs;
        this.downstreamRefs = downstreamRefs;
    }

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

    public List<MetadataLineageRef> getUpstreamRefs() {
        return upstreamRefs;
    }

    public void setUpstreamRefs(List<MetadataLineageRef> upstreamRefs) {
        this.upstreamRefs = upstreamRefs;
    }

    public List<MetadataLineageRef> getDownstreamRefs() {
        return downstreamRefs;
    }

    public void setDownstreamRefs(List<MetadataLineageRef> downstreamRefs) {
        this.downstreamRefs = downstreamRefs;
    }
}
