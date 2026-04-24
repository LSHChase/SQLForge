package com.company.benchmarkengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "benchmark-engine.artifact-storage")
public class BenchmarkArtifactStorageProperties {

    private String baseDir = "target/benchmark-engine-artifacts";
    private String storageType = "LOCAL_FILE";
    private boolean recoveryEnabled = true;
    private boolean cleanupStaleFiles = true;

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public boolean isRecoveryEnabled() {
        return recoveryEnabled;
    }

    public void setRecoveryEnabled(boolean recoveryEnabled) {
        this.recoveryEnabled = recoveryEnabled;
    }

    public boolean isCleanupStaleFiles() {
        return cleanupStaleFiles;
    }

    public void setCleanupStaleFiles(boolean cleanupStaleFiles) {
        this.cleanupStaleFiles = cleanupStaleFiles;
    }
}
