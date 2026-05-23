package com.company.governance.application.service;

import com.company.governance.application.controller.dto.AuditWriteRequest;
import com.company.governance.application.controller.vo.AuditWriteResponse;
import com.company.governance.config.GovernanceAuditProperties;
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
import com.company.sqlforge.common.access.AccessAuditContract;
import com.company.sqlforge.common.access.AccessChannel;
import com.company.sqlforge.common.audit.AuditContext;
import com.company.sqlforge.common.audit.AuditEvent;
import com.company.sqlforge.common.config.MessagingMode;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.RequestMetadataContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.DateUtils;
import com.company.sqlforge.common.utils.JsonUtils;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final String AUTH_LOGIN_FAILURE_SUMMARY = "建立受保护上下文前，认证请求已被拒绝";
    private static final String AUTH_LOGIN_SUCCESS_SUMMARY = "已通过无状态请求头认证建立受保护请求上下文";
    private static final String AUTH_LOGOUT_SUMMARY = "请求完成后已释放受保护请求上下文";
    private static final String GOVERNANCE_AUDIT_ROUTE_UNAVAILABLE_MESSAGE =
        "治理审计契约路由不可用";
    private static final String UNKNOWN_VALUE = "UNKNOWN";

    private final GovernanceProtectedPersistenceService governanceProtectedPersistenceService;
    private final ConfigSnapshotMapper configSnapshotMapper;
    private final ExecutionResultMapper executionResultMapper;
    private final QueryHistoryMapper queryHistoryMapper;
    private final ExportRecordMapper exportRecordMapper;
    private final MessageQueueRepository messageQueueRepository;
    private final MessageProducer messageProducer;
    private final MessagingProperties messagingProperties;
    private final GovernanceAuditProperties governanceAuditProperties;
    private final GovernanceMetricsRecorder metricsRecorder;

    @Autowired
    public GovernanceAuditTrailService(GovernanceProtectedPersistenceService governanceProtectedPersistenceService,
                                       ConfigSnapshotMapper configSnapshotMapper,
                                       ExecutionResultMapper executionResultMapper,
                                       QueryHistoryMapper queryHistoryMapper,
                                       ExportRecordMapper exportRecordMapper,
                                       MessageQueueRepository messageQueueRepository,
                                       MessageProducer messageProducer,
                                       MessagingProperties messagingProperties,
                                       GovernanceAuditProperties governanceAuditProperties,
                                       GovernanceMetricsRecorder metricsRecorder) {
        this.governanceProtectedPersistenceService = governanceProtectedPersistenceService;
        this.configSnapshotMapper = configSnapshotMapper;
        this.executionResultMapper = executionResultMapper;
        this.queryHistoryMapper = queryHistoryMapper;
        this.exportRecordMapper = exportRecordMapper;
        this.messageQueueRepository = messageQueueRepository;
        this.messageProducer = messageProducer;
        this.messagingProperties = messagingProperties;
        this.governanceAuditProperties = governanceAuditProperties;
        this.metricsRecorder = metricsRecorder;
    }

    public GovernanceAuditTrailService(GovernanceProtectedPersistenceService governanceProtectedPersistenceService,
                                       ConfigSnapshotMapper configSnapshotMapper,
                                       ExecutionResultMapper executionResultMapper,
                                       QueryHistoryMapper queryHistoryMapper,
                                       ExportRecordMapper exportRecordMapper,
                                       MessageQueueRepository messageQueueRepository,
                                       MessageProducer messageProducer,
                                       MessagingProperties messagingProperties,
                                       GovernanceAuditProperties governanceAuditProperties) {
        this(
            governanceProtectedPersistenceService,
            configSnapshotMapper,
            executionResultMapper,
            queryHistoryMapper,
            exportRecordMapper,
            messageQueueRepository,
            messageProducer,
            messagingProperties,
            governanceAuditProperties,
            GovernanceMetricsRecorder.noop()
        );
    }

    public GovernanceAuditTrailService(GovernanceProtectedPersistenceService governanceProtectedPersistenceService,
                                       ConfigSnapshotMapper configSnapshotMapper,
                                       ExecutionResultMapper executionResultMapper,
                                       QueryHistoryMapper queryHistoryMapper,
                                       ExportRecordMapper exportRecordMapper,
                                       MessageProducer messageProducer,
                                       MessagingProperties messagingProperties,
                                       GovernanceAuditProperties governanceAuditProperties) {
        this(
            governanceProtectedPersistenceService,
            configSnapshotMapper,
            executionResultMapper,
            queryHistoryMapper,
            exportRecordMapper,
            null,
            messageProducer,
            messagingProperties,
            governanceAuditProperties,
            GovernanceMetricsRecorder.noop()
        );
    }

    public AuditWriteResponse writeAudit(AuditWriteRequest request) {
        String tenantId = requireContextValue(RequestContext.getTenantId(), "tenantId");
        String userId = requireContextValue(RequestContext.getUserId(), "userId");
        String requestId = requireContextValue(RequestContext.getRequestId(), "requestId");
        String traceId = requireContextValue(RequestContext.getTraceId(), "traceId");
        AccessAuditContract accessAuditContract = resolveAccessAuditContract(request);

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
            mergeRequestParams(request == null ? null : request.getRequestParams(), accessAuditContract),
            request == null ? null : request.getResponseSummary(),
            resultStatus,
            elapsedMs
        );
        governanceProtectedPersistenceService.saveAuditLog(auditLogRecord);
        publishAuditEvent(tenantId, userId, requestId, traceId, serviceCode, operationCode, resourceType, resourceId,
            resultStatus, elapsedMs, accessAuditContract.getAccessChannel().name(), accessAuditContract.getAuthSource(),
            sourceIp, userAgent);
        LOGGER.info("治理审计记录已持久化，auditId={}, serviceCode={}, operationCode={}, status={}",
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
        String authSource = trimToNull(headerValue(request, RequestHeaderConstants.AUTH_SOURCE));
        AccessChannel accessChannel = resolveAccessChannel(trimToNull(headerValue(request, RequestHeaderConstants.ACCESS_CHANNEL)), authSource);
        String requestUri = request == null ? UNKNOWN_VALUE : request.getRequestURI();
        String userId = firstNonBlank(RequestContext.getUserId(), trimToNull(headerValue(request, RequestHeaderConstants.USER_ID)), UNKNOWN_VALUE);
        Map<String, String> authPayload = new HashMap<String, String>();
        authPayload.put("uri", requestUri);
        authPayload.put("method", request == null ? UNKNOWN_VALUE : request.getMethod());
        authPayload.put("accessChannel", accessChannel.name());
        authPayload.put("authSource", firstNonBlank(authSource, UNKNOWN_VALUE));
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
            accessChannel.name(),
            firstNonBlank(authSource, UNKNOWN_VALUE),
            resolveSourceIp(request),
            resolveUserAgent(request)
        ));
        LOGGER.info("认证审计记录已持久化，auditId={}, operationType={}, status={}, uri={}",
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
                                   String accessChannel,
                                   String authSource,
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
            accessChannel,
            authSource,
            sourceIp,
            userAgent
        );
        AuditContext.set(auditEvent);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("traceId", traceId);
        headers.put("requestId", requestId);
        try {
            if (shouldForcePrimaryDeliveryFailure(traceId)) {
                throw new IllegalStateException("用于 smoke 验证的主审计投递模拟失败");
            }
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

    private boolean shouldForcePrimaryDeliveryFailure(String traceId) {
        return governanceAuditProperties.isSmokeForcePrimaryDeliveryFailureEnabled()
            && StringUtils.hasText(governanceAuditProperties.getSmokeForceTracePrefix())
            && StringUtils.hasText(traceId)
            && traceId.startsWith(governanceAuditProperties.getSmokeForceTracePrefix());
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
            metricsRecorder.recordAuditFallback();
            LOGGER.warn("主审计消息投递失败，已写入兜底队列，tenantId={}, reason={}",
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

    private AccessAuditContract resolveAccessAuditContract(AuditWriteRequest request) {
        AccessAuditContract baseline = AccessAuditContract.capture();
        AccessChannel accessChannel = resolveAccessChannel(
            request == null ? null : request.getAccessChannel(),
            request == null ? null : request.getAuthSource()
        );
        return new AccessAuditContractView(
            accessChannel,
            trimToNull(request == null ? null : request.getAuthSource()),
            baseline
        ).toContract();
    }

    private AccessChannel resolveAccessChannel(String requestAccessChannel, String requestAuthSource) {
        AccessChannel accessChannel = AccessChannel.fromWireValue(requestAccessChannel);
        if (accessChannel != null) {
            return accessChannel;
        }
        if (StringUtils.hasText(requestAccessChannel)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
                HttpStatus.BAD_REQUEST,
                "accessChannel 必须是 PAGE/API/JDBC_AGENT/SDK/CLIENT 之一"
            );
        }
        accessChannel = AccessChannel.fromWireValue(RequestMetadataContext.getAccessChannel());
        if (accessChannel != null) {
            return accessChannel;
        }
        String authSource = firstNonBlank(trimToNull(requestAuthSource), trimToNull(RequestContext.getAuthSource()), null);
        return StringUtils.hasText(authSource) ? AccessChannel.API : AccessChannel.API;
    }

    private String mergeRequestParams(String rawRequestParams, AccessAuditContract accessAuditContract) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("accessChannel", accessAuditContract.getAccessChannel().name());
        payload.put("authSource", accessAuditContract.getAuthSource());
        payload.put("tenantId", accessAuditContract.getTenantId());
        payload.put("userId", accessAuditContract.getUserId());
        payload.put("requestId", accessAuditContract.getRequestId());
        payload.put("traceId", accessAuditContract.getTraceId());
        payload.put("sourceIp", accessAuditContract.getSourceIp());
        payload.put("userAgent", accessAuditContract.getUserAgent());
        if (!StringUtils.hasText(rawRequestParams)) {
            return JsonUtils.toJson(payload);
        }
        Map<?, ?> existing = null;
        try {
            existing = JsonUtils.fromJson(rawRequestParams, Map.class);
        } catch (IllegalArgumentException ex) {
            payload.put("requestPayload", rawRequestParams);
            return JsonUtils.toJson(payload);
        }
        if (existing == null) {
            payload.put("requestPayload", rawRequestParams);
            return JsonUtils.toJson(payload);
        }
        for (Map.Entry<?, ?> entry : existing.entrySet()) {
            if (entry.getKey() != null && !payload.containsKey(String.valueOf(entry.getKey()))) {
                payload.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return JsonUtils.toJson(payload);
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
                "消息模式未配置"
            );
        }
        return messagingMode;
    }

    private String requireContextValue(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "受保护请求上下文缺失：" + fieldName
            );
        }
        return value;
    }

    private String requireAuditText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
                HttpStatus.BAD_REQUEST,
                fieldName + " 不能为空"
            );
        }
        return value;
    }

    private long requireAuditElapsedMs(Long elapsedMs) {
        if (elapsedMs == null || elapsedMs.longValue() < 0L) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
                HttpStatus.BAD_REQUEST,
                "elapsedMs 不能为负数"
            );
        }
        return elapsedMs.longValue();
    }

    private BizException invalidAuditReference(String fieldName, String fieldValue) {
        return new BizException(
            ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
            HttpStatus.BAD_REQUEST,
            fieldName + " 未引用已存在的追溯记录：" + fieldValue
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

    private static final class AccessAuditContractView {

        private final AccessChannel accessChannel;
        private final String authSource;
        private final AccessAuditContract baseline;

        private AccessAuditContractView(AccessChannel accessChannel, String authSource, AccessAuditContract baseline) {
            this.accessChannel = accessChannel;
            this.authSource = authSource;
            this.baseline = baseline;
        }

        private AccessAuditContract toContract() {
            return new AccessAuditContract(
                accessChannel,
                StringUtils.hasText(authSource) ? authSource : baseline.getAuthSource(),
                baseline.getTenantId(),
                baseline.getUserId(),
                baseline.getRequestId(),
                baseline.getTraceId(),
                baseline.getSourceIp(),
                baseline.getUserAgent()
            );
        }
    }
}
