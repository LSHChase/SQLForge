package com.company.governance.application.service;

import com.company.governance.application.controller.dto.ReportInterfaceConfigUpsertRequest;
import com.company.governance.domain.reportinterface.ReportInterfaceConfig;
import com.company.governance.domain.reportinterface.repository.ReportInterfaceConfigRepository;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigRequest;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReportInterfaceConfigApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "REPORT_INTERFACE_CONFIG_BASELINE";
    private static final int DEFAULT_TIMEOUT_MS = 3000;

    private final ReportInterfaceConfigRepository reportInterfaceConfigRepository;

    public ReportInterfaceConfigApplicationService(ReportInterfaceConfigRepository reportInterfaceConfigRepository) {
        this.reportInterfaceConfigRepository = reportInterfaceConfigRepository;
    }

    public GovernanceReportInterfaceConfigResponse upsert(ReportInterfaceConfigUpsertRequest request) {
        return save(null, request);
    }

    public GovernanceReportInterfaceConfigResponse update(String interfaceId, ReportInterfaceConfigUpsertRequest request) {
        return save(requireText(interfaceId, "interfaceId"), request);
    }

    public List<GovernanceReportInterfaceConfigResponse> list(String tenantId) {
        String normalizedTenant = requireTenant(tenantId);
        List<ReportInterfaceConfig> configs = reportInterfaceConfigRepository.findByTenantId(normalizedTenant);
        List<GovernanceReportInterfaceConfigResponse> responses = new ArrayList<GovernanceReportInterfaceConfigResponse>(configs.size());
        for (ReportInterfaceConfig config : configs) {
            responses.add(toResponse(config, config.isEnabled() ? "ACTIVE" : "DISABLED", null));
        }
        return responses;
    }

    public GovernanceReportInterfaceConfigResponse resolve(GovernanceReportInterfaceConfigRequest request) {
        String tenantId = requireTenant(request == null ? null : request.getTenantId());
        ReportInterfaceConfig config = reportInterfaceConfigRepository
            .findBestMatch(tenantId, trimToNull(request == null ? null : request.getDatasourceCode()), trimToNull(request == null ? null : request.getStage()))
            .orElse(null);
        if (config == null) {
            GovernanceReportInterfaceConfigResponse response = new GovernanceReportInterfaceConfigResponse();
            response.setTenantId(tenantId);
            response.setDatasourceCode(request == null ? null : request.getDatasourceCode());
            response.setStage(request == null ? null : request.getStage());
            response.setSourceType("TXT_MOCK_SOURCE");
            response.setEnabled(Boolean.FALSE);
            response.setResolverStatus("MOCK_FALLBACK");
            response.setUnavailableReason("REPORT_INTERFACE_CONFIG_NOT_FOUND");
            response.setContractStage(CONTRACT_STAGE);
            response.setImplementationStage(IMPLEMENTATION_STAGE);
            return response;
        }
        return toResponse(config, config.isEnabled() ? "ACTIVE" : "DISABLED",
            config.isEnabled() ? null : "REPORT_INTERFACE_CONFIG_DISABLED");
    }

    private GovernanceReportInterfaceConfigResponse save(String interfaceId, ReportInterfaceConfigUpsertRequest request) {
        String tenantId = requireTenant(request == null ? null : request.getTenantId());
        String configId = interfaceId;
        if (StringUtils.hasText(interfaceId)) {
            reportInterfaceConfigRepository.findByTenantIdAndConfigId(tenantId, interfaceId).orElseThrow(() -> new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "报表接口配置不存在：" + interfaceId
            ));
        } else {
            configId = UUID.randomUUID().toString();
        }
        ReportInterfaceConfig config = new ReportInterfaceConfig(
            configId,
            tenantId,
            trimToNull(request == null ? null : request.getDatasourceCode()),
            trimToNull(request == null ? null : request.getStage()),
            firstNonBlank(request == null ? null : request.getSourceType(), "HTTP_API").toUpperCase(Locale.ROOT),
            requireText(request == null ? null : request.getEndpointCode(), "endpointCode"),
            firstNonBlank(request == null ? null : request.getEndpointName(), request == null ? null : request.getEndpointCode()),
            trimToNull(request == null ? null : request.getBaseUrl()),
            firstNonBlank(request == null ? null : request.getPathTemplate(), "/reports/sql"),
            firstNonBlank(request == null ? null : request.getHttpMethod(), "GET").toUpperCase(Locale.ROOT),
            firstNonBlank(request == null ? null : request.getReportCodeParamName(), "report_code"),
            firstNonBlank(request == null ? null : request.getSqlJsonPath(), "$.sql"),
            firstNonBlank(request == null ? null : request.getAuthMode(), "NONE"),
            normalizeTimeout(request == null ? null : request.getTimeoutMs()),
            request == null || request.getEnabled() == null || request.getEnabled().booleanValue(),
            Instant.now()
        );
        validateConfig(config);
        reportInterfaceConfigRepository.save(config);
        return toResponse(config, "ACTIVE", null);
    }

    private void validateConfig(ReportInterfaceConfig config) {
        if ("HTTP_API".equals(config.getSourceType()) && !StringUtils.hasText(config.getBaseUrl())) {
            throw invalidArgument("baseUrl", "HTTP_API 报表接口配置必须提供 baseUrl");
        }
        if (!"GET".equals(config.getHttpMethod()) && !"POST".equals(config.getHttpMethod())) {
            throw invalidArgument("httpMethod", "httpMethod 必须为 GET or POST");
        }
    }

    private GovernanceReportInterfaceConfigResponse toResponse(ReportInterfaceConfig config,
                                                               String resolverStatus,
                                                               String unavailableReason) {
        GovernanceReportInterfaceConfigResponse response = new GovernanceReportInterfaceConfigResponse();
        response.setConfigId(config.getConfigId());
        response.setTenantId(config.getTenantId());
        response.setDatasourceCode(config.getDatasourceCode());
        response.setStage(config.getStage());
        response.setSourceType(config.getSourceType());
        response.setEndpointCode(config.getEndpointCode());
        response.setEndpointName(config.getEndpointName());
        response.setBaseUrl(config.getBaseUrl());
        response.setPathTemplate(config.getPathTemplate());
        response.setHttpMethod(config.getHttpMethod());
        response.setReportCodeParamName(config.getReportCodeParamName());
        response.setSqlJsonPath(config.getSqlJsonPath());
        response.setAuthMode(config.getAuthMode());
        response.setTimeoutMs(Integer.valueOf(config.getTimeoutMs()));
        response.setEnabled(Boolean.valueOf(config.isEnabled()));
        response.setResolverStatus(resolverStatus);
        response.setUnavailableReason(unavailableReason);
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        return response;
    }

    private String requireTenant(String requestTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(ErrorCodeConstants.SYSTEM_CONTEXT_MISSING, HttpStatus.UNAUTHORIZED,
                "已认证请求上下文缺少 tenantId");
        }
        String normalized = trimToNull(requestTenantId);
        if (StringUtils.hasText(normalized) && !contextTenantId.equals(normalized)) {
            throw new AccessDeniedException("请求 tenantId 与已认证租户上下文不一致");
        }
        return contextTenantId;
    }

    private int normalizeTimeout(Integer timeoutMs) {
        if (timeoutMs == null || timeoutMs.intValue() <= 0) {
            return DEFAULT_TIMEOUT_MS;
        }
        return timeoutMs.intValue();
    }

    private String requireText(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            throw invalidArgument(fieldName, fieldName + " 为必填项");
        }
        return normalized;
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
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private BizException invalidArgument(String fieldName, String message) {
        return new BizException(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, HttpStatus.BAD_REQUEST, message + " [" + fieldName + "]");
    }
}
