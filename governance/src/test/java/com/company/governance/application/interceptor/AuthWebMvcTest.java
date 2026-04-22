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
import com.company.governance.application.controller.GovernanceHistoryController;
import com.company.governance.application.controller.MessageAdminController;
import com.company.governance.application.controller.TenantConfigController;
import com.company.governance.application.controller.vo.AuditWriteResponse;
import com.company.governance.application.controller.vo.DatasourceAccessCheckResponse;
import com.company.governance.application.controller.vo.GovernanceTraceDetailVO;
import com.company.governance.application.controller.vo.GovernanceTraceSummaryVO;
import com.company.governance.application.controller.vo.HealthStatusVO;
import com.company.governance.application.controller.vo.MessageStatsVO;
import com.company.governance.application.controller.vo.ScheduleExtensionStatusVO;
import com.company.governance.application.controller.vo.TenantConfigVO;
import com.company.governance.application.controller.vo.TenantScopeCheckResponse;
import com.company.governance.application.service.GovernanceCapabilityApplicationService;
import com.company.governance.application.service.GovernanceAuditTrailService;
import com.company.governance.application.service.GovernanceHistoryApplicationService;
import com.company.governance.application.service.HealthStatusApplicationService;
import com.company.governance.application.service.MessageAdminApplicationService;
import com.company.governance.application.service.TenantConfigApplicationService;
import java.time.LocalDateTime;
import java.util.Collections;
import com.company.governance.config.AuthProperties;
import com.company.governance.config.WebMvcConfig;
import com.company.governance.infrastructure.persistence.mapper.AuditLogMapper;
import com.company.governance.infrastructure.persistence.mapper.ConfigSnapshotMapper;
import com.company.governance.infrastructure.persistence.mapper.ExecutionResultMapper;
import com.company.governance.infrastructure.persistence.mapper.ExportRecordMapper;
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
    GovernanceHistoryController.class,
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
    private AuditLogMapper auditLogMapper;

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
    void shouldProtectGovernanceHistoryEndpoints() throws Exception {
        GovernanceTraceSummaryVO summary = new GovernanceTraceSummaryVO(
            "trace-001",
            "request-001",
            "QUERY_EXECUTION",
            "EXECUTE_QUERY",
            "QUERY",
            "fp-001",
            "PARTIAL",
            LocalDateTime.parse("2026-04-22T10:00:00"),
            Integer.valueOf(1),
            Integer.valueOf(1),
            Integer.valueOf(0),
            Integer.valueOf(0),
            null,
            null,
            "fp-001",
            "12000",
            "HIVE",
            Boolean.TRUE
        );
        GovernanceTraceDetailVO detail = new GovernanceTraceDetailVO();
        detail.setTraceId("trace-001");
        detail.setLatestStatus("PARTIAL");
        detail.setAuditEventCount(Integer.valueOf(1));
        detail.setAuditEvents(Collections.<GovernanceTraceDetailVO.AuditEventVO>emptyList());
        detail.setQueryHistories(Collections.<GovernanceTraceDetailVO.QueryHistoryVO>emptyList());
        detail.setExportRecords(Collections.<GovernanceTraceDetailVO.ExportRecordVO>emptyList());

        when(governanceHistoryApplicationService.findRecentTraces("system", Integer.valueOf(5)))
            .thenReturn(Collections.singletonList(summary));
        when(governanceHistoryApplicationService.findTraceDetail("system", "trace-001", Integer.valueOf(5)))
            .thenReturn(detail);

        mockMvc.perform(addProtectedHeaders(get("/api/governance/history/traces?limit=5")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].traceId").value("trace-001"))
            .andExpect(jsonPath("$[0].latestStatus").value("PARTIAL"));

        mockMvc.perform(addProtectedHeaders(get("/api/governance/history/traces/trace-001?limit=5")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.traceId").value("trace-001"))
            .andExpect(jsonPath("$.auditEventCount").value(1));

        verify(governanceHistoryApplicationService).findRecentTraces("system", Integer.valueOf(5));
        verify(governanceHistoryApplicationService).findTraceDetail("system", "trace-001", Integer.valueOf(5));
    }

    @Test
    void shouldRejectMessageRetryWhenHeadersMissing() throws Exception {
        mockMvc.perform(post("/api/governance/admin/messages/retry"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    void shouldProtectGovernanceInternalEndpoints() throws Exception {
        when(governanceCapabilityApplicationService.checkTenantScope(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new TenantScopeCheckResponse("system", "tenant-b", false, "CROSS_TENANT_ACCESS_REQUIRES_PLATFORM_ADMIN"));
        when(governanceCapabilityApplicationService.checkDatasourceAccess(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new DatasourceAccessCheckResponse(
                "system",
                "ds-01",
                true,
                "ALLOWED",
                null,
                "LONG_TERM_BASELINE",
                "TRANSITIONAL_SKELETON"
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
        when(governanceCapabilityApplicationService.getScheduleExtensionStatus())
            .thenReturn(new ScheduleExtensionStatusVO(
                "governance.schedule.dispatch",
                "GOVERNANCE",
                "ACTIVE",
                "DATABASE",
                "TRANSITIONAL_SKELETON",
                "TRANSITIONAL_SKELETON"
            ));

        mockMvc.perform(addProtectedHeaders(post("/api/governance/internal/tenant-scope/check")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"system\",\"targetTenantId\":\"tenant-b\"}")))
            .andExpect(status().isOk())
            .andExpect(header().exists(RequestHeaderConstants.TRACE_ID))
            .andExpect(jsonPath("$.allowed").value(false));

        mockMvc.perform(addProtectedHeaders(post("/api/governance/internal/datasource-access/check")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"system\",\"datasourceId\":\"ds-01\"}")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allowed").value(true))
            .andExpect(jsonPath("$.contractStage").value("LONG_TERM_BASELINE"));

        mockMvc.perform(addProtectedHeaders(post("/api/governance/internal/audit/write")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"serviceCode\":\"QUERY_EXECUTION\",\"operationCode\":\"AUDIT_QUERY\","
                    + "\"resourceType\":\"Query\",\"resourceId\":\"q-01\",\"resultStatus\":\"SUCCESS\","
                    + "\"elapsedMs\":12,\"sourceIp\":\"127.0.0.1\",\"userAgent\":\"JUnit\"}")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACCEPTED"))
            .andExpect(jsonPath("$.deliveryMode").value("DATABASE"));

        mockMvc.perform(addProtectedHeaders(get("/api/governance/internal/schedule/extensions")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.contractStage").value("TRANSITIONAL_SKELETON"));
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
            .header(RequestHeaderConstants.USER_ID, "operator-001")
            .header(RequestHeaderConstants.ROLE_CODES, "TENANT_ADMIN,OPERATOR")
            .header(RequestHeaderConstants.REQUEST_ID, "request-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }
}
