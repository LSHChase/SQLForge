package com.company.governance.application.controller.vo;

public class TenantConfigVO {

    private final String tenantId;
    private final Integer quotaConcurrent;
    private final Integer quotaStorage;
    private final String defaultEngine;
    private final String backupEngine;
    private final String auditLevel;
    private final Integer retentionDays;
    private final Integer accelerationQuota;

    public TenantConfigVO(String tenantId,
                          Integer quotaConcurrent,
                          Integer quotaStorage,
                          String defaultEngine,
                          String backupEngine,
                          String auditLevel,
                          Integer retentionDays,
                          Integer accelerationQuota) {
        this.tenantId = tenantId;
        this.quotaConcurrent = quotaConcurrent;
        this.quotaStorage = quotaStorage;
        this.defaultEngine = defaultEngine;
        this.backupEngine = backupEngine;
        this.auditLevel = auditLevel;
        this.retentionDays = retentionDays;
        this.accelerationQuota = accelerationQuota;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Integer getQuotaConcurrent() {
        return quotaConcurrent;
    }

    public Integer getQuotaStorage() {
        return quotaStorage;
    }

    public String getDefaultEngine() {
        return defaultEngine;
    }

    public String getBackupEngine() {
        return backupEngine;
    }

    public String getAuditLevel() {
        return auditLevel;
    }

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public Integer getAccelerationQuota() {
        return accelerationQuota;
    }
}
