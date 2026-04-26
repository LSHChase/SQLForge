package com.company.sqloptimization.application.controller.vo;

import com.company.sqloptimization.domain.plan.AccelerationPlanStatus;
import java.time.Instant;

public class AccelerationPlanStatusHistoryVO {

    private final AccelerationPlanStatus previousStatus;
    private final AccelerationPlanStatus currentStatus;
    private final Instant occurredAt;
    private final String note;

    public AccelerationPlanStatusHistoryVO(AccelerationPlanStatus previousStatus,
                                           AccelerationPlanStatus currentStatus,
                                           Instant occurredAt,
                                           String note) {
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
