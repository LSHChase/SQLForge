package com.company.governance.application.controller.vo;

public class TenantScopeCheckResponse {

    private final String tenantId;
    private final String targetTenantId;
    private final boolean allowed;
    private final String reason;

    public TenantScopeCheckResponse(String tenantId, String targetTenantId, boolean allowed, String reason) {
        this.tenantId = tenantId;
        this.targetTenantId = targetTenantId;
        this.allowed = allowed;
        this.reason = reason;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTargetTenantId() {
        return targetTenantId;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getReason() {
        return reason;
    }
}
