package com.company.sqloptimization.application.controller.vo;

import java.time.Instant;
import java.util.List;

public class ParseBatchStatusResponse {

    private String batchId;
    private String tenantId;
    private String batchName;
    private String importMode;
    private String sourceType;
    private String fileType;
    private String templateVersion;
    private String datasourceCode;
    private Boolean structureParseOnly;
    private String status;
    private Integer totalRecords;
    private Integer successRecords;
    private Integer partialSuccessRecords;
    private Integer failedRecords;
    private Double structureParseSuccessRate;
    private Double accessParseSuccessRate;
    private Integer itemPreviewLimit;
    private Boolean itemPreviewTruncated;
    private Integer omittedItemCount;
    private Integer failurePreviewLimit;
    private Boolean failurePreviewTruncated;
    private Integer omittedFailureCount;
    private List<String> supportedFileTypes;
    private List<ParseBatchTemplateColumnVO> templateColumns;
    private List<ParseBatchStatusHistoryVO> statusHistory;
    private List<ParseBatchItemVO> importedRecords;
    private List<ParseBatchItemVO> failureRecords;
    private ParseBatchStageStatisticsVO structureParseStatistics;
    private ParseBatchStageStatisticsVO accessParseStatistics;
    private List<ParseBatchIssueStatisticVO> issueStatistics;
    private List<ParseBatchReportStatisticVO> reportStatistics;
    private Instant createdAt;
    private Instant updatedAt;

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }
    public String getImportMode() { return importMode; }
    public void setImportMode(String importMode) { this.importMode = importMode; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public Boolean getStructureParseOnly() { return structureParseOnly; }
    public void setStructureParseOnly(Boolean structureParseOnly) { this.structureParseOnly = structureParseOnly; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalRecords() { return totalRecords; }
    public void setTotalRecords(Integer totalRecords) { this.totalRecords = totalRecords; }
    public Integer getSuccessRecords() { return successRecords; }
    public void setSuccessRecords(Integer successRecords) { this.successRecords = successRecords; }
    public Integer getPartialSuccessRecords() { return partialSuccessRecords; }
    public void setPartialSuccessRecords(Integer partialSuccessRecords) { this.partialSuccessRecords = partialSuccessRecords; }
    public Integer getFailedRecords() { return failedRecords; }
    public void setFailedRecords(Integer failedRecords) { this.failedRecords = failedRecords; }
    public Double getStructureParseSuccessRate() { return structureParseSuccessRate; }
    public void setStructureParseSuccessRate(Double structureParseSuccessRate) { this.structureParseSuccessRate = structureParseSuccessRate; }
    public Double getAccessParseSuccessRate() { return accessParseSuccessRate; }
    public void setAccessParseSuccessRate(Double accessParseSuccessRate) { this.accessParseSuccessRate = accessParseSuccessRate; }
    public Integer getItemPreviewLimit() { return itemPreviewLimit; }
    public void setItemPreviewLimit(Integer itemPreviewLimit) { this.itemPreviewLimit = itemPreviewLimit; }
    public Boolean getItemPreviewTruncated() { return itemPreviewTruncated; }
    public void setItemPreviewTruncated(Boolean itemPreviewTruncated) { this.itemPreviewTruncated = itemPreviewTruncated; }
    public Integer getOmittedItemCount() { return omittedItemCount; }
    public void setOmittedItemCount(Integer omittedItemCount) { this.omittedItemCount = omittedItemCount; }
    public Integer getFailurePreviewLimit() { return failurePreviewLimit; }
    public void setFailurePreviewLimit(Integer failurePreviewLimit) { this.failurePreviewLimit = failurePreviewLimit; }
    public Boolean getFailurePreviewTruncated() { return failurePreviewTruncated; }
    public void setFailurePreviewTruncated(Boolean failurePreviewTruncated) { this.failurePreviewTruncated = failurePreviewTruncated; }
    public Integer getOmittedFailureCount() { return omittedFailureCount; }
    public void setOmittedFailureCount(Integer omittedFailureCount) { this.omittedFailureCount = omittedFailureCount; }
    public List<String> getSupportedFileTypes() { return supportedFileTypes; }
    public void setSupportedFileTypes(List<String> supportedFileTypes) { this.supportedFileTypes = supportedFileTypes; }
    public List<ParseBatchTemplateColumnVO> getTemplateColumns() { return templateColumns; }
    public void setTemplateColumns(List<ParseBatchTemplateColumnVO> templateColumns) { this.templateColumns = templateColumns; }
    public List<ParseBatchStatusHistoryVO> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<ParseBatchStatusHistoryVO> statusHistory) { this.statusHistory = statusHistory; }
    public List<ParseBatchItemVO> getImportedRecords() { return importedRecords; }
    public void setImportedRecords(List<ParseBatchItemVO> importedRecords) { this.importedRecords = importedRecords; }
    public List<ParseBatchItemVO> getFailureRecords() { return failureRecords; }
    public void setFailureRecords(List<ParseBatchItemVO> failureRecords) { this.failureRecords = failureRecords; }
    public ParseBatchStageStatisticsVO getStructureParseStatistics() { return structureParseStatistics; }
    public void setStructureParseStatistics(ParseBatchStageStatisticsVO structureParseStatistics) { this.structureParseStatistics = structureParseStatistics; }
    public ParseBatchStageStatisticsVO getAccessParseStatistics() { return accessParseStatistics; }
    public void setAccessParseStatistics(ParseBatchStageStatisticsVO accessParseStatistics) { this.accessParseStatistics = accessParseStatistics; }
    public List<ParseBatchIssueStatisticVO> getIssueStatistics() { return issueStatistics; }
    public void setIssueStatistics(List<ParseBatchIssueStatisticVO> issueStatistics) { this.issueStatistics = issueStatistics; }
    public List<ParseBatchReportStatisticVO> getReportStatistics() { return reportStatistics; }
    public void setReportStatistics(List<ParseBatchReportStatisticVO> reportStatistics) { this.reportStatistics = reportStatistics; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
