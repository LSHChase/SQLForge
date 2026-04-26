package com.company.queryexecution.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "query-execution.cache.backend")
public class QueryExecutionCacheBackendProperties {

    private String type = "IN_MEMORY";
    private String providerName = "REPO_CLOSED_IN_MEMORY";
    private String environmentLabel = "repo-default";
    private int defaultMaxEntriesPerTenant = 1024;
    private int defaultMaxEntriesPerPolicy = 128;
    private long defaultTtlSeconds = 0L;
    private Redis redis = new Redis();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getEnvironmentLabel() {
        return environmentLabel;
    }

    public void setEnvironmentLabel(String environmentLabel) {
        this.environmentLabel = environmentLabel;
    }

    public int getDefaultMaxEntriesPerTenant() {
        return defaultMaxEntriesPerTenant;
    }

    public void setDefaultMaxEntriesPerTenant(int defaultMaxEntriesPerTenant) {
        this.defaultMaxEntriesPerTenant = defaultMaxEntriesPerTenant;
    }

    public int getDefaultMaxEntriesPerPolicy() {
        return defaultMaxEntriesPerPolicy;
    }

    public void setDefaultMaxEntriesPerPolicy(int defaultMaxEntriesPerPolicy) {
        this.defaultMaxEntriesPerPolicy = defaultMaxEntriesPerPolicy;
    }

    public long getDefaultTtlSeconds() {
        return defaultTtlSeconds;
    }

    public void setDefaultTtlSeconds(long defaultTtlSeconds) {
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public static class Redis {

        private String host = "";
        private int port = 6379;
        private String password = "";
        private int database = 0;
        private int connectTimeoutMs = 1000;
        private int readTimeoutMs = 1000;

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
    }
}
