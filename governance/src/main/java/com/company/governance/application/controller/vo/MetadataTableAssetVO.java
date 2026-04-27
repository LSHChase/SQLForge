package com.company.governance.application.controller.vo;

import java.time.Instant;
import java.util.List;

public class MetadataTableAssetVO {

    private String tenantId;
    private String datasourceCode;
    private String catalogName;
    private String schemaName;
    private String tableName;
    private String objectKey;
    private Integer columnCount;
    private Integer partitionCount;
    private Long rowCount;
    private Long storageBytes;
    private String freshnessStatus;
    private String slaStatus;
    private String queryabilityStatus;
    private String evidenceStatus;
    private Integer upstreamCount;
    private Integer downstreamCount;
    private Instant latestRefreshTime;
    private Instant latestSnapshotTime;
    private List<MetadataLineageVO> upstreamRefs;
    private List<MetadataLineageVO> downstreamRefs;
    private String contractStage;
    private String implementationStage;

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

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
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

    public Instant getLatestRefreshTime() {
        return latestRefreshTime;
    }

    public void setLatestRefreshTime(Instant latestRefreshTime) {
        this.latestRefreshTime = latestRefreshTime;
    }

    public Instant getLatestSnapshotTime() {
        return latestSnapshotTime;
    }

    public void setLatestSnapshotTime(Instant latestSnapshotTime) {
        this.latestSnapshotTime = latestSnapshotTime;
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
