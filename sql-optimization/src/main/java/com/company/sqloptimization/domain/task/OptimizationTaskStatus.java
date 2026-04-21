package com.company.sqloptimization.domain.task;

/**
 * External lifecycle states for async optimization tasks.
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
