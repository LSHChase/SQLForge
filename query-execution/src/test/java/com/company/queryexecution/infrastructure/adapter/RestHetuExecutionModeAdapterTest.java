package com.company.queryexecution.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.domain.query.AccelerationPreference;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

class RestHetuExecutionModeAdapterTest {

    @Test
    void shouldExecuteRestModeAndPreserveReturnedTelemetry() throws Exception {
        AtomicReference<String> authorizationHeader = new AtomicReference<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/query", new JsonResponseHandler(authorizationHeader));
        server.start();
        try {
            QueryExecutionHetuProperties properties = new QueryExecutionHetuProperties();
            properties.getRest().setEndpoint("http://localhost:" + server.getAddress().getPort() + "/query");
            properties.getRest().setAuthToken("secret-token");
            properties.getRest().setMaxRows(1);
            RestHetuExecutionModeAdapter adapter =
                new RestHetuExecutionModeAdapter(properties, new RestTemplateBuilder());

            QueryExecutionStep step = adapter.execute("SELECT 1", acceleratedRequest(), false);

            assertEquals("REST", step.getExecutionMode());
            assertEquals(Collections.singletonList("REST"), step.getAttemptedModes());
            assertEquals(45L, step.getElapsedMs());
            assertEquals(128L, step.getScannedRows());
            assertTrue(step.isCacheHit());
            assertTrue(step.isAccelerationApplied());
            assertEquals(1, step.getRows().size());
            assertEquals("HETU", step.getRows().get(0).get("engine"));
            assertEquals("PRIMARY", step.getRows().get(0).get("mode"));
            assertEquals("REST", step.getRows().get(0).get("executionMode"));
            assertEquals(1, ((Number) step.getRows().get(0).get("id")).intValue());
            assertEquals("READY", step.getRows().get(0).get("state"));
            assertEquals("Bearer secret-token", authorizationHeader.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldRejectRestModeWithoutEndpoint() {
        RestHetuExecutionModeAdapter adapter =
            new RestHetuExecutionModeAdapter(new QueryExecutionHetuProperties(), new RestTemplateBuilder());

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> adapter.execute("SELECT 1", acceleratedRequest(), false)
        );

        assertEquals("Hetu REST endpoint is not configured", ex.getMessage());
    }

    private QueryExecuteRequest acceleratedRequest() {
        QueryExecuteRequest request = new QueryExecuteRequest();
        request.setTenantId("tenant-a");
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        request.setAccelerationPreference(AccelerationPreference.PREFER_ACCELERATED);
        return request;
    }

    private static final class JsonResponseHandler implements HttpHandler {

        private final AtomicReference<String> authorizationHeader;

        private JsonResponseHandler(AtomicReference<String> authorizationHeader) {
            this.authorizationHeader = authorizationHeader;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] responseBody = (
                "{\"rows\":[{\"id\":1,\"state\":\"READY\"},{\"id\":2,\"state\":\"DONE\"}],"
                    + "\"elapsedMs\":45,\"scannedRows\":128,\"cacheHit\":true}"
            ).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBody.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBody);
            }
        }
    }
}
