package com.company.sqloptimization.application.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqloptimization.SqlOptimizationApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = SqlOptimizationApplication.class)
@AutoConfigureMockMvc
class OptimizationTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldSubmitTaskAndPollSucceededStatus() throws Exception {
        MvcResult submitResult = mockMvc.perform(post("/api/sql-optimization/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"REWRITE\",\"sqlText\":\"SELECT * FROM orders\","
                    + "\"datasourceType\":\"HETU\",\"taskContext\":{\"priority\":\"HIGH\",\"parseDepth\":\"DEEP\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andExpect(jsonPath("$.currentPhase").value("SUBMITTED"))
            .andExpect(jsonPath("$.contractStage").value("LONG_TERM_BASELINE"))
            .andExpect(jsonPath("$.implementationStage").value("ASYNC_TASK_API_SKELETON"))
            .andReturn();

        String taskId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.taskId");

        mockMvc.perform(get("/api/sql-optimization/tasks/{taskId}", taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").value(taskId))
            .andExpect(jsonPath("$.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.currentPhase").value("FINISHED"))
            .andExpect(jsonPath("$.summary", containsString("Rewrite suggestion placeholder completed")))
            .andExpect(jsonPath("$.statusHistory[0].note").value("TASK_SUBMITTED"))
            .andExpect(jsonPath("$.statusHistory[1].note").value("TASK_STARTED"));
    }

    @Test
    void shouldExposeFailedPlaceholderStatusWhenFailureMarkerIsPresent() throws Exception {
        MvcResult submitResult = mockMvc.perform(post("/api/sql-optimization/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"ACCELERATION_SUGGESTION\","
                    + "\"sqlText\":\"SELECT * FROM orders /*FAIL_OPTIMIZATION*/\",\"datasourceType\":\"HETU\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andReturn();

        String taskId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.taskId");

        mockMvc.perform(get("/api/sql-optimization/tasks/{taskId}", taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.currentPhase").value("FINISHED"))
            .andExpect(jsonPath("$.error.code").value(13000))
            .andExpect(jsonPath("$.error.retryable").value(true));
    }

    @Test
    void shouldReturnNotFoundForUnknownTaskId() throws Exception {
        mockMvc.perform(get("/api/sql-optimization/tasks/{taskId}", "missing-task"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(22001));
    }

    @Test
    void shouldRejectInvalidCallbackUrl() throws Exception {
        mockMvc.perform(post("/api/sql-optimization/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"PARSE\",\"sqlText\":\"SELECT 1\","
                    + "\"datasourceType\":\"HETU\",\"taskContext\":{\"callbackUrl\":\"ftp://callback\"}}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(22000));
    }
}
