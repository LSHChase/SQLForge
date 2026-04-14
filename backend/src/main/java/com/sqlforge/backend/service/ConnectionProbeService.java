package com.sqlforge.backend.service;

import com.sqlforge.backend.model.ConnectionProbeResult;
import com.sqlforge.backend.model.EngineDescriptor;
import com.sqlforge.backend.web.dto.ConnectionRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ConnectionProbeService {

    private static final int DEFAULT_TIMEOUT_MS = 2000;

    private final JdbcConnectionSupportService jdbcConnectionSupportService;

    public ConnectionProbeService(JdbcConnectionSupportService jdbcConnectionSupportService) {
        this.jdbcConnectionSupportService = jdbcConnectionSupportService;
    }

    public ConnectionProbeResult probe(ConnectionRequest request) {
        EngineDescriptor engine = jdbcConnectionSupportService.findEngine(request.getEngineCode());
        String jdbcUrl = jdbcConnectionSupportService.buildJdbcUrl(request);
        String driverClassName = engine == null ? "" : engine.getDriverClassName();
        long startedAt = System.currentTimeMillis();

        try {
            probeSocket(request.getHost().trim(), request.getPort().intValue(), DEFAULT_TIMEOUT_MS);

            if (!jdbcConnectionSupportService.isDriverAvailable(driverClassName)) {
                long duration = System.currentTimeMillis() - startedAt;
                return new ConnectionProbeResult(
                    true,
                    "driver-missing",
                    "reachable",
                    "missing",
                    jdbcUrl,
                    driverClassName,
                    false,
                    duration,
                    Arrays.asList("tcp connectivity check succeeded", "jdbc driver is not available on the classpath")
                );
            }

            try {
                attemptJdbcConnection(jdbcUrl, request);
                long duration = System.currentTimeMillis() - startedAt;
                return new ConnectionProbeResult(
                    true,
                    "connected",
                    "reachable",
                    "loaded",
                    jdbcUrl,
                    driverClassName,
                    true,
                    duration,
                    Arrays.asList("tcp connectivity check succeeded", "jdbc driver loaded", "jdbc connection succeeded")
                );
            } catch (SQLException exception) {
                long duration = System.currentTimeMillis() - startedAt;
                return new ConnectionProbeResult(
                    true,
                    "jdbc-connect-failed",
                    "reachable",
                    "loaded",
                    jdbcUrl,
                    driverClassName,
                    true,
                    duration,
                    Arrays.asList("tcp connectivity check succeeded", "jdbc driver loaded", exception.getMessage())
                );
            }
        } catch (IOException exception) {
            long duration = System.currentTimeMillis() - startedAt;
            return new ConnectionProbeResult(
                false,
                "unreachable",
                "unreachable",
                "not-attempted",
                jdbcUrl,
                driverClassName,
                false,
                duration,
                Arrays.asList("tcp connectivity check failed", exception.getMessage())
            );
        }
    }

    public String buildJdbcUrl(ConnectionRequest request) {
        return jdbcConnectionSupportService.buildJdbcUrl(request);
    }

    protected void attemptJdbcConnection(String jdbcUrl, ConnectionRequest request) throws SQLException {
        try (Connection ignored = jdbcConnectionSupportService.openConnection(jdbcUrl, request, DEFAULT_TIMEOUT_MS)) {
            // successful probe
        }
    }

    protected void probeSocket(String host, int port, int timeoutMs) throws IOException {
        Socket socket = new Socket();

        try {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
                // best-effort cleanup
            }
        }
    }
}
