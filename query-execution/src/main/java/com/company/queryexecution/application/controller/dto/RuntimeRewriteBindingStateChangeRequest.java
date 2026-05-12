package com.company.queryexecution.application.controller.dto;

public class RuntimeRewriteBindingStateChangeRequest {

    private String tenantId;
    private String runtimeBindingId;
    private String sqlFingerprint;
    private String reason;
    private String operatorId;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getRuntimeBindingId() { return runtimeBindingId; }
    public void setRuntimeBindingId(String runtimeBindingId) { this.runtimeBindingId = runtimeBindingId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
}
