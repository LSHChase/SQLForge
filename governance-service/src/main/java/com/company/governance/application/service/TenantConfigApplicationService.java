package com.company.governance.application.service;

import com.company.common.constants.ErrorCodeConstants;
import com.company.common.exception.BizException;
import com.company.governance.application.controller.vo.TenantConfigVO;
import com.company.governance.application.service.converter.TenantConfigConverter;
import com.company.governance.common.context.RequestContext;
import com.company.governance.common.context.TenantContext;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.domain.tenant.repository.TenantConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TenantConfigApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantConfigApplicationService.class);
    private static final String DEFAULT_DATA_SOURCE_ID = "governance-tenant-config";

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
        if (!tenantAccessLogic.validateDataSourceAccess(currentTenantId, DEFAULT_DATA_SOURCE_ID)) {
            throw new BizException(
                ErrorCodeConstants.ERROR_ACCESS_DENIED,
                ErrorCodeConstants.ERROR_ACCESS_DENIED_MESSAGE
            );
        }
        if (!StringUtils.hasText(tenantId)) {
            throw new BizException(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, "Tenant id must not be empty");
        }

        LOGGER.info("Loading tenant config, currentTenantId={}, targetTenantId={}, traceId={}",
            currentTenantId,
            tenantId,
            RequestContext.getTraceId());
        return tenantConfigRepository.findByTenantId(tenantId)
            .map(tenantConfigConverter::toVO)
            .orElseThrow(() -> new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                "Tenant config not found"
            ));
    }
}
