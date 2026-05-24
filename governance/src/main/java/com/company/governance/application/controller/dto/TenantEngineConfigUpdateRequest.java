package com.company.governance.application.controller.dto;

public class TenantEngineConfigUpdateRequest {

    private String tenantId;
    private String defaultEngine;
    private String backupEngine;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDefaultEngine() {
        return defaultEngine;
    }

    public void setDefaultEngine(String defaultEngine) {
        this.defaultEngine = defaultEngine;
    }

    public String getBackupEngine() {
        return backupEngine;
    }

    public void setBackupEngine(String backupEngine) {
        this.backupEngine = backupEngine;
    }
}
