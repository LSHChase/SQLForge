package com.company.queryexecution.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.domain.query.AccelerationPreference;
import com.company.queryexecution.domain.query.FaultToleranceStrategy;
import com.company.queryexecution.domain.query.QueryExecutionStatus;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import org.junit.jupiter.api.Test;

class QueryExecutionContractApplicationServiceTest {

    @Test
    void shouldDescribeContractUsingQueryDtoAndErrorCodeBaseline() {
        QueryExecutionContractApplicationService service = new QueryExecutionContractApplicationService();
        QueryExecuteRequest request = new QueryExecuteRequest();
        request.setSqlText("SELECT * FROM orders WHERE ds = '2026-04-20'");
        request.setTenantId("tenant-a");
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        request.setAccelerationPreference(AccelerationPreference.PREFER_ACCELERATED);
        request.setFaultToleranceStrategy(FaultToleranceStrategy.RETRY_THEN_FALLBACK);

        QueryExecuteResponse response = service.describeExecutionContract(request);

        assertEquals(QueryExecutionStatus.FAILED, response.getStatus());
        assertEquals("HETU", response.getMetadata().getTargetEngine());
        assertEquals(
            ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_PIPELINE_NOT_READY,
            response.getError().getCode()
        );
        assertEquals("LONG_TERM_BASELINE", response.getContractStage());
        assertEquals("TRANSITIONAL_SKELETON", response.getImplementationStage());
        assertFalse(response.isDegraded());
        assertNotNull(response.getSqlFingerprint());
        assertFalse(response.getSqlFingerprint().isEmpty());
    }
}
