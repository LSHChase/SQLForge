package com.company.governance.application.service;

import com.company.governance.application.controller.dto.DatasourceConfigUpsertRequest;
import com.company.governance.application.controller.dto.DatasourceConnectionTestRequest;
import com.company.governance.application.controller.vo.DatasourceConfigVO;
import com.company.governance.application.controller.vo.DatasourceConnectionTestVO;
import com.company.governance.domain.datasource.DatasourceConfig;
import com.company.governance.domain.datasource.repository.DatasourceConfigRepository;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DatasourceConfigApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "DATASOURCE_CONFIG_BASELINE";
    private static final String HEALTHCHECK_IMPLEMENTATION_STAGE = "DATASOURCE_HEALTHCHECK_BASELINE";
    private static final int DEFAULT_TIMEOUT_MS = 3000;
    private static final List<String> SUPPORTED_CONNECTION_MODES = Arrays.asList("JDBC", "API", "CLIENT", "GATEWAY", "PROXY");

    private final DatasourceConfigRepository datasourceConfigRepository;

    public DatasourceConfigApplicationService(DatasourceConfigRepository datasourceConfigRepository) {
        this.datasourceConfigRepository = datasourceConfigRepository;
    }

    public DatasourceConfigVO create(DatasourceConfigUpsertRequest request) {
        return upsert(UUID.randomUUID().toString(), request, false);
    }

    public DatasourceConfigVO update(String datasourceId, DatasourceConfigUpsertRequest request) {
        return upsert(requireText(datasourceId, "datasourceId"), request, true);
    }

    public List<DatasourceConfigVO> list(String tenantId) {
        String effectiveTenantId = requireTenant(tenantId);
        List<DatasourceConfig> configs = datasourceConfigRepository.findByTenantId(effectiveTenantId);
        List<DatasourceConfigVO> responses = new ArrayList<DatasourceConfigVO>(configs.size());
        for (DatasourceConfig config : configs) {
            responses.add(toConfigVO(config));
        }
        return responses;
    }

    public DatasourceConfigVO find(String tenantId, String datasourceId) {
        return toConfigVO(loadConfig(requireTenant(tenantId), requireText(datasourceId, "datasourceId")));
    }

    public DatasourceConnectionTestVO testConnection(String datasourceId, DatasourceConnectionTestRequest request) {
        String tenantId = requireTenant(request == null ? null : request.getTenantId());
        DatasourceConfig current = loadConfig(tenantId, requireText(datasourceId, "datasourceId"));
        Instant checkedAt = Instant.now();
        int timeoutMs = request != null && request.getTimeoutMsOverride() != null && request.getTimeoutMsOverride().intValue() > 0
            ? request.getTimeoutMsOverride().intValue()
            : current.getTimeoutMs();

        String connectionStatus;
        String healthStatus;
        String failureReason = null;
        if (!current.isEnabled()) {
            connectionStatus = "UNAVAILABLE";
            healthStatus = "DISABLED";
            failureReason = "DATASOURCE_DISABLED";
        } else {
            failureReason = evaluateFailureReason(current);
            if (failureReason == null) {
                connectionStatus = "CONNECTED";
                healthStatus = "HEALTHY";
            } else {
                connectionStatus = "FAILED";
                healthStatus = "DEGRADED";
            }
        }

        DatasourceConfig refreshed = datasourceConfigRepository.save(current.withHealthStatus(healthStatus, failureReason, checkedAt));
        DatasourceConnectionTestVO response = new DatasourceConnectionTestVO();
        response.setTenantId(refreshed.getTenantId());
        response.setDatasourceId(refreshed.getDatasourceId());
        response.setConnectionStatus(connectionStatus);
        response.setHealthStatus(refreshed.getHealthStatus());
        response.setLastFailureReason(refreshed.getLastFailureReason());
        response.setCheckedAt(refreshed.getLastCheckedAt());
        response.setTimeoutMs(Integer.valueOf(timeoutMs));
        response.setReadonlyBoundary(refreshed.isReadonly() ? "READONLY_ONLY" : "UNSPECIFIED");
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(HEALTHCHECK_IMPLEMENTATION_STAGE);
        return response;
    }

    private DatasourceConfigVO upsert(String datasourceId, DatasourceConfigUpsertRequest request, boolean update) {
        String tenantId = requireTenant(request == null ? null : request.getTenantId());
        String connectionMode = normalizeConnectionMode(request == null ? null : request.getConnectionMode());
        validateConnectionFields(connectionMode, request);
        if (update) {
            loadConfig(tenantId, datasourceId);
        }
        DatasourceConfig config = new DatasourceConfig(
            datasourceId,
            tenantId,
            requireText(request == null ? null : request.getDatasourceCode(), "datasourceCode"),
            firstNonBlank(request == null ? null : request.getDatasourceName(), request == null ? null : request.getDatasourceCode()),
            connectionMode,
            firstNonBlank(request == null ? null : request.getStage(), "PROD").toUpperCase(Locale.ROOT),
            trimToNull(request == null ? null : request.getJdbcUrl()),
            trimToNull(request == null ? null : request.getApiBaseUrl()),
            trimToNull(request == null ? null : request.getClientEndpoint()),
            trimToNull(request == null ? null : request.getGatewayEndpoint()),
            trimToNull(request == null ? null : request.getProxyEndpoint()),
            firstNonBlank(request == null ? null : request.getAuthMode(), "NONE").toUpperCase(Locale.ROOT),
            firstNonBlank(request == null ? null : request.getCredentialMode(), "NONE").toUpperCase(Locale.ROOT),
            trimToNull(request == null ? null : request.getCredentialRef()),
            maskSecret(request == null ? null : request.getCredentialSecret()),
            request != null && Boolean.TRUE.equals(request.getTlsEnabled()),
            request == null || request.getVerifyPeer() == null || request.getVerifyPeer().booleanValue(),
            request == null || request.getReadonly() == null || request.getReadonly().booleanValue(),
            request == null || request.getEnabled() == null || request.getEnabled().booleanValue(),
            normalizeTimeout(request == null ? null : request.getTimeoutMs()),
            "UNKNOWN",
            null,
            null,
            Instant.now()
        );
        datasourceConfigRepository.save(config);
        return toConfigVO(config);
    }

    private void validateConnectionFields(String connectionMode, DatasourceConfigUpsertRequest request) {
        if ("JDBC".equals(connectionMode) && !StringUtils.hasText(request.getJdbcUrl())) {
            throw invalidArgument("jdbcUrl", "jdbcUrl is required for JDBC datasource config");
        }
        if ("API".equals(connectionMode) && !StringUtils.hasText(request.getApiBaseUrl())) {
            throw invalidArgument("apiBaseUrl", "apiBaseUrl is required for API datasource config");
        }
        if ("CLIENT".equals(connectionMode) && !StringUtils.hasText(request.getClientEndpoint())) {
            throw invalidArgument("clientEndpoint", "clientEndpoint is required for CLIENT datasource config");
        }
        if ("GATEWAY".equals(connectionMode) && !StringUtils.hasText(request.getGatewayEndpoint())) {
            throw invalidArgument("gatewayEndpoint", "gatewayEndpoint is required for GATEWAY datasource config");
        }
        if ("PROXY".equals(connectionMode) && !StringUtils.hasText(request.getProxyEndpoint())) {
            throw invalidArgument("proxyEndpoint", "proxyEndpoint is required for PROXY datasource config");
        }
    }

    private String evaluateFailureReason(DatasourceConfig config) {
        String endpoint = resolveEndpoint(config);
        if (!StringUtils.hasText(endpoint)) {
            return "CONNECTION_ENDPOINT_MISSING";
        }
        String normalized = endpoint.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("unreachable") || normalized.contains("invalid")) {
            return "CONNECTION_ENDPOINT_UNREACHABLE";
        }
        if (!"NONE".equalsIgnoreCase(config.getCredentialMode()) && !StringUtils.hasText(config.getCredentialRef())
            && !StringUtils.hasText(config.getCredentialMask())) {
            return "CREDENTIAL_REFERENCE_MISSING";
        }
        return null;
    }

    private String resolveEndpoint(DatasourceConfig config) {
        if ("JDBC".equals(config.getConnectionMode())) {
            return config.getJdbcUrl();
        }
        if ("API".equals(config.getConnectionMode())) {
            return config.getApiBaseUrl();
        }
        if ("CLIENT".equals(config.getConnectionMode())) {
            return config.getClientEndpoint();
        }
        if ("GATEWAY".equals(config.getConnectionMode())) {
            return config.getGatewayEndpoint();
        }
        return config.getProxyEndpoint();
    }

    private DatasourceConfig loadConfig(String tenantId, String datasourceId) {
        return datasourceConfigRepository.findByTenantIdAndDatasourceId(tenantId, datasourceId)
            .orElseThrow(() -> new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Datasource config not found: " + datasourceId
            ));
    }

    private DatasourceConfigVO toConfigVO(DatasourceConfig config) {
        DatasourceConfigVO response = new DatasourceConfigVO();
        response.setDatasourceId(config.getDatasourceId());
        response.setTenantId(config.getTenantId());
        response.setDatasourceCode(config.getDatasourceCode());
        response.setDatasourceName(config.getDatasourceName());
        response.setConnectionMode(config.getConnectionMode());
        response.setStage(config.getStage());
        response.setJdbcUrl(config.getJdbcUrl());
        response.setApiBaseUrl(config.getApiBaseUrl());
        response.setClientEndpoint(config.getClientEndpoint());
        response.setGatewayEndpoint(config.getGatewayEndpoint());
        response.setProxyEndpoint(config.getProxyEndpoint());
        response.setAuthMode(config.getAuthMode());
        response.setCredentialMode(config.getCredentialMode());
        response.setCredentialRef(config.getCredentialRef());
        response.setCredentialMask(config.getCredentialMask());
        response.setTlsEnabled(config.isTlsEnabled());
        response.setVerifyPeer(config.isVerifyPeer());
        response.setReadonly(config.isReadonly());
        response.setEnabled(config.isEnabled());
        response.setTimeoutMs(Integer.valueOf(config.getTimeoutMs()));
        response.setHealthStatus(config.getHealthStatus());
        response.setLastFailureReason(config.getLastFailureReason());
        response.setLastCheckedAt(config.getLastCheckedAt());
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

    private String normalizeConnectionMode(String value) {
        String normalized = requireText(value, "connectionMode").toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CONNECTION_MODES.contains(normalized)) {
            throw invalidArgument("connectionMode", "connectionMode must be JDBC/API/CLIENT/GATEWAY/PROXY");
        }
        return normalized;
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
            throw invalidArgument(fieldName, fieldName + " is required");
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

    private String maskSecret(String secret) {
        String normalized = trimToNull(secret);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.length() <= 4) {
            return "****";
        }
        return "****" + normalized.substring(normalized.length() - 4);
    }

    private BizException invalidArgument(String fieldName, String message) {
        return new BizException(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, HttpStatus.BAD_REQUEST, message + " [" + fieldName + "]");
    }
}
