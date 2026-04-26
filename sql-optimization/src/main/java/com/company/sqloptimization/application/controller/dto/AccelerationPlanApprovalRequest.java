package com.company.sqloptimization.application.controller.dto;

import javax.validation.constraints.NotNull;

public class AccelerationPlanApprovalRequest {

    @NotNull(message = "approve is required")
    private Boolean approve;

    private String reviewNote;

    public Boolean getApprove() {
        return approve;
    }

    public void setApprove(Boolean approve) {
        this.approve = approve;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }
}
