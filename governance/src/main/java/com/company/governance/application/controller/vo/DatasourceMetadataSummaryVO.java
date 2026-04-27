package com.company.governance.application.controller.vo;

import java.time.Instant;

public class DatasourceMetadataSummaryVO {

    private Integer snapshotCount;
    private Integer schemaCount;
    private Integer tableCount;
    private Integer dbViewCount;
    private Integer logicalViewCount;
    private String freshnessStatus;
    private String slaStatus;
    private String queryabilityStatus;
    private Instant latestSnapshotTime;

    public Integer getSnapshotCount() {
        return snapshotCount;
    }

    public void setSnapshotCount(Integer snapshotCount) {
        this.snapshotCount = snapshotCount;
    }

    public Integer getSchemaCount() {
        return schemaCount;
    }

    public void setSchemaCount(Integer schemaCount) {
        this.schemaCount = schemaCount;
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

    public Instant getLatestSnapshotTime() {
        return latestSnapshotTime;
    }

    public void setLatestSnapshotTime(Instant latestSnapshotTime) {
        this.latestSnapshotTime = latestSnapshotTime;
    }
}
