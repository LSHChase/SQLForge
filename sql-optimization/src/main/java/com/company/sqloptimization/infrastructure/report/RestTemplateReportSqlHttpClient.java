package com.company.sqloptimization.infrastructure.report;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigResponse;
import com.company.sqloptimization.application.service.report.ReportSqlResolveRequest;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class RestTemplateReportSqlHttpClient implements ReportSqlHttpClient {

    private final RestTemplate restTemplate;

    public RestTemplateReportSqlHttpClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public String fetchSql(GovernanceReportInterfaceConfigResponse config, ReportSqlResolveRequest request) {
        if (!"GET".equals(config.getHttpMethod())) {
            throw invalidConfig("第一版 HTTP 抽象仅实现 GET 方式获取报表 SQL");
        }
        String url = buildUrl(config, request);
        try {
            Map response = restTemplate.getForObject(url, Map.class);
            Object sql = response == null ? null : response.get(resolveSqlField(config.getSqlJsonPath()));
            if (sql == null || !StringUtils.hasText(String.valueOf(sql))) {
                throw invalidConfig("报表 SQL 响应不包含 SQL 字段 " + config.getSqlJsonPath());
            }
            return String.valueOf(sql);
        } catch (RestClientException ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.SERVICE_UNAVAILABLE,
                "报表 SQL 接口不可用",
                ex
            );
        }
    }

    private String buildUrl(GovernanceReportInterfaceConfigResponse config, ReportSqlResolveRequest request) {
        String baseUrl = trimTrailingSlash(config.getBaseUrl());
        String path = config.getPathTemplate() == null ? "" : config.getPathTemplate();
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl(baseUrl + (path.startsWith("/") ? path : "/" + path))
            .queryParam(firstNonBlank(config.getReportCodeParamName(), "report_code"), request.getReportCode());
        if (StringUtils.hasText(request.getDatasourceCode())) {
            builder.queryParam("datasource", request.getDatasourceCode());
        }
        if (StringUtils.hasText(request.getStage())) {
            builder.queryParam("stage", request.getStage());
        }
        return builder.build(true).toUriString();
    }

    private String resolveSqlField(String sqlJsonPath) {
        if (!StringUtils.hasText(sqlJsonPath) || "$.sql".equals(sqlJsonPath)) {
            return "sql";
        }
        if (sqlJsonPath.startsWith("$.")) {
            return sqlJsonPath.substring(2);
        }
        return sqlJsonPath;
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            throw invalidConfig("报表接口 baseUrl 为必填项");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String firstNonBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private BizException invalidConfig(String message) {
        return new BizException(ErrorCodeConstants.SYSTEM_CONFIG_INVALID, HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
