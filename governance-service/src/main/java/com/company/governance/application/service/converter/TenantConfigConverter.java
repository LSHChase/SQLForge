package com.company.governance.application.service.converter;

import com.company.governance.application.controller.vo.TenantConfigVO;
import com.company.governance.domain.tenant.entity.TenantConfig;
import org.springframework.stereotype.Component;

@Component
public class TenantConfigConverter {

    public TenantConfigVO toVO(TenantConfig tenantConfig) {
        return new TenantConfigVO(
            tenantConfig.getTenantId(),
            tenantConfig.getQuotaConcurrent(),
            tenantConfig.getQuotaStorage(),
            tenantConfig.getDefaultEngine() == null ? null : tenantConfig.getDefaultEngine().name(),
            tenantConfig.getBackupEngine() == null ? null : tenantConfig.getBackupEngine().name(),
            tenantConfig.getAuditLevel(),
            tenantConfig.getRetentionDays(),
            tenantConfig.getAccelerationQuota()
        );
    }
}
