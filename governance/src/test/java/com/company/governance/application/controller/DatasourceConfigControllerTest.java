package com.company.governance.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.governance.application.controller.vo.DatasourceConfigVO;
import com.company.governance.application.controller.vo.DatasourceConnectionTestVO;
import com.company.governance.application.service.DatasourceConfigApplicationService;
import com.company.sqlforge.common.exception.GlobalExceptionHandler;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DatasourceConfigControllerTest {

    @Test
    void shouldExposeDatasourceCrudAndHealthCheckEndpoints() throws Exception {
        DatasourceConfigApplicationService service = org.mockito.Mockito.mock(DatasourceConfigApplicationService.class);
        DatasourceConfigVO config = sampleConfig();
        DatasourceConnectionTestVO health = new DatasourceConnectionTestVO();
        health.setDatasourceId("datasource-001");
        health.setTenantId("tenant-a");
        health.setDatasourceCode("hetu_main");
        health.setEngineType("HETU");
        health.setConnectionStatus("CONNECTED");
        health.setHealthStatus("HEALTHY");
        health.setRealJdbcProbe(Boolean.TRUE);
        health.setElapsedMs(Long.valueOf(7L));
        health.setCheckedAt(Instant.parse("2026-04-27T05:00:00Z"));
        when(service.list("tenant-a")).thenReturn(Collections.singletonList(config));
        when(service.find("tenant-a", "datasource-001")).thenReturn(config);
        when(service.create(any())).thenReturn(config);
        when(service.update(org.mockito.Mockito.eq("datasource-001"), any())).thenReturn(config);
        when(service.testConnection(org.mockito.Mockito.eq("datasource-001"), any())).thenReturn(health);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DatasourceConfigController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        mockMvc.perform(get("/api/governance/datasources").param("tenantId", "tenant-a"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].datasourceId").value("datasource-001"))
            .andExpect(jsonPath("$[0].connectionMode").value("JDBC"))
            .andExpect(jsonPath("$[0].engineType").value("HETU"));

        mockMvc.perform(get("/api/governance/datasources/datasource-001").param("tenantId", "tenant-a"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.healthStatus").value("UNKNOWN"));

        mockMvc.perform(post("/api/governance/datasources")
            .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"datasourceCode\":\"hetu_main\",\"engineType\":\"HETU\","
                    + "\"connectionMode\":\"JDBC\",\"jdbcUrl\":\"jdbc:mysql://localhost:3306/sqlforge\","
                    + "\"jdbcDriverClassName\":\"com.mysql.cj.jdbc.Driver\",\"username\":\"hetu_user\","
                    + "\"credentialSecret\":\"secret\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.datasourceCode").value("hetu_main"));

        mockMvc.perform(put("/api/governance/datasources/datasource-001")
            .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"datasourceCode\":\"hetu_main\",\"engineType\":\"HETU\","
                    + "\"connectionMode\":\"JDBC\",\"jdbcUrl\":\"jdbc:mysql://localhost:3306/sqlforge\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.datasourceId").value("datasource-001"));

        mockMvc.perform(post("/api/governance/datasources/datasource-001/test-connection")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.connectionStatus").value("CONNECTED"))
            .andExpect(jsonPath("$.healthStatus").value("HEALTHY"))
            .andExpect(jsonPath("$.realJdbcProbe").value(true));

        verify(service).list("tenant-a");
        verify(service).find("tenant-a", "datasource-001");
    }

    private DatasourceConfigVO sampleConfig() {
        DatasourceConfigVO config = new DatasourceConfigVO();
        config.setDatasourceId("datasource-001");
        config.setTenantId("tenant-a");
        config.setDatasourceCode("hetu_main");
        config.setDatasourceName("Hetu Main");
        config.setEngineType("HETU");
        config.setConnectionMode("JDBC");
        config.setJdbcUrl("jdbc:mysql://localhost:3306/sqlforge");
        config.setJdbcDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setUsername("hetu_user");
        config.setCredentialMask("****cret");
        config.setEnabled(true);
        config.setReadonly(true);
        config.setTimeoutMs(Integer.valueOf(3000));
        config.setHealthStatus("UNKNOWN");
        config.setContractStage("LONG_TERM_BASELINE");
        config.setImplementationStage("DATASOURCE_CONFIG_BASELINE");
        return config;
    }
}
