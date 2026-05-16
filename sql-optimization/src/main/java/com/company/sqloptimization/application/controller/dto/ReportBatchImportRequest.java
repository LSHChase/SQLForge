package com.company.sqloptimization.application.controller.dto;

import com.company.sqloptimization.domain.parse.SqlParserMode;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class ReportBatchImportRequest {

    @NotBlank(message = "tenantId 为必填项")
    private String tenantId;

    @NotBlank(message = "batchName 为必填项")
    private String batchName;

    private String fileType;

    private String fileName;

    @NotBlank(message = "reportCodeField 为必填项")
    private String reportCodeField;

    private String datasourceCode;
    private String stage;
    private String priority;
    @Pattern(regexp = SqlParserMode.REQUEST_PATTERN, message = "parserMode 必须为 JSQLPARSER、APACHE_CALCITE、JSQLPARSER_WITH_PLAN 或 APACHE_CALCITE_WITH_PLAN")
    private String parserMode;
    private String contentBase64;
    private String charset;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getReportCodeField() { return reportCodeField; }
    public void setReportCodeField(String reportCodeField) { this.reportCodeField = reportCodeField; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getParserMode() { return parserMode; }
    public void setParserMode(String parserMode) { this.parserMode = parserMode; }
    public String getContentBase64() { return contentBase64; }
    public void setContentBase64(String contentBase64) { this.contentBase64 = contentBase64; }
    public String getCharset() { return charset; }
    public void setCharset(String charset) { this.charset = charset; }
}
