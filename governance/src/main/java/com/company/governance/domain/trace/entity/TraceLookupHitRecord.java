package com.company.governance.domain.trace.entity;

import java.time.LocalDateTime;

public class TraceLookupHitRecord {

    private String traceId;
    private LocalDateTime lastSeenAt;
    private Long lastAuditId;

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Long getLastAuditId() {
        return lastAuditId;
    }

    public void setLastAuditId(Long lastAuditId) {
        this.lastAuditId = lastAuditId;
    }
}
