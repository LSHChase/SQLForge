package com.company.benchmarkengine.application.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.benchmarkengine.BenchmarkEngineApplication;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationAccelerationRecommendation;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationRecommendationClient;
import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
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

@SpringBootTest(classes = BenchmarkEngineApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BenchmarkTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GovernanceCapabilityClient governanceCapabilityClient;

    @MockBean
    private SqlOptimizationRecommendationClient sqlOptimizationRecommendationClient;

    @Test
    void shouldSubmitTaskAndPollSucceededStatus() throws Exception {
        MvcResult submitResult = mockMvc.perform(addProtectedHeaders(post("/api/benchmark-engine/tasks"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"COMPARISON\",\"sqlText\":\"SELECT * FROM orders\","
                    + "\"taskContext\":{\"priority\":\"HIGH\",\"targetEngines\":[\"HETU\",\"HIVE\"],"
                    + "\"concurrency\":16,\"durationSeconds\":300,\"rampUpSeconds\":30,"
                    + "\"templateId\":\"comparison-dual-engine\",\"templateType\":\"CROSS_ENGINE_COMPARISON\","
                    + "\"templateVersion\":\"v2026.04\",\"testSetId\":\"set-route-comparison\","
                    + "\"testSetSource\":\"RECOMMENDATION_GENERATION\","
                    + "\"testSetLabels\":[{\"type\":\"SCENARIO\",\"value\":\"COMPARISON\"},"
                    + "{\"type\":\"DOMAIN\",\"value\":\"ROUTE_GOVERNANCE\"}],"
                    + "\"testSetSourceRefs\":[{\"type\":\"RECOMMENDATION\",\"referenceId\":\"rec-001\"},"
                    + "{\"type\":\"SQL_FINGERPRINT\",\"referenceId\":\"fp-001\"}],"
                    + "\"readonlyRequired\":true,\"shadowEnvironmentMode\":\"REQUIRED\","
                    + "\"thresholds\":[{\"metric\":\"QPS\",\"operator\":\"GREATER_THAN_OR_EQUAL\",\"targetValue\":120,"
                    + "\"severity\":\"CRITICAL\",\"description\":\"throughput\"}]}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andExpect(jsonPath("$.currentPhase").value("SUBMITTED"))
            .andExpect(jsonPath("$.statusQueryPath", startsWith("/api/benchmark-engine/tasks/")))
            .andExpect(jsonPath("$.contractStage").value("LONG_TERM_BASELINE"))
            .andExpect(jsonPath("$.implementationStage").value("EXTERNALIZED_ARTIFACT_GOVERNANCE_TRACE_BASELINE"))
            .andExpect(header().exists(RequestHeaderConstants.TRACE_ID))
            .andReturn();

        String taskId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.taskId");

        mockMvc.perform(addProtectedHeaders(get("/api/benchmark-engine/tasks/{taskId}", taskId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("QUEUED"));

        waitForTerminalStatus(taskId, "SUCCEEDED");
    }

    @Test
    void shouldExposeFailedWorkerStatusWhenFailureMarkerIsPresent() throws Exception {
        MvcResult submitResult = mockMvc.perform(addProtectedHeaders(post("/api/benchmark-engine/tasks"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"BASELINE\","
                    + "\"sqlText\":\"SELECT * FROM orders /*FAIL_BENCHMARK*/\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andReturn();

        String taskId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.taskId");
        waitForTerminalStatus(taskId, "FAILED");
    }

    @Test
    void shouldReturnNotFoundForUnknownTaskId() throws Exception {
        mockMvc.perform(addProtectedHeaders(get("/api/benchmark-engine/tasks/{taskId}", "missing-task")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(23001));
    }

    @Test
    void shouldRejectUnsafeIsolationPolicy() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/benchmark-engine/tasks"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"BASELINE\",\"sqlText\":\"SELECT 1\","
                    + "\"taskContext\":{\"readonlyRequired\":false}}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(23003));
    }

    @Test
    void shouldRejectMismatchedTemplateContract() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/benchmark-engine/tasks"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"BASELINE\",\"sqlText\":\"SELECT 1\","
                    + "\"taskContext\":{\"templateType\":\"CROSS_ENGINE_COMPARISON\","
                    + "\"targetEngines\":[\"HETU\"]}}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingProtectedHeaders() throws Exception {
        mockMvc.perform(post("/api/benchmark-engine/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"BASELINE\",\"sqlText\":\"SELECT 1\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    void shouldRejectCrossTenantTaskAccess() throws Exception {
        MvcResult submitResult = mockMvc.perform(addProtectedHeaders(post("/api/benchmark-engine/tasks"), "tenant-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"BASELINE\",\"sqlText\":\"SELECT * FROM orders\"}"))
            .andExpect(status().isOk())
            .andReturn();

        String taskId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.taskId");

        mockMvc.perform(addProtectedHeaders(get("/api/benchmark-engine/tasks/{taskId}", taskId), "tenant-b"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(10003));
    }

    @Test
    void shouldSubmitRecommendationComparisonBenchmark() throws Exception {
        org.mockito.Mockito.when(sqlOptimizationRecommendationClient.getRecommendation("rec-001"))
            .thenReturn(recommendation("rec-001", "SELECT * FROM orders", "SELECT order_id, total_amount FROM orders"));

        mockMvc.perform(addProtectedHeaders(post("/api/benchmark-engine/tasks/recommendations/{recommendationId}/comparison", "rec-001"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"targetEngines\":[\"HETU\",\"HIVE\"],"
                    + "\"benchmarkSqlRole\":\"RECOMMENDED_SQL\",\"concurrency\":12,\"durationSeconds\":180}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendationId").value("rec-001"))
            .andExpect(jsonPath("$.benchmarkSqlRole").value("RECOMMENDED_SQL"))
            .andExpect(jsonPath("$.testSet.testSetSource").value("RECOMMENDATION_GENERATION"))
            .andExpect(jsonPath("$.testSet.totalCases").value(2))
            .andExpect(jsonPath("$.benchmarkTask.status").value("QUEUED"))
            .andExpect(jsonPath("$.benchmarkTask.statusQueryPath", startsWith("/api/benchmark-engine/tasks/")))
            .andExpect(jsonPath("$.implementationStage").value("RECOMMENDATION_COMPARISON_ORCHESTRATION_BASELINE"));
    }

    @Test
    void shouldRejectRecommendationComparisonWithoutEnoughTargetEngines() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/benchmark-engine/tasks/recommendations/{recommendationId}/comparison", "rec-001"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"targetEngines\":[\"HETU\"]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(10001));
    }

    private void waitForTerminalStatus(String taskId, String expectedStatus) throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) {
            MvcResult result = mockMvc.perform(addProtectedHeaders(get("/api/benchmark-engine/tasks/{taskId}", taskId)))
                .andExpect(status().isOk())
                .andReturn();
            String status = JsonTestUtils.readValue(result.getResponse().getContentAsString(), "$.status");
            if (expectedStatus.equals(status)) {
                if ("SUCCEEDED".equals(expectedStatus)) {
                    mockMvc.perform(addProtectedHeaders(get("/api/benchmark-engine/tasks/{taskId}", taskId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.taskId").value(taskId))
                        .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                        .andExpect(jsonPath("$.currentPhase").value("FINISHED"))
                        .andExpect(jsonPath("$.reportId", startsWith("report-")))
                        .andExpect(jsonPath("$.targetEngines[0]").value("HETU"))
                        .andExpect(jsonPath("$.templateId").value("comparison-dual-engine"))
                        .andExpect(jsonPath("$.templateType").value("CROSS_ENGINE_COMPARISON"))
                        .andExpect(jsonPath("$.testSetId").value("set-route-comparison"))
                        .andExpect(jsonPath("$.testSetSource").value("RECOMMENDATION_GENERATION"))
                        .andExpect(jsonPath("$.testSetLabels[0].type").value("SCENARIO"))
                        .andExpect(jsonPath("$.testSetSourceRefs[0].type").value("RECOMMENDATION"))
                        .andExpect(jsonPath("$.shadowEnvironmentMode").value("REQUIRED"))
                        .andExpect(jsonPath("$.implementationStage").value("EXTERNALIZED_ARTIFACT_GOVERNANCE_TRACE_BASELINE"));
                } else {
                    mockMvc.perform(addProtectedHeaders(get("/api/benchmark-engine/tasks/{taskId}", taskId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("FAILED"))
                        .andExpect(jsonPath("$.currentPhase").value("FINISHED"))
                        .andExpect(jsonPath("$.error.code").value(14000))
                        .andExpect(jsonPath("$.error.retryable").value(true))
                        .andExpect(jsonPath("$.error.message", containsString("报告写回完成")));
                }
                return;
            }
            Thread.sleep(40L);
        }
        throw new AssertionError("Benchmark task did not reach expected status " + expectedStatus);
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder) {
        return addProtectedHeaders(builder, "tenant-a");
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder, String tenantId) {
        long now = System.currentTimeMillis();
        return builder
            .header(RequestHeaderConstants.TENANT_ID, tenantId)
            .header(RequestHeaderConstants.USER_ID, "operator-001")
            .header(RequestHeaderConstants.ROLE_CODES, "TENANT_ADMIN,OPERATOR")
            .header(RequestHeaderConstants.REQUEST_ID, "request-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }

    private SqlOptimizationAccelerationRecommendation recommendation(String recommendationId,
                                                                     String sourceSqlText,
                                                                     String recommendedSqlText) {
        SqlOptimizationAccelerationRecommendation recommendation = new SqlOptimizationAccelerationRecommendation();
        recommendation.setRecommendationId(recommendationId);
        recommendation.setTenantId("tenant-a");
        recommendation.setRecommendationType("REWRITE");
        recommendation.setHistoryId("history-001");
        recommendation.setParseTaskId("parse-001");
        recommendation.setSqlFingerprint("fp-source-001");
        recommendation.setSourceSqlText(sourceSqlText);
        recommendation.setRecommendedSqlText(recommendedSqlText);
        recommendation.setTargetEngine("HIVE");
        recommendation.setTargetDatasource("HIVE");
        recommendation.setReportCode("report-001");
        recommendation.setLogicalObjectKey("agg.orders");
        recommendation.setExpectedGain("Lower scanned rows");
        recommendation.setBenefitLevel("HIGH");
        recommendation.setRiskLevel("LOW");
        recommendation.setStatus("RECOMMENDED");
        recommendation.setRequiresDispatch(Boolean.FALSE);
        return recommendation;
    }
}
