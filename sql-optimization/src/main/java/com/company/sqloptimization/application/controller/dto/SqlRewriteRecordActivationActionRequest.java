package com.company.sqloptimization.application.controller.dto;

public class SqlRewriteRecordActivationActionRequest {

    private String tenantId;
    private String reason;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
