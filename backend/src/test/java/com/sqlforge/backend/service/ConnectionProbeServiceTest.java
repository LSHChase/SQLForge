package com.sqlforge.backend.service;

import com.sqlforge.backend.model.ConnectionProbeResult;
import com.sqlforge.backend.web.dto.ConnectionRequest;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionProbeServiceTest {

    @Test
    void shouldBuildMysqlJdbcUrl() {
        ConnectionProbeService service = new ConnectionProbeService(new EngineCatalogService());
        ConnectionRequest request = request("mysql", "127.0.0.1", 3306, "sqlforge");

        String jdbcUrl = service.buildJdbcUrl(request);

        assertEquals("jdbc:mysql://127.0.0.1:3306/sqlforge", jdbcUrl);
    }

    @Test
    void shouldReportReachableWhenSocketAcceptsConnections() throws Exception {
        ConnectionProbeService service = new ConnectionProbeService(new EngineCatalogService());

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            ConnectionRequest request = request("trino", "127.0.0.1", serverSocket.getLocalPort(), "lakehouse");

            ConnectionProbeResult result = service.probe(request);

            assertTrue(result.isReachable());
            assertEquals("reachable", result.getStatus());
            assertTrue(result.getJdbcUrl().startsWith("jdbc:trino://127.0.0.1:"));
        }
    }

    @Test
    void shouldReportUnreachableWhenSocketCannotConnect() {
        ConnectionProbeService service = new ConnectionProbeService(new EngineCatalogService());
        ConnectionRequest request = request("trino", "127.0.0.1", 1, "lakehouse");

        ConnectionProbeResult result = service.probe(request);

        assertEquals("unreachable", result.getStatus());
        assertTrue(result.getMessages().size() >= 2);
    }

    private ConnectionRequest request(String engineCode, String host, int port, String catalog) {
        ConnectionRequest request = new ConnectionRequest();
        request.setName("Probe");
        request.setEngineCode(engineCode);
        request.setHost(host);
        request.setPort(port);
        request.setCatalog(catalog);
        request.setUsername("analyst");
        request.setPassword("changeit");
        request.setSslEnabled(true);
        return request;
    }
}
