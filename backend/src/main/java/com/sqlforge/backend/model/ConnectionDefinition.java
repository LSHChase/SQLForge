package com.sqlforge.backend.model;

import java.time.Instant;

public class ConnectionDefinition {

    private final String id;
    private final String name;
    private final String engineCode;
    private final String host;
    private final Integer port;
    private final String catalog;
    private final String username;
    private final boolean sslEnabled;
    private final String status;
    private final Instant createdAt;

    public ConnectionDefinition(
        String id,
        String name,
        String engineCode,
        String host,
        Integer port,
        String catalog,
        String username,
        boolean sslEnabled,
        String status,
        Instant createdAt
    ) {
        this.id = id;
        this.name = name;
        this.engineCode = engineCode;
        this.host = host;
        this.port = port;
        this.catalog = catalog;
        this.username = username;
        this.sslEnabled = sslEnabled;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEngineCode() {
        return engineCode;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getCatalog() {
        return catalog;
    }

    public String getUsername() {
        return username;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
