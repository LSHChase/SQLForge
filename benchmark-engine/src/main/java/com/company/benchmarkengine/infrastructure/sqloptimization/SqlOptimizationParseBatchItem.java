package com.company.benchmarkengine.infrastructure.sqloptimization;

import java.util.List;

public class SqlOptimizationParseBatchItem {

    private String itemId;
    private Integer sequenceNumber;
    private String reportCode;
    private String datasourceCode;
    private String stage;
    private String sqlText;
    private String status;
    private String parseTaskId;
    private String structureSyntaxStatus;
    private List<String> issueScenes;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getReportCode() {
        return reportCode;
    }

    public void setReportCode(String reportCode) {
        this.reportCode = reportCode;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public void setDatasourceCode(String datasourceCode) {
        this.datasourceCode = datasourceCode;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getParseTaskId() {
        return parseTaskId;
    }

    public void setParseTaskId(String parseTaskId) {
        this.parseTaskId = parseTaskId;
    }

    public String getStructureSyntaxStatus() {
        return structureSyntaxStatus;
    }

    public void setStructureSyntaxStatus(String structureSyntaxStatus) {
        this.structureSyntaxStatus = structureSyntaxStatus;
    }

    public List<String> getIssueScenes() {
        return issueScenes;
    }

    public void setIssueScenes(List<String> issueScenes) {
        this.issueScenes = issueScenes;
    }
}
