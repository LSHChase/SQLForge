package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.application.controller.dto.AuditWriteRequest;
import com.company.governance.application.controller.dto.DatasourceAccessScopeChangeRequest;
import com.company.governance.application.controller.vo.AuditWriteResponse;
import com.company.governance.application.controller.vo.DatasourceAccessScopeChangeResponse;
import com.company.governance.application.controller.vo.ScheduleExtensionStatusVO;
import com.company.governance.config.MessagingProperties;
import com.company.governance.domain.tenant.entity.TenantConfig;
import com.company.governance.domain.tenant.repository.TenantConfigRepository;
import com.company.sqlforge.common.config.MessagingMode;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceRequest;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceResponse;
import com.company.sqlforge.common.governance.GovernanceDatasourceAccessCheckRequest;
import com.company.sqlforge.common.governance.GovernanceDatasourceAccessCheckResponse;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertResponse;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveRequest;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveResponse;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteResponse;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigRequest;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigResponse;
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
        GovernanceDatasourceAccessApplicationService matrixService =
            mock(GovernanceDatasourceAccessApplicationService.class);
        GovernanceAuditTrailService governanceAuditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceBenchmarkTraceabilityApplicationService benchmarkTraceabilityApplicationService =
            mock(GovernanceBenchmarkTraceabilityApplicationService.class);
        GovernanceAccelerationPlanTraceabilityApplicationService accelerationPlanTraceabilityApplicationService =
            mock(GovernanceAccelerationPlanTraceabilityApplicationService.class);
        DatabaseViewCatalogApplicationService databaseViewCatalogApplicationService =
            mock(DatabaseViewCatalogApplicationService.class);
        TenantConfigRepository tenantConfigRepository = mock(TenantConfigRepository.class);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            matrixService,
            governanceAuditTrailService,
            benchmarkTraceabilityApplicationService,
            mock(GovernanceBenchmarkRegressionAlertApplicationService.class),
            accelerationPlanTraceabilityApplicationService,
            mock(GovernanceQueryExecutionHistoryApplicationService.class),
            databaseViewCatalogApplicationService,
            mock(ReportInterfaceConfigApplicationService.class),
            mock(DatasourceConfigApplicationService.class),
            databaseMessaging(),
            tenantConfigRepository
        );
        RequestContext.set(
            "tenant-a",
            "tenant-admin-001",
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

        GovernanceDatasourceAccessCheckRequest decisionRequest = new GovernanceDatasourceAccessCheckRequest();
        decisionRequest.setServiceCode("QUERY_EXECUTION");
        decisionRequest.setTenantId("tenant-a");
        decisionRequest.setResourceType("QUERY_EXECUTION_QUERY");
        decisionRequest.setResourceId("fp-001");
        decisionRequest.setOperationCode("QUERY_EXECUTE_SYNC");
        decisionRequest.setDatasourceId("query-hetu");
        GovernanceDatasourceAccessCheckResponse decisionResponse = new GovernanceDatasourceAccessCheckResponse();
        decisionResponse.setTenantId("tenant-a");
        decisionResponse.setResourceType("QUERY_EXECUTION_QUERY");
        decisionResponse.setResourceId("fp-001");
        decisionResponse.setOperationCode("QUERY_EXECUTE_SYNC");
        decisionResponse.setDatasourceId("query-hetu");
        decisionResponse.setAllowed(true);
        decisionResponse.setReason("ALLOWED");

        DatasourceAccessScopeChangeRequest changeRequest = new DatasourceAccessScopeChangeRequest();
        changeRequest.setTenantId("tenant-a");
        changeRequest.setDatasourceId("query-hetu");
        changeRequest.setState("REVOKED");
        changeRequest.setChangeReason("runtime revoke");
        DatasourceAccessScopeChangeResponse changeResponse = new DatasourceAccessScopeChangeResponse(
            "tenant-a",
            "query-hetu",
            "REVOKED",
            java.util.Collections.<String>emptyList(),
            "UPDATED",
            "LONG_TERM_BASELINE",
            "DATASOURCE_ACCESS_SCOPE_BASELINE"
        );

        when(matrixService.checkTenantScope(tenantScopeRequest)).thenReturn(tenantScopeResponse);
        when(matrixService.checkDatasourceAccess(decisionRequest)).thenReturn(decisionResponse);
        when(matrixService.applyDatasourceAccessScopeChange(changeRequest)).thenReturn(changeResponse);

        GovernanceTenantScopeCheckResponse actualTenantScope = service.checkTenantScope(tenantScopeRequest);
        GovernanceDatasourceAccessCheckResponse actualDecision = service.checkDatasourceAccess(decisionRequest);
        DatasourceAccessScopeChangeResponse actualChange = service.changeDatasourceAccessScope(changeRequest);

        assertEquals("ALLOWED", actualTenantScope.getReason());
        assertEquals("ALLOWED", actualDecision.getReason());
        assertEquals("UPDATED", actualChange.getStatus());
        verify(matrixService).checkTenantScope(tenantScopeRequest);
        verify(matrixService).checkDatasourceAccess(decisionRequest);
        verify(matrixService).applyDatasourceAccessScopeChange(changeRequest);
    }

    @Test
    void shouldDelegateAuditWriteAndResolveScheduleStatus() {
        GovernanceDatasourceAccessApplicationService matrixService =
            mock(GovernanceDatasourceAccessApplicationService.class);
        GovernanceAuditTrailService governanceAuditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceBenchmarkTraceabilityApplicationService benchmarkTraceabilityApplicationService =
            mock(GovernanceBenchmarkTraceabilityApplicationService.class);
        GovernanceAccelerationPlanTraceabilityApplicationService accelerationPlanTraceabilityApplicationService =
            mock(GovernanceAccelerationPlanTraceabilityApplicationService.class);
        DatabaseViewCatalogApplicationService databaseViewCatalogApplicationService =
            mock(DatabaseViewCatalogApplicationService.class);
        TenantConfigRepository tenantConfigRepository = mock(TenantConfigRepository.class);
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.MOCK);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            matrixService,
            governanceAuditTrailService,
            benchmarkTraceabilityApplicationService,
            mock(GovernanceBenchmarkRegressionAlertApplicationService.class),
            accelerationPlanTraceabilityApplicationService,
            mock(GovernanceQueryExecutionHistoryApplicationService.class),
            databaseViewCatalogApplicationService,
            mock(ReportInterfaceConfigApplicationService.class),
            mock(DatasourceConfigApplicationService.class),
            messagingProperties,
            tenantConfigRepository
        );
        RequestContext.set(
            "tenant-a",
            "user-01",
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
            mock(GovernanceDatasourceAccessApplicationService.class),
            mock(GovernanceAuditTrailService.class),
            mock(GovernanceBenchmarkTraceabilityApplicationService.class),
            mock(GovernanceBenchmarkRegressionAlertApplicationService.class),
            mock(GovernanceAccelerationPlanTraceabilityApplicationService.class),
            mock(GovernanceQueryExecutionHistoryApplicationService.class),
            mock(DatabaseViewCatalogApplicationService.class),
            mock(ReportInterfaceConfigApplicationService.class),
            mock(DatasourceConfigApplicationService.class),
            databaseMessaging(),
            mock(TenantConfigRepository.class)
        );
        GovernanceDatasourceAccessCheckRequest request = new GovernanceDatasourceAccessCheckRequest();
        request.setServiceCode("QUERY_EXECUTION");
        request.setTenantId("tenant-a");
        request.setResourceType("QUERY_EXECUTION_QUERY");
        request.setOperationCode("QUERY_EXECUTE_SYNC");
        request.setDatasourceId("query-hetu");

        BizException ex = assertThrows(BizException.class, () -> service.checkDatasourceAccess(request));

        assertEquals(ErrorCodeConstants.SYSTEM_CONTEXT_MISSING, ex.getCode());
    }

    @Test
    void shouldResolveTenantArtifactPolicyFromTenantConfig() {
        TenantConfigRepository tenantConfigRepository = mock(TenantConfigRepository.class);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            mock(GovernanceDatasourceAccessApplicationService.class),
            mock(GovernanceAuditTrailService.class),
            mock(GovernanceBenchmarkTraceabilityApplicationService.class),
            mock(GovernanceBenchmarkRegressionAlertApplicationService.class),
            mock(GovernanceAccelerationPlanTraceabilityApplicationService.class),
            mock(GovernanceQueryExecutionHistoryApplicationService.class),
            mock(DatabaseViewCatalogApplicationService.class),
            mock(ReportInterfaceConfigApplicationService.class),
            mock(DatasourceConfigApplicationService.class),
            databaseMessaging(),
            tenantConfigRepository
        );
        RequestContext.set(
            "tenant-a",
            "service-user",
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

    @Test
    void shouldDelegateAccelerationPlanTraceWrite() {
        GovernanceAccelerationPlanTraceabilityApplicationService accelerationPlanTraceabilityApplicationService =
            mock(GovernanceAccelerationPlanTraceabilityApplicationService.class);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            mock(GovernanceDatasourceAccessApplicationService.class),
            mock(GovernanceAuditTrailService.class),
            mock(GovernanceBenchmarkTraceabilityApplicationService.class),
            mock(GovernanceBenchmarkRegressionAlertApplicationService.class),
            accelerationPlanTraceabilityApplicationService,
            mock(GovernanceQueryExecutionHistoryApplicationService.class),
            mock(DatabaseViewCatalogApplicationService.class),
            mock(ReportInterfaceConfigApplicationService.class),
            mock(DatasourceConfigApplicationService.class),
            databaseMessaging(),
            mock(TenantConfigRepository.class)
        );
        RequestContext.set(
            "tenant-a",
            "service-user",
            "request-030",
            "trace-030",
            "header",
            100L,
            200L
        );
        GovernanceAccelerationPlanTraceRequest request = new GovernanceAccelerationPlanTraceRequest();
        request.setPlanId("plan-001");
        request.setSourceTaskId("task-001");
        request.setSqlFingerprint("fp-001");
        request.setDatasourceType("HETU");
        request.setPlanStatus("READY");
        GovernanceAccelerationPlanTraceResponse traceResponse = new GovernanceAccelerationPlanTraceResponse();
        traceResponse.setConfigSnapshotId("cfg-acceleration-plan-plan-001");
        when(accelerationPlanTraceabilityApplicationService.writeAccelerationPlanTrace(request)).thenReturn(traceResponse);

        GovernanceAccelerationPlanTraceResponse response = service.writeAccelerationPlanTrace(request);

        assertEquals("cfg-acceleration-plan-plan-001", response.getConfigSnapshotId());
        verify(accelerationPlanTraceabilityApplicationService).writeAccelerationPlanTrace(request);
    }

    @Test
    void shouldDelegateQueryExecutionHistoryWrite() {
        GovernanceQueryExecutionHistoryApplicationService queryExecutionHistoryApplicationService =
            mock(GovernanceQueryExecutionHistoryApplicationService.class);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            mock(GovernanceDatasourceAccessApplicationService.class),
            mock(GovernanceAuditTrailService.class),
            mock(GovernanceBenchmarkTraceabilityApplicationService.class),
            mock(GovernanceBenchmarkRegressionAlertApplicationService.class),
            mock(GovernanceAccelerationPlanTraceabilityApplicationService.class),
            queryExecutionHistoryApplicationService,
            mock(DatabaseViewCatalogApplicationService.class),
            mock(ReportInterfaceConfigApplicationService.class),
            mock(DatasourceConfigApplicationService.class),
            databaseMessaging(),
            mock(TenantConfigRepository.class)
        );
        RequestContext.set(
            "tenant-a",
            "service-user",
            "request-031",
            "trace-031",
            "header",
            100L,
            200L
        );
        GovernanceQueryExecutionHistoryWriteRequest request = new GovernanceQueryExecutionHistoryWriteRequest();
        request.setTenantId("tenant-a");
        request.setSqlFingerprint("fp-031");
        request.setDatasourceType("HETU");
        request.setResultStatus("SUCCESS");
        GovernanceQueryExecutionHistoryWriteResponse historyResponse =
            new GovernanceQueryExecutionHistoryWriteResponse();
        historyResponse.setHistoryId("history-qe-031");
        when(queryExecutionHistoryApplicationService.writeQueryExecutionHistory(request)).thenReturn(historyResponse);

        GovernanceQueryExecutionHistoryWriteResponse response = service.writeQueryExecutionHistory(request);

        assertEquals("history-qe-031", response.getHistoryId());
        verify(queryExecutionHistoryApplicationService).writeQueryExecutionHistory(request);
    }

    @Test
    void shouldDelegateDbViewResolution() {
        DatabaseViewCatalogApplicationService databaseViewCatalogApplicationService =
            mock(DatabaseViewCatalogApplicationService.class);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            mock(GovernanceDatasourceAccessApplicationService.class),
            mock(GovernanceAuditTrailService.class),
            mock(GovernanceBenchmarkTraceabilityApplicationService.class),
            mock(GovernanceBenchmarkRegressionAlertApplicationService.class),
            mock(GovernanceAccelerationPlanTraceabilityApplicationService.class),
            mock(GovernanceQueryExecutionHistoryApplicationService.class),
            databaseViewCatalogApplicationService,
            mock(ReportInterfaceConfigApplicationService.class),
            mock(DatasourceConfigApplicationService.class),
            databaseMessaging(),
            mock(TenantConfigRepository.class)
        );
        RequestContext.set(
            "tenant-a",
            "service-user",
            "request-040",
            "trace-040",
            "header",
            100L,
            200L
        );
        GovernanceDbViewResolveRequest request = new GovernanceDbViewResolveRequest();
        request.setTenantId("tenant-a");
        request.setDatasourceCode("hetu_main");
        request.setViewName("vw_sales_daily");
        GovernanceDbViewResolveResponse resolveResponse = new GovernanceDbViewResolveResponse();
        resolveResponse.setResolved(Boolean.TRUE);
        when(databaseViewCatalogApplicationService.resolveDbView(request)).thenReturn(resolveResponse);

        GovernanceDbViewResolveResponse response = service.resolveDbView(request);

        assertEquals(Boolean.TRUE, response.getResolved());
        verify(databaseViewCatalogApplicationService).resolveDbView(request);
    }

    @Test
    void shouldDelegateReportInterfaceConfigResolution() {
        ReportInterfaceConfigApplicationService reportInterfaceConfigApplicationService =
            mock(ReportInterfaceConfigApplicationService.class);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            mock(GovernanceDatasourceAccessApplicationService.class),
            mock(GovernanceAuditTrailService.class),
            mock(GovernanceBenchmarkTraceabilityApplicationService.class),
            mock(GovernanceBenchmarkRegressionAlertApplicationService.class),
            mock(GovernanceAccelerationPlanTraceabilityApplicationService.class),
            mock(GovernanceQueryExecutionHistoryApplicationService.class),
            mock(DatabaseViewCatalogApplicationService.class),
            reportInterfaceConfigApplicationService,
            mock(DatasourceConfigApplicationService.class),
            databaseMessaging(),
            mock(TenantConfigRepository.class)
        );
        RequestContext.set(
            "tenant-a",
            "service-user",
            "request-050",
            "trace-050",
            "header",
            100L,
            200L
        );
        GovernanceReportInterfaceConfigRequest request = new GovernanceReportInterfaceConfigRequest();
        request.setTenantId("tenant-a");
        request.setDatasourceCode("hetu_main");
        request.setStage("PROD");
        GovernanceReportInterfaceConfigResponse resolveResponse = new GovernanceReportInterfaceConfigResponse();
        resolveResponse.setResolverStatus("ACTIVE");
        when(reportInterfaceConfigApplicationService.resolve(request)).thenReturn(resolveResponse);

        GovernanceReportInterfaceConfigResponse response = service.resolveReportInterfaceConfig(request);

        assertEquals("ACTIVE", response.getResolverStatus());
        verify(reportInterfaceConfigApplicationService).resolve(request);
    }

    @Test
    void shouldDelegateBenchmarkRegressionAlertEmission() {
        GovernanceBenchmarkRegressionAlertApplicationService regressionAlertApplicationService =
            mock(GovernanceBenchmarkRegressionAlertApplicationService.class);
        GovernanceCapabilityApplicationService service = new GovernanceCapabilityApplicationService(
            mock(GovernanceDatasourceAccessApplicationService.class),
            mock(GovernanceAuditTrailService.class),
            mock(GovernanceBenchmarkTraceabilityApplicationService.class),
            regressionAlertApplicationService,
            mock(GovernanceAccelerationPlanTraceabilityApplicationService.class),
            mock(GovernanceQueryExecutionHistoryApplicationService.class),
            mock(DatabaseViewCatalogApplicationService.class),
            mock(ReportInterfaceConfigApplicationService.class),
            mock(DatasourceConfigApplicationService.class),
            databaseMessaging(),
            mock(TenantConfigRepository.class)
        );
        RequestContext.set(
            "tenant-a",
            "service-user",
            "request-060",
            "trace-060",
            "header",
            100L,
            200L
        );
        GovernanceBenchmarkRegressionAlertRequest request = new GovernanceBenchmarkRegressionAlertRequest();
        request.setTenantId("tenant-a");
        request.setReportId("report-001");
        request.setTaskId("task-001");
        GovernanceBenchmarkRegressionAlertResponse resolveResponse = new GovernanceBenchmarkRegressionAlertResponse();
        resolveResponse.setAlertTriggered(Boolean.TRUE);
        when(regressionAlertApplicationService.emit(request)).thenReturn(resolveResponse);

        GovernanceBenchmarkRegressionAlertResponse response = service.emitBenchmarkRegressionAlert(request);

        assertEquals(Boolean.TRUE, response.getAlertTriggered());
        verify(regressionAlertApplicationService).emit(request);
    }

    private MessagingProperties databaseMessaging() {
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.DATABASE);
        return messagingProperties;
    }
}
