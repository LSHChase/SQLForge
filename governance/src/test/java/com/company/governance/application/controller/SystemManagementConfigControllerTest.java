package com.company.governance.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.governance.application.controller.vo.DispatchPolicyVO;
import com.company.governance.application.controller.vo.RedisRuleSourceVO;
import com.company.governance.application.service.DispatchPolicyApplicationService;
import com.company.governance.application.service.RedisRuleSourceApplicationService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SystemManagementConfigControllerTest {

    @Test
    void shouldExposeRedisRuleSourceAndDispatchPolicyEndpoints() throws Exception {
        RedisRuleSourceApplicationService redisService = mock(RedisRuleSourceApplicationService.class);
        DispatchPolicyApplicationService dispatchService = mock(DispatchPolicyApplicationService.class);
        when(redisService.list("tenant-a")).thenReturn(Collections.singletonList(sampleRedis()));
        when(redisService.create(any())).thenReturn(sampleRedis());
        when(redisService.update(org.mockito.Mockito.eq("source-001"), any())).thenReturn(sampleRedis());
        when(dispatchService.list("tenant-a")).thenReturn(Collections.singletonList(sampleDispatch()));
        when(dispatchService.create(any())).thenReturn(sampleDispatch());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SystemManagementConfigController(redisService, dispatchService)).build();

        mockMvc.perform(get("/api/governance/redis-rule-sources").param("tenantId", "tenant-a"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sourceName").value("jdbc-agent-rules"));

        mockMvc.perform(post("/api/governance/redis-rule-sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"sourceName\":\"jdbc-agent-rules\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.healthStatus").value("SIMULATED_READY"));

        mockMvc.perform(put("/api/governance/redis-rule-sources/source-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"sourceName\":\"jdbc-agent-rules\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activationMode").value("CONFIG_ONLY"));

        mockMvc.perform(get("/api/governance/dispatch-policies").param("tenantId", "tenant-a"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].policyName").value("dispatch-main"));

        mockMvc.perform(post("/api/governance/dispatch-policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"policyName\":\"dispatch-main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.executionBoundary").value("EXTERNAL_MODULE_REQUIRED"));
    }

    private RedisRuleSourceVO sampleRedis() {
        RedisRuleSourceVO response = new RedisRuleSourceVO();
        response.setSourceId("source-001");
        response.setTenantId("tenant-a");
        response.setSourceName("jdbc-agent-rules");
        response.setHealthStatus("SIMULATED_READY");
        response.setActivationMode("CONFIG_ONLY");
        return response;
    }

    private DispatchPolicyVO sampleDispatch() {
        DispatchPolicyVO response = new DispatchPolicyVO();
        response.setPolicyId("policy-001");
        response.setTenantId("tenant-a");
        response.setPolicyName("dispatch-main");
        response.setExecutionBoundary("EXTERNAL_MODULE_REQUIRED");
        return response;
    }
}
