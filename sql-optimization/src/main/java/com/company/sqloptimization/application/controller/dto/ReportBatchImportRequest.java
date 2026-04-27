package com.company.sqloptimization.application.controller.dto;

import javax.validation.constraints.NotBlank;

public class ReportBatchImportRequest {

    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotBlank(message = "batchName is required")
    private String batchName;

    @NotBlank(message = "fileType is required")
    private String fileType;

    @NotBlank(message = "reportCodeField is required")
    private String reportCodeField;

    private String datasourceCode;
    private String stage;
    private String priority;
    private String contentBase64;
    private String charset;

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
    public String getContentBase64() { return contentBase64; }
    public void setContentBase64(String contentBase64) { this.contentBase64 = contentBase64; }
    public String getCharset() { return charset; }
    public void setCharset(String charset) { this.charset = charset; }
}
