package com.company.governance.application.service;

import com.company.governance.application.controller.vo.HealthStatusVO;
import org.springframework.stereotype.Service;

@Service
public class HealthStatusApplicationService {

    public HealthStatusVO currentStatus() {
        return new HealthStatusVO(
            "governance",
            "UP",
            null,
            null,
            null,
            "PUBLIC"
        );
    }
}
