package com.company.sqlforge.common.queryexecution;

public class QueryExecutionMaterializedViewCreateRequest {

    private String tenantId;
    private String recommendationId;
    private String rewriteRecordId;
    private String mvName;
    private String targetEngine;
    private String targetDatasource;
    private String ddlSql;
    private String refreshSql;
    private String reason;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

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

    public String getDdlSql() {
        return ddlSql;
    }

    public void setDdlSql(String ddlSql) {
        this.ddlSql = ddlSql;
    }

    public String getRefreshSql() {
        return refreshSql;
    }

    public void setRefreshSql(String refreshSql) {
        this.refreshSql = refreshSql;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
