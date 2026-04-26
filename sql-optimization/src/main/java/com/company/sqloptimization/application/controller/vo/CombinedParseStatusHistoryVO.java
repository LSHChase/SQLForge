package com.company.sqloptimization.application.controller.vo;

public class CombinedParseStatusHistoryVO {

    private String status;
    private String note;
    private Long occurredAtEpochMs;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getOccurredAtEpochMs() {
        return occurredAtEpochMs;
    }

    public void setOccurredAtEpochMs(Long occurredAtEpochMs) {
        this.occurredAtEpochMs = occurredAtEpochMs;
    }
}
