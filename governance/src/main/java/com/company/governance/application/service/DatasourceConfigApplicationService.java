package com.company.governance.application.service;

import com.company.governance.application.controller.dto.DatasourceConfigUpsertRequest;
import com.company.governance.application.controller.dto.DatasourceConnectionTestRequest;
import com.company.governance.application.controller.vo.DatasourceConfigVO;
import com.company.governance.application.controller.vo.DatasourceConnectionTestVO;
import com.company.governance.domain.datasource.DatasourceConfig;
import com.company.governance.domain.datasource.JdbcDriverArtifact;
import com.company.governance.domain.datasource.repository.DatasourceConfigRepository;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveResponse;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteCandidate;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveResponse;
import com.company.sqlforge.common.security.SensitiveDataCryptoService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DatasourceConfigApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "DATASOURCE_CONFIG_BASELINE";
    private static final String HEALTHCHECK_IMPLEMENTATION_STAGE = "DATASOURCE_HEALTHCHECK_BASELINE";
    private static final String JDBC_RESOLVE_IMPLEMENTATION_STAGE = "HETU_JDBC_DATASOURCE_RESOLVE_BASELINE";
    private static final String JDBC_ROUTE_IMPLEMENTATION_STAGE = "MULTI_ENGINE_JDBC_ROUTE_BASELINE";
    private static final int DEFAULT_TIMEOUT_MS = 3000;
    private static final String DEFAULT_ENGINE_TYPE = "HETU";
    private static final String DEFAULT_DRIVER_SOURCE_TYPE = "CLASSPATH";
    private static final String SYSTEM_TENANT_ID = "system";
    private static final List<String> SUPPORTED_CONNECTION_MODES = Arrays.asList("JDBC", "API", "REST", "CLIENT", "GATEWAY", "PROXY");
    private static final List<String> ROUTE_ORDER = Arrays.asList("TRINO", "HETU", "HIVE");

    private final DatasourceConfigRepository datasourceConfigRepository;
    private final SensitiveDataCryptoService sensitiveDataCryptoService;
    private final DatasourceJdbcConnectionProbe jdbcConnectionProbe;
    private final JdbcDriverArtifactApplicationService jdbcDriverArtifactApplicationService;

    @Autowired
    public DatasourceConfigApplicationService(DatasourceConfigRepository datasourceConfigRepository,
                                              SensitiveDataCryptoService sensitiveDataCryptoService,
                                              DatasourceJdbcConnectionProbe jdbcConnectionProbe,
                                              JdbcDriverArtifactApplicationService jdbcDriverArtifactApplicationService) {
        this.datasourceConfigRepository = datasourceConfigRepository;
        this.sensitiveDataCryptoService = sensitiveDataCryptoService;
        this.jdbcConnectionProbe = jdbcConnectionProbe;
        this.jdbcDriverArtifactApplicationService = jdbcDriverArtifactApplicationService;
    }

    public DatasourceConfigApplicationService(DatasourceConfigRepository datasourceConfigRepository,
                                              SensitiveDataCryptoService sensitiveDataCryptoService,
                                              DatasourceJdbcConnectionProbe jdbcConnectionProbe) {
        this(datasourceConfigRepository, sensitiveDataCryptoService, jdbcConnectionProbe, null);
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
        long start = System.currentTimeMillis();
        int timeoutMs = request != null && request.getTimeoutMsOverride() != null && request.getTimeoutMsOverride().intValue() > 0
            ? request.getTimeoutMsOverride().intValue()
            : current.getTimeoutMs();

        String connectionStatus;
        String healthStatus;
        String failureReason = null;
        boolean realJdbcProbe = false;
        long elapsedMs;
        if (!current.isEnabled()) {
            connectionStatus = "UNAVAILABLE";
            healthStatus = "DISABLED";
            failureReason = "DATASOURCE_DISABLED";
            elapsedMs = System.currentTimeMillis() - start;
        } else if ("JDBC".equals(current.getConnectionMode())) {
            realJdbcProbe = true;
            DatasourceJdbcConnectionProbe.JdbcProbeResult probeResult =
                jdbcConnectionProbe.probe(current, decryptCredential(current), timeoutMs);
            elapsedMs = probeResult.getElapsedMs();
            if (probeResult.isConnected()) {
                connectionStatus = "CONNECTED";
                healthStatus = "HEALTHY";
            } else {
                connectionStatus = "FAILED";
                healthStatus = "DEGRADED";
                failureReason = probeResult.getFailureReason();
            }
        } else {
            failureReason = evaluateFailureReason(current);
            elapsedMs = System.currentTimeMillis() - start;
            if (failureReason == null) {
                connectionStatus = "CONNECTED";
                healthStatus = "HEALTHY";
            } else {
                connectionStatus = "FAILED";
                healthStatus = "DEGRADED";
            }
        }

        DatasourceConfig refreshed = datasourceConfigRepository.save(current.withHealthStatus(
            healthStatus,
            failureReason,
            checkedAt,
            Long.valueOf(elapsedMs)
        ));
        DatasourceConnectionTestVO response = new DatasourceConnectionTestVO();
        response.setTenantId(refreshed.getTenantId());
        response.setDatasourceId(refreshed.getDatasourceId());
        response.setDatasourceCode(refreshed.getDatasourceCode());
        response.setEngineType(refreshed.getEngineType());
        response.setConnectionStatus(connectionStatus);
        response.setHealthStatus(refreshed.getHealthStatus());
        response.setLastFailureReason(refreshed.getLastFailureReason());
        response.setCheckedAt(refreshed.getLastCheckedAt());
        response.setElapsedMs(Long.valueOf(elapsedMs));
        response.setTimeoutMs(Integer.valueOf(timeoutMs));
        response.setRealJdbcProbe(Boolean.valueOf(realJdbcProbe));
        response.setReadonlyBoundary(refreshed.isReadonly() ? "READONLY_ONLY" : "UNSPECIFIED");
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(HEALTHCHECK_IMPLEMENTATION_STAGE);
        return response;
    }

    public GovernanceJdbcDatasourceResolveResponse resolveJdbcDatasource(GovernanceJdbcDatasourceResolveRequest request) {
        String tenantId = requireTenant(request == null ? null : request.getTenantId());
        String datasourceCode = requireText(request == null ? null : request.getDatasourceCode(), "datasourceCode");
        String engineType = normalizeEngineType(request == null ? null : request.getEngineType());
        DatasourceConfig config = findDatasourceByCodeAndEngine(tenantId, datasourceCode, engineType);
        if (config == null) {
            return unresolved(tenantId, datasourceCode, engineType, "HETU_JDBC_CONFIG_NOT_FOUND");
        }
        if (!"JDBC".equals(config.getConnectionMode())) {
            return unresolved(tenantId, datasourceCode, engineType, "HETU_JDBC_CONFIG_NOT_FOUND");
        }
        if (!config.isEnabled()) {
            return unresolved(tenantId, datasourceCode, engineType, "HETU_JDBC_DATASOURCE_DISABLED");
        }
        if (!StringUtils.hasText(config.getJdbcUrl())) {
            return unresolved(tenantId, datasourceCode, engineType, "HETU_JDBC_URL_MISSING");
        }
        GovernanceJdbcDatasourceResolveResponse response = baseResolveResponse(config.getTenantId(), config.getDatasourceCode(), config.getEngineType());
        response.setResolved(true);
        response.setDatasourceId(config.getDatasourceId());
        response.setConnectionMode(config.getConnectionMode());
        response.setEnabled(config.isEnabled());
        response.setReadonly(config.isReadonly());
        response.setJdbcUrl(config.getJdbcUrl());
        response.setDriverClassName(config.getJdbcDriverClassName());
        response.setDriverSourceType(config.getDriverSourceType());
        response.setDriverArtifactId(config.getDriverArtifactId());
        response.setDriverVersionLabel(config.getDriverVersionLabel());
        response.setDriverSha256(config.getDriverSha256());
        if (StringUtils.hasText(config.getDriverArtifactId())) {
            JdbcDriverArtifact artifact = jdbcDriverArtifactApplicationService.requireArtifact(
                config.getTenantId(),
                config.getDriverArtifactId()
            );
            response.setDriverRelativePath(
                jdbcDriverArtifactApplicationService.resolveArtifactPath(artifact).toString()
            );
        }
        response.setUsername(config.getUsername());
        response.setPassword(decryptCredential(config));
        response.setTimeoutMs(Integer.valueOf(config.getTimeoutMs()));
        response.setCredentialMask(config.getCredentialMask());
        return response;
    }

    public GovernanceJdbcRouteResolveResponse resolveJdbcRoute(GovernanceJdbcRouteResolveRequest request) {
        String tenantId = requireTenant(request == null ? null : request.getTenantId());
        String datasourceCode = requireText(request == null ? null : request.getDatasourceCode(), "datasourceCode");
        String requestedType = request == null || request.getDatasourceType() == null
            ? "AUTO"
            : request.getDatasourceType().name();
        List<GovernanceJdbcRouteCandidate> candidates = new ArrayList<GovernanceJdbcRouteCandidate>();
        for (String engineType : resolveRouteOrder(requestedType)) {
            DatasourceConfig config = findDatasourceByCodeAndEngine(tenantId, datasourceCode, engineType);
            if (config == null || !"JDBC".equals(config.getConnectionMode())) {
                continue;
            }
            GovernanceJdbcRouteCandidate candidate = new GovernanceJdbcRouteCandidate();
            candidate.setEngineType(config.getEngineType());
            candidate.setDatasourceCode(config.getDatasourceCode());
            candidate.setConnectionMode(config.getConnectionMode());
            candidate.setJdbcUrl(config.getJdbcUrl());
            candidate.setDriverClassName(config.getJdbcDriverClassName());
            candidate.setDriverArtifactId(config.getDriverArtifactId());
            candidate.setDriverSha256(config.getDriverSha256());
            candidate.setTimeoutMs(Integer.valueOf(config.getTimeoutMs()));
            candidate.setHealthStatus(config.getHealthStatus());
            candidate.setEnabled(config.isEnabled());
            candidate.setReadonly(config.isReadonly());
            candidates.add(candidate);
        }
        GovernanceJdbcRouteResolveResponse response = new GovernanceJdbcRouteResolveResponse();
        response.setTenantId(tenantId);
        response.setDatasourceCode(datasourceCode);
        response.setRequestedDatasourceType(requestedType);
        response.setCandidates(candidates);
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(JDBC_ROUTE_IMPLEMENTATION_STAGE);
        return response;
    }

    private DatasourceConfigVO upsert(String datasourceId, DatasourceConfigUpsertRequest request, boolean update) {
        String tenantId = requireTenant(request == null ? null : request.getTenantId());
        requireDatasourceAdmin();
        String connectionMode = normalizeConnectionMode(request == null ? null : request.getConnectionMode());
        validateConnectionFields(connectionMode, request);
        DatasourceConfig existing = null;
        if (update) {
            existing = loadConfig(tenantId, datasourceId);
        }
        CredentialEnvelope credentialEnvelope = buildCredentialEnvelope(request, existing);
        DriverBinding driverBinding = resolveDriverBinding(tenantId, request, existing);
        DatasourceConfig config = new DatasourceConfig(
            datasourceId,
            tenantId,
            requireText(request == null ? null : request.getDatasourceCode(), "datasourceCode"),
            firstNonBlank(request == null ? null : request.getDatasourceName(), request == null ? null : request.getDatasourceCode()),
            normalizeEngineType(request == null ? null : request.getEngineType()),
            connectionMode,
            firstNonBlank(request == null ? null : request.getStage(), "PROD").toUpperCase(Locale.ROOT),
            trimToNull(request == null ? null : request.getJdbcUrl()),
            trimToNull(request == null ? null : request.getJdbcDriverClassName()),
            driverBinding.driverSourceType,
            driverBinding.driverArtifactId,
            driverBinding.driverVersionLabel,
            driverBinding.driverSha256,
            driverBinding.driverLoadStatus,
            trimToNull(request == null ? null : request.getUsername()),
            trimToNull(request == null ? null : request.getApiBaseUrl()),
            trimToNull(request == null ? null : request.getClientEndpoint()),
            trimToNull(request == null ? null : request.getGatewayEndpoint()),
            trimToNull(request == null ? null : request.getProxyEndpoint()),
            firstNonBlank(request == null ? null : request.getAuthMode(), "NONE").toUpperCase(Locale.ROOT),
            firstNonBlank(request == null ? null : request.getCredentialMode(), "NONE").toUpperCase(Locale.ROOT),
            trimToNull(request == null ? null : request.getCredentialRef()),
            credentialEnvelope.mask,
            credentialEnvelope.ciphertext,
            credentialEnvelope.algorithm,
            credentialEnvelope.keyId,
            request != null && Boolean.TRUE.equals(request.getTlsEnabled()),
            request == null || request.getVerifyPeer() == null || request.getVerifyPeer().booleanValue(),
            request == null || request.getReadonly() == null || request.getReadonly().booleanValue(),
            request == null || request.getEnabled() == null || request.getEnabled().booleanValue(),
            normalizeTimeout(request == null ? null : request.getTimeoutMs()),
            existing == null ? "UNKNOWN" : firstNonBlank(existing.getHealthStatus(), "UNKNOWN"),
            existing == null ? null : existing.getLastFailureReason(),
            existing == null ? null : existing.getLastCheckedAt(),
            existing == null ? null : existing.getLastCheckElapsedMs(),
            Instant.now()
        );
        datasourceConfigRepository.save(config);
        return toConfigVO(config);
    }

    private void validateConnectionFields(String connectionMode, DatasourceConfigUpsertRequest request) {
        if ("JDBC".equals(connectionMode) && !StringUtils.hasText(request.getJdbcUrl())) {
            throw invalidArgument("jdbcUrl", "JDBC 数据源配置必须提供 jdbcUrl");
        }
        if (("API".equals(connectionMode) || "REST".equals(connectionMode)) && !StringUtils.hasText(request.getApiBaseUrl())) {
            throw invalidArgument("apiBaseUrl", "API/REST 数据源配置必须提供 apiBaseUrl");
        }
        if ("CLIENT".equals(connectionMode) && !StringUtils.hasText(request.getClientEndpoint())) {
            throw invalidArgument("clientEndpoint", "CLIENT 数据源配置必须提供 clientEndpoint");
        }
        if ("GATEWAY".equals(connectionMode) && !StringUtils.hasText(request.getGatewayEndpoint())) {
            throw invalidArgument("gatewayEndpoint", "GATEWAY 数据源配置必须提供 gatewayEndpoint");
        }
        if ("PROXY".equals(connectionMode) && !StringUtils.hasText(request.getProxyEndpoint())) {
            throw invalidArgument("proxyEndpoint", "PROXY 数据源配置必须提供 proxyEndpoint");
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
        if ("API".equals(config.getConnectionMode()) || "REST".equals(config.getConnectionMode())) {
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
                "数据源配置不存在：" + datasourceId
            ));
    }

    private DatasourceConfigVO toConfigVO(DatasourceConfig config) {
        DatasourceConfigVO response = new DatasourceConfigVO();
        response.setDatasourceId(config.getDatasourceId());
        response.setTenantId(config.getTenantId());
        response.setDatasourceCode(config.getDatasourceCode());
        response.setDatasourceName(config.getDatasourceName());
        response.setEngineType(config.getEngineType());
        response.setConnectionMode(config.getConnectionMode());
        response.setStage(config.getStage());
        response.setJdbcUrl(config.getJdbcUrl());
        response.setJdbcDriverClassName(config.getJdbcDriverClassName());
        response.setDriverSourceType(config.getDriverSourceType());
        response.setDriverArtifactId(config.getDriverArtifactId());
        response.setDriverVersionLabel(config.getDriverVersionLabel());
        response.setDriverSha256(config.getDriverSha256());
        response.setDriverLoadStatus(config.getDriverLoadStatus());
        response.setUsername(config.getUsername());
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
                "已认证请求上下文缺少 tenantId");
        }
        String normalized = trimToNull(requestTenantId);
        if (StringUtils.hasText(normalized) && !contextTenantId.equals(normalized)) {
            throw new AccessDeniedException("请求 tenantId 与已认证租户上下文不一致");
        }
        return contextTenantId;
    }

    private String normalizeConnectionMode(String value) {
        String normalized = requireText(value, "connectionMode").toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CONNECTION_MODES.contains(normalized)) {
            throw invalidArgument("connectionMode", "connectionMode 必须是 JDBC/API/REST/CLIENT/GATEWAY/PROXY 之一");
        }
        return normalized;
    }

    private String normalizeEngineType(String value) {
        return firstNonBlank(value, DEFAULT_ENGINE_TYPE).toUpperCase(Locale.ROOT);
    }

    private void requireDatasourceAdmin() {
        if (SYSTEM_TENANT_ID.equals(RequestContext.getTenantId())) {
            return;
        }
        throw new AccessDeniedException("当前请求不允许管理数据源配置");
    }

    private DatasourceConfig findDatasourceByCodeAndEngine(String tenantId, String datasourceCode, String engineType) {
        return datasourceConfigRepository
            .findByTenantIdAndDatasourceCodeAndEngineType(tenantId, datasourceCode, engineType)
            .orElse(null);
    }

    private List<String> resolveRouteOrder(String requestedType) {
        if ("AUTO".equalsIgnoreCase(requestedType)) {
            return ROUTE_ORDER;
        }
        if ("TRINO".equalsIgnoreCase(requestedType)) {
            return Arrays.asList("TRINO", "HIVE");
        }
        if ("HETU".equalsIgnoreCase(requestedType)) {
            return Arrays.asList("HETU", "HIVE");
        }
        if ("HIVE".equalsIgnoreCase(requestedType)) {
            return Collections.singletonList("HIVE");
        }
        return Collections.emptyList();
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

    private CredentialEnvelope buildCredentialEnvelope(DatasourceConfigUpsertRequest request, DatasourceConfig existing) {
        String secret = trimToNull(request == null ? null : request.getCredentialSecret());
        if (!StringUtils.hasText(secret) && existing != null) {
            return new CredentialEnvelope(
                existing.getCredentialMask(),
                existing.getCredentialCiphertext(),
                existing.getEncryptionAlgorithm(),
                existing.getEncryptionKeyId()
            );
        }
        if (!StringUtils.hasText(secret)) {
            return new CredentialEnvelope(null, null, null, null);
        }
        return new CredentialEnvelope(
            maskSecret(secret),
            sensitiveDataCryptoService.encrypt(secret),
            sensitiveDataCryptoService.getAlgorithm(),
            sensitiveDataCryptoService.getKeyId()
        );
    }

    private DriverBinding resolveDriverBinding(String tenantId,
                                               DatasourceConfigUpsertRequest request,
                                               DatasourceConfig existing) {
        String driverSourceType = firstNonBlank(
            request == null ? null : request.getDriverSourceType(),
            existing == null ? null : existing.getDriverSourceType(),
            DEFAULT_DRIVER_SOURCE_TYPE
        ).toUpperCase(Locale.ROOT);
        if (!"CLASSPATH".equals(driverSourceType) && !"UPLOADED".equals(driverSourceType)) {
            throw invalidArgument("driverSourceType", "driverSourceType 必须是 CLASSPATH 或 UPLOADED");
        }
        if ("CLASSPATH".equals(driverSourceType)) {
            return new DriverBinding("CLASSPATH", null, null, null, "CLASSPATH_READY");
        }
        String artifactId = firstNonBlank(
            request == null ? null : request.getDriverArtifactId(),
            existing == null ? null : existing.getDriverArtifactId()
        );
        if (!StringUtils.hasText(artifactId)) {
            throw invalidArgument("driverArtifactId", "UPLOADED 驱动必须提供 driverArtifactId");
        }
        if (jdbcDriverArtifactApplicationService == null) {
            throw invalidArgument("driverArtifactId", "当前环境未启用上传驱动绑定能力");
        }
        JdbcDriverArtifact artifact = jdbcDriverArtifactApplicationService.requireArtifact(tenantId, artifactId);
        String engineType = normalizeEngineType(request == null ? null : request.getEngineType());
        if (!artifact.getEngineType().equalsIgnoreCase(engineType)) {
            throw invalidArgument("driverArtifactId", "上传驱动与 datasource engineType 不一致");
        }
        if (request != null) {
            request.setJdbcDriverClassName(artifact.getDriverClassName());
        }
        return new DriverBinding(
            "UPLOADED",
            artifact.getArtifactId(),
            artifact.getVersionLabel(),
            artifact.getSha256(),
            "READY"
        );
    }

    private String decryptCredential(DatasourceConfig config) {
        if (config == null || !StringUtils.hasText(config.getCredentialCiphertext())) {
            return null;
        }
        return sensitiveDataCryptoService.decrypt(config.getCredentialCiphertext());
    }

    private GovernanceJdbcDatasourceResolveResponse unresolved(String tenantId,
                                                               String datasourceCode,
                                                               String engineType,
                                                               String failureReason) {
        GovernanceJdbcDatasourceResolveResponse response = baseResolveResponse(tenantId, datasourceCode, engineType);
        response.setResolved(false);
        response.setFailureReason(failureReason);
        return response;
    }

    private GovernanceJdbcDatasourceResolveResponse baseResolveResponse(String tenantId,
                                                                        String datasourceCode,
                                                                        String engineType) {
        GovernanceJdbcDatasourceResolveResponse response = new GovernanceJdbcDatasourceResolveResponse();
        response.setTenantId(tenantId);
        response.setDatasourceCode(datasourceCode);
        response.setEngineType(engineType);
        response.setConnectionMode("JDBC");
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(JDBC_RESOLVE_IMPLEMENTATION_STAGE);
        return response;
    }

    private BizException invalidArgument(String fieldName, String message) {
        return new BizException(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, HttpStatus.BAD_REQUEST, message + " [" + fieldName + "]");
    }

    private static final class CredentialEnvelope {

        private final String mask;
        private final String ciphertext;
        private final String algorithm;
        private final String keyId;

        private CredentialEnvelope(String mask, String ciphertext, String algorithm, String keyId) {
            this.mask = mask;
            this.ciphertext = ciphertext;
            this.algorithm = algorithm;
            this.keyId = keyId;
        }
    }

    private static final class DriverBinding {

        private final String driverSourceType;
        private final String driverArtifactId;
        private final String driverVersionLabel;
        private final String driverSha256;
        private final String driverLoadStatus;

        private DriverBinding(String driverSourceType,
                              String driverArtifactId,
                              String driverVersionLabel,
                              String driverSha256,
                              String driverLoadStatus) {
            this.driverSourceType = driverSourceType;
            this.driverArtifactId = driverArtifactId;
            this.driverVersionLabel = driverVersionLabel;
            this.driverSha256 = driverSha256;
            this.driverLoadStatus = driverLoadStatus;
        }
    }
}
