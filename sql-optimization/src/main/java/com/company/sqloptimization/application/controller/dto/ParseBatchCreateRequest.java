package com.company.sqloptimization.application.controller.dto;

import com.company.sqloptimization.domain.parse.SqlParserMode;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class ParseBatchCreateRequest {

    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotBlank(message = "batchName is required")
    private String batchName;

    @NotBlank(message = "importMode is required")
    private String importMode;

    @NotBlank(message = "fileType is required")
    private String fileType;

    private String templateVersion;
    private String datasourceCode;
    @Pattern(regexp = SqlParserMode.REQUEST_PATTERN, message = "parserMode must be JSQLPARSER or APACHE_CALCITE")
    private String parserMode;

    @NotNull(message = "structureParseOnly is required")
    private Boolean structureParseOnly;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }
    public String getImportMode() { return importMode; }
    public void setImportMode(String importMode) { this.importMode = importMode; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public String getParserMode() { return parserMode; }
    public void setParserMode(String parserMode) { this.parserMode = parserMode; }
    public Boolean getStructureParseOnly() { return structureParseOnly; }
    public void setStructureParseOnly(Boolean structureParseOnly) { this.structureParseOnly = structureParseOnly; }
}
