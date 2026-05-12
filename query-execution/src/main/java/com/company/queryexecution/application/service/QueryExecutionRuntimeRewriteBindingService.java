package com.company.queryexecution.application.service;

import com.company.queryexecution.domain.rewrite.RuntimeRewriteBinding;
import com.company.queryexecution.domain.rewrite.repository.RuntimeRewriteBindingRepository;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingPublishRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResolveRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingStateChangeRequest;
import com.company.sqlforge.common.utils.JsonUtils;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class QueryExecutionRuntimeRewriteBindingService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "RUNTIME_REWRITE_BINDING_DB_BASELINE";
    private static final String RULE_VERSION_PREFIX = "runtime-rewrite-v";

    private final RuntimeRewriteBindingRepository runtimeRewriteBindingRepository;
    private final JdbcAgentRewriteRuleSyncPort jdbcAgentRewriteRuleSyncPort;

    @Autowired
    public QueryExecutionRuntimeRewriteBindingService(
        RuntimeRewriteBindingRepository runtimeRewriteBindingRepository,
        JdbcAgentRewriteRuleSyncPort jdbcAgentRewriteRuleSyncPort
    ) {
        this.runtimeRewriteBindingRepository = runtimeRewriteBindingRepository;
        this.jdbcAgentRewriteRuleSyncPort = jdbcAgentRewriteRuleSyncPort == null
            ? new NoopJdbcAgentRewriteRuleSyncPort()
            : jdbcAgentRewriteRuleSyncPort;
    }

    public QueryExecutionRuntimeRewriteBindingService(
        RuntimeRewriteBindingRepository runtimeRewriteBindingRepository
    ) {
        this(runtimeRewriteBindingRepository, new NoopJdbcAgentRewriteRuleSyncPort());
    }

    @Transactional
    public RuntimeRewriteBindingResponse publish(RuntimeRewriteBindingPublishRequest request) {
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        requireProtectedTenant(tenantId);
        String rewriteRecordId = requireText(request.getRewriteRecordId(), "rewriteRecordId");
        String sqlFingerprint = requireText(request.getSqlFingerprint(), "sqlFingerprint");
        RuntimeRewriteBinding active =
            runtimeRewriteBindingRepository.findActiveByTenantIdAndSqlFingerprint(tenantId, sqlFingerprint);
        if (active != null) {
            if (rewriteRecordId.equals(active.getRewriteRecordId())) {
                JdbcAgentRewriteRuleSyncResult syncResult = syncPublish(active);
                return responseFrom(
                    active,
                    "Runtime rewrite binding is already active for this rewrite record.",
                    syncResult
                );
            }
            throw new BizException(
                ErrorCodeConstants.QUERY_EXECUTION_ROUTE_REJECTED,
                HttpStatus.CONFLICT,
                "An active runtime rewrite binding already exists for this tenant and SQL fingerprint"
            );
        }

        RuntimeRewriteBinding latest =
            runtimeRewriteBindingRepository.findLatestByTenantIdAndSqlFingerprint(tenantId, sqlFingerprint);
        long nextRuleVersion = latest == null ? 1L : latest.getRuleVersion() + 1L;
        Instant now = Instant.now();
        RuntimeRewriteBinding binding = RuntimeRewriteBinding.builder()
            .runtimeBindingId(newRuntimeBindingId())
            .tenantId(tenantId)
            .rewriteRecordId(rewriteRecordId)
            .recommendationId(request.getRecommendationId())
            .sourceType(requireText(request.getSourceType(), "sourceType"))
            .sourceKind(requireText(request.getSourceKind(), "sourceKind"))
            .sourceId(requireText(request.getSourceId(), "sourceId"))
            .sqlFingerprint(sqlFingerprint)
            .originalSqlDigest(requireText(request.getOriginalSqlDigest(), "originalSqlDigest"))
            .recommendedSqlText(requireText(request.getRecommendedSqlText(), "recommendedSqlText"))
            .datasourceCode(requireText(request.getDatasourceCode(), "datasourceCode"))
            .ruleVersion(nextRuleVersion)
            .runtimeRuleVersion(RULE_VERSION_PREFIX + nextRuleVersion)
            .publishedBy(resolveOperator(request.getPublishedBy()))
            .publishedAt(now)
            .createdAt(now)
            .updatedAt(now)
            .build();
        runtimeRewriteBindingRepository.save(binding);
        JdbcAgentRewriteRuleSyncResult syncResult = syncPublish(binding);
        return responseFrom(
            binding,
            "Runtime rewrite binding is active for production auto rewrite lookup.",
            syncResult
        );
    }

    @Transactional
    public RuntimeRewriteBindingResponse pause(RuntimeRewriteBindingStateChangeRequest request) {
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        requireProtectedTenant(tenantId);
        RuntimeRewriteBinding binding = resolveMutationTarget(request);
        if (binding == null) {
            return missingResponse(tenantId, request == null ? null : request.getSqlFingerprint(),
                "No runtime rewrite binding is available to pause.");
        }
        RuntimeRewriteBinding paused =
            binding.pause(resolveOperator(request.getOperatorId()), request.getReason(), Instant.now());
        runtimeRewriteBindingRepository.save(paused);
        JdbcAgentRewriteRuleSyncResult syncResult = syncDisable(paused);
        return responseFrom(
            paused,
            "Runtime rewrite binding is paused and no longer eligible for auto rewrite.",
            syncResult
        );
    }

    @Transactional
    public RuntimeRewriteBindingResponse unpublish(RuntimeRewriteBindingStateChangeRequest request) {
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        requireProtectedTenant(tenantId);
        RuntimeRewriteBinding binding = resolveMutationTarget(request);
        if (binding == null) {
            return missingResponse(tenantId, request == null ? null : request.getSqlFingerprint(),
                "No runtime rewrite binding is available to unpublish.");
        }
        RuntimeRewriteBinding unpublished =
            binding.unpublish(resolveOperator(request.getOperatorId()), request.getReason(), Instant.now());
        runtimeRewriteBindingRepository.save(unpublished);
        JdbcAgentRewriteRuleSyncResult syncResult = syncDisable(unpublished);
        return responseFrom(
            unpublished,
            "Runtime rewrite binding is unpublished and retained for version trace.",
            syncResult
        );
    }

    public RuntimeRewriteBindingResponse resolveActive(RuntimeRewriteBindingResolveRequest request) {
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        requireProtectedTenant(tenantId);
        String sqlFingerprint = requireText(request.getSqlFingerprint(), "sqlFingerprint");
        RuntimeRewriteBinding binding =
            runtimeRewriteBindingRepository.findActiveByTenantIdAndSqlFingerprint(tenantId, sqlFingerprint);
        if (binding == null) {
            return missingResponse(tenantId, sqlFingerprint, "No active runtime rewrite binding was found.");
        }
        if (StringUtils.hasText(request.getDatasourceCode())
            && !request.getDatasourceCode().trim().equalsIgnoreCase(binding.getDatasourceCode())) {
            return missingResponse(tenantId, sqlFingerprint, "No active runtime rewrite binding matched datasource evidence.");
        }
        return responseFrom(binding, "Active runtime rewrite binding was found.", null);
    }

    private RuntimeRewriteBinding resolveMutationTarget(RuntimeRewriteBindingStateChangeRequest request) {
        if (StringUtils.hasText(request.getRuntimeBindingId())) {
            RuntimeRewriteBinding binding =
                runtimeRewriteBindingRepository.findByRuntimeBindingId(request.getRuntimeBindingId().trim());
            if (binding != null && !request.getTenantId().trim().equals(binding.getTenantId())) {
                throw new AccessDeniedException("Request tenantId does not match runtime rewrite binding tenant");
            }
            return binding;
        }
        String sqlFingerprint = requireText(request.getSqlFingerprint(), "sqlFingerprint");
        return runtimeRewriteBindingRepository.findActiveByTenantIdAndSqlFingerprint(
            request.getTenantId().trim(),
            sqlFingerprint
        );
    }

    private RuntimeRewriteBindingResponse responseFrom(RuntimeRewriteBinding binding,
                                                       String summary,
                                                       JdbcAgentRewriteRuleSyncResult syncResult) {
        RuntimeRewriteBindingResponse response = new RuntimeRewriteBindingResponse();
        response.setTenantId(binding.getTenantId());
        response.setRuntimeBindingId(binding.getRuntimeBindingId());
        response.setRewriteRecordId(binding.getRewriteRecordId());
        response.setRecommendationId(binding.getRecommendationId());
        response.setSourceType(binding.getSourceType());
        response.setSourceKind(binding.getSourceKind());
        response.setSourceId(binding.getSourceId());
        response.setSqlFingerprint(binding.getSqlFingerprint());
        response.setOriginalSqlDigest(binding.getOriginalSqlDigest());
        response.setRecommendedSqlText(binding.getRecommendedSqlText());
        response.setDatasourceCode(binding.getDatasourceCode());
        response.setStatus(binding.getStatus().name());
        response.setActive(binding.isActive());
        response.setRuleVersion(Long.valueOf(binding.getRuleVersion()));
        response.setRuntimeRuleVersion(binding.getRuntimeRuleVersion());
        response.setRuntimeSummary(summary);
        response.setRuntimeDetailsJson(JsonUtils.toJson(detailsFrom(binding, syncResult)));
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        return response;
    }

    private RuntimeRewriteBindingResponse missingResponse(String tenantId, String sqlFingerprint, String summary) {
        RuntimeRewriteBindingResponse response = new RuntimeRewriteBindingResponse();
        response.setTenantId(tenantId);
        response.setSqlFingerprint(sqlFingerprint);
        response.setStatus("MISSING");
        response.setActive(false);
        response.setRuntimeSummary(summary);
        response.setRuntimeDetailsJson(JsonUtils.toJson(details("bindingState", "MISSING")));
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        return response;
    }

    private Map<String, Object> detailsFrom(RuntimeRewriteBinding binding,
                                            JdbcAgentRewriteRuleSyncResult syncResult) {
        Map<String, Object> details = details(
            "bindingState", binding.getStatus().name(),
            "runtimeBindingId", binding.getRuntimeBindingId(),
            "runtimeRuleVersion", binding.getRuntimeRuleVersion(),
            "ruleVersion", Long.valueOf(binding.getRuleVersion()),
            "publishedAt", binding.getPublishedAt() == null ? null : binding.getPublishedAt().toString(),
            "publishedBy", binding.getPublishedBy()
        );
        if (binding.getPausedAt() != null) {
            details.put("pausedAt", binding.getPausedAt().toString());
            details.put("pausedBy", binding.getPausedBy());
            details.put("pauseReason", binding.getPauseReason());
        }
        if (binding.getUnpublishedAt() != null) {
            details.put("unpublishedAt", binding.getUnpublishedAt().toString());
            details.put("unpublishedBy", binding.getUnpublishedBy());
            details.put("unpublishReason", binding.getUnpublishReason());
        }
        if (syncResult != null) {
            details.put("jdbcAgentRedisSync", syncResult.toDetails());
        }
        return details;
    }

    private JdbcAgentRewriteRuleSyncResult syncPublish(RuntimeRewriteBinding binding) {
        try {
            return jdbcAgentRewriteRuleSyncPort.publish(binding);
        } catch (RuntimeException ex) {
            return syncFailed("PUBLISH", binding, ex);
        }
    }

    private JdbcAgentRewriteRuleSyncResult syncDisable(RuntimeRewriteBinding binding) {
        try {
            return jdbcAgentRewriteRuleSyncPort.disable(binding);
        } catch (RuntimeException ex) {
            return syncFailed("DISABLE", binding, ex);
        }
    }

    private JdbcAgentRewriteRuleSyncResult syncFailed(String action, RuntimeRewriteBinding binding, RuntimeException ex) {
        return JdbcAgentRewriteRuleSyncResult.builder()
            .syncStatus("FAILED")
            .syncAction(action)
            .failureReason(ex.getMessage())
            .retryable(true)
            .alertRequired(true)
            .build();
    }

    private Map<String, Object> details(Object... keyValues) {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        for (int i = 0; i < keyValues.length; i += 2) {
            details.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return details;
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

    private String resolveOperator(String operatorId) {
        if (StringUtils.hasText(operatorId)) {
            return operatorId.trim();
        }
        return requireText(RequestContext.getUserId(), "operatorId");
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

    private String newRuntimeBindingId() {
        return "rwb-" + UUID.randomUUID().toString().replace("-", "");
    }
}
