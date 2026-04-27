package com.company.sqloptimization.application.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqloptimization.application.controller.vo.DispatchEventVO;
import com.company.sqloptimization.application.service.DispatchEventApplicationService;
import com.company.sqloptimization.domain.dispatch.DispatchEventStatus;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(DispatchEventController.class)
class DispatchEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DispatchEventApplicationService dispatchEventApplicationService;

    @Test
    void shouldExposeDispatchEventLifecycleEndpoints() throws Exception {
        DispatchEventVO event = event("dispatch-001", "PUBLISHED");
        DispatchEventVO pulled = event("dispatch-001", "PULLED");
        DispatchEventVO acked = event("dispatch-001", "ACKED");
        when(dispatchEventApplicationService.dispatchRecommendation("rec-001", null)).thenReturn(event);
        when(dispatchEventApplicationService.listEvents(DispatchEventStatus.PUBLISHED)).thenReturn(Collections.singletonList(event));
        when(dispatchEventApplicationService.getEvent("dispatch-001")).thenReturn(event);
        when(dispatchEventApplicationService.markPulled("dispatch-001")).thenReturn(pulled);
        when(dispatchEventApplicationService.ack("dispatch-001", null)).thenReturn(acked);

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/recommendations/rec-001/dispatch")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/dispatch-events").param("status", "PUBLISHED")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].dispatchEventId").value("dispatch-001"));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/dispatch-events/dispatch-001")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dispatchType").value("PREWARM_SQL"));

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/dispatch-events/dispatch-001/pull")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PULLED"));

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/dispatch-events/dispatch-001/ack")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACKED"));
    }

    private DispatchEventVO event(String eventId, String status) {
        DispatchEventVO vo = new DispatchEventVO();
        vo.setDispatchEventId(eventId);
        vo.setTenantId("tenant-a");
        vo.setRecommendationId("rec-001");
        vo.setDispatchType("PREWARM_SQL");
        vo.setDispatchPayloadJson("{\"recommendationId\":\"rec-001\"}");
        vo.setTargetEngine("trino");
        vo.setTargetDatasource("hetu_main");
        vo.setStatus(status);
        return vo;
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder) {
        long now = System.currentTimeMillis();
        return builder
            .header(RequestHeaderConstants.TENANT_ID, "tenant-a")
            .header(RequestHeaderConstants.USER_ID, "operator-001")
            .header(RequestHeaderConstants.ROLE_CODES, "TENANT_ADMIN,OPERATOR")
            .header(RequestHeaderConstants.REQUEST_ID, "request-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }
}
