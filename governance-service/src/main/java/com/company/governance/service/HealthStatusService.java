package com.company.governance.service;

import com.company.governance.controller.vo.HealthStatusVO;
import org.springframework.stereotype.Service;

@Service
public class HealthStatusService {

    public HealthStatusVO currentStatus() {
        return new HealthStatusVO("governance-service", "UP");
    }
}
