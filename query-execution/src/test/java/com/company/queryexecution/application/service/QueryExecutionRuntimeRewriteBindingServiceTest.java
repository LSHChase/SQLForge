package com.company.queryexecution.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.queryexecution.application.controller.dto.RuntimeRewriteBindingPublishRequest;
import com.company.queryexecution.application.controller.dto.RuntimeRewriteBindingResolveRequest;
import com.company.queryexecution.application.controller.dto.RuntimeRewriteBindingResponse;
import com.company.queryexecution.application.controller.dto.RuntimeRewriteBindingStateChangeRequest;
import com.company.queryexecution.domain.rewrite.RuntimeRewriteBinding;
import com.company.queryexecution.domain.rewrite.RuntimeRewriteBindingStatus;
import com.company.queryexecution.domain.rewrite.repository.RuntimeRewriteBindingRepository;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
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
        QueryExecutionRuntimeRewriteBindingService service =
            new QueryExecutionRuntimeRewriteBindingService(repository);

        RuntimeRewriteBindingResponse published = service.publish(publishRequest("rewrite-001"));

        assertEquals("ACTIVE", published.getStatus());
        assertTrue(published.isActive());
        assertEquals(Long.valueOf(1), published.getRuleVersion());
        assertEquals("runtime-rewrite-v1", published.getRuntimeRuleVersion());
        assertEquals("SELECT id FROM orders", published.getRecommendedSqlText());

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
}
