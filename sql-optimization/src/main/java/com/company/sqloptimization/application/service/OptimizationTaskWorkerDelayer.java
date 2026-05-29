package com.company.sqloptimization.application.service;

import com.company.sqloptimization.config.OptimizationTaskExecutionProperties;

final class OptimizationTaskWorkerDelayer {

    private OptimizationTaskWorkerDelayer() {
    }

    static void delay(OptimizationTaskExecutionProperties executionProperties) {
        if (executionProperties.getPhaseDelayMs() <= 0L) {
            return;
        }
        try {
            Thread.sleep(executionProperties.getPhaseDelayMs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("优化 worker 被中断", ex);
        }
    }
}
