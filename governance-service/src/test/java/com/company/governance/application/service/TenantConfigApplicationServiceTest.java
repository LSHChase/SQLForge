package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.application.controller.vo.TenantConfigVO;
import com.company.governance.application.service.converter.TenantConfigConverter;
import com.company.governance.domain.tenant.entity.TenantConfig;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.domain.tenant.repository.TenantConfigRepository;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import com.company.sqlforge.common.exception.BizException;
import java.util.Arrays;
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

        RequestContext.set("system", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        when(tenantAccessLogic.validateDataSourceAccess("system", "governance-tenant-config")).thenReturn(false);

        BizException ex = assertThrows(BizException.class, () -> service.findByTenantId("system"));

        assertEquals(ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED, ex.getCode());
        assertEquals(ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED_MESSAGE, ex.getMessage());
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

        RequestContext.set(
            "system",
            "operator-001",
            Arrays.asList("TENANT_ADMIN", "OPERATOR"),
            "request-001",
            "trace-id-001",
            "header",
            100L,
            200L
        );
        when(tenantAccessLogic.validateDataSourceAccess("system", "governance-tenant-config")).thenReturn(true);
        when(repository.findByTenantId(eq("system"))).thenReturn(Optional.of(tenantConfig));

        TenantConfigVO result = service.findByTenantId("system");

        assertEquals("system", result.getTenantId());
        assertEquals("HETU", result.getDefaultEngine());
        verify(tenantAccessLogic).validateDataSourceAccess("system", "governance-tenant-config");
        verify(repository).findByTenantId("system");
    }

    @Test
    void shouldRejectCrossTenantAccessWithoutPlatformRole() {
        TenantConfigRepository repository = mock(TenantConfigRepository.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        TenantConfigApplicationService service = new TenantConfigApplicationService(
            repository,
            tenantAccessLogic,
            new TenantConfigConverter()
        );

        RequestContext.set(
            "system",
            "operator-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-id-001",
            "header",
            100L,
            200L
        );

        BizException ex = assertThrows(BizException.class, () -> service.findByTenantId("tenant-b"));

        assertEquals(ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED, ex.getCode());
        verify(repository, never()).findByTenantId("tenant-b");
    }

    @Test
    void shouldRejectTenantConfigReadWithoutTenantAdminRole() {
        TenantConfigRepository repository = mock(TenantConfigRepository.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        TenantConfigApplicationService service = new TenantConfigApplicationService(
            repository,
            tenantAccessLogic,
            new TenantConfigConverter()
        );

        RequestContext.set(
            "system",
            "operator-001",
            Arrays.asList("OPERATOR"),
            "request-001",
            "trace-id-001",
            "header",
            100L,
            200L
        );

        BizException ex = assertThrows(BizException.class, () -> service.findByTenantId("system"));

        assertEquals(ErrorCodeConstants.GOVERNANCE_ACCESS_DENIED, ex.getCode());
        verify(repository, never()).findByTenantId("system");
        verify(tenantAccessLogic, never()).validateDataSourceAccess("system", "governance-tenant-config");
    }

    @Test
    void shouldAllowPlatformAdminCrossTenantReadWithoutPlaceholderDatasourceCheck() {
        TenantConfigRepository repository = mock(TenantConfigRepository.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        TenantConfigApplicationService service = new TenantConfigApplicationService(
            repository,
            tenantAccessLogic,
            new TenantConfigConverter()
        );
        TenantConfig tenantConfig = new TenantConfig();
        tenantConfig.setTenantId("tenant-b");
        tenantConfig.setDefaultEngine(DataSourceTypeEnum.HETU);

        RequestContext.set(
            "system",
            "platform-admin-001",
            Arrays.asList("PLATFORM_ADMIN"),
            "request-001",
            "trace-id-001",
            "gateway",
            100L,
            200L
        );
        when(repository.findByTenantId("tenant-b")).thenReturn(Optional.of(tenantConfig));

        TenantConfigVO result = service.findByTenantId("tenant-b");

        assertEquals("tenant-b", result.getTenantId());
        verify(repository, times(1)).findByTenantId("tenant-b");
        verify(tenantAccessLogic, never()).validateDataSourceAccess("system", "governance-tenant-config");
    }
}
