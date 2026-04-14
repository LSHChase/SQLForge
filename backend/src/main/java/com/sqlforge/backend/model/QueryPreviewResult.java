package com.sqlforge.backend.model;

import java.util.List;
import java.util.Map;

public class QueryPreviewResult {

    private final String status;
    private final String driverStatus;
    private final String jdbcUrl;
    private final int rowCount;
    private final List<String> columns;
    private final List<Map<String, Object>> rows;
    private final long durationMs;
    private final List<String> messages;

    public QueryPreviewResult(
        String status,
        String driverStatus,
        String jdbcUrl,
        int rowCount,
        List<String> columns,
        List<Map<String, Object>> rows,
        long durationMs,
        List<String> messages
    ) {
        this.status = status;
        this.driverStatus = driverStatus;
        this.jdbcUrl = jdbcUrl;
        this.rowCount = rowCount;
        this.columns = columns;
        this.rows = rows;
        this.durationMs = durationMs;
        this.messages = messages;
    }

    public String getStatus() {
        return status;
    }

    public String getDriverStatus() {
        return driverStatus;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public int getRowCount() {
        return rowCount;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public List<String> getMessages() {
        return messages;
    }
}
