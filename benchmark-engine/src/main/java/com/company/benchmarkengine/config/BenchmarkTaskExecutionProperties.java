package com.company.benchmarkengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "benchmark-engine.task-execution")
public class BenchmarkTaskExecutionProperties {

    private long pollIntervalMs = 25L;
    private long queueVisibilityDelayMs = 100L;
    private long phaseDelayMs = 40L;

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public long getQueueVisibilityDelayMs() {
        return queueVisibilityDelayMs;
    }

    public void setQueueVisibilityDelayMs(long queueVisibilityDelayMs) {
        this.queueVisibilityDelayMs = queueVisibilityDelayMs;
    }

    public long getPhaseDelayMs() {
        return phaseDelayMs;
    }

    public void setPhaseDelayMs(long phaseDelayMs) {
        this.phaseDelayMs = phaseDelayMs;
    }
}
