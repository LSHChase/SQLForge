package com.company.sqloptimization.domain.reportbatch;

import java.time.Instant;

public class ReportBatchStatusTransition {

    private final ReportBatch.ParseStatus previousStatus;
    private final ReportBatch.ParseStatus currentStatus;
    private final Instant occurredAt;
    private final String note;

    public ReportBatchStatusTransition(ReportBatch.ParseStatus previousStatus,
                                       ReportBatch.ParseStatus currentStatus,
                                       Instant occurredAt,
                                       String note) {
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.occurredAt = occurredAt;
        this.note = note;
    }

    public ReportBatch.ParseStatus getPreviousStatus() { return previousStatus; }
    public ReportBatch.ParseStatus getCurrentStatus() { return currentStatus; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getNote() { return note; }
}
