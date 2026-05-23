package com.company.queryexecution.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.queryexecution.application.controller.dto.QueryContextDTO;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.queryexecution.QueryExecutionCachePolicyApplyRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionCachePolicyResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionCachePolicyVerifyRequest;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QueryExecutionCacheGovernanceRuntimeServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldExposeDistributedBackendEvidenceWhenProviderBackendStoresAndHits() {
        RequestContext.set(
            "tenant-a",
            "user-a",
            "request-001",
            "trace-001",
            "unit-test",
            1L,
            2L
        );
        QueryExecutionCacheGovernanceRuntimeService service =
            new QueryExecutionCacheGovernanceRuntimeService(new FakeDistributedBackend(false));
        QueryExecutionCachePolicyResponse applyResponse = service.apply(cachePolicy());

        QueryContextDTO queryContext = new QueryContextDTO();
        queryContext.setSchemaVersion("schema-v1");
        QueryExecutionCacheGovernanceRuntimeService.CacheResolution miss =
            service.resolve("tenant-a", SqlFingerprintUtils.fingerprint("SELECT * FROM orders"), "HETU", queryContext);
        QueryExecutionStep backfilled = service.finalizeSuccessfulExecution(miss, queryStep());

        QueryExecutionCacheGovernanceRuntimeService.CacheResolution hit =
            service.resolve("tenant-a", SqlFingerprintUtils.fingerprint("SELECT * FROM orders"), "HETU", queryContext);
        QueryExecutionStep cachedStep = service.buildCacheHitStep(hit);

        assertEquals("APPLIED", applyResponse.getStatus());
        assertTrue(applyResponse.getRuntimeDetailsJson().contains("\"cacheBackendType\":\"REDIS\""));
        assertEquals("BACKFILLED", backfilled.getCacheGovernanceStatus());
        assertTrue(backfilled.getCacheGovernanceEvidence().contains("cacheBackendType=REDIS"));
        assertTrue(backfilled.getCacheGovernanceEvidence().contains("cacheBackendDistributed=true"));
        assertTrue(backfilled.getCacheGovernanceEvidence().contains("providerWriteStatus=STORED"));
        assertEquals("HIT", cachedStep.getCacheGovernanceStatus());
        assertTrue(cachedStep.isCacheHit());
        assertTrue(cachedStep.getCacheGovernanceEvidence().contains("providerReadStatus=HIT"));
    }

    @Test
    void shouldBypassGovernedCacheWhenDistributedBackendUnavailable() {
        RequestContext.set(
            "tenant-a",
            "user-a",
            "request-001",
            "trace-001",
            "unit-test",
            1L,
            2L
        );
        QueryExecutionCacheGovernanceRuntimeService service =
            new QueryExecutionCacheGovernanceRuntimeService(new FakeDistributedBackend(true));
        service.apply(cachePolicy());
        QueryContextDTO queryContext = new QueryContextDTO();
        queryContext.setSchemaVersion("schema-v1");

        QueryExecutionCacheGovernanceRuntimeService.CacheResolution resolution =
            service.resolve("tenant-a", SqlFingerprintUtils.fingerprint("SELECT * FROM orders"), "HETU", queryContext);
        QueryExecutionStep step = service.finalizeSuccessfulExecution(resolution, queryStep());

        assertEquals("BYPASSED", step.getCacheGovernanceStatus());
        assertFalse(step.isCacheHit());
        assertTrue(step.getCacheGovernanceEvidence().contains("riskCode=DISTRIBUTED_BACKEND_UNAVAILABLE"));
        assertTrue(step.getCacheGovernanceEvidence().contains("providerReadStatus=UNAVAILABLE"));
    }

    @Test
    void shouldEvictExpiredEntriesAndExposeTtlEvidence() throws Exception {
        RequestContext.set(
            "tenant-a",
            "user-a",
            "request-001",
            "trace-001",
            "unit-test",
            1L,
            2L
        );
        QueryExecutionCacheGovernanceRuntimeService service =
            new QueryExecutionCacheGovernanceRuntimeService(new FakeDistributedBackend(false), 10, 10, 0L);
        QueryExecutionCachePolicyApplyRequest policy = cachePolicy();
        policy.setTtlSeconds(Long.valueOf(1L));
        service.apply(policy);
        QueryContextDTO queryContext = new QueryContextDTO();
        queryContext.setSchemaVersion("schema-v1");
        QueryExecutionCacheGovernanceRuntimeService.CacheResolution miss =
            service.resolve("tenant-a", SqlFingerprintUtils.fingerprint("SELECT * FROM orders"), "HETU", queryContext);
        service.finalizeSuccessfulExecution(miss, queryStep());

        Thread.sleep(1100L);

        QueryExecutionCacheGovernanceRuntimeService.CacheResolution expired =
            service.resolve("tenant-a", SqlFingerprintUtils.fingerprint("SELECT * FROM orders"), "HETU", queryContext);
        QueryExecutionStep refreshed = service.finalizeSuccessfulExecution(expired, queryStep());

        assertEquals("BACKFILLED", refreshed.getCacheGovernanceStatus());
        assertTrue(refreshed.getCacheGovernanceEvidence().contains("evictionReason=TTL_EXPIRED"));
        assertTrue(refreshed.getCacheGovernanceEvidence().contains("ttlSeconds=1"));
    }

    @Test
    void shouldEvictOldestTenantEntryWhenTenantCapacityIsExceeded() {
        RequestContext.set(
            "tenant-a",
            "user-a",
            "request-001",
            "trace-001",
            "unit-test",
            1L,
            2L
        );
        QueryExecutionCacheGovernanceRuntimeService service =
            new QueryExecutionCacheGovernanceRuntimeService(new FakeDistributedBackend(false), 1, 10, 0L);
        service.apply(cachePolicy());
        QueryExecutionCachePolicyApplyRequest secondPolicy = cachePolicy("cache-policy-002", "SELECT * FROM customers");
        service.apply(secondPolicy);
        QueryContextDTO queryContext = new QueryContextDTO();
        queryContext.setSchemaVersion("schema-v1");
        QueryExecutionCacheGovernanceRuntimeService.CacheResolution firstMiss =
            service.resolve("tenant-a", SqlFingerprintUtils.fingerprint("SELECT * FROM orders"), "HETU", queryContext);
        service.finalizeSuccessfulExecution(firstMiss, queryStep());

        QueryExecutionCacheGovernanceRuntimeService.CacheResolution secondMiss =
            service.resolve("tenant-a", SqlFingerprintUtils.fingerprint("SELECT * FROM customers"), "HETU", queryContext);
        QueryExecutionStep secondBackfill = service.finalizeSuccessfulExecution(secondMiss, queryStep());

        QueryExecutionCacheGovernanceRuntimeService.CacheResolution evictedFirst =
            service.resolve("tenant-a", SqlFingerprintUtils.fingerprint("SELECT * FROM orders"), "HETU", queryContext);
        QueryExecutionStep refreshedFirst = service.finalizeSuccessfulExecution(evictedFirst, queryStep());
        QueryExecutionCachePolicyResponse verifyResponse =
            service.verify(cachePolicyVerify("cache-policy-002", "SELECT * FROM customers"));

        assertTrue(secondBackfill.getCacheGovernanceEvidence().contains("evictionReason=CAPACITY_EVICTED"));
        assertTrue(secondBackfill.getCacheGovernanceEvidence().contains("maxEntriesPerTenant=1"));
        assertEquals("BACKFILLED", refreshedFirst.getCacheGovernanceStatus());
        assertTrue(refreshedFirst.getCacheGovernanceEvidence().contains("providerReadStatus=MISS"));
        assertTrue(verifyResponse.getRuntimeDetailsJson().contains("\"maxEntriesPerPolicy\":10"));
        assertTrue(verifyResponse.getRuntimeDetailsJson().contains("\"maxEntriesPerTenant\":1"));
        assertTrue(verifyResponse.getRuntimeDetailsJson().contains("\"capacityRemainingForTenant\":0"));
        assertTrue(verifyResponse.getRuntimeDetailsJson().contains("\"evictionSummary\""));
        assertTrue(verifyResponse.getRuntimeDetailsJson().contains("\"cacheBackendType\":\"REDIS\""));
    }

    private QueryExecutionCachePolicyApplyRequest cachePolicy() {
        return cachePolicy("cache-policy-001", "SELECT * FROM orders");
    }

    private QueryExecutionCachePolicyApplyRequest cachePolicy(String policyId, String sqlText) {
        QueryExecutionCachePolicyApplyRequest request = new QueryExecutionCachePolicyApplyRequest();
        request.setTenantId("tenant-a");
        request.setPolicyId(policyId);
        request.setSqlFingerprint(SqlFingerprintUtils.fingerprint(sqlText));
        request.setDatasourceType("HETU");
        request.setSchemaVersion("schema-v1");
        request.setPolicyReason("distributed cache governance test");
        return request;
    }

    private QueryExecutionCachePolicyVerifyRequest cachePolicyVerify(String policyId, String sqlText) {
        QueryExecutionCachePolicyVerifyRequest request = new QueryExecutionCachePolicyVerifyRequest();
        request.setTenantId("tenant-a");
        request.setPolicyId(policyId);
        request.setSqlFingerprint(SqlFingerprintUtils.fingerprint(sqlText));
        request.setDatasourceType("HETU");
        return request;
    }

    private QueryExecutionStep queryStep() {
        return new QueryExecutionStep(
            DataSourceTypeEnum.HETU,
            Collections.<Map<String, Object>>singletonList(Collections.<String, Object>singletonMap("order_id", "1")),
            12L,
            10L,
            false,
            false
        );
    }

    private static final class FakeDistributedBackend implements QueryExecutionResultCacheBackend {

        private final boolean 不可用;
        private final Map<String, QueryExecutionStep> entries = new ConcurrentHashMap<String, QueryExecutionStep>();

        private FakeDistributedBackend(boolean 不可用) {
            this.不可用 = 不可用;
        }

        @Override
        public BackendDescriptor descriptor() {
            return new BackendDescriptor("REDIS", "redis-test", "PROVIDER_NATIVE_RESP", "unit-test", true);
        }

        @Override
        public CacheEntryReadResult read(String cacheKey) {
            if (不可用) {
                return CacheEntryReadResult.unavailable("providerReadStatus=UNAVAILABLE", "connection refused");
            }
            QueryExecutionStep step = entries.get(cacheKey);
            return step == null
                ? CacheEntryReadResult.miss("providerReadStatus=MISS")
                : CacheEntryReadResult.hit(step, "providerReadStatus=HIT");
        }

        @Override
        public CacheEntryWriteResult write(String cacheKey, QueryExecutionStep step) {
            entries.put(cacheKey, step);
            return CacheEntryWriteResult.written("providerWriteStatus=STORED");
        }

        @Override
        public CacheEntryInvalidateResult invalidateByPrefix(String cacheKeyPrefix) {
            int invalidated = 0;
            for (String cacheKey : entries.keySet()) {
                if (cacheKey.startsWith(cacheKeyPrefix) && entries.remove(cacheKey) != null) {
                    invalidated++;
                }
            }
            return CacheEntryInvalidateResult.completed(invalidated, "providerInvalidateStatus=COMPLETED");
        }

        @Override
        public CacheEntryCountResult countByPrefix(String cacheKeyPrefix) {
            int count = 0;
            for (String cacheKey : entries.keySet()) {
                if (cacheKey.startsWith(cacheKeyPrefix)) {
                    count++;
                }
            }
            return CacheEntryCountResult.completed(count, "providerCountStatus=COMPLETED");
        }

        @Override
        public CacheBackendVerifyResult verify() {
            return 不可用
                ? CacheBackendVerifyResult.unavailable("providerVerifyStatus=UNAVAILABLE", "connection refused")
                : CacheBackendVerifyResult.available("providerVerifyStatus=AVAILABLE");
        }
    }
}
