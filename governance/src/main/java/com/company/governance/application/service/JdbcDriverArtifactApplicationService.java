package com.company.governance.application.service;

import com.company.governance.application.controller.vo.JdbcDriverArtifactVO;
import com.company.governance.config.GovernanceDatasourceDriverProperties;
import com.company.governance.domain.datasource.JdbcDriverArtifact;
import com.company.governance.domain.datasource.repository.JdbcDriverArtifactRepository;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class JdbcDriverArtifactApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "JDBC_DRIVER_ARTIFACT_BASELINE";
    private static final List<String> SUPPORTED_ENGINES = Arrays.asList("TRINO", "HETU", "HIVE");

    private final JdbcDriverArtifactRepository repository;
    private final JdbcDriverArtifactStorageService storageService;
    private final GovernanceDatasourceDriverProperties properties;

    public JdbcDriverArtifactApplicationService(JdbcDriverArtifactRepository repository,
                                                JdbcDriverArtifactStorageService storageService,
                                                GovernanceDatasourceDriverProperties properties) {
        this.repository = repository;
        this.storageService = storageService;
        this.properties = properties;
    }

    public JdbcDriverArtifactVO upload(String tenantId,
                                       String engineType,
                                       String driverClassName,
                                       String versionLabel,
                                       MultipartFile file) {
        String effectiveTenantId = requireTenant(tenantId);
        requireDatasourceAdmin();
        String normalizedEngineType = normalizeEngineType(engineType);
        String normalizedDriverClassName = requireText(driverClassName, "driverClassName");
        String normalizedVersionLabel = requireText(versionLabel, "versionLabel");
        if (file == null || file.isEmpty()) {
            throw invalidArgument("file", "file 为必填项");
        }
        String originalFileName = requireText(file.getOriginalFilename(), "file");
        if (!originalFileName.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            throw invalidArgument("file", "只允许上传 .jar 驱动文件");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw invalidArgument("file", "驱动文件超过大小限制");
        }
        String artifactId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        try (InputStream inputStream = file.getInputStream()) {
            byte[] bytes = toByteArray(inputStream, file.getSize());
            String sha256 = sha256(bytes);
            Path storedPath = storageService.store(
                effectiveTenantId,
                normalizedEngineType,
                artifactId,
                originalFileName,
                new java.io.ByteArrayInputStream(bytes)
            );
            Path basePath = java.nio.file.Paths.get(properties.getStoragePath()).toAbsolutePath().normalize();
            JdbcDriverArtifact artifact = new JdbcDriverArtifact(
                artifactId,
                effectiveTenantId,
                normalizedEngineType,
                normalizedDriverClassName,
                normalizedVersionLabel,
                originalFileName,
                bytes.length,
                sha256,
                basePath.relativize(storedPath).toString().replace('\\', '/'),
                "READY",
                firstNonBlank(RequestContext.getUserId(), "unknown"),
                now,
                now
            );
            repository.save(artifact);
            return toVO(artifact);
        } catch (IOException ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "驱动文件保存失败",
                ex
            );
        }
    }

    public List<JdbcDriverArtifactVO> list(String tenantId) {
        String effectiveTenantId = requireTenant(tenantId);
        List<JdbcDriverArtifact> artifacts = repository.findByTenantId(effectiveTenantId);
        List<JdbcDriverArtifactVO> responses = new ArrayList<JdbcDriverArtifactVO>(artifacts.size());
        for (JdbcDriverArtifact artifact : artifacts) {
            responses.add(toVO(artifact));
        }
        return responses;
    }

    public JdbcDriverArtifactVO find(String tenantId, String artifactId) {
        String effectiveTenantId = requireTenant(tenantId);
        JdbcDriverArtifact artifact = repository.findByTenantIdAndArtifactId(effectiveTenantId, requireText(artifactId, "artifactId"))
            .orElseThrow(() -> new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "驱动制品不存在：" + artifactId
            ));
        return toVO(artifact);
    }

    public JdbcDriverArtifact requireArtifact(String tenantId, String artifactId) {
        return repository.findByTenantIdAndArtifactId(tenantId, artifactId)
            .orElseThrow(() -> new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "驱动制品不存在：" + artifactId
            ));
    }

    public Path resolveArtifactPath(JdbcDriverArtifact artifact) {
        return storageService.resolve(artifact.getRelativePath());
    }

    private JdbcDriverArtifactVO toVO(JdbcDriverArtifact artifact) {
        JdbcDriverArtifactVO response = new JdbcDriverArtifactVO();
        response.setArtifactId(artifact.getArtifactId());
        response.setTenantId(artifact.getTenantId());
        response.setEngineType(artifact.getEngineType());
        response.setDriverClassName(artifact.getDriverClassName());
        response.setVersionLabel(artifact.getVersionLabel());
        response.setOriginalFileName(artifact.getOriginalFileName());
        response.setSizeBytes(Long.valueOf(artifact.getSizeBytes()));
        response.setSha256(artifact.getSha256());
        response.setStatus(artifact.getStatus());
        response.setUploadedBy(artifact.getUploadedBy());
        response.setCreatedAt(artifact.getCreatedAt());
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        return response;
    }

    private void requireDatasourceAdmin() {
        if (RequestContext.hasRole("PLATFORM_ADMIN") || RequestContext.hasRole("TENANT_ADMIN")) {
            return;
        }
        throw new AccessDeniedException("当前请求缺少数据源驱动管理权限");
    }

    private String requireTenant(String requestTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(ErrorCodeConstants.SYSTEM_CONTEXT_MISSING, HttpStatus.UNAUTHORIZED, "已认证请求上下文缺少 tenantId");
        }
        String normalized = trimToNull(requestTenantId);
        if (StringUtils.hasText(normalized) && !contextTenantId.equals(normalized)) {
            throw new AccessDeniedException("请求 tenantId 与已认证租户上下文不一致");
        }
        return contextTenantId;
    }

    private String normalizeEngineType(String value) {
        String normalized = requireText(value, "engineType").toUpperCase(Locale.ROOT);
        if (!SUPPORTED_ENGINES.contains(normalized)) {
            throw invalidArgument("engineType", "engineType 仅支持 TRINO/HETU/HIVE");
        }
        return normalized;
    }

    private byte[] toByteArray(InputStream inputStream, long expectedSize) throws IOException {
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream(expectedSize > 0 ? (int) expectedSize : 8192);
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", Byte.valueOf(value)));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 初始化失败", ex);
        }
    }

    private BizException invalidArgument(String fieldName, String message) {
        return new BizException(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, HttpStatus.BAD_REQUEST, message + " [" + fieldName + "]");
    }

    private String requireText(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            throw invalidArgument(fieldName, fieldName + " 为必填项");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String firstNonBlank(String left, String right) {
        return StringUtils.hasText(left) ? left : right;
    }
}
