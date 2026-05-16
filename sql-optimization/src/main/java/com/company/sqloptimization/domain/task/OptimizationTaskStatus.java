package com.company.sqloptimization.domain.task;

/**
 * 异步优化任务的外部生命周期状态。
 */
public enum OptimizationTaskStatus {
    QUEUED(false),
    RUNNING(false),
    SUCCEEDED(true),
    FAILED(true),
    CANCELLED(true);

    private final boolean terminal;

    OptimizationTaskStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean isTerminal() {
        return terminal;
    }
}
