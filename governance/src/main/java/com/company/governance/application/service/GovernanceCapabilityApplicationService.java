package com.company.governance.application.service;

import com.company.governance.application.controller.dto.AuditWriteRequest;
import com.company.governance.application.controller.dto.DatasourceAuthorizationChangeRequest;
import com.company.governance.application.controller.vo.AuditWriteResponse;
import com.company.governance.application.controller.vo.DatasourceAuthorizationChangeResponse;
import com.company.governance.application.controller.vo.ScheduleExtensionStatusVO;
import com.company.governance.config.MessagingProperties;
import com.company.governance.domain.tenant.entity.TenantConfig;
import com.company.governance.domain.tenant.repository.TenantConfigRepository;
import com.company.sqlforge.common.config.MessagingMode;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceRequest;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceResponse;
import com.company.sqlforge.common.governance.GovernanceAuthorizationDecisionRequest;
import com.company.sqlforge.common.governance.GovernanceAuthorizationDecisionResponse;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertResponse;
import com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceResponse;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveRequest;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveResponse;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveResponse;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteResponse;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigRequest;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigResponse;
import com.company.sqlforge.common.governance.GovernanceTenantArtifactPolicyRequest;
import com.company.sqlforge.common.governance.GovernanceTenantArtifactPolicyResponse;
import com.company.sqlforge.common.governance.GovernanceTenantScopeCheckRequest;
import com.company.sqlforge.common.governance.GovernanceTenantScopeCheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GovernanceCapabilityApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GovernanceCapabilityApplicationService.class);
    private static final String CONTRACT_STAGE_TRANSITIONAL_SKELETON = "TRANSITIONAL_SKELETON";
    private static final String IMPLEMENTATION_STAGE_TRANSITIONAL_SKELETON = "TRANSITIONAL_SKELETON";
    private static final String CONTRACT_STAGE_LONG_TERM_BASELINE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE_TENANT_ARTIFACT_POLICY_BASELINE =
        "TENANT_ARTIFACT_POLICY_BASELINE";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_EXTERNALIZED = "EXTERNALIZED";
    private static final String STATUS_TEST_ONLY = "TEST_ONLY";
    private static final String GOVERNANCE_SCHEDULE_EXTENSION_POINT = "governance.schedule.dispatch";

    private final GovernanceAuthorizationMatrixApplicationService governanceAuthorizationMatrixApplicationService;
    private final GovernanceAuditTrailService governanceAuditTrailService;
    private final GovernanceBenchmarkTraceabilityApplicationService governanceBenchmarkTraceabilityApplicationService;
    private final GovernanceBenchmarkRegressionAlertApplicationService governanceBenchmarkRegressionAlertApplicationService;
    private final GovernanceAccelerationPlanTraceabilityApplicationService governanceAccelerationPlanTraceabilityApplicationService;
    private final GovernanceQueryExecutionHistoryApplicationService governanceQueryExecutionHistoryApplicationService;
    private final DatabaseViewCatalogApplicationService databaseViewCatalogApplicationService;
    private final ReportInterfaceConfigApplicationService reportInterfaceConfigApplicationService;
    private final DatasourceConfigApplicationService datasourceConfigApplicationService;
    private final MessagingProperties messagingProperties;
    private final TenantConfigRepository tenantConfigRepository;

    public GovernanceCapabilityApplicationService(
                                                  GovernanceAuthorizationMatrixApplicationService governanceAuthorizationMatrixApplicationService,
                                                  GovernanceAuditTrailService governanceAuditTrailService,
                                                  GovernanceBenchmarkTraceabilityApplicationService governanceBenchmarkTraceabilityApplicationService,
                                                  GovernanceBenchmarkRegressionAlertApplicationService governanceBenchmarkRegressionAlertApplicationService,
                                                  GovernanceAccelerationPlanTraceabilityApplicationService governanceAccelerationPlanTraceabilityApplicationService,
                                                  GovernanceQueryExecutionHistoryApplicationService governanceQueryExecutionHistoryApplicationService,
                                                  DatabaseViewCatalogApplicationService databaseViewCatalogApplicationService,
                                                  ReportInterfaceConfigApplicationService reportInterfaceConfigApplicationService,
                                                  DatasourceConfigApplicationService datasourceConfigApplicationService,
                                                  MessagingProperties messagingProperties,
                                                  TenantConfigRepository tenantConfigRepository) {
        this.governanceAuthorizationMatrixApplicationService = governanceAuthorizationMatrixApplicationService;
        this.governanceAuditTrailService = governanceAuditTrailService;
        this.governanceBenchmarkTraceabilityApplicationService = governanceBenchmarkTraceabilityApplicationService;
        this.governanceBenchmarkRegressionAlertApplicationService = governanceBenchmarkRegressionAlertApplicationService;
        this.governanceAccelerationPlanTraceabilityApplicationService = governanceAccelerationPlanTraceabilityApplicationService;
        this.governanceQueryExecutionHistoryApplicationService = governanceQueryExecutionHistoryApplicationService;
        this.databaseViewCatalogApplicationService = databaseViewCatalogApplicationService;
        this.reportInterfaceConfigApplicationService = reportInterfaceConfigApplicationService;
        this.datasourceConfigApplicationService = datasourceConfigApplicationService;
        this.messagingProperties = messagingProperties;
        this.tenantConfigRepository = tenantConfigRepository;
    }

    public GovernanceTenantScopeCheckResponse checkTenantScope(GovernanceTenantScopeCheckRequest request) {
        requireProtectedTenantContext();
        return governanceAuthorizationMatrixApplicationService.checkTenantScope(request);
    }

    public GovernanceAuthorizationDecisionResponse decideAuthorization(GovernanceAuthorizationDecisionRequest request) {
        requireProtectedTenantContext();
        return governanceAuthorizationMatrixApplicationService.decideAuthorization(request);
    }

    public DatasourceAuthorizationChangeResponse changeDatasourceAuthorization(
        DatasourceAuthorizationChangeRequest request
    ) {
        requireProtectedTenantContext();
        return governanceAuthorizationMatrixApplicationService.applyDatasourceAuthorizationChange(request);
    }

    public AuditWriteResponse publishAuditEvent(AuditWriteRequest request) {
        requireProtectedTenantContext();
        return governanceAuditTrailService.writeAudit(request);
    }

    public GovernanceBenchmarkReportTraceResponse writeBenchmarkReportTrace(GovernanceBenchmarkReportTraceRequest request) {
        requireProtectedTenantContext();
        return governanceBenchmarkTraceabilityApplicationService.writeBenchmarkReportTrace(request);
    }

    public GovernanceBenchmarkRegressionAlertResponse emitBenchmarkRegressionAlert(
        GovernanceBenchmarkRegressionAlertRequest request
    ) {
        requireProtectedTenantContext();
        return governanceBenchmarkRegressionAlertApplicationService.emit(request);
    }

    public GovernanceAccelerationPlanTraceResponse writeAccelerationPlanTrace(
        GovernanceAccelerationPlanTraceRequest request
    ) {
        requireProtectedTenantContext();
        return governanceAccelerationPlanTraceabilityApplicationService.writeAccelerationPlanTrace(request);
    }

    public GovernanceQueryExecutionHistoryWriteResponse writeQueryExecutionHistory(
        GovernanceQueryExecutionHistoryWriteRequest request
    ) {
        requireProtectedTenantContext();
        return governanceQueryExecutionHistoryApplicationService.writeQueryExecutionHistory(request);
    }

    public GovernanceDbViewResolveResponse resolveDbView(GovernanceDbViewResolveRequest request) {
        requireProtectedTenantContext();
        return databaseViewCatalogApplicationService.resolveDbView(request);
    }

    public GovernanceJdbcDatasourceResolveResponse resolveJdbcDatasource(GovernanceJdbcDatasourceResolveRequest request) {
        requireProtectedTenantContext();
        return datasourceConfigApplicationService.resolveJdbcDatasource(request);
    }

    public GovernanceTenantArtifactPolicyResponse resolveTenantArtifactPolicy(
        GovernanceTenantArtifactPolicyRequest request
    ) {
        requireProtectedTenantContext();
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        TenantConfig tenantConfig = tenantConfigRepository.findByTenantId(tenantId).orElse(null);

        GovernanceTenantArtifactPolicyResponse response = new GovernanceTenantArtifactPolicyResponse();
        response.setTenantId(tenantId);
        response.setPolicyScope(request == null ? null : request.getPolicyScope());
        response.setContractStage(CONTRACT_STAGE_LONG_TERM_BASELINE);
        response.setImplementationStage(IMPLEMENTATION_STAGE_TENANT_ARTIFACT_POLICY_BASELINE);
        if (tenantConfig == null) {
            response.setRetentionPolicySource("GOVERNANCE_TENANT_CONFIG_MISSING");
            response.setRetentionPolicyStatus("LONG_TERM_DEFAULT");
            return response;
        }
        Integer retentionDays = tenantConfig.getRetentionDays();
        if (retentionDays == null || retentionDays.intValue() <= 0) {
            response.setRetentionPolicySource("GOVERNANCE_TENANT_CONFIG_RETENTION_DAYS");
            response.setRetentionPolicyStatus("LONG_TERM_DEFAULT");
            return response;
        }
        response.setRetentionDays(retentionDays);
        response.setRetentionPolicySource("GOVERNANCE_TENANT_CONFIG_RETENTION_DAYS");
        response.setRetentionPolicyStatus("TENANT_RETENTION_ACTIVE");
        return response;
    }

    public GovernanceReportInterfaceConfigResponse resolveReportInterfaceConfig(
        GovernanceReportInterfaceConfigRequest request
    ) {
        requireProtectedTenantContext();
        return reportInterfaceConfigApplicationService.resolve(request);
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
