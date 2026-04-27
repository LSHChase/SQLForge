package com.company.sqloptimization.application.controller.vo;

public class ParseBatchTemplateColumnVO {

    private String columnKey;
    private String displayName;
    private Boolean required;
    private String description;

    public String getColumnKey() { return columnKey; }
    public void setColumnKey(String columnKey) { this.columnKey = columnKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
