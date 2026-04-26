package com.company.sqlforge.common.queryexecution;

public class QueryExecutionAccelerationPlanResponse {

    private String tenantId;
    private String planId;
    private String sqlFingerprint;
    private String targetEngine;
    private boolean active;
    private String status;
    private String runtimeSummary;
    private String runtimeDetailsJson;
    private String contractStage;
    private String implementationStage;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
    }

    public String getTargetEngine() {
        return targetEngine;
    }

    public void setTargetEngine(String targetEngine) {
        this.targetEngine = targetEngine;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getContractStage() {
        return contractStage;
    }

    public void setContractStage(String contractStage) {
        this.contractStage = contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }

    public void setImplementationStage(String implementationStage) {
        this.implementationStage = implementationStage;
    }
}
