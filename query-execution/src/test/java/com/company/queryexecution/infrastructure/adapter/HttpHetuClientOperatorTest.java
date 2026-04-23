package com.company.queryexecution.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.domain.query.AccelerationPreference;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

class HttpHetuClientOperatorTest {

    @Test
    void shouldExecuteHetuClientProtocolAndAggregatePages() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<String>();
        AtomicReference<String> userHeader = new AtomicReference<String>();
        AtomicReference<String> sourceHeader = new AtomicReference<String>();
        AtomicReference<String> catalogHeader = new AtomicReference<String>();
        AtomicReference<String> schemaHeader = new AtomicReference<String>();
        AtomicReference<String> authorizationHeader = new AtomicReference<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/statement", new InitialStatementHandler(
            requestBody,
            userHeader,
            sourceHeader,
            catalogHeader,
            schemaHeader,
            authorizationHeader,
            server
        ));
        server.createContext("/v1/statement/query-1/next", new NextStatementHandler());
        server.start();
        try {
            QueryExecutionHetuProperties properties = new QueryExecutionHetuProperties();
            properties.getClient().setEnabled(true);
            properties.getClient().setEndpoint("http://localhost:" + server.getAddress().getPort() + "/v1/statement");
            properties.getClient().setUser("sqlforge-smoke");
            properties.getClient().setSource("sqlforge-client");
            properties.getClient().setCatalog("hive");
            properties.getClient().setSchema("default");
            properties.getClient().setAuthToken("secret-token");
            properties.getClient().setMaxRows(2);
            HttpHetuClientOperator operator = new HttpHetuClientOperator(properties, new RestTemplateBuilder());

            QueryExecuteRequest request = new QueryExecuteRequest();
            request.setAccelerationPreference(AccelerationPreference.PREFER_ACCELERATED);

            com.company.queryexecution.domain.query.QueryExecutionStep step =
                operator.execute("SELECT id, state FROM orders", request, false);

            assertEquals(27L, step.getElapsedMs());
            assertEquals(128L, step.getScannedRows());
            assertTrue(step.isAccelerationApplied());
            assertEquals(2, step.getRows().size());
            assertEquals(1, ((Number) step.getRows().get(0).get("id")).intValue());
            assertEquals("DONE", step.getRows().get(1).get("state"));
            assertEquals("SELECT id, state FROM orders", requestBody.get());
            assertEquals("sqlforge-smoke", userHeader.get());
            assertEquals("sqlforge-client", sourceHeader.get());
            assertEquals("hive", catalogHeader.get());
            assertEquals("default", schemaHeader.get());
            assertEquals("Bearer secret-token", authorizationHeader.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldRejectClientModeWithoutEndpoint() {
        HttpHetuClientOperator operator =
            new HttpHetuClientOperator(new QueryExecutionHetuProperties(), new RestTemplateBuilder());

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> operator.execute("SELECT 1", new QueryExecuteRequest(), false)
        );

        assertEquals("Hetu client endpoint is not configured", ex.getMessage());
    }

    private static final class InitialStatementHandler implements HttpHandler {

        private final AtomicReference<String> requestBody;
        private final AtomicReference<String> userHeader;
        private final AtomicReference<String> sourceHeader;
        private final AtomicReference<String> catalogHeader;
        private final AtomicReference<String> schemaHeader;
        private final AtomicReference<String> authorizationHeader;
        private final HttpServer server;

        private InitialStatementHandler(AtomicReference<String> requestBody,
                                        AtomicReference<String> userHeader,
                                        AtomicReference<String> sourceHeader,
                                        AtomicReference<String> catalogHeader,
                                        AtomicReference<String> schemaHeader,
                                        AtomicReference<String> authorizationHeader,
                                        HttpServer server) {
            this.requestBody = requestBody;
            this.userHeader = userHeader;
            this.sourceHeader = sourceHeader;
            this.catalogHeader = catalogHeader;
            this.schemaHeader = schemaHeader;
            this.authorizationHeader = authorizationHeader;
            this.server = server;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            requestBody.set(readBody(exchange));
            userHeader.set(exchange.getRequestHeaders().getFirst("X-Presto-User"));
            sourceHeader.set(exchange.getRequestHeaders().getFirst("X-Presto-Source"));
            catalogHeader.set(exchange.getRequestHeaders().getFirst("X-Presto-Catalog"));
            schemaHeader.set(exchange.getRequestHeaders().getFirst("X-Presto-Schema"));
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] responseBody = (
                "{\"id\":\"query-1\",\"columns\":[{\"name\":\"id\"},{\"name\":\"state\"}],"
                    + "\"data\":[[1,\"READY\"]],"
                    + "\"stats\":{\"elapsedTimeMillis\":19,\"processedRows\":64},"
                    + "\"nextUri\":\"http://localhost:" + server.getAddress().getPort() + "/v1/statement/query-1/next\"}"
            ).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBody.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBody);
            }
        }
    }

    private static final class NextStatementHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] responseBody = (
                "{\"id\":\"query-1\",\"columns\":[{\"name\":\"id\"},{\"name\":\"state\"}],"
                    + "\"data\":[[2,\"DONE\"]],"
                    + "\"stats\":{\"elapsedTimeMillis\":27,\"processedRows\":128}}"
            ).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBody.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBody);
            }
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            byte[] body = new byte[1024];
            int size = inputStream.read(body);
            return size <= 0 ? "" : new String(body, 0, size, StandardCharsets.UTF_8);
        }
    }
}
