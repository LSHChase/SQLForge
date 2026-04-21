package com.company.governance.application.service;

import com.company.governance.application.controller.dto.AuditWriteRequest;
import com.company.governance.application.controller.dto.DatasourceAccessCheckRequest;
import com.company.governance.application.controller.dto.TenantScopeCheckRequest;
import com.company.governance.application.controller.vo.AuditWriteResponse;
import com.company.governance.application.controller.vo.DatasourceAccessCheckResponse;
import com.company.governance.application.controller.vo.ScheduleExtensionStatusVO;
import com.company.governance.application.controller.vo.TenantScopeCheckResponse;
import com.company.governance.config.MessagingProperties;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.sqlforge.common.config.MessagingMode;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GovernanceCapabilityApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GovernanceCapabilityApplicationService.class);
    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final String CONTRACT_STAGE_LONG_TERM_BASELINE = "LONG_TERM_BASELINE";
    private static final String CONTRACT_STAGE_TRANSITIONAL_SKELETON = "TRANSITIONAL_SKELETON";
    private static final String IMPLEMENTATION_STAGE_TRANSITIONAL_SKELETON = "TRANSITIONAL_SKELETON";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_EXTERNALIZED = "EXTERNALIZED";
    private static final String STATUS_TEST_ONLY = "TEST_ONLY";
    private static final String REASON_ALLOWED = "ALLOWED";
    private static final String REASON_PLATFORM_ADMIN_OVERRIDE = "PLATFORM_ADMIN_OVERRIDE";
    private static final String REASON_CALLER_TENANT_MISMATCH = "CALLER_TENANT_MISMATCH";
    private static final String REASON_CROSS_TENANT_PLATFORM_ADMIN_REQUIRED =
        "CROSS_TENANT_ACCESS_REQUIRES_PLATFORM_ADMIN";
    private static final String REASON_TENANT_DATASOURCE_ACCESS_DENIED = "TENANT_DATASOURCE_ACCESS_DENIED";
    private static final String GOVERNANCE_SCHEDULE_EXTENSION_POINT = "governance.schedule.dispatch";

    private final TenantAccessLogic tenantAccessLogic;
    private final GovernanceAuditTrailService governanceAuditTrailService;
    private final MessagingProperties messagingProperties;

    public GovernanceCapabilityApplicationService(TenantAccessLogic tenantAccessLogic,
                                                  GovernanceAuditTrailService governanceAuditTrailService,
                                                  MessagingProperties messagingProperties) {
        this.tenantAccessLogic = tenantAccessLogic;
        this.governanceAuditTrailService = governanceAuditTrailService;
        this.messagingProperties = messagingProperties;
    }

    public TenantScopeCheckResponse checkTenantScope(TenantScopeCheckRequest request) {
        String callerTenantId = requireProtectedTenantContext();
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        String targetTenantId = requireText(request == null ? null : request.getTargetTenantId(), "targetTenantId");
        boolean platformAdmin = RequestContext.hasRole(PLATFORM_ADMIN);
        if (!platformAdmin && !callerTenantId.equals(tenantId)) {
            return new TenantScopeCheckResponse(
                tenantId,
                targetTenantId,
                false,
                REASON_CALLER_TENANT_MISMATCH
            );
        }
        boolean allowed = platformAdmin || tenantId.equals(targetTenantId);
        return new TenantScopeCheckResponse(
            tenantId,
            targetTenantId,
            allowed,
            allowed
                ? (platformAdmin && !tenantId.equals(targetTenantId) ? REASON_PLATFORM_ADMIN_OVERRIDE : REASON_ALLOWED)
                : REASON_CROSS_TENANT_PLATFORM_ADMIN_REQUIRED
        );
    }

    public DatasourceAccessCheckResponse checkDatasourceAccess(DatasourceAccessCheckRequest request) {
        String callerTenantId = requireProtectedTenantContext();
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        String datasourceId = requireText(request == null ? null : request.getDatasourceId(), "datasourceId");
        boolean platformAdmin = RequestContext.hasRole(PLATFORM_ADMIN);
        if (!platformAdmin && !callerTenantId.equals(tenantId)) {
            return new DatasourceAccessCheckResponse(
                tenantId,
                datasourceId,
                false,
                REASON_CALLER_TENANT_MISMATCH,
                Integer.valueOf(ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED),
                CONTRACT_STAGE_LONG_TERM_BASELINE,
                IMPLEMENTATION_STAGE_TRANSITIONAL_SKELETON
            );
        }
        boolean allowed = platformAdmin || tenantAccessLogic.validateDataSourceAccess(tenantId, datasourceId);
        return new DatasourceAccessCheckResponse(
            tenantId,
            datasourceId,
            allowed,
            allowed
                ? (platformAdmin ? REASON_PLATFORM_ADMIN_OVERRIDE : REASON_ALLOWED)
                : REASON_TENANT_DATASOURCE_ACCESS_DENIED,
            allowed ? null : Integer.valueOf(ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED),
            CONTRACT_STAGE_LONG_TERM_BASELINE,
            IMPLEMENTATION_STAGE_TRANSITIONAL_SKELETON
        );
    }

    public AuditWriteResponse publishAuditEvent(AuditWriteRequest request) {
        requireProtectedTenantContext();
        return governanceAuditTrailService.writeAudit(request);
    }

    public ScheduleExtensionStatusVO getScheduleExtensionStatus() {
        MessagingMode messagingMode = requireMessagingMode();
        String status = STATUS_ACTIVE;
        if (messagingMode == MessagingMode.KAFKA) {
            status = STATUS_EXTERNALIZED;
        }
        if (messagingMode == MessagingMode.MOCK) {
            status = STATUS_TEST_ONLY;
        }
        return new ScheduleExtensionStatusVO(
            GOVERNANCE_SCHEDULE_EXTENSION_POINT,
            ServiceCodeConstants.GOVERNANCE,
            status,
            messagingMode.name(),
            CONTRACT_STAGE_TRANSITIONAL_SKELETON,
            IMPLEMENTATION_STAGE_TRANSITIONAL_SKELETON
        );
    }

    private String requireProtectedTenantContext() {
        String tenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(tenantId)
            || !StringUtils.hasText(RequestContext.getUserId())
            || !StringUtils.hasText(RequestContext.getRequestId())
            || !StringUtils.hasText(RequestContext.getTraceId())) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "Protected request context is missing"
            );
        }
        return tenantId;
    }

    private MessagingMode requireMessagingMode() {
        MessagingMode messagingMode = messagingProperties.getMode();
        if (messagingMode == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_MESSAGE_MODE_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Messaging mode is not configured"
            );
        }
        return messagingMode;
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                fieldName + " must not be empty"
            );
        }
        return value;
    }
}
