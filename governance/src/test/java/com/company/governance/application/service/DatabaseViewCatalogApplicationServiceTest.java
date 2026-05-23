package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.governance.domain.dbview.entity.DatabaseViewDependency;
import com.company.governance.domain.dbview.entity.DatabaseViewRef;
import com.company.governance.domain.dbview.repository.DatabaseViewDependencyRepository;
import com.company.governance.domain.dbview.repository.DatabaseViewRepository;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.infrastructure.repository.InMemoryMetadataSnapshotRepository;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DatabaseViewCatalogApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldResolveDbViewDependencies() {
        DatabaseViewRepository databaseViewRepository = mock(DatabaseViewRepository.class);
        DatabaseViewDependencyRepository databaseViewDependencyRepository = mock(DatabaseViewDependencyRepository.class);
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        DatabaseViewCatalogApplicationService service = new DatabaseViewCatalogApplicationService(
            databaseViewRepository,
            databaseViewDependencyRepository,
            new InMemoryMetadataSnapshotRepository(),
            tenantAccessLogic
        );
        RequestContext.set("tenant-a", "service-user", "req-1", "trace-1", "header", 1L, 2L);
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "hetu_main", "USE")).thenReturn(true);
        when(databaseViewRepository.findByTenantDatasourceAndViewName("tenant-a", "hetu_main", "vw_sales_daily"))
            .thenReturn(Optional.of(buildDbView()));
        when(databaseViewDependencyRepository.findByTenantAndDbViewId("tenant-a", "dbview-001"))
            .thenReturn(Collections.singletonList(buildDependency()));

        GovernanceDbViewResolveRequest request = new GovernanceDbViewResolveRequest();
        request.setTenantId("tenant-a");
        request.setDatasourceCode("hetu_main");
        request.setViewName("vw_sales_daily");

        assertEquals("DB_VIEW:vw_sales_daily", service.resolveDbView(request).getObjectKey());
        assertEquals(1, service.resolveDbView(request).getDependencies().size());
    }

    @Test
    void shouldRejectCrossTenantDbViewResolution() {
        DatabaseViewCatalogApplicationService service = new DatabaseViewCatalogApplicationService(
            mock(DatabaseViewRepository.class),
            mock(DatabaseViewDependencyRepository.class),
            new InMemoryMetadataSnapshotRepository(),
            mock(TenantAccessLogic.class)
        );
        RequestContext.set("tenant-a", "service-user", "req-1", "trace-1", "header", 1L, 2L);
        GovernanceDbViewResolveRequest request = new GovernanceDbViewResolveRequest();
        request.setTenantId("tenant-b");
        request.setDatasourceCode("hetu_main");
        request.setViewName("vw_sales_daily");

        try {
            service.resolveDbView(request);
        } catch (BizException ex) {
            assertEquals(20001, ex.getCode());
            return;
        }
        throw new AssertionError("Expected cross-tenant db view resolve to be rejected");
    }

    private DatabaseViewRef buildDbView() {
        DatabaseViewRef ref = new DatabaseViewRef();
        ref.setId("dbview-001");
        ref.setTenantId("tenant-a");
        ref.setDatasourceCode("hetu_main");
        ref.setViewName("vw_sales_daily");
        ref.setObjectKey("DB_VIEW:vw_sales_daily");
        ref.setQueryable(Boolean.TRUE);
        return ref;
    }

    private DatabaseViewDependency buildDependency() {
        DatabaseViewDependency dependency = new DatabaseViewDependency();
        dependency.setId("dep-001");
        dependency.setTenantId("tenant-a");
        dependency.setDbViewId("dbview-001");
        dependency.setDependencyObjectType("TABLE");
        dependency.setDependencyObjectKey("TABLE:sales.orders");
        dependency.setDependencyObjectName("sales.orders");
        return dependency;
    }
}
