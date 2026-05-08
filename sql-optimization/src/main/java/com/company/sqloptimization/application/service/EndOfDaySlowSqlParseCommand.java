package com.company.sqloptimization.application.service;

import java.time.Instant;

public class EndOfDaySlowSqlParseCommand {

    private String tenantId;
    private Instant windowStart;
    private Instant windowEnd;
    private Long slowThresholdMs;
    private Integer limit;
    private String parserMode;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Instant getWindowStart() { return windowStart; }
    public void setWindowStart(Instant windowStart) { this.windowStart = windowStart; }
    public Instant getWindowEnd() { return windowEnd; }
    public void setWindowEnd(Instant windowEnd) { this.windowEnd = windowEnd; }
    public Long getSlowThresholdMs() { return slowThresholdMs; }
    public void setSlowThresholdMs(Long slowThresholdMs) { this.slowThresholdMs = slowThresholdMs; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public String getParserMode() { return parserMode; }
    public void setParserMode(String parserMode) { this.parserMode = parserMode; }
}
