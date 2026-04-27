package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.governance.application.controller.vo.BusinessLogicalViewVO;
import com.company.governance.domain.logicalview.entity.BusinessLogicalView;
import com.company.governance.domain.logicalview.entity.LogicalObjectMapping;
import com.company.governance.domain.logicalview.repository.BusinessLogicalViewRepository;
import com.company.governance.domain.logicalview.repository.LogicalObjectMappingRepository;
import com.company.governance.infrastructure.repository.InMemoryMetadataSnapshotRepository;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import com.company.sqlforge.common.exception.BizException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LogicalViewCatalogApplicationServiceTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        RequestContext.clear();
    }

    @Test
    void shouldListBusinessLogicalViewsWithPhysicalTargets() {
        BusinessLogicalViewRepository viewRepository = mock(BusinessLogicalViewRepository.class);
        LogicalObjectMappingRepository mappingRepository = mock(LogicalObjectMappingRepository.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        LogicalViewCatalogApplicationService service = new LogicalViewCatalogApplicationService(
            viewRepository,
            mappingRepository,
            new InMemoryMetadataSnapshotRepository(),
            tenantAccessLogic
        );

        RequestContext.set("tenant-a", "analyst-001", Arrays.asList("ANALYST"), "req-1", "trace-1", "header", 1L, 2L);
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "hetu_main")).thenReturn(true);
        when(viewRepository.findByTenantAndDatasource("tenant-a", "hetu_main"))
            .thenReturn(Collections.singletonList(buildView("view-001", "RPT_SALES_DAILY")));
        when(mappingRepository.findByTenantAndLogicalViewId("tenant-a", "view-001"))
            .thenReturn(Collections.singletonList(buildMapping("TABLE:sales.orders", "sales.orders")));

        List<BusinessLogicalViewVO> views = service.listLogicalViews("tenant-a", "hetu_main");

        assertEquals(1, views.size());
        assertEquals("RPT_SALES_DAILY", views.get(0).getViewCode());
        assertEquals("LOGICAL_VIEW:RPT_SALES_DAILY", views.get(0).getObjectKey());
        assertEquals(Integer.valueOf(1), views.get(0).getUpstreamCount());
        assertEquals("TABLE:sales.orders", views.get(0).getPhysicalTargets().get(0).getTargetObjectKey());
    }

    @Test
    void shouldRejectCrossTenantLogicalViewLookupForNonPlatformAdmin() {
        BusinessLogicalViewRepository viewRepository = mock(BusinessLogicalViewRepository.class);
        LogicalObjectMappingRepository mappingRepository = mock(LogicalObjectMappingRepository.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        LogicalViewCatalogApplicationService service = new LogicalViewCatalogApplicationService(
            viewRepository,
            mappingRepository,
            new InMemoryMetadataSnapshotRepository(),
            tenantAccessLogic
        );

        RequestContext.set("tenant-a", "analyst-001", Arrays.asList("ANALYST"), "req-1", "trace-1", "header", 1L, 2L);

        assertThrows(BizException.class, () -> service.findLogicalView("tenant-b", "RPT_SALES_DAILY"));
    }

    private BusinessLogicalView buildView(String id, String viewCode) {
        BusinessLogicalView record = new BusinessLogicalView();
        record.setId(id);
        record.setTenantId("tenant-a");
        record.setViewCode(viewCode);
        record.setViewName("Sales Daily");
        record.setDatasourceCode("hetu_main");
        record.setSubjectArea("sales");
        record.setOwnerUser("owner-001");
        record.setFreshnessStatus("FRESH");
        record.setSlaStatus("ON_TIME");
        record.setQueryable(Boolean.TRUE);
        record.setLatestRefreshTime(LocalDateTime.parse("2026-04-26T09:00:00"));
        record.setDescription("Daily sales logical view");
        return record;
    }

    private LogicalObjectMapping buildMapping(String targetObjectKey, String targetObjectName) {
        LogicalObjectMapping mapping = new LogicalObjectMapping();
        mapping.setId("mapping-001");
        mapping.setTenantId("tenant-a");
        mapping.setLogicalViewId("view-001");
        mapping.setTargetObjectType("TABLE");
        mapping.setTargetObjectKey(targetObjectKey);
        mapping.setTargetObjectName(targetObjectName);
        mapping.setMappingRole("SOURCE");
        return mapping;
    }
}
