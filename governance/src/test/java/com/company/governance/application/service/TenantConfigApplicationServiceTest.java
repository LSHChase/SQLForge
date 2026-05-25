package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.application.controller.dto.TenantEngineConfigUpdateRequest;
import com.company.governance.application.controller.vo.TenantConfigOptionVO;
import com.company.governance.application.controller.vo.TenantConfigVO;
import com.company.governance.application.service.converter.TenantConfigConverter;
import com.company.governance.domain.tenant.entity.TenantConfig;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.domain.tenant.repository.TenantConfigRepository;
import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import com.company.sqlforge.common.exception.BizException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
        GovernanceProtectedPersistenceService protectedPersistenceService = mock(GovernanceProtectedPersistenceService.class);
        TenantConfigApplicationService service = new TenantConfigApplicationService(
            repository,
            tenantAccessLogic,
            new TenantConfigConverter(),
            protectedPersistenceService
        );

        RequestContext.set("system", "user-001", "request-001", "trace-001", "header", 1L, 2L);
        when(tenantAccessLogic.validateDataSourceAccess("system", "governance-tenant-config", "READ")).thenReturn(false);

        BizException ex = assertThrows(BizException.class, () -> service.findByTenantId("system"));

        assertEquals(ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED, ex.getCode());
        assertEquals(ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED_MESSAGE, ex.getMessage());
        verify(repository, never()).findByTenantId("system");
    }

    @Test
    void shouldLoadTenantConfigAfterAccessValidation() {
        TenantConfigRepository repository = mock(TenantConfigRepository.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceProtectedPersistenceService protectedPersistenceService = mock(GovernanceProtectedPersistenceService.class);
        TenantConfigApplicationService service = new TenantConfigApplicationService(
            repository,
            tenantAccessLogic,
            new TenantConfigConverter(),
            protectedPersistenceService
        );
        TenantConfig tenantConfig = tenantConfig("system", DataSourceTypeEnum.HETU, DataSourceTypeEnum.HIVE);

        RequestContext.set("system", "user-001", "request-001", "trace-id-001", "header", 100L, 200L);
        when(tenantAccessLogic.validateDataSourceAccess("system", "governance-tenant-config", "READ")).thenReturn(true);
        when(repository.findByTenantId(eq("system"))).thenReturn(Optional.of(tenantConfig));

        TenantConfigVO result = service.findByTenantId("system");

        assertEquals("system", result.getTenantId());
        assertEquals("HETU", result.getDefaultEngine());
        verify(tenantAccessLogic).validateDataSourceAccess("system", "governance-tenant-config", "READ");
        verify(repository).findByTenantId("system");
    }

    @Test
    void shouldRejectCrossTenantAccessWhenCurrentTenantIsNotSystem() {
        TenantConfigRepository repository = mock(TenantConfigRepository.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceProtectedPersistenceService protectedPersistenceService = mock(GovernanceProtectedPersistenceService.class);
        TenantConfigApplicationService service = new TenantConfigApplicationService(
            repository,
            tenantAccessLogic,
            new TenantConfigConverter(),
            protectedPersistenceService
        );

        RequestContext.set("tenant-a", "user-001", "request-001", "trace-id-001", "header", 100L, 200L);

        BizException ex = assertThrows(BizException.class, () -> service.findByTenantId("tenant-b"));

        assertEquals(ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED, ex.getCode());
        verify(repository, never()).findByTenantId("tenant-b");
        verify(tenantAccessLogic, never()).validateDataSourceAccess("tenant-a", "governance-tenant-config", "READ");
    }

    @Test
    void shouldListSelfTenantOptionForOrdinaryTenant() {
        TenantConfigRepository repository = mock(TenantConfigRepository.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceProtectedPersistenceService protectedPersistenceService = mock(GovernanceProtectedPersistenceService.class);
        TenantConfigApplicationService service = new TenantConfigApplicationService(
            repository,
            tenantAccessLogic,
            new TenantConfigConverter(),
            protectedPersistenceService
        );

        RequestContext.set("tenant-a", "user-001", "request-001", "trace-id-001", "header", 100L, 200L);
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "governance-tenant-config", "READ")).thenReturn(true);
        when(repository.findByTenantId("tenant-a")).thenReturn(Optional.of(tenantConfig("tenant-a", DataSourceTypeEnum.HETU, DataSourceTypeEnum.HIVE)));

        List<TenantConfigOptionVO> options = service.listTenantOptions();

        assertEquals(1, options.size());
        assertEquals("tenant-a", options.get(0).getTenantId());
        assertEquals("HETU", options.get(0).getDefaultEngine());
        verify(repository, never()).findAll();
    }

    @Test
    void shouldListAllTenantOptionsForSystemTenant() {
        TenantConfigRepository repository = mock(TenantConfigRepository.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceProtectedPersistenceService protectedPersistenceService = mock(GovernanceProtectedPersistenceService.class);
        TenantConfigApplicationService service = new TenantConfigApplicationService(
            repository,
            tenantAccessLogic,
            new TenantConfigConverter(),
            protectedPersistenceService
        );

        RequestContext.set("system", "user-001", "request-001", "trace-id-001", "header", 100L, 200L);
        when(tenantAccessLogic.validateDataSourceAccess("system", "governance-tenant-config", "READ")).thenReturn(true);
        when(repository.findAll()).thenReturn(Arrays.asList(
            tenantConfig("system", DataSourceTypeEnum.HETU, DataSourceTypeEnum.HIVE),
            tenantConfig("tenant-a", DataSourceTypeEnum.TRINO, DataSourceTypeEnum.HIVE),
            tenantConfig("tenant-b", DataSourceTypeEnum.TRINO, DataSourceTypeEnum.HIVE)
        ));

        List<TenantConfigOptionVO> options = service.listTenantOptions();

        assertEquals(3, options.size());
        assertEquals("system", options.get(0).getTenantId());
        assertEquals("tenant-a", options.get(1).getTenantId());
        assertEquals("tenant-b", options.get(2).getTenantId());
        verify(repository).findAll();
    }

    @Test
    void shouldUpdateTenantEnginesWithManagePermissionAndAuditLog() {
        TenantConfigRepository repository = mock(TenantConfigRepository.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceProtectedPersistenceService protectedPersistenceService = mock(GovernanceProtectedPersistenceService.class);
        TenantConfigApplicationService service = new TenantConfigApplicationService(
            repository,
            tenantAccessLogic,
            new TenantConfigConverter(),
            protectedPersistenceService
        );
        TenantConfig tenantConfig = tenantConfig("tenant-a", DataSourceTypeEnum.HETU, DataSourceTypeEnum.HIVE);
        TenantEngineConfigUpdateRequest request = new TenantEngineConfigUpdateRequest();
        request.setTenantId("tenant-a");
        request.setDefaultEngine("TRINO");
        request.setBackupEngine("HIVE");

        RequestContext.set("tenant-a", "user-001", "request-001", "trace-id-001", "header", 100L, 200L);
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "governance-tenant-config", "MANAGE")).thenReturn(true);
        when(repository.findByTenantId("tenant-a")).thenReturn(Optional.of(tenantConfig));
        when(repository.update(tenantConfig)).thenReturn(1);

        TenantConfigVO result = service.updateTenantEngines(request);

        assertEquals("TRINO", result.getDefaultEngine());
        assertEquals("HIVE", result.getBackupEngine());
        verify(repository).update(tenantConfig);
        ArgumentCaptor<AuditLogRecord> auditCaptor = ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(protectedPersistenceService).saveAuditLog(auditCaptor.capture());
        assertEquals("TENANT_CONFIG_ENGINE_UPDATE", auditCaptor.getValue().getOperationType());
        assertEquals("tenant-a", auditCaptor.getValue().getTargetId());
        assertEquals("trace-id-001", auditCaptor.getValue().getTraceId());
    }

    @Test
    void shouldRejectUnsupportedEngineWhenUpdatingTenantEngines() {
        TenantConfigRepository repository = mock(TenantConfigRepository.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        GovernanceProtectedPersistenceService protectedPersistenceService = mock(GovernanceProtectedPersistenceService.class);
        TenantConfigApplicationService service = new TenantConfigApplicationService(
            repository,
            tenantAccessLogic,
            new TenantConfigConverter(),
            protectedPersistenceService
        );
        TenantEngineConfigUpdateRequest request = new TenantEngineConfigUpdateRequest();
        request.setTenantId("tenant-a");
        request.setDefaultEngine("UNKNOWN");
        request.setBackupEngine("HIVE");

        RequestContext.set("tenant-a", "user-001", "request-001", "trace-id-001", "header", 100L, 200L);
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "governance-tenant-config", "MANAGE")).thenReturn(true);

        BizException ex = assertThrows(BizException.class, () -> service.updateTenantEngines(request));

        assertEquals(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, ex.getCode());
        verify(repository, never()).update(org.mockito.ArgumentMatchers.any());
        verify(protectedPersistenceService, never()).saveAuditLog(org.mockito.ArgumentMatchers.any());
    }

    private TenantConfig tenantConfig(String tenantId,
                                      DataSourceTypeEnum defaultEngine,
                                      DataSourceTypeEnum backupEngine) {
        TenantConfig tenantConfig = new TenantConfig();
        tenantConfig.setTenantId(tenantId);
        tenantConfig.setQuotaConcurrent(20);
        tenantConfig.setQuotaStorage(2048);
        tenantConfig.setDefaultEngine(defaultEngine);
        tenantConfig.setBackupEngine(backupEngine);
        tenantConfig.setAuditLevel("NORMAL");
        tenantConfig.setRetentionDays(180);
        tenantConfig.setAccelerationQuota(50);
        return tenantConfig;
    }
}
