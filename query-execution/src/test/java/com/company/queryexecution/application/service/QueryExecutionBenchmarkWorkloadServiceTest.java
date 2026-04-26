package com.company.queryexecution.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.application.controller.vo.QueryExecutionMetadataVO;
import com.company.queryexecution.domain.query.QueryExecutionStatus;
import com.company.queryexecution.infrastructure.governance.GovernanceCapabilityClient;
import com.company.queryexecution.infrastructure.governance.QueryExecutionAuditRecord;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadRequest;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QueryExecutionBenchmarkWorkloadServiceTest {

    @Test
    void shouldCaptureLiveQueryExecutionWorkload() {
        QueryExecutionApplicationService queryExecutionApplicationService = mock(QueryExecutionApplicationService.class);
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        when(queryExecutionApplicationService.executeSynchronously(any()))
            .thenReturn(new QueryExecuteResponse(
                QueryExecutionStatus.SUCCESS,
                Collections.singletonList(Collections.<String, Object>singletonMap("id", Integer.valueOf(1))),
                null,
                new QueryExecutionMetadataVO(
                    "HETU",
                    "SELECT 1",
                    48L,
                    512L,
                    false,
                    false,
                    "CLIENT",
                    Arrays.asList("CLIENT"),
                    1,
                    null,
                    Collections.<String>emptyList(),
                    null,
                    null,
                    "BACKFILLED",
                    "policyId=cache-policy-001;status=BACKFILLED;schemaVersion=schema-v1;entryState=REFRESHED"
                ),
                false,
                null,
                Collections.emptyList(),
                null,
                "fp-001",
                "LONG_TERM_BASELINE",
                "HETU_REAL_INTEGRATION"
            ));

        QueryExecutionBenchmarkWorkloadService service =
            new QueryExecutionBenchmarkWorkloadService(queryExecutionApplicationService, governanceCapabilityClient);

        com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadResponse response =
            service.capture(baseRequest());

        assertEquals("QUERY_EXECUTION_SYNC", response.getWorkloadSource());
        assertEquals(false, response.isBackfillApplied());
        assertEquals(1, response.getEngineSnapshots().size());
        assertEquals("QUERY_EXECUTION_SYNC", response.getEngineSnapshots().get(0).getWorkloadSource());
        assertEquals("CLIENT", response.getEngineSnapshots().get(0).getExecutionMode());
        assertEquals("BACKFILLED", response.getEngineSnapshots().get(0).getCacheGovernanceStatus());
        assertTrue(response.getEngineSnapshots().get(0).getEvidence().contains("cacheGovernanceStatus=BACKFILLED"));
        verify(governanceCapabilityClient).writeAudit(any(QueryExecutionAuditRecord.class));
    }

    @Test
    void shouldCompensateReplayWhenOneEngineFallsBackButAnotherIsLive() {
        QueryExecutionApplicationService queryExecutionApplicationService = mock(QueryExecutionApplicationService.class);
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        when(queryExecutionApplicationService.executeSynchronously(any())).thenAnswer(invocation -> {
            QueryExecuteRequest request = invocation.getArgument(0);
            if (request.getDatasourceType() == DataSourceTypeEnum.HETU) {
                return new QueryExecuteResponse(
                    QueryExecutionStatus.SUCCESS,
                    Collections.singletonList(Collections.<String, Object>singletonMap("id", Integer.valueOf(1))),
                    null,
                    new QueryExecutionMetadataVO(
                        "HETU",
                        "SELECT 1",
                        48L,
                        512L,
                        false,
                    false,
                    "CLIENT",
                    Arrays.asList("CLIENT"),
                    1,
                    null,
                    Collections.<String>emptyList(),
                    null,
                    null,
                    "BACKFILLED",
                    "policyId=cache-policy-001;status=BACKFILLED;schemaVersion=schema-v1;entryState=REFRESHED"
                ),
                    false,
                    null,
                    Collections.emptyList(),
                    null,
                    "fp-001",
                    "LONG_TERM_BASELINE",
                    "HETU_REAL_INTEGRATION"
                );
            }
            throw new IllegalStateException("route unavailable");
        });

        QueryExecutionBenchmarkWorkloadService service =
            new QueryExecutionBenchmarkWorkloadService(queryExecutionApplicationService, governanceCapabilityClient);

        QueryExecutionBenchmarkWorkloadRequest request = baseRequest();
        request.setTargetEngines(Arrays.asList(DataSourceTypeEnum.HETU, DataSourceTypeEnum.HIVE));
        com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadResponse response =
            service.capture(request);

        assertEquals("LIVE_WITH_COMPENSATED_REPLAY", response.getWorkloadSource());
        assertTrue(response.isBackfillApplied());
        assertTrue(response.isCompensationApplied());
        assertEquals("PRIMARY_LIVE_ENGINE_REPLAY", response.getCompensationStrategy());
        assertEquals("COMPENSATED_REPLAY", response.getEngineSnapshots().get(1).getWorkloadSource());
        assertEquals(Boolean.TRUE, response.getEngineSnapshots().get(1).getCompensationApplied());
        assertEquals(DataSourceTypeEnum.HETU, response.getEngineSnapshots().get(1).getCompensationSourceEngine());
        assertEquals("PRIMARY_LIVE_ENGINE_REPLAY", response.getEngineSnapshots().get(1).getCompensationStrategy());
        assertTrue(response.getEngineSnapshots().get(1).getEvidence().contains("sourceEngine=HETU"));
    }

    @Test
    void shouldBackfillWhenQueryExecutionFails() {
        QueryExecutionApplicationService queryExecutionApplicationService = mock(QueryExecutionApplicationService.class);
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        when(queryExecutionApplicationService.executeSynchronously(any()))
            .thenThrow(new IllegalStateException("route unavailable"));

        QueryExecutionBenchmarkWorkloadService service =
            new QueryExecutionBenchmarkWorkloadService(queryExecutionApplicationService, governanceCapabilityClient);

        com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadResponse response =
            service.capture(baseRequest());

        assertEquals("LIVE_WITH_BACKFILL", response.getWorkloadSource());
        assertTrue(response.isBackfillApplied());
        assertEquals("SYNTHETIC_BACKFILL", response.getEngineSnapshots().get(0).getWorkloadSource());
        ArgumentCaptor<QueryExecutionAuditRecord> captor = ArgumentCaptor.forClass(QueryExecutionAuditRecord.class);
        verify(governanceCapabilityClient).writeAudit(captor.capture());
        assertEquals("QUERY_BENCHMARK_WORKLOAD_CAPTURE", captor.getValue().getOperationCode());
        assertEquals("PARTIAL", captor.getValue().getResultStatus());
    }

    private QueryExecutionBenchmarkWorkloadRequest baseRequest() {
        QueryExecutionBenchmarkWorkloadRequest request = new QueryExecutionBenchmarkWorkloadRequest();
        request.setTenantId("tenant-a");
        request.setBenchmarkTaskId("benchmark-task-001");
        request.setBenchmarkTaskType("BASELINE");
        request.setSqlText("SELECT * FROM orders");
        request.setSqlFingerprint("fp-001");
        request.setTargetEngines(Collections.singletonList(DataSourceTypeEnum.HETU));
        request.setConcurrency(Integer.valueOf(4));
        request.setDatasetSizeLabel("TEN_GB");
        return request;
    }
}
