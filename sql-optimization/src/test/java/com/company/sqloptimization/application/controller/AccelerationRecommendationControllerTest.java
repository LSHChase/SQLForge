package com.company.sqloptimization.application.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqloptimization.application.controller.vo.AccelerationRecommendationVO;
import com.company.sqloptimization.application.service.AccelerationRecommendationApplicationService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AccelerationRecommendationController.class)
class AccelerationRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccelerationRecommendationApplicationService recommendationApplicationService;

    @Test
    void shouldExposeRecommendationListAndDetailEndpoints() throws Exception {
        AccelerationRecommendationVO recommendation = new AccelerationRecommendationVO();
        recommendation.setRecommendationId("rec-001");
        recommendation.setTenantId("tenant-a");
        recommendation.setRecommendationType("PREWARM");
        recommendation.setRecommendedSqlText("INSERT INTO agg_sales SELECT * FROM sales");
        recommendation.setExpectedGain("Warms report table before peak window");
        recommendation.setBenefitLevel("HIGH");
        recommendation.setRiskLevel("LOW");
        recommendation.setRequiresDispatch(Boolean.TRUE);
        recommendation.setStatus("RECOMMENDED");
        when(recommendationApplicationService.listRecommendations()).thenReturn(Collections.singletonList(recommendation));
        when(recommendationApplicationService.getRecommendation("rec-001")).thenReturn(recommendation);

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/recommendations")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].recommendationType").value("PREWARM"))
            .andExpect(jsonPath("$[0].benefitLevel").value("HIGH"))
            .andExpect(jsonPath("$[0].riskLevel").value("LOW"));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/recommendations/rec-001")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendationId").value("rec-001"))
            .andExpect(jsonPath("$.requiresDispatch").value(true));
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
            .header(RequestHeaderConstants.ACCESS_CHANNEL, "api")
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }
}
