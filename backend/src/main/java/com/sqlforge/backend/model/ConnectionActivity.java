package com.sqlforge.backend.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class ConnectionActivity {

    private final String id;
    private final String connectionId;
    private final String connectionName;
    private final String actionType;
    private final String actionStatus;
    private final String details;
    private final Instant createdAt;

    @JsonCreator
    public ConnectionActivity(
        @JsonProperty("id") String id,
        @JsonProperty("connectionId") String connectionId,
        @JsonProperty("connectionName") String connectionName,
        @JsonProperty("actionType") String actionType,
        @JsonProperty("actionStatus") String actionStatus,
        @JsonProperty("details") String details,
        @JsonProperty("createdAt") Instant createdAt
    ) {
        this.id = id;
        this.connectionId = connectionId;
        this.connectionName = connectionName;
        this.actionType = actionType;
        this.actionStatus = actionStatus;
        this.details = details;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public String getActionType() {
        return actionType;
    }

    public String getActionStatus() {
        return actionStatus;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
