package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.application.controller.dto.AuditWriteRequest;
import com.company.governance.application.controller.dto.DatasourceAccessCheckRequest;
import com.company.governance.application.controller.dto.TenantScopeCheckRequest;
import com.company.governance.application.controller.vo.AuditWriteResponse;
import com.company.governance.application.controller.vo.DatasourceAccessCheckResponse;
import com.company.governance.application.controller.vo.ScheduleExtensionStatusVO;
import com.company.governance.application.controller.vo.TenantScopeCheckResponse;
import com.company.governance.config.MessagingProperties;
import com.company.governance.domain.messaging.MessageProducer;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.infrastructure.messaging.GovernanceMessagingTopics;
import com.company.sqlforge.common.audit.AuditContext;
import com.company.sqlforge.common.config.MessagingMode;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GovernanceCapabilityApplicationServiceTest {

    @AfterEach
    void tearDown() {
        AuditContext.clear();
        RequestContext.clear();
    }

    @Test
    void shouldCheckTenantScopeAndDatasourceAccess() {
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        MessageProducer messageProducer = mock(MessageProducer.class);
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.DATABASE);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            tenantAccessLogic,
            messageProducer,
            messagingProperties
        );
        RequestContext.set(
            "tenant-a",
            "user-01",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            100L,
            200L
        );

        TenantScopeCheckRequest tenantScopeCheckRequest = new TenantScopeCheckRequest();
        tenantScopeCheckRequest.setTenantId("tenant-a");
        tenantScopeCheckRequest.setTargetTenantId("tenant-b");

        DatasourceAccessCheckRequest datasourceAccessCheckRequest = new DatasourceAccessCheckRequest();
        datasourceAccessCheckRequest.setTenantId("tenant-a");
        datasourceAccessCheckRequest.setDatasourceId("ds-01");
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "ds-01")).thenReturn(true);

        TenantScopeCheckResponse tenantScopeCheckResponse = service.checkTenantScope(tenantScopeCheckRequest);
        DatasourceAccessCheckResponse datasourceAccessCheckResponse = service.checkDatasourceAccess(
            datasourceAccessCheckRequest
        );

        assertFalse(tenantScopeCheckResponse.isAllowed());
        assertTrue(datasourceAccessCheckResponse.isAllowed());
        assertEquals("CROSS_TENANT_ACCESS_REQUIRES_PLATFORM_ADMIN", tenantScopeCheckResponse.getReason());
        assertEquals("LONG_TERM_BASELINE", datasourceAccessCheckResponse.getContractStage());
        assertEquals("TRANSITIONAL_SKELETON", datasourceAccessCheckResponse.getImplementationStage());
        assertNull(datasourceAccessCheckResponse.getErrorCode());
    }

    @Test
    void shouldAllowPlatformAdminToResolveCrossTenantChecks() {
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        MessageProducer messageProducer = mock(MessageProducer.class);
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.DATABASE);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            tenantAccessLogic,
            messageProducer,
            messagingProperties
        );
        RequestContext.set(
            "system",
            "platform-admin-01",
            Arrays.asList("PLATFORM_ADMIN"),
            "request-010",
            "trace-010",
            "gateway",
            100L,
            200L
        );

        TenantScopeCheckRequest tenantScopeCheckRequest = new TenantScopeCheckRequest();
        tenantScopeCheckRequest.setTenantId("tenant-a");
        tenantScopeCheckRequest.setTargetTenantId("tenant-b");
        DatasourceAccessCheckRequest datasourceAccessCheckRequest = new DatasourceAccessCheckRequest();
        datasourceAccessCheckRequest.setTenantId("tenant-a");
        datasourceAccessCheckRequest.setDatasourceId("ds-01");

        TenantScopeCheckResponse tenantScopeCheckResponse = service.checkTenantScope(tenantScopeCheckRequest);
        DatasourceAccessCheckResponse datasourceAccessCheckResponse = service.checkDatasourceAccess(
            datasourceAccessCheckRequest
        );

        assertTrue(tenantScopeCheckResponse.isAllowed());
        assertEquals("PLATFORM_ADMIN_OVERRIDE", tenantScopeCheckResponse.getReason());
        assertTrue(datasourceAccessCheckResponse.isAllowed());
        assertEquals("PLATFORM_ADMIN_OVERRIDE", datasourceAccessCheckResponse.getReason());
        assertNull(datasourceAccessCheckResponse.getErrorCode());
    }

    @Test
    void shouldReturnExplicitDatasourceDenialContract() {
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        MessageProducer messageProducer = mock(MessageProducer.class);
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.DATABASE);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            tenantAccessLogic,
            messageProducer,
            messagingProperties
        );
        RequestContext.set(
            "tenant-a",
            "user-01",
            Arrays.asList("TENANT_ADMIN"),
            "request-020",
            "trace-020",
            "header",
            100L,
            200L
        );
        DatasourceAccessCheckRequest request = new DatasourceAccessCheckRequest();
        request.setTenantId("tenant-a");
        request.setDatasourceId("ds-02");
        when(tenantAccessLogic.validateDataSourceAccess("tenant-a", "ds-02")).thenReturn(false);

        DatasourceAccessCheckResponse response = service.checkDatasourceAccess(request);

        assertFalse(response.isAllowed());
        assertEquals("TENANT_DATASOURCE_ACCESS_DENIED", response.getReason());
        assertEquals(Integer.valueOf(ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED), response.getErrorCode());
        assertEquals("LONG_TERM_BASELINE", response.getContractStage());
        assertEquals("TRANSITIONAL_SKELETON", response.getImplementationStage());
    }

    @Test
    void shouldPublishAuditEventThroughConfiguredMessageProducer() {
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        MessageProducer messageProducer = mock(MessageProducer.class);
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.MOCK);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            tenantAccessLogic,
            messageProducer,
            messagingProperties
        );
        RequestContext.set(
            "tenant-a",
            "user-01",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            100L,
            200L
        );

        AuditWriteRequest request = new AuditWriteRequest();
        request.setServiceCode("QUERY_EXECUTION");
        request.setOperationCode("AUDIT_QUERY");
        request.setResourceType("AuditLog");
        request.setResourceId("audit-001");
        request.setResultStatus("SUCCESS");
        request.setElapsedMs(42L);
        request.setSourceIp("127.0.0.1");
        request.setUserAgent("JUnit");

        AuditWriteResponse response = service.publishAuditEvent(request);
        ScheduleExtensionStatusVO scheduleExtensionStatusVO = service.getScheduleExtensionStatus();

        assertEquals("ACCEPTED", response.getStatus());
        assertEquals("QUERY_EXECUTION", response.getServiceCode());
        assertEquals("governance.audit.event", response.getMessageTopic());
        assertEquals("MOCK", response.getDeliveryMode());
        assertEquals("LONG_TERM_BASELINE", response.getContractStage());
        assertEquals("TRANSITIONAL_SKELETON", response.getImplementationStage());
        assertEquals("MOCK", scheduleExtensionStatusVO.getCurrentMode());
        assertEquals("TEST_ONLY", scheduleExtensionStatusVO.getStatus());
        assertEquals("GOVERNANCE", scheduleExtensionStatusVO.getOwnerService());
        assertEquals("TRANSITIONAL_SKELETON", scheduleExtensionStatusVO.getContractStage());
        verify(messageProducer).send(
            eq(GovernanceMessagingTopics.AUDIT_EVENT),
            eq("tenant-a"),
            contains("\"serviceCode\":\"QUERY_EXECUTION\""),
            anyMap()
        );
    }

    @Test
    void shouldRejectAuditEventWhenRequiredContractFieldsMissing() {
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        MessageProducer messageProducer = mock(MessageProducer.class);
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.DATABASE);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            tenantAccessLogic,
            messageProducer,
            messagingProperties
        );
        RequestContext.set(
            "tenant-a",
            "user-01",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            100L,
            200L
        );

        AuditWriteRequest request = new AuditWriteRequest();
        request.setServiceCode(" ");
        request.setResourceType("AuditLog");
        request.setResourceId("audit-001");
        request.setResultStatus("SUCCESS");
        request.setElapsedMs(10L);
        request.setSourceIp("127.0.0.1");
        request.setUserAgent("JUnit");

        BizException ex = assertThrows(BizException.class, () -> service.publishAuditEvent(request));

        assertEquals(ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID, ex.getCode());
    }

    @Test
    void shouldTranslateAuditRouteFailureToGovernanceSystemError() {
        TenantAccessLogic tenantAccessLogic = mock(TenantAccessLogic.class);
        MessageProducer messageProducer = mock(MessageProducer.class);
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.DATABASE);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            tenantAccessLogic,
            messageProducer,
            messagingProperties
        );
        RequestContext.set(
            "tenant-a",
            "user-01",
            Arrays.asList("TENANT_ADMIN"),
            "request-030",
            "trace-030",
            "header",
            100L,
            200L
        );
        AuditWriteRequest request = new AuditWriteRequest();
        request.setServiceCode("QUERY_EXECUTION");
        request.setOperationCode("AUDIT_QUERY");
        request.setResourceType("AuditLog");
        request.setResourceId("audit-001");
        request.setResultStatus("SUCCESS");
        request.setElapsedMs(5L);
        request.setSourceIp("127.0.0.1");
        request.setUserAgent("JUnit");
        doThrow(new IllegalStateException("route unavailable"))
            .when(messageProducer)
            .send(eq(GovernanceMessagingTopics.AUDIT_EVENT), eq("tenant-a"), contains("\"operationCode\":\"AUDIT_QUERY\""), anyMap());

        BizException ex = assertThrows(BizException.class, () -> service.publishAuditEvent(request));

        assertEquals(ErrorCodeConstants.GOVERNANCE_SYSTEM_MESSAGE_ROUTE_INVALID, ex.getCode());
    }
}
