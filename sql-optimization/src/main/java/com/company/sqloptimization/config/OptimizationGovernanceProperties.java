package com.company.sqloptimization.config;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sql-optimization.governance")
public class OptimizationGovernanceProperties {

    private String baseUrl = "http://localhost:8080/api/governance/internal";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
    private final Map<String, String> datasourceIdMap = new LinkedHashMap<String, String>();

    public OptimizationGovernanceProperties() {
        datasourceIdMap.put(DataSourceTypeEnum.AUTO.name(), "optimization-hetu");
        datasourceIdMap.put(DataSourceTypeEnum.HETU.name(), "optimization-hetu");
        datasourceIdMap.put(DataSourceTypeEnum.HIVE.name(), "optimization-hive");
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
