package com.company.queryexecution.application.service;

import com.company.queryexecution.application.controller.dto.QueryContextDTO;
import com.company.queryexecution.config.QueryExecutionCacheBackendProperties;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.sqlforge.common.constants.CacheKeyConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.queryexecution.QueryExecutionCachePolicyApplyRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionCachePolicyInvalidateRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionCachePolicyResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionCachePolicyVerifyRequest;
import com.company.sqlforge.common.utils.JsonUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class QueryExecutionCacheGovernanceRuntimeService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "CACHE_GOVERNANCE_RUNTIME_BASELINE";
    private static final String SESSION_CACHE_BYPASS_KEY = "sqlforge.cache.bypass";
    private static final String STATUS_UNGOVERNED = "UNGOVERNED";
    private static final String STATUS_BYPASSED = "BYPASSED";
    private static final String STATUS_HIT = "HIT";
    private static final String STATUS_BACKFILLED = "BACKFILLED";
    private static final String STATUS_INVALIDATED = "INVALIDATED";
    private static final String RISK_SCHEMA_VERSION_MISSING = "SCHEMA_VERSION_MISSING";
    private static final String RISK_SCHEMA_VERSION_MISMATCH = "SCHEMA_VERSION_MISMATCH";
    private static final String RISK_SESSION_BYPASS = "SESSION_VARIABLE_BYPASS";
    private static final String RISK_BACKEND_UNAVAILABLE = "DISTRIBUTED_BACKEND_UNAVAILABLE";
    private static final String RISK_BACKEND_WRITE_FAILED = "DISTRIBUTED_BACKEND_WRITE_FAILED";
    private static final String EVICTION_TTL_EXPIRED = "TTL_EXPIRED";
    private static final String EVICTION_CAPACITY_EVICTED = "CAPACITY_EVICTED";
    private static final String EVICTION_MANUAL_INVALIDATED = "MANUAL_INVALIDATED";
    private static final String EVICTION_SCHEMA_VERSION_MISMATCH = "SCHEMA_VERSION_MISMATCH";

    private final Map<String, CachePolicyBinding> bindings = new ConcurrentHashMap<String, CachePolicyBinding>();
    private final Map<String, CacheEntryMetadata> entryMetadata = new ConcurrentHashMap<String, CacheEntryMetadata>();
    private final Map<String, EvictionSummary> evictionSummaries = new ConcurrentHashMap<String, EvictionSummary>();
    private final QueryExecutionResultCacheBackend cacheBackend;
    private final int defaultMaxEntriesPerTenant;
    private final int defaultMaxEntriesPerPolicy;
    private final long defaultTtlSeconds;

    public QueryExecutionCacheGovernanceRuntimeService() {
        this(new InMemoryQueryExecutionResultCacheBackend(), 1024, 128, 0L);
    }

    @Autowired
    public QueryExecutionCacheGovernanceRuntimeService(QueryExecutionCacheBackendProperties properties) {
        this(
            createBackend(properties),
            normalizePositive(properties == null ? 1024 : properties.getDefaultMaxEntriesPerTenant(), 1024),
            normalizePositive(properties == null ? 128 : properties.getDefaultMaxEntriesPerPolicy(), 128),
            normalizeNonNegative(properties == null ? 0L : properties.getDefaultTtlSeconds())
        );
    }

    QueryExecutionCacheGovernanceRuntimeService(QueryExecutionResultCacheBackend cacheBackend) {
        this(cacheBackend, 1024, 128, 0L);
    }

    QueryExecutionCacheGovernanceRuntimeService(QueryExecutionResultCacheBackend cacheBackend,
                                                int defaultMaxEntriesPerTenant,
                                                int defaultMaxEntriesPerPolicy,
                                                long defaultTtlSeconds) {
        this.cacheBackend = cacheBackend == null ? new InMemoryQueryExecutionResultCacheBackend() : cacheBackend;
        this.defaultMaxEntriesPerTenant = normalizePositive(defaultMaxEntriesPerTenant, 1024);
        this.defaultMaxEntriesPerPolicy = normalizePositive(defaultMaxEntriesPerPolicy, 128);
        this.defaultTtlSeconds = normalizeNonNegative(defaultTtlSeconds);
    }

    public QueryExecutionCachePolicyResponse apply(QueryExecutionCachePolicyApplyRequest request) {
        requireProtectedTenant(request == null ? null : request.getTenantId());
        String tenantId = request.getTenantId().trim();
        String policyId = requireText(request.getPolicyId(), "policyId");
        String sqlFingerprint = requireText(request.getSqlFingerprint(), "sqlFingerprint");
        String datasourceType = requireText(request.getDatasourceType(), "datasourceType");
        String schemaVersion = requireText(request.getSchemaVersion(), "schemaVersion");
        String bindingKey = bindingKey(tenantId, sqlFingerprint, datasourceType);
        CachePolicyBinding existing = bindings.get(bindingKey);
        if (existing != null && !policyId.equals(existing.getPolicyId())) {
            throw new BizException(
                ErrorCodeConstants.QUERY_EXECUTION_ROUTE_REJECTED,
                HttpStatus.CONFLICT,
                "A governed cache policy is already active for this tenant, SQL fingerprint, and datasource"
            );
        }
        CachePolicyBinding binding = new CachePolicyBinding(
            tenantId,
            policyId,
            sqlFingerprint,
            datasourceType,
            schemaVersion,
            trimToNull(request.getSourcePlanId()),
            trimToNull(request.getPolicyReason()),
            normalizeMaxEntries(request.getMaxEntries()),
            normalizeTtlSeconds(request.getTtlSeconds()),
            Instant.now()
        );
        bindings.put(bindingKey, binding);
        return responseFrom(
            binding,
            "APPLIED",
            true,
            "Governed cache policy is now active for result-cache eligibility, backend validation, capacity limits, TTL, and version checks.",
            countEntries(binding)
        );
    }

    public QueryExecutionCachePolicyResponse verify(QueryExecutionCachePolicyVerifyRequest request) {
        requireProtectedTenant(request == null ? null : request.getTenantId());
        String tenantId = request.getTenantId().trim();
        String policyId = requireText(request.getPolicyId(), "policyId");
        String sqlFingerprint = requireText(request.getSqlFingerprint(), "sqlFingerprint");
        String datasourceType = requireText(request.getDatasourceType(), "datasourceType");
        CachePolicyBinding binding = bindings.get(bindingKey(tenantId, sqlFingerprint, datasourceType));
        if (binding == null || !policyId.equals(binding.getPolicyId())) {
            QueryExecutionCachePolicyResponse response = new QueryExecutionCachePolicyResponse();
            response.setTenantId(tenantId);
            response.setPolicyId(policyId);
            response.setSqlFingerprint(sqlFingerprint);
            response.setTargetEngine(datasourceType);
            response.setActive(false);
            response.setStatus("MISSING");
            response.setPolicySummary("No governed cache policy is currently active for runtime verification.");
            response.setRuntimeDetailsJson(JsonUtils.toJson(withBackendDetails(details(
                "bindingState", "MISSING",
                "cachedEntryCount", Integer.valueOf(0),
                "capacitySummary", "NO_ACTIVE_POLICY"
            ), cacheBackend.verify().getProviderEvidence())));
            response.setContractStage(CONTRACT_STAGE);
            response.setImplementationStage(IMPLEMENTATION_STAGE);
            return response;
        }
        return responseFrom(
            binding,
            "VERIFIED",
            true,
            "Governed cache policy remains active with version-aware runtime checks, capacity limits, TTL, and backend verification.",
            countEntries(binding)
        );
    }

    public QueryExecutionCachePolicyResponse invalidate(QueryExecutionCachePolicyInvalidateRequest request) {
        requireProtectedTenant(request == null ? null : request.getTenantId());
        String tenantId = request.getTenantId().trim();
        String policyId = requireText(request.getPolicyId(), "policyId");
        String sqlFingerprint = requireText(request.getSqlFingerprint(), "sqlFingerprint");
        String datasourceType = requireText(request.getDatasourceType(), "datasourceType");
        CachePolicyBinding binding = bindings.get(bindingKey(tenantId, sqlFingerprint, datasourceType));
        QueryExecutionResultCacheBackend.CacheEntryInvalidateResult invalidation =
            binding == null || !policyId.equals(binding.getPolicyId())
                ? QueryExecutionResultCacheBackend.CacheEntryInvalidateResult.completed(0, "providerInvalidateStatus=SKIPPED")
                : invalidateEntries(binding, EVICTION_MANUAL_INVALIDATED);
        int invalidatedEntries = invalidation.getInvalidatedCount();
        QueryExecutionCachePolicyResponse response = new QueryExecutionCachePolicyResponse();
        response.setTenantId(tenantId);
        response.setPolicyId(policyId);
        response.setSqlFingerprint(sqlFingerprint);
        response.setTargetEngine(datasourceType);
        response.setSchemaVersion(binding == null ? trimToNull(request.getSchemaVersion()) : binding.getSchemaVersion());
        response.setSourcePlanId(binding == null ? null : binding.getSourcePlanId());
        response.setActive(binding != null && policyId.equals(binding.getPolicyId()));
        response.setStatus("INVALIDATED");
        response.setPolicySummary(invalidatedEntries > 0
            ? "Governed cache entries were invalidated and will require backfill on the next eligible execution."
            : "No governed cache entries were present, so invalidation completed idempotently.");
        response.setRuntimeDetailsJson(
            JsonUtils.toJson(
                withBackendDetails(
                    details(
                    "bindingState", response.isActive() ? "ACTIVE" : "ABSENT",
                    "invalidateReason", trimToNull(request.getInvalidateReason()),
                    "invalidatedEntryCount", Integer.valueOf(invalidatedEntries),
                    "backendOperationStatus", invalidation.isCompleted() ? "COMPLETED" : "FAILED",
                    "backendFailureReason", invalidation.getFailureReason(),
                    "evictionReason", EVICTION_MANUAL_INVALIDATED
                    ),
                    invalidation.getProviderEvidence()
                )
            )
        );
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        return response;
    }

    public CacheResolution resolve(String tenantId,
                                   String sqlFingerprint,
                                   String datasourceType,
                                   QueryContextDTO queryContext) {
        if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(sqlFingerprint) || !StringUtils.hasText(datasourceType)) {
            return CacheResolution.ungoverned();
        }
        CachePolicyBinding binding = bindings.get(bindingKey(tenantId, sqlFingerprint, datasourceType));
        if (binding == null) {
            return CacheResolution.ungoverned();
        }
        String schemaVersion = trimToNull(queryContext == null ? null : queryContext.getSchemaVersion());
        if (isSessionBypassRequested(queryContext)) {
            return CacheResolution.bypassed(
                binding,
                schemaVersion,
                RISK_SESSION_BYPASS,
                "Session variable requested cache bypass.",
                backendEvidence("providerReadStatus=SKIPPED")
            );
        }
        if (!StringUtils.hasText(schemaVersion)) {
            return CacheResolution.bypassed(
                binding,
                null,
                RISK_SCHEMA_VERSION_MISSING,
                "queryContext.schemaVersion is required for governed cache hits.",
                backendEvidence("providerReadStatus=SKIPPED")
            );
        }
        if (!binding.getSchemaVersion().equals(schemaVersion)) {
            QueryExecutionResultCacheBackend.CacheEntryInvalidateResult invalidation =
                invalidateEntries(binding, EVICTION_SCHEMA_VERSION_MISMATCH);
            CachePolicyBinding refreshedBinding = binding.withSchemaVersion(schemaVersion);
            bindings.put(bindingKey(tenantId, sqlFingerprint, datasourceType), refreshedBinding);
            return CacheResolution.invalidated(
                refreshedBinding,
                schemaVersion,
                cacheKey(tenantId, sqlFingerprint, datasourceType, schemaVersion),
                invalidation.getInvalidatedCount(),
                "Schema version changed from governed baseline " + binding.getSchemaVersion() + ".",
                backendEvidence(invalidation.getProviderEvidence()) + ";evictionReason=" + EVICTION_SCHEMA_VERSION_MISMATCH
            );
        }
        String cacheKey = cacheKey(tenantId, sqlFingerprint, datasourceType, schemaVersion);
        TtlEvictionResult ttlEviction = evictExpiredEntry(binding, cacheKey, Instant.now());
        if (ttlEviction.isEvicted()) {
            return CacheResolution.miss(
                binding,
                schemaVersion,
                cacheKey,
                backendEvidence(ttlEviction.getProviderEvidence())
                    + ";evictionReason=" + EVICTION_TTL_EXPIRED
                    + ";evictedEntryCount=1"
            );
        }
        QueryExecutionResultCacheBackend.CacheEntryReadResult readResult = cacheBackend.read(cacheKey);
        if (!readResult.isAvailable()) {
            return CacheResolution.bypassed(
                binding,
                schemaVersion,
                RISK_BACKEND_UNAVAILABLE,
                "Distributed cache backend is unavailable: " + readResult.getFailureReason(),
                backendEvidence(readResult.getProviderEvidence())
            );
        }
        if (readResult.getStep() == null) {
            return CacheResolution.miss(
                binding,
                schemaVersion,
                cacheKey,
                backendEvidence(readResult.getProviderEvidence()) + capacityEvidence(binding)
            );
        }
        touchMetadata(cacheKey);
        return CacheResolution.hit(
            binding,
            schemaVersion,
            cacheKey,
            readResult.getStep(),
            backendEvidence(readResult.getProviderEvidence()) + capacityEvidence(binding)
        );
    }

    public QueryExecutionStep buildCacheHitStep(CacheResolution resolution) {
        if (resolution == null || resolution.getCacheEntry() == null) {
            return null;
        }
        return resolution.getCacheEntry()
            .withCacheGovernance(true, STATUS_HIT, resolution.buildEvidence(false, "HIT"));
    }

    public QueryExecutionStep finalizeSuccessfulExecution(CacheResolution resolution, QueryExecutionStep step) {
        if (resolution == null || step == null) {
            return step;
        }
        if (!resolution.isGoverned()) {
            return step.withCacheGovernance(step.isCacheHit(), STATUS_UNGOVERNED, "governed=false");
        }
        if (!resolution.shouldStore()) {
            return step.withCacheGovernance(false, resolution.getStatus(), resolution.buildEvidence(false, null));
        }
        CapacityEvictionResult capacityEviction = evictForCapacity(resolution.getBinding(), resolution.getCacheKey(), Instant.now());
        QueryExecutionStep cacheableStep = step.withCacheGovernance(false, resolution.getStatus(), resolution.buildEvidence(false, null));
        QueryExecutionResultCacheBackend.CacheEntryWriteResult writeResult =
            cacheBackend.write(resolution.getCacheKey(), cacheableStep);
        if (!writeResult.isWritten()) {
            CacheResolution failedResolution = resolution.withBackendFailure(
                RISK_BACKEND_WRITE_FAILED,
                "Distributed cache backend write failed: " + writeResult.getFailureReason(),
                backendEvidence(writeResult.getProviderEvidence())
            );
            return step.withCacheGovernance(false, STATUS_BYPASSED, failedResolution.buildEvidence(false, "BACKFILL_FAILED"));
        }
        storeMetadata(resolution.getBinding(), resolution.getCacheKey(), Instant.now());
        CacheResolution storedResolution = resolution.withBackendEvidence(
            backendEvidence(writeResult.getProviderEvidence())
                + capacityEvidence(resolution.getBinding())
                + capacityEviction.evidence()
        );
        return step.withCacheGovernance(false, STATUS_BACKFILLED, storedResolution.buildEvidence(true, "REFRESHED"));
    }

    private QueryExecutionResultCacheBackend.CacheEntryInvalidateResult invalidateEntries(CachePolicyBinding binding,
                                                                                        String evictionReason) {
        if (binding == null) {
            return QueryExecutionResultCacheBackend.CacheEntryInvalidateResult.completed(0, "providerInvalidateStatus=SKIPPED");
        }
        QueryExecutionResultCacheBackend.CacheEntryInvalidateResult result = cacheBackend.invalidateByPrefix(entryPrefix(binding));
        int metadataInvalidated = removeMetadataByPrefix(binding, entryPrefix(binding), evictionReason);
        int invalidatedCount = Math.max(result.getInvalidatedCount(), metadataInvalidated);
        recordEviction(binding, evictionReason, invalidatedCount);
        String evidence = result.getProviderEvidence()
            + ";evictionReason=" + evictionReason
            + ";evictedEntryCount=" + invalidatedCount;
        return result.isCompleted()
            ? QueryExecutionResultCacheBackend.CacheEntryInvalidateResult.completed(invalidatedCount, evidence)
            : QueryExecutionResultCacheBackend.CacheEntryInvalidateResult.failed(evidence, result.getFailureReason());
    }

    private int countEntries(CachePolicyBinding binding) {
        if (binding == null) {
            return 0;
        }
        evictExpiredEntries(binding, Instant.now());
        QueryExecutionResultCacheBackend.CacheEntryCountResult result = cacheBackend.countByPrefix(entryPrefix(binding));
        int metadataCount = countPolicyMetadata(binding);
        return result.isCompleted() ? Math.max(result.getCount(), metadataCount) : metadataCount;
    }

    private int countTenantEntries(String tenantId) {
        int count = 0;
        for (CacheEntryMetadata metadata : entryMetadata.values()) {
            if (metadata != null && metadata.getTenantId().equals(tenantId)) {
                count++;
            }
        }
        return count;
    }

    private int countPolicyMetadata(CachePolicyBinding binding) {
        int count = 0;
        String bindingKey = bindingKey(binding.getTenantId(), binding.getSqlFingerprint(), binding.getDatasourceType());
        for (CacheEntryMetadata metadata : entryMetadata.values()) {
            if (metadata != null && metadata.getBindingKey().equals(bindingKey)) {
                count++;
            }
        }
        return count;
    }

    private TtlEvictionResult evictExpiredEntry(CachePolicyBinding binding, String cacheKey, Instant now) {
        CacheEntryMetadata metadata = entryMetadata.get(cacheKey);
        if (metadata == null || !metadata.isExpired(now)) {
            return TtlEvictionResult.notEvicted();
        }
        QueryExecutionResultCacheBackend.CacheEntryInvalidateResult result = cacheBackend.invalidateByPrefix(cacheKey);
        entryMetadata.remove(cacheKey);
        recordEviction(binding, EVICTION_TTL_EXPIRED, 1);
        return new TtlEvictionResult(
            true,
            result.getProviderEvidence() + ";providerInvalidateStatus=TTL_EVICTED"
        );
    }

    private void evictExpiredEntries(CachePolicyBinding binding, Instant now) {
        String prefix = entryPrefix(binding);
        for (String cacheKey : new ArrayList<String>(entryMetadata.keySet())) {
            CacheEntryMetadata metadata = entryMetadata.get(cacheKey);
            if (metadata != null && cacheKey.startsWith(prefix) && metadata.isExpired(now)) {
                cacheBackend.invalidateByPrefix(cacheKey);
                entryMetadata.remove(cacheKey);
                recordEviction(binding, EVICTION_TTL_EXPIRED, 1);
            }
        }
    }

    private CapacityEvictionResult evictForCapacity(CachePolicyBinding binding, String incomingCacheKey, Instant now) {
        if (binding == null) {
            return CapacityEvictionResult.none();
        }
        evictExpiredEntries(binding, now);
        int evicted = 0;
        List<String> evictedKeys = new ArrayList<String>();
        while (countPolicyMetadata(binding) >= binding.getMaxEntries()) {
            String oldest = oldestPolicyKey(binding, incomingCacheKey);
            if (!StringUtils.hasText(oldest)) {
                break;
            }
            evictKey(binding, oldest, EVICTION_CAPACITY_EVICTED);
            evicted++;
            evictedKeys.add(oldest);
        }
        while (countTenantEntries(binding.getTenantId()) >= defaultMaxEntriesPerTenant) {
            String oldest = oldestTenantKey(binding.getTenantId(), incomingCacheKey);
            if (!StringUtils.hasText(oldest)) {
                break;
            }
            CacheEntryMetadata metadata = entryMetadata.get(oldest);
            CachePolicyBinding evictionBinding = metadata == null ? binding : metadata.toBindingSnapshot(binding);
            evictKey(evictionBinding, oldest, EVICTION_CAPACITY_EVICTED);
            evicted++;
            evictedKeys.add(oldest);
        }
        if (evicted == 0) {
            return CapacityEvictionResult.none();
        }
        recordEviction(binding, EVICTION_CAPACITY_EVICTED, evicted);
        return new CapacityEvictionResult(evicted, evictedKeys);
    }

    private void evictKey(CachePolicyBinding binding, String cacheKey, String evictionReason) {
        cacheBackend.invalidateByPrefix(cacheKey);
        entryMetadata.remove(cacheKey);
        recordEviction(binding, evictionReason, 1);
    }

    private String oldestPolicyKey(CachePolicyBinding binding, String excludeCacheKey) {
        String bindingKey = bindingKey(binding.getTenantId(), binding.getSqlFingerprint(), binding.getDatasourceType());
        return oldestKey(bindingKey, binding.getTenantId(), excludeCacheKey, true);
    }

    private String oldestTenantKey(String tenantId, String excludeCacheKey) {
        return oldestKey(null, tenantId, excludeCacheKey, false);
    }

    private String oldestKey(String bindingKey, String tenantId, String excludeCacheKey, boolean policyScoped) {
        String oldestKey = null;
        Instant oldestAccess = null;
        for (Map.Entry<String, CacheEntryMetadata> entry : entryMetadata.entrySet()) {
            CacheEntryMetadata metadata = entry.getValue();
            if (metadata == null || entry.getKey().equals(excludeCacheKey)) {
                continue;
            }
            if (policyScoped && !metadata.getBindingKey().equals(bindingKey)) {
                continue;
            }
            if (!policyScoped && !metadata.getTenantId().equals(tenantId)) {
                continue;
            }
            if (oldestAccess == null || metadata.getLastAccessAt().isBefore(oldestAccess)) {
                oldestAccess = metadata.getLastAccessAt();
                oldestKey = entry.getKey();
            }
        }
        return oldestKey;
    }

    private void storeMetadata(CachePolicyBinding binding, String cacheKey, Instant now) {
        if (binding == null || !StringUtils.hasText(cacheKey)) {
            return;
        }
        entryMetadata.put(cacheKey, CacheEntryMetadata.from(binding, cacheKey, now));
    }

    private void touchMetadata(String cacheKey) {
        CacheEntryMetadata metadata = entryMetadata.get(cacheKey);
        if (metadata != null) {
            entryMetadata.put(cacheKey, metadata.touch(Instant.now()));
        }
    }

    private int removeMetadataByPrefix(CachePolicyBinding binding, String prefix, String evictionReason) {
        int removed = 0;
        for (String cacheKey : new ArrayList<String>(entryMetadata.keySet())) {
            if (cacheKey.startsWith(prefix) && entryMetadata.remove(cacheKey) != null) {
                removed++;
            }
        }
        if (removed > 0) {
            recordEviction(binding, evictionReason, removed);
        }
        return removed;
    }

    private void recordEviction(CachePolicyBinding binding, String evictionReason, int count) {
        if (binding == null || count <= 0 || !StringUtils.hasText(evictionReason)) {
            return;
        }
        String bindingKey = bindingKey(binding.getTenantId(), binding.getSqlFingerprint(), binding.getDatasourceType());
        EvictionSummary current = evictionSummaries.get(bindingKey);
        evictionSummaries.put(bindingKey, EvictionSummary.record(current, evictionReason, count));
    }

    private Map<String, Object> evictionSummary(CachePolicyBinding binding) {
        String bindingKey = bindingKey(binding.getTenantId(), binding.getSqlFingerprint(), binding.getDatasourceType());
        EvictionSummary summary = evictionSummaries.get(bindingKey);
        return summary == null ? null : summary.toMap();
    }

    private String capacityEvidence(CachePolicyBinding binding) {
        if (binding == null) {
            return "";
        }
        int policyCount = countPolicyMetadata(binding);
        int tenantCount = countTenantEntries(binding.getTenantId());
        return ";maxEntriesPerPolicy=" + binding.getMaxEntries()
            + ";maxEntriesPerTenant=" + defaultMaxEntriesPerTenant
            + ";policyCachedEntryCount=" + policyCount
            + ";tenantCachedEntryCount=" + tenantCount
            + ";ttlSeconds=" + binding.getTtlSeconds();
    }

    private QueryExecutionCachePolicyResponse responseFrom(CachePolicyBinding binding,
                                                           String status,
                                                           boolean active,
                                                           String summary,
                                                           int cachedEntryCount) {
        QueryExecutionCachePolicyResponse response = new QueryExecutionCachePolicyResponse();
        response.setTenantId(binding.getTenantId());
        response.setPolicyId(binding.getPolicyId());
        response.setSqlFingerprint(binding.getSqlFingerprint());
        response.setTargetEngine(binding.getDatasourceType());
        response.setSchemaVersion(binding.getSchemaVersion());
        response.setSourcePlanId(binding.getSourcePlanId());
        response.setActive(active);
        response.setStatus(status);
        response.setPolicySummary(summary);
        response.setRuntimeDetailsJson(
            JsonUtils.toJson(
                withBackendDetails(
                    details(
                    "bindingState", active ? "ACTIVE" : "INACTIVE",
                    "cachedEntryCount", Integer.valueOf(cachedEntryCount),
                    "tenantCachedEntryCount", Integer.valueOf(countTenantEntries(binding.getTenantId())),
                    "maxEntriesPerPolicy", Integer.valueOf(binding.getMaxEntries()),
                    "maxEntriesPerTenant", Integer.valueOf(defaultMaxEntriesPerTenant),
                    "ttlSeconds", Long.valueOf(binding.getTtlSeconds()),
                    "capacityRemainingForPolicy", Integer.valueOf(Math.max(0, binding.getMaxEntries() - cachedEntryCount)),
                    "capacityRemainingForTenant", Integer.valueOf(Math.max(0, defaultMaxEntriesPerTenant - countTenantEntries(binding.getTenantId()))),
                    "evictionSummary", evictionSummary(binding),
                    "appliedAt", binding.getAppliedAt().toString(),
                    "policyReason", binding.getPolicyReason()
                    ),
                    cacheBackend.verify().getProviderEvidence()
                )
            )
        );
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        return response;
    }

    private boolean isSessionBypassRequested(QueryContextDTO queryContext) {
        if (queryContext == null || queryContext.getSessionVariables() == null) {
            return false;
        }
        String value = queryContext.getSessionVariables().get(SESSION_CACHE_BYPASS_KEY);
        return "true".equalsIgnoreCase(trimToNull(value));
    }

    private String bindingKey(String tenantId, String sqlFingerprint, String datasourceType) {
        return CacheKeyConstants.QUERY_RESULT_CACHE_POLICY_PREFIX
            + tenantId.trim() + ":" + datasourceType.trim() + ":" + sqlFingerprint.trim();
    }

    private String cacheKey(String tenantId, String sqlFingerprint, String datasourceType, String schemaVersion) {
        return CacheKeyConstants.QUERY_RESULT_CACHE_ENTRY_PREFIX
            + tenantId.trim() + ":" + datasourceType.trim() + ":" + sqlFingerprint.trim() + ":" + schemaVersion.trim();
    }

    private String entryPrefix(CachePolicyBinding binding) {
        return CacheKeyConstants.QUERY_RESULT_CACHE_ENTRY_PREFIX
            + binding.getTenantId() + ":" + binding.getDatasourceType() + ":" + binding.getSqlFingerprint() + ":";
    }

    private Map<String, Object> withBackendDetails(Map<String, Object> details, String providerEvidence) {
        QueryExecutionResultCacheBackend.BackendDescriptor descriptor = cacheBackend.descriptor();
        details.put("cacheBackendType", descriptor.getBackendType());
        details.put("cacheBackendProvider", descriptor.getProviderName());
        details.put("cacheBackendCarrier", descriptor.getCarrierMode());
        details.put("cacheBackendDistributed", Boolean.valueOf(descriptor.isDistributed()));
        details.put("cacheBackendEnvironment", descriptor.getEnvironmentLabel());
        details.put("providerEvidence", providerEvidence);
        return details;
    }

    private String backendEvidence(String providerEvidence) {
        QueryExecutionResultCacheBackend.BackendDescriptor descriptor = cacheBackend.descriptor();
        StringBuilder evidence = new StringBuilder();
        evidence.append("cacheBackendType=").append(descriptor.getBackendType())
            .append(";cacheBackendProvider=").append(descriptor.getProviderName())
            .append(";cacheBackendCarrier=").append(descriptor.getCarrierMode())
            .append(";cacheBackendDistributed=").append(descriptor.isDistributed())
            .append(";cacheBackendEnvironment=").append(descriptor.getEnvironmentLabel());
        if (StringUtils.hasText(providerEvidence)) {
            evidence.append(";").append(providerEvidence);
        }
        return evidence.toString();
    }

    private int normalizeMaxEntries(Integer requestedMaxEntries) {
        if (requestedMaxEntries == null || requestedMaxEntries.intValue() <= 0) {
            return defaultMaxEntriesPerPolicy;
        }
        return Math.min(defaultMaxEntriesPerPolicy, requestedMaxEntries.intValue());
    }

    private long normalizeTtlSeconds(Long requestedTtlSeconds) {
        if (requestedTtlSeconds == null || requestedTtlSeconds.longValue() < 0L) {
            return defaultTtlSeconds;
        }
        if (defaultTtlSeconds <= 0L) {
            return requestedTtlSeconds.longValue();
        }
        if (requestedTtlSeconds.longValue() <= 0L) {
            return defaultTtlSeconds;
        }
        return Math.min(defaultTtlSeconds, requestedTtlSeconds.longValue());
    }

    private static int normalizePositive(int value, int fallback) {
        return value <= 0 ? fallback : value;
    }

    private static long normalizeNonNegative(long value) {
        return Math.max(0L, value);
    }

    private static QueryExecutionResultCacheBackend createBackend(QueryExecutionCacheBackendProperties properties) {
        if (properties == null || !StringUtils.hasText(properties.getType())) {
            return new InMemoryQueryExecutionResultCacheBackend();
        }
        String type = properties.getType().trim();
        if ("REDIS".equalsIgnoreCase(type) || "PROVIDER_NATIVE_REDIS".equalsIgnoreCase(type)) {
            return new RedisProtocolQueryExecutionResultCacheBackend(properties);
        }
        return new InMemoryQueryExecutionResultCacheBackend(properties.getProviderName(), properties.getEnvironmentLabel());
    }

    private void requireProtectedTenant(String tenantId) {
        if (!StringUtils.hasText(RequestContext.getTenantId())) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context"
            );
        }
        if (!RequestContext.getTenantId().equals(tenantId == null ? null : tenantId.trim())) {
            throw new AccessDeniedException("Request tenantId does not match authenticated tenant context");
        }
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                fieldName + " must not be empty"
            );
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Map<String, Object> details(Object... keyValues) {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        for (int index = 0; index < keyValues.length; index += 2) {
            details.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return details;
    }

    static final class CacheResolution {

        private final CachePolicyBinding binding;
        private final String status;
        private final String schemaVersion;
        private final String cacheKey;
        private final QueryExecutionStep cacheEntry;
        private final String riskCode;
        private final String reason;
        private final Integer invalidatedEntryCount;
        private final String providerEvidence;

        private CacheResolution(CachePolicyBinding binding,
                                String status,
                                String schemaVersion,
                                String cacheKey,
                                QueryExecutionStep cacheEntry,
                                String riskCode,
                                String reason,
                                Integer invalidatedEntryCount,
                                String providerEvidence) {
            this.binding = binding;
            this.status = status;
            this.schemaVersion = schemaVersion;
            this.cacheKey = cacheKey;
            this.cacheEntry = cacheEntry;
            this.riskCode = riskCode;
            this.reason = reason;
            this.invalidatedEntryCount = invalidatedEntryCount;
            this.providerEvidence = providerEvidence;
        }

        static CacheResolution ungoverned() {
            return new CacheResolution(null, STATUS_UNGOVERNED, null, null, null, null, null, null, null);
        }

        static CacheResolution bypassed(CachePolicyBinding binding, String schemaVersion, String riskCode, String reason) {
            return bypassed(binding, schemaVersion, riskCode, reason, null);
        }

        static CacheResolution bypassed(CachePolicyBinding binding,
                                        String schemaVersion,
                                        String riskCode,
                                        String reason,
                                        String providerEvidence) {
            return new CacheResolution(binding, STATUS_BYPASSED, schemaVersion, null, null, riskCode, reason, null, providerEvidence);
        }

        static CacheResolution invalidated(CachePolicyBinding binding,
                                           String schemaVersion,
                                           String cacheKey,
                                           int invalidatedEntryCount,
                                           String reason,
                                           String providerEvidence) {
            return new CacheResolution(
                binding,
                STATUS_INVALIDATED,
                schemaVersion,
                cacheKey,
                null,
                RISK_SCHEMA_VERSION_MISMATCH,
                reason,
                Integer.valueOf(invalidatedEntryCount),
                providerEvidence
            );
        }

        static CacheResolution miss(CachePolicyBinding binding, String schemaVersion, String cacheKey, String providerEvidence) {
            return new CacheResolution(binding, STATUS_BACKFILLED, schemaVersion, cacheKey, null, null, null, null, providerEvidence);
        }

        static CacheResolution hit(CachePolicyBinding binding,
                                   String schemaVersion,
                                   String cacheKey,
                                   QueryExecutionStep cacheEntry,
                                   String providerEvidence) {
            return new CacheResolution(binding, STATUS_HIT, schemaVersion, cacheKey, cacheEntry, null, null, null, providerEvidence);
        }

        boolean isGoverned() {
            return binding != null;
        }

        CachePolicyBinding getBinding() {
            return binding;
        }

        boolean shouldStore() {
            return binding != null
                && StringUtils.hasText(cacheKey)
                && !STATUS_HIT.equals(status)
                && !STATUS_BYPASSED.equals(status);
        }

        String buildEvidence(boolean backfillApplied, String entryState) {
            if (binding == null) {
                return "governed=false";
            }
            StringBuilder evidence = new StringBuilder();
            evidence.append("governed=true")
                .append(";policyId=").append(binding.getPolicyId())
                .append(";datasourceType=").append(binding.getDatasourceType())
                .append(";schemaVersion=").append(schemaVersion)
                .append(";status=").append(status)
                .append(";sourcePlanId=").append(binding.getSourcePlanId())
                .append(";backfillApplied=").append(backfillApplied);
            if (StringUtils.hasText(entryState)) {
                evidence.append(";entryState=").append(entryState);
            }
            if (StringUtils.hasText(riskCode)) {
                evidence.append(";riskCode=").append(riskCode);
            }
            if (StringUtils.hasText(reason)) {
                evidence.append(";reason=").append(reason.replace(';', ','));
            }
            if (invalidatedEntryCount != null) {
                evidence.append(";invalidatedEntryCount=").append(invalidatedEntryCount.intValue());
            }
            if (StringUtils.hasText(providerEvidence)) {
                evidence.append(";").append(providerEvidence.replace('\n', ' ').replace('\r', ' '));
            }
            return evidence.toString();
        }

        CacheResolution withBackendEvidence(String newProviderEvidence) {
            return new CacheResolution(
                binding,
                status,
                schemaVersion,
                cacheKey,
                cacheEntry,
                riskCode,
                reason,
                invalidatedEntryCount,
                appendEvidence(providerEvidence, newProviderEvidence)
            );
        }

        CacheResolution withBackendFailure(String newRiskCode, String newReason, String newProviderEvidence) {
            return new CacheResolution(
                binding,
                STATUS_BYPASSED,
                schemaVersion,
                cacheKey,
                cacheEntry,
                newRiskCode,
                newReason,
                invalidatedEntryCount,
                appendEvidence(providerEvidence, newProviderEvidence)
            );
        }

        private String appendEvidence(String currentEvidence, String newEvidence) {
            if (!StringUtils.hasText(currentEvidence)) {
                return newEvidence;
            }
            if (!StringUtils.hasText(newEvidence)) {
                return currentEvidence;
            }
            return currentEvidence + ";" + newEvidence;
        }

        String getStatus() {
            return status;
        }

        String getCacheKey() {
            return cacheKey;
        }

        QueryExecutionStep getCacheEntry() {
            return cacheEntry;
        }
    }

    private static final class CachePolicyBinding {

        private final String tenantId;
        private final String policyId;
        private final String sqlFingerprint;
        private final String datasourceType;
        private final String schemaVersion;
        private final String sourcePlanId;
        private final String policyReason;
        private final int maxEntries;
        private final long ttlSeconds;
        private final Instant appliedAt;

        private CachePolicyBinding(String tenantId,
                                   String policyId,
                                   String sqlFingerprint,
                                   String datasourceType,
                                   String schemaVersion,
                                   String sourcePlanId,
                                   String policyReason,
                                   int maxEntries,
                                   long ttlSeconds,
                                   Instant appliedAt) {
            this.tenantId = tenantId;
            this.policyId = policyId;
            this.sqlFingerprint = sqlFingerprint;
            this.datasourceType = datasourceType;
            this.schemaVersion = schemaVersion;
            this.sourcePlanId = sourcePlanId;
            this.policyReason = policyReason;
            this.maxEntries = maxEntries;
            this.ttlSeconds = ttlSeconds;
            this.appliedAt = appliedAt;
        }

        private String getTenantId() {
            return tenantId;
        }

        private String getPolicyId() {
            return policyId;
        }

        private String getSqlFingerprint() {
            return sqlFingerprint;
        }

        private String getDatasourceType() {
            return datasourceType;
        }

        private String getSchemaVersion() {
            return schemaVersion;
        }

        private String getSourcePlanId() {
            return sourcePlanId;
        }

        private String getPolicyReason() {
            return policyReason;
        }

        private int getMaxEntries() {
            return maxEntries;
        }

        private long getTtlSeconds() {
            return ttlSeconds;
        }

        private Instant getAppliedAt() {
            return appliedAt;
        }

        private CachePolicyBinding withSchemaVersion(String newSchemaVersion) {
            return new CachePolicyBinding(
                tenantId,
                policyId,
                sqlFingerprint,
                datasourceType,
                newSchemaVersion,
                sourcePlanId,
                policyReason,
                maxEntries,
                ttlSeconds,
                appliedAt
            );
        }
    }

    private static final class CacheEntryMetadata {

        private final String cacheKey;
        private final String bindingKey;
        private final String tenantId;
        private final String policyId;
        private final String sqlFingerprint;
        private final String datasourceType;
        private final String schemaVersion;
        private final Instant createdAt;
        private final Instant lastAccessAt;
        private final Instant expiresAt;

        private CacheEntryMetadata(String cacheKey,
                                   String bindingKey,
                                   String tenantId,
                                   String policyId,
                                   String sqlFingerprint,
                                   String datasourceType,
                                   String schemaVersion,
                                   Instant createdAt,
                                   Instant lastAccessAt,
                                   Instant expiresAt) {
            this.cacheKey = cacheKey;
            this.bindingKey = bindingKey;
            this.tenantId = tenantId;
            this.policyId = policyId;
            this.sqlFingerprint = sqlFingerprint;
            this.datasourceType = datasourceType;
            this.schemaVersion = schemaVersion;
            this.createdAt = createdAt;
            this.lastAccessAt = lastAccessAt;
            this.expiresAt = expiresAt;
        }

        static CacheEntryMetadata from(CachePolicyBinding binding, String cacheKey, Instant now) {
            Instant expiresAt = binding.getTtlSeconds() <= 0L ? null : now.plusSeconds(binding.getTtlSeconds());
            return new CacheEntryMetadata(
                cacheKey,
                CacheKeyConstants.QUERY_RESULT_CACHE_POLICY_PREFIX
                    + binding.getTenantId() + ":" + binding.getDatasourceType() + ":" + binding.getSqlFingerprint(),
                binding.getTenantId(),
                binding.getPolicyId(),
                binding.getSqlFingerprint(),
                binding.getDatasourceType(),
                binding.getSchemaVersion(),
                now,
                now,
                expiresAt
            );
        }

        boolean isExpired(Instant now) {
            return expiresAt != null && !expiresAt.isAfter(now);
        }

        CacheEntryMetadata touch(Instant now) {
            return new CacheEntryMetadata(
                cacheKey,
                bindingKey,
                tenantId,
                policyId,
                sqlFingerprint,
                datasourceType,
                schemaVersion,
                createdAt,
                now,
                expiresAt
            );
        }

        CachePolicyBinding toBindingSnapshot(CachePolicyBinding fallback) {
            return new CachePolicyBinding(
                tenantId,
                policyId,
                sqlFingerprint,
                datasourceType,
                schemaVersion,
                fallback == null ? null : fallback.getSourcePlanId(),
                fallback == null ? null : fallback.getPolicyReason(),
                fallback == null ? 1 : fallback.getMaxEntries(),
                fallback == null ? 0L : fallback.getTtlSeconds(),
                fallback == null ? createdAt : fallback.getAppliedAt()
            );
        }

        String getBindingKey() {
            return bindingKey;
        }

        String getTenantId() {
            return tenantId;
        }

        Instant getLastAccessAt() {
            return lastAccessAt;
        }
    }

    private static final class EvictionSummary {

        private final String lastEvictionReason;
        private final int totalEvictedEntries;
        private final Instant lastEvictedAt;

        private EvictionSummary(String lastEvictionReason, int totalEvictedEntries, Instant lastEvictedAt) {
            this.lastEvictionReason = lastEvictionReason;
            this.totalEvictedEntries = totalEvictedEntries;
            this.lastEvictedAt = lastEvictedAt;
        }

        static EvictionSummary record(EvictionSummary current, String reason, int count) {
            int existing = current == null ? 0 : current.totalEvictedEntries;
            return new EvictionSummary(reason, existing + count, Instant.now());
        }

        Map<String, Object> toMap() {
            Map<String, Object> summary = new LinkedHashMap<String, Object>();
            summary.put("lastEvictionReason", lastEvictionReason);
            summary.put("totalEvictedEntries", Integer.valueOf(totalEvictedEntries));
            summary.put("lastEvictedAt", lastEvictedAt.toString());
            return summary;
        }
    }

    private static final class CapacityEvictionResult {

        private final int evictedCount;
        private final List<String> evictedKeys;

        private CapacityEvictionResult(int evictedCount, List<String> evictedKeys) {
            this.evictedCount = evictedCount;
            this.evictedKeys = evictedKeys;
        }

        static CapacityEvictionResult none() {
            return new CapacityEvictionResult(0, new ArrayList<String>());
        }

        String evidence() {
            if (evictedCount <= 0) {
                return "";
            }
            return ";evictionReason=" + EVICTION_CAPACITY_EVICTED
                + ";evictedEntryCount=" + evictedCount
                + ";evictedKeySample=" + (evictedKeys.isEmpty() ? null : evictedKeys.get(0));
        }
    }

    private static final class TtlEvictionResult {

        private final boolean evicted;
        private final String providerEvidence;

        private TtlEvictionResult(boolean evicted, String providerEvidence) {
            this.evicted = evicted;
            this.providerEvidence = providerEvidence;
        }

        static TtlEvictionResult notEvicted() {
            return new TtlEvictionResult(false, null);
        }

        boolean isEvicted() {
            return evicted;
        }

        String getProviderEvidence() {
            return providerEvidence;
        }
    }

}
