package com.company.queryexecution.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.queryexecution.infrastructure.adapter.QueryExecutionAdapter;
import com.company.queryexecution.infrastructure.governance.GovernanceCapabilityClient;
import com.company.queryexecution.infrastructure.governance.QueryExecutionAuditRecord;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteResponse;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QueryExecutionResultDigestServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldReturnDigestAndLimitedSampleWithoutFullRows() {
        setTenant("tenant-a");
        QueryExecutionApplicationService queryService =
            new QueryExecutionApplicationService(new SampleRowsAdapter(), new NoopGovernanceCapabilityClient());
        QueryExecutionResultDigestService digestService = new QueryExecutionResultDigestService(queryService);
        QueryExecutionResultDigestRequest request = new QueryExecutionResultDigestRequest();
        request.setTenantId("tenant-a");
        request.setValidationRunId("validation-001");
        request.setRewriteRecordId("rewrite-001");
        request.setSqlText("SELECT id, amount FROM orders");
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        request.setComparisonPolicy(Collections.<String, Object>singletonMap("sampleLimit", Integer.valueOf(2)));

        QueryExecutionResultDigestResponse response = digestService.executeDigest(request);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("READONLY_RESULT_DIGEST_BASELINE", response.getImplementationStage());
        assertEquals(Long.valueOf(3L), response.getResultDigest().get("rowCount"));
        assertEquals(Integer.valueOf(2), response.getResultDigest().get("sampleRowCount"));
        assertEquals(2, response.getLimitedSample().size());
        assertTrue(response.getExecutionEvidence().containsKey("elapsedMs"));
        assertFalse(response.getResultDigest().containsKey("rows"));
    }

    @Test
    void shouldRejectNonReadonlySqlThroughExistingGuard() {
        setTenant("tenant-a");
        QueryExecutionApplicationService queryService =
            new QueryExecutionApplicationService(new SampleRowsAdapter(), new NoopGovernanceCapabilityClient());
        QueryExecutionResultDigestService digestService = new QueryExecutionResultDigestService(queryService);
        QueryExecutionResultDigestRequest request = new QueryExecutionResultDigestRequest();
        request.setTenantId("tenant-a");
        request.setSqlText("UPDATE orders SET amount = 1");
        request.setDatasourceType(DataSourceTypeEnum.HETU);

        QueryExecutionResultDigestResponse response = digestService.executeDigest(request);

        assertEquals("FAILED", response.getStatus());
        assertEquals("21000", response.getErrorCode());
        assertTrue(response.getLimitedSample().isEmpty());
    }

    private void setTenant(String tenantId) {
        RequestContext.set(
            tenantId,
            "service-user",
            Arrays.asList("SERVICE"),
            "request-001",
            "trace-001",
            "header",
            1L,
            2L
        );
    }

    private static final class SampleRowsAdapter implements QueryExecutionAdapter {

        @Override
        public QueryExecutionStep execute(DataSourceTypeEnum targetEngine,
                                          String actualSql,
                                          QueryExecuteRequest request,
                                          boolean degradedPath) {
            return new QueryExecutionStep(
                targetEngine,
                rows(),
                12L,
                3L,
                false,
                false
            );
        }

        private List<Map<String, Object>> rows() {
            return Arrays.<Map<String, Object>>asList(
                row(1, "10.00"),
                row(2, "20.00"),
                row(3, "30.00")
            );
        }

        private Map<String, Object> row(int id, String amount) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", Integer.valueOf(id));
            row.put("amount", amount);
            return row;
        }
    }

    private static final class NoopGovernanceCapabilityClient implements GovernanceCapabilityClient {

        @Override
        public void assertAuthorization(String tenantId,
                                        DataSourceTypeEnum datasourceType,
                                        String resourceType,
                                        String resourceId,
                                        String operationCode) {
        }

        @Override
        public void writeAudit(QueryExecutionAuditRecord auditRecord) {
        }

        @Override
        public GovernanceJdbcRouteResolveResponse resolveJdbcRoute(GovernanceJdbcRouteResolveRequest request) {
            return new GovernanceJdbcRouteResolveResponse();
        }

        @Override
        public GovernanceQueryExecutionHistoryWriteResponse writeQueryExecutionHistory(
            GovernanceQueryExecutionHistoryWriteRequest request
        ) {
            return new GovernanceQueryExecutionHistoryWriteResponse();
        }
    }
}
