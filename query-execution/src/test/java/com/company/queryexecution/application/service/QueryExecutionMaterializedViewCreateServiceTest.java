package com.company.queryexecution.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.infrastructure.governance.GovernanceCapabilityClient;
import com.company.queryexecution.infrastructure.governance.QueryExecutionAuditRecord;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveResponse;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveResponse;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionMaterializedViewCreateRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionMaterializedViewCreateResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QueryExecutionMaterializedViewCreateServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldExecuteDdlAndRefreshAndWriteAudit() {
        setTenant("tenant-a");
        RecordingGovernanceClient governanceClient = new RecordingGovernanceClient();
        RecordingJdbcExecutor jdbcExecutor = new RecordingJdbcExecutor();
        QueryExecutionMaterializedViewCreateService service =
            new QueryExecutionMaterializedViewCreateService(
                governanceClient,
                new QueryExecutionHetuProperties(),
                jdbcExecutor
            );

        QueryExecutionMaterializedViewCreateResponse response = service.create(request("tenant-a", "HETU"));

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("SUCCESS", response.getDdlStatus());
        assertEquals("SUCCESS", response.getRefreshStatus());
        assertEquals("CREATE MATERIALIZED VIEW mv_orders_customer AS SELECT 1", jdbcExecutor.sqlStatements.get(0));
        assertEquals("REFRESH MATERIALIZED VIEW mv_orders_customer", jdbcExecutor.sqlStatements.get(1));
        assertEquals("MATERIALIZED_VIEW_CREATE", governanceClient.operationCode);
        assertEquals("MATERIALIZED_VIEW_CREATE", governanceClient.auditRecords.get(0).getOperationCode());
        assertTrue(response.getRuntimeDetailsJson().contains("MATERIALIZED_VIEW_CREATE_JDBC_BASELINE"));
    }

    @Test
    void shouldReturnPartialSuccessWhenRefreshFailsWithoutRollback() {
        setTenant("tenant-a");
        RecordingGovernanceClient governanceClient = new RecordingGovernanceClient();
        RecordingJdbcExecutor jdbcExecutor = new RecordingJdbcExecutor();
        jdbcExecutor.failOnSecondStatement = true;
        QueryExecutionMaterializedViewCreateService service =
            new QueryExecutionMaterializedViewCreateService(
                governanceClient,
                new QueryExecutionHetuProperties(),
                jdbcExecutor
            );

        QueryExecutionMaterializedViewCreateResponse response = service.create(request("tenant-a", "HETU"));

        assertEquals("PARTIAL_SUCCESS", response.getStatus());
        assertEquals("SUCCESS", response.getDdlStatus());
        assertEquals("FAILED", response.getRefreshStatus());
        assertEquals(2, jdbcExecutor.sqlStatements.size());
        assertEquals("PARTIAL_SUCCESS", governanceClient.auditRecords.get(0).getResultStatus());
        assertTrue(response.getRuntimeDetailsJson().contains("\"automaticRollback\":false"));
    }

    @Test
    void shouldFailWhenDdlFailsAndStillWriteAudit() {
        setTenant("tenant-a");
        RecordingGovernanceClient governanceClient = new RecordingGovernanceClient();
        RecordingJdbcExecutor jdbcExecutor = new RecordingJdbcExecutor();
        jdbcExecutor.failOnFirstStatement = true;
        QueryExecutionMaterializedViewCreateService service =
            new QueryExecutionMaterializedViewCreateService(
                governanceClient,
                new QueryExecutionHetuProperties(),
                jdbcExecutor
            );

        assertThrows(BizException.class, () -> service.create(request("tenant-a", "HETU")));

        assertEquals(1, jdbcExecutor.sqlStatements.size());
        assertEquals("FAILED", governanceClient.auditRecords.get(0).getResultStatus());
    }

    @Test
    void shouldRejectMismatchedProtectedTenant() {
        setTenant("tenant-a");
        QueryExecutionMaterializedViewCreateService service =
            new QueryExecutionMaterializedViewCreateService(
                new RecordingGovernanceClient(),
                new QueryExecutionHetuProperties(),
                new RecordingJdbcExecutor()
            );

        assertThrows(AccessDeniedException.class, () -> service.create(request("tenant-b", "HETU")));
    }

    private QueryExecutionMaterializedViewCreateRequest request(String tenantId, String targetEngine) {
        QueryExecutionMaterializedViewCreateRequest request = new QueryExecutionMaterializedViewCreateRequest();
        request.setTenantId(tenantId);
        request.setRecommendationId("rec-001");
        request.setRewriteRecordId("rewrite-001");
        request.setMvName("mv_orders_customer");
        request.setTargetEngine(targetEngine);
        request.setTargetDatasource("hetu_main");
        request.setDdlSql("CREATE MATERIALIZED VIEW mv_orders_customer AS SELECT 1");
        request.setRefreshSql("REFRESH MATERIALIZED VIEW mv_orders_customer");
        request.setReason("manual click");
        return request;
    }

    private void setTenant(String tenantId) {
        RequestContext.set(
            tenantId,
            "service-user",
            "request-001",
            "trace-001",
            "header",
            1L,
            2L
        );
    }

    private static final class RecordingJdbcExecutor
        implements QueryExecutionMaterializedViewCreateService.MaterializedViewJdbcExecutor {

        private final List<String> sqlStatements = new ArrayList<String>();
        private boolean failOnFirstStatement;
        private boolean failOnSecondStatement;

        @Override
        public QueryExecutionMaterializedViewCreateService.ExecutionEvidence execute(
            QueryExecutionMaterializedViewCreateService.ResolvedJdbcDatasource datasource,
            String sql
        ) {
            sqlStatements.add(sql);
            if ((failOnFirstStatement && sqlStatements.size() == 1)
                || (failOnSecondStatement && sqlStatements.size() == 2)) {
                throw new IllegalStateException("simulated jdbc failure");
            }
            return new QueryExecutionMaterializedViewCreateService.ExecutionEvidence("SUCCESS", 3L, null, null);
        }
    }

    private static final class RecordingGovernanceClient implements GovernanceCapabilityClient {

        private String operationCode;
        private final List<QueryExecutionAuditRecord> auditRecords = new ArrayList<QueryExecutionAuditRecord>();

        @Override
        public void assertDatasourceAccess(String tenantId,
                                           DataSourceTypeEnum datasourceType,
                                           String resourceType,
                                           String resourceId,
                                           String operationCode) {
            this.operationCode = operationCode;
        }

        @Override
        public void writeAudit(QueryExecutionAuditRecord auditRecord) {
            auditRecords.add(auditRecord);
        }

        @Override
        public GovernanceJdbcRouteResolveResponse resolveJdbcRoute(GovernanceJdbcRouteResolveRequest request) {
            return new GovernanceJdbcRouteResolveResponse();
        }

        @Override
        public GovernanceJdbcDatasourceResolveResponse resolveJdbcDatasource(
            GovernanceJdbcDatasourceResolveRequest request
        ) {
            GovernanceJdbcDatasourceResolveResponse response = new GovernanceJdbcDatasourceResolveResponse();
            response.setTenantId(request.getTenantId());
            response.setDatasourceCode(request.getDatasourceCode());
            response.setEngineType(request.getEngineType());
            response.setResolved(true);
            response.setEnabled(true);
            response.setJdbcUrl("jdbc:test://localhost/default");
            response.setUsername("user");
            response.setPassword("secret");
            response.setTimeoutMs(Integer.valueOf(3000));
            return response;
        }

        @Override
        public GovernanceQueryExecutionHistoryWriteResponse writeQueryExecutionHistory(
            GovernanceQueryExecutionHistoryWriteRequest request
        ) {
            return new GovernanceQueryExecutionHistoryWriteResponse();
        }
    }
}
