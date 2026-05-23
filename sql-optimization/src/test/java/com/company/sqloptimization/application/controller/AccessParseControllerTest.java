package com.company.sqloptimization.application.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqloptimization.SqlOptimizationApplication;
import com.company.sqloptimization.SqlOptimizationTestPersistenceConfiguration;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(classes = {SqlOptimizationApplication.class, SqlOptimizationTestPersistenceConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccessParseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GovernanceCapabilityClient governanceCapabilityClient;

    @Test
    void shouldReturnUnavailableAccessParseWhenDatasourceIsMissing() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/access"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT 1\",\"connectionRequired\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parseType").value("ACCESS"))
            .andExpect(jsonPath("$.serviceStatus").value("UNAVAILABLE"))
            .andExpect(jsonPath("$.connectionStatus").value("UNAVAILABLE"))
            .andExpect(jsonPath("$.degradeReason").value("DATASOURCE_CODE_MISSING"));
    }

    @Test
    void shouldTriggerAsyncAccessParseAfterStructureSuccess() throws Exception {
        MvcResult submitResult = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/combined"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT * FROM orders WHERE dt = '2026-04-01'\",\"datasourceCode\":\"hetu_main\",\"connectionRequired\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACCESS_PARSING"))
            .andExpect(jsonPath("$.structureParse.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.conclusion.overallStatus").value("WAITING"))
            .andExpect(jsonPath("$.statusHistory[0].status").value("STRUCTURE_SUCCEEDED"))
            .andExpect(jsonPath("$.statusHistory[1].status").value("ACCESS_PARSING"))
            .andReturn();

        String parseTaskId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.parseTaskId");

        waitForCombinedStatus(parseTaskId, "ACCESS_SUCCEEDED");
    }

    private void waitForCombinedStatus(String parseTaskId, String expectedStatus) throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) {
            MvcResult result = mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/parse/{parseTaskId}", parseTaskId)))
                .andExpect(status().isOk())
                .andReturn();
            String status = JsonTestUtils.readValue(result.getResponse().getContentAsString(), "$.status");
            if (expectedStatus.equals(status)) {
                mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/parse/{parseTaskId}", parseTaskId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conclusion.overallStatus").value("SUCCESS"))
                    .andExpect(jsonPath("$.accessParse.parseType").value("ACCESS"))
                    .andExpect(jsonPath("$.accessParse.serviceStatus").value("AVAILABLE"))
                    .andExpect(jsonPath("$.accessParse.connectionStatus").value("CONNECTED"))
                    .andExpect(jsonPath("$.statusHistory[2].status").value("ACCESS_SUCCEEDED"));
                return;
            }
            Thread.sleep(40L);
        }
        throw new AssertionError("Combined parse did not reach expected status " + expectedStatus);
    }

    @Test
    void shouldExposePartialSuccessWhenAccessParseIsUnavailable() throws Exception {
        MvcResult submitResult = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/combined"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT * FROM orders WHERE dt = '2026-04-01'\",\"connectionRequired\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACCESS_PARSING"))
            .andReturn();

        String parseTaskId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.parseTaskId");

        for (int attempt = 0; attempt < 20; attempt++) {
            MvcResult result = mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/parse/{parseTaskId}", parseTaskId)))
                .andExpect(status().isOk())
                .andReturn();
            String status = JsonTestUtils.readValue(result.getResponse().getContentAsString(), "$.status");
            if ("PARTIAL_SUCCEEDED".equals(status)) {
                mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/parse/{parseTaskId}", parseTaskId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conclusion.overallStatus").value("PARTIAL_SUCCESS"))
                    .andExpect(jsonPath("$.accessParse.serviceStatus").value("UNAVAILABLE"))
                    .andExpect(jsonPath("$.degradeReason").value("DATASOURCE_CODE_MISSING"))
                    .andExpect(jsonPath("$.statusHistory[2].status").value("PARTIAL_SUCCEEDED"));
                return;
            }
            Thread.sleep(40L);
        }
        throw new AssertionError("Combined parse did not reach expected status PARTIAL_SUCCEEDED");
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder) {
        long now = System.currentTimeMillis();
        return builder
            .header(RequestHeaderConstants.TENANT_ID, "tenant-a")
            .header(RequestHeaderConstants.USER_ID, "user-001")
            .header(RequestHeaderConstants.REQUEST_ID, "request-access-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-access-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }
}
