package com.company.sqlforge.common.governance;

public class GovernanceAccelerationPlanTraceRequest {

    private String planId;
    private String sourceTaskId;
    private String sqlFingerprint;
    private String datasourceType;
    private String sqlText;
    private String planStatus;
    private String snapshotPayloadJson;
    private String resultSummaryJson;
    private String resultPayloadJson;
    private String queryContextJson;
    private String createdAt;
    private String updatedAt;
    private Integer errorCode;
    private String errorMessage;

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getSourceTaskId() {
        return sourceTaskId;
    }

    public void setSourceTaskId(String sourceTaskId) {
        this.sourceTaskId = sourceTaskId;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }

    public String getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(String datasourceType) {
        this.datasourceType = datasourceType;
    }

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public String getPlanStatus() {
        return planStatus;
    }

    public void setPlanStatus(String planStatus) {
        this.planStatus = planStatus;
    }

    public String getSnapshotPayloadJson() {
        return snapshotPayloadJson;
    }

    public void setSnapshotPayloadJson(String snapshotPayloadJson) {
        this.snapshotPayloadJson = snapshotPayloadJson;
    }

    public String getResultSummaryJson() {
        return resultSummaryJson;
    }

    public void setResultSummaryJson(String resultSummaryJson) {
        this.resultSummaryJson = resultSummaryJson;
    }

    public String getResultPayloadJson() {
        return resultPayloadJson;
    }

    public void setResultPayloadJson(String resultPayloadJson) {
        this.resultPayloadJson = resultPayloadJson;
    }

    public String getQueryContextJson() {
        return queryContextJson;
    }

    public void setQueryContextJson(String queryContextJson) {
        this.queryContextJson = queryContextJson;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
