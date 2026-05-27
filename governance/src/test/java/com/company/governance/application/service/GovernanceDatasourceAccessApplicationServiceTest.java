package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.company.governance.application.controller.dto.AuditWriteRequest;
import com.company.governance.application.controller.dto.DatasourceAccessScopeChangeRequest;
import com.company.governance.application.controller.vo.DatasourceAccessScopeChangeResponse;
import com.company.governance.config.GovernanceAccessProperties;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.governance.GovernanceDatasourceAccessCheckRequest;
import com.company.sqlforge.common.governance.GovernanceDatasourceAccessCheckResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GovernanceDatasourceAccessApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldAllowAuthorizedRequestAndAuditSuccess() {
        GovernanceAuditTrailService auditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceDatasourceAccessApplicationService service = new GovernanceDatasourceAccessApplicationService(
            new GovernanceAccessProperties(),
            auditTrailService
        );
        service.initializeRuntimeDatasourceScopes();
        RequestContext.set(
            "tenant-a",
            "tenant-admin-001",
            "request-001",
            "trace-001",
            "header",
            100L,
            200L
        );

        GovernanceDatasourceAccessCheckResponse response = service.checkDatasourceAccess(queryExecutionRequest("tenant-a"));

        assertTrue(response.isAllowed());
        assertEquals("ALLOWED", response.getReason());
        ArgumentCaptor<AuditWriteRequest> captor = ArgumentCaptor.forClass(AuditWriteRequest.class);
        verify(auditTrailService).writeAudit(captor.capture());
        assertEquals("DATASOURCE_ACCESS_CHECK", captor.getValue().getOperationCode());
        assertEquals("SUCCESS", captor.getValue().getResultStatus());
    }

    @Test
    void shouldAllowRewriteRecordListForHistoryReaders() {
        GovernanceAuditTrailService auditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceDatasourceAccessApplicationService service = new GovernanceDatasourceAccessApplicationService(
            new GovernanceAccessProperties(),
            auditTrailService
        );
        service.initializeRuntimeDatasourceScopes();
        RequestContext.set(
            "tenant-a",
            "auditor-001",
            "request-rewrite-record-list",
            "trace-rewrite-record-list",
            "header",
            100L,
            200L
        );

        GovernanceDatasourceAccessCheckResponse response =
            service.checkDatasourceAccess(rewriteRecordListRequest("tenant-a"));

        assertTrue(response.isAllowed());
        assertEquals("ALLOWED", response.getReason());
    }

    @Test
    void shouldAllowRewriteRecordActivationForSubmitters() {
        GovernanceAuditTrailService auditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceDatasourceAccessApplicationService service = new GovernanceDatasourceAccessApplicationService(
            new GovernanceAccessProperties(),
            auditTrailService
        );
        service.initializeRuntimeDatasourceScopes();
        RequestContext.set(
            "tenant-a",
            "operator-activate-001",
            "request-rewrite-record-activate",
            "trace-rewrite-record-activate",
            "header",
            100L,
            200L
        );

        GovernanceDatasourceAccessCheckResponse response =
            service.checkDatasourceAccess(rewriteRecordActivationRequest("tenant-a"));

        assertTrue(response.isAllowed());
        assertEquals("ALLOWED", response.getReason());
    }

    @Test
    void shouldDenySystemTenantForUnscopedQueryDatasource() {
        GovernanceAuditTrailService auditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceDatasourceAccessApplicationService service = new GovernanceDatasourceAccessApplicationService(
            new GovernanceAccessProperties(),
            auditTrailService
        );
        service.initializeRuntimeDatasourceScopes();
        RequestContext.set(
            "system",
            "system-runtime",
            "request-system-trino",
            "trace-system-trino",
            "header",
            100L,
            200L
        );

        GovernanceDatasourceAccessCheckResponse response =
            service.checkDatasourceAccess(queryExecutionRequest("system", "query-trino"));

        assertFalse(response.isAllowed());
        assertEquals("DATASOURCE_SCOPE_MISSING", response.getReason());
    }

    @Test
    void shouldAllowSystemTenantToChangeDatasourceAccessScope() {
        GovernanceAuditTrailService auditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceDatasourceAccessApplicationService service = new GovernanceDatasourceAccessApplicationService(
            new GovernanceAccessProperties(),
            auditTrailService
        );
        service.initializeRuntimeDatasourceScopes();
        RequestContext.set(
            "system",
            "system-runtime",
            "request-system-permission-change",
            "trace-system-permission-change",
            "header",
            100L,
            200L
        );

        DatasourceAccessScopeChangeRequest changeRequest = new DatasourceAccessScopeChangeRequest();
        changeRequest.setTenantId("tenant-b");
        changeRequest.setDatasourceId("query-trino");
        changeRequest.setState("ACTIVE");
        changeRequest.getActions().add("USE");
        changeRequest.setChangeReason("system bootstrap");

        DatasourceAccessScopeChangeResponse response =
            service.applyDatasourceAccessScopeChange(changeRequest);

        assertEquals("UPDATED", response.getStatus());
        assertTrue(service.isDatasourceActionAllowed("tenant-b", "query-trino", "USE"));
        ArgumentCaptor<AuditWriteRequest> captor = ArgumentCaptor.forClass(AuditWriteRequest.class);
        verify(auditTrailService).writeAudit(captor.capture());
        assertEquals("DATASOURCE_ACCESS_SCOPE_CHANGE", captor.getValue().getOperationCode());
        assertEquals("SUCCESS", captor.getValue().getResultStatus());
        assertEquals("tenant-b:query-trino", captor.getValue().getResourceId());
    }

    @Test
    void shouldDenyWhenDatasourceActionMissingAndAuditFailure() {
        GovernanceAuditTrailService auditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceDatasourceAccessApplicationService service = new GovernanceDatasourceAccessApplicationService(
            new GovernanceAccessProperties(),
            auditTrailService
        );
        service.initializeRuntimeDatasourceScopes();
        RequestContext.set(
            "tenant-a",
            "readonly-001",
            "request-002",
            "trace-002",
            "header",
            100L,
            200L
        );

        GovernanceDatasourceAccessCheckRequest request = queryExecutionRequest("tenant-a");
        request.setAction("EXPORT");
        GovernanceDatasourceAccessCheckResponse response = service.checkDatasourceAccess(request);

        assertFalse(response.isAllowed());
        assertEquals("DATASOURCE_ACTION_DENIED", response.getReason());
        assertEquals(Integer.valueOf(ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED), response.getErrorCode());
        ArgumentCaptor<AuditWriteRequest> captor = ArgumentCaptor.forClass(AuditWriteRequest.class);
        verify(auditTrailService).writeAudit(captor.capture());
        assertEquals("FAILED", captor.getValue().getResultStatus());
    }

    @Test
    void shouldDenyCrossTenantRequest() {
        GovernanceAuditTrailService auditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceDatasourceAccessApplicationService service = new GovernanceDatasourceAccessApplicationService(
            new GovernanceAccessProperties(),
            auditTrailService
        );
        service.initializeRuntimeDatasourceScopes();
        RequestContext.set(
            "tenant-a",
            "tenant-admin-002",
            "request-003",
            "trace-003",
            "header",
            100L,
            200L
        );

        GovernanceDatasourceAccessCheckResponse response = service.checkDatasourceAccess(queryExecutionRequest("tenant-b"));

        assertFalse(response.isAllowed());
        assertEquals("CALLER_TENANT_MISMATCH", response.getReason());
        assertEquals(Integer.valueOf(ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED), response.getErrorCode());
    }

    @Test
    void shouldDenyRevokedDatasourceAfterPermissionChangeAndAuditMutation() {
        GovernanceAuditTrailService auditTrailService = mock(GovernanceAuditTrailService.class);
        GovernanceDatasourceAccessApplicationService service = new GovernanceDatasourceAccessApplicationService(
            new GovernanceAccessProperties(),
            auditTrailService
        );
        service.initializeRuntimeDatasourceScopes();
        RequestContext.set(
            "system",
            "system-runtime",
            "request-004",
            "trace-004",
            "header",
            100L,
            200L
        );

        DatasourceAccessScopeChangeRequest changeRequest = new DatasourceAccessScopeChangeRequest();
        changeRequest.setTenantId("tenant-a");
        changeRequest.setDatasourceId("query-hetu");
        changeRequest.setState("REVOKED");
        changeRequest.setChangeReason("incident revoke");

        DatasourceAccessScopeChangeResponse changeResponse =
            service.applyDatasourceAccessScopeChange(changeRequest);
        RequestContext.set(
            "tenant-a",
            "service-user",
            "request-005",
            "trace-005",
            "header",
            100L,
            200L
        );
        GovernanceDatasourceAccessCheckResponse decisionResponse = service.checkDatasourceAccess(
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
        assertTrue(hasAuditEvent(auditRequests, "DATASOURCE_ACCESS_SCOPE_CHANGE", "SUCCESS", "tenant-a:query-hetu"));
        assertTrue(hasAuditEvent(auditRequests, "DATASOURCE_ACCESS_CHECK", "FAILED", "query-fingerprint-001"));
    }

    private GovernanceDatasourceAccessCheckRequest queryExecutionRequest(String tenantId) {
        return queryExecutionRequest(tenantId, "query-hetu");
    }

    private GovernanceDatasourceAccessCheckRequest queryExecutionRequest(String tenantId, String datasourceId) {
        GovernanceDatasourceAccessCheckRequest request = new GovernanceDatasourceAccessCheckRequest();
        request.setServiceCode("QUERY_EXECUTION");
        request.setTenantId(tenantId);
        request.setResourceType("QUERY_EXECUTION_QUERY");
        request.setResourceId("query-fingerprint-001");
        request.setOperationCode("QUERY_EXECUTE_SYNC");
        request.setDatasourceId(datasourceId);
        request.setAction("USE");
        return request;
    }

    private GovernanceDatasourceAccessCheckRequest rewriteRecordListRequest(String tenantId) {
        GovernanceDatasourceAccessCheckRequest request = new GovernanceDatasourceAccessCheckRequest();
        request.setServiceCode("SQL_OPTIMIZATION");
        request.setTenantId(tenantId);
        request.setResourceType("SQL_REWRITE_RECORD");
        request.setResourceId("history-001");
        request.setOperationCode("SQL_REWRITE_RECORD_LIST");
        request.setDatasourceId("optimization-hetu");
        request.setAction("USE");
        return request;
    }

    private GovernanceDatasourceAccessCheckRequest rewriteRecordActivationRequest(String tenantId) {
        GovernanceDatasourceAccessCheckRequest request = new GovernanceDatasourceAccessCheckRequest();
        request.setServiceCode("SQL_OPTIMIZATION");
        request.setTenantId(tenantId);
        request.setResourceType("SQL_REWRITE_RECORD");
        request.setResourceId("rewrite-001");
        request.setOperationCode("SQL_REWRITE_RECORD_ACTIVATE");
        request.setDatasourceId("optimization-hetu");
        request.setAction("USE");
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
