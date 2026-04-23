package com.company.queryexecution.infrastructure.adapter;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.domain.query.AccelerationPreference;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpHetuClientOperator implements HetuClientOperator {

    private static final String HEADER_USER = "X-Presto-User";
    private static final String HEADER_SOURCE = "X-Presto-Source";
    private static final String HEADER_CATALOG = "X-Presto-Catalog";
    private static final String HEADER_SCHEMA = "X-Presto-Schema";

    private final QueryExecutionHetuProperties properties;
    private final RestTemplateBuilder restTemplateBuilder;

    public HttpHetuClientOperator(QueryExecutionHetuProperties properties,
                                  RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public QueryExecutionStep execute(String actualSql, QueryExecuteRequest request, boolean degradedPath) {
        QueryExecutionHetuProperties.Client client = properties.getClient();
        if (!StringUtils.hasText(client.getEndpoint())) {
            throw new IllegalStateException("Hetu client endpoint is not configured");
        }
        if (!StringUtils.hasText(client.getUser())) {
            throw new IllegalStateException("Hetu client user is not configured");
        }
        RestTemplate restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(client.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(client.getReadTimeoutMs()))
            .build();
        HttpHeaders headers = buildHeaders(client);
        long start = System.currentTimeMillis();
        try {
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                client.getEndpoint(),
                HttpMethod.POST,
                new HttpEntity<String>(actualSql, headers),
                Map.class
            );
            Map<String, Object> response = responseEntity.getBody();
            List<String> columnNames = resolveColumnNames(response);
            List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
            long scannedRows = 0L;
            long elapsedMs = 0L;
            int pageCount = 0;
            while (response != null && pageCount < client.getMaxPages() && rows.size() < client.getMaxRows()) {
                pageCount++;
                if (columnNames.isEmpty()) {
                    columnNames = resolveColumnNames(response);
                }
                appendRows(rows, columnNames, responseDataRows(response), client.getMaxRows());
                elapsedMs = resolveElapsedMs(response, start, elapsedMs);
                scannedRows = resolveScannedRows(response, rows.size(), scannedRows);
                String nextUri = responseString(response, "nextUri");
                if (!StringUtils.hasText(nextUri) || rows.size() >= client.getMaxRows()) {
                    break;
                }
                response = restTemplate.exchange(
                    nextUri,
                    HttpMethod.GET,
                    new HttpEntity<Void>(headers),
                    Map.class
                ).getBody();
            }
            return new QueryExecutionStep(
                DataSourceTypeEnum.HETU,
                rows,
                elapsedMs > 0L ? elapsedMs : System.currentTimeMillis() - start,
                scannedRows > 0L ? scannedRows : rows.size(),
                resolveCacheHit(response),
                shouldApplyAcceleration(request, degradedPath)
            );
        } catch (RestClientException ex) {
            throw new IllegalStateException("Hetu client execution failed", ex);
        }
    }

    private HttpHeaders buildHeaders(QueryExecutionHetuProperties.Client client) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HEADER_USER, client.getUser());
        headers.set(HEADER_SOURCE, StringUtils.hasText(client.getSource()) ? client.getSource() : "sqlforge-query-execution");
        if (StringUtils.hasText(client.getCatalog())) {
            headers.set(HEADER_CATALOG, client.getCatalog());
        }
        if (StringUtils.hasText(client.getSchema())) {
            headers.set(HEADER_SCHEMA, client.getSchema());
        }
        if (StringUtils.hasText(client.getAuthToken())) {
            headers.setBearerAuth(client.getAuthToken());
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveColumnNames(Map<String, Object> response) {
        if (response == null || !(response.get("columns") instanceof List)) {
            return Collections.<String>emptyList();
        }
        List<String> columnNames = new ArrayList<String>();
        for (Object column : (List<Object>) response.get("columns")) {
            if (column instanceof Map && ((Map<?, ?>) column).get("name") != null) {
                columnNames.add(String.valueOf(((Map<?, ?>) column).get("name")));
            }
        }
        return columnNames;
    }

    @SuppressWarnings("unchecked")
    private List<List<Object>> responseDataRows(Map<String, Object> response) {
        if (response == null || !(response.get("data") instanceof List)) {
            return Collections.<List<Object>>emptyList();
        }
        List<List<Object>> rows = new ArrayList<List<Object>>();
        for (Object row : (List<Object>) response.get("data")) {
            if (row instanceof List) {
                rows.add((List<Object>) row);
            }
        }
        return rows;
    }

    private void appendRows(List<Map<String, Object>> rows,
                            List<String> columnNames,
                            List<List<Object>> responseRows,
                            int maxRows) {
        for (List<Object> rawRow : responseRows) {
            if (rows.size() >= maxRows) {
                return;
            }
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            for (int index = 0; index < rawRow.size(); index++) {
                String columnName = index < columnNames.size() ? columnNames.get(index) : "col_" + (index + 1);
                row.put(columnName, rawRow.get(index));
            }
            rows.add(row);
        }
    }

    @SuppressWarnings("unchecked")
    private long resolveElapsedMs(Map<String, Object> response, long start, long currentValue) {
        if (response == null) {
            return currentValue > 0L ? currentValue : System.currentTimeMillis() - start;
        }
        if (response.get("stats") instanceof Map) {
            Object statsValue = ((Map<String, Object>) response.get("stats")).get("elapsedTimeMillis");
            if (statsValue instanceof Number) {
                return ((Number) statsValue).longValue();
            }
        }
        if (response.get("elapsedMs") instanceof Number) {
            return ((Number) response.get("elapsedMs")).longValue();
        }
        return currentValue > 0L ? currentValue : System.currentTimeMillis() - start;
    }

    @SuppressWarnings("unchecked")
    private long resolveScannedRows(Map<String, Object> response, int rowCount, long currentValue) {
        if (response == null) {
            return currentValue > 0L ? currentValue : rowCount;
        }
        if (response.get("stats") instanceof Map) {
            Object statsValue = ((Map<String, Object>) response.get("stats")).get("processedRows");
            if (statsValue instanceof Number) {
                return ((Number) statsValue).longValue();
            }
        }
        if (response.get("scannedRows") instanceof Number) {
            return ((Number) response.get("scannedRows")).longValue();
        }
        return currentValue > 0L ? currentValue : rowCount;
    }

    @SuppressWarnings("unchecked")
    private boolean resolveCacheHit(Map<String, Object> response) {
        if (response == null) {
            return false;
        }
        if (response.get("cacheHit") instanceof Boolean) {
            return ((Boolean) response.get("cacheHit")).booleanValue();
        }
        if (response.get("stats") instanceof Map) {
            Object statsValue = ((Map<String, Object>) response.get("stats")).get("cacheHit");
            if (statsValue instanceof Boolean) {
                return ((Boolean) statsValue).booleanValue();
            }
        }
        return false;
    }

    private String responseString(Map<String, Object> response, String key) {
        return response != null && response.get(key) != null ? String.valueOf(response.get(key)) : "";
    }

    private boolean shouldApplyAcceleration(QueryExecuteRequest request, boolean degradedPath) {
        return !degradedPath
            && request != null
            && request.getAccelerationPreference() == AccelerationPreference.PREFER_ACCELERATED;
    }
}
