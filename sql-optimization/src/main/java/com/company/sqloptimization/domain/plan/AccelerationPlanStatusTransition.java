package com.company.sqloptimization.domain.plan;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class AccelerationPlanStatusTransition {

    private final AccelerationPlanStatus previousStatus;
    private final AccelerationPlanStatus currentStatus;
    private final Instant occurredAt;
    private final String note;

    @JsonCreator
    public AccelerationPlanStatusTransition(@JsonProperty("previousStatus") AccelerationPlanStatus previousStatus,
                                            @JsonProperty("currentStatus") AccelerationPlanStatus currentStatus,
                                            @JsonProperty("occurredAt") Instant occurredAt,
                                            @JsonProperty("note") String note) {
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.occurredAt = occurredAt;
        this.note = note;
    }

    public AccelerationPlanStatus getPreviousStatus() {
        return previousStatus;
    }

    public AccelerationPlanStatus getCurrentStatus() {
        return currentStatus;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getNote() {
        return note;
    }
}
