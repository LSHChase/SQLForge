package com.company.sqloptimization.application.controller.dto;

import java.util.Map;
import javax.validation.constraints.NotBlank;

public class StructureParseRequest {

    @NotBlank
    private String sqlText;
    private String sqlTemplateText;
    private Map<String, Object> bindParameters;
    private String bindingMode;
    private String datasourceCode;
    private Map<String, Object> commentContext;
    private Boolean historyWriteEnabled;

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public String getSqlTemplateText() {
        return sqlTemplateText;
    }

    public void setSqlTemplateText(String sqlTemplateText) {
        this.sqlTemplateText = sqlTemplateText;
    }

    public Map<String, Object> getBindParameters() {
        return bindParameters;
    }

    public void setBindParameters(Map<String, Object> bindParameters) {
        this.bindParameters = bindParameters;
    }

    public String getBindingMode() {
        return bindingMode;
    }

    public void setBindingMode(String bindingMode) {
        this.bindingMode = bindingMode;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public void setDatasourceCode(String datasourceCode) {
        this.datasourceCode = datasourceCode;
    }

    public Map<String, Object> getCommentContext() {
        return commentContext;
    }

    public void setCommentContext(Map<String, Object> commentContext) {
        this.commentContext = commentContext;
    }

    public Boolean getHistoryWriteEnabled() {
        return historyWriteEnabled;
    }

    public void setHistoryWriteEnabled(Boolean historyWriteEnabled) {
        this.historyWriteEnabled = historyWriteEnabled;
    }
}
