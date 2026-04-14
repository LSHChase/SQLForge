package com.sqlforge.backend.service;

import com.sqlforge.backend.model.ConnectionProbeResult;
import com.sqlforge.backend.model.EngineDescriptor;
import com.sqlforge.backend.web.dto.ConnectionRequest;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.net.ServerSocket;
import java.util.Properties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionProbeServiceTest {

    @Test
    void shouldBuildMysqlJdbcUrl() {
        ConnectionProbeService service = new ConnectionProbeService(
            new JdbcConnectionSupportService(new EngineCatalogService())
        );
        ConnectionRequest request = request("mysql", "127.0.0.1", 3306, "sqlforge");

        String jdbcUrl = service.buildJdbcUrl(request);

        assertEquals("jdbc:mysql://127.0.0.1:3306/sqlforge", jdbcUrl);
    }

    @Test
    void shouldReportReachableWhenSocketAcceptsConnections() throws Exception {
        ConnectionProbeService service = new ConnectionProbeService(
            new JdbcConnectionSupportService(new EngineCatalogService())
        );

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            ConnectionRequest request = request("trino", "127.0.0.1", serverSocket.getLocalPort(), "lakehouse");

            ConnectionProbeResult result = service.probe(request);

            assertTrue(result.isReachable());
            assertEquals("driver-missing", result.getStatus());
            assertEquals("reachable", result.getTransportStatus());
            assertEquals("missing", result.getDriverStatus());
            assertTrue(result.getJdbcUrl().startsWith("jdbc:trino://127.0.0.1:"));
        }
    }

    @Test
    void shouldReportUnreachableWhenSocketCannotConnect() {
        ConnectionProbeService service = new ConnectionProbeService(
            new JdbcConnectionSupportService(new EngineCatalogService())
        );
        ConnectionRequest request = request("trino", "127.0.0.1", 1, "lakehouse");

        ConnectionProbeResult result = service.probe(request);

        assertEquals("unreachable", result.getStatus());
        assertEquals("unreachable", result.getTransportStatus());
        assertEquals("not-attempted", result.getDriverStatus());
        assertTrue(result.getMessages().size() >= 2);
    }

    @Test
    void shouldReportConnectedWhenDriverLoadsAndJdbcConnectSucceeds() {
        ConnectionProbeService service = new ProbeServiceForTest(new FakeEngineCatalogService(), true);
        ConnectionRequest request = request("fake-engine", "fake-host", 1111, "catalog");

        ConnectionProbeResult result = service.probe(request);

        assertTrue(result.isReachable());
        assertEquals("connected", result.getStatus());
        assertEquals("loaded", result.getDriverStatus());
        assertTrue(result.isJdbcAttempted());
        assertEquals(FakeJdbcDriver.class.getName(), result.getDriverClassName());
    }

    @Test
    void shouldReportJdbcConnectFailedWhenDriverRejectsConnection() {
        ConnectionProbeService service = new ProbeServiceForTest(new FakeEngineCatalogService(), false);
        ConnectionRequest request = request("fake-engine", "fake-host", 1111, "catalog");

        ConnectionProbeResult result = service.probe(request);

        assertTrue(result.isReachable());
        assertEquals("jdbc-connect-failed", result.getStatus());
        assertEquals("loaded", result.getDriverStatus());
        assertTrue(result.isJdbcAttempted());
        assertFalse(result.getMessages().isEmpty());
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

    static class ProbeServiceForTest extends ConnectionProbeService {

        private final boolean connectSuccessfully;

        ProbeServiceForTest(EngineCatalogService engineCatalogService, boolean connectSuccessfully) {
            super(new JdbcConnectionSupportService(engineCatalogService));
            this.connectSuccessfully = connectSuccessfully;
        }

        @Override
        protected void probeSocket(String host, int port, int timeoutMs) {
            // bypass network for deterministic JDBC tests
        }

        @Override
        protected void attemptJdbcConnection(String jdbcUrl, ConnectionRequest request) throws SQLException {
            if (!connectSuccessfully) {
                throw new SQLException("simulated jdbc connection failure");
            }
        }
    }

    static class FakeEngineCatalogService extends EngineCatalogService {

        @Override
        public EngineDescriptor findByCode(String code) {
            if ("fake-engine".equals(code)) {
                return new EngineDescriptor(
                    "fake-engine",
                    "Fake Engine",
                    "test",
                    1111,
                    "tcp",
                    "fake",
                    FakeJdbcDriver.class.getName(),
                    "fake profile for jdbc-aware probe tests",
                    true
                );
            }

            return super.findByCode(code);
        }
    }

    public static final class FakeJdbcDriver implements Driver {

        static {
            try {
                DriverManager.registerDriver(new FakeJdbcDriver());
            } catch (SQLException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public Connection connect(String url, Properties info) {
            return null;
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:fake://");
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return java.util.logging.Logger.getGlobal();
        }
    }
}
