package com.company.governance.application.controller.vo;

public class MessageStatsVO {

    private final long total;
    private final long pending;
    private final long sent;
    private final long consumed;
    private final long failed;

    public MessageStatsVO(long total, long pending, long sent, long consumed, long failed) {
        this.total = total;
        this.pending = pending;
        this.sent = sent;
        this.consumed = consumed;
        this.failed = failed;
    }

    public long getTotal() {
        return total;
    }

    public long getPending() {
        return pending;
    }

    public long getSent() {
        return sent;
    }

    public long getConsumed() {
        return consumed;
    }

    public long getFailed() {
        return failed;
    }
}
