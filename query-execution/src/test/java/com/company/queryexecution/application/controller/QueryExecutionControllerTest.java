package com.company.queryexecution.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.application.interceptor.AuthInterceptor;
import com.company.queryexecution.application.controller.vo.QueryExecutionMetadataVO;
import com.company.queryexecution.application.service.QueryExecutionApplicationService;
import com.company.queryexecution.config.AuthProperties;
import com.company.queryexecution.config.WebMvcConfig;
import com.company.queryexecution.domain.query.QueryExecutionStatus;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import com.company.sqlforge.common.exception.GlobalExceptionHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QueryExecutionController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, AuthInterceptor.class, QueryExecutionControllerTest.TestConfig.class})
class QueryExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryExecutionApplicationService queryExecutionApplicationService;

    @Test
    void shouldReturnSynchronousQueryResponse() throws Exception {
        LogicalObjectSurface logicalObjectSurface = new LogicalObjectSurface();
        logicalObjectSurface.setObjectType("TABLE");
        logicalObjectSurface.setObjectKey("TABLE:orders");
        logicalObjectSurface.setObjectName("orders");
        when(queryExecutionApplicationService.executeSynchronously(any()))
            .thenReturn(new QueryExecuteResponse(
                QueryExecutionStatus.SUCCESS,
                Collections.singletonList(Collections.<String, Object>singletonMap("engine", "HETU")),
                null,
                new QueryExecutionMetadataVO(
                    "HETU",
                    "SELECT 1",
                    74L,
                    32L,
                    false,
                    true,
                    "CLIENT",
                    Arrays.asList("CLIENT"),
                    1
                ),
                false,
                null,
                Collections.emptyList(),
                null,
                Collections.singletonMap("report_code", "RPT_SALES_DAILY"),
                new LinkedHashMap<String, Object>() {{
                    put("queryDateStart", "2026-04-27");
                    put("queryDateEnd", "2026-04-27");
                    put("queryDateFields", Collections.singletonList("query_date"));
                    put("queryDateStatus", "RESOLVED");
                }},
                new LinkedHashMap<String, Object>() {{
                    put("parameterizedSqlFlag", Boolean.FALSE);
                    put("bindingMode", "NONE");
                    put("bindingRenderStatus", "SUCCESS");
                }},
                Collections.singletonList(logicalObjectSurface),
                new LinkedHashMap<String, Object>() {{
                    put("selectedEngine", "HETU");
                    put("executionMode", "CLIENT");
                }},
                new LinkedHashMap<String, Object>() {{
                    put("cacheHit", Boolean.FALSE);
                    put("cacheGovernanceStatus", "BYPASSED");
                }},
                new LinkedHashMap<String, Object>() {{
                    put("sqlType", "SELECT");
                    put("syntaxStatus", "VALID");
                }},
                "fingerprint-001",
                "LONG_TERM_BASELINE",
                "HETU_REAL_INTEGRATION"
            ));

        mockMvc.perform(post("/api/query-execution/queries/execute")
                .header("X-Tenant-Id", "tenant-a")
                .header("X-User-Id", "user-01")
                .header("X-Role-Codes", "TENANT_ADMIN,ANALYST")
                .header("X-Request-Id", "request-001")
                .header("X-Trace-Id", "trace-001")
                .header("X-Auth-Source", "header")
                .header("X-Access-Channel", "api")
                .header("X-Issued-At", "1713700000000")
                .header("X-Expires-At", "2713700000000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT 1\",\"tenantId\":\"tenant-a\",\"datasourceType\":\"HETU\"}"))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Trace-Id"))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.metadata.targetEngine").value("HETU"))
            .andExpect(jsonPath("$.metadata.executionMode").value("CLIENT"))
            .andExpect(jsonPath("$.metadata.attemptedModes[0]").value("CLIENT"))
            .andExpect(jsonPath("$.metadata.rowCount").value(1))
            .andExpect(jsonPath("$.rows[0].engine").value("HETU"))
            .andExpect(jsonPath("$.commentContext.report_code").value("RPT_SALES_DAILY"))
            .andExpect(jsonPath("$.queryDateSummary.queryDateStatus").value("RESOLVED"))
            .andExpect(jsonPath("$.bindingSummary.bindingRenderStatus").value("SUCCESS"))
            .andExpect(jsonPath("$.logicalObjectHits[0].objectKey").value("TABLE:orders"))
            .andExpect(jsonPath("$.routeSummary.selectedEngine").value("HETU"))
            .andExpect(jsonPath("$.cacheSummary.cacheHit").value(false))
            .andExpect(jsonPath("$.lightweightParseSummary.sqlType").value("SELECT"))
            .andExpect(jsonPath("$.contractStage").value("LONG_TERM_BASELINE"))
            .andExpect(jsonPath("$.implementationStage").value("HETU_REAL_INTEGRATION"));

        verify(queryExecutionApplicationService).executeSynchronously(any());
    }

    @Test
    void shouldRejectInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/query-execution/queries/execute")
                .header("X-Tenant-Id", "tenant-a")
                .header("X-User-Id", "user-01")
                .header("X-Role-Codes", "TENANT_ADMIN,ANALYST")
                .header("X-Request-Id", "request-002")
                .header("X-Trace-Id", "trace-002")
                .header("X-Auth-Source", "header")
                .header("X-Access-Channel", "api")
                .header("X-Issued-At", "1713700000000")
                .header("X-Expires-At", "2713700000000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"\",\"tenantId\":\"tenant-a\",\"datasourceType\":\"HETU\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(10001))
            .andExpect(jsonPath("$.message").value("sqlText 为必填项"));
    }

    @Test
    void shouldRejectProtectedEndpointWithoutFullHeaders() throws Exception {
        mockMvc.perform(post("/api/query-execution/queries/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT 1\",\"tenantId\":\"tenant-a\",\"datasourceType\":\"HETU\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(10002));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        AuthProperties authProperties() {
            AuthProperties authProperties = new AuthProperties();
            authProperties.setEnabled(false);
            authProperties.getTrustedAuthSources().add("header");
            return authProperties;
        }
    }
}
