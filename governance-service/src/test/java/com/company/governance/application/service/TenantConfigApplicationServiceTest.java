package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.common.constants.DataSourceTypeEnum;
import com.company.common.constants.ErrorCodeConstants;
import com.company.common.exception.BizException;
import com.company.governance.application.controller.vo.TenantConfigVO;
import com.company.governance.application.service.converter.TenantConfigConverter;
import com.company.governance.common.context.RequestContext;
import com.company.governance.common.context.TenantContext;
import com.company.governance.domain.tenant.entity.TenantConfig;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.domain.tenant.repository.TenantConfigRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantConfigApplicationServiceTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        RequestContext.clear();
    }

    @Test
    void shouldRejectWhenTenantCannotAccessTargetResource() {
        TenantConfigRepository repository = mock(TenantConfigRepository.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        TenantConfigApplicationService service = new TenantConfigApplicationService(
            repository,
            tenantAccessLogic,
            new TenantConfigConverter()
        );

        TenantContext.set("system");
        when(tenantAccessLogic.validateDataSourceAccess("system", "governance-tenant-config")).thenReturn(false);

        BizException ex = assertThrows(BizException.class, () -> service.findByTenantId("system"));

        assertEquals(ErrorCodeConstants.ERROR_ACCESS_DENIED, ex.getCode());
        assertEquals(ErrorCodeConstants.ERROR_ACCESS_DENIED_MESSAGE, ex.getMessage());
        verify(repository, never()).findByTenantId("system");
    }

    @Test
    void shouldLoadTenantConfigAfterAccessValidation() {
        TenantConfigRepository repository = mock(TenantConfigRepository.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        TenantConfigApplicationService service = new TenantConfigApplicationService(
            repository,
            tenantAccessLogic,
            new TenantConfigConverter()
        );

        TenantConfig tenantConfig = new TenantConfig();
        tenantConfig.setTenantId("system");
        tenantConfig.setQuotaConcurrent(20);
        tenantConfig.setQuotaStorage(2048);
        tenantConfig.setDefaultEngine(DataSourceTypeEnum.HETU);
        tenantConfig.setBackupEngine(DataSourceTypeEnum.HIVE);
        tenantConfig.setAuditLevel("NORMAL");
        tenantConfig.setRetentionDays(180);
        tenantConfig.setAccelerationQuota(50);

        TenantContext.set("system");
        RequestContext.set("trace-id-001", "dev-token");
        when(tenantAccessLogic.validateDataSourceAccess("system", "governance-tenant-config")).thenReturn(true);
        when(repository.findByTenantId(eq("system"))).thenReturn(Optional.of(tenantConfig));

        TenantConfigVO result = service.findByTenantId("system");

        assertEquals("system", result.getTenantId());
        assertEquals("HETU", result.getDefaultEngine());
        verify(tenantAccessLogic).validateDataSourceAccess("system", "governance-tenant-config");
        verify(repository).findByTenantId("system");
    }
}
