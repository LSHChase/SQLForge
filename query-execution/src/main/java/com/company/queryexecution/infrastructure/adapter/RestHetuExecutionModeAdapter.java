package com.company.queryexecution.infrastructure.adapter;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.domain.query.AccelerationPreference;
import com.company.queryexecution.domain.query.QueryExecutionAccessMode;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class RestHetuExecutionModeAdapter implements HetuExecutionModeAdapter {

    private final QueryExecutionHetuProperties properties;
    private final RestTemplateBuilder restTemplateBuilder;

    public RestHetuExecutionModeAdapter(QueryExecutionHetuProperties properties,
                                        RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    @Override
    public QueryExecutionAccessMode getMode() {
        return QueryExecutionAccessMode.REST;
    }

    @Override
    @SuppressWarnings("unchecked")
    public QueryExecutionStep execute(String actualSql, QueryExecuteRequest request, boolean degradedPath) {
        QueryExecutionHetuProperties.Rest rest = properties.getRest();
        if (!StringUtils.hasText(rest.getEndpoint())) {
            throw new IllegalStateException("Hetu REST endpoint is not configured");
        }
        RestTemplate restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(rest.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(rest.getReadTimeoutMs()))
            .build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(rest.getAuthToken())) {
            headers.setBearerAuth(rest.getAuthToken());
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("sqlText", actualSql);
        payload.put("tenantId", request == null ? "" : request.getTenantId());
        payload.put("degradedPath", Boolean.valueOf(degradedPath));
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> response = restTemplate.postForObject(
                rest.getEndpoint(),
                new HttpEntity<Map<String, Object>>(payload, headers),
                Map.class
            );
            List<Map<String, Object>> rows = sanitizeRows((List<Map<String, Object>>) (response == null ? null : response.get("rows")), rest.getMaxRows());
            return new QueryExecutionStep(
                com.company.sqlforge.common.constants.DataSourceTypeEnum.HETU,
                rows,
                responseLong(response, "elapsedMs", System.currentTimeMillis() - start),
                responseLong(response, "scannedRows", rows.size()),
                responseBoolean(response, "cacheHit"),
                shouldApplyAcceleration(request, degradedPath),
                QueryExecutionAccessMode.REST.name(),
                Collections.singletonList(QueryExecutionAccessMode.REST.name())
            );
        } catch (RestClientException ex) {
            throw new IllegalStateException("Hetu REST execution failed", ex);
        }
    }

    private List<Map<String, Object>> sanitizeRows(List<Map<String, Object>> rows, int maxRows) {
        if (rows == null) {
            return Collections.<Map<String, Object>>emptyList();
        }
        List<Map<String, Object>> sanitized = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            if (sanitized.size() >= maxRows) {
                break;
            }
            Map<String, Object> copy = new LinkedHashMap<String, Object>(row);
            copy.put("engine", "HETU");
            copy.put("mode", "PRIMARY");
            copy.put("executionMode", QueryExecutionAccessMode.REST.name());
            sanitized.add(copy);
        }
        return sanitized;
    }

    private long responseLong(Map<String, Object> response, String key, long defaultValue) {
        if (response == null || response.get(key) == null) {
            return defaultValue;
        }
        Object value = response.get(key);
        return value instanceof Number ? ((Number) value).longValue() : defaultValue;
    }

    private boolean responseBoolean(Map<String, Object> response, String key) {
        return response != null
            && response.get(key) instanceof Boolean
            && ((Boolean) response.get(key)).booleanValue();
    }

    private boolean shouldApplyAcceleration(QueryExecuteRequest request, boolean degradedPath) {
        return !degradedPath
            && request != null
            && request.getAccelerationPreference() == AccelerationPreference.PREFER_ACCELERATED;
    }
}
