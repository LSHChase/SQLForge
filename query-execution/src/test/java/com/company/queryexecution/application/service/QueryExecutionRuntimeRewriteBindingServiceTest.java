package com.company.queryexecution.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.queryexecution.domain.rewrite.RuntimeRewriteBinding;
import com.company.queryexecution.domain.rewrite.RuntimeRewriteBindingStatus;
import com.company.queryexecution.domain.rewrite.repository.RuntimeRewriteBindingRepository;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.logicalobject.SqlSurfaceObjectRefExtractor;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingActivationRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResolveRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingStateChangeRequest;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class QueryExecutionRuntimeRewriteBindingServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldActivatePauseAndTrackRuleVersion() {
        setRequestContext();
        InMemoryRuntimeRewriteBindingRepository repository = new InMemoryRuntimeRewriteBindingRepository();
        RecordingJdbcAgentRewriteRuleSyncPort syncPort = new RecordingJdbcAgentRewriteRuleSyncPort();
        QueryExecutionRuntimeRewriteBindingService service =
            new QueryExecutionRuntimeRewriteBindingService(repository, syncPort);

        RuntimeRewriteBindingResponse activated = service.activate(activationRequest("rewrite-001"));

        assertEquals("ACTIVE", activated.getStatus());
        assertTrue(activated.isActive());
        assertEquals(Long.valueOf(1), activated.getRuleVersion());
        assertEquals("runtime-rewrite-v1", activated.getRuntimeRuleVersion());
        assertEquals("SELECT id FROM orders", activated.getRecommendedSqlText());
        assertTrue(activated.getRuntimeDetailsJson().contains("\"syncStatus\":\"SYNCED\""));
        assertEquals(1, syncPort.activatedBindings.size());

        RuntimeRewriteBindingResolveRequest resolveRequest = new RuntimeRewriteBindingResolveRequest();
        resolveRequest.setTenantId("tenant-a");
        resolveRequest.setSqlFingerprint("fp-001");
        resolveRequest.setSqlText("SELECT * FROM orders WHERE tenant_id = 2 AND status = 'CANCELLED'");
        resolveRequest.setDatasourceCode("hetu_main");
        assertEquals("ACTIVE", service.resolveActive(resolveRequest).getStatus());

        RuntimeRewriteBindingStateChangeRequest pauseRequest = new RuntimeRewriteBindingStateChangeRequest();
        pauseRequest.setTenantId("tenant-a");
        pauseRequest.setRuntimeBindingId(activated.getRuntimeBindingId());
        pauseRequest.setReason("scheduled validation divergence");
        RuntimeRewriteBindingResponse paused = service.pause(pauseRequest);
        assertEquals("PAUSED", paused.getStatus());
        assertFalse(paused.isActive());
        assertTrue(paused.getRuntimeDetailsJson().contains("\"syncAction\":\"DISABLE\""));
        assertEquals("MISSING", service.resolveActive(resolveRequest).getStatus());

        RuntimeRewriteBindingResponse second = service.activate(activationRequest("rewrite-002"));
        assertEquals(Long.valueOf(2), second.getRuleVersion());
        assertEquals("runtime-rewrite-v2", second.getRuntimeRuleVersion());

        BizException conflict = assertThrows(BizException.class, () -> service.activate(activationRequest("rewrite-003")));
        assertEquals(HttpStatus.CONFLICT, conflict.getHttpStatus());

        RuntimeRewriteBindingStateChangeRequest secondPauseRequest = new RuntimeRewriteBindingStateChangeRequest();
        secondPauseRequest.setTenantId("tenant-a");
        secondPauseRequest.setRuntimeBindingId(second.getRuntimeBindingId());
        secondPauseRequest.setReason("operator pause");
        RuntimeRewriteBindingResponse secondPaused = service.pause(secondPauseRequest);
        assertEquals("PAUSED", secondPaused.getStatus());
        assertFalse(secondPaused.isActive());
        assertEquals(2, syncPort.disabledBindings.size());
    }

    @Test
    void shouldRejectActivateForTenantMismatch() {
        setRequestContext();
        QueryExecutionRuntimeRewriteBindingService service =
            new QueryExecutionRuntimeRewriteBindingService(new InMemoryRuntimeRewriteBindingRepository());
        RuntimeRewriteBindingActivationRequest request = activationRequest("rewrite-001");
        request.setTenantId("tenant-b");

        assertThrows(com.company.sqlforge.common.exception.AccessDeniedException.class, () -> service.activate(request));
    }

    @Test
    void shouldKeepRuntimeBindingActiveWhenRedisSyncFails() {
        setRequestContext();
        QueryExecutionRuntimeRewriteBindingService service =
            new QueryExecutionRuntimeRewriteBindingService(
                new InMemoryRuntimeRewriteBindingRepository(),
                new FailingJdbcAgentRewriteRuleSyncPort()
            );

        RuntimeRewriteBindingResponse activated = service.activate(activationRequest("rewrite-001"));

        assertEquals("ACTIVE", activated.getStatus());
        assertTrue(activated.isActive());
        assertTrue(activated.getRuntimeDetailsJson().contains("\"syncStatus\":\"FAILED\""));
        assertTrue(activated.getRuntimeDetailsJson().contains("\"alertRequired\":true"));
        assertTrue(activated.getRuntimeDetailsJson().contains("\"retryable\":true"));
    }

    @Test
    void shouldRetryRedisSyncForIdempotentActivateOfExistingActiveBinding() {
        setRequestContext();
        RecordingJdbcAgentRewriteRuleSyncPort syncPort = new RecordingJdbcAgentRewriteRuleSyncPort();
        QueryExecutionRuntimeRewriteBindingService service =
            new QueryExecutionRuntimeRewriteBindingService(new InMemoryRuntimeRewriteBindingRepository(), syncPort);

        RuntimeRewriteBindingResponse first = service.activate(activationRequest("rewrite-001"));
        RuntimeRewriteBindingResponse retried = service.activate(activationRequest("rewrite-001"));

        assertEquals(first.getRuntimeBindingId(), retried.getRuntimeBindingId());
        assertEquals(2, syncPort.activatedBindings.size());
    }

    @Test
    void shouldReplayCurrentPredicatesForExactRuntimeRewriteHit() {
        setRequestContext();
        QueryExecutionRuntimeRewriteBindingService service =
            new QueryExecutionRuntimeRewriteBindingService(new InMemoryRuntimeRewriteBindingRepository());
        service.activate(activationRequest("rewrite-001"));

        RuntimeRewriteBindingResolveRequest resolveRequest = new RuntimeRewriteBindingResolveRequest();
        resolveRequest.setTenantId("tenant-a");
        resolveRequest.setSqlFingerprint("fp-001");
        resolveRequest.setSqlText(
            "SELECT * FROM orders WHERE tenant_id = 8 AND status = 'CANCELLED' AND dt = '2026-05-22'"
        );
        RuntimeRewriteBindingResponse response = service.resolveActive(resolveRequest);

        assertEquals("ACTIVE", response.getStatus());
        assertEquals(
            "SELECT id FROM orders WHERE tenant_id = 8 AND status = 'CANCELLED' AND dt = '2026-05-22'",
            response.getRecommendedSqlText()
        );
        assertEquals("TEMPLATE_CONDITION_REPLAY", response.getRewriteMatchMode());
        assertTrue(response.getRewriteProgramJson().contains("template-replay-v1"));
    }

    @Test
    void shouldMatchRuntimeRewriteTemplateWhenFingerprintChangesByConditionDelta() {
        setRequestContext();
        QueryExecutionRuntimeRewriteBindingService service =
            new QueryExecutionRuntimeRewriteBindingService(new InMemoryRuntimeRewriteBindingRepository());
        service.activate(activationRequest("rewrite-001"));

        RuntimeRewriteBindingResolveRequest resolveRequest = new RuntimeRewriteBindingResolveRequest();
        resolveRequest.setTenantId("tenant-a");
        String currentSql = "SELECT * FROM orders WHERE tenant_id = 8";
        String currentFingerprint = SqlFingerprintUtils.fingerprint(currentSql);
        resolveRequest.setSqlFingerprint("fp-current-different");
        resolveRequest.setSqlText(currentSql);
        RuntimeRewriteBindingResponse response = service.resolveActive(resolveRequest);

        assertEquals("ACTIVE", response.getStatus());
        assertEquals(currentFingerprint, response.getSqlFingerprint());
        assertEquals("SELECT id FROM orders WHERE tenant_id = 8", response.getRecommendedSqlText());
    }

    @Test
    void shouldResolveFormattedSqlAgainstBindingCreatedFromCompactSql() {
        setRequestContext();
        QueryExecutionRuntimeRewriteBindingService service =
            new QueryExecutionRuntimeRewriteBindingService(new InMemoryRuntimeRewriteBindingRepository());
        RuntimeRewriteBindingActivationRequest request = activationRequest("rewrite-format-001");
        request.setSqlFingerprint("stale-client-fingerprint");
        request.setOriginalSqlText("SELECT SUM(a-b) AS delta FROM orders WHERE tenant_id=1 AND amount>=100");
        request.setRecommendedSqlText(
            "WITH metric AS (SELECT SUM(a-b) AS delta FROM orders WHERE tenant_id=1 AND amount>=100) "
                + "SELECT delta FROM metric"
        );
        RuntimeRewriteBindingResponse activated = service.activate(request);
        String expectedFingerprint = SqlFingerprintUtils.fingerprint(request.getOriginalSqlText());

        RuntimeRewriteBindingResolveRequest resolveRequest = new RuntimeRewriteBindingResolveRequest();
        resolveRequest.setTenantId("tenant-a");
        resolveRequest.setSqlFingerprint("formatted-editor-fingerprint");
        resolveRequest.setSqlText("SELECT\n"
            + "  SUM( a - b ) AS delta\n"
            + "FROM orders\n"
            + "WHERE tenant_id = 2\n"
            + "  AND amount >= 200");
        RuntimeRewriteBindingResponse response = service.resolveActive(resolveRequest);

        assertEquals(expectedFingerprint, activated.getSqlFingerprint());
        assertEquals("ACTIVE", response.getStatus());
        assertTrue(response.isActive());
        assertEquals(SqlFingerprintUtils.fingerprint(resolveRequest.getSqlText()), response.getSqlFingerprint());
        assertTrue(response.getRecommendedSqlText().contains("tenant_id=2"));
        assertTrue(response.getRecommendedSqlText().contains("amount>=200"));
    }

    @Test
    void shouldMatchRuntimeRewriteOnlyOnSurfaceObjectNames() {
        setRequestContext();
        QueryExecutionRuntimeRewriteBindingService service =
            new QueryExecutionRuntimeRewriteBindingService(new InMemoryRuntimeRewriteBindingRepository());
        RuntimeRewriteBindingActivationRequest request = activationRequest("rewrite-view-001");
        request.setSqlFingerprint("fp-view-001");
        request.setOriginalSqlText("SELECT * FROM vw_orders WHERE tenant_id = 1");
        request.setRecommendedSqlText("SELECT id FROM vw_orders");
        request.setRuntimeMatchObjectRefs(SqlSurfaceObjectRefExtractor.extractSurfaceRefs(request.getOriginalSqlText()));
        request.setRuntimeMatchObjectNames(SqlSurfaceObjectRefExtractor.extractSurfaceObjectNames(request.getOriginalSqlText()));
        request.setAnalysisPhysicalObjectRefs(SqlSurfaceObjectRefExtractor.extractSurfaceRefs("SELECT * FROM orders_base"));
        RuntimeRewriteBindingResponse activated = service.activate(request);

        assertEquals(Arrays.asList("vw_orders"), activated.getRuntimeMatchObjectNames());
        assertEquals("orders_base", activated.getAnalysisPhysicalObjectRefs().get(0).getObjectName());

        RuntimeRewriteBindingResolveRequest bottomTableRequest = new RuntimeRewriteBindingResolveRequest();
        bottomTableRequest.setTenantId("tenant-a");
        bottomTableRequest.setSqlFingerprint("fp-view-001");
        bottomTableRequest.setSqlText("SELECT * FROM vw_orders WHERE tenant_id = 2");
        bottomTableRequest.setRuntimeMatchObjectNames(Arrays.asList("orders_base"));
        assertEquals("MISSING", service.resolveActive(bottomTableRequest).getStatus());

        RuntimeRewriteBindingResolveRequest extractedBottomTableRequest = new RuntimeRewriteBindingResolveRequest();
        extractedBottomTableRequest.setTenantId("tenant-a");
        extractedBottomTableRequest.setSqlFingerprint("fp-view-001");
        extractedBottomTableRequest.setSqlText("SELECT * FROM orders_base WHERE tenant_id = 2");
        assertEquals("MISSING", service.resolveActive(extractedBottomTableRequest).getStatus());

        RuntimeRewriteBindingResolveRequest viewRequest = new RuntimeRewriteBindingResolveRequest();
        viewRequest.setTenantId("tenant-a");
        viewRequest.setSqlFingerprint("fp-view-001");
        viewRequest.setSqlText("SELECT * FROM vw_orders WHERE tenant_id = 2");
        viewRequest.setRuntimeMatchObjectNames(Arrays.asList("vw_orders"));
        assertEquals("ACTIVE", service.resolveActive(viewRequest).getStatus());
    }

    @Test
    void shouldNormalizeBiViewCatalogSurfaceNamesForActivationAndResolve() {
        setRequestContext();
        QueryExecutionRuntimeRewriteBindingService service =
            new QueryExecutionRuntimeRewriteBindingService(new InMemoryRuntimeRewriteBindingRepository());
        RuntimeRewriteBindingActivationRequest request = activationRequest("rewrite-bi-001");
        request.setOriginalSqlText("SELECT * FROM BI_SALES_V.orders WHERE tenant_id = 1");
        request.setRecommendedSqlText("SELECT id FROM BI_SALES_V.orders WHERE tenant_id = 1");
        request.setRuntimeMatchObjectNames(Arrays.asList("BI_SALES_V.orders"));
        RuntimeRewriteBindingResponse activated = service.activate(request);
        String currentSql = "SELECT * FROM BI_SALES_HETU.orders WHERE tenant_id = 2";

        RuntimeRewriteBindingResolveRequest resolveRequest = new RuntimeRewriteBindingResolveRequest();
        resolveRequest.setTenantId("tenant-a");
        resolveRequest.setSqlFingerprint(SqlFingerprintUtils.fingerprint(currentSql));
        resolveRequest.setSqlText(currentSql);
        resolveRequest.setDatasourceCode("hetu_main");
        RuntimeRewriteBindingResponse resolved = service.resolveActive(resolveRequest);

        assertEquals(Arrays.asList("bi_sales_hetu.orders"), activated.getRuntimeMatchObjectNames());
        assertEquals("ACTIVE", resolved.getStatus());
        assertTrue(resolved.isActive());
    }

    private RuntimeRewriteBindingActivationRequest activationRequest(String rewriteRecordId) {
        RuntimeRewriteBindingActivationRequest request = new RuntimeRewriteBindingActivationRequest();
        request.setTenantId("tenant-a");
        request.setRewriteRecordId(rewriteRecordId);
        request.setRecommendationId("recommendation-001");
        request.setSourceType("QUERY");
        request.setSourceKind("QUERY_HISTORY");
        request.setSourceId("history-001");
        request.setSqlFingerprint("fp-001");
        request.setOriginalSqlDigest("digest-original-001");
        request.setOriginalSqlText("SELECT * FROM orders WHERE tenant_id = 1 AND status = 'PAID'");
        request.setRecommendedSqlText("SELECT id FROM orders");
        request.setDatasourceCode("hetu_main");
        request.setActivatedBy("publisher-001");
        return request;
    }

    private void setRequestContext() {
        RequestContext.set(
            "tenant-a",
            "service-user",
            "request-001",
            "trace-001",
            "header",
            1L,
            2L
        );
    }

    private static final class InMemoryRuntimeRewriteBindingRepository implements RuntimeRewriteBindingRepository {
        private final Map<String, RuntimeRewriteBinding> bindings =
            new LinkedHashMap<String, RuntimeRewriteBinding>();

        @Override
        public RuntimeRewriteBinding save(RuntimeRewriteBinding binding) {
            bindings.put(binding.getRuntimeBindingId(), binding);
            return binding;
        }

        @Override
        public RuntimeRewriteBinding findByRuntimeBindingId(String runtimeBindingId) {
            return bindings.get(runtimeBindingId);
        }

        @Override
        public RuntimeRewriteBinding findActiveByTenantIdAndSqlFingerprint(String tenantId, String sqlFingerprint) {
            for (RuntimeRewriteBinding binding : bindings.values()) {
                if (tenantId.equals(binding.getTenantId())
                    && sqlFingerprint.equals(binding.getSqlFingerprint())
                    && binding.getStatus() == RuntimeRewriteBindingStatus.ACTIVE) {
                    return binding;
                }
            }
            return null;
        }

        @Override
        public RuntimeRewriteBinding findLatestByTenantIdAndSqlFingerprint(String tenantId, String sqlFingerprint) {
            RuntimeRewriteBinding latest = null;
            for (RuntimeRewriteBinding binding : bindings.values()) {
                if (!tenantId.equals(binding.getTenantId()) || !sqlFingerprint.equals(binding.getSqlFingerprint())) {
                    continue;
                }
                if (latest == null || binding.getRuleVersion() > latest.getRuleVersion()) {
                    latest = binding;
                }
            }
            return latest;
        }

        @Override
        public List<RuntimeRewriteBinding> findByTenantIdAndSqlFingerprint(String tenantId, String sqlFingerprint) {
            List<RuntimeRewriteBinding> result = new ArrayList<RuntimeRewriteBinding>();
            for (RuntimeRewriteBinding binding : bindings.values()) {
                if (tenantId.equals(binding.getTenantId()) && sqlFingerprint.equals(binding.getSqlFingerprint())) {
                    result.add(binding);
                }
            }
            return result;
        }

        @Override
        public List<RuntimeRewriteBinding> findActiveByTenantId(String tenantId) {
            List<RuntimeRewriteBinding> result = new ArrayList<RuntimeRewriteBinding>();
            for (RuntimeRewriteBinding binding : bindings.values()) {
                if (tenantId.equals(binding.getTenantId())
                    && binding.getStatus() == RuntimeRewriteBindingStatus.ACTIVE) {
                    result.add(binding);
                }
            }
            return result;
        }
    }

    private static final class RecordingJdbcAgentRewriteRuleSyncPort implements JdbcAgentRewriteRuleSyncPort {
        private final List<RuntimeRewriteBinding> activatedBindings = new ArrayList<RuntimeRewriteBinding>();
        private final List<RuntimeRewriteBinding> disabledBindings = new ArrayList<RuntimeRewriteBinding>();

        @Override
        public JdbcAgentRewriteRuleSyncResult activate(RuntimeRewriteBinding binding) {
            activatedBindings.add(binding);
            return JdbcAgentRewriteRuleSyncResult.builder()
                .syncStatus("SYNCED")
                .syncAction("ACTIVATE")
                .redisRewriteKey("sqlforge:jdbc-agent:tenant:" + binding.getTenantId()
                    + ":rewrite:" + binding.getSqlFingerprint())
                .redisMetadataKey("sqlforge:jdbc-agent:tenant:" + binding.getTenantId()
                    + ":meta:" + binding.getSqlFingerprint())
                .build();
        }

        @Override
        public JdbcAgentRewriteRuleSyncResult disable(RuntimeRewriteBinding binding) {
            disabledBindings.add(binding);
            return JdbcAgentRewriteRuleSyncResult.builder()
                .syncStatus("SYNCED")
                .syncAction("DISABLE")
                .redisRewriteKey("sqlforge:jdbc-agent:tenant:" + binding.getTenantId()
                    + ":rewrite:" + binding.getSqlFingerprint())
                .redisMetadataKey("sqlforge:jdbc-agent:tenant:" + binding.getTenantId()
                    + ":meta:" + binding.getSqlFingerprint())
                .build();
        }
    }

    private static final class FailingJdbcAgentRewriteRuleSyncPort implements JdbcAgentRewriteRuleSyncPort {
        @Override
        public JdbcAgentRewriteRuleSyncResult activate(RuntimeRewriteBinding binding) {
            return JdbcAgentRewriteRuleSyncResult.builder()
                .syncStatus("FAILED")
                .syncAction("ACTIVATE")
                .failureReason("redis 不可用")
                .retryable(true)
                .alertRequired(true)
                .build();
        }

        @Override
        public JdbcAgentRewriteRuleSyncResult disable(RuntimeRewriteBinding binding) {
            return JdbcAgentRewriteRuleSyncResult.builder()
                .syncStatus("FAILED")
                .syncAction("DISABLE")
                .failureReason("redis 不可用")
                .retryable(true)
                .alertRequired(true)
                .build();
        }
    }
}
