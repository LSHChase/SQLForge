package com.company.governance.application.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.governance.application.controller.vo.BusinessLogicalViewVO;
import com.company.governance.application.controller.vo.LogicalObjectMappingVO;
import com.company.governance.application.service.LogicalViewCatalogApplicationService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LogicalViewCatalogControllerTest {

    @Test
    void shouldReturnLogicalViewListContract() throws Exception {
        LogicalViewCatalogApplicationService service = mock(LogicalViewCatalogApplicationService.class);
        when(service.listLogicalViews("tenant-a", "hetu_main"))
            .thenReturn(Collections.singletonList(buildView()));
        LogicalViewCatalogController controller = new LogicalViewCatalogController(service);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/governance/logical-views")
                .param("tenantId", "tenant-a")
                .param("datasourceCode", "hetu_main"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].viewCode").value("RPT_SALES_DAILY"))
            .andExpect(jsonPath("$[0].physicalTargets[0].targetObjectKey").value("TABLE:sales.orders"));
    }

    @Test
    void shouldReturnLogicalViewDetailContract() throws Exception {
        LogicalViewCatalogApplicationService service = mock(LogicalViewCatalogApplicationService.class);
        when(service.findLogicalView("tenant-a", "RPT_SALES_DAILY"))
            .thenReturn(buildView());
        LogicalViewCatalogController controller = new LogicalViewCatalogController(service);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/governance/logical-views/RPT_SALES_DAILY")
                .param("tenantId", "tenant-a"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.viewName").value("Sales Daily"))
            .andExpect(jsonPath("$.physicalTargets[0].mappingRole").value("SOURCE"));
    }

    private BusinessLogicalViewVO buildView() {
        LogicalObjectMappingVO mapping = new LogicalObjectMappingVO();
        mapping.setTargetObjectType("TABLE");
        mapping.setTargetObjectKey("TABLE:sales.orders");
        mapping.setTargetObjectName("sales.orders");
        mapping.setMappingRole("SOURCE");

        BusinessLogicalViewVO view = new BusinessLogicalViewVO();
        view.setViewId("view-001");
        view.setTenantId("tenant-a");
        view.setViewCode("RPT_SALES_DAILY");
        view.setViewName("Sales Daily");
        view.setDatasourceCode("hetu_main");
        view.setPhysicalTargets(Collections.singletonList(mapping));
        return view;
    }
}
