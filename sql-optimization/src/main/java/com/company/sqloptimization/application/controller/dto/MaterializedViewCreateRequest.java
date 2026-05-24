package com.company.sqloptimization.application.controller.dto;

public class MaterializedViewCreateRequest {

    private String tenantId;
    private String rewriteRecordId;
    private String reason;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRewriteRecordId() {
        return rewriteRecordId;
    }

    public void setRewriteRecordId(String rewriteRecordId) {
        this.rewriteRecordId = rewriteRecordId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
