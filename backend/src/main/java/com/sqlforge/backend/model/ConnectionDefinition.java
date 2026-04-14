package com.sqlforge.backend.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonCreator
    public ConnectionDefinition(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("engineCode") String engineCode,
        @JsonProperty("host") String host,
        @JsonProperty("port") Integer port,
        @JsonProperty("catalog") String catalog,
        @JsonProperty("username") String username,
        @JsonProperty("sslEnabled") boolean sslEnabled,
        @JsonProperty("status") String status,
        @JsonProperty("createdAt") Instant createdAt
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
