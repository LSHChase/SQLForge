package com.company.benchmarkengine.domain.benchmark;

import java.time.Instant;

public class BenchmarkTaskStatusTransition {

    private final BenchmarkTaskStatus previousStatus;
    private final BenchmarkTaskStatus currentStatus;
    private final BenchmarkTaskPhase previousPhase;
    private final BenchmarkTaskPhase currentPhase;
    private final Instant occurredAt;
    private final String note;

    public BenchmarkTaskStatusTransition(BenchmarkTaskStatus previousStatus,
                                         BenchmarkTaskStatus currentStatus,
                                         BenchmarkTaskPhase previousPhase,
                                         BenchmarkTaskPhase currentPhase,
                                         Instant occurredAt,
                                         String note) {
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.previousPhase = previousPhase;
        this.currentPhase = currentPhase;
        this.occurredAt = occurredAt;
        this.note = note;
    }

    public BenchmarkTaskStatus getPreviousStatus() {
        return previousStatus;
    }

    public BenchmarkTaskStatus getCurrentStatus() {
        return currentStatus;
    }

    public BenchmarkTaskPhase getPreviousPhase() {
        return previousPhase;
    }

    public BenchmarkTaskPhase getCurrentPhase() {
        return currentPhase;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getNote() {
        return note;
    }
}
