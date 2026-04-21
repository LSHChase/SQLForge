package com.company.benchmarkengine.domain.benchmark;

public enum BenchmarkTaskStatus {

    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED;
    }
}
