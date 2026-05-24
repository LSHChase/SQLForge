package com.company.governance.application.controller.vo;

public class TenantConfigOptionVO {

    private final String tenantId;
    private final String label;
    private final String defaultEngine;
    private final String backupEngine;

    public TenantConfigOptionVO(String tenantId, String label, String defaultEngine, String backupEngine) {
        this.tenantId = tenantId;
        this.label = label;
        this.defaultEngine = defaultEngine;
        this.backupEngine = backupEngine;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getLabel() {
        return label;
    }

    public String getDefaultEngine() {
        return defaultEngine;
    }

    public String getBackupEngine() {
        return backupEngine;
    }
}
