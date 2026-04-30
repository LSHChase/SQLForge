package com.company.sqloptimization.application.controller.vo;

import java.time.Instant;
import java.util.List;

public class ReportBatchStatusResponse {

    private String batchId;
    private String tenantId;
    private String batchName;
    private String fileType;
    private String reportCodeField;
    private String datasourceCode;
    private String stage;
    private String priority;
    private String sourceType;
    private String status;
    private Integer totalReports;
    private Integer resolvedReports;
    private Integer failedReports;
    private Integer totalSqls;
    private Integer resolvedSqls;
    private Integer failedSqls;
    private List<ReportBatchItemVO> reportItems;
    private List<ReportBatchStatusHistoryVO> statusHistory;
    private Instant createdAt;
    private Instant updatedAt;

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getReportCodeField() { return reportCodeField; }
    public void setReportCodeField(String reportCodeField) { this.reportCodeField = reportCodeField; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalReports() { return totalReports; }
    public void setTotalReports(Integer totalReports) { this.totalReports = totalReports; }
    public Integer getResolvedReports() { return resolvedReports; }
    public void setResolvedReports(Integer resolvedReports) { this.resolvedReports = resolvedReports; }
    public Integer getFailedReports() { return failedReports; }
    public void setFailedReports(Integer failedReports) { this.failedReports = failedReports; }
    public Integer getTotalSqls() { return totalSqls; }
    public void setTotalSqls(Integer totalSqls) { this.totalSqls = totalSqls; }
    public Integer getResolvedSqls() { return resolvedSqls; }
    public void setResolvedSqls(Integer resolvedSqls) { this.resolvedSqls = resolvedSqls; }
    public Integer getFailedSqls() { return failedSqls; }
    public void setFailedSqls(Integer failedSqls) { this.failedSqls = failedSqls; }
    public List<ReportBatchItemVO> getReportItems() { return reportItems; }
    public void setReportItems(List<ReportBatchItemVO> reportItems) { this.reportItems = reportItems; }
    public List<ReportBatchStatusHistoryVO> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<ReportBatchStatusHistoryVO> statusHistory) { this.statusHistory = statusHistory; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
