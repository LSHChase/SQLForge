package com.company.queryexecution.application.service;

import com.company.queryexecution.domain.query.QueryExecutionStep;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryQueryExecutionResultCacheBackend implements QueryExecutionResultCacheBackend {

    private final Map<String, QueryExecutionStep> cacheEntries = new ConcurrentHashMap<String, QueryExecutionStep>();
    private final BackendDescriptor descriptor;

    InMemoryQueryExecutionResultCacheBackend() {
        this("REPO_CLOSED_IN_MEMORY", "repo-default");
    }

    InMemoryQueryExecutionResultCacheBackend(String providerName, String environmentLabel) {
        this.descriptor = new BackendDescriptor(
            "IN_MEMORY",
            providerName == null || providerName.trim().isEmpty() ? "REPO_CLOSED_IN_MEMORY" : providerName.trim(),
            "LOCAL_PROCESS",
            environmentLabel == null || environmentLabel.trim().isEmpty() ? "repo-default" : environmentLabel.trim(),
            false
        );
    }

    @Override
    public BackendDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public CacheEntryReadResult read(String cacheKey) {
        QueryExecutionStep step = cacheEntries.get(cacheKey);
        if (step == null) {
            return CacheEntryReadResult.miss("providerReadStatus=MISS");
        }
        return CacheEntryReadResult.hit(step, "providerReadStatus=HIT");
    }

    @Override
    public CacheEntryWriteResult write(String cacheKey, QueryExecutionStep step) {
        cacheEntries.put(cacheKey, step);
        return CacheEntryWriteResult.written("providerWriteStatus=STORED");
    }

    @Override
    public CacheEntryInvalidateResult invalidateByPrefix(String cacheKeyPrefix) {
        int invalidated = 0;
        for (String cacheKey : new ArrayList<String>(cacheEntries.keySet())) {
            if (cacheKey.startsWith(cacheKeyPrefix) && cacheEntries.remove(cacheKey) != null) {
                invalidated++;
            }
        }
        return CacheEntryInvalidateResult.completed(invalidated, "providerInvalidateStatus=COMPLETED");
    }

    @Override
    public CacheEntryCountResult countByPrefix(String cacheKeyPrefix) {
        int count = 0;
        for (String cacheKey : cacheEntries.keySet()) {
            if (cacheKey.startsWith(cacheKeyPrefix)) {
                count++;
            }
        }
        return CacheEntryCountResult.completed(count, "providerCountStatus=COMPLETED");
    }

    @Override
    public CacheBackendVerifyResult verify() {
        return CacheBackendVerifyResult.available("providerVerifyStatus=AVAILABLE");
    }
}
