package com.company.queryexecution.application.service;

import com.company.queryexecution.domain.rewrite.RuntimeRewriteBinding;
import com.company.queryexecution.domain.rewrite.repository.RuntimeRewriteBindingRepository;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingActivationRequest;
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
    public RuntimeRewriteBindingResponse activate(RuntimeRewriteBindingActivationRequest request) {
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        requireProtectedTenant(tenantId);
        String rewriteRecordId = requireText(request.getRewriteRecordId(), "rewriteRecordId");
        String sqlFingerprint = requireText(request.getSqlFingerprint(), "sqlFingerprint");
        RuntimeRewriteBinding active =
            runtimeRewriteBindingRepository.findActiveByTenantIdAndSqlFingerprint(tenantId, sqlFingerprint);
        if (active != null) {
            if (rewriteRecordId.equals(active.getRewriteRecordId())) {
                JdbcAgentRewriteRuleSyncResult syncResult = syncActivate(active);
                return responseFrom(
                    active,
                    "该改写记录的运行时改写绑定已处于生效状态。",
                    syncResult
                );
            }
            throw new BizException(
                ErrorCodeConstants.QUERY_EXECUTION_ROUTE_REJECTED,
                HttpStatus.CONFLICT,
                "该租户与 SQL 指纹已存在生效的运行时改写绑定"
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
            .activatedBy(resolveOperator(request.getActivatedBy()))
            .activatedAt(now)
            .createdAt(now)
            .updatedAt(now)
            .build();
        runtimeRewriteBindingRepository.save(binding);
        JdbcAgentRewriteRuleSyncResult syncResult = syncActivate(binding);
        return responseFrom(
            binding,
            "运行时改写绑定已生效，可用于生产自动改写查找。",
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
                "没有可暂停的运行时改写绑定。");
        }
        RuntimeRewriteBinding paused =
            binding.pause(resolveOperator(request.getOperatorId()), request.getReason(), Instant.now());
        runtimeRewriteBindingRepository.save(paused);
        JdbcAgentRewriteRuleSyncResult syncResult = syncDisable(paused);
        return responseFrom(
            paused,
            "运行时改写绑定已暂停，不再参与自动改写。",
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
            return missingResponse(tenantId, sqlFingerprint, "未找到生效的运行时改写绑定。");
        }
        if (StringUtils.hasText(request.getDatasourceCode())
            && !request.getDatasourceCode().trim().equalsIgnoreCase(binding.getDatasourceCode())) {
            return missingResponse(tenantId, sqlFingerprint, "没有生效的运行时改写绑定与数据源证据匹配。");
        }
        return responseFrom(binding, "已找到生效的运行时改写绑定。", null);
    }

    private RuntimeRewriteBinding resolveMutationTarget(RuntimeRewriteBindingStateChangeRequest request) {
        if (StringUtils.hasText(request.getRuntimeBindingId())) {
            RuntimeRewriteBinding binding =
                runtimeRewriteBindingRepository.findByRuntimeBindingId(request.getRuntimeBindingId().trim());
            if (binding != null && !request.getTenantId().trim().equals(binding.getTenantId())) {
                throw new AccessDeniedException("请求 tenantId 与运行时改写绑定租户不一致");
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
            "activatedAt", binding.getActivatedAt() == null ? null : binding.getActivatedAt().toString(),
            "activatedBy", binding.getActivatedBy()
        );
        if (binding.getPausedAt() != null) {
            details.put("pausedAt", binding.getPausedAt().toString());
            details.put("pausedBy", binding.getPausedBy());
            details.put("pauseReason", binding.getPauseReason());
        }
        if (syncResult != null) {
            details.put("jdbcAgentRedisSync", syncResult.toDetails());
        }
        return details;
    }

    private JdbcAgentRewriteRuleSyncResult syncActivate(RuntimeRewriteBinding binding) {
        try {
            return jdbcAgentRewriteRuleSyncPort.activate(binding);
        } catch (RuntimeException ex) {
            return syncFailed("ACTIVATE", binding, ex);
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
                "已认证请求上下文缺少 tenantId"
            );
        }
        if (!RequestContext.getTenantId().equals(tenantId == null ? null : tenantId.trim())) {
            throw new AccessDeniedException("请求 tenantId 与已认证租户上下文不一致");
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
                fieldName + " 不能为空"
            );
        }
        return value.trim();
    }

    private String newRuntimeBindingId() {
        return "rwb-" + UUID.randomUUID().toString().replace("-", "");
    }
}
