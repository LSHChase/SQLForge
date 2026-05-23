package com.company.governance.application.interceptor;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.governance.application.controller.HealthController;
import com.company.governance.application.controller.GovernanceCapabilityController;
import com.company.governance.application.controller.GovernanceQueryHistoryController;
import com.company.governance.application.controller.dto.GovernanceQueryHistoryExportRequest;
import com.company.governance.application.controller.MessageAdminController;
import com.company.governance.application.controller.TenantConfigController;
import com.company.governance.application.controller.vo.AuditWriteResponse;
import com.company.governance.application.controller.vo.DatasourceAccessScopeChangeResponse;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryDetailVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryExportVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryPageVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistorySummaryVO;
import com.company.governance.application.controller.vo.HealthStatusVO;
import com.company.governance.application.controller.vo.MessageStatsVO;
import com.company.governance.application.controller.vo.ScheduleExtensionStatusVO;
import com.company.governance.application.controller.vo.TenantConfigVO;
import com.company.governance.application.service.GovernanceCapabilityApplicationService;
import com.company.governance.application.service.GovernanceAuditTrailService;
import com.company.governance.application.service.GovernanceHistoryApplicationService;
import com.company.governance.application.service.GovernanceSqlRewriteDivergenceAlertApplicationService;
import com.company.governance.application.service.HealthStatusApplicationService;
import com.company.governance.application.service.MessageAdminApplicationService;
import com.company.governance.application.service.TenantConfigApplicationService;
import com.company.sqlforge.common.governance.GovernanceDatasourceAccessCheckResponse;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveResponse;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteResponse;
import com.company.sqlforge.common.governance.GovernanceTenantScopeCheckResponse;
import java.util.Collections;
import com.company.governance.config.AuthProperties;
import com.company.governance.config.WebMvcConfig;
import com.company.governance.infrastructure.persistence.mapper.AuditLogMapper;
import com.company.governance.infrastructure.persistence.mapper.AlertEventMapper;
import com.company.governance.infrastructure.persistence.mapper.AlertNotificationLogMapper;
import com.company.governance.infrastructure.persistence.mapper.AlertPolicyMapper;
import com.company.governance.infrastructure.persistence.mapper.ConfigSnapshotMapper;
import com.company.governance.infrastructure.persistence.mapper.BusinessLogicalViewMapper;
import com.company.governance.infrastructure.persistence.mapper.DatabaseViewDependencyMapper;
import com.company.governance.infrastructure.persistence.mapper.DatabaseViewMapper;
import com.company.governance.infrastructure.persistence.mapper.DatasourceConfigMapper;
import com.company.governance.infrastructure.persistence.mapper.ExecutionResultMapper;
import com.company.governance.infrastructure.persistence.mapper.ExportRecordMapper;
import com.company.governance.infrastructure.persistence.mapper.GovernanceHistoryLookupIndexMapper;
import com.company.governance.infrastructure.persistence.mapper.LogicalObjectMappingMapper;
import com.company.governance.infrastructure.persistence.mapper.MessageQueueMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.governance.infrastructure.persistence.mapper.SystemConfigMapper;
import com.company.governance.infrastructure.persistence.mapper.TenantConfigMapper;
import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

@WebMvcTest(controllers = {
    HealthController.class,
    TenantConfigController.class,
    MessageAdminController.class,
    GovernanceQueryHistoryController.class,
    GovernanceCapabilityController.class
})
@Import({WebMvcConfig.class, AuthInterceptor.class})
class AuthWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthStatusApplicationService healthStatusApplicationService;

    @MockBean
    private TenantConfigApplicationService tenantConfigApplicationService;

    @MockBean
    private MessageAdminApplicationService messageAdminApplicationService;

    @MockBean
    private GovernanceHistoryApplicationService governanceHistoryApplicationService;

    @MockBean
    private GovernanceCapabilityApplicationService governanceCapabilityApplicationService;

    @MockBean
    private GovernanceSqlRewriteDivergenceAlertApplicationService sqlRewriteDivergenceAlertApplicationService;

    @MockBean
    private GovernanceAuditTrailService governanceAuditTrailService;

    @MockBean
    private TenantConfigMapper tenantConfigMapper;

    @MockBean
    private MessageQueueMapper messageQueueMapper;

    @MockBean
    private ConfigSnapshotMapper configSnapshotMapper;

    @MockBean
    private ExecutionResultMapper executionResultMapper;

    @MockBean
    private QueryHistoryMapper queryHistoryMapper;

    @MockBean
    private ExportRecordMapper exportRecordMapper;

    @MockBean
    private GovernanceHistoryLookupIndexMapper governanceHistoryLookupIndexMapper;

    @MockBean
    private AuditLogMapper auditLogMapper;

    @MockBean
    private AlertEventMapper alertEventMapper;

    @MockBean
    private AlertNotificationLogMapper alertNotificationLogMapper;

    @MockBean
    private AlertPolicyMapper alertPolicyMapper;

    @MockBean
    private BusinessLogicalViewMapper businessLogicalViewMapper;

    @MockBean
    private DatabaseViewDependencyMapper databaseViewDependencyMapper;

    @MockBean
    private DatabaseViewMapper databaseViewMapper;

    @MockBean
    private DatasourceConfigMapper datasourceConfigMapper;

    @MockBean
    private LogicalObjectMappingMapper logicalObjectMappingMapper;

    @MockBean
    private SystemConfigMapper systemConfigMapper;

    @Test
    void shouldBypassInterceptorForHealthEndpoint() throws Exception {
        when(healthStatusApplicationService.currentStatus())
            .thenReturn(new HealthStatusVO("governance", "UP", null, null, null, "PUBLIC"));

        mockMvc.perform(get("/api/governance/health"))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("X-Trace-Id"))
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldResolveTenantHeaderAndTraceIdForTenantConfigEndpoint() throws Exception {
        when(tenantConfigApplicationService.findByTenantId("system"))
            .thenReturn(new TenantConfigVO("system", 20, 2048, "HETU", "HIVE", "NORMAL", 180, 50));

        mockMvc.perform(addProtectedHeaders(get("/api/governance/tenant-config")))
            .andExpect(status().isOk())
            .andExpect(header().exists(RequestHeaderConstants.TRACE_ID))
            .andExpect(jsonPath("$.tenantId").value("system"));

        verify(tenantConfigApplicationService).findByTenantId("system");
    }

    @Test
    void shouldRejectProtectedEndpointWithoutFullHeaders() throws Exception {
        mockMvc.perform(get("/api/governance/tenant-config"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    void shouldProtectMessageAdminEndpointUnderApiNamespace() throws Exception {
        when(messageAdminApplicationService.getMessageStats())
            .thenReturn(new MessageStatsVO(10L, 2L, 3L, 4L, 1L));

        mockMvc.perform(addProtectedHeaders(get("/api/governance/admin/messages/stats")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(10L))
            .andExpect(header().exists(RequestHeaderConstants.REQUEST_ID));

        verify(messageAdminApplicationService).getMessageStats();
    }

    @Test
    void shouldProtectQueryHistoryEndpoints() throws Exception {
        GovernanceQueryHistorySummaryVO historySummary = new GovernanceQueryHistorySummaryVO();
        historySummary.setHistoryId("history-001");
        historySummary.setTraceId("trace-001");
        historySummary.setResultStatus("PARTIAL");
        GovernanceQueryHistoryDetailVO historyDetail = new GovernanceQueryHistoryDetailVO();
        historyDetail.setHistoryId("history-001");
        historyDetail.setTraceId("trace-001");
        GovernanceQueryHistoryExportVO exportVO = new GovernanceQueryHistoryExportVO();
        exportVO.setExportId("export-history-001");
        exportVO.setHistoryId("history-001");
        exportVO.setTraceId("trace-001");
        exportVO.setExportFormat("JSON");
        exportVO.setExportStatus("GENERATED");
        exportVO.setPayload("{\"historyId\":\"history-001\"}");

        when(governanceHistoryApplicationService.findQueryHistoryPage(
            "system",
            null,
            "RPT_SALES_DAILY",
            "hetu_main",
            "PROD",
            null,
            null,
            null,
            "PARTIAL",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Integer.valueOf(1),
            Integer.valueOf(5)
        )).thenReturn(new GovernanceQueryHistoryPageVO(
            Collections.singletonList(historySummary),
            Integer.valueOf(1),
            Integer.valueOf(5),
            Integer.valueOf(1),
            Integer.valueOf(1),
            Boolean.FALSE,
            Collections.<String, Object>emptyMap()
        ));
        when(governanceHistoryApplicationService.findQueryHistoryDetail("system", "history-001"))
            .thenReturn(historyDetail);
        when(governanceHistoryApplicationService.exportQueryHistory(org.mockito.ArgumentMatchers.eq("system"), org.mockito.ArgumentMatchers.any(GovernanceQueryHistoryExportRequest.class)))
            .thenReturn(exportVO);

        mockMvc.perform(addProtectedHeaders(get("/api/governance/query-history?pageNo=1&pageSize=5&reportCode=RPT_SALES_DAILY&datasourceCode=hetu_main&stage=PROD&status=PARTIAL")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].historyId").value("history-001"))
            .andExpect(jsonPath("$.items[0].traceId").value("trace-001"))
            .andExpect(jsonPath("$.hasMore").value(false));

        mockMvc.perform(addProtectedHeaders(get("/api/governance/query-history/history-001")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.historyId").value("history-001"))
            .andExpect(jsonPath("$.traceId").value("trace-001"));

        mockMvc.perform(addProtectedHeaders(post("/api/governance/query-history/export")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"historyId\":\"history-001\",\"exportFormat\":\"JSON\",\"includeTraceDetail\":true}")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exportId").value("export-history-001"))
            .andExpect(jsonPath("$.payload").value("{\"historyId\":\"history-001\"}"));

        verify(governanceHistoryApplicationService).findQueryHistoryPage(
            "system",
            null,
            "RPT_SALES_DAILY",
            "hetu_main",
            "PROD",
            null,
            null,
            null,
            "PARTIAL",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Integer.valueOf(1),
            Integer.valueOf(5)
        );
        verify(governanceHistoryApplicationService).findQueryHistoryDetail("system", "history-001");
        verify(governanceHistoryApplicationService).exportQueryHistory(
            org.mockito.ArgumentMatchers.eq("system"),
            org.mockito.ArgumentMatchers.any(GovernanceQueryHistoryExportRequest.class)
        );
    }

    @Test
    void shouldRejectMessageRetryWhenHeadersMissing() throws Exception {
        mockMvc.perform(post("/api/governance/admin/messages/retry"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    void shouldProtectGovernanceInternalEndpoints() throws Exception {
        GovernanceTenantScopeCheckResponse tenantScopeResponse = new GovernanceTenantScopeCheckResponse();
        tenantScopeResponse.setTenantId("system");
        tenantScopeResponse.setTargetTenantId("tenant-b");
        tenantScopeResponse.setAllowed(false);
        tenantScopeResponse.setReason("CROSS_TENANT_ACCESS_REQUIRES_PLATFORM_ADMIN");
        GovernanceDatasourceAccessCheckResponse authorizationResponse = new GovernanceDatasourceAccessCheckResponse();
        authorizationResponse.setTenantId("system");
        authorizationResponse.setResourceType("QUERY_EXECUTION_QUERY");
        authorizationResponse.setResourceId("fp-001");
        authorizationResponse.setOperationCode("QUERY_EXECUTE_SYNC");
        authorizationResponse.setDatasourceId("query-hetu");
        authorizationResponse.setAllowed(true);
        authorizationResponse.setReason("ALLOWED");
        authorizationResponse.setContractStage("LONG_TERM_BASELINE");
        authorizationResponse.setImplementationStage("DATASOURCE_ACCESS_SCOPE_BASELINE");
        when(governanceCapabilityApplicationService.checkTenantScope(org.mockito.ArgumentMatchers.any()))
            .thenReturn(tenantScopeResponse);
        when(governanceCapabilityApplicationService.checkDatasourceAccess(org.mockito.ArgumentMatchers.any()))
            .thenReturn(authorizationResponse);
        when(governanceCapabilityApplicationService.changeDatasourceAccessScope(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new DatasourceAccessScopeChangeResponse(
                "system",
                "query-hetu",
                "ACTIVE",
                java.util.Collections.singletonList("USE"),
                "UPDATED",
                "LONG_TERM_BASELINE",
                "DATASOURCE_ACCESS_SCOPE_BASELINE"
            ));
        when(governanceCapabilityApplicationService.publishAuditEvent(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new AuditWriteResponse(
                Long.valueOf(1L),
                "QUERY_EXECUTION",
                "AUDIT_QUERY",
                "ACCEPTED",
                "governance.audit.event",
                "DATABASE",
                "LONG_TERM_BASELINE",
                "TRANSITIONAL_SKELETON"
            ));
        GovernanceQueryExecutionHistoryWriteResponse queryExecutionHistoryResponse =
            new GovernanceQueryExecutionHistoryWriteResponse();
        queryExecutionHistoryResponse.setHistoryId("history-qe-001");
        queryExecutionHistoryResponse.setResultId("result-qe-001");
        queryExecutionHistoryResponse.setContractStage("LONG_TERM_BASELINE");
        queryExecutionHistoryResponse.setImplementationStage("QUERY_EXECUTION_HISTORY_PERSISTENCE_BASELINE");
        when(governanceCapabilityApplicationService.writeQueryExecutionHistory(org.mockito.ArgumentMatchers.any()))
            .thenReturn(queryExecutionHistoryResponse);
        when(governanceCapabilityApplicationService.getScheduleExtensionStatus())
            .thenReturn(new ScheduleExtensionStatusVO(
                "governance.schedule.dispatch",
                "GOVERNANCE",
                "ACTIVE",
                "DATABASE",
                "TRANSITIONAL_SKELETON",
                "TRANSITIONAL_SKELETON"
            ));
        GovernanceJdbcDatasourceResolveResponse jdbcResolveResponse = new GovernanceJdbcDatasourceResolveResponse();
        jdbcResolveResponse.setTenantId("system");
        jdbcResolveResponse.setDatasourceCode("hetu_main");
        jdbcResolveResponse.setEngineType("HETU");
        jdbcResolveResponse.setConnectionMode("JDBC");
        jdbcResolveResponse.setResolved(true);
        jdbcResolveResponse.setJdbcUrl("jdbc:hetu://coordinator:8080/hive/default");
        jdbcResolveResponse.setDriverClassName("io.prestosql.jdbc.PrestoDriver");
        jdbcResolveResponse.setUsername("hetu_user");
        jdbcResolveResponse.setPassword("secret");
        jdbcResolveResponse.setTimeoutMs(Integer.valueOf(3000));
        jdbcResolveResponse.setCredentialMask("****cret");
        jdbcResolveResponse.setContractStage("LONG_TERM_BASELINE");
        jdbcResolveResponse.setImplementationStage("HETU_JDBC_DATASOURCE_RESOLVE_BASELINE");
        when(governanceCapabilityApplicationService.resolveJdbcDatasource(org.mockito.ArgumentMatchers.any()))
            .thenReturn(jdbcResolveResponse);

        mockMvc.perform(addProtectedHeaders(post("/api/governance/internal/tenant-scope/check")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"system\",\"targetTenantId\":\"tenant-b\"}")))
            .andExpect(status().isOk())
            .andExpect(header().exists(RequestHeaderConstants.TRACE_ID))
            .andExpect(jsonPath("$.allowed").value(false));

        mockMvc.perform(addProtectedHeaders(post("/api/governance/internal/datasource-access/check")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"serviceCode\":\"QUERY_EXECUTION\",\"tenantId\":\"system\","
                    + "\"resourceType\":\"QUERY_EXECUTION_QUERY\",\"resourceId\":\"fp-001\","
                    + "\"operationCode\":\"QUERY_EXECUTE_SYNC\",\"datasourceId\":\"query-hetu\"}")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allowed").value(true))
            .andExpect(jsonPath("$.contractStage").value("LONG_TERM_BASELINE"))
            .andExpect(jsonPath("$.implementationStage").value("DATASOURCE_ACCESS_SCOPE_BASELINE"));

        mockMvc.perform(addProtectedHeaders(post("/api/governance/internal/datasource-access/scope/change")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"system\",\"datasourceId\":\"query-hetu\","
                    + "\"state\":\"ACTIVE\",\"actions\":[\"USE\"],\"changeReason\":\"restore\"}")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UPDATED"))
            .andExpect(jsonPath("$.datasourceId").value("query-hetu"));

        mockMvc.perform(addProtectedHeaders(post("/api/governance/internal/audit/write")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"serviceCode\":\"QUERY_EXECUTION\",\"operationCode\":\"AUDIT_QUERY\","
                    + "\"resourceType\":\"Query\",\"resourceId\":\"q-01\",\"resultStatus\":\"SUCCESS\","
                    + "\"elapsedMs\":12,\"sourceIp\":\"127.0.0.1\",\"userAgent\":\"JUnit\"}")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACCEPTED"))
            .andExpect(jsonPath("$.deliveryMode").value("DATABASE"));

        mockMvc.perform(addProtectedHeaders(post("/api/governance/internal/query-execution-history/write")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"system\",\"sqlText\":\"SELECT 1\",\"sqlFingerprint\":\"fp-001\","
                    + "\"datasourceType\":\"HETU\",\"historyType\":\"QUERY_EXECUTION\","
                    + "\"resultStatus\":\"SUCCESS\",\"targetEngine\":\"HETU\"}")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.historyId").value("history-qe-001"))
            .andExpect(jsonPath("$.implementationStage").value("QUERY_EXECUTION_HISTORY_PERSISTENCE_BASELINE"));

        mockMvc.perform(addProtectedHeaders(get("/api/governance/internal/schedule/extensions")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.contractStage").value("TRANSITIONAL_SKELETON"));

        mockMvc.perform(addProtectedHeaders(post("/api/governance/internal/datasources/jdbc/resolve")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"system\",\"datasourceCode\":\"hetu_main\",\"engineType\":\"HETU\"}")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resolved").value(true))
            .andExpect(jsonPath("$.jdbcUrl").value("jdbc:hetu://coordinator:8080/hive/default"))
            .andExpect(jsonPath("$.password").value("secret"))
            .andExpect(jsonPath("$.credentialMask").value("****cret"));
    }

    @Test
    void shouldRejectGovernanceInternalEndpointWhenHeadersMissing() throws Exception {
        mockMvc.perform(post("/api/governance/internal/tenant-scope/check")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"system\",\"targetTenantId\":\"tenant-b\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(10002));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        AuthProperties authProperties() {
            AuthProperties authProperties = new AuthProperties();
            authProperties.setEnabled(false);
            authProperties.setTrustedAuthSources(java.util.Collections.singletonList(AuthSourceConstants.HEADER));
            return authProperties;
        }
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder addProtectedHeaders(
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        long now = System.currentTimeMillis();
        return builder
            .header(RequestHeaderConstants.TENANT_ID, "system")
            .header(RequestHeaderConstants.USER_ID, "user-001")
            .header(RequestHeaderConstants.REQUEST_ID, "request-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }
}
