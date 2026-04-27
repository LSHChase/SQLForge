package com.company.governance.application.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.governance.application.controller.vo.MetadataSchemaAssetVO;
import com.company.governance.application.controller.vo.MetadataTableAssetVO;
import com.company.governance.application.service.MetadataAssetCatalogApplicationService;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MetadataAssetCatalogControllerTest {

    @Test
    void shouldExposeSchemaAndTableEndpoints() throws Exception {
        MetadataAssetCatalogApplicationService service = mock(MetadataAssetCatalogApplicationService.class);
        when(service.listSchemas("tenant-a", "hetu_main")).thenReturn(Collections.singletonList(buildSchema()));
        when(service.findTable("tenant-a", "hetu_main", "sales", "orders")).thenReturn(buildTable());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MetadataAssetCatalogController(service)).build();

        mockMvc.perform(get("/api/governance/metadata/schemas")
                .param("tenantId", "tenant-a")
                .param("datasourceCode", "hetu_main"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].schemaName").value("analytics"))
            .andExpect(jsonPath("$[0].logicalViewCount").value(1));

        mockMvc.perform(get("/api/governance/metadata/tables/orders")
                .param("tenantId", "tenant-a")
                .param("datasourceCode", "hetu_main")
                .param("schemaName", "sales"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.objectKey").value("TABLE:sales.orders"))
            .andExpect(jsonPath("$.rowCount").value(1280000))
            .andExpect(jsonPath("$.downstreamCount").value(1));

        verify(service).listSchemas("tenant-a", "hetu_main");
        verify(service).findTable("tenant-a", "hetu_main", "sales", "orders");
    }

    private MetadataSchemaAssetVO buildSchema() {
        MetadataSchemaAssetVO schema = new MetadataSchemaAssetVO();
        schema.setTenantId("tenant-a");
        schema.setDatasourceCode("hetu_main");
        schema.setCatalogName("lakehouse");
        schema.setSchemaName("analytics");
        schema.setLogicalViewCount(Integer.valueOf(1));
        schema.setContractStage("LONG_TERM_BASELINE");
        return schema;
    }

    private MetadataTableAssetVO buildTable() {
        MetadataTableAssetVO table = new MetadataTableAssetVO();
        table.setTenantId("tenant-a");
        table.setDatasourceCode("hetu_main");
        table.setSchemaName("sales");
        table.setTableName("orders");
        table.setObjectKey("TABLE:sales.orders");
        table.setRowCount(Long.valueOf(1280000L));
        table.setDownstreamCount(Integer.valueOf(1));
        table.setLatestSnapshotTime(Instant.parse("2026-04-27T03:05:00Z"));
        return table;
    }
}
