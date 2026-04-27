package com.company.governance.application.controller.vo;

import java.time.Instant;

public class MetadataSchemaAssetVO {

    private String tenantId;
    private String datasourceCode;
    private String catalogName;
    private String schemaName;
    private Integer snapshotCount;
    private Integer tableCount;
    private Integer dbViewCount;
    private Integer logicalViewCount;
    private Long rowCount;
    private Long storageBytes;
    private String freshnessStatus;
    private String slaStatus;
    private String queryabilityStatus;
    private String evidenceStatus;
    private Instant latestRefreshTime;
    private Instant latestSnapshotTime;
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

    public Integer getSnapshotCount() {
        return snapshotCount;
    }

    public void setSnapshotCount(Integer snapshotCount) {
        this.snapshotCount = snapshotCount;
    }

    public Integer getTableCount() {
        return tableCount;
    }

    public void setTableCount(Integer tableCount) {
        this.tableCount = tableCount;
    }

    public Integer getDbViewCount() {
        return dbViewCount;
    }

    public void setDbViewCount(Integer dbViewCount) {
        this.dbViewCount = dbViewCount;
    }

    public Integer getLogicalViewCount() {
        return logicalViewCount;
    }

    public void setLogicalViewCount(Integer logicalViewCount) {
        this.logicalViewCount = logicalViewCount;
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
