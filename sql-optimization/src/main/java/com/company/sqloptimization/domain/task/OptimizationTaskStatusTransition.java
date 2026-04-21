package com.company.sqloptimization.domain.task;

import java.time.Instant;

/**
 * Immutable transition record used for audit-friendly task status history.
 */
public class OptimizationTaskStatusTransition {

    private final OptimizationTaskStatus previousStatus;
    private final OptimizationTaskStatus currentStatus;
    private final OptimizationTaskPhase previousPhase;
    private final OptimizationTaskPhase currentPhase;
    private final Instant occurredAt;
    private final String note;

    public OptimizationTaskStatusTransition(OptimizationTaskStatus previousStatus,
                                            OptimizationTaskStatus currentStatus,
                                            OptimizationTaskPhase previousPhase,
                                            OptimizationTaskPhase currentPhase,
                                            Instant occurredAt,
                                            String note) {
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.previousPhase = previousPhase;
        this.currentPhase = currentPhase;
        this.occurredAt = occurredAt;
        this.note = note;
    }

    public OptimizationTaskStatus getPreviousStatus() {
        return previousStatus;
    }

    public OptimizationTaskStatus getCurrentStatus() {
        return currentStatus;
    }

    public OptimizationTaskPhase getPreviousPhase() {
        return previousPhase;
    }

    public OptimizationTaskPhase getCurrentPhase() {
        return currentPhase;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getNote() {
        return note;
    }
}
