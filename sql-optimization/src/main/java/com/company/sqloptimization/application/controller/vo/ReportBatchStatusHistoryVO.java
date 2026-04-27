package com.company.sqloptimization.application.controller.vo;

import java.time.Instant;

public class ReportBatchStatusHistoryVO {

    private String previousStatus;
    private String currentStatus;
    private Instant occurredAt;
    private String note;

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
