package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.application.controller.dto.AuditWriteRequest;
import com.company.governance.application.controller.vo.AuditWriteResponse;
import com.company.governance.config.MessagingProperties;
import com.company.governance.domain.messaging.MessageProducer;
import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.governance.domain.trace.entity.ConfigSnapshotRecord;
import com.company.governance.domain.trace.entity.ExecutionResultRecord;
import com.company.governance.domain.trace.entity.ExportRecord;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.infrastructure.messaging.GovernanceMessagingTopics;
import com.company.governance.infrastructure.persistence.mapper.AuditLogMapper;
import com.company.governance.infrastructure.persistence.mapper.ConfigSnapshotMapper;
import com.company.governance.infrastructure.persistence.mapper.ExecutionResultMapper;
import com.company.governance.infrastructure.persistence.mapper.ExportRecordMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.sqlforge.common.audit.AuditContext;
import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.MessagingMode;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

class GovernanceAuditTrailServiceTest {

    @AfterEach
    void tearDown() {
        AuditContext.clear();
        RequestContext.clear();
    }

    @Test
    void shouldPersistAndPublishSqlAuditEventWithTraceabilityReferences() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        ConfigSnapshotMapper configSnapshotMapper = mock(ConfigSnapshotMapper.class);
        ExecutionResultMapper executionResultMapper = mock(ExecutionResultMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        MessageProducer messageProducer = mock(MessageProducer.class);
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.DATABASE);
        GovernanceAuditTrailService service = new GovernanceAuditTrailService(
            auditLogMapper,
            configSnapshotMapper,
            executionResultMapper,
            queryHistoryMapper,
            exportRecordMapper,
            messageProducer,
            messagingProperties
        );
        RequestContext.set(
            "tenant-a",
            "user-01",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            100L,
            200L
        );
        when(configSnapshotMapper.selectById("cfg-001")).thenReturn(new ConfigSnapshotRecord());
        when(executionResultMapper.selectById("res-001")).thenReturn(new ExecutionResultRecord());
        when(queryHistoryMapper.selectById("hist-001")).thenReturn(new QueryHistoryRecord());
        when(exportRecordMapper.selectById("exp-001")).thenReturn(new ExportRecord());
        doAnswer(invocation -> {
            AuditLogRecord record = invocation.getArgument(0);
            record.setId(Long.valueOf(9001L));
            return 1;
        }).when(auditLogMapper).insert(org.mockito.ArgumentMatchers.any(AuditLogRecord.class));

        AuditWriteRequest request = new AuditWriteRequest();
        request.setServiceCode("QUERY_EXECUTION");
        request.setOperationCode("QUERY_EXECUTE_SYNC");
        request.setResourceType("SQL_QUERY");
        request.setResourceId("query-001");
        request.setResultStatus("SUCCESS");
        request.setElapsedMs(42L);
        request.setSourceIp("127.0.0.1");
        request.setUserAgent("JUnit");
        request.setSagaId("saga-001");
        request.setConfigSnapshotId("cfg-001");
        request.setResultId("res-001");
        request.setHistoryId("hist-001");
        request.setExportId("exp-001");
        request.setRequestParams("{\"sqlFingerprint\":\"abc\"}");
        request.setResponseSummary("query executed");

        AuditWriteResponse response = service.writeAudit(request);

        assertEquals(Long.valueOf(9001L), response.getAuditId());
        assertEquals("DATABASE_AUDIT_WRITE_BASELINE", response.getImplementationStage());
        verify(messageProducer).send(
            eq(GovernanceMessagingTopics.AUDIT_EVENT),
            eq("tenant-a"),
            contains("\"operationCode\":\"QUERY_EXECUTE_SYNC\""),
            anyMap()
        );
        ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(auditLogMapper).insert(captor.capture());
        AuditLogRecord inserted = captor.getValue();
        assertEquals("cfg-001", inserted.getConfigSnapshotId());
        assertEquals("res-001", inserted.getResultId());
        assertEquals("hist-001", inserted.getHistoryId());
        assertEquals("exp-001", inserted.getExportId());
        assertEquals("QUERY_EXECUTE_SYNC", inserted.getOperationType());
    }

    @Test
    void shouldPersistPermissionChangeAuditThroughSameWritePath() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        MessageProducer messageProducer = mock(MessageProducer.class);
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.MOCK);
        GovernanceAuditTrailService service = new GovernanceAuditTrailService(
            auditLogMapper,
            mock(ConfigSnapshotMapper.class),
            mock(ExecutionResultMapper.class),
            mock(QueryHistoryMapper.class),
            mock(ExportRecordMapper.class),
            messageProducer,
            messagingProperties
        );
        RequestContext.set(
            "tenant-a",
            "platform-admin-01",
            Arrays.asList("PLATFORM_ADMIN"),
            "request-010",
            "trace-010",
            "gateway",
            100L,
            200L
        );
        doAnswer(invocation -> {
            AuditLogRecord record = invocation.getArgument(0);
            record.setId(Long.valueOf(12L));
            return 1;
        }).when(auditLogMapper).insert(org.mockito.ArgumentMatchers.any(AuditLogRecord.class));

        AuditWriteRequest request = new AuditWriteRequest();
        request.setServiceCode("GOVERNANCE");
        request.setOperationCode("PERMISSION_CHANGE");
        request.setResourceType("ROLE_BINDING");
        request.setResourceId("rbac-001");
        request.setResultStatus("SUCCESS");
        request.setElapsedMs(11L);
        request.setSourceIp("127.0.0.1");
        request.setUserAgent("JUnit");
        request.setResponseSummary("platform admin updated tenant role binding");

        AuditWriteResponse response = service.writeAudit(request);

        assertEquals(Long.valueOf(12L), response.getAuditId());
        assertEquals("PERMISSION_CHANGE", response.getOperationCode());
    }

    @Test
    void shouldRejectWhenTraceabilityReferenceIsMissing() {
        GovernanceAuditTrailService service = new GovernanceAuditTrailService(
            mock(AuditLogMapper.class),
            mock(ConfigSnapshotMapper.class),
            mock(ExecutionResultMapper.class),
            mock(QueryHistoryMapper.class),
            mock(ExportRecordMapper.class),
            mock(MessageProducer.class),
            databaseMessaging()
        );
        RequestContext.set(
            "tenant-a",
            "user-01",
            Arrays.asList("TENANT_ADMIN"),
            "request-011",
            "trace-011",
            "header",
            100L,
            200L
        );

        AuditWriteRequest request = new AuditWriteRequest();
        request.setServiceCode("QUERY_EXECUTION");
        request.setOperationCode("QUERY_EXECUTE_SYNC");
        request.setResourceType("SQL_QUERY");
        request.setResourceId("query-001");
        request.setResultStatus("FAILED");
        request.setElapsedMs(7L);
        request.setSourceIp("127.0.0.1");
        request.setUserAgent("JUnit");
        request.setConfigSnapshotId("missing-cfg");

        BizException ex = assertThrows(BizException.class, () -> service.writeAudit(request));

        assertEquals(ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID, ex.getCode());
    }

    @Test
    void shouldRecordAuthenticationLoginAndLogoutLocally() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        GovernanceAuditTrailService service = new GovernanceAuditTrailService(
            auditLogMapper,
            mock(ConfigSnapshotMapper.class),
            mock(ExecutionResultMapper.class),
            mock(QueryHistoryMapper.class),
            mock(ExportRecordMapper.class),
            mock(MessageProducer.class),
            databaseMessaging()
        );
        doAnswer(invocation -> {
            AuditLogRecord record = invocation.getArgument(0);
            record.setId(Long.valueOf(100L));
            return 1;
        }).when(auditLogMapper).insert(org.mockito.ArgumentMatchers.any(AuditLogRecord.class));
        RequestContext.set(
            "system",
            "operator-001",
            Arrays.asList("TENANT_ADMIN", "OPERATOR"),
            "request-020",
            "trace-020",
            "header",
            100L,
            200L
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/governance/tenant-config");
        request.addHeader("User-Agent", "JUnit");
        request.setRemoteAddr("127.0.0.1");

        service.recordAuthenticationAccepted(request);
        service.recordAuthenticationReleased(request, null);

        ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(auditLogMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals("LOGIN", captor.getAllValues().get(0).getOperationType());
        assertEquals("LOGOUT", captor.getAllValues().get(1).getOperationType());
    }

    @Test
    void shouldRecordAuthenticationFailureWithFallbackTraceability() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        GovernanceAuditTrailService service = new GovernanceAuditTrailService(
            auditLogMapper,
            mock(ConfigSnapshotMapper.class),
            mock(ExecutionResultMapper.class),
            mock(QueryHistoryMapper.class),
            mock(ExportRecordMapper.class),
            mock(MessageProducer.class),
            databaseMessaging()
        );
        doAnswer(invocation -> {
            AuditLogRecord record = invocation.getArgument(0);
            record.setId(Long.valueOf(200L));
            return 1;
        }).when(auditLogMapper).insert(org.mockito.ArgumentMatchers.any(AuditLogRecord.class));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/governance/tenant-config");
        request.addHeader(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER);
        request.addHeader("User-Agent", "JUnit");
        request.setRemoteAddr("127.0.0.1");

        service.recordAuthenticationRejected(request, new IllegalStateException("Missing X-Tenant-Id header"));

        ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(auditLogMapper).insert(captor.capture());
        AuditLogRecord inserted = captor.getValue();
        assertEquals("LOGIN", inserted.getOperationType());
        assertEquals("FAILED", inserted.getStatus());
        assertNotNull(inserted.getRequestId());
        assertNotNull(inserted.getTraceId());
    }

    private MessagingProperties databaseMessaging() {
        MessagingProperties messagingProperties = new MessagingProperties();
        messagingProperties.setMode(MessagingMode.DATABASE);
        return messagingProperties;
    }
}
