package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.application.controller.dto.AuditWriteRequest;
import com.company.governance.application.controller.dto.DatasourceAuthorizationChangeRequest;
import com.company.governance.application.controller.vo.AuditWriteResponse;
import com.company.governance.application.controller.vo.DatasourceAuthorizationChangeResponse;
import com.company.governance.application.controller.vo.ScheduleExtensionStatusVO;
import com.company.governance.config.MessagingProperties;
import com.company.governance.domain.tenant.entity.TenantConfig;
import com.company.governance.domain.tenant.repository.TenantConfigRepository;
import com.company.sqlforge.common.config.MessagingMode;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceAuthorizationDecisionRequest;
import com.company.sqlforge.common.governance.GovernanceAuthorizationDecisionResponse;
import com.company.sqlforge.common.governance.GovernanceTenantArtifactPolicyRequest;
import com.company.sqlforge.common.governance.GovernanceTenantArtifactPolicyResponse;
import com.company.sqlforge.common.governance.GovernanceTenantScopeCheckRequest;
import com.company.sqlforge.common.governance.GovernanceTenantScopeCheckResponse;
import java.util.Optional;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GovernanceCapabilityApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldDelegateAuthorizationEntryPointsToMatrixService() {
        GovernanceAuthorizationMatrixApplicationService matrixService =
            mock(GovernanceAuthorizationMatrixApplicationService.class);
        GovernanceAuditTrailService governanceAuditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceBenchmarkTraceabilityApplicationService benchmarkTraceabilityApplicationService =
            mock(GovernanceBenchmarkTraceabilityApplicationService.class);
        TenantConfigRepository tenantConfigRepository = mock(TenantConfigRepository.class);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            matrixService,
            governanceAuditTrailService,
            benchmarkTraceabilityApplicationService,
            databaseMessaging(),
            tenantConfigRepository
        );
        RequestContext.set(
            "tenant-a",
            "tenant-admin-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            100L,
            200L
        );

        GovernanceTenantScopeCheckRequest tenantScopeRequest = new GovernanceTenantScopeCheckRequest();
        tenantScopeRequest.setTenantId("tenant-a");
        tenantScopeRequest.setTargetTenantId("tenant-a");
        GovernanceTenantScopeCheckResponse tenantScopeResponse = new GovernanceTenantScopeCheckResponse();
        tenantScopeResponse.setTenantId("tenant-a");
        tenantScopeResponse.setTargetTenantId("tenant-a");
        tenantScopeResponse.setAllowed(true);
        tenantScopeResponse.setReason("ALLOWED");

        GovernanceAuthorizationDecisionRequest decisionRequest = new GovernanceAuthorizationDecisionRequest();
        decisionRequest.setServiceCode("QUERY_EXECUTION");
        decisionRequest.setTenantId("tenant-a");
        decisionRequest.setResourceType("QUERY_EXECUTION_QUERY");
        decisionRequest.setResourceId("fp-001");
        decisionRequest.setOperationCode("QUERY_EXECUTE_SYNC");
        decisionRequest.setDatasourceId("query-hetu");
        GovernanceAuthorizationDecisionResponse decisionResponse = new GovernanceAuthorizationDecisionResponse();
        decisionResponse.setTenantId("tenant-a");
        decisionResponse.setResourceType("QUERY_EXECUTION_QUERY");
        decisionResponse.setResourceId("fp-001");
        decisionResponse.setOperationCode("QUERY_EXECUTE_SYNC");
        decisionResponse.setDatasourceId("query-hetu");
        decisionResponse.setAllowed(true);
        decisionResponse.setReason("ALLOWED");

        DatasourceAuthorizationChangeRequest changeRequest = new DatasourceAuthorizationChangeRequest();
        changeRequest.setTenantId("tenant-a");
        changeRequest.setDatasourceId("query-hetu");
        changeRequest.setState("REVOKED");
        changeRequest.setChangeReason("runtime revoke");
        DatasourceAuthorizationChangeResponse changeResponse = new DatasourceAuthorizationChangeResponse(
            "tenant-a",
            "query-hetu",
            "REVOKED",
            java.util.Collections.<String>emptyList(),
            "UPDATED",
            "LONG_TERM_BASELINE",
            "AUTHORIZATION_MATRIX_BASELINE"
        );

        when(matrixService.checkTenantScope(tenantScopeRequest)).thenReturn(tenantScopeResponse);
        when(matrixService.decideAuthorization(decisionRequest)).thenReturn(decisionResponse);
        when(matrixService.applyDatasourceAuthorizationChange(changeRequest)).thenReturn(changeResponse);

        GovernanceTenantScopeCheckResponse actualTenantScope = service.checkTenantScope(tenantScopeRequest);
        GovernanceAuthorizationDecisionResponse actualDecision = service.decideAuthorization(decisionRequest);
        DatasourceAuthorizationChangeResponse actualChange = service.changeDatasourceAuthorization(changeRequest);

        assertEquals("ALLOWED", actualTenantScope.getReason());
        assertEquals("ALLOWED", actualDecision.getReason());
        assertEquals("UPDATED", actualChange.getStatus());
        verify(matrixService).checkTenantScope(tenantScopeRequest);
        verify(matrixService).decideAuthorization(decisionRequest);
        verify(matrixService).applyDatasourceAuthorizationChange(changeRequest);
    }

    @Test
    void shouldDelegateAuditWriteAndResolveScheduleStatus() {
        GovernanceAuthorizationMatrixApplicationService matrixService =
            mock(GovernanceAuthorizationMatrixApplicationService.class);
        GovernanceAuditTrailService governanceAuditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceBenchmarkTraceabilityApplicationService benchmarkTraceabilityApplicationService =
            mock(GovernanceBenchmarkTraceabilityApplicationService.class);
        TenantConfigRepository tenantConfigRepository = mock(TenantConfigRepository.class);
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.MOCK);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            matrixService,
            governanceAuditTrailService,
            benchmarkTraceabilityApplicationService,
            messagingProperties,
            tenantConfigRepository
        );
        RequestContext.set(
            "tenant-a",
            "user-01",
            Arrays.asList("TENANT_ADMIN"),
            "request-010",
            "trace-010",
            "header",
            100L,
            200L
        );

        AuditWriteRequest request = new AuditWriteRequest();
        request.setServiceCode("QUERY_EXECUTION");
        request.setOperationCode("QUERY_EXECUTE_SYNC");
        request.setResourceType("QUERY_EXECUTION_QUERY");
        request.setResourceId("fp-010");
        request.setResultStatus("SUCCESS");
        request.setElapsedMs(42L);
        request.setSourceIp("127.0.0.1");
        request.setUserAgent("JUnit");
        when(governanceAuditTrailService.writeAudit(request)).thenReturn(new AuditWriteResponse(
            Long.valueOf(101L),
            "QUERY_EXECUTION",
            "QUERY_EXECUTE_SYNC",
            "ACCEPTED",
            "governance.audit.event",
            "MOCK",
            "LONG_TERM_BASELINE",
            "DATABASE_AUDIT_WRITE_BASELINE"
        ));

        AuditWriteResponse response = service.publishAuditEvent(request);
        ScheduleExtensionStatusVO extensionStatus = service.getScheduleExtensionStatus();

        assertEquals(Long.valueOf(101L), response.getAuditId());
        assertEquals("TEST_ONLY", extensionStatus.getStatus());
        assertEquals("MOCK", extensionStatus.getCurrentMode());
        verify(governanceAuditTrailService).writeAudit(request);
    }

    @Test
    void shouldRejectWhenProtectedContextMissing() {
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            mock(GovernanceAuthorizationMatrixApplicationService.class),
            mock(GovernanceAuditTrailService.class),
            mock(GovernanceBenchmarkTraceabilityApplicationService.class),
            databaseMessaging(),
            mock(TenantConfigRepository.class)
        );
        GovernanceAuthorizationDecisionRequest request = new GovernanceAuthorizationDecisionRequest();
        request.setServiceCode("QUERY_EXECUTION");
        request.setTenantId("tenant-a");
        request.setResourceType("QUERY_EXECUTION_QUERY");
        request.setOperationCode("QUERY_EXECUTE_SYNC");
        request.setDatasourceId("query-hetu");

        BizException ex = assertThrows(BizException.class, () -> service.decideAuthorization(request));

        assertEquals(ErrorCodeConstants.SYSTEM_CONTEXT_MISSING, ex.getCode());
    }

    @Test
    void shouldResolveTenantArtifactPolicyFromTenantConfig() {
        TenantConfigRepository tenantConfigRepository = mock(TenantConfigRepository.class);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            mock(GovernanceAuthorizationMatrixApplicationService.class),
            mock(GovernanceAuditTrailService.class),
            mock(GovernanceBenchmarkTraceabilityApplicationService.class),
            databaseMessaging(),
            tenantConfigRepository
        );
        RequestContext.set(
            "tenant-a",
            "service-user",
            Arrays.asList("SERVICE"),
            "request-020",
            "trace-020",
            "header",
            100L,
            200L
        );
        TenantConfig tenantConfig = new TenantConfig();
        tenantConfig.setTenantId("tenant-a");
        tenantConfig.setRetentionDays(Integer.valueOf(180));
        when(tenantConfigRepository.findByTenantId("tenant-a")).thenReturn(Optional.of(tenantConfig));
        GovernanceTenantArtifactPolicyRequest request = new GovernanceTenantArtifactPolicyRequest();
        request.setTenantId("tenant-a");
        request.setPolicyScope("BENCHMARK_ARTIFACT");

        GovernanceTenantArtifactPolicyResponse response = service.resolveTenantArtifactPolicy(request);

        assertEquals("tenant-a", response.getTenantId());
        assertEquals(Integer.valueOf(180), response.getRetentionDays());
        assertEquals("GOVERNANCE_TENANT_CONFIG_RETENTION_DAYS", response.getRetentionPolicySource());
        assertEquals("TENANT_RETENTION_ACTIVE", response.getRetentionPolicyStatus());
        verify(tenantConfigRepository).findByTenantId("tenant-a");
    }

    private MessagingProperties databaseMessaging() {
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.DATABASE);
        return messagingProperties;
    }
}
