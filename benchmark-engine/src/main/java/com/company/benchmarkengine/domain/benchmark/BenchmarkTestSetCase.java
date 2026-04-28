package com.company.benchmarkengine.domain.benchmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BenchmarkTestSetCase {

    private final String caseId;
    private final String testSetId;
    private final Integer sequenceNumber;
    private final Integer sourceLineNumber;
    private final String caseName;
    private final String sqlText;
    private final String sqlFingerprint;
    private final String datasourceCode;
    private final String reportCode;
    private final List<String> tags;
    private final String bindParametersJson;
    private final BenchmarkTestSetCaseStatus status;
    private final String rejectionReason;
    private final String rawCaseDataJson;

    public BenchmarkTestSetCase(String caseId,
                                String testSetId,
                                Integer sequenceNumber,
                                Integer sourceLineNumber,
                                String caseName,
                                String sqlText,
                                String sqlFingerprint,
                                String datasourceCode,
                                String reportCode,
                                List<String> tags,
                                String bindParametersJson,
                                BenchmarkTestSetCaseStatus status,
                                String rejectionReason,
                                String rawCaseDataJson) {
        this.caseId = caseId;
        this.testSetId = testSetId;
        this.sequenceNumber = sequenceNumber;
        this.sourceLineNumber = sourceLineNumber;
        this.caseName = caseName;
        this.sqlText = sqlText;
        this.sqlFingerprint = sqlFingerprint;
        this.datasourceCode = datasourceCode;
        this.reportCode = reportCode;
        this.tags = tags == null
            ? Collections.<String>emptyList()
            : Collections.unmodifiableList(new ArrayList<String>(tags));
        this.bindParametersJson = bindParametersJson;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.rawCaseDataJson = rawCaseDataJson;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getTestSetId() {
        return testSetId;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public Integer getSourceLineNumber() {
        return sourceLineNumber;
    }

    public String getCaseName() {
        return caseName;
    }

    public String getSqlText() {
        return sqlText;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public String getReportCode() {
        return reportCode;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getBindParametersJson() {
        return bindParametersJson;
    }

    public BenchmarkTestSetCaseStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getRawCaseDataJson() {
        return rawCaseDataJson;
    }
}
