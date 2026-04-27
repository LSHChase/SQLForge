package com.company.sqlforge.common.jdbcagent;

import java.util.ArrayList;
import java.util.List;

public class JdbcAgentProperties {

    private JdbcAgentMode agentMode = JdbcAgentMode.OBSERVE;
    private List<String> redisEndpoints = new ArrayList<String>();
    private String redisNamespace = "sqlforge:jdbc-agent";
    private String apiBaseUrl;
    private boolean routeEnabled;
    private boolean rewriteEnabled;
    private long lightParseTimeoutMs = 20L;
    private JdbcAgentFallbackStrategy fallbackStrategy = JdbcAgentFallbackStrategy.DIRECT_JDBC;
    private boolean historyReportEnabled = true;

    public JdbcAgentMode getAgentMode() {
        return agentMode;
    }

    public void setAgentMode(JdbcAgentMode agentMode) {
        this.agentMode = agentMode;
    }

    public List<String> getRedisEndpoints() {
        return redisEndpoints;
    }

    public void setRedisEndpoints(List<String> redisEndpoints) {
        this.redisEndpoints = redisEndpoints == null ? new ArrayList<String>() : new ArrayList<String>(redisEndpoints);
    }

    public String getRedisNamespace() {
        return redisNamespace;
    }

    public void setRedisNamespace(String redisNamespace) {
        this.redisNamespace = redisNamespace;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public boolean isRouteEnabled() {
        return routeEnabled;
    }

    public void setRouteEnabled(boolean routeEnabled) {
        this.routeEnabled = routeEnabled;
    }

    public boolean isRewriteEnabled() {
        return rewriteEnabled;
    }

    public void setRewriteEnabled(boolean rewriteEnabled) {
        this.rewriteEnabled = rewriteEnabled;
    }

    public long getLightParseTimeoutMs() {
        return lightParseTimeoutMs;
    }

    public void setLightParseTimeoutMs(long lightParseTimeoutMs) {
        this.lightParseTimeoutMs = lightParseTimeoutMs;
    }

    public JdbcAgentFallbackStrategy getFallbackStrategy() {
        return fallbackStrategy;
    }

    public void setFallbackStrategy(JdbcAgentFallbackStrategy fallbackStrategy) {
        this.fallbackStrategy = fallbackStrategy;
    }

    public boolean isHistoryReportEnabled() {
        return historyReportEnabled;
    }

    public void setHistoryReportEnabled(boolean historyReportEnabled) {
        this.historyReportEnabled = historyReportEnabled;
    }
}
