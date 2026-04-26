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
import java.util.LinkedHashMap;
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

    private final Map<String, CachePolicyBinding> bindings = new ConcurrentHashMap<String, CachePolicyBinding>();
    private final QueryExecutionResultCacheBackend cacheBackend;

    public QueryExecutionCacheGovernanceRuntimeService() {
        this(new InMemoryQueryExecutionResultCacheBackend());
    }

    @Autowired
    public QueryExecutionCacheGovernanceRuntimeService(QueryExecutionCacheBackendProperties properties) {
        this(createBackend(properties));
    }

    QueryExecutionCacheGovernanceRuntimeService(QueryExecutionResultCacheBackend cacheBackend) {
        this.cacheBackend = cacheBackend == null ? new InMemoryQueryExecutionResultCacheBackend() : cacheBackend;
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
            Instant.now()
        );
        bindings.put(bindingKey, binding);
        return responseFrom(
            binding,
            "APPLIED",
            true,
            "Governed cache policy is now active for result-cache eligibility, backend validation, and version checks.",
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
                "cachedEntryCount", Integer.valueOf(0)
            ), cacheBackend.verify().getProviderEvidence())));
            response.setContractStage(CONTRACT_STAGE);
            response.setImplementationStage(IMPLEMENTATION_STAGE);
            return response;
        }
        return responseFrom(
            binding,
            "VERIFIED",
            true,
            "Governed cache policy remains active with version-aware runtime checks and backend verification.",
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
                : invalidateEntries(binding);
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
                    "backendFailureReason", invalidation.getFailureReason()
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
            QueryExecutionResultCacheBackend.CacheEntryInvalidateResult invalidation = invalidateEntries(binding);
            CachePolicyBinding refreshedBinding = binding.withSchemaVersion(schemaVersion);
            bindings.put(bindingKey(tenantId, sqlFingerprint, datasourceType), refreshedBinding);
            return CacheResolution.invalidated(
                refreshedBinding,
                schemaVersion,
                cacheKey(tenantId, sqlFingerprint, datasourceType, schemaVersion),
                invalidation.getInvalidatedCount(),
                "Schema version changed from governed baseline " + binding.getSchemaVersion() + ".",
                backendEvidence(invalidation.getProviderEvidence())
            );
        }
        String cacheKey = cacheKey(tenantId, sqlFingerprint, datasourceType, schemaVersion);
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
            return CacheResolution.miss(binding, schemaVersion, cacheKey, backendEvidence(readResult.getProviderEvidence()));
        }
        return CacheResolution.hit(binding, schemaVersion, cacheKey, readResult.getStep(), backendEvidence(readResult.getProviderEvidence()));
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
        CacheResolution storedResolution = resolution.withBackendEvidence(backendEvidence(writeResult.getProviderEvidence()));
        return step.withCacheGovernance(false, STATUS_BACKFILLED, storedResolution.buildEvidence(true, "REFRESHED"));
    }

    private QueryExecutionResultCacheBackend.CacheEntryInvalidateResult invalidateEntries(CachePolicyBinding binding) {
        if (binding == null) {
            return QueryExecutionResultCacheBackend.CacheEntryInvalidateResult.completed(0, "providerInvalidateStatus=SKIPPED");
        }
        return cacheBackend.invalidateByPrefix(entryPrefix(binding));
    }

    private int countEntries(CachePolicyBinding binding) {
        if (binding == null) {
            return 0;
        }
        QueryExecutionResultCacheBackend.CacheEntryCountResult result = cacheBackend.countByPrefix(entryPrefix(binding));
        return result.isCompleted() ? result.getCount() : 0;
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
                newProviderEvidence
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
                newProviderEvidence
            );
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
        private final Instant appliedAt;

        private CachePolicyBinding(String tenantId,
                                   String policyId,
                                   String sqlFingerprint,
                                   String datasourceType,
                                   String schemaVersion,
                                   String sourcePlanId,
                                   String policyReason,
                                   Instant appliedAt) {
            this.tenantId = tenantId;
            this.policyId = policyId;
            this.sqlFingerprint = sqlFingerprint;
            this.datasourceType = datasourceType;
            this.schemaVersion = schemaVersion;
            this.sourcePlanId = sourcePlanId;
            this.policyReason = policyReason;
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
                appliedAt
            );
        }
    }

}
