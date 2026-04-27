package com.company.sqloptimization.domain.dispatch;

import java.time.Instant;

public class DispatchEventTransition {

    private final DispatchEventStatus previousStatus;
    private final DispatchEventStatus currentStatus;
    private final Instant occurredAt;
    private final String note;
    private final String operator;

    public DispatchEventTransition(DispatchEventStatus previousStatus,
                                   DispatchEventStatus currentStatus,
                                   Instant occurredAt,
                                   String note,
                                   String operator) {
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.occurredAt = occurredAt;
        this.note = note;
        this.operator = operator;
    }

    public DispatchEventStatus getPreviousStatus() { return previousStatus; }
    public DispatchEventStatus getCurrentStatus() { return currentStatus; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getNote() { return note; }
    public String getOperator() { return operator; }
}
