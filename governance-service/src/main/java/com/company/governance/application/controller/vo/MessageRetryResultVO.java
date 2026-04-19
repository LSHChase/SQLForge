package com.company.governance.application.controller.vo;

public class MessageRetryResultVO {

    private final int retriedCount;
    private final String status;

    public MessageRetryResultVO(int retriedCount, String status) {
        this.retriedCount = retriedCount;
        this.status = status;
    }

    public int getRetriedCount() {
        return retriedCount;
    }

    public String getStatus() {
        return status;
    }
}
