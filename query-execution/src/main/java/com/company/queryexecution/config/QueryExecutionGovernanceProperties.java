package com.company.queryexecution.config;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "query-execution.governance")
public class QueryExecutionGovernanceProperties {

    private String baseUrl = "http://localhost:8080/api/governance/internal";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
    private final Map<String, String> datasourceIdMap = new LinkedHashMap<String, String>();

    public QueryExecutionGovernanceProperties() {
        datasourceIdMap.put(DataSourceTypeEnum.AUTO.name(), "query-hetu");
        datasourceIdMap.put(DataSourceTypeEnum.TRINO.name(), "query-trino");
        datasourceIdMap.put(DataSourceTypeEnum.HETU.name(), "query-hetu");
        datasourceIdMap.put(DataSourceTypeEnum.HIVE.name(), "query-hive");
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
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

    public Map<String, String> getDatasourceIdMap() {
        return datasourceIdMap;
    }
}
