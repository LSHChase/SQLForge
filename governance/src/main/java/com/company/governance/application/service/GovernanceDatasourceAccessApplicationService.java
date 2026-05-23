package com.company.governance.application.service;

import com.company.governance.application.controller.dto.AuditWriteRequest;
import com.company.governance.application.controller.dto.DatasourceAccessScopeChangeRequest;
import com.company.governance.application.controller.vo.DatasourceAccessScopeChangeResponse;
import com.company.governance.config.GovernanceAccessProperties;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceDatasourceAccessCheckRequest;
import com.company.sqlforge.common.governance.GovernanceDatasourceAccessCheckResponse;
import com.company.sqlforge.common.governance.GovernanceTenantScopeCheckRequest;
import com.company.sqlforge.common.governance.GovernanceTenantScopeCheckResponse;
import com.company.sqlforge.common.governance.ProtectedGovernanceRequestSupport;
import com.company.sqlforge.common.utils.JsonUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class GovernanceDatasourceAccessApplicationService {

    private static final String SYSTEM_TENANT_ID = "system";
    private static final String CONTRACT_STAGE_LONG_TERM_BASELINE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE_DATASOURCE_ACCESS_SCOPE_BASELINE =
        "DATASOURCE_ACCESS_SCOPE_BASELINE";
    private static final String STATUS_UPDATED = "UPDATED";
    private static final String ACCESS_CHECK_OPERATION = "DATASOURCE_ACCESS_CHECK";
    private static final String SCOPE_CHANGE_OPERATION = "DATASOURCE_ACCESS_SCOPE_CHANGE";
    private static final String REASON_ALLOWED = "ALLOWED";
    private static final String REASON_CALLER_TENANT_MISMATCH = "CALLER_TENANT_MISMATCH";
    private static final String REASON_DATASOURCE_SCOPE_MISSING = "DATASOURCE_SCOPE_MISSING";
    private static final String REASON_DATASOURCE_ACTION_DENIED = "DATASOURCE_ACTION_DENIED";
    private static final String REASON_DATASOURCE_ACCESS_REVOKED = "DATASOURCE_ACCESS_REVOKED";
    private static final String REASON_ACCESS_CONTROL_DISABLED = "ACCESS_CONTROL_DISABLED";
    private static final String REASON_SCOPE_CHANGE_REQUIRES_SYSTEM_CONTEXT =
        "DATASOURCE_SCOPE_CHANGE_REQUIRES_SYSTEM_CONTEXT";
    private static final String STATE_ACTIVE = "ACTIVE";
    private static final String STATE_REVOKED = "REVOKED";
    private static final String RESOURCE_TYPE_DATASOURCE_ACCESS = "DATASOURCE_ACCESS_SCOPE";

    private final GovernanceAccessProperties governanceAccessProperties;
    private final GovernanceAuditTrailService governanceAuditTrailService;
    private final Map<String, Map<String, GovernanceAccessProperties.DatasourceAccessScopeProperties>> runtimeDatasourceScopes =
        new ConcurrentHashMap<String, Map<String, GovernanceAccessProperties.DatasourceAccessScopeProperties>>();

    public GovernanceDatasourceAccessApplicationService(GovernanceAccessProperties governanceAccessProperties,
                                                        GovernanceAuditTrailService governanceAuditTrailService) {
        this.governanceAccessProperties = governanceAccessProperties;
        this.governanceAuditTrailService = governanceAuditTrailService;
    }

    @PostConstruct
    void initializeRuntimeDatasourceScopes() {
        synchronized (runtimeDatasourceScopes) {
            runtimeDatasourceScopes.clear();
            for (Map.Entry<String, Map<String, GovernanceAccessProperties.DatasourceAccessScopeProperties>> tenantEntry
                : governanceAccessProperties.getDatasourceScopes().entrySet()) {
                Map<String, GovernanceAccessProperties.DatasourceAccessScopeProperties> datasourceScopes =
                    new ConcurrentHashMap<String, GovernanceAccessProperties.DatasourceAccessScopeProperties>();
                for (Map.Entry<String, GovernanceAccessProperties.DatasourceAccessScopeProperties> datasourceEntry
                    : tenantEntry.getValue().entrySet()) {
                    datasourceScopes.put(datasourceEntry.getKey(), copyDatasourceScope(datasourceEntry.getValue()));
                }
                runtimeDatasourceScopes.put(tenantEntry.getKey(), datasourceScopes);
            }
        }
    }

    public GovernanceTenantScopeCheckResponse checkTenantScope(GovernanceTenantScopeCheckRequest request) {
        String callerTenantId = requireProtectedTenantContext();
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        String targetTenantId = requireText(request == null ? null : request.getTargetTenantId(), "targetTenantId");
        boolean callerMatchesTenant = callerTenantId.equals(tenantId);
        boolean allowed = callerMatchesTenant && tenantId.equals(targetTenantId);
        GovernanceTenantScopeCheckResponse response = new GovernanceTenantScopeCheckResponse();
        response.setTenantId(tenantId);
        response.setTargetTenantId(targetTenantId);
        response.setAllowed(allowed);
        response.setReason(allowed ? REASON_ALLOWED : REASON_CALLER_TENANT_MISMATCH);
        return response;
    }

    public GovernanceDatasourceAccessCheckResponse checkDatasourceAccess(GovernanceDatasourceAccessCheckRequest request) {
        long start = System.currentTimeMillis();
        String callerTenantId = requireProtectedTenantContext();
        GovernanceDatasourceAccessCheckResponse response = evaluateDatasourceAccess(request, callerTenantId);
        auditDatasourceAccessCheck(request, response, System.currentTimeMillis() - start);
        return response;
    }

    public void assertDatasourceAccess(GovernanceDatasourceAccessCheckRequest request) {
        GovernanceDatasourceAccessCheckResponse response = checkDatasourceAccess(request);
        if (!response.isAllowed()) {
            int errorCode = response.getErrorCode() == null
                ? ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED
                : response.getErrorCode().intValue();
            throw new BizException(errorCode, HttpStatus.FORBIDDEN, response.getReason());
        }
    }

    public DatasourceAccessScopeChangeResponse applyDatasourceAccessScopeChange(DatasourceAccessScopeChangeRequest request) {
        String callerTenantId = requireProtectedTenantContext();
        if (!SYSTEM_TENANT_ID.equals(callerTenantId)) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                REASON_SCOPE_CHANGE_REQUIRES_SYSTEM_CONTEXT
            );
        }
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        String datasourceId = requireText(request == null ? null : request.getDatasourceId(), "datasourceId");
        String state = normalizeState(request == null ? null : request.getState());
        List<String> actions = normalizeActions(request == null ? null : request.getActions());
        if (STATE_ACTIVE.equals(state) && CollectionUtils.isEmpty(actions)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "数据源范围状态为 ACTIVE 时 actions 不能为空"
            );
        }

        GovernanceAccessProperties.DatasourceAccessScopeProperties scope =
            new GovernanceAccessProperties.DatasourceAccessScopeProperties();
        scope.setState(state);
        scope.getActions().addAll(actions);
        synchronized (runtimeDatasourceScopes) {
            Map<String, GovernanceAccessProperties.DatasourceAccessScopeProperties> datasourceScopes =
                runtimeDatasourceScopes.get(tenantId);
            if (datasourceScopes == null) {
                datasourceScopes =
                    new ConcurrentHashMap<String, GovernanceAccessProperties.DatasourceAccessScopeProperties>();
                runtimeDatasourceScopes.put(tenantId, datasourceScopes);
            }
            datasourceScopes.put(datasourceId, scope);
        }
        auditDatasourceAccessScopeChange(tenantId, datasourceId, state, actions, request == null ? null : request.getChangeReason());
        return new DatasourceAccessScopeChangeResponse(
            tenantId,
            datasourceId,
            state,
            actions,
            STATUS_UPDATED,
            CONTRACT_STAGE_LONG_TERM_BASELINE,
            IMPLEMENTATION_STAGE_DATASOURCE_ACCESS_SCOPE_BASELINE
        );
    }

    public boolean isDatasourceActionAllowed(String tenantId, String datasourceId, String datasourceAction) {
        GovernanceAccessProperties.DatasourceAccessScopeProperties scope = resolveDatasourceScope(tenantId, datasourceId);
        return scope != null
            && STATE_ACTIVE.equalsIgnoreCase(trimToEmpty(scope.getState()))
            && scope.getActions().contains(trimToEmpty(datasourceAction).toUpperCase());
    }

    private GovernanceDatasourceAccessCheckResponse evaluateDatasourceAccess(
        GovernanceDatasourceAccessCheckRequest request,
        String callerTenantId
    ) {
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        String datasourceId = requireText(request == null ? null : request.getDatasourceId(), "datasourceId");
        String action = normalizeAction(request == null ? null : request.getAction());
        String resourceType = trimToNull(request == null ? null : request.getResourceType());
        String resourceId = trimToNull(request == null ? null : request.getResourceId());
        String operationCode = trimToNull(request == null ? null : request.getOperationCode());
        if (!governanceAccessProperties.isEnabled()) {
            return deny(tenantId, datasourceId, action, resourceType, resourceId, operationCode,
                REASON_ACCESS_CONTROL_DISABLED, Integer.valueOf(ErrorCodeConstants.GOVERNANCE_ACCESS_DENIED));
        }
        if (!callerTenantId.equals(tenantId)) {
            return deny(tenantId, datasourceId, action, resourceType, resourceId, operationCode,
                REASON_CALLER_TENANT_MISMATCH, Integer.valueOf(ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED));
        }
        GovernanceAccessProperties.DatasourceAccessScopeProperties datasourceScope =
            resolveDatasourceScope(tenantId, datasourceId);
        if (datasourceScope == null) {
            return governanceAccessProperties.isDenyByDefault()
                ? deny(tenantId, datasourceId, action, resourceType, resourceId, operationCode,
                    REASON_DATASOURCE_SCOPE_MISSING,
                    Integer.valueOf(ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED))
                : allow(tenantId, datasourceId, action, resourceType, resourceId, operationCode, REASON_ALLOWED);
        }
        if (!STATE_ACTIVE.equalsIgnoreCase(trimToEmpty(datasourceScope.getState()))) {
            return deny(tenantId, datasourceId, action, resourceType, resourceId, operationCode,
                REASON_DATASOURCE_ACCESS_REVOKED,
                Integer.valueOf(ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED));
        }
        if (!datasourceScope.getActions().contains(action)) {
            return deny(tenantId, datasourceId, action, resourceType, resourceId, operationCode,
                REASON_DATASOURCE_ACTION_DENIED,
                Integer.valueOf(ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED));
        }
        return allow(tenantId, datasourceId, action, resourceType, resourceId, operationCode, REASON_ALLOWED);
    }

    private GovernanceDatasourceAccessCheckResponse allow(String tenantId,
                                                          String datasourceId,
                                                          String action,
                                                          String resourceType,
                                                          String resourceId,
                                                          String operationCode,
                                                          String reason) {
        GovernanceDatasourceAccessCheckResponse response = new GovernanceDatasourceAccessCheckResponse();
        response.setTenantId(tenantId);
        response.setDatasourceId(datasourceId);
        response.setAction(action);
        response.setResourceType(resourceType);
        response.setResourceId(resourceId);
        response.setOperationCode(operationCode);
        response.setAllowed(true);
        response.setReason(reason);
        response.setContractStage(CONTRACT_STAGE_LONG_TERM_BASELINE);
        response.setImplementationStage(IMPLEMENTATION_STAGE_DATASOURCE_ACCESS_SCOPE_BASELINE);
        return response;
    }

    private GovernanceDatasourceAccessCheckResponse deny(String tenantId,
                                                         String datasourceId,
                                                         String action,
                                                         String resourceType,
                                                         String resourceId,
                                                         String operationCode,
                                                         String reason,
                                                         Integer errorCode) {
        GovernanceDatasourceAccessCheckResponse response = allow(
            tenantId,
            datasourceId,
            action,
            resourceType,
            resourceId,
            operationCode,
            reason
        );
        response.setAllowed(false);
        response.setErrorCode(errorCode);
        return response;
    }

    private GovernanceAccessProperties.DatasourceAccessScopeProperties resolveDatasourceScope(
        String tenantId,
        String datasourceId
    ) {
        if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(datasourceId)) {
            return null;
        }
        Map<String, GovernanceAccessProperties.DatasourceAccessScopeProperties> datasourceScopes =
            runtimeDatasourceScopes.get(tenantId);
        if (datasourceScopes == null) {
            return null;
        }
        return datasourceScopes.get(datasourceId);
    }

    private GovernanceAccessProperties.DatasourceAccessScopeProperties copyDatasourceScope(
        GovernanceAccessProperties.DatasourceAccessScopeProperties source
    ) {
        GovernanceAccessProperties.DatasourceAccessScopeProperties copy =
            new GovernanceAccessProperties.DatasourceAccessScopeProperties();
        if (source != null) {
            copy.setState(source.getState());
            copy.getActions().addAll(normalizeActions(source.getActions()));
        }
        return copy;
    }

    private void auditDatasourceAccessCheck(GovernanceDatasourceAccessCheckRequest request,
                                            GovernanceDatasourceAccessCheckResponse response,
                                            long elapsedMs) {
        if (!governanceAccessProperties.isAuditDecisions()) {
            return;
        }
        AuditWriteRequest auditRequest = new AuditWriteRequest();
        auditRequest.setServiceCode(ServiceCodeConstants.GOVERNANCE);
        auditRequest.setOperationCode(ACCESS_CHECK_OPERATION);
        auditRequest.setResourceType(RESOURCE_TYPE_DATASOURCE_ACCESS);
        auditRequest.setResourceId(resolveDatasourceAccessAuditResourceId(request));
        auditRequest.setResultStatus(response.isAllowed() ? "SUCCESS" : "FAILED");
        auditRequest.setElapsedMs(Long.valueOf(elapsedMs));
        auditRequest.setSourceIp(ProtectedGovernanceRequestSupport.resolveSourceIp("127.0.0.1"));
        auditRequest.setUserAgent(ProtectedGovernanceRequestSupport.resolveUserAgent("SQLForge-Governance"));
        auditRequest.setRequestParams(JsonUtils.toJson(request));
        auditRequest.setResponseSummary(JsonUtils.toJson(response));
        governanceAuditTrailService.writeAudit(auditRequest);
    }

    private void auditDatasourceAccessScopeChange(String tenantId,
                                                  String datasourceId,
                                                  String state,
                                                  List<String> actions,
                                                  String changeReason) {
        AuditWriteRequest auditRequest = new AuditWriteRequest();
        auditRequest.setServiceCode(ServiceCodeConstants.GOVERNANCE);
        auditRequest.setOperationCode(SCOPE_CHANGE_OPERATION);
        auditRequest.setResourceType(RESOURCE_TYPE_DATASOURCE_ACCESS);
        auditRequest.setResourceId(tenantId + ":" + datasourceId);
        auditRequest.setResultStatus("SUCCESS");
        auditRequest.setElapsedMs(Long.valueOf(0L));
        auditRequest.setSourceIp(ProtectedGovernanceRequestSupport.resolveSourceIp("127.0.0.1"));
        auditRequest.setUserAgent(ProtectedGovernanceRequestSupport.resolveUserAgent("SQLForge-Governance"));
        DatasourceAccessScopeChangeAuditPayload payload =
            new DatasourceAccessScopeChangeAuditPayload(tenantId, datasourceId, state, actions, changeReason);
        auditRequest.setRequestParams(JsonUtils.toJson(payload));
        auditRequest.setResponseSummary("数据源访问范围已更新");
        governanceAuditTrailService.writeAudit(auditRequest);
    }

    private String resolveDatasourceAccessAuditResourceId(GovernanceDatasourceAccessCheckRequest request) {
        String resourceId = trimToNull(request == null ? null : request.getResourceId());
        if (resourceId != null) {
            return resourceId;
        }
        List<String> tokens = new ArrayList<String>();
        tokens.add(trimToEmpty(request == null ? null : request.getTenantId()));
        tokens.add(trimToEmpty(request == null ? null : request.getDatasourceId()));
        tokens.add(trimToEmpty(request == null ? null : request.getAction()));
        return String.join(":", tokens);
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
                "受保护请求上下文缺失"
            );
        }
        return tenantId;
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                fieldName + " 不能为空"
            );
        }
        return value.trim();
    }

    private String normalizeState(String value) {
        String state = trimToEmpty(value).toUpperCase();
        if (!StringUtils.hasText(state)) {
            return STATE_ACTIVE;
        }
        if (!Arrays.asList(STATE_ACTIVE, STATE_REVOKED).contains(state)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "不支持的数据源范围状态：" + value
            );
        }
        return state;
    }

    private String normalizeAction(String action) {
        String normalized = trimToEmpty(action).toUpperCase();
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "action 不能为空"
            );
        }
        return normalized;
    }

    private List<String> normalizeActions(List<String> actions) {
        List<String> normalized = new ArrayList<String>();
        if (actions == null) {
            return normalized;
        }
        for (String action : actions) {
            if (StringUtils.hasText(action)) {
                normalized.add(action.trim().toUpperCase());
            }
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class DatasourceAccessScopeChangeAuditPayload {

        private final String tenantId;
        private final String datasourceId;
        private final String state;
        private final List<String> actions;
        private final String changeReason;

        private DatasourceAccessScopeChangeAuditPayload(String tenantId,
                                                        String datasourceId,
                                                        String state,
                                                        List<String> actions,
                                                        String changeReason) {
            this.tenantId = tenantId;
            this.datasourceId = datasourceId;
            this.state = state;
            this.actions = actions;
            this.changeReason = changeReason;
        }

        public String getTenantId() {
            return tenantId;
        }

        public String getDatasourceId() {
            return datasourceId;
        }

        public String getState() {
            return state;
        }

        public List<String> getActions() {
            return actions;
        }

        public String getChangeReason() {
            return changeReason;
        }
    }
}
