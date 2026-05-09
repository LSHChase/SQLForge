package com.company.sqloptimization.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanResponse;
import com.company.sqloptimization.SqlOptimizationApplication;
import com.company.sqloptimization.SqlOptimizationTestPersistenceConfiguration;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqloptimization.infrastructure.queryexecution.QueryExecutionAccelerationPlanClient;
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
class AccelerationPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GovernanceCapabilityClient governanceCapabilityClient;

    @MockBean
    private QueryExecutionAccelerationPlanClient queryExecutionAccelerationPlanClient;

    @Test
    void shouldExecuteGovernedAccelerationPlanLifecycle() throws Exception {
        doNothing().when(governanceCapabilityClient).assertAuthorization(any(), any(), any(), any(), any());
        doNothing().when(governanceCapabilityClient).writeAudit(any());
        GovernanceAccelerationPlanTraceResponse traceResponse = new GovernanceAccelerationPlanTraceResponse();
        traceResponse.setConfigSnapshotId("cfg-plan-001");
        traceResponse.setResultId("result-plan-001");
        traceResponse.setHistoryId("history-plan-001");
        when(governanceCapabilityClient.writeAccelerationPlanTrace(any())).thenReturn(traceResponse);
        when(queryExecutionAccelerationPlanClient.apply(any())).thenReturn(runtimeResponse("APPLIED", true));
        when(queryExecutionAccelerationPlanClient.verify(any())).thenReturn(runtimeResponse("VERIFIED", true));
        when(queryExecutionAccelerationPlanClient.rollback(any())).thenReturn(runtimeResponse("ROLLED_BACK", false));

        String taskId = createSucceededAccelerationTask();

        MvcResult submitResult = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/acceleration-plans"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"sourceTaskId\":\"" + taskId + "\"}"))
            .andExpect(status().isOk())
            .andExpect(header().exists(RequestHeaderConstants.TRACE_ID))
            .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
            .andExpect(jsonPath("$.implementationStage").value("ACCELERATION_PLAN_GOVERNANCE_BASELINE"))
            .andReturn();

        String planId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.planId");

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/acceleration-plans/{planId}", planId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configSnapshotId").value("cfg-plan-001"))
            .andExpect(jsonPath("$.selectedSuggestionTypes").isArray());

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/acceleration-plans/{planId}/approval", planId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approve\":true,\"reviewNote\":\"approve for runtime activation\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.approvedBy").value("operator-001"));

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/acceleration-plans/{planId}/apply", planId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"activate approved binding\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPLIED"))
            .andExpect(jsonPath("$.runtimeBindingBy").value("operator-001"));

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/acceleration-plans/{planId}/verify", planId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"verify runtime state\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("VERIFIED"))
            .andExpect(jsonPath("$.verifiedBy").value("operator-001"));

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/acceleration-plans/{planId}/rollback", planId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"deactivate binding\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ROLLED_BACK"))
            .andExpect(jsonPath("$.rolledBackBy").value("operator-001"));
    }

    @Test
    void shouldRejectApplyBeforeApproval() throws Exception {
        doNothing().when(governanceCapabilityClient).assertAuthorization(any(), any(), any(), any(), any());
        doNothing().when(governanceCapabilityClient).writeAudit(any());
        GovernanceAccelerationPlanTraceResponse traceResponse = new GovernanceAccelerationPlanTraceResponse();
        traceResponse.setConfigSnapshotId("cfg-plan-002");
        traceResponse.setResultId("result-plan-002");
        traceResponse.setHistoryId("history-plan-002");
        when(governanceCapabilityClient.writeAccelerationPlanTrace(any())).thenReturn(traceResponse);

        String taskId = createSucceededAccelerationTask();
        MvcResult submitResult = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/acceleration-plans"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"sourceTaskId\":\"" + taskId + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        String planId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.planId");

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/acceleration-plans/{planId}/apply", planId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"skip approval\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(22006));
    }

    private String createSucceededAccelerationTask() throws Exception {
        MvcResult submitTaskResult = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/tasks"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"ACCELERATION_SUGGESTION\","
                    + "\"sqlText\":\"SELECT * FROM orders\",\"datasourceType\":\"HETU\"}"))
            .andExpect(status().isOk())
            .andReturn();
        String taskId = JsonTestUtils.readValue(submitTaskResult.getResponse().getContentAsString(), "$.taskId");
        waitForTerminalStatus(taskId, "SUCCEEDED");
        return taskId;
    }

    private void waitForTerminalStatus(String taskId, String expectedStatus) throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) {
            MvcResult result = mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/tasks/{taskId}", taskId)))
                .andExpect(status().isOk())
                .andReturn();
            String status = JsonTestUtils.readValue(result.getResponse().getContentAsString(), "$.status");
            if (expectedStatus.equals(status)) {
                return;
            }
            Thread.sleep(40L);
        }
        throw new AssertionError("Task did not reach expected status " + expectedStatus);
    }

    private QueryExecutionAccelerationPlanResponse runtimeResponse(String status, boolean active) {
        QueryExecutionAccelerationPlanResponse response = new QueryExecutionAccelerationPlanResponse();
        response.setTenantId("tenant-a");
        response.setPlanId("plan-001");
        response.setSqlFingerprint("fp-001");
        response.setTargetEngine("HETU");
        response.setActive(active);
        response.setStatus(status);
        response.setRuntimeSummary(status + " runtime response");
        response.setRuntimeDetailsJson("{\"bindingState\":\"" + status + "\"}");
        response.setContractStage("LONG_TERM_BASELINE");
        response.setImplementationStage("APPROVED_ACCELERATION_RUNTIME_BASELINE");
        return response;
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
