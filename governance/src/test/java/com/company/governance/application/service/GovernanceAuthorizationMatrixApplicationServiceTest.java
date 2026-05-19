package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.company.governance.application.controller.dto.AuditWriteRequest;
import com.company.governance.application.controller.dto.DatasourceAuthorizationChangeRequest;
import com.company.governance.application.controller.vo.DatasourceAuthorizationChangeResponse;
import com.company.governance.config.GovernanceAccessProperties;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.governance.GovernanceAuthorizationDecisionRequest;
import com.company.sqlforge.common.governance.GovernanceAuthorizationDecisionResponse;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GovernanceAuthorizationMatrixApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldAllowAuthorizedRequestAndAuditSuccess() {
        GovernanceAuditTrailService auditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceAuthorizationMatrixApplicationService service = new GovernanceAuthorizationMatrixApplicationService(
            new GovernanceAccessProperties(),
            auditTrailService
        );
        service.initializeRuntimeDatasourceMatrix();
        RequestContext.set(
            "tenant-a",
            "tenant-admin-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            100L,
            200L
        );

        GovernanceAuthorizationDecisionResponse response = service.decideAuthorization(queryExecutionRequest("tenant-a"));

        assertTrue(response.isAllowed());
        assertEquals("ALLOWED", response.getReason());
        ArgumentCaptor<AuditWriteRequest> captor = ArgumentCaptor.forClass(AuditWriteRequest.class);
        verify(auditTrailService).writeAudit(captor.capture());
        assertEquals("AUTHORIZATION_DECISION", captor.getValue().getOperationCode());
        assertEquals("SUCCESS", captor.getValue().getResultStatus());
    }

    @Test
    void shouldAllowRewriteRecordListForHistoryReaders() {
        GovernanceAuditTrailService auditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceAuthorizationMatrixApplicationService service = new GovernanceAuthorizationMatrixApplicationService(
            new GovernanceAccessProperties(),
            auditTrailService
        );
        service.initializeRuntimeDatasourceMatrix();
        RequestContext.set(
            "tenant-a",
            "auditor-001",
            Arrays.asList("AUDITOR"),
            "request-rewrite-record-list",
            "trace-rewrite-record-list",
            "header",
            100L,
            200L
        );

        GovernanceAuthorizationDecisionResponse response =
            service.decideAuthorization(rewriteRecordListRequest("tenant-a"));

        assertTrue(response.isAllowed());
        assertEquals("ALLOWED", response.getReason());
    }

    @Test
    void shouldDenyWhenRolePermissionMissingAndAuditFailure() {
        GovernanceAuditTrailService auditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceAuthorizationMatrixApplicationService service = new GovernanceAuthorizationMatrixApplicationService(
            new GovernanceAccessProperties(),
            auditTrailService
        );
        service.initializeRuntimeDatasourceMatrix();
        RequestContext.set(
            "tenant-a",
            "readonly-001",
            Arrays.asList("READONLY"),
            "request-002",
            "trace-002",
            "header",
            100L,
            200L
        );

        GovernanceAuthorizationDecisionResponse response = service.decideAuthorization(queryExecutionRequest("tenant-a"));

        assertFalse(response.isAllowed());
        assertEquals("ROLE_PERMISSION_DENIED", response.getReason());
        assertEquals(Integer.valueOf(ErrorCodeConstants.GOVERNANCE_ACCESS_DENIED), response.getErrorCode());
        ArgumentCaptor<AuditWriteRequest> captor = ArgumentCaptor.forClass(AuditWriteRequest.class);
        verify(auditTrailService).writeAudit(captor.capture());
        assertEquals("FAILED", captor.getValue().getResultStatus());
    }

    @Test
    void shouldDenyCrossTenantRequest() {
        GovernanceAuditTrailService auditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceAuthorizationMatrixApplicationService service = new GovernanceAuthorizationMatrixApplicationService(
            new GovernanceAccessProperties(),
            auditTrailService
        );
        service.initializeRuntimeDatasourceMatrix();
        RequestContext.set(
            "tenant-a",
            "tenant-admin-002",
            Arrays.asList("TENANT_ADMIN"),
            "request-003",
            "trace-003",
            "header",
            100L,
            200L
        );

        GovernanceAuthorizationDecisionResponse response = service.decideAuthorization(queryExecutionRequest("tenant-b"));

        assertFalse(response.isAllowed());
        assertEquals("CALLER_TENANT_MISMATCH", response.getReason());
        assertEquals(Integer.valueOf(ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED), response.getErrorCode());
    }

    @Test
    void shouldDenyRevokedDatasourceAfterPermissionChangeAndAuditMutation() {
        GovernanceAuditTrailService auditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceAuthorizationMatrixApplicationService service = new GovernanceAuthorizationMatrixApplicationService(
            new GovernanceAccessProperties(),
            auditTrailService
        );
        service.initializeRuntimeDatasourceMatrix();
        RequestContext.set(
            "tenant-a",
            "tenant-admin-003",
            Arrays.asList("TENANT_ADMIN"),
            "request-004",
            "trace-004",
            "header",
            100L,
            200L
        );

        DatasourceAuthorizationChangeRequest changeRequest = new DatasourceAuthorizationChangeRequest();
        changeRequest.setTenantId("tenant-a");
        changeRequest.setDatasourceId("query-hetu");
        changeRequest.setState("REVOKED");
        changeRequest.setChangeReason("incident revoke");

        DatasourceAuthorizationChangeResponse changeResponse =
            service.applyDatasourceAuthorizationChange(changeRequest);
        GovernanceAuthorizationDecisionResponse decisionResponse = service.decideAuthorization(
            queryExecutionRequest("tenant-a")
        );

        assertEquals("UPDATED", changeResponse.getStatus());
        assertFalse(service.isDatasourceActionAllowed("tenant-a", "query-hetu", "USE"));
        assertFalse(decisionResponse.isAllowed());
        assertEquals("DATASOURCE_ACCESS_REVOKED", decisionResponse.getReason());
        assertEquals(
            Integer.valueOf(ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED),
            decisionResponse.getErrorCode()
        );

        ArgumentCaptor<AuditWriteRequest> captor = ArgumentCaptor.forClass(AuditWriteRequest.class);
        verify(auditTrailService, atLeastOnce()).writeAudit(captor.capture());
        List<AuditWriteRequest> auditRequests = captor.getAllValues();
        assertTrue(hasAuditEvent(auditRequests, "PERMISSION_CHANGE", "SUCCESS", "tenant-a:query-hetu"));
        assertTrue(hasAuditEvent(auditRequests, "AUTHORIZATION_DECISION", "FAILED", "query-fingerprint-001"));
    }

    private GovernanceAuthorizationDecisionRequest queryExecutionRequest(String tenantId) {
        GovernanceAuthorizationDecisionRequest request = new GovernanceAuthorizationDecisionRequest();
        request.setServiceCode("QUERY_EXECUTION");
        request.setTenantId(tenantId);
        request.setResourceType("QUERY_EXECUTION_QUERY");
        request.setResourceId("query-fingerprint-001");
        request.setOperationCode("QUERY_EXECUTE_SYNC");
        request.setDatasourceId("query-hetu");
        return request;
    }

    private GovernanceAuthorizationDecisionRequest rewriteRecordListRequest(String tenantId) {
        GovernanceAuthorizationDecisionRequest request = new GovernanceAuthorizationDecisionRequest();
        request.setServiceCode("SQL_OPTIMIZATION");
        request.setTenantId(tenantId);
        request.setResourceType("SQL_REWRITE_RECORD");
        request.setResourceId("history-001");
        request.setOperationCode("SQL_REWRITE_RECORD_LIST");
        request.setDatasourceId("optimization-hetu");
        return request;
    }

    private boolean hasAuditEvent(List<AuditWriteRequest> auditRequests,
                                  String operationCode,
                                  String resultStatus,
                                  String resourceId) {
        for (AuditWriteRequest auditRequest : auditRequests) {
            if (operationCode.equals(auditRequest.getOperationCode())
                && resultStatus.equals(auditRequest.getResultStatus())
                && resourceId.equals(auditRequest.getResourceId())) {
                return true;
            }
        }
        return false;
    }
}
