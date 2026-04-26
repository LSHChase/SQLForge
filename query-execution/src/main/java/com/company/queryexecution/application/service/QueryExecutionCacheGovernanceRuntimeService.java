package com.company.queryexecution.application.service;

import com.company.queryexecution.application.controller.dto.QueryContextDTO;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<String, CachePolicyBinding> bindings = new ConcurrentHashMap<String, CachePolicyBinding>();
    private final Map<String, CacheEntry> cacheEntries = new ConcurrentHashMap<String, CacheEntry>();

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
            "Governed cache policy is now active for result-cache eligibility and version validation.",
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
            response.setRuntimeDetailsJson(JsonUtils.toJson(details("bindingState", "MISSING", "cachedEntryCount", Integer.valueOf(0))));
            response.setContractStage(CONTRACT_STAGE);
            response.setImplementationStage(IMPLEMENTATION_STAGE);
            return response;
        }
        return responseFrom(
            binding,
            "VERIFIED",
            true,
            "Governed cache policy remains active with version-aware runtime checks.",
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
        int invalidatedEntries = binding == null || !policyId.equals(binding.getPolicyId()) ? 0 : invalidateEntries(binding);
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
                details(
                    "bindingState", response.isActive() ? "ACTIVE" : "ABSENT",
                    "invalidateReason", trimToNull(request.getInvalidateReason()),
                    "invalidatedEntryCount", Integer.valueOf(invalidatedEntries)
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
            return CacheResolution.bypassed(binding, schemaVersion, RISK_SESSION_BYPASS, "Session variable requested cache bypass.");
        }
        if (!StringUtils.hasText(schemaVersion)) {
            return CacheResolution.bypassed(
                binding,
                null,
                RISK_SCHEMA_VERSION_MISSING,
                "queryContext.schemaVersion is required for governed cache hits."
            );
        }
        if (!binding.getSchemaVersion().equals(schemaVersion)) {
            int invalidatedEntries = invalidateEntries(binding);
            CachePolicyBinding refreshedBinding = binding.withSchemaVersion(schemaVersion);
            bindings.put(bindingKey(tenantId, sqlFingerprint, datasourceType), refreshedBinding);
            return CacheResolution.invalidated(
                refreshedBinding,
                schemaVersion,
                cacheKey(tenantId, sqlFingerprint, datasourceType, schemaVersion),
                invalidatedEntries,
                "Schema version changed from governed baseline " + binding.getSchemaVersion() + "."
            );
        }
        String cacheKey = cacheKey(tenantId, sqlFingerprint, datasourceType, schemaVersion);
        CacheEntry cacheEntry = cacheEntries.get(cacheKey);
        if (cacheEntry == null) {
            return CacheResolution.miss(binding, schemaVersion, cacheKey);
        }
        return CacheResolution.hit(binding, schemaVersion, cacheKey, cacheEntry);
    }

    public QueryExecutionStep buildCacheHitStep(CacheResolution resolution) {
        if (resolution == null || resolution.getCacheEntry() == null || resolution.getCacheEntry().getStep() == null) {
            return null;
        }
        return resolution.getCacheEntry()
            .getStep()
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
        cacheEntries.put(
            resolution.getCacheKey(),
            new CacheEntry(cacheableStep)
        );
        return step.withCacheGovernance(false, STATUS_BACKFILLED, resolution.buildEvidence(true, "REFRESHED"));
    }

    private int invalidateEntries(CachePolicyBinding binding) {
        if (binding == null) {
            return 0;
        }
        int invalidated = 0;
        String prefix = CacheKeyConstants.QUERY_RESULT_CACHE_ENTRY_PREFIX
            + binding.getTenantId() + ":" + binding.getDatasourceType() + ":" + binding.getSqlFingerprint() + ":";
        for (String cacheKey : new ArrayList<String>(cacheEntries.keySet())) {
            if (cacheKey.startsWith(prefix) && cacheEntries.remove(cacheKey) != null) {
                invalidated++;
            }
        }
        return invalidated;
    }

    private int countEntries(CachePolicyBinding binding) {
        if (binding == null) {
            return 0;
        }
        int count = 0;
        String prefix = CacheKeyConstants.QUERY_RESULT_CACHE_ENTRY_PREFIX
            + binding.getTenantId() + ":" + binding.getDatasourceType() + ":" + binding.getSqlFingerprint() + ":";
        for (String cacheKey : cacheEntries.keySet()) {
            if (cacheKey.startsWith(prefix)) {
                count++;
            }
        }
        return count;
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
                details(
                    "bindingState", active ? "ACTIVE" : "INACTIVE",
                    "cachedEntryCount", Integer.valueOf(cachedEntryCount),
                    "appliedAt", binding.getAppliedAt().toString(),
                    "policyReason", binding.getPolicyReason()
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
        private final CacheEntry cacheEntry;
        private final String riskCode;
        private final String reason;
        private final Integer invalidatedEntryCount;

        private CacheResolution(CachePolicyBinding binding,
                                String status,
                                String schemaVersion,
                                String cacheKey,
                                CacheEntry cacheEntry,
                                String riskCode,
                                String reason,
                                Integer invalidatedEntryCount) {
            this.binding = binding;
            this.status = status;
            this.schemaVersion = schemaVersion;
            this.cacheKey = cacheKey;
            this.cacheEntry = cacheEntry;
            this.riskCode = riskCode;
            this.reason = reason;
            this.invalidatedEntryCount = invalidatedEntryCount;
        }

        static CacheResolution ungoverned() {
            return new CacheResolution(null, STATUS_UNGOVERNED, null, null, null, null, null, null);
        }

        static CacheResolution bypassed(CachePolicyBinding binding, String schemaVersion, String riskCode, String reason) {
            return new CacheResolution(binding, STATUS_BYPASSED, schemaVersion, null, null, riskCode, reason, null);
        }

        static CacheResolution invalidated(CachePolicyBinding binding,
                                           String schemaVersion,
                                           String cacheKey,
                                           int invalidatedEntryCount,
                                           String reason) {
            return new CacheResolution(
                binding,
                STATUS_INVALIDATED,
                schemaVersion,
                cacheKey,
                null,
                RISK_SCHEMA_VERSION_MISMATCH,
                reason,
                Integer.valueOf(invalidatedEntryCount)
            );
        }

        static CacheResolution miss(CachePolicyBinding binding, String schemaVersion, String cacheKey) {
            return new CacheResolution(binding, STATUS_BACKFILLED, schemaVersion, cacheKey, null, null, null, null);
        }

        static CacheResolution hit(CachePolicyBinding binding, String schemaVersion, String cacheKey, CacheEntry cacheEntry) {
            return new CacheResolution(binding, STATUS_HIT, schemaVersion, cacheKey, cacheEntry, null, null, null);
        }

        boolean isGoverned() {
            return binding != null;
        }

        boolean shouldStore() {
            return binding != null
                && StringUtils.hasText(cacheKey)
                && STATUS_HIT != status
                && STATUS_BYPASSED != status;
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
            return evidence.toString();
        }

        String getStatus() {
            return status;
        }

        String getCacheKey() {
            return cacheKey;
        }

        CacheEntry getCacheEntry() {
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

    private static final class CacheEntry {

        private final QueryExecutionStep step;

        private CacheEntry(QueryExecutionStep step) {
            this.step = step;
        }

        private QueryExecutionStep getStep() {
            return step;
        }
    }
}
