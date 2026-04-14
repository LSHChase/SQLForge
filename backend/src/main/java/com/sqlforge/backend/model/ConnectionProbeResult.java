package com.sqlforge.backend.model;

import java.util.List;

public class ConnectionProbeResult {

    private final boolean reachable;
    private final String status;
    private final String jdbcUrl;
    private final long durationMs;
    private final List<String> messages;

    public ConnectionProbeResult(
        boolean reachable,
        String status,
        String jdbcUrl,
        long durationMs,
        List<String> messages
    ) {
        this.reachable = reachable;
        this.status = status;
        this.jdbcUrl = jdbcUrl;
        this.durationMs = durationMs;
        this.messages = messages;
    }

    public boolean isReachable() {
        return reachable;
    }

    public String getStatus() {
        return status;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public List<String> getMessages() {
        return messages;
    }
}
