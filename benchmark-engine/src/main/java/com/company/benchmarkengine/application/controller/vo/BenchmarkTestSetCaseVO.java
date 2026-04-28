package com.company.benchmarkengine.application.controller.vo;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetCaseStatus;
import java.util.List;

public class BenchmarkTestSetCaseVO {

    private String caseId;
    private Integer sequenceNumber;
    private Integer sourceLineNumber;
    private String caseName;
    private String sqlText;
    private String sqlFingerprint;
    private String datasourceCode;
    private String reportCode;
    private List<String> tags;
    private String bindParametersJson;
    private BenchmarkTestSetCaseStatus status;
    private String rejectionReason;
    private String rawCaseDataJson;

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getBindParametersJson() {
        return bindParametersJson;
    }

    public void setBindParametersJson(String bindParametersJson) {
        this.bindParametersJson = bindParametersJson;
    }

    public BenchmarkTestSetCaseStatus getStatus() {
        return status;
    }

    public void setStatus(BenchmarkTestSetCaseStatus status) {
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
}
