package com.company.sqloptimization.application.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class OptimizationTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GovernanceCapabilityClient governanceCapabilityClient;

    @Test
    void shouldSubmitTaskAndPollSucceededStatus() throws Exception {
        MvcResult submitResult = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/tasks"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"REWRITE\",\"sqlText\":\"SELECT * FROM orders\","
                    + "\"datasourceType\":\"HETU\",\"taskContext\":{\"priority\":\"HIGH\",\"parseDepth\":\"DEEP\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andExpect(jsonPath("$.currentPhase").value("SUBMITTED"))
            .andExpect(jsonPath("$.contractStage").value("LONG_TERM_BASELINE"))
            .andExpect(jsonPath("$.implementationStage").value("ACCELERATION_PLAN_GOVERNANCE_BASELINE"))
            .andExpect(header().exists(RequestHeaderConstants.TRACE_ID))
            .andReturn();

        String taskId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.taskId");

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/tasks/{taskId}", taskId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("QUEUED"));

        waitForTerminalStatus(taskId, "SUCCEEDED");
    }

    @Test
    void shouldExposeFailedPlaceholderStatusWhenFailureMarkerIsPresent() throws Exception {
        MvcResult submitResult = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/tasks"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"ACCELERATION_SUGGESTION\","
                    + "\"sqlText\":\"SELECT * FROM orders /*FAIL_OPTIMIZATION*/\",\"datasourceType\":\"HETU\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andReturn();

        String taskId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.taskId");
        waitForTerminalStatus(taskId, "FAILED");
    }

    @Test
    void shouldReturnNotFoundForUnknownTaskId() throws Exception {
        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/tasks/{taskId}", "missing-task")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(22001));
    }

    @Test
    void shouldRejectInvalidCallbackUrl() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/tasks"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"PARSE\",\"sqlText\":\"SELECT 1\","
                    + "\"datasourceType\":\"HETU\",\"taskContext\":{\"callbackUrl\":\"ftp://callback\"}}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(22000));
    }

    @Test
    void shouldRejectMissingProtectedHeaders() throws Exception {
        mockMvc.perform(post("/api/sql-optimization/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"PARSE\",\"sqlText\":\"SELECT 1\",\"datasourceType\":\"HETU\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    void shouldRejectCrossTenantAccess() throws Exception {
        MvcResult submitResult = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/tasks"), "tenant-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"REWRITE\",\"sqlText\":\"SELECT * FROM orders\","
                    + "\"datasourceType\":\"HETU\"}"))
            .andExpect(status().isOk())
            .andReturn();

        String taskId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.taskId");

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/tasks/{taskId}", taskId), "tenant-b"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(10003));
    }

    private void waitForTerminalStatus(String taskId, String expectedStatus) throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) {
            MvcResult result = mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/tasks/{taskId}", taskId)))
                .andExpect(status().isOk())
                .andReturn();
            String status = JsonTestUtils.readValue(result.getResponse().getContentAsString(), "$.status");
            if (expectedStatus.equals(status)) {
                if ("SUCCEEDED".equals(expectedStatus)) {
                    mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/tasks/{taskId}", taskId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.taskId").value(taskId))
                        .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                        .andExpect(jsonPath("$.currentPhase").value("FINISHED"))
                        .andExpect(jsonPath("$.suggestion.summary", containsString("语句解析成功")))
                        .andExpect(jsonPath("$.suggestion.primaryRecommendation", containsString("解析制品")))
                        .andExpect(jsonPath("$.suggestion.artifacts[0].category").value("REWRITTEN_SQL"))
                        .andExpect(jsonPath("$.suggestion.benefits[0].category").value("PLAN_SIMPLIFICATION"))
                        .andExpect(jsonPath("$.suggestion.costs[0].category").value("VALIDATION"))
                        .andExpect(jsonPath("$.suggestion.risks[0].category").value("SELECT_STAR"))
                        .andExpect(jsonPath("$.statusHistory[0].note").value("TASK_SUBMITTED"))
                        .andExpect(jsonPath("$.statusHistory[1].note").value("TASK_STARTED"));
                } else {
                    mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/tasks/{taskId}", taskId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("FAILED"))
                        .andExpect(jsonPath("$.currentPhase").value("FINISHED"))
                        .andExpect(jsonPath("$.failure.code").value(13000))
                        .andExpect(jsonPath("$.failure.retryable").value(true))
                        .andExpect(jsonPath("$.failure.failedPhase").value("DEEP_PARSING"))
                        .andExpect(jsonPath("$.failure.risks[0].category").value("PIPELINE_READINESS"));
                }
                return;
            }
            Thread.sleep(40L);
        }
        throw new AssertionError("Task did not reach expected status " + expectedStatus);
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder) {
        return addProtectedHeaders(builder, "tenant-a");
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder, String tenantId) {
        long now = System.currentTimeMillis();
        return builder
            .header(RequestHeaderConstants.TENANT_ID, tenantId)
            .header(RequestHeaderConstants.USER_ID, "user-001")
            .header(RequestHeaderConstants.REQUEST_ID, "request-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }
}
