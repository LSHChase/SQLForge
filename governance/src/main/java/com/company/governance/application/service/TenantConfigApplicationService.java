package com.company.governance.application.service;

import com.company.governance.application.controller.vo.TenantConfigVO;
import com.company.governance.application.service.converter.TenantConfigConverter;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.domain.tenant.repository.TenantConfigRepository;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import com.company.sqlforge.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TenantConfigApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantConfigApplicationService.class);
    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final String TENANT_ADMIN = "TENANT_ADMIN";
    private static final String DEFAULT_DATA_SOURCE_ID = "governance-tenant-config";
    private static final String TENANT_CONFIG_ACCESS_DENIED_MESSAGE = "当前角色无权读取租户配置";

    private final TenantConfigRepository tenantConfigRepository;
    private final TenantAccessLogic tenantAccessLogic;
    private final TenantConfigConverter tenantConfigConverter;

    public TenantConfigApplicationService(TenantConfigRepository tenantConfigRepository,
                                          TenantAccessLogic tenantAccessLogic,
                                          TenantConfigConverter tenantConfigConverter) {
        this.tenantConfigRepository = tenantConfigRepository;
        this.tenantAccessLogic = tenantAccessLogic;
        this.tenantConfigConverter = tenantConfigConverter;
    }

    public TenantConfigVO findByTenantId(String tenantId) {
        String currentTenantId = TenantContext.get();
        boolean platformAdmin = RequestContext.hasRole(PLATFORM_ADMIN);
        if (!StringUtils.hasText(currentTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "租户上下文缺失"
            );
        }
        if (!StringUtils.hasText(tenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "租户 ID 不能为空"
            );
        }
        if (!platformAdmin && !RequestContext.hasRole(TENANT_ADMIN)) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                TENANT_CONFIG_ACCESS_DENIED_MESSAGE
            );
        }
        if (!currentTenantId.equals(tenantId) && !platformAdmin) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED_MESSAGE
            );
        }
        if (!platformAdmin && !tenantAccessLogic.validateDataSourceAccess(currentTenantId, DEFAULT_DATA_SOURCE_ID)) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED_MESSAGE
            );
        }

        LOGGER.info("正在加载租户配置，currentTenantId={}, targetTenantId={}, traceId={}",
            currentTenantId,
            tenantId,
            RequestContext.getTraceId());
        return tenantConfigRepository.findByTenantId(tenantId)
            .map(tenantConfigConverter::toVO)
            .orElseThrow(() -> new BizException(
                ErrorCodeConstants.GOVERNANCE_TENANT_CONFIG_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "租户配置不存在"
            ));
    }
}
