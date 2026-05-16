package com.company.queryexecution.application.service;

import com.company.queryexecution.domain.query.ApprovedAccelerationBinding;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanApplyRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanRollbackRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanVerifyRequest;
import com.company.sqlforge.common.utils.JsonUtils;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class QueryExecutionAccelerationRuntimeService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "APPROVED_ACCELERATION_RUNTIME_BASELINE";
    private static final String TARGET_ENGINE = "HETU";

    private final Map<String, ApprovedAccelerationBinding> bindings = new ConcurrentHashMap<String, ApprovedAccelerationBinding>();

    public QueryExecutionAccelerationPlanResponse apply(QueryExecutionAccelerationPlanApplyRequest request) {
        requireProtectedTenant(request == null ? null : request.getTenantId());
        String planId = requireText(request == null ? null : request.getPlanId(), "planId");
        String sqlFingerprint = requireText(request == null ? null : request.getSqlFingerprint(), "sqlFingerprint");
        String bindingKey = bindingKey(request.getTenantId(), sqlFingerprint);
        ApprovedAccelerationBinding existing = bindings.get(bindingKey);
        if (existing != null && !planId.equals(existing.getPlanId())) {
            throw new BizException(
                ErrorCodeConstants.QUERY_EXECUTION_ROUTE_REJECTED,
                HttpStatus.CONFLICT,
                "该租户和 SQL 指纹已有已批准的加速方案在运行中"
            );
        }
        ApprovedAccelerationBinding binding = new ApprovedAccelerationBinding(
            request.getTenantId(),
            planId,
            sqlFingerprint,
            requireText(request.getDatasourceType(), "datasourceType"),
            request.getSelectedSuggestionTypes(),
            request.getPlanSummary(),
            request.getPrimaryRecommendation(),
            Instant.now()
        );
        bindings.put(bindingKey, binding);
        return responseFrom(
            binding,
            "APPLIED",
            true,
            "已批准加速方案已在运行时偏好门控中生效。"
        );
    }

    public QueryExecutionAccelerationPlanResponse verify(QueryExecutionAccelerationPlanVerifyRequest request) {
        requireProtectedTenant(request == null ? null : request.getTenantId());
        String planId = requireText(request == null ? null : request.getPlanId(), "planId");
        String sqlFingerprint = requireText(request == null ? null : request.getSqlFingerprint(), "sqlFingerprint");
        ApprovedAccelerationBinding binding = bindings.get(bindingKey(request.getTenantId(), sqlFingerprint));
        if (binding == null || !planId.equals(binding.getPlanId())) {
            QueryExecutionAccelerationPlanResponse response = new QueryExecutionAccelerationPlanResponse();
            response.setTenantId(request.getTenantId());
            response.setPlanId(planId);
            response.setSqlFingerprint(sqlFingerprint);
            response.setTargetEngine(TARGET_ENGINE);
            response.setActive(false);
            response.setStatus("MISSING");
            response.setRuntimeSummary("当前没有可用于运行时校验的已批准加速绑定。");
            response.setRuntimeDetailsJson(JsonUtils.toJson(details("bindingState", "MISSING", "verified", Boolean.FALSE)));
            response.setContractStage(CONTRACT_STAGE);
            response.setImplementationStage(IMPLEMENTATION_STAGE);
            return response;
        }
        return responseFrom(
            binding,
            "VERIFIED",
            true,
            "已批准加速方案仍在运行时注册表中保持生效。"
        );
    }

    public QueryExecutionAccelerationPlanResponse rollback(QueryExecutionAccelerationPlanRollbackRequest request) {
        requireProtectedTenant(request == null ? null : request.getTenantId());
        String planId = requireText(request == null ? null : request.getPlanId(), "planId");
        String sqlFingerprint = requireText(request == null ? null : request.getSqlFingerprint(), "sqlFingerprint");
        ApprovedAccelerationBinding binding = bindings.remove(bindingKey(request.getTenantId(), sqlFingerprint));
        QueryExecutionAccelerationPlanResponse response = new QueryExecutionAccelerationPlanResponse();
        response.setTenantId(request.getTenantId());
        response.setPlanId(planId);
        response.setSqlFingerprint(sqlFingerprint);
        response.setTargetEngine(TARGET_ENGINE);
        response.setActive(false);
        response.setStatus("ROLLED_BACK");
        response.setRuntimeSummary(binding == null
            ? "当前没有残留的活跃加速绑定，回滚已按幂等方式完成。"
            : "已从运行时注册表移除已批准加速绑定。");
        response.setRuntimeDetailsJson(
            JsonUtils.toJson(details("bindingState", binding == null ? "ABSENT" : "REMOVED", "verified", Boolean.TRUE))
        );
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        return response;
    }

    public ApprovedAccelerationBinding resolveActiveBinding(String tenantId,
                                                            String sqlFingerprint,
                                                            String datasourceType) {
        if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(sqlFingerprint)) {
            return null;
        }
        ApprovedAccelerationBinding binding = bindings.get(bindingKey(tenantId, sqlFingerprint));
        if (binding == null) {
            return null;
        }
        if (!StringUtils.hasText(datasourceType)) {
            return binding;
        }
        return datasourceType.trim().equalsIgnoreCase(binding.getDatasourceType()) ? binding : null;
    }

    private QueryExecutionAccelerationPlanResponse responseFrom(ApprovedAccelerationBinding binding,
                                                                String status,
                                                                boolean active,
                                                                String summary) {
        QueryExecutionAccelerationPlanResponse response = new QueryExecutionAccelerationPlanResponse();
        response.setTenantId(binding.getTenantId());
        response.setPlanId(binding.getPlanId());
        response.setSqlFingerprint(binding.getSqlFingerprint());
        response.setTargetEngine(TARGET_ENGINE);
        response.setActive(active);
        response.setStatus(status);
        response.setRuntimeSummary(summary);
        response.setRuntimeDetailsJson(
            JsonUtils.toJson(
                details(
                    "bindingState",
                    active ? "ACTIVE" : "INACTIVE",
                    "selectedSuggestionTypes",
                    binding.getSelectedSuggestionTypes(),
                    "appliedAt",
                    binding.getAppliedAt().toString()
                )
            )
        );
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        return response;
    }

    private Map<String, Object> details(Object... keyValues) {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        for (int i = 0; i < keyValues.length; i += 2) {
            details.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return details;
    }

    private String bindingKey(String tenantId, String sqlFingerprint) {
        return tenantId.trim() + "::" + sqlFingerprint.trim();
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
}
