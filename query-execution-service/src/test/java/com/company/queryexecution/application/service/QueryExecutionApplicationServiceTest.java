package com.company.queryexecution.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.queryexecution.application.controller.dto.QueryContextDTO;
import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.domain.query.AccelerationPreference;
import com.company.queryexecution.domain.query.FaultToleranceStrategy;
import com.company.queryexecution.domain.query.QueryExecutionStatus;
import com.company.queryexecution.infrastructure.adapter.DeterministicQueryExecutionAdapter;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import org.junit.jupiter.api.Test;

class QueryExecutionApplicationServiceTest {

    @Test
    void shouldExecuteSynchronouslyForReadonlyHetuQuery() {
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new DeterministicQueryExecutionAdapter());
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders");
        request.setAccelerationPreference(AccelerationPreference.PREFER_ACCELERATED);

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.SUCCESS, response.getStatus());
        assertEquals("HETU", response.getMetadata().getTargetEngine());
        assertEquals("MINIMAL_SYNC_BASELINE", response.getImplementationStage());
        assertFalse(response.isDegraded());
        assertNull(response.getError());
        assertEquals(1, response.getRows().size());
        assertTrue(response.getMetadata().isAccelerationApplied());
    }

    @Test
    void shouldReturnTimeoutWhenFailFastThresholdIsExceeded() {
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new DeterministicQueryExecutionAdapter());
        QueryExecuteRequest request = baseRequest("SELECT * FROM orders");
        request.setQueryContext(timeoutContext(30L));
        request.setFaultToleranceStrategy(FaultToleranceStrategy.FAIL_FAST);

        QueryExecuteResponse response = service.executeSynchronously(request);

        assertEquals(QueryExecutionStatus.TIMEOUT, response.getStatus());
        assertNotNull(response.getError());
        assertEquals(ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ENGINE_TIMEOUT, response.getError().getCode());
        assertEquals(1, response.getRetryPath().size());
        assertEquals("HETU", response.getRetryPath().get(0).getEngine());
    }

    @Test
    void shouldRejectNonReadonlySqlBeforeExecution() {
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new DeterministicQueryExecutionAdapter());
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
        QueryExecutionApplicationService service =
            new QueryExecutionApplicationService(new DeterministicQueryExecutionAdapter());
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
        assertNull(response.getError());
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
}
