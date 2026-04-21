package com.company.governance.application.controller.vo;

public class HealthStatusVO {

    private final String service;
    private final String status;
    private final String tenantId;
    private final String userId;
    private final String role;
    private final String accessScope;

    public HealthStatusVO(String service,
                          String status,
                          String tenantId,
                          String userId,
                          String role,
                          String accessScope) {
        this.service = service;
        this.status = status;
        this.tenantId = tenantId;
        this.userId = userId;
        this.role = role;
        this.accessScope = accessScope;
    }

    public String getService() {
        return service;
    }

    public String getStatus() {
        return status;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getAccessScope() {
        return accessScope;
    }
}
