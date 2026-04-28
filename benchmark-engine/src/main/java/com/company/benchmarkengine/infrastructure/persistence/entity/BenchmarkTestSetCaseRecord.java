package com.company.benchmarkengine.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class BenchmarkTestSetCaseRecord {

    private String caseId;
    private String testSetId;
    private Integer sequenceNumber;
    private Integer sourceLineNumber;
    private String caseName;
    private String sqlText;
    private String sqlFingerprint;
    private String datasourceCode;
    private String reportCode;
    private String tagsJson;
    private String bindParametersJson;
    private String status;
    private String rejectionReason;
    private String rawCaseDataJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getTestSetId() {
        return testSetId;
    }

    public void setTestSetId(String testSetId) {
        this.testSetId = testSetId;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Integer getSourceLineNumber() {
        return sourceLineNumber;
    }

    public void setSourceLineNumber(Integer sourceLineNumber) {
        this.sourceLineNumber = sourceLineNumber;
    }

    public String getCaseName() {
        return caseName;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public void setDatasourceCode(String datasourceCode) {
        this.datasourceCode = datasourceCode;
    }

    public String getReportCode() {
        return reportCode;
    }

    public void setReportCode(String reportCode) {
        this.reportCode = reportCode;
    }

    public String getTagsJson() {
        return tagsJson;
    }

    public void setTagsJson(String tagsJson) {
        this.tagsJson = tagsJson;
    }

    public String getBindParametersJson() {
        return bindParametersJson;
    }

    public void setBindParametersJson(String bindParametersJson) {
        this.bindParametersJson = bindParametersJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRawCaseDataJson() {
        return rawCaseDataJson;
    }

    public void setRawCaseDataJson(String rawCaseDataJson) {
        this.rawCaseDataJson = rawCaseDataJson;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
