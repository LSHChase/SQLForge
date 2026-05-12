package com.company.queryexecution.config;

import com.company.sqlforge.common.jdbcagent.JdbcAgentRedisRuleKeys;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "query-execution.jdbc-agent.redis")
public class QueryExecutionJdbcAgentRedisProperties {

    private boolean enabled;
    private String namespace = JdbcAgentRedisRuleKeys.DEFAULT_NAMESPACE;
    private String host = "";
    private int port = 6379;
    private String password = "";
    private int database = 0;
    private int connectTimeoutMs = 1000;
    private int readTimeoutMs = 1000;
    private long ttlSeconds = 0L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
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

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}
