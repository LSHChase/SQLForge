package com.company.governance.application.controller;

import com.company.governance.application.controller.dto.ReportInterfaceConfigUpsertRequest;
import com.company.governance.application.service.ReportInterfaceConfigApplicationService;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/governance/report-interface-configs", "/api/governance/report-interfaces"})
public class ReportInterfaceConfigController {

    private final ReportInterfaceConfigApplicationService reportInterfaceConfigApplicationService;

    public ReportInterfaceConfigController(ReportInterfaceConfigApplicationService reportInterfaceConfigApplicationService) {
        this.reportInterfaceConfigApplicationService = reportInterfaceConfigApplicationService;
    }

    @PostMapping
    public GovernanceReportInterfaceConfigResponse upsert(@RequestBody ReportInterfaceConfigUpsertRequest request) {
        return reportInterfaceConfigApplicationService.upsert(request);
    }

    @PutMapping("/{interfaceId}")
    public GovernanceReportInterfaceConfigResponse update(@PathVariable("interfaceId") String interfaceId,
                                                          @RequestBody ReportInterfaceConfigUpsertRequest request) {
        return reportInterfaceConfigApplicationService.update(interfaceId, request);
    }

    @GetMapping
    public List<GovernanceReportInterfaceConfigResponse> list(@RequestParam("tenantId") String tenantId) {
        return reportInterfaceConfigApplicationService.list(tenantId);
    }
}
