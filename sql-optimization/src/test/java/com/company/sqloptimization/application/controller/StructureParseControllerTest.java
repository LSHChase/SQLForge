package com.company.sqloptimization.application.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqloptimization.SqlOptimizationApplication;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(classes = SqlOptimizationApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StructureParseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GovernanceCapabilityClient governanceCapabilityClient;

    @Test
    void shouldReturnStructureParseResultForValidReadSql() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT * FROM vw_sales_daily WHERE dt = '2026-04-01' AND dt = '2026-04-01' ORDER BY id\","
                    + "\"datasourceCode\":\"hetu_main\",\"bindingMode\":\"POSITIONAL\","
                    + "\"commentContext\":{\"report_code\":\"RPT_SALES_DAILY\",\"stage\":\"PROD\"}}"))
            .andExpect(status().isOk())
            .andExpect(header().exists(RequestHeaderConstants.TRACE_ID))
            .andExpect(jsonPath("$.parseType").value("STRUCTURE"))
            .andExpect(jsonPath("$.syntaxStatus").value("VALID"))
            .andExpect(jsonPath("$.sqlType").value("SELECT"))
            .andExpect(jsonPath("$.queryDateSummary.queryDateStart").value("2026-04-01"))
            .andExpect(jsonPath("$.queryDateSummary.queryDateStatus").value("RESOLVED"))
            .andExpect(jsonPath("$.logicalObjectHits[0].objectType").value("DB_VIEW"))
            .andExpect(jsonPath("$.riskTags[0]").value("SELECT_STAR"))
            .andExpect(jsonPath("$.rewriteCandidates[0]").value("DEDUPLICATE_WHERE_PREDICATES"))
            .andExpect(jsonPath("$.issues[0].issueCode").value("SELECT_STAR"))
            .andExpect(jsonPath("$.priorityLevel").value("P3"));
    }

    @Test
    void shouldReturnInvalidStructureParseInsteadOfFailingHard() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse/structure"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT FROM\",\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parseType").value("STRUCTURE"))
            .andExpect(jsonPath("$.syntaxStatus").value("INVALID"))
            .andExpect(jsonPath("$.sqlType").value("UNKNOWN"))
            .andExpect(jsonPath("$.issues[0].issueCode").value("SQL_SYNTAX_INVALID"))
            .andExpect(jsonPath("$.issues[0].issueDomain").value("STRUCTURE"))
            .andExpect(jsonPath("$.queryDateSummary.queryDateStatus").value("UNRESOLVED"))
            .andExpect(jsonPath("$.rewriteCandidates").isEmpty());
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder) {
        long now = System.currentTimeMillis();
        return builder
            .header(RequestHeaderConstants.TENANT_ID, "tenant-a")
            .header(RequestHeaderConstants.USER_ID, "operator-001")
            .header(RequestHeaderConstants.ROLE_CODES, "TENANT_ADMIN,OPERATOR")
            .header(RequestHeaderConstants.REQUEST_ID, "request-parse-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-parse-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }
}
