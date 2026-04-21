package com.company.governance.application.service;

import com.company.governance.application.controller.dto.AuditWriteRequest;
import com.company.governance.application.controller.vo.AuditWriteResponse;
import com.company.governance.config.MessagingProperties;
import com.company.governance.domain.message.entity.MessageQueueRecord;
import com.company.governance.domain.message.repository.MessageQueueRepository;
import com.company.governance.domain.messaging.MessageProducer;
import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.governance.domain.trace.entity.ConfigSnapshotRecord;
import com.company.governance.domain.trace.entity.ExecutionResultRecord;
import com.company.governance.domain.trace.entity.ExportRecord;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.infrastructure.messaging.GovernanceMessagingTopics;
import com.company.governance.infrastructure.persistence.mapper.ConfigSnapshotMapper;
import com.company.governance.infrastructure.persistence.mapper.ExecutionResultMapper;
import com.company.governance.infrastructure.persistence.mapper.ExportRecordMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.sqlforge.common.audit.AuditContext;
import com.company.sqlforge.common.audit.AuditEvent;
import com.company.sqlforge.common.config.MessagingMode;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.DateUtils;
import com.company.sqlforge.common.utils.JsonUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GovernanceAuditTrailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GovernanceAuditTrailService.class);

    private static final String CONTRACT_STAGE_LONG_TERM_BASELINE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE_DATABASE_AUDIT_WRITE_BASELINE = "DATABASE_AUDIT_WRITE_BASELINE";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String AUTH_TARGET_TYPE = "AUTH_REQUEST";
    private static final String AUTH_LOGIN_OPERATION = "LOGIN";
    private static final String AUTH_LOGOUT_OPERATION = "LOGOUT";
    private static final String AUTH_LOGIN_FAILURE_SUMMARY = "Authentication request rejected before protected context was established";
    private static final String AUTH_LOGIN_SUCCESS_SUMMARY = "Protected request context established through stateless header authentication";
    private static final String AUTH_LOGOUT_SUMMARY = "Protected request context released after request completion";
    private static final String GOVERNANCE_AUDIT_ROUTE_UNAVAILABLE_MESSAGE =
        "Governance audit contract route is unavailable";
    private static final String UNKNOWN_VALUE = "UNKNOWN";

    private final GovernanceProtectedPersistenceService governanceProtectedPersistenceService;
    private final ConfigSnapshotMapper configSnapshotMapper;
    private final ExecutionResultMapper executionResultMapper;
    private final QueryHistoryMapper queryHistoryMapper;
    private final ExportRecordMapper exportRecordMapper;
    private final MessageQueueRepository messageQueueRepository;
    private final MessageProducer messageProducer;
    private final MessagingProperties messagingProperties;

    public GovernanceAuditTrailService(GovernanceProtectedPersistenceService governanceProtectedPersistenceService,
                                       ConfigSnapshotMapper configSnapshotMapper,
                                       ExecutionResultMapper executionResultMapper,
                                       QueryHistoryMapper queryHistoryMapper,
                                       ExportRecordMapper exportRecordMapper,
                                       MessageQueueRepository messageQueueRepository,
                                       MessageProducer messageProducer,
                                       MessagingProperties messagingProperties) {
        this.governanceProtectedPersistenceService = governanceProtectedPersistenceService;
        this.configSnapshotMapper = configSnapshotMapper;
        this.executionResultMapper = executionResultMapper;
        this.queryHistoryMapper = queryHistoryMapper;
        this.exportRecordMapper = exportRecordMapper;
        this.messageQueueRepository = messageQueueRepository;
        this.messageProducer = messageProducer;
        this.messagingProperties = messagingProperties;
    }

    public GovernanceAuditTrailService(GovernanceProtectedPersistenceService governanceProtectedPersistenceService,
                                       ConfigSnapshotMapper configSnapshotMapper,
                                       ExecutionResultMapper executionResultMapper,
                                       QueryHistoryMapper queryHistoryMapper,
                                       ExportRecordMapper exportRecordMapper,
                                       MessageProducer messageProducer,
                                       MessagingProperties messagingProperties) {
        this(
            governanceProtectedPersistenceService,
            configSnapshotMapper,
            executionResultMapper,
            queryHistoryMapper,
            exportRecordMapper,
            null,
            messageProducer,
            messagingProperties
        );
    }

    public AuditWriteResponse writeAudit(AuditWriteRequest request) {
        String tenantId = requireContextValue(RequestContext.getTenantId(), "tenantId");
        String userId = requireContextValue(RequestContext.getUserId(), "userId");
        String requestId = requireContextValue(RequestContext.getRequestId(), "requestId");
        String traceId = requireContextValue(RequestContext.getTraceId(), "traceId");

        String serviceCode = requireAuditText(request == null ? null : request.getServiceCode(), "serviceCode");
        String operationCode = requireAuditText(request == null ? null : request.getOperationCode(), "operationCode");
        String resourceType = requireAuditText(request == null ? null : request.getResourceType(), "resourceType");
        String resourceId = requireAuditText(request == null ? null : request.getResourceId(), "resourceId");
        String resultStatus = requireAuditText(request == null ? null : request.getResultStatus(), "resultStatus");
        long elapsedMs = requireAuditElapsedMs(request == null ? null : request.getElapsedMs());
        String sourceIp = requireAuditText(request == null ? null : request.getSourceIp(), "sourceIp");
        String userAgent = requireAuditText(request == null ? null : request.getUserAgent(), "userAgent");

        validateTraceabilityReferences(request);

        AuditLogRecord auditLogRecord = buildAuditLogRecord(
            tenantId,
            serviceCode,
            operationCode,
            resourceType,
            resourceId,
            requestId,
            traceId,
            request == null ? null : request.getSagaId(),
            request == null ? null : request.getConfigSnapshotId(),
            request == null ? null : request.getResultId(),
            request == null ? null : request.getHistoryId(),
            request == null ? null : request.getExportId(),
            request == null ? null : request.getRequestParams(),
            request == null ? null : request.getResponseSummary(),
            resultStatus,
            elapsedMs
        );
        governanceProtectedPersistenceService.saveAuditLog(auditLogRecord);
        publishAuditEvent(tenantId, userId, requestId, traceId, serviceCode, operationCode, resourceType, resourceId,
            resultStatus, elapsedMs, sourceIp, userAgent);
        LOGGER.info("Persisted governance audit record, auditId={}, serviceCode={}, operationCode={}, status={}",
            auditLogRecord.getId(), serviceCode, operationCode, resultStatus);
        return new AuditWriteResponse(
            auditLogRecord.getId(),
            serviceCode,
            operationCode,
            STATUS_ACCEPTED,
            GovernanceMessagingTopics.AUDIT_EVENT,
            requireMessagingMode().name(),
            CONTRACT_STAGE_LONG_TERM_BASELINE,
            IMPLEMENTATION_STAGE_DATABASE_AUDIT_WRITE_BASELINE
        );
    }

    public void recordAuthenticationAccepted(HttpServletRequest request) {
        persistAuthenticationEvent(request, AUTH_LOGIN_OPERATION, "SUCCESS", AUTH_LOGIN_SUCCESS_SUMMARY, null);
    }

    public void recordAuthenticationRejected(HttpServletRequest request, RuntimeException failure) {
        String summary = AUTH_LOGIN_FAILURE_SUMMARY;
        if (failure != null && StringUtils.hasText(failure.getMessage())) {
            summary = failure.getMessage();
        }
        persistAuthenticationEvent(request, AUTH_LOGIN_OPERATION, "FAILED", summary, failure);
    }

    public void recordAuthenticationReleased(HttpServletRequest request, Exception completionError) {
        persistAuthenticationEvent(request, AUTH_LOGOUT_OPERATION,
            completionError == null ? "SUCCESS" : "PARTIAL", AUTH_LOGOUT_SUMMARY, completionError);
    }

    private void persistAuthenticationEvent(HttpServletRequest request,
                                            String operationType,
                                            String resultStatus,
                                            String responseSummary,
                                            Exception error) {
        String tenantId = firstNonBlank(RequestContext.getTenantId(), trimToNull(headerValue(request, RequestHeaderConstants.TENANT_ID)), UNKNOWN_VALUE);
        String requestId = firstNonBlank(RequestContext.getRequestId(), trimToNull(headerValue(request, RequestHeaderConstants.REQUEST_ID)), generateFallbackCorrelationId("request"));
        String traceId = firstNonBlank(RequestContext.getTraceId(), trimToNull(headerValue(request, RequestHeaderConstants.TRACE_ID)), generateFallbackCorrelationId("trace"));
        String roleCodes = trimToNull(headerValue(request, RequestHeaderConstants.ROLE_CODES));
        String authSource = trimToNull(headerValue(request, RequestHeaderConstants.AUTH_SOURCE));
        String requestUri = request == null ? UNKNOWN_VALUE : request.getRequestURI();
        String userId = firstNonBlank(RequestContext.getUserId(), trimToNull(headerValue(request, RequestHeaderConstants.USER_ID)), UNKNOWN_VALUE);
        Map<String, String> authPayload = new HashMap<String, String>();
        authPayload.put("uri", requestUri);
        authPayload.put("method", request == null ? UNKNOWN_VALUE : request.getMethod());
        authPayload.put("authSource", firstNonBlank(authSource, UNKNOWN_VALUE));
        authPayload.put("roleCodes", firstNonBlank(roleCodes, UNKNOWN_VALUE));
        authPayload.put("sourceIp", resolveSourceIp(request));
        authPayload.put("userAgent", resolveUserAgent(request));
        if (error != null && StringUtils.hasText(error.getMessage())) {
            authPayload.put("error", error.getMessage());
        }

        AuditLogRecord auditLogRecord = buildAuditLogRecord(
            tenantId,
            ServiceCodeConstants.GOVERNANCE,
            operationType,
            AUTH_TARGET_TYPE,
            requestUri,
            requestId,
            traceId,
            null,
            null,
            null,
            null,
            null,
            JsonUtils.toJson(authPayload),
            responseSummary,
            resultStatus,
            0L
        );
        governanceProtectedPersistenceService.saveAuditLog(auditLogRecord);
        AuditContext.set(new AuditEvent(
            DateUtils.format(DateUtils.now()),
            tenantId,
            userId,
            ServiceCodeConstants.GOVERNANCE,
            operationType,
            AUTH_TARGET_TYPE,
            requestUri,
            resultStatus,
            0L,
            traceId,
            requestId,
            resolveSourceIp(request),
            resolveUserAgent(request)
        ));
        LOGGER.info("Persisted authentication audit record, auditId={}, operationType={}, status={}, uri={}",
            auditLogRecord.getId(), operationType, resultStatus, requestUri);
    }

    private void publishAuditEvent(String tenantId,
                                   String userId,
                                   String requestId,
                                   String traceId,
                                   String serviceCode,
                                   String operationCode,
                                   String resourceType,
                                   String resourceId,
                                   String resultStatus,
                                   long elapsedMs,
                                   String sourceIp,
                                   String userAgent) {
        AuditEvent auditEvent = new AuditEvent(
            DateUtils.format(DateUtils.now()),
            tenantId,
            userId,
            serviceCode,
            operationCode,
            resourceType,
            resourceId,
            resultStatus,
            elapsedMs,
            traceId,
            requestId,
            sourceIp,
            userAgent
        );
        AuditContext.set(auditEvent);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("traceId", traceId);
        headers.put("requestId", requestId);
        try {
            messageProducer.send(
                GovernanceMessagingTopics.AUDIT_EVENT,
                tenantId,
                JsonUtils.toJson(auditEvent),
                headers
            );
        } catch (RuntimeException ex) {
            enqueueAuditFallback(tenantId, auditEvent, headers, ex);
        }
    }

    private void enqueueAuditFallback(String tenantId,
                                      AuditEvent auditEvent,
                                      Map<String, String> headers,
                                      RuntimeException failure) {
        try {
            MessageQueueRecord fallbackMessage = MessageQueueRecord.pending(
                GovernanceMessagingTopics.AUDIT_EVENT,
                tenantId,
                JsonUtils.toJson(auditEvent),
                JsonUtils.toJson(headers)
            );
            if (messageQueueRepository == null) {
                throw failure;
            }
            messageQueueRepository.enqueueMessage(fallbackMessage);
            LOGGER.warn("Primary audit message delivery failed, queued fallback message, tenantId={}, reason={}",
                tenantId, failure.getMessage());
        } catch (RuntimeException fallbackEx) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_SYSTEM_MESSAGE_ROUTE_INVALID,
                HttpStatus.SERVICE_UNAVAILABLE,
                GOVERNANCE_AUDIT_ROUTE_UNAVAILABLE_MESSAGE,
                fallbackEx
            );
        }
    }

    private void validateTraceabilityReferences(AuditWriteRequest request) {
        if (request == null) {
            return;
        }
        if (StringUtils.hasText(request.getConfigSnapshotId())) {
            ConfigSnapshotRecord record = configSnapshotMapper.selectById(request.getConfigSnapshotId());
            if (record == null) {
                throw invalidAuditReference("configSnapshotId", request.getConfigSnapshotId());
            }
        }
        if (StringUtils.hasText(request.getResultId())) {
            ExecutionResultRecord record = executionResultMapper.selectById(request.getResultId());
            if (record == null) {
                throw invalidAuditReference("resultId", request.getResultId());
            }
        }
        if (StringUtils.hasText(request.getHistoryId())) {
            QueryHistoryRecord record = queryHistoryMapper.selectById(request.getHistoryId());
            if (record == null) {
                throw invalidAuditReference("historyId", request.getHistoryId());
            }
        }
        if (StringUtils.hasText(request.getExportId())) {
            ExportRecord record = exportRecordMapper.selectById(request.getExportId());
            if (record == null) {
                throw invalidAuditReference("exportId", request.getExportId());
            }
        }
    }

    private AuditLogRecord buildAuditLogRecord(String tenantId,
                                               String serviceCode,
                                               String operationType,
                                               String targetType,
                                               String targetId,
                                               String requestId,
                                               String traceId,
                                               String sagaId,
                                               String configSnapshotId,
                                               String resultId,
                                               String historyId,
                                               String exportId,
                                               String requestParams,
                                               String responseSummary,
                                               String status,
                                               long costMs) {
        AuditLogRecord auditLogRecord = new AuditLogRecord();
        auditLogRecord.setTenantId(tenantId);
        auditLogRecord.setServiceCode(serviceCode);
        auditLogRecord.setOperationType(operationType);
        auditLogRecord.setTargetType(targetType);
        auditLogRecord.setTargetId(targetId);
        auditLogRecord.setRequestId(requestId);
        auditLogRecord.setTraceId(traceId);
        auditLogRecord.setSagaId(trimToNull(sagaId));
        auditLogRecord.setConfigSnapshotId(trimToNull(configSnapshotId));
        auditLogRecord.setResultId(trimToNull(resultId));
        auditLogRecord.setHistoryId(trimToNull(historyId));
        auditLogRecord.setExportId(trimToNull(exportId));
        auditLogRecord.setRequestParams(trimToNull(requestParams));
        auditLogRecord.setResponseSummary(trimToNull(responseSummary));
        auditLogRecord.setStatus(status);
        auditLogRecord.setCostMs(Long.valueOf(costMs));
        auditLogRecord.setCreateTime(DateUtils.now());
        return auditLogRecord;
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

    private String requireContextValue(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "Protected request context is missing: " + fieldName
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

    private BizException invalidAuditReference(String fieldName, String fieldValue) {
        return new BizException(
            ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
            HttpStatus.BAD_REQUEST,
            fieldName + " does not reference an existing traceability record: " + fieldValue
        );
    }

    private String resolveSourceIp(HttpServletRequest request) {
        String forwarded = headerValue(request, "X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.trim();
        }
        return request == null ? UNKNOWN_VALUE : firstNonBlank(trimToNull(request.getRemoteAddr()), UNKNOWN_VALUE);
    }

    private String resolveUserAgent(HttpServletRequest request) {
        return request == null ? UNKNOWN_VALUE : firstNonBlank(trimToNull(request.getHeader("User-Agent")), UNKNOWN_VALUE);
    }

    private String headerValue(HttpServletRequest request, String headerName) {
        return request == null ? null : request.getHeader(headerName);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String generateFallbackCorrelationId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString();
    }

    private String firstNonBlank(String first, String second, String third) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return third;
    }

    private String firstNonBlank(String first, String second) {
        return firstNonBlank(first, second, second);
    }
}
