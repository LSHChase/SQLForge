package com.company.sqloptimization.application.controller.dto;

import com.company.sqloptimization.domain.governance.RewriteReviewStatus;
import javax.validation.constraints.NotNull;

public class SqlRewriteRecordReviewRequest {

    private String tenantId;

    @NotNull(message = "reviewStatus 为必填项")
    private RewriteReviewStatus reviewStatus;

    private String reviewNote;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public RewriteReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(RewriteReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
}
