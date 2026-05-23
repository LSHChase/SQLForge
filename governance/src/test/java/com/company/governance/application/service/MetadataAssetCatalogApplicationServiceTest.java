package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.governance.application.controller.vo.MetadataSchemaAssetVO;
import com.company.governance.application.controller.vo.MetadataTableAssetVO;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.infrastructure.repository.InMemoryMetadataSnapshotRepository;
import com.company.sqlforge.common.context.RequestContext;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MetadataAssetCatalogApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldListSchemaAndTableAssetsFromSnapshots() {
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "hetu_main", "USE")).thenReturn(true);
        MetadataAssetCatalogApplicationService service = new MetadataAssetCatalogApplicationService(
            new InMemoryMetadataSnapshotRepository(),
            tenantAccessLogic
        );
        RequestContext.set("tenant-a", "user-002", "request-030", "trace-030", "header", 1L, 2L);

        List<MetadataSchemaAssetVO> schemas = service.listSchemas("tenant-a", "hetu_main");
        List<MetadataTableAssetVO> tables = service.listTables("tenant-a", "hetu_main", "sales");

        assertEquals(2, schemas.size());
        assertEquals("analytics", schemas.get(0).getSchemaName());
        assertEquals("AT_RISK", schemas.get(0).getSlaStatus());
        assertEquals(1, tables.size());
        assertEquals("orders", tables.get(0).getTableName());
        assertEquals(Long.valueOf(1280000L), tables.get(0).getRowCount());
    }

    @Test
    void shouldResolveSchemaAndTableDetails() {
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "hetu_main", "USE")).thenReturn(true);
        MetadataAssetCatalogApplicationService service = new MetadataAssetCatalogApplicationService(
            new InMemoryMetadataSnapshotRepository(),
            tenantAccessLogic
        );
        RequestContext.set("tenant-a", "user-002", "request-031", "trace-031", "header", 1L, 2L);

        MetadataSchemaAssetVO schema = service.findSchema("tenant-a", "hetu_main", "analytics");
        MetadataTableAssetVO table = service.findTable("tenant-a", "hetu_main", "sales", "orders");

        assertEquals("analytics", schema.getSchemaName());
        assertEquals(Integer.valueOf(1), schema.getDbViewCount());
        assertEquals("TABLE:sales.orders", table.getObjectKey());
        assertEquals(Integer.valueOf(1), table.getDownstreamCount());
    }
}
