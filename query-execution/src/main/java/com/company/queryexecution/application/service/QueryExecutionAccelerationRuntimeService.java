package com.company.queryexecution.application.service;

import com.company.queryexecution.domain.query.ActivatedAccelerationBinding;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanActivationRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanPauseRequest;
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
    private static final String IMPLEMENTATION_STAGE = "ACCELERATION_RUNTIME_ACTIVATION_BASELINE";
    private static final String TARGET_ENGINE = "HETU";

    private final Map<String, ActivatedAccelerationBinding> bindings = new ConcurrentHashMap<String, ActivatedAccelerationBinding>();

    public QueryExecutionAccelerationPlanResponse activate(QueryExecutionAccelerationPlanActivationRequest request) {
        requireProtectedTenant(request == null ? null : request.getTenantId());
        String planId = requireText(request == null ? null : request.getPlanId(), "planId");
        String sqlFingerprint = requireText(request == null ? null : request.getSqlFingerprint(), "sqlFingerprint");
        String bindingKey = bindingKey(request.getTenantId(), sqlFingerprint);
        ActivatedAccelerationBinding existing = bindings.get(bindingKey);
        if (existing != null && !planId.equals(existing.getPlanId())) {
            throw new BizException(
                ErrorCodeConstants.QUERY_EXECUTION_ROUTE_REJECTED,
                HttpStatus.CONFLICT,
                "该租户和 SQL 指纹已有已激活的加速方案在运行中"
            );
        }
        ActivatedAccelerationBinding binding = new ActivatedAccelerationBinding(
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
            "ACTIVE",
            true,
            "加速方案已在运行时偏好门控中激活。"
        );
    }

    public QueryExecutionAccelerationPlanResponse pause(QueryExecutionAccelerationPlanPauseRequest request) {
        requireProtectedTenant(request == null ? null : request.getTenantId());
        String planId = requireText(request == null ? null : request.getPlanId(), "planId");
        String sqlFingerprint = requireText(request == null ? null : request.getSqlFingerprint(), "sqlFingerprint");
        ActivatedAccelerationBinding binding = bindings.remove(bindingKey(request.getTenantId(), sqlFingerprint));
        QueryExecutionAccelerationPlanResponse response = new QueryExecutionAccelerationPlanResponse();
        response.setTenantId(request.getTenantId());
        response.setPlanId(planId);
        response.setSqlFingerprint(sqlFingerprint);
        response.setTargetEngine(TARGET_ENGINE);
        response.setActive(false);
        response.setStatus("PAUSED");
        response.setRuntimeSummary(binding == null
            ? "当前没有残留的活跃加速绑定，暂停已按幂等方式完成。"
            : "已从运行时注册表暂停加速绑定。");
        response.setRuntimeDetailsJson(
            JsonUtils.toJson(details("bindingState", binding == null ? "ABSENT" : "PAUSED", "pauseConfirmed", Boolean.TRUE))
        );
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        return response;
    }

    public ActivatedAccelerationBinding resolveActiveBinding(String tenantId,
                                                            String sqlFingerprint,
                                                            String datasourceType) {
        if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(sqlFingerprint)) {
            return null;
        }
        ActivatedAccelerationBinding binding = bindings.get(bindingKey(tenantId, sqlFingerprint));
        if (binding == null) {
            return null;
        }
        if (!StringUtils.hasText(datasourceType)) {
            return binding;
        }
        return datasourceType.trim().equalsIgnoreCase(binding.getDatasourceType()) ? binding : null;
    }

    private QueryExecutionAccelerationPlanResponse responseFrom(ActivatedAccelerationBinding binding,
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
                    "activatedAt",
                    binding.getActivatedAt().toString()
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
