package com.company.sqloptimization.application.controller.dto;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.parse.SqlParserMode;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class ParseBatchCreateRequest {

    @NotBlank(message = "tenantId 为必填项")
    private String tenantId;

    @NotBlank(message = "batchName 为必填项")
    private String batchName;

    @NotBlank(message = "importMode 为必填项")
    private String importMode;

    @NotBlank(message = "fileType 为必填项")
    private String fileType;

    private String templateVersion;
    private String datasourceCode;
    private DataSourceTypeEnum datasourceType;
    @Pattern(regexp = SqlParserMode.REQUEST_PATTERN, message = "parserMode 必须为 JSQLPARSER、APACHE_CALCITE、JSQLPARSER_WITH_PLAN 或 APACHE_CALCITE_WITH_PLAN")
    private String parserMode;

    @NotNull(message = "structureParseOnly 为必填项")
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
    public DataSourceTypeEnum getDatasourceType() { return datasourceType; }
    public void setDatasourceType(DataSourceTypeEnum datasourceType) { this.datasourceType = datasourceType; }
    public String getParserMode() { return parserMode; }
    public void setParserMode(String parserMode) { this.parserMode = parserMode; }
    public Boolean getStructureParseOnly() { return structureParseOnly; }
    public void setStructureParseOnly(Boolean structureParseOnly) { this.structureParseOnly = structureParseOnly; }
}
