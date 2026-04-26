package com.company.queryexecution.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.company.queryexecution.application.controller.dto.QueryContextDTO;
import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.domain.query.AccelerationPreference;
import com.company.queryexecution.domain.query.FaultToleranceStrategy;
import com.company.queryexecution.domain.query.ApprovedAccelerationBinding;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.queryexecution.domain.query.QueryExecutionStatus;
import com.company.queryexecution.infrastructure.adapter.DeterministicQueryExecutionAdapter;
import com.company.queryexecution.infrastructure.adapter.HetuExecutionUnavailableException;
import com.company.queryexecution.infrastructure.adapter.QueryExecutionAdapter;
import com.company.queryexecution.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanApplyRequest;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class QueryExecutionApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldExecuteSynchronouslyForReadonlyHetuQuery(CapturedOutput output) {
        setRequestContext("tenant-a");
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        QueryExecutionApplicationService service =
            newService(new DeterministicQueryExecutionAdapter(), mockGovernanceClient(), meterRegistry);
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders");
        request.setAccelerationPreference(AccelerationPreference.PREFER_ACCELERATED);

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        assertEquals("HETU", response.getMetadata().getTargetEngine());
        assertEquals("HETU_REAL_INTEGRATION", response.getImplementationStage());
        assertFalse(response.isDegraded());
        assertNull(response.getError());
        assertEquals(1, response.getRows().size());
        assertFalse(response.getMetadata().isAccelerationApplied());
        assertEquals("SIMULATED", response.getMetadata().getExecutionMode());
        assertEquals(1, response.getMetadata().getAttemptedModes().size());
        assertEquals("SIMULATED", response.getMetadata().getAttemptedModes().get(0));
        assertEquals(1, response.getMetadata().getRowCount());
        assertTrue(output.getOut().contains("operation=QUERY_EXECUTE_SYNC"));
        assertTrue(output.getOut().contains("status=START"));
        assertTrue(output.getOut().contains("to=PRIMARY_ROUTE_SELECTED"));
        assertTrue(output.getOut().contains("status=END resultStatus=SUCCESS"));
        assertFalse(output.getOut().contains("SELECT * FROM orders"));
        assertEquals(1.0D, meterRegistry.get("sqlforge.query.execution.requests").tags(
            "requested_datasource", "HETU",
            "target_engine", "HETU",
            "result_status", "SUCCESS",
            "degraded", "false",
            "execution_mode", "SIMULATED",
            "fault_tolerance", "FAIL_FAST"
        ).counter().count());
        assertEquals(1L, meterRegistry.get("sqlforge.query.execution.latency").timer().count());
        assertEquals(1.0D, meterRegistry.get("sqlforge.query.execution.mode.hits").tags(
            "requested_datasource", "HETU",
            "target_engine", "HETU",
            "mode", "SIMULATED"
        ).counter().count());
    }

    @Test
    void shouldApplyAccelerationOnlyWhenApprovedBindingExists() {
        setRequestContext("tenant-a");
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        QueryExecutionAccelerationRuntimeService runtimeService = new QueryExecutionAccelerationRuntimeService();
        QueryExecutionAccelerationPlanApplyRequest applyRequest = new QueryExecutionAccelerationPlanApplyRequest();
        applyRequest.setTenantId("tenant-a");
        applyRequest.setPlanId("plan-001");
        applyRequest.setSqlFingerprint(com.company.sqlforge.common.utils.SqlFingerprintUtils.fingerprint("SELECT * FROM orders"));
        applyRequest.setDatasourceType("HETU");
        applyRequest.setSelectedSuggestionTypes(java.util.Collections.singletonList("PRECOMPUTE"));
        applyRequest.setPlanSummary("approved plan");
        applyRequest.setPrimaryRecommendation("use approved runtime config");
        runtimeService.apply(applyRequest);

        QueryExecutionApplicationService service = new QueryExecutionApplicationService(
            new DeterministicQueryExecutionAdapter(),
            governanceCapabilityClient,
            QueryExecutionMetricsRecorder.noop(),
            runtimeService
        );
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders");
        request.setAccelerationPreference(AccelerationPreference.PREFER_ACCELERATED);

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        assertTrue(response.getMetadata().isAccelerationApplied());
    }

    @Test
    void shouldReturnTimeoutWhenFailFastThresholdIsExceeded() {
        setRequestContext("tenant-a");
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new DeterministicQueryExecutionAdapter(), mockGovernanceClient());
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders");
        request.setQueryContext(timeoutContext(30L));
        request.setFaultToleranceStrategy(FaultToleranceStrategy.FAIL_FAST);

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.TIMEOUT, response.getStatus());
        assertNotNull(response.getError());
        assertEquals(ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ENGINE_TIMEOUT, response.getError().getCode());
        assertEquals(1, response.getRetryPath().size());
        assertEquals("HETU", response.getRetryPath().get(0).getEngine());
        assertEquals("TIMEOUT", response.getRetryPath().get(0).getResultStatus());
        assertEquals("LOCAL_TIMEOUT_ROLLBACK_MARKED", response.getRetryPath().get(0).getLocalRecoveryMarker());
        assertEquals("CLOSE_PRIMARY_ATTEMPT_CONTEXT", response.getRetryPath().get(0).getLocalRecoveryAction());
    }

    @Test
    void shouldRejectNonReadonlySqlBeforeExecution() {
        setRequestContext("tenant-a");
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new DeterministicQueryExecutionAdapter(), mockGovernanceClient());
        QueryExecuteRequest request = baseRequest("DELETE FROM orders");

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.FAILED, response.getStatus());
        assertNotNull(response.getError());
        assertEquals(ErrorCodeConstants.QUERY_EXECUTION_RISK_REJECTED, response.getError().getCode());
        assertTrue(response.getRows().isEmpty());
        assertFalse(response.isDegraded());
    }

    @Test
    void shouldFallbackToHiveWhenRetryThenFallbackIsEnabled() {
        setRequestContext("tenant-a");
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        QueryExecutionApplicationService service =
            newService(new DeterministicQueryExecutionAdapter(), mockGovernanceClient(), meterRegistry);
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders");
        request.setQueryContext(timeoutContext(30L));
        request.setFaultToleranceStrategy(FaultToleranceStrategy.RETRY_THEN_FALLBACK);

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.PARTIAL, response.getStatus());
        assertTrue(response.isDegraded());
        assertEquals("HIVE", response.getMetadata().getTargetEngine());
        assertEquals(2, response.getRetryPath().size());
        assertEquals("HETU", response.getRetryPath().get(0).getEngine());
        assertEquals("HIVE", response.getRetryPath().get(1).getEngine());
        assertEquals("LOCAL_TIMEOUT_ROLLBACK_MARKED", response.getRetryPath().get(0).getLocalRecoveryMarker());
        assertEquals("LOCAL_FALLBACK_COMPENSATION_MARKED", response.getRetryPath().get(1).getLocalRecoveryMarker());
        assertEquals("RECORD_DEGRADED_RESULT", response.getRetryPath().get(1).getLocalRecoveryAction());
        assertEquals("HIVE_FALLBACK", response.getMetadata().getExecutionMode());
        assertEquals(1, response.getMetadata().getAttemptedModes().size());
        assertEquals("HIVE_FALLBACK", response.getMetadata().getAttemptedModes().get(0));
        assertEquals(1, response.getMetadata().getRowCount());
        assertNull(response.getError());
        assertEquals(1.0D, meterRegistry.get("sqlforge.query.execution.fallbacks").tags(
            "requested_datasource", "HETU",
            "target_engine", "HIVE",
            "fault_tolerance", "RETRY_THEN_FALLBACK",
            "execution_mode", "HIVE_FALLBACK"
        ).counter().count());
        assertEquals(1.0D, meterRegistry.get("sqlforge.query.execution.timeouts").tags(
            "requested_datasource", "HETU",
            "target_engine", "HIVE",
            "fault_tolerance", "RETRY_THEN_FALLBACK"
        ).counter().count());
    }

    @Test
    void shouldReturnStructuredRouteUnavailableWhenHetuChainIsNotAvailable() {
        setRequestContext("tenant-a");
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        QueryExecutionApplicationService service =
            newService(new QueryExecutionAdapter() {
                @Override
                public QueryExecutionStep execute(DataSourceTypeEnum targetEngine,
                                                  String actualSql,
                                                  QueryExecuteRequest request,
                                                  boolean degradedPath) {
                    throw new HetuExecutionUnavailableException(
                        "Hetu execution chain is disabled for the current environment",
                        java.util.Collections.singletonList("CHAIN_DISABLED"),
                        "REPO_CLOSED_BASELINE",
                        java.util.Arrays.asList("JDBC", "REST", "CLIENT"),
                        "REPO_CLOSED_CONFIGURATION",
                        "PENDING_ENV_WINDOW"
                    );
                }
            }, mockGovernanceClient(), meterRegistry);

        QueryExecuteResponse response = service.executeSynchronously(baseRequest("SELECT * FROM orders"));

        assertEquals(QueryExecutionStatus.FAILED, response.getStatus());
        assertEquals(ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ROUTE_UNAVAILABLE, response.getError().getCode());
        assertEquals(java.util.Collections.singletonList("CHAIN_DISABLED"), response.getMetadata().getAttemptedModes());
        assertEquals("REPO_CLOSED_BASELINE", response.getMetadata().getRouteProfile());
        assertEquals(java.util.Arrays.asList("JDBC", "REST", "CLIENT"), response.getMetadata().getRouteOrder());
        assertEquals("REPO_CLOSED_CONFIGURATION", response.getMetadata().getRouteEvidenceSource());
        assertEquals(1, response.getRetryPath().size());
        assertEquals("LOCAL_PRIMARY_ROUTE_FAILURE_MARKED", response.getRetryPath().get(0).getLocalRecoveryMarker());
        assertEquals(1.0D, meterRegistry.get("sqlforge.query.execution.route_unavailable").tags(
            "requested_datasource", "HETU",
            "target_engine", "HETU",
            "fault_tolerance", "FAIL_FAST"
        ).counter().count());
        assertEquals(1.0D, meterRegistry.get("sqlforge.query.execution.mode.attempts").tags(
            "requested_datasource", "HETU",
            "mode", "CHAIN",
            "outcome", "DISABLED"
        ).counter().count());
    }

    @Test
    void shouldFallbackToHiveWhenHetuModeChainFailsAndFallbackIsEnabled() {
        setRequestContext("tenant-a");
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new QueryExecutionAdapter() {
                @Override
                public QueryExecutionStep execute(DataSourceTypeEnum targetEngine,
                                                  String actualSql,
                                                  QueryExecuteRequest request,
                                                  boolean degradedPath) {
                    if (targetEngine == DataSourceTypeEnum.HIVE) {
                        return new DeterministicQueryExecutionAdapter().execute(targetEngine, actualSql, request, degradedPath);
                    }
                    throw new HetuExecutionUnavailableException(
                        "No calibrated Hetu execution mode succeeded. attemptedModes=[JDBC, JDBC:FAILED_EXECUTION, REST, REST:FAILED_EXECUTION]",
                        java.util.Arrays.asList("JDBC", "JDBC:FAILED_EXECUTION", "REST", "REST:FAILED_EXECUTION"),
                        "REPO_CLOSED_BASELINE",
                        java.util.Arrays.asList("JDBC", "REST"),
                        "REPO_CLOSED_CONFIGURATION",
                        "PENDING_ENV_WINDOW"
                    );
                }
            }, mockGovernanceClient());
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders");
        request.setFaultToleranceStrategy(FaultToleranceStrategy.RETRY_THEN_FALLBACK);

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.PARTIAL, response.getStatus());
        assertTrue(response.isDegraded());
        assertEquals("HIVE", response.getMetadata().getTargetEngine());
        assertEquals(2, response.getRetryPath().size());
        assertEquals("LOCAL_PRIMARY_ROUTE_FAILURE_MARKED", response.getRetryPath().get(0).getLocalRecoveryMarker());
        assertEquals("LOCAL_FALLBACK_COMPENSATION_MARKED", response.getRetryPath().get(1).getLocalRecoveryMarker());
    }

    @Test
    void shouldLogExceptionWhenExecutionAdapterFails(CapturedOutput output) {
        setRequestContext("tenant-a");
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new QueryExecutionAdapter() {
                @Override
                public QueryExecutionStep execute(DataSourceTypeEnum targetEngine,
                                                  String actualSql,
                                                  QueryExecuteRequest request,
                                                  boolean degradedPath) {
                    throw new IllegalStateException("simulated adapter failure");
                }
            }, governanceCapabilityClient);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.executeSynchronously(
            baseRequest("SELECT * FROM orders")
        ));

        assertEquals("simulated adapter failure", ex.getMessage());
        assertTrue(output.getOut().contains("status=FAILED phase=EXCEPTION"));
        assertTrue(output.getOut().contains("reason=simulated adapter failure"));
        verify(governanceCapabilityClient).writeAudit(any());
    }

    @Test
    void shouldCallGovernanceChecksAndAuditOnSuccessfulExecution() {
        setRequestContext("tenant-a");
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new DeterministicQueryExecutionAdapter(), governanceCapabilityClient);

        QueryExecuteResponse response = service.executeSynchronously(baseRequest("SELECT * FROM orders"));

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        verify(governanceCapabilityClient).assertAuthorization(
            org.mockito.Mockito.eq("tenant-a"),
            org.mockito.Mockito.eq(DataSourceTypeEnum.HETU),
            org.mockito.Mockito.eq("QUERY_EXECUTION_QUERY"),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.Mockito.eq("QUERY_EXECUTE_SYNC")
        );
        verify(governanceCapabilityClient).writeAudit(any());
    }

    @Test
    void shouldRejectSpoofedTenantBeforeLoggingOrGovernanceCall() {
        setRequestContext("tenant-a");
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new DeterministicQueryExecutionAdapter(), governanceCapabilityClient);
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders");
        request.setTenantId("tenant-b");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> service.executeSynchronously(request));

        assertEquals("Request tenantId does not match authenticated tenant context", ex.getMessage());
        verify(governanceCapabilityClient, org.mockito.Mockito.never()).assertAuthorization(any(), any(), any(), any(), any());
        verify(governanceCapabilityClient, org.mockito.Mockito.never()).writeAudit(any());
    }

    private QueryExecuteRequest baseRequest(String sqlText) {
        QueryExecuteRequest request = new QueryExecuteRequest();
        request.setSqlText(sqlText);
        request.setTenantId("tenant-a");
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        return request;
    }

    private QueryContextDTO timeoutContext(Long timeoutMs) {
        QueryContextDTO queryContext = new QueryContextDTO();
        queryContext.setTimeoutMs(timeoutMs);
        return queryContext;
    }

    private GovernanceCapabilityClient mockGovernanceClient() {
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        doNothing().when(governanceCapabilityClient).assertAuthorization(any(), any(), any(), any(), any());
        doNothing().when(governanceCapabilityClient).writeAudit(any());
        return governanceCapabilityClient;
    }

    private QueryExecutionApplicationService newService(QueryExecutionAdapter adapter,
                                                        GovernanceCapabilityClient governanceCapabilityClient,
                                                        SimpleMeterRegistry meterRegistry) {
        return new QueryExecutionApplicationService(
            adapter,
            governanceCapabilityClient,
            new QueryExecutionMetricsRecorder(meterRegistry),
            new QueryExecutionAccelerationRuntimeService()
        );
    }

    private void setRequestContext(String tenantId) {
        RequestContext.set(
            tenantId,
            "operator-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            1L,
            System.currentTimeMillis() + 60000L
        );
    }
}
