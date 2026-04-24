package com.company.benchmarkengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "benchmark-engine.task-execution")
public class BenchmarkTaskExecutionProperties {

    private long pollIntervalMs = 25L;
    private long queueVisibilityDelayMs = 100L;
    private long phaseDelayMs = 40L;
    private int isolationSampleCount = 8;
    private int isolationWorkIterations = 96;

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

    public int getIsolationSampleCount() {
        return isolationSampleCount;
    }

    public void setIsolationSampleCount(int isolationSampleCount) {
        this.isolationSampleCount = isolationSampleCount;
    }

    public int getIsolationWorkIterations() {
        return isolationWorkIterations;
    }

    public void setIsolationWorkIterations(int isolationWorkIterations) {
        this.isolationWorkIterations = isolationWorkIterations;
    }
}
