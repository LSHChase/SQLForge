package com.company.governance.application.service;

import com.company.governance.application.controller.dto.TenantEngineConfigUpdateRequest;
import com.company.governance.application.controller.vo.TenantConfigOptionVO;
import com.company.governance.application.controller.vo.TenantConfigVO;
import com.company.governance.application.service.converter.TenantConfigConverter;
import com.company.governance.domain.tenant.entity.TenantConfig;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.domain.tenant.repository.TenantConfigRepository;
import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.JsonUtils;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TenantConfigApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantConfigApplicationService.class);
    private static final String DEFAULT_DATA_SOURCE_ID = "governance-tenant-config";

    private final TenantConfigRepository tenantConfigRepository;
    private final TenantAccessLogic tenantAccessLogic;
    private final TenantConfigConverter tenantConfigConverter;
    private final GovernanceProtectedPersistenceService governanceProtectedPersistenceService;

    public TenantConfigApplicationService(TenantConfigRepository tenantConfigRepository,
                                          TenantAccessLogic tenantAccessLogic,
                                          TenantConfigConverter tenantConfigConverter,
                                          GovernanceProtectedPersistenceService governanceProtectedPersistenceService) {
        this.tenantConfigRepository = tenantConfigRepository;
        this.tenantAccessLogic = tenantAccessLogic;
        this.tenantConfigConverter = tenantConfigConverter;
        this.governanceProtectedPersistenceService = governanceProtectedPersistenceService;
    }

    public TenantConfigVO findByTenantId(String tenantId) {
        String currentTenantId = requireCurrentTenantId();
        String targetTenantId = requireTenantId(tenantId);
        requireTargetTenantAccess(currentTenantId, targetTenantId);
        requireGovernanceConfigAccess(currentTenantId, "READ");

        LOGGER.info("正在加载租户配置，currentTenantId={}, targetTenantId={}, traceId={}",
            currentTenantId,
            targetTenantId,
            RequestContext.getTraceId());
        return tenantConfigRepository.findByTenantId(targetTenantId)
            .map(tenantConfigConverter::toVO)
            .orElseThrow(() -> new BizException(
                ErrorCodeConstants.GOVERNANCE_TENANT_CONFIG_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "租户配置不存在"
            ));
    }

    public List<TenantConfigOptionVO> listTenantOptions() {
        String currentTenantId = requireCurrentTenantId();
        requireGovernanceConfigAccess(currentTenantId, "READ");

        List<TenantConfig> tenantConfigs = "system".equals(currentTenantId)
            ? tenantConfigRepository.findAll()
            : tenantConfigRepository.findByTenantId(currentTenantId)
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
        return tenantConfigs.stream()
            .map(this::toOption)
            .collect(Collectors.toList());
    }

    public TenantConfigVO updateTenantEngines(TenantEngineConfigUpdateRequest request) {
        if (request == null) {
            throw invalidArgument("请求体不能为空");
        }
        String currentTenantId = requireCurrentTenantId();
        String targetTenantId = requireTenantId(request.getTenantId());
        requireTargetTenantAccess(currentTenantId, targetTenantId);
        requireGovernanceConfigAccess(currentTenantId, "MANAGE");

        DataSourceTypeEnum defaultEngine = parseEngine(request.getDefaultEngine(), "defaultEngine");
        DataSourceTypeEnum backupEngine = parseEngine(request.getBackupEngine(), "backupEngine");
        TenantConfig tenantConfig = tenantConfigRepository.findByTenantId(targetTenantId)
            .orElseThrow(() -> new BizException(
                ErrorCodeConstants.GOVERNANCE_TENANT_CONFIG_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "租户配置不存在"
            ));

        tenantConfig.setDefaultEngine(defaultEngine);
        tenantConfig.setBackupEngine(backupEngine);
        tenantConfig.setUpdateTime(LocalDateTime.now());
        int updated = tenantConfigRepository.update(tenantConfig);
        if (updated <= 0) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_TENANT_CONFIG_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "租户配置不存在"
            );
        }
        saveUpdateAuditLog(currentTenantId, targetTenantId, defaultEngine, backupEngine);
        LOGGER.info("租户默认/备用引擎已更新，currentTenantId={}, targetTenantId={}, defaultEngine={}, backupEngine={}, traceId={}",
            currentTenantId,
            targetTenantId,
            defaultEngine,
            backupEngine,
            RequestContext.getTraceId());
        return tenantConfigConverter.toVO(tenantConfig);
    }

    private TenantConfigOptionVO toOption(TenantConfig tenantConfig) {
        return new TenantConfigOptionVO(
            tenantConfig.getTenantId(),
            tenantConfig.getTenantId(),
            tenantConfig.getDefaultEngine() == null ? null : tenantConfig.getDefaultEngine().name(),
            tenantConfig.getBackupEngine() == null ? null : tenantConfig.getBackupEngine().name()
        );
    }

    private String requireCurrentTenantId() {
        String currentTenantId = TenantContext.get();
        if (!StringUtils.hasText(currentTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "租户上下文缺失"
            );
        }
        return currentTenantId;
    }

    private String requireTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            throw invalidArgument("租户 ID 不能为空");
        }
        return tenantId.trim();
    }

    private void requireTargetTenantAccess(String currentTenantId, String targetTenantId) {
        if (!currentTenantId.equals(targetTenantId) && !"system".equals(currentTenantId)) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED_MESSAGE
            );
        }
    }

    private void requireGovernanceConfigAccess(String currentTenantId, String action) {
        if (!tenantAccessLogic.validateDataSourceAccess(currentTenantId, DEFAULT_DATA_SOURCE_ID, action)) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED_MESSAGE
            );
        }
    }

    private DataSourceTypeEnum parseEngine(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw invalidArgument(fieldName + " 不能为空");
        }
        try {
            return DataSourceTypeEnum.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw invalidArgument(fieldName + " 不支持：" + value);
        }
    }

    private BizException invalidArgument(String message) {
        return new BizException(
            ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
            HttpStatus.BAD_REQUEST,
            message
        );
    }

    private void saveUpdateAuditLog(String currentTenantId,
                                    String targetTenantId,
                                    DataSourceTypeEnum defaultEngine,
                                    DataSourceTypeEnum backupEngine) {
        Map<String, Object> requestParams = new LinkedHashMap<String, Object>();
        requestParams.put("currentTenantId", currentTenantId);
        requestParams.put("tenantId", targetTenantId);
        requestParams.put("defaultEngine", defaultEngine.name());
        requestParams.put("backupEngine", backupEngine.name());

        AuditLogRecord auditLogRecord = new AuditLogRecord();
        auditLogRecord.setTenantId(targetTenantId);
        auditLogRecord.setServiceCode("governance");
        auditLogRecord.setOperationType("TENANT_CONFIG_ENGINE_UPDATE");
        auditLogRecord.setTargetType("TENANT_CONFIG");
        auditLogRecord.setTargetId(targetTenantId);
        auditLogRecord.setRequestId(RequestContext.getRequestId());
        auditLogRecord.setTraceId(RequestContext.getTraceId());
        auditLogRecord.setRequestParams(JsonUtils.toJson(requestParams));
        auditLogRecord.setResponseSummary("defaultEngine=" + defaultEngine.name() + ", backupEngine=" + backupEngine.name());
        auditLogRecord.setStatus("SUCCESS");
        auditLogRecord.setCostMs(0L);
        auditLogRecord.setCreateTime(LocalDateTime.now());
        governanceProtectedPersistenceService.saveAuditLog(auditLogRecord);
    }
}
