package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.governance.application.controller.dto.DatasourceConfigUpsertRequest;
import com.company.governance.application.controller.dto.DatasourceConnectionTestRequest;
import com.company.governance.application.controller.vo.DatasourceConfigVO;
import com.company.governance.application.controller.vo.DatasourceConnectionTestVO;
import com.company.governance.domain.datasource.DatasourceConfig;
import com.company.governance.infrastructure.repository.InMemoryDatasourceConfigRepository;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveResponse;
import com.company.sqlforge.common.security.SensitiveDataCryptoProperties;
import com.company.sqlforge.common.security.SensitiveDataCryptoService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DatasourceConfigApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldCreateListAndTestHealthyJdbcDatasource() {
        RequestContext.set("tenant-a", "admin-001", "request-010", "trace-010", "header", 1L, 2L);
        DatasourceConfigApplicationService service =
            newService(new InMemoryDatasourceConfigRepository(), successfulProbe());

        DatasourceConfigVO created = service.create(jdbcRequest("jdbc:mysql://localhost:3306/sqlforge"));
        List<DatasourceConfigVO> listed = service.list("tenant-a");
        DatasourceConnectionTestVO health = service.testConnection(created.getDatasourceId(), new DatasourceConnectionTestRequest());

        assertEquals("JDBC", created.getConnectionMode());
        assertEquals("HETU", created.getEngineType());
        assertEquals("****cret", created.getCredentialMask());
        assertEquals(1, listed.size());
        assertEquals("CONNECTED", health.getConnectionStatus());
        assertEquals("HEALTHY", health.getHealthStatus());
        assertTrue(health.getRealJdbcProbe());
        assertNull(health.getLastFailureReason());
    }

    @Test
    void shouldReturnFailedHealthCheckForUnreachableApiDatasource() {
        RequestContext.set("tenant-a", "admin-002", "request-011", "trace-011", "header", 1L, 2L);
        DatasourceConfigApplicationService service =
            newService(new InMemoryDatasourceConfigRepository(), successfulProbe());

        DatasourceConfigUpsertRequest request = new DatasourceConfigUpsertRequest();
        request.setTenantId("tenant-a");
        request.setDatasourceCode("report_api");
        request.setDatasourceName("Report API");
        request.setConnectionMode("API");
        request.setApiBaseUrl("http://unreachable-report-api.local");
        request.setCredentialMode("TOKEN");
        request.setCredentialSecret("token-1234");
        DatasourceConfigVO created = service.create(request);

        DatasourceConnectionTestRequest healthRequest = new DatasourceConnectionTestRequest();
        healthRequest.setTenantId("tenant-a");
        healthRequest.setTimeoutMsOverride(Integer.valueOf(5000));
        DatasourceConnectionTestVO health = service.testConnection(created.getDatasourceId(), healthRequest);

        assertEquals("FAILED", health.getConnectionStatus());
        assertEquals("DEGRADED", health.getHealthStatus());
        assertEquals("CONNECTION_ENDPOINT_UNREACHABLE", health.getLastFailureReason());
        assertEquals(Integer.valueOf(5000), health.getTimeoutMs());
    }

    @Test
    void shouldEncryptPasswordAndResolvePlaintextOnlyForInternalJdbcEndpoint() {
        RequestContext.set("tenant-a", "service-001", "request-012", "trace-012", "header", 1L, 2L);
        InMemoryDatasourceConfigRepository repository = new InMemoryDatasourceConfigRepository();
        DatasourceConfigApplicationService service = newService(repository, successfulProbe());

        DatasourceConfigVO created = service.create(jdbcRequest("jdbc:hetu://coordinator:8080/hive/default"));
        DatasourceConfig stored = repository.findByTenantIdAndDatasourceId("tenant-a", created.getDatasourceId()).get();
        assertTrue(stored.getCredentialCiphertext().startsWith("ENC::AES256_GCM::"));
        assertFalse(stored.getCredentialCiphertext().contains("secret"));

        GovernanceJdbcDatasourceResolveRequest request = new GovernanceJdbcDatasourceResolveRequest();
        request.setTenantId("tenant-a");
        request.setDatasourceCode("hetu_main");
        request.setEngineType("HETU");

        GovernanceJdbcDatasourceResolveResponse response = service.resolveJdbcDatasource(request);

        assertTrue(response.isResolved());
        assertEquals("jdbc:hetu://coordinator:8080/hive/default", response.getJdbcUrl());
        assertEquals("secret", response.getPassword());
        assertEquals("****cret", response.getCredentialMask());
    }

    @Test
    void shouldReturnFailedRealJdbcProbeReason() {
        RequestContext.set("tenant-a", "admin-003", "request-013", "trace-013", "header", 1L, 2L);
        DatasourceConfigApplicationService service = newService(
            new InMemoryDatasourceConfigRepository(),
            (config, password, timeoutMs) -> new DatasourceJdbcConnectionProbe.JdbcProbeResult(false, "JDBC_CONNECT_FAILED: refused", 11L)
        );

        DatasourceConfigVO created = service.create(jdbcRequest("jdbc:hetu://unreachable:8080/hive/default"));
        DatasourceConnectionTestVO health = service.testConnection(created.getDatasourceId(), new DatasourceConnectionTestRequest());

        assertEquals("FAILED", health.getConnectionStatus());
        assertEquals("DEGRADED", health.getHealthStatus());
        assertTrue(health.getRealJdbcProbe());
        assertEquals("JDBC_CONNECT_FAILED: refused", health.getLastFailureReason());
        assertEquals(Long.valueOf(11L), health.getElapsedMs());
    }

    private DatasourceConfigUpsertRequest jdbcRequest(String jdbcUrl) {
        DatasourceConfigUpsertRequest request = new DatasourceConfigUpsertRequest();
        request.setTenantId("tenant-a");
        request.setDatasourceCode("hetu_main");
        request.setDatasourceName("Hetu Main");
        request.setEngineType("HETU");
        request.setConnectionMode("JDBC");
        request.setJdbcUrl(jdbcUrl);
        request.setJdbcDriverClassName("io.prestosql.jdbc.PrestoDriver");
        request.setUsername("hetu_user");
        request.setCredentialMode("PASSWORD");
        request.setCredentialSecret("secret");
        request.setAuthMode("PASSWORD");
        return request;
    }

    private DatasourceConfigApplicationService newService(InMemoryDatasourceConfigRepository repository,
                                                          DatasourceJdbcConnectionProbe probe) {
        SensitiveDataCryptoProperties properties = new SensitiveDataCryptoProperties();
        properties.setBase64Key("MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=");
        return new DatasourceConfigApplicationService(repository, new SensitiveDataCryptoService(properties), probe);
    }

    private DatasourceJdbcConnectionProbe successfulProbe() {
        return (config, password, timeoutMs) -> new DatasourceJdbcConnectionProbe.JdbcProbeResult(true, null, 7L);
    }
}
