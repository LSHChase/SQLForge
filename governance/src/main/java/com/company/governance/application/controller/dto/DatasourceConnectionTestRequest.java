package com.company.governance.application.controller.dto;

public class DatasourceConnectionTestRequest {

    private String tenantId;
    private Integer timeoutMsOverride;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getTimeoutMsOverride() {
        return timeoutMsOverride;
    }

    public void setTimeoutMsOverride(Integer timeoutMsOverride) {
        this.timeoutMsOverride = timeoutMsOverride;
    }
}
