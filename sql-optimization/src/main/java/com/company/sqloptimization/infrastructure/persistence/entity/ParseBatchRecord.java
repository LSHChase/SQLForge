package com.company.sqloptimization.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class ParseBatchRecord {

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
    private String statusHistoryJson;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
    public String getStatusHistoryJson() { return statusHistoryJson; }
    public void setStatusHistoryJson(String statusHistoryJson) { this.statusHistoryJson = statusHistoryJson; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
