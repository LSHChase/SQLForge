package com.company.sqloptimization.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sql-optimization.query-execution")
public class OptimizationQueryExecutionProperties {

    private String baseUrl = "http://localhost:8081/api/query-execution/internal/acceleration-plans";
    private String resultDigestBaseUrl = "http://localhost:8081/api/query-execution/internal/result-digests";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getResultDigestBaseUrl() {
        return resultDigestBaseUrl;
    }

    public void setResultDigestBaseUrl(String resultDigestBaseUrl) {
        this.resultDigestBaseUrl = resultDigestBaseUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
