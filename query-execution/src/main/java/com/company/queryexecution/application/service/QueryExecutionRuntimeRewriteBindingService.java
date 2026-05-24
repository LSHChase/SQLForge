package com.company.queryexecution.application.service;

import com.company.queryexecution.domain.rewrite.RuntimeRewriteBinding;
import com.company.queryexecution.domain.rewrite.repository.RuntimeRewriteBindingRepository;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import com.company.sqlforge.common.logicalobject.SqlSurfaceObjectRefExtractor;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingActivationRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResolveRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingStateChangeRequest;
import com.company.sqlforge.common.rewrite.RuntimeSqlRewriteTemplateEngine;
import com.company.sqlforge.common.rewrite.RuntimeSqlRewriteTemplateResult;
import com.company.sqlforge.common.utils.JsonUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
        List<LogicalObjectSurface> runtimeMatchObjectRefs = resolveRuntimeMatchObjectRefs(request);
        List<String> runtimeMatchObjectNames = resolveRuntimeMatchObjectNames(request, runtimeMatchObjectRefs);
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
            .originalSqlText(trimToNull(request.getOriginalSqlText()))
            .recommendedSqlText(requireText(request.getRecommendedSqlText(), "recommendedSqlText"))
            .rewriteMatchMode(resolveRewriteMatchMode(request))
            .rewriteProgramJson(resolveRewriteProgramJson(request))
            .templateFamilyFingerprint(resolveTemplateFamilyFingerprint(request))
            .runtimeMatchObjectRefs(runtimeMatchObjectRefs)
            .runtimeMatchObjectNames(runtimeMatchObjectNames)
            .analysisPhysicalObjectRefs(nullSafeSurfaceList(request.getAnalysisPhysicalObjectRefs()))
            .metadataSnapshotVersion(request.getMetadataSnapshotVersion())
            .viewDefinitionHash(request.getViewDefinitionHash())
            .metadataDegradationReason(request.getMetadataDegradationReason())
            .datasourceCode(requireText(request.getDatasourceCode(), "datasourceCode"))
            .ruleVersion(nextRuleVersion)
            .runtimeRuleVersion(RULE_VERSION_PREFIX + nextRuleVersion)
            .activatedBy(resolveActor(request.getActivatedBy()))
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
            binding.pause(resolveActor(request.getActorId()), request.getReason(), Instant.now());
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
        List<String> currentRuntimeMatchObjectNames = resolveRequestedRuntimeMatchObjectNames(request);
        RuntimeRewriteBinding binding =
            runtimeRewriteBindingRepository.findActiveByTenantIdAndSqlFingerprint(tenantId, sqlFingerprint);
        if (binding != null) {
            if (!datasourceMatches(request, binding)) {
                return missingResponse(tenantId, sqlFingerprint, "没有生效的运行时改写绑定与数据源证据匹配。");
            }
            if (!runtimeObjectNamesMatch(currentRuntimeMatchObjectNames, binding)) {
                return missingResponse(tenantId, sqlFingerprint, "生效的运行时改写绑定未通过 SQL 表面对象名匹配。");
            }
            RuntimeSqlRewriteTemplateResult templateResult = resolveTemplateReplay(binding, request);
            if (!templateResult.isApplied() && StringUtils.hasText(binding.getOriginalSqlText())) {
                return missingResponse(
                    tenantId,
                    sqlFingerprint,
                    "生效的运行时改写绑定未通过模板匹配：" + templateResult.getFailureReason()
                );
            }
            return responseFrom(
                binding,
                "已找到生效的运行时改写绑定。",
                null,
                templateResult,
                sqlFingerprint
            );
        }
        RuntimeTemplateMatch match = resolveTemplateFamilyMatch(
            tenantId,
            sqlFingerprint,
            request,
            currentRuntimeMatchObjectNames
        );
        if (match.isAmbiguous()) {
            return missingResponse(tenantId, sqlFingerprint, "多个运行时改写模板同时匹配，已保守跳过自动改写。");
        }
        if (match.getBinding() == null) {
            return missingResponse(tenantId, sqlFingerprint, "未找到生效的运行时改写绑定。");
        }
        return responseFrom(
            match.getBinding(),
            "已通过运行时改写模板匹配找到生效绑定。",
            null,
            match.getTemplateResult(),
            sqlFingerprint
        );
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
        return responseFrom(binding, summary, syncResult, null, null);
    }

    private RuntimeRewriteBindingResponse responseFrom(RuntimeRewriteBinding binding,
                                                       String summary,
                                                       JdbcAgentRewriteRuleSyncResult syncResult,
                                                       RuntimeSqlRewriteTemplateResult templateResult,
                                                       String resolvedSqlFingerprint) {
        RuntimeRewriteBindingResponse response = new RuntimeRewriteBindingResponse();
        response.setTenantId(binding.getTenantId());
        response.setRuntimeBindingId(binding.getRuntimeBindingId());
        response.setRewriteRecordId(binding.getRewriteRecordId());
        response.setRecommendationId(binding.getRecommendationId());
        response.setSourceType(binding.getSourceType());
        response.setSourceKind(binding.getSourceKind());
        response.setSourceId(binding.getSourceId());
        response.setSqlFingerprint(StringUtils.hasText(resolvedSqlFingerprint)
            ? resolvedSqlFingerprint
            : binding.getSqlFingerprint());
        response.setOriginalSqlDigest(binding.getOriginalSqlDigest());
        response.setOriginalSqlText(binding.getOriginalSqlText());
        response.setRecommendedSqlText(templateResult != null && templateResult.isApplied()
            ? templateResult.getRewrittenSql()
            : binding.getRecommendedSqlText());
        response.setRewriteMatchMode(templateResult != null && templateResult.isApplied()
            ? templateResult.getMatchMode()
            : binding.getRewriteMatchMode());
        response.setRewriteProgramJson(StringUtils.hasText(binding.getRewriteProgramJson())
            ? binding.getRewriteProgramJson()
            : templateResult == null ? null : templateResult.getProgramJson());
        response.setTemplateFamilyFingerprint(binding.getTemplateFamilyFingerprint());
        response.setRuntimeMatchObjectRefs(binding.getRuntimeMatchObjectRefs());
        response.setRuntimeMatchObjectNames(binding.getRuntimeMatchObjectNames());
        response.setAnalysisPhysicalObjectRefs(binding.getAnalysisPhysicalObjectRefs());
        response.setMetadataSnapshotVersion(binding.getMetadataSnapshotVersion());
        response.setViewDefinitionHash(binding.getViewDefinitionHash());
        response.setMetadataDegradationReason(binding.getMetadataDegradationReason());
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
            "rewriteMatchMode", binding.getRewriteMatchMode(),
            "templateFamilyFingerprint", binding.getTemplateFamilyFingerprint(),
            "runtimeMatchObjectNames", binding.getRuntimeMatchObjectNames(),
            "analysisPhysicalObjectRefs", binding.getAnalysisPhysicalObjectRefs(),
            "metadataSnapshotVersion", binding.getMetadataSnapshotVersion(),
            "viewDefinitionHash", binding.getViewDefinitionHash(),
            "metadataDegradationReason", binding.getMetadataDegradationReason(),
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

    private RuntimeSqlRewriteTemplateResult resolveTemplateReplay(RuntimeRewriteBinding binding,
                                                                  RuntimeRewriteBindingResolveRequest request) {
        if (!StringUtils.hasText(binding.getOriginalSqlText())) {
            return RuntimeSqlRewriteTemplateResult.notApplied("ORIGINAL_SQL_TEMPLATE_UNAVAILABLE");
        }
        return RuntimeSqlRewriteTemplateEngine.rewrite(
            binding.getOriginalSqlText(),
            binding.getRecommendedSqlText(),
            request == null ? null : request.getSqlText()
        );
    }

    private RuntimeTemplateMatch resolveTemplateFamilyMatch(String tenantId,
                                                           String sqlFingerprint,
                                                           RuntimeRewriteBindingResolveRequest request,
                                                           List<String> currentRuntimeMatchObjectNames) {
        if (!StringUtils.hasText(request == null ? null : request.getSqlText())) {
            return RuntimeTemplateMatch.none();
        }
        List<RuntimeTemplateMatch> matches = new ArrayList<RuntimeTemplateMatch>();
        List<RuntimeRewriteBinding> activeBindings = runtimeRewriteBindingRepository.findActiveByTenantId(tenantId);
        for (RuntimeRewriteBinding candidate : activeBindings) {
            if (candidate == null
                || sqlFingerprint.equals(candidate.getSqlFingerprint())
                || !datasourceMatches(request, candidate)) {
                continue;
            }
            if (!runtimeObjectNamesMatch(currentRuntimeMatchObjectNames, candidate)) {
                continue;
            }
            RuntimeSqlRewriteTemplateResult templateResult = resolveTemplateReplay(candidate, request);
            if (templateResult.isApplied()) {
                matches.add(RuntimeTemplateMatch.one(candidate, templateResult));
            }
        }
        if (matches.isEmpty()) {
            return RuntimeTemplateMatch.none();
        }
        if (matches.size() > 1) {
            return RuntimeTemplateMatch.ambiguous();
        }
        return matches.get(0);
    }

    private boolean datasourceMatches(RuntimeRewriteBindingResolveRequest request, RuntimeRewriteBinding binding) {
        return !StringUtils.hasText(request == null ? null : request.getDatasourceCode())
            || request.getDatasourceCode().trim().equalsIgnoreCase(binding.getDatasourceCode());
    }

    private String resolveRewriteMatchMode(RuntimeRewriteBindingActivationRequest request) {
        return StringUtils.hasText(request.getOriginalSqlText())
            ? RuntimeSqlRewriteTemplateEngine.MATCH_MODE
            : "EXACT_FINGERPRINT";
    }

    private String resolveRewriteProgramJson(RuntimeRewriteBindingActivationRequest request) {
        if (StringUtils.hasText(request.getRewriteProgramJson())) {
            return request.getRewriteProgramJson().trim();
        }
        if (!StringUtils.hasText(request.getOriginalSqlText())) {
            return null;
        }
        return RuntimeSqlRewriteTemplateEngine.buildProgramJson(
            request.getOriginalSqlText(),
            request.getRecommendedSqlText()
        );
    }

    private String resolveTemplateFamilyFingerprint(RuntimeRewriteBindingActivationRequest request) {
        if (StringUtils.hasText(request.getTemplateFamilyFingerprint())) {
            return request.getTemplateFamilyFingerprint().trim();
        }
        if (!StringUtils.hasText(request.getOriginalSqlText())) {
            return request.getSqlFingerprint();
        }
        return RuntimeSqlRewriteTemplateEngine.templateFamilyFingerprint(request.getOriginalSqlText());
    }

    private List<LogicalObjectSurface> resolveRuntimeMatchObjectRefs(RuntimeRewriteBindingActivationRequest request) {
        if (request != null && request.getRuntimeMatchObjectRefs() != null
            && !request.getRuntimeMatchObjectRefs().isEmpty()) {
            return nullSafeSurfaceList(request.getRuntimeMatchObjectRefs());
        }
        return SqlSurfaceObjectRefExtractor.extractSurfaceRefs(request == null ? null : request.getOriginalSqlText());
    }

    private List<String> resolveRuntimeMatchObjectNames(RuntimeRewriteBindingActivationRequest request,
                                                        List<LogicalObjectSurface> refs) {
        if (request != null && request.getRuntimeMatchObjectNames() != null
            && !request.getRuntimeMatchObjectNames().isEmpty()) {
            return SqlSurfaceObjectRefExtractor.normalizeObjectNames(request.getRuntimeMatchObjectNames());
        }
        return SqlSurfaceObjectRefExtractor.surfaceObjectNames(refs);
    }

    private List<String> resolveRequestedRuntimeMatchObjectNames(RuntimeRewriteBindingResolveRequest request) {
        if (request == null) {
            return Collections.emptyList();
        }
        if (request.getRuntimeMatchObjectNames() != null && !request.getRuntimeMatchObjectNames().isEmpty()) {
            return SqlSurfaceObjectRefExtractor.normalizeObjectNames(request.getRuntimeMatchObjectNames());
        }
        if (request.getRuntimeMatchObjectRefs() != null && !request.getRuntimeMatchObjectRefs().isEmpty()) {
            return SqlSurfaceObjectRefExtractor.surfaceObjectNames(request.getRuntimeMatchObjectRefs());
        }
        return SqlSurfaceObjectRefExtractor.extractSurfaceObjectNames(request.getSqlText());
    }

    private boolean runtimeObjectNamesMatch(List<String> currentNames, RuntimeRewriteBinding binding) {
        List<String> bindingNames = binding == null
            ? Collections.<String>emptyList()
            : SqlSurfaceObjectRefExtractor.normalizeObjectNames(binding.getRuntimeMatchObjectNames());
        if (bindingNames.isEmpty()) {
            return true;
        }
        List<String> normalizedCurrent = SqlSurfaceObjectRefExtractor.normalizeObjectNames(currentNames);
        if (normalizedCurrent.isEmpty()) {
            return false;
        }
        return new LinkedHashSet<String>(bindingNames).equals(new LinkedHashSet<String>(normalizedCurrent));
    }

    private List<LogicalObjectSurface> nullSafeSurfaceList(List<LogicalObjectSurface> refs) {
        if (refs == null || refs.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<LogicalObjectSurface>(refs);
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

    private String resolveActor(String actorId) {
        if (StringUtils.hasText(actorId)) {
            return actorId.trim();
        }
        return requireText(RequestContext.getUserId(), "actorId");
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                fieldName + " 不能为空"
            );
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String newRuntimeBindingId() {
        return "rwb-" + UUID.randomUUID().toString().replace("-", "");
    }

    private static final class RuntimeTemplateMatch {
        private final RuntimeRewriteBinding binding;
        private final RuntimeSqlRewriteTemplateResult templateResult;
        private final boolean ambiguous;

        private RuntimeTemplateMatch(RuntimeRewriteBinding binding,
                                     RuntimeSqlRewriteTemplateResult templateResult,
                                     boolean ambiguous) {
            this.binding = binding;
            this.templateResult = templateResult;
            this.ambiguous = ambiguous;
        }

        static RuntimeTemplateMatch one(RuntimeRewriteBinding binding,
                                        RuntimeSqlRewriteTemplateResult templateResult) {
            return new RuntimeTemplateMatch(binding, templateResult, false);
        }

        static RuntimeTemplateMatch none() {
            return new RuntimeTemplateMatch(null, null, false);
        }

        static RuntimeTemplateMatch ambiguous() {
            return new RuntimeTemplateMatch(null, null, true);
        }

        RuntimeRewriteBinding getBinding() { return binding; }
        RuntimeSqlRewriteTemplateResult getTemplateResult() { return templateResult; }
        boolean isAmbiguous() { return ambiguous; }
    }
}
