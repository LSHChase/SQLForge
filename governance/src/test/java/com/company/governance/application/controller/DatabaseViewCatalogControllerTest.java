package com.company.governance.application.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.governance.application.controller.vo.DatabaseViewDependencyVO;
import com.company.governance.application.controller.vo.DatabaseViewRefVO;
import com.company.governance.application.service.DatabaseViewCatalogApplicationService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DatabaseViewCatalogControllerTest {

    @Test
    void shouldReturnDbViewListContract() throws Exception {
        DatabaseViewCatalogApplicationService service = mock(DatabaseViewCatalogApplicationService.class);
        when(service.listDbViews("tenant-a", "hetu_main")).thenReturn(Collections.singletonList(buildView()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DatabaseViewCatalogController(service)).build();

        mockMvc.perform(get("/api/governance/db-views")
                .param("tenantId", "tenant-a")
                .param("datasourceCode", "hetu_main"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].viewName").value("vw_sales_daily"))
            .andExpect(jsonPath("$[0].dependencies[0].dependencyObjectKey").value("TABLE:sales.orders"));
    }

    @Test
    void shouldReturnDbViewDetailContract() throws Exception {
        DatabaseViewCatalogApplicationService service = mock(DatabaseViewCatalogApplicationService.class);
        when(service.findDbView("tenant-a", "hetu_main", "vw_sales_daily")).thenReturn(buildView());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DatabaseViewCatalogController(service)).build();

        mockMvc.perform(get("/api/governance/db-views/vw_sales_daily")
                .param("tenantId", "tenant-a")
                .param("datasourceCode", "hetu_main"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.objectKey").value("DB_VIEW:vw_sales_daily"))
            .andExpect(jsonPath("$.dependencies[0].dependencyObjectName").value("sales.orders"));
    }

    private DatabaseViewRefVO buildView() {
        DatabaseViewDependencyVO dependency = new DatabaseViewDependencyVO();
        dependency.setDependencyObjectType("TABLE");
        dependency.setDependencyObjectKey("TABLE:sales.orders");
        dependency.setDependencyObjectName("sales.orders");

        DatabaseViewRefVO view = new DatabaseViewRefVO();
        view.setViewId("dbview-001");
        view.setTenantId("tenant-a");
        view.setDatasourceCode("hetu_main");
        view.setViewName("vw_sales_daily");
        view.setObjectKey("DB_VIEW:vw_sales_daily");
        view.setDependencies(Collections.singletonList(dependency));
        return view;
    }
}
