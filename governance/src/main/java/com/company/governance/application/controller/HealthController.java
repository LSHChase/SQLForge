package com.company.governance.application.controller;

import com.company.governance.application.controller.vo.HealthStatusVO;
import com.company.governance.application.service.HealthStatusApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance/health")
public class HealthController {

    private final HealthStatusApplicationService healthStatusApplicationService;

    public HealthController(HealthStatusApplicationService healthStatusApplicationService) {
        this.healthStatusApplicationService = healthStatusApplicationService;
    }

    @GetMapping
    public HealthStatusVO getHealthStatus() {
        return healthStatusApplicationService.currentStatus();
    }
}
