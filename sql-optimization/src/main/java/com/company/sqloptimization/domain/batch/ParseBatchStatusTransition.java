package com.company.sqloptimization.domain.batch;

import java.time.Instant;

public class ParseBatchStatusTransition {

    private final ParseBatchStatus previousStatus;
    private final ParseBatchStatus currentStatus;
    private final Instant occurredAt;
    private final String note;

    public ParseBatchStatusTransition(ParseBatchStatus previousStatus,
                                      ParseBatchStatus currentStatus,
                                      Instant occurredAt,
                                      String note) {
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.occurredAt = occurredAt;
        this.note = note;
    }

    public ParseBatchStatus getPreviousStatus() {
        return previousStatus;
    }

    public ParseBatchStatus getCurrentStatus() {
        return currentStatus;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getNote() {
        return note;
    }
}
