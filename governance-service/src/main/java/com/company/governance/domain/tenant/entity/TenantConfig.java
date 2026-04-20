package com.company.governance.domain.tenant.entity;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.time.LocalDateTime;

public class TenantConfig {

    private Long id;
    private String tenantId;
    private Integer quotaConcurrent;
    private Integer quotaStorage;
    private DataSourceTypeEnum defaultEngine;
    private DataSourceTypeEnum backupEngine;
    private String auditLevel;
    private Integer retentionDays;
    private Integer accelerationQuota;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getQuotaConcurrent() {
        return quotaConcurrent;
    }

    public void setQuotaConcurrent(Integer quotaConcurrent) {
        this.quotaConcurrent = quotaConcurrent;
    }

    public Integer getQuotaStorage() {
        return quotaStorage;
    }

    public void setQuotaStorage(Integer quotaStorage) {
        this.quotaStorage = quotaStorage;
    }

    public DataSourceTypeEnum getDefaultEngine() {
        return defaultEngine;
    }

    public void setDefaultEngine(DataSourceTypeEnum defaultEngine) {
        this.defaultEngine = defaultEngine;
    }

    public DataSourceTypeEnum getBackupEngine() {
        return backupEngine;
    }

    public void setBackupEngine(DataSourceTypeEnum backupEngine) {
        this.backupEngine = backupEngine;
    }

    public String getAuditLevel() {
        return auditLevel;
    }

    public void setAuditLevel(String auditLevel) {
        this.auditLevel = auditLevel;
    }

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }

    public Integer getAccelerationQuota() {
        return accelerationQuota;
    }

    public void setAccelerationQuota(Integer accelerationQuota) {
        this.accelerationQuota = accelerationQuota;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
