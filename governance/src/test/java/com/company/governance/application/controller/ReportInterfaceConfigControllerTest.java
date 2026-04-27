package com.company.governance.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.governance.application.service.ReportInterfaceConfigApplicationService;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigResponse;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ReportInterfaceConfigControllerTest {

    @Test
    void shouldExposeReportInterfacesAliasEndpoints() throws Exception {
        ReportInterfaceConfigApplicationService service = mock(ReportInterfaceConfigApplicationService.class);
        GovernanceReportInterfaceConfigResponse response = sampleResponse();
        when(service.list("tenant-a")).thenReturn(Collections.singletonList(response));
        when(service.upsert(any())).thenReturn(response);
        when(service.update(org.mockito.Mockito.eq("if-001"), any())).thenReturn(response);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ReportInterfaceConfigController(service)).build();

        mockMvc.perform(get("/api/governance/report-interfaces").param("tenantId", "tenant-a"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].endpointCode").value("report-api-main"));

        mockMvc.perform(post("/api/governance/report-interfaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"endpointCode\":\"report-api-main\",\"baseUrl\":\"http://report-api.local\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resolverStatus").value("ACTIVE"));

        mockMvc.perform(put("/api/governance/report-interfaces/if-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"endpointCode\":\"report-api-main\",\"baseUrl\":\"http://report-api.local\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.endpointName").value("Report SQL API"));
    }

    private GovernanceReportInterfaceConfigResponse sampleResponse() {
        GovernanceReportInterfaceConfigResponse response = new GovernanceReportInterfaceConfigResponse();
        response.setTenantId("tenant-a");
        response.setEndpointCode("report-api-main");
        response.setEndpointName("Report SQL API");
        response.setResolverStatus("ACTIVE");
        return response;
    }
}
