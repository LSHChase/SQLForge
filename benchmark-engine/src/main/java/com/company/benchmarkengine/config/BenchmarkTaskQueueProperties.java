package com.company.benchmarkengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "benchmark-engine.queues")
public class BenchmarkTaskQueueProperties {

    private String mode = "database-worker";
    private String externalFileQueueDir = "target/benchmark-engine-queue/external-file-spool";
    private boolean cleanupConsumedFiles = true;
    private int acquireBatchSize = 32;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getExternalFileQueueDir() {
        return externalFileQueueDir;
    }

    public void setExternalFileQueueDir(String externalFileQueueDir) {
        this.externalFileQueueDir = externalFileQueueDir;
    }

    public boolean isCleanupConsumedFiles() {
        return cleanupConsumedFiles;
    }

    public void setCleanupConsumedFiles(boolean cleanupConsumedFiles) {
        this.cleanupConsumedFiles = cleanupConsumedFiles;
    }

    public int getAcquireBatchSize() {
        return acquireBatchSize;
    }

    public void setAcquireBatchSize(int acquireBatchSize) {
        this.acquireBatchSize = acquireBatchSize;
    }
}
