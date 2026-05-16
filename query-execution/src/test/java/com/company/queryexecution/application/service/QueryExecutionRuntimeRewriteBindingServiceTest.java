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
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingPublishRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResolveRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingStateChangeRequest;
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
    void shouldPublishPauseUnpublishAndTrackRuleVersion() {
        setRequestContext();
        InMemoryRuntimeRewriteBindingRepository repository = new InMemoryRuntimeRewriteBindingRepository();
        RecordingJdbcAgentRewriteRuleSyncPort syncPort = new RecordingJdbcAgentRewriteRuleSyncPort();
        QueryExecutionRuntimeRewriteBindingService service =
            new QueryExecutionRuntimeRewriteBindingService(repository, syncPort);

        RuntimeRewriteBindingResponse published = service.publish(publishRequest("rewrite-001"));

        assertEquals("ACTIVE", published.getStatus());
        assertTrue(published.isActive());
        assertEquals(Long.valueOf(1), published.getRuleVersion());
        assertEquals("runtime-rewrite-v1", published.getRuntimeRuleVersion());
        assertEquals("SELECT id FROM orders", published.getRecommendedSqlText());
        assertTrue(published.getRuntimeDetailsJson().contains("\"syncStatus\":\"SYNCED\""));
        assertEquals(1, syncPort.publishedBindings.size());

        RuntimeRewriteBindingResolveRequest resolveRequest = new RuntimeRewriteBindingResolveRequest();
        resolveRequest.setTenantId("tenant-a");
        resolveRequest.setSqlFingerprint("fp-001");
        resolveRequest.setDatasourceCode("hetu_main");
        assertEquals("ACTIVE", service.resolveActive(resolveRequest).getStatus());

        RuntimeRewriteBindingStateChangeRequest pauseRequest = new RuntimeRewriteBindingStateChangeRequest();
        pauseRequest.setTenantId("tenant-a");
        pauseRequest.setRuntimeBindingId(published.getRuntimeBindingId());
        pauseRequest.setReason("scheduled validation divergence");
        RuntimeRewriteBindingResponse paused = service.pause(pauseRequest);
        assertEquals("PAUSED", paused.getStatus());
        assertFalse(paused.isActive());
        assertTrue(paused.getRuntimeDetailsJson().contains("\"syncAction\":\"DISABLE\""));
        assertEquals("MISSING", service.resolveActive(resolveRequest).getStatus());

        RuntimeRewriteBindingResponse second = service.publish(publishRequest("rewrite-002"));
        assertEquals(Long.valueOf(2), second.getRuleVersion());
        assertEquals("runtime-rewrite-v2", second.getRuntimeRuleVersion());

        BizException conflict = assertThrows(BizException.class, () -> service.publish(publishRequest("rewrite-003")));
        assertEquals(HttpStatus.CONFLICT, conflict.getHttpStatus());

        RuntimeRewriteBindingStateChangeRequest unpublishRequest = new RuntimeRewriteBindingStateChangeRequest();
        unpublishRequest.setTenantId("tenant-a");
        unpublishRequest.setRuntimeBindingId(second.getRuntimeBindingId());
        unpublishRequest.setReason("operator rollback");
        RuntimeRewriteBindingResponse unpublished = service.unpublish(unpublishRequest);
        assertEquals("UNPUBLISHED", unpublished.getStatus());
        assertFalse(unpublished.isActive());
        assertEquals(2, syncPort.disabledBindings.size());
    }

    @Test
    void shouldRejectPublishForTenantMismatch() {
        setRequestContext();
        QueryExecutionRuntimeRewriteBindingService service =
            new QueryExecutionRuntimeRewriteBindingService(new InMemoryRuntimeRewriteBindingRepository());
        RuntimeRewriteBindingPublishRequest request = publishRequest("rewrite-001");
        request.setTenantId("tenant-b");

        assertThrows(com.company.sqlforge.common.exception.AccessDeniedException.class, () -> service.publish(request));
    }

    @Test
    void shouldKeepRuntimeBindingActiveWhenRedisSyncFails() {
        setRequestContext();
        QueryExecutionRuntimeRewriteBindingService service =
            new QueryExecutionRuntimeRewriteBindingService(
                new InMemoryRuntimeRewriteBindingRepository(),
                new FailingJdbcAgentRewriteRuleSyncPort()
            );

        RuntimeRewriteBindingResponse published = service.publish(publishRequest("rewrite-001"));

        assertEquals("ACTIVE", published.getStatus());
        assertTrue(published.isActive());
        assertTrue(published.getRuntimeDetailsJson().contains("\"syncStatus\":\"FAILED\""));
        assertTrue(published.getRuntimeDetailsJson().contains("\"alertRequired\":true"));
        assertTrue(published.getRuntimeDetailsJson().contains("\"retryable\":true"));
    }

    @Test
    void shouldRetryRedisSyncForIdempotentPublishOfExistingActiveBinding() {
        setRequestContext();
        RecordingJdbcAgentRewriteRuleSyncPort syncPort = new RecordingJdbcAgentRewriteRuleSyncPort();
        QueryExecutionRuntimeRewriteBindingService service =
            new QueryExecutionRuntimeRewriteBindingService(new InMemoryRuntimeRewriteBindingRepository(), syncPort);

        RuntimeRewriteBindingResponse first = service.publish(publishRequest("rewrite-001"));
        RuntimeRewriteBindingResponse retried = service.publish(publishRequest("rewrite-001"));

        assertEquals(first.getRuntimeBindingId(), retried.getRuntimeBindingId());
        assertEquals(2, syncPort.publishedBindings.size());
    }

    private RuntimeRewriteBindingPublishRequest publishRequest(String rewriteRecordId) {
        RuntimeRewriteBindingPublishRequest request = new RuntimeRewriteBindingPublishRequest();
        request.setTenantId("tenant-a");
        request.setRewriteRecordId(rewriteRecordId);
        request.setRecommendationId("recommendation-001");
        request.setSourceType("QUERY");
        request.setSourceKind("QUERY_HISTORY");
        request.setSourceId("history-001");
        request.setSqlFingerprint("fp-001");
        request.setOriginalSqlDigest("digest-original-001");
        request.setRecommendedSqlText("SELECT id FROM orders");
        request.setDatasourceCode("hetu_main");
        request.setPublishedBy("publisher-001");
        return request;
    }

    private void setRequestContext() {
        RequestContext.set(
            "tenant-a",
            "service-user",
            Arrays.asList("SERVICE"),
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
    }

    private static final class RecordingJdbcAgentRewriteRuleSyncPort implements JdbcAgentRewriteRuleSyncPort {
        private final List<RuntimeRewriteBinding> publishedBindings = new ArrayList<RuntimeRewriteBinding>();
        private final List<RuntimeRewriteBinding> disabledBindings = new ArrayList<RuntimeRewriteBinding>();

        @Override
        public JdbcAgentRewriteRuleSyncResult publish(RuntimeRewriteBinding binding) {
            publishedBindings.add(binding);
            return JdbcAgentRewriteRuleSyncResult.builder()
                .syncStatus("SYNCED")
                .syncAction("PUBLISH")
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
        public JdbcAgentRewriteRuleSyncResult publish(RuntimeRewriteBinding binding) {
            return JdbcAgentRewriteRuleSyncResult.builder()
                .syncStatus("FAILED")
                .syncAction("PUBLISH")
                .failureReason("redis 不可用")
                .retryable(true)
                .alertRequired(true)
                .build();
        }

        @Override
        public JdbcAgentRewriteRuleSyncResult disable(RuntimeRewriteBinding binding) {
            return publish(binding);
        }
    }
}
