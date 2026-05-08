package com.company.sqloptimization.domain.parsehistory;

import java.time.Instant;

public class SlowSqlExecutionHistoryQuery {

    private String tenantId;
    private Instant windowStart;
    private Instant windowEnd;
    private long slowThresholdMs;
    private int limit;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Instant getWindowStart() { return windowStart; }
    public void setWindowStart(Instant windowStart) { this.windowStart = windowStart; }
    public Instant getWindowEnd() { return windowEnd; }
    public void setWindowEnd(Instant windowEnd) { this.windowEnd = windowEnd; }
    public long getSlowThresholdMs() { return slowThresholdMs; }
    public void setSlowThresholdMs(long slowThresholdMs) { this.slowThresholdMs = slowThresholdMs; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
