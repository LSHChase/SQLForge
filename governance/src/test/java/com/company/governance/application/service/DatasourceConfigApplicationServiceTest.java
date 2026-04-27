package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.company.governance.application.controller.dto.DatasourceConfigUpsertRequest;
import com.company.governance.application.controller.dto.DatasourceConnectionTestRequest;
import com.company.governance.application.controller.vo.DatasourceConfigVO;
import com.company.governance.application.controller.vo.DatasourceConnectionTestVO;
import com.company.governance.infrastructure.repository.InMemoryDatasourceConfigRepository;
import com.company.sqlforge.common.context.RequestContext;
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
        RequestContext.set("tenant-a", "admin-001", Arrays.asList("TENANT_ADMIN"), "request-010", "trace-010", "header", 1L, 2L);
        DatasourceConfigApplicationService service =
            new DatasourceConfigApplicationService(new InMemoryDatasourceConfigRepository());

        DatasourceConfigVO created = service.create(jdbcRequest("jdbc:mysql://localhost:3306/sqlforge"));
        List<DatasourceConfigVO> listed = service.list("tenant-a");
        DatasourceConnectionTestVO health = service.testConnection(created.getDatasourceId(), new DatasourceConnectionTestRequest());

        assertEquals("JDBC", created.getConnectionMode());
        assertEquals("****cret", created.getCredentialMask());
        assertEquals(1, listed.size());
        assertEquals("CONNECTED", health.getConnectionStatus());
        assertEquals("HEALTHY", health.getHealthStatus());
        assertNull(health.getLastFailureReason());
    }

    @Test
    void shouldReturnFailedHealthCheckForUnreachableApiDatasource() {
        RequestContext.set("tenant-a", "admin-002", Arrays.asList("TENANT_ADMIN"), "request-011", "trace-011", "header", 1L, 2L);
        DatasourceConfigApplicationService service =
            new DatasourceConfigApplicationService(new InMemoryDatasourceConfigRepository());

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

    private DatasourceConfigUpsertRequest jdbcRequest(String jdbcUrl) {
        DatasourceConfigUpsertRequest request = new DatasourceConfigUpsertRequest();
        request.setTenantId("tenant-a");
        request.setDatasourceCode("hetu_main");
        request.setDatasourceName("Hetu Main");
        request.setConnectionMode("JDBC");
        request.setJdbcUrl(jdbcUrl);
        request.setCredentialMode("PASSWORD");
        request.setCredentialSecret("secret");
        request.setAuthMode("PASSWORD");
        return request;
    }
}
