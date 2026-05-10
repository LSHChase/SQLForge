package com.company.sqloptimization.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqloptimization.application.controller.vo.AccelerationCandidateVO;
import com.company.sqloptimization.application.service.AccelerationCandidateApplicationService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AccelerationCandidateController.class)
class AccelerationCandidateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccelerationCandidateApplicationService accelerationCandidateApplicationService;

    @Test
    void shouldExposeAccelerationCandidateContractEndpoints() throws Exception {
        AccelerationCandidateVO candidate = new AccelerationCandidateVO();
        candidate.setCandidateId("candidate-001");
        candidate.setTenantId("tenant-a");
        candidate.setSourceType("PARSE");
        candidate.setSourceKind("STRUCTURE_PARSE");
        candidate.setSourceId("parse-history-001");
        candidate.setEvidenceLevel("STATIC_PARSE");
        candidate.setStatus("DRAFT");
        candidate.setContractStage("LONG_TERM_BASELINE");
        candidate.setImplementationStage("ACCELERATION_REWRITE_CONTRACT_BASELINE");
        when(accelerationCandidateApplicationService.createCandidate(any())).thenReturn(candidate);
        when(accelerationCandidateApplicationService.getCandidate("candidate-001")).thenReturn(candidate);
        when(accelerationCandidateApplicationService.listCandidates()).thenReturn(Collections.singletonList(candidate));

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/acceleration-candidates"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"sourceType\":\"PARSE\",\"sourceKind\":\"STRUCTURE_PARSE\","
                    + "\"sourceId\":\"parse-history-001\",\"evidenceLevel\":\"STATIC_PARSE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceKind").value("STRUCTURE_PARSE"))
            .andExpect(jsonPath("$.evidenceLevel").value("STATIC_PARSE"));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/acceleration-candidates/candidate-001")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.candidateId").value("candidate-001"))
            .andExpect(jsonPath("$.implementationStage").value("ACCELERATION_REWRITE_CONTRACT_BASELINE"));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/acceleration-candidates")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sourceType").value("PARSE"));
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
