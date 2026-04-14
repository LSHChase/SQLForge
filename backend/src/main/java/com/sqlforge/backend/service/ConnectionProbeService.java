package com.sqlforge.backend.service;

import com.sqlforge.backend.model.ConnectionProbeResult;
import com.sqlforge.backend.model.EngineDescriptor;
import com.sqlforge.backend.web.dto.ConnectionRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ConnectionProbeService {

    private static final int DEFAULT_TIMEOUT_MS = 2000;

    private final EngineCatalogService engineCatalogService;

    public ConnectionProbeService(EngineCatalogService engineCatalogService) {
        this.engineCatalogService = engineCatalogService;
    }

    public ConnectionProbeResult probe(ConnectionRequest request) {
        String jdbcUrl = buildJdbcUrl(request);
        long startedAt = System.currentTimeMillis();

        try {
            probeSocket(request.getHost().trim(), request.getPort().intValue(), DEFAULT_TIMEOUT_MS);
            long duration = System.currentTimeMillis() - startedAt;
            return new ConnectionProbeResult(
                true,
                "reachable",
                jdbcUrl,
                duration,
                Arrays.asList("tcp connectivity check succeeded", "jdbc url generated")
            );
        } catch (IOException exception) {
            long duration = System.currentTimeMillis() - startedAt;
            return new ConnectionProbeResult(
                false,
                "unreachable",
                jdbcUrl,
                duration,
                Arrays.asList("tcp connectivity check failed", exception.getMessage())
            );
        }
    }

    public String buildJdbcUrl(ConnectionRequest request) {
        EngineDescriptor engine = engineCatalogService.findByCode(request.getEngineCode());

        if (engine == null) {
            return "unsupported://" + request.getHost() + ":" + request.getPort();
        }

        String engineCode = engine.getCode().toLowerCase(Locale.ROOT);
        String host = request.getHost().trim();
        Integer port = request.getPort();
        String catalog = request.getCatalog().trim();

        if ("mysql".equals(engineCode)) {
            return "jdbc:mysql://" + host + ":" + port + "/" + catalog;
        }

        if ("clickhouse".equals(engineCode)) {
            return "jdbc:clickhouse://" + host + ":" + port + "/" + catalog;
        }

        if ("kyligence".equals(engineCode)) {
            return "jdbc:kylin://" + host + ":" + port + "/" + catalog;
        }

        if ("trino".equals(engineCode) || "presto".equals(engineCode) || "mrs-hetu".equals(engineCode)) {
            return "jdbc:" + engineCode + "://" + host + ":" + port + "/" + catalog;
        }

        return "jdbc:" + engineCode + "://" + host + ":" + port + "/" + catalog;
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
