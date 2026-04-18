package com.company.governance.controller;

import com.company.governance.controller.vo.HealthStatusVO;
import com.company.governance.service.HealthStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance/health")
public class HealthController {

    private final HealthStatusService healthStatusService;

    public HealthController(HealthStatusService healthStatusService) {
        this.healthStatusService = healthStatusService;
    }

    @GetMapping
    public HealthStatusVO getHealthStatus() {
        return healthStatusService.currentStatus();
    }
}
