package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.config.BenchmarkTaskExecutionProperties;

final class BenchmarkTaskWorkerDelayer {

    private BenchmarkTaskWorkerDelayer() {
    }

    static void delay(BenchmarkTaskExecutionProperties executionProperties) {
        if (executionProperties.getPhaseDelayMs() <= 0L) {
            return;
        }
        try {
            Thread.sleep(executionProperties.getPhaseDelayMs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("压测 worker 被中断", ex);
        }
    }
}
