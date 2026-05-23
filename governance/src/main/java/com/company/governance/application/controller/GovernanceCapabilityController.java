package com.company.governance.application.controller;

import com.company.governance.application.controller.dto.AuditWriteRequest;
import com.company.governance.application.controller.dto.DatasourceAccessScopeChangeRequest;
import com.company.governance.application.controller.vo.AuditWriteResponse;
import com.company.governance.application.controller.vo.DatasourceAccessScopeChangeResponse;
import com.company.governance.application.controller.vo.ScheduleExtensionStatusVO;
import com.company.governance.application.service.GovernanceCapabilityApplicationService;
import com.company.governance.application.service.GovernanceSqlRewriteDivergenceAlertApplicationService;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceRequest;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceResponse;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertResponse;
import com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceResponse;
import com.company.sqlforge.common.governance.GovernanceDatasourceAccessCheckRequest;
import com.company.sqlforge.common.governance.GovernanceDatasourceAccessCheckResponse;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveRequest;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveResponse;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveResponse;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveResponse;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteResponse;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigRequest;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigResponse;
import com.company.sqlforge.common.governance.GovernanceSqlRewriteDivergenceAlertRequest;
import com.company.sqlforge.common.governance.GovernanceSqlRewriteDivergenceAlertResponse;
import com.company.sqlforge.common.governance.GovernanceTenantArtifactPolicyRequest;
import com.company.sqlforge.common.governance.GovernanceTenantArtifactPolicyResponse;
import com.company.sqlforge.common.governance.GovernanceTenantScopeCheckRequest;
import com.company.sqlforge.common.governance.GovernanceTenantScopeCheckResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance/internal")
public class GovernanceCapabilityController {

    private final GovernanceCapabilityApplicationService governanceCapabilityApplicationService;
    private final GovernanceSqlRewriteDivergenceAlertApplicationService sqlRewriteDivergenceAlertApplicationService;

    public GovernanceCapabilityController(
        GovernanceCapabilityApplicationService governanceCapabilityApplicationService,
        GovernanceSqlRewriteDivergenceAlertApplicationService sqlRewriteDivergenceAlertApplicationService) {
        this.governanceCapabilityApplicationService = governanceCapabilityApplicationService;
        this.sqlRewriteDivergenceAlertApplicationService = sqlRewriteDivergenceAlertApplicationService;
    }

    @PostMapping("/tenant-scope/check")
    public GovernanceTenantScopeCheckResponse checkTenantScope(@RequestBody GovernanceTenantScopeCheckRequest request) {
        return governanceCapabilityApplicationService.checkTenantScope(request);
    }

    @PostMapping("/datasource-access/check")
    public GovernanceDatasourceAccessCheckResponse checkDatasourceAccess(
        @RequestBody GovernanceDatasourceAccessCheckRequest request
    ) {
        return governanceCapabilityApplicationService.checkDatasourceAccess(request);
    }

    @PostMapping("/datasource-access/scope/change")
    public DatasourceAccessScopeChangeResponse changeDatasourceAccessScope(
        @RequestBody DatasourceAccessScopeChangeRequest request
    ) {
        return governanceCapabilityApplicationService.changeDatasourceAccessScope(request);
    }

    @PostMapping("/audit/write")
    public AuditWriteResponse writeAudit(@RequestBody AuditWriteRequest request) {
        return governanceCapabilityApplicationService.publishAuditEvent(request);
    }

    @PostMapping("/query-execution-history/write")
    public GovernanceQueryExecutionHistoryWriteResponse writeQueryExecutionHistory(
        @RequestBody GovernanceQueryExecutionHistoryWriteRequest request
    ) {
        return governanceCapabilityApplicationService.writeQueryExecutionHistory(request);
    }

    @PostMapping("/benchmark/report-trace/write")
    public GovernanceBenchmarkReportTraceResponse writeBenchmarkReportTrace(
        @RequestBody GovernanceBenchmarkReportTraceRequest request
    ) {
        return governanceCapabilityApplicationService.writeBenchmarkReportTrace(request);
    }

    @PostMapping("/alerts/benchmark-regression/emit")
    public GovernanceBenchmarkRegressionAlertResponse emitBenchmarkRegressionAlert(
        @RequestBody GovernanceBenchmarkRegressionAlertRequest request
    ) {
        return governanceCapabilityApplicationService.emitBenchmarkRegressionAlert(request);
    }

    @PostMapping("/alerts/sql-rewrite-divergence/emit")
    public GovernanceSqlRewriteDivergenceAlertResponse emitSqlRewriteDivergenceAlert(
        @RequestBody GovernanceSqlRewriteDivergenceAlertRequest request
    ) {
        return sqlRewriteDivergenceAlertApplicationService.emit(request);
    }

    @PostMapping("/acceleration-plan/trace/write")
    public GovernanceAccelerationPlanTraceResponse writeAccelerationPlanTrace(
        @RequestBody GovernanceAccelerationPlanTraceRequest request
    ) {
        return governanceCapabilityApplicationService.writeAccelerationPlanTrace(request);
    }

    @PostMapping("/db-views/resolve")
    public GovernanceDbViewResolveResponse resolveDbView(@RequestBody GovernanceDbViewResolveRequest request) {
        return governanceCapabilityApplicationService.resolveDbView(request);
    }

    @PostMapping("/datasources/jdbc/resolve")
    public GovernanceJdbcDatasourceResolveResponse resolveJdbcDatasource(
        @RequestBody GovernanceJdbcDatasourceResolveRequest request
    ) {
        return governanceCapabilityApplicationService.resolveJdbcDatasource(request);
    }

    @PostMapping("/datasources/jdbc/resolve-route")
    public GovernanceJdbcRouteResolveResponse resolveJdbcRoute(
        @RequestBody GovernanceJdbcRouteResolveRequest request
    ) {
        return governanceCapabilityApplicationService.resolveJdbcRoute(request);
    }

    @PostMapping("/tenant-artifact-policy/resolve")
    public GovernanceTenantArtifactPolicyResponse resolveTenantArtifactPolicy(
        @RequestBody GovernanceTenantArtifactPolicyRequest request
    ) {
        return governanceCapabilityApplicationService.resolveTenantArtifactPolicy(request);
    }

    @PostMapping("/report-interface-configs/resolve")
    public GovernanceReportInterfaceConfigResponse resolveReportInterfaceConfig(
        @RequestBody GovernanceReportInterfaceConfigRequest request
    ) {
        return governanceCapabilityApplicationService.resolveReportInterfaceConfig(request);
    }

    @GetMapping("/schedule/extensions")
    public ScheduleExtensionStatusVO scheduleExtensions() {
        return governanceCapabilityApplicationService.getScheduleExtensionStatus();
    }
}
