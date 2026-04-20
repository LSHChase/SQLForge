package com.company.governance.application.controller;

import com.company.governance.application.controller.dto.AuditWriteRequest;
import com.company.governance.application.controller.dto.DatasourceAccessCheckRequest;
import com.company.governance.application.controller.dto.TenantScopeCheckRequest;
import com.company.governance.application.controller.vo.AuditWriteResponse;
import com.company.governance.application.controller.vo.DatasourceAccessCheckResponse;
import com.company.governance.application.controller.vo.ScheduleExtensionStatusVO;
import com.company.governance.application.controller.vo.TenantScopeCheckResponse;
import com.company.governance.application.service.GovernanceCapabilityApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance/internal")
public class GovernanceCapabilityController {

    private final GovernanceCapabilityApplicationService governanceCapabilityApplicationService;

    public GovernanceCapabilityController(
        GovernanceCapabilityApplicationService governanceCapabilityApplicationService) {
        this.governanceCapabilityApplicationService = governanceCapabilityApplicationService;
    }

    @PostMapping("/tenant-scope/check")
    public TenantScopeCheckResponse checkTenantScope(@RequestBody TenantScopeCheckRequest request) {
        return governanceCapabilityApplicationService.checkTenantScope(request);
    }

    @PostMapping("/datasource-access/check")
    public DatasourceAccessCheckResponse checkDatasourceAccess(@RequestBody DatasourceAccessCheckRequest request) {
        return governanceCapabilityApplicationService.checkDatasourceAccess(request);
    }

    @PostMapping("/audit/write")
    public AuditWriteResponse writeAudit(@RequestBody AuditWriteRequest request) {
        return governanceCapabilityApplicationService.publishAuditEvent(request);
    }

    @GetMapping("/schedule/extensions")
    public ScheduleExtensionStatusVO scheduleExtensions() {
        return governanceCapabilityApplicationService.getScheduleExtensionStatus();
    }
}
