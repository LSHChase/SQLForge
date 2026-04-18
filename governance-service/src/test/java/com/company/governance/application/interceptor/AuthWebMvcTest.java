package com.company.governance.application.interceptor;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.governance.application.controller.HealthController;
import com.company.governance.application.controller.TenantConfigController;
import com.company.governance.application.controller.vo.HealthStatusVO;
import com.company.governance.application.controller.vo.TenantConfigVO;
import com.company.governance.application.service.HealthStatusApplicationService;
import com.company.governance.application.service.TenantConfigApplicationService;
import com.company.governance.config.AuthProperties;
import com.company.governance.config.WebMvcConfig;
import com.company.governance.infrastructure.persistence.mapper.TenantConfigMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

@WebMvcTest(controllers = {HealthController.class, TenantConfigController.class})
@Import({WebMvcConfig.class, AuthInterceptor.class})
class AuthWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthStatusApplicationService healthStatusApplicationService;

    @MockBean
    private TenantConfigApplicationService tenantConfigApplicationService;

    @MockBean
    private TenantConfigMapper tenantConfigMapper;

    @Test
    void shouldBypassInterceptorForHealthEndpoint() throws Exception {
        when(healthStatusApplicationService.currentStatus())
            .thenReturn(new HealthStatusVO("governance-service", "UP", null, null, null, "PUBLIC"));

        mockMvc.perform(get("/api/governance/health"))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("X-Trace-Id"))
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldResolveTenantHeaderAndTraceIdForTenantConfigEndpoint() throws Exception {
        when(tenantConfigApplicationService.findByTenantId("system"))
            .thenReturn(new TenantConfigVO("system", 20, 2048, "HETU", "HIVE", "NORMAL", 180, 50));

        mockMvc.perform(get("/api/governance/tenant-config").header("X-Tenant-Id", "system"))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Trace-Id"))
            .andExpect(jsonPath("$.tenantId").value("system"));

        verify(tenantConfigApplicationService).findByTenantId("system");
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        AuthProperties authProperties() {
            AuthProperties authProperties = new AuthProperties();
            authProperties.setEnabled(false);
            return authProperties;
        }
    }
}
