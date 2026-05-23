package com.company.sqloptimization.application.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqloptimization.application.controller.vo.RecommendationTraceVO;
import com.company.sqloptimization.application.service.RecommendationTraceApplicationService;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(RecommendationTraceController.class)
class RecommendationTraceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendationTraceApplicationService recommendationTraceApplicationService;

    @Test
    void shouldExposeRecommendationTraceEndpoint() throws Exception {
        RecommendationTraceVO trace = new RecommendationTraceVO();
        trace.setRecommendationId("rec-001");
        trace.setTenantId("tenant-a");
        trace.setHistoryId("history-001");
        trace.setParseTaskId("parse-task-001");
        trace.setBatchId("batch-001");
        trace.setRouteDecisionId("route-001");
        trace.setAlertId("alert-001");
        trace.setDispatchEvents(Collections.emptyList());
        LinkedHashMap<String, Object> refs = new LinkedHashMap<String, Object>();
        refs.put("historyId", "history-001");
        trace.setTraceRefs(refs);
        when(recommendationTraceApplicationService.trace("rec-001")).thenReturn(trace);

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/recommendations/rec-001/trace")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.historyId").value("history-001"))
            .andExpect(jsonPath("$.parseTaskId").value("parse-task-001"))
            .andExpect(jsonPath("$.traceRefs.historyId").value("history-001"));
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder) {
        long now = System.currentTimeMillis();
        return builder
            .header(RequestHeaderConstants.TENANT_ID, "tenant-a")
            .header(RequestHeaderConstants.USER_ID, "user-001")
            .header(RequestHeaderConstants.REQUEST_ID, "request-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }
}
