package com.company.governance.application.service;

import com.company.governance.application.controller.dto.AuditWriteRequest;
import com.company.governance.application.controller.dto.DatasourceAuthorizationChangeRequest;
import com.company.governance.application.controller.vo.DatasourceAuthorizationChangeResponse;
import com.company.governance.config.GovernanceAccessProperties;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceAuthorizationDecisionRequest;
import com.company.sqlforge.common.governance.GovernanceAuthorizationDecisionResponse;
import com.company.sqlforge.common.governance.GovernanceTenantScopeCheckRequest;
import com.company.sqlforge.common.governance.GovernanceTenantScopeCheckResponse;
import com.company.sqlforge.common.governance.ProtectedGovernanceRequestSupport;
import com.company.sqlforge.common.utils.JsonUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class GovernanceAuthorizationMatrixApplicationService {

    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final String CONTRACT_STAGE_LONG_TERM_BASELINE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE_AUTHORIZATION_MATRIX_BASELINE =
        "AUTHORIZATION_MATRIX_BASELINE";
    private static final String STATUS_UPDATED = "UPDATED";
    private static final String AUTHORIZATION_DECISION_OPERATION = "AUTHORIZATION_DECISION";
    private static final String PERMISSION_CHANGE_OPERATION = "PERMISSION_CHANGE";
    private static final String REASON_ALLOWED = "ALLOWED";
    private static final String REASON_PLATFORM_ADMIN_OVERRIDE = "PLATFORM_ADMIN_OVERRIDE";
    private static final String REASON_CALLER_TENANT_MISMATCH = "CALLER_TENANT_MISMATCH";
    private static final String REASON_ROLE_PERMISSION_DENIED = "ROLE_PERMISSION_DENIED";
    private static final String REASON_RESOURCE_POLICY_MISSING = "RESOURCE_POLICY_MISSING";
    private static final String REASON_OPERATION_POLICY_MISSING = "OPERATION_POLICY_MISSING";
    private static final String REASON_DATASOURCE_BINDING_MISSING = "DATASOURCE_BINDING_MISSING";
    private static final String REASON_DATASOURCE_ACTION_DENIED = "DATASOURCE_ACTION_DENIED";
    private static final String REASON_DATASOURCE_ACCESS_REVOKED = "DATASOURCE_ACCESS_REVOKED";
    private static final String REASON_ACCESS_CONTROL_DISABLED = "ACCESS_CONTROL_DISABLED";
    private static final String STATE_ACTIVE = "ACTIVE";
    private static final String STATE_REVOKED = "REVOKED";
    private static final String RESOURCE_TYPE_AUTHORIZATION = "AUTHORIZATION_RESOURCE";
    private static final String RESOURCE_TYPE_PERMISSION_CHANGE = "DATASOURCE_AUTHORIZATION_MATRIX";

    private final GovernanceAccessProperties governanceAccessProperties;
    private final GovernanceAuditTrailService governanceAuditTrailService;
    private final Map<String, Map<String, GovernanceAccessProperties.DatasourceAuthorizationProperties>> runtimeDatasourceMatrix =
        new ConcurrentHashMap<String, Map<String, GovernanceAccessProperties.DatasourceAuthorizationProperties>>();

    public GovernanceAuthorizationMatrixApplicationService(
        GovernanceAccessProperties governanceAccessProperties,
        GovernanceAuditTrailService governanceAuditTrailService
    ) {
        this.governanceAccessProperties = governanceAccessProperties;
        this.governanceAuditTrailService = governanceAuditTrailService;
    }

    @PostConstruct
    void initializeRuntimeDatasourceMatrix() {
        synchronized (runtimeDatasourceMatrix) {
            runtimeDatasourceMatrix.clear();
            for (Map.Entry<String, Map<String, GovernanceAccessProperties.DatasourceAuthorizationProperties>> tenantEntry
                : governanceAccessProperties.getDatasourceAuthorizationMatrix().entrySet()) {
                Map<String, GovernanceAccessProperties.DatasourceAuthorizationProperties> datasourcePolicies =
                    new ConcurrentHashMap<String, GovernanceAccessProperties.DatasourceAuthorizationProperties>();
                for (Map.Entry<String, GovernanceAccessProperties.DatasourceAuthorizationProperties> datasourceEntry
                    : tenantEntry.getValue().entrySet()) {
                    datasourcePolicies.put(datasourceEntry.getKey(), copyDatasourcePolicy(datasourceEntry.getValue()));
                }
                runtimeDatasourceMatrix.put(tenantEntry.getKey(), datasourcePolicies);
            }
        }
    }

    public GovernanceTenantScopeCheckResponse checkTenantScope(GovernanceTenantScopeCheckRequest request) {
        String callerTenantId = requireProtectedTenantContext();
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        String targetTenantId = requireText(request == null ? null : request.getTargetTenantId(), "targetTenantId");
        boolean platformAdmin = RequestContext.hasRole(PLATFORM_ADMIN) || hasWildcardPermission(resolvePermissions());
        if (!platformAdmin && !callerTenantId.equals(tenantId)) {
            return new GovernanceTenantScopeCheckResponseBuilder()
                .tenantId(tenantId)
                .targetTenantId(targetTenantId)
                .allowed(false)
                .reason(REASON_CALLER_TENANT_MISMATCH)
                .build();
        }
        boolean allowed = platformAdmin || tenantId.equals(targetTenantId);
        return new GovernanceTenantScopeCheckResponseBuilder()
            .tenantId(tenantId)
            .targetTenantId(targetTenantId)
            .allowed(allowed)
            .reason(
                allowed
                    ? (platformAdmin && !tenantId.equals(targetTenantId) ? REASON_PLATFORM_ADMIN_OVERRIDE : REASON_ALLOWED)
                    : "CROSS_TENANT_ACCESS_REQUIRES_PLATFORM_ADMIN"
            )
            .build();
    }

    public GovernanceAuthorizationDecisionResponse decideAuthorization(GovernanceAuthorizationDecisionRequest request) {
        long start = System.currentTimeMillis();
        String callerTenantId = requireProtectedTenantContext();
        GovernanceAuthorizationDecisionResponse response = evaluateDecision(request, callerTenantId);
        auditAuthorizationDecision(request, response, System.currentTimeMillis() - start);
        return response;
    }

    public void assertAuthorized(GovernanceAuthorizationDecisionRequest request) {
        GovernanceAuthorizationDecisionResponse response = decideAuthorization(request);
        if (!response.isAllowed()) {
            int errorCode = response.getErrorCode() == null
                ? ErrorCodeConstants.GOVERNANCE_ACCESS_DENIED
                : response.getErrorCode().intValue();
            throw new BizException(errorCode, HttpStatus.FORBIDDEN, response.getReason());
        }
    }

    public DatasourceAuthorizationChangeResponse applyDatasourceAuthorizationChange(
        DatasourceAuthorizationChangeRequest request
    ) {
        String callerTenantId = requireProtectedTenantContext();
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        String datasourceId = requireText(request == null ? null : request.getDatasourceId(), "datasourceId");
        String state = normalizeState(request == null ? null : request.getState());
        List<String> actions = normalizeActions(request == null ? null : request.getActions());
        Set<String> permissions = resolvePermissions();
        boolean platformAdmin = RequestContext.hasRole(PLATFORM_ADMIN) || hasWildcardPermission(permissions);

        if (!platformAdmin && !callerTenantId.equals(tenantId)) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED_MESSAGE
            );
        }
        if (!hasAnyPermission(permissions, Collections.singletonList("governance.permission.manage"))) {
            throw new BizException(
                ErrorCodeConstants.GOVERNANCE_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                "当前角色无权变更数据源授权矩阵"
            );
        }
        if (STATE_ACTIVE.equals(state) && CollectionUtils.isEmpty(actions)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "actions must not be empty when datasource authorization state is ACTIVE"
            );
        }

        GovernanceAccessProperties.DatasourceAuthorizationProperties policy =
            new GovernanceAccessProperties.DatasourceAuthorizationProperties();
        policy.setState(state);
        policy.getActions().addAll(actions);
        synchronized (runtimeDatasourceMatrix) {
            Map<String, GovernanceAccessProperties.DatasourceAuthorizationProperties> datasourcePolicies =
                runtimeDatasourceMatrix.get(tenantId);
            if (datasourcePolicies == null) {
                datasourcePolicies =
                    new ConcurrentHashMap<String, GovernanceAccessProperties.DatasourceAuthorizationProperties>();
                runtimeDatasourceMatrix.put(tenantId, datasourcePolicies);
            }
            datasourcePolicies.put(datasourceId, policy);
        }
        auditPermissionChange(tenantId, datasourceId, state, actions, request == null ? null : request.getChangeReason());
        return new DatasourceAuthorizationChangeResponse(
            tenantId,
            datasourceId,
            state,
            actions,
            STATUS_UPDATED,
            CONTRACT_STAGE_LONG_TERM_BASELINE,
            IMPLEMENTATION_STAGE_AUTHORIZATION_MATRIX_BASELINE
        );
    }

    public boolean isDatasourceActionAllowed(String tenantId, String datasourceId, String datasourceAction) {
        GovernanceAccessProperties.DatasourceAuthorizationProperties policy = resolveDatasourcePolicy(tenantId, datasourceId);
        return policy != null
            && STATE_ACTIVE.equalsIgnoreCase(trimToEmpty(policy.getState()))
            && policy.getActions().contains(trimToEmpty(datasourceAction).toUpperCase());
    }

    private GovernanceAuthorizationDecisionResponse evaluateDecision(
        GovernanceAuthorizationDecisionRequest request,
        String callerTenantId
    ) {
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        String serviceCode = requireText(request == null ? null : request.getServiceCode(), "serviceCode");
        String resourceType = requireText(request == null ? null : request.getResourceType(), "resourceType");
        String operationCode = requireText(request == null ? null : request.getOperationCode(), "operationCode");
        String datasourceId = trimToNull(request == null ? null : request.getDatasourceId());
        String resourceId = trimToNull(request == null ? null : request.getResourceId());
        Set<String> permissions = resolvePermissions();
        boolean platformAdmin = RequestContext.hasRole(PLATFORM_ADMIN) || hasWildcardPermission(permissions);
        if (!governanceAccessProperties.isEnabled()) {
            return deny(tenantId, resourceType, resourceId, operationCode, datasourceId,
                REASON_ACCESS_CONTROL_DISABLED, Integer.valueOf(ErrorCodeConstants.GOVERNANCE_ACCESS_DENIED));
        }
        if (!platformAdmin && !callerTenantId.equals(tenantId)) {
            return deny(tenantId, resourceType, resourceId, operationCode, datasourceId,
                REASON_CALLER_TENANT_MISMATCH, Integer.valueOf(ErrorCodeConstants.GOVERNANCE_TENANT_ACCESS_DENIED));
        }
        GovernanceAccessProperties.ResourcePolicyProperties resourcePolicy =
            governanceAccessProperties.getResourceModel().get(resourceType);
        if (resourcePolicy == null) {
            return governanceAccessProperties.isDenyByDefault()
                ? deny(tenantId, resourceType, resourceId, operationCode, datasourceId,
                    REASON_RESOURCE_POLICY_MISSING, Integer.valueOf(ErrorCodeConstants.GOVERNANCE_ACCESS_DENIED))
                : allow(tenantId, resourceType, resourceId, operationCode, datasourceId, REASON_ALLOWED);
        }
        GovernanceAccessProperties.OperationPolicyProperties operationPolicy =
            resourcePolicy.getOperations().get(operationCode);
        if (operationPolicy == null) {
            return governanceAccessProperties.isDenyByDefault()
                ? deny(tenantId, resourceType, resourceId, operationCode, datasourceId,
                    REASON_OPERATION_POLICY_MISSING, Integer.valueOf(ErrorCodeConstants.GOVERNANCE_ACCESS_DENIED))
                : allow(tenantId, resourceType, resourceId, operationCode, datasourceId, REASON_ALLOWED);
        }
        if (!hasAnyPermission(permissions, operationPolicy.getRequiredPermissions())) {
            return deny(tenantId, resourceType, resourceId, operationCode, datasourceId,
                REASON_ROLE_PERMISSION_DENIED, Integer.valueOf(ErrorCodeConstants.GOVERNANCE_ACCESS_DENIED));
        }
        String datasourceAction = trimToNull(operationPolicy.getDatasourceAction());
        if (StringUtils.hasText(datasourceAction)) {
            GovernanceAccessProperties.DatasourceAuthorizationProperties datasourcePolicy =
                resolveDatasourcePolicy(tenantId, datasourceId);
            if (datasourcePolicy == null) {
                return deny(tenantId, resourceType, resourceId, operationCode, datasourceId,
                    REASON_DATASOURCE_BINDING_MISSING,
                    Integer.valueOf(ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED));
            }
            if (!STATE_ACTIVE.equalsIgnoreCase(trimToEmpty(datasourcePolicy.getState()))) {
                return deny(tenantId, resourceType, resourceId, operationCode, datasourceId,
                    REASON_DATASOURCE_ACCESS_REVOKED,
                    Integer.valueOf(ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED));
            }
            if (!datasourcePolicy.getActions().contains(datasourceAction.toUpperCase())) {
                return deny(tenantId, resourceType, resourceId, operationCode, datasourceId,
                    REASON_DATASOURCE_ACTION_DENIED,
                    Integer.valueOf(ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED));
            }
        }
        return allow(
            tenantId,
            resourceType,
            resourceId,
            operationCode,
            datasourceId,
            platformAdmin ? REASON_PLATFORM_ADMIN_OVERRIDE : REASON_ALLOWED
        );
    }

    private GovernanceAuthorizationDecisionResponse allow(String tenantId,
                                                          String resourceType,
                                                          String resourceId,
                                                          String operationCode,
                                                          String datasourceId,
                                                          String reason) {
        GovernanceAuthorizationDecisionResponse response = new GovernanceAuthorizationDecisionResponse();
        response.setTenantId(tenantId);
        response.setResourceType(resourceType);
        response.setResourceId(resourceId);
        response.setOperationCode(operationCode);
        response.setDatasourceId(datasourceId);
        response.setAllowed(true);
        response.setReason(reason);
        response.setContractStage(CONTRACT_STAGE_LONG_TERM_BASELINE);
        response.setImplementationStage(IMPLEMENTATION_STAGE_AUTHORIZATION_MATRIX_BASELINE);
        return response;
    }

    private GovernanceAuthorizationDecisionResponse deny(String tenantId,
                                                         String resourceType,
                                                         String resourceId,
                                                         String operationCode,
                                                         String datasourceId,
                                                         String reason,
                                                         Integer errorCode) {
        GovernanceAuthorizationDecisionResponse response = allow(
            tenantId,
            resourceType,
            resourceId,
            operationCode,
            datasourceId,
            reason
        );
        response.setAllowed(false);
        response.setErrorCode(errorCode);
        return response;
    }

    private GovernanceAccessProperties.DatasourceAuthorizationProperties resolveDatasourcePolicy(
        String tenantId,
        String datasourceId
    ) {
        if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(datasourceId)) {
            return null;
        }
        Map<String, GovernanceAccessProperties.DatasourceAuthorizationProperties> datasourcePolicies =
            runtimeDatasourceMatrix.get(tenantId);
        if (datasourcePolicies == null) {
            return null;
        }
        return datasourcePolicies.get(datasourceId);
    }

    private GovernanceAccessProperties.DatasourceAuthorizationProperties copyDatasourcePolicy(
        GovernanceAccessProperties.DatasourceAuthorizationProperties source
    ) {
        GovernanceAccessProperties.DatasourceAuthorizationProperties copy =
            new GovernanceAccessProperties.DatasourceAuthorizationProperties();
        if (source != null) {
            copy.setState(source.getState());
            copy.getActions().addAll(normalizeActions(source.getActions()));
        }
        return copy;
    }

    private Set<String> resolvePermissions() {
        Set<String> permissions = new LinkedHashSet<String>();
        for (String roleCode : RequestContext.getRoleCodes()) {
            GovernanceAccessProperties.RolePolicyProperties rolePolicy =
                governanceAccessProperties.getRoleMatrix().get(roleCode);
            if (rolePolicy != null) {
                for (String permission : rolePolicy.getPermissions()) {
                    if (StringUtils.hasText(permission)) {
                        permissions.add(permission.trim());
                    }
                }
            }
        }
        return permissions;
    }

    private boolean hasAnyPermission(Set<String> permissions, List<String> requiredPermissions) {
        if (hasWildcardPermission(permissions)) {
            return true;
        }
        if (CollectionUtils.isEmpty(requiredPermissions)) {
            return true;
        }
        for (String requiredPermission : requiredPermissions) {
            if (permissions.contains(requiredPermission)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasWildcardPermission(Set<String> permissions) {
        return permissions.contains("*");
    }

    private void auditAuthorizationDecision(GovernanceAuthorizationDecisionRequest request,
                                            GovernanceAuthorizationDecisionResponse response,
                                            long elapsedMs) {
        if (!governanceAccessProperties.isAuditDecisions()) {
            return;
        }
        AuditWriteRequest auditRequest = new AuditWriteRequest();
        auditRequest.setServiceCode(ServiceCodeConstants.GOVERNANCE);
        auditRequest.setOperationCode(AUTHORIZATION_DECISION_OPERATION);
        auditRequest.setResourceType(RESOURCE_TYPE_AUTHORIZATION);
        auditRequest.setResourceId(resolveAuthorizationAuditResourceId(request));
        auditRequest.setResultStatus(response.isAllowed() ? "SUCCESS" : "FAILED");
        auditRequest.setElapsedMs(Long.valueOf(elapsedMs));
        auditRequest.setSourceIp(ProtectedGovernanceRequestSupport.resolveSourceIp("127.0.0.1"));
        auditRequest.setUserAgent(ProtectedGovernanceRequestSupport.resolveUserAgent("SQLForge-Governance"));
        auditRequest.setRequestParams(JsonUtils.toJson(buildAuthorizationAuditPayload(request)));
        auditRequest.setResponseSummary(JsonUtils.toJson(buildAuthorizationAuditSummary(response)));
        governanceAuditTrailService.writeAudit(auditRequest);
    }

    private void auditPermissionChange(String tenantId,
                                       String datasourceId,
                                       String state,
                                       List<String> actions,
                                       String changeReason) {
        AuditWriteRequest auditRequest = new AuditWriteRequest();
        auditRequest.setServiceCode(ServiceCodeConstants.GOVERNANCE);
        auditRequest.setOperationCode(PERMISSION_CHANGE_OPERATION);
        auditRequest.setResourceType(RESOURCE_TYPE_PERMISSION_CHANGE);
        auditRequest.setResourceId(tenantId + ":" + datasourceId);
        auditRequest.setResultStatus("SUCCESS");
        auditRequest.setElapsedMs(Long.valueOf(0L));
        auditRequest.setSourceIp(ProtectedGovernanceRequestSupport.resolveSourceIp("127.0.0.1"));
        auditRequest.setUserAgent(ProtectedGovernanceRequestSupport.resolveUserAgent("SQLForge-Governance"));
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("tenantId", tenantId);
        payload.put("datasourceId", datasourceId);
        payload.put("state", state);
        payload.put("actions", actions);
        payload.put("changeReason", changeReason);
        auditRequest.setRequestParams(JsonUtils.toJson(payload));
        auditRequest.setResponseSummary("Datasource authorization matrix updated");
        governanceAuditTrailService.writeAudit(auditRequest);
    }

    private Map<String, Object> buildAuthorizationAuditPayload(GovernanceAuthorizationDecisionRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", request.getServiceCode());
        payload.put("tenantId", request.getTenantId());
        payload.put("resourceType", request.getResourceType());
        payload.put("resourceId", request.getResourceId());
        payload.put("operationCode", request.getOperationCode());
        payload.put("datasourceId", request.getDatasourceId());
        payload.put("roleCodes", new ArrayList<String>(RequestContext.getRoleCodes()));
        return payload;
    }

    private Map<String, Object> buildAuthorizationAuditSummary(GovernanceAuthorizationDecisionResponse response) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("allowed", Boolean.valueOf(response.isAllowed()));
        payload.put("reason", response.getReason());
        payload.put("errorCode", response.getErrorCode());
        payload.put("implementationStage", response.getImplementationStage());
        return payload;
    }

    private String resolveAuthorizationAuditResourceId(GovernanceAuthorizationDecisionRequest request) {
        String resourceId = trimToNull(request.getResourceId());
        if (resourceId != null) {
            return resourceId;
        }
        List<String> tokens = new ArrayList<String>();
        tokens.add(trimToEmpty(request.getResourceType()));
        tokens.add(trimToEmpty(request.getOperationCode()));
        if (StringUtils.hasText(request.getDatasourceId())) {
            tokens.add(request.getDatasourceId().trim());
        }
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
                "Protected request context is missing"
            );
        }
        return tenantId;
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                fieldName + " must not be empty"
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
                "Unsupported datasource authorization state: " + value
            );
        }
        return state;
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

    private static final class GovernanceTenantScopeCheckResponseBuilder {

        private final GovernanceTenantScopeCheckResponse response = new GovernanceTenantScopeCheckResponse();

        private GovernanceTenantScopeCheckResponseBuilder tenantId(String tenantId) {
            response.setTenantId(tenantId);
            return this;
        }

        private GovernanceTenantScopeCheckResponseBuilder targetTenantId(String targetTenantId) {
            response.setTargetTenantId(targetTenantId);
            return this;
        }

        private GovernanceTenantScopeCheckResponseBuilder allowed(boolean allowed) {
            response.setAllowed(allowed);
            return this;
        }

        private GovernanceTenantScopeCheckResponseBuilder reason(String reason) {
            response.setReason(reason);
            return this;
        }

        private GovernanceTenantScopeCheckResponse build() {
            return response;
        }
    }
}
