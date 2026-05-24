package com.company.sqlforge.common.queryexecution;

public class QueryExecutionMaterializedViewCreateResponse {

    private String recommendationId;
    private String rewriteRecordId;
    private String mvName;
    private String targetEngine;
    private String targetDatasource;
    private String status;
    private String ddlStatus;
    private String refreshStatus;
    private String runtimeSummary;
    private String runtimeDetailsJson;

    public String getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(String recommendationId) {
        this.recommendationId = recommendationId;
    }

    public String getRewriteRecordId() {
        return rewriteRecordId;
    }

    public void setRewriteRecordId(String rewriteRecordId) {
        this.rewriteRecordId = rewriteRecordId;
    }

    public String getMvName() {
        return mvName;
    }

    public void setMvName(String mvName) {
        this.mvName = mvName;
    }

    public String getTargetEngine() {
        return targetEngine;
    }

    public void setTargetEngine(String targetEngine) {
        this.targetEngine = targetEngine;
    }

    public String getTargetDatasource() {
        return targetDatasource;
    }

    public void setTargetDatasource(String targetDatasource) {
        this.targetDatasource = targetDatasource;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDdlStatus() {
        return ddlStatus;
    }

    public void setDdlStatus(String ddlStatus) {
        this.ddlStatus = ddlStatus;
    }

    public String getRefreshStatus() {
        return refreshStatus;
    }

    public void setRefreshStatus(String refreshStatus) {
        this.refreshStatus = refreshStatus;
    }

    public String getRuntimeSummary() {
        return runtimeSummary;
    }

    public void setRuntimeSummary(String runtimeSummary) {
        this.runtimeSummary = runtimeSummary;
    }

    public String getRuntimeDetailsJson() {
        return runtimeDetailsJson;
    }

    public void setRuntimeDetailsJson(String runtimeDetailsJson) {
        this.runtimeDetailsJson = runtimeDetailsJson;
    }
}
