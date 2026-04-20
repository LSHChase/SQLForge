package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.company.governance.application.controller.vo.HealthStatusVO;
import org.junit.jupiter.api.Test;

class HealthStatusApplicationServiceTest {

    @Test
    void shouldReturnPublicUpStatus() {
        HealthStatusApplicationService service = new HealthStatusApplicationService();

        HealthStatusVO status = service.currentStatus();

        assertEquals("governance-service", status.getService());
        assertEquals("UP", status.getStatus());
        assertEquals("PUBLIC", status.getAccessScope());
        assertNull(status.getTenantId());
        assertNull(status.getUserId());
        assertNull(status.getRole());
    }
}
