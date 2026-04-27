package com.company.governance.application.service;

import com.company.governance.application.controller.dto.DispatchPolicyUpsertRequest;
import com.company.governance.application.controller.vo.DispatchPolicyVO;
import com.company.governance.domain.dispatchpolicy.DispatchPolicyConfig;
import com.company.governance.domain.dispatchpolicy.repository.DispatchPolicyRepository;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DispatchPolicyApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "DISPATCH_POLICY_BASELINE";

    private final DispatchPolicyRepository dispatchPolicyRepository;

    public DispatchPolicyApplicationService(DispatchPolicyRepository dispatchPolicyRepository) {
        this.dispatchPolicyRepository = dispatchPolicyRepository;
    }

    public DispatchPolicyVO create(DispatchPolicyUpsertRequest request) {
        String tenantId = requireTenant(request == null ? null : request.getTenantId());
        DispatchPolicyConfig config = new DispatchPolicyConfig(
            UUID.randomUUID().toString(),
            tenantId,
            firstNonBlank(request == null ? null : request.getPolicyName(), "dispatch-policy"),
            firstNonBlank(request == null ? null : request.getDispatchType(), "PULL_ONLY").toUpperCase(Locale.ROOT),
            trimToNull(request == null ? null : request.getTargetEngine()),
            trimToNull(request == null ? null : request.getTargetDatasource()),
            firstNonBlank(request == null ? null : request.getAckMode(), "MANUAL").toUpperCase(Locale.ROOT),
            normalizePositive(request == null ? null : request.getPullWindowSeconds(), 60),
            normalizePositive(request == null ? null : request.getMaxBatchSize(), 100),
            firstNonBlank(request == null ? null : request.getRetryStrategy(), "MANUAL_RETRY").toUpperCase(Locale.ROOT),
            request == null || request.getEnabled() == null || request.getEnabled().booleanValue(),
            Instant.now()
        );
        dispatchPolicyRepository.save(config);
        return toVo(config);
    }

    public List<DispatchPolicyVO> list(String tenantId) {
        String effectiveTenantId = requireTenant(tenantId);
        List<DispatchPolicyConfig> configs = dispatchPolicyRepository.findByTenantId(effectiveTenantId);
        List<DispatchPolicyVO> responses = new ArrayList<DispatchPolicyVO>(configs.size());
        for (DispatchPolicyConfig config : configs) {
            responses.add(toVo(config));
        }
        return responses;
    }

    private DispatchPolicyVO toVo(DispatchPolicyConfig config) {
        DispatchPolicyVO response = new DispatchPolicyVO();
        response.setPolicyId(config.getPolicyId());
        response.setTenantId(config.getTenantId());
        response.setPolicyName(config.getPolicyName());
        response.setDispatchType(config.getDispatchType());
        response.setTargetEngine(config.getTargetEngine());
        response.setTargetDatasource(config.getTargetDatasource());
        response.setAckMode(config.getAckMode());
        response.setPullWindowSeconds(Integer.valueOf(config.getPullWindowSeconds()));
        response.setMaxBatchSize(Integer.valueOf(config.getMaxBatchSize()));
        response.setRetryStrategy(config.getRetryStrategy());
        response.setEnabled(config.isEnabled());
        response.setEnforcementStatus(config.isEnabled() ? "SIMULATED_ACTIVE" : "DISABLED");
        response.setExecutionBoundary("EXTERNAL_MODULE_REQUIRED");
        response.setUpdatedAt(config.getUpdatedAt());
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        return response;
    }

    private String requireTenant(String requestTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(ErrorCodeConstants.SYSTEM_CONTEXT_MISSING, HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context");
        }
        String normalized = trimToNull(requestTenantId);
        if (StringUtils.hasText(normalized) && !contextTenantId.equals(normalized)) {
            throw new AccessDeniedException("Request tenantId does not match authenticated tenant context");
        }
        return contextTenantId;
    }

    private int normalizePositive(Integer value, int defaultValue) {
        return value == null || value.intValue() <= 0 ? defaultValue : value.intValue();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (StringUtils.hasText(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
