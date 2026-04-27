package com.company.sqloptimization.application.controller.vo;

import java.time.Instant;

public class DispatchEventStatusHistoryVO {

    private String previousStatus;
    private String currentStatus;
    private Instant occurredAt;
    private String note;
    private String operator;

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
