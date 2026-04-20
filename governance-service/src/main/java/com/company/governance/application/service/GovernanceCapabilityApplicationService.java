package com.company.governance.application.service;

import com.company.governance.application.controller.dto.AuditWriteRequest;
import com.company.governance.application.controller.dto.DatasourceAccessCheckRequest;
import com.company.governance.application.controller.dto.TenantScopeCheckRequest;
import com.company.governance.application.controller.vo.AuditWriteResponse;
import com.company.governance.application.controller.vo.DatasourceAccessCheckResponse;
import com.company.governance.application.controller.vo.ScheduleExtensionStatusVO;
import com.company.governance.application.controller.vo.TenantScopeCheckResponse;
import com.company.governance.config.MessagingProperties;
import com.company.governance.domain.messaging.MessageProducer;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.governance.infrastructure.messaging.GovernanceMessagingTopics;
import com.company.sqlforge.common.audit.AuditEvent;
import com.company.sqlforge.common.audit.AuditContext;
import com.company.sqlforge.common.config.MessagingMode;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.DateUtils;
import com.company.sqlforge.common.utils.JsonUtils;
import java.util.HashMap;
import java.util.Map;
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
    private static final String STATUS_ACCEPTED = "ACCEPTED";
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
    private static final String GOVERNANCE_AUDIT_ROUTE_UNAVAILABLE_MESSAGE =
        "Governance audit contract route is unavailable";

    private final TenantAccessLogic tenantAccessLogic;
    private final MessageProducer messageProducer;
    private final MessagingProperties messagingProperties;

    public GovernanceCapabilityApplicationService(TenantAccessLogic tenantAccessLogic,
                                                  MessageProducer messageProducer,
                                                  MessagingProperties messagingProperties) {
        this.tenantAccessLogic = tenantAccessLogic;
        this.messageProducer = messageProducer;
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
        String serviceCode = requireAuditText(request == null ? null : request.getServiceCode(), "serviceCode");
        String operationCode = requireAuditText(request == null ? null : request.getOperationCode(), "operationCode");
        String resourceType = requireAuditText(request == null ? null : request.getResourceType(), "resourceType");
        String resourceId = requireAuditText(request == null ? null : request.getResourceId(), "resourceId");
        String resultStatus = requireAuditText(request == null ? null : request.getResultStatus(), "resultStatus");
        long elapsedMs = requireAuditElapsedMs(request == null ? null : request.getElapsedMs());
        String sourceIp = requireAuditText(request == null ? null : request.getSourceIp(), "sourceIp");
        String userAgent = requireAuditText(request == null ? null : request.getUserAgent(), "userAgent");
        AuditEvent auditEvent = new AuditEvent(
            DateUtils.format(DateUtils.now()),
            RequestContext.getTenantId(),
            RequestContext.getUserId(),
            serviceCode,
            operationCode,
            resourceType,
            resourceId,
            resultStatus,
            elapsedMs,
            RequestContext.getTraceId(),
            RequestContext.getRequestId(),
            sourceIp,
            userAgent
        );
        AuditContext.set(auditEvent);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("traceId", RequestContext.getTraceId());
        headers.put("requestId", RequestContext.getRequestId());
        try {
            messageProducer.send(
                GovernanceMessagingTopics.AUDIT_EVENT,
                RequestContext.getTenantId(),
                JsonUtils.toJson(auditEvent),
                headers
            );
        } catch (RuntimeException ex) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_SYSTEM_MESSAGE_ROUTE_INVALID,
                HttpStatus.SERVICE_UNAVAILABLE,
                GOVERNANCE_AUDIT_ROUTE_UNAVAILABLE_MESSAGE,
                ex
            );
        }
        LOGGER.info("Published governance audit contract event, serviceCode={}, operationCode={}, mode={}",
            serviceCode, operationCode, messagingProperties.getMode());
        return new AuditWriteResponse(
            serviceCode,
            operationCode,
            STATUS_ACCEPTED,
            GovernanceMessagingTopics.AUDIT_EVENT,
            requireMessagingMode().name(),
            CONTRACT_STAGE_LONG_TERM_BASELINE,
            IMPLEMENTATION_STAGE_TRANSITIONAL_SKELETON
        );
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
            ServiceCodeConstants.GOVERNANCE_SERVICE,
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

    private String requireAuditText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
                HttpStatus.BAD_REQUEST,
                fieldName + " must not be empty"
            );
        }
        return value;
    }

    private long requireAuditElapsedMs(Long elapsedMs) {
        if (elapsedMs == null || elapsedMs.longValue() < 0L) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
                HttpStatus.BAD_REQUEST,
                "elapsedMs must not be negative"
            );
        }
        return elapsedMs.longValue();
    }
}
