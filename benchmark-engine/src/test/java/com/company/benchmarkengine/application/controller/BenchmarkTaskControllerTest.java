package com.company.benchmarkengine.application.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.benchmarkengine.BenchmarkEngineApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = BenchmarkEngineApplication.class)
@AutoConfigureMockMvc
class BenchmarkTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldSubmitTaskAndPollSucceededStatus() throws Exception {
        MvcResult submitResult = mockMvc.perform(post("/api/benchmark-engine/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"COMPARISON\",\"sqlText\":\"SELECT * FROM orders\","
                    + "\"taskContext\":{\"priority\":\"HIGH\",\"targetEngines\":[\"HETU\",\"HIVE\"],"
                    + "\"concurrency\":16,\"durationSeconds\":300,\"rampUpSeconds\":30,"
                    + "\"readonlyRequired\":true,\"shadowEnvironmentMode\":\"REQUIRED\","
                    + "\"thresholds\":[{\"metric\":\"QPS\",\"operator\":\"GREATER_THAN_OR_EQUAL\",\"targetValue\":120,"
                    + "\"severity\":\"CRITICAL\",\"description\":\"throughput\"}]}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andExpect(jsonPath("$.currentPhase").value("SUBMITTED"))
            .andExpect(jsonPath("$.statusQueryPath", startsWith("/api/benchmark-engine/tasks/")))
            .andExpect(jsonPath("$.contractStage").value("LONG_TERM_BASELINE"))
            .andExpect(jsonPath("$.implementationStage").value("ASYNC_TASK_API_SKELETON"))
            .andReturn();

        String taskId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.taskId");

        mockMvc.perform(get("/api/benchmark-engine/tasks/{taskId}", taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").value(taskId))
            .andExpect(jsonPath("$.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.currentPhase").value("FINISHED"))
            .andExpect(jsonPath("$.reportId", startsWith("report-")))
            .andExpect(jsonPath("$.targetEngines[0]").value("HETU"))
            .andExpect(jsonPath("$.shadowEnvironmentMode").value("REQUIRED"))
            .andExpect(jsonPath("$.implementationStage").value("ASYNC_TASK_API_SKELETON"));
    }

    @Test
    void shouldExposeFailedPlaceholderStatusWhenFailureMarkerIsPresent() throws Exception {
        MvcResult submitResult = mockMvc.perform(post("/api/benchmark-engine/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"BASELINE\","
                    + "\"sqlText\":\"SELECT * FROM orders /*FAIL_BENCHMARK*/\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andReturn();

        String taskId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.taskId");

        mockMvc.perform(get("/api/benchmark-engine/tasks/{taskId}", taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.currentPhase").value("FINISHED"))
            .andExpect(jsonPath("$.error.code").value(14000))
            .andExpect(jsonPath("$.error.retryable").value(true))
            .andExpect(jsonPath("$.error.message", containsString("压测引擎任务与报告模型已固化")));
    }

    @Test
    void shouldReturnNotFoundForUnknownTaskId() throws Exception {
        mockMvc.perform(get("/api/benchmark-engine/tasks/{taskId}", "missing-task"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(23001));
    }

    @Test
    void shouldRejectUnsafeIsolationPolicy() throws Exception {
        mockMvc.perform(post("/api/benchmark-engine/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"BASELINE\",\"sqlText\":\"SELECT 1\","
                    + "\"taskContext\":{\"readonlyRequired\":false}}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(23003));
    }
}
