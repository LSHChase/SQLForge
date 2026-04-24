package com.company.benchmarkengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "benchmark-engine.artifact-storage")
public class BenchmarkArtifactStorageProperties {

    private String baseDir = "target/benchmark-engine-artifacts";
    private String storageType = "LOCAL_FILE";
    private boolean recoveryEnabled = true;
    private boolean cleanupStaleFiles = true;
    private Integer defaultRetentionDays;
    private boolean tenantPolicyBackfillEnabled = true;
    private final EnvironmentObjectStorage environmentObjectStorage = new EnvironmentObjectStorage();

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

    public Integer getDefaultRetentionDays() {
        return defaultRetentionDays;
    }

    public void setDefaultRetentionDays(Integer defaultRetentionDays) {
        this.defaultRetentionDays = defaultRetentionDays;
    }

    public boolean isTenantPolicyBackfillEnabled() {
        return tenantPolicyBackfillEnabled;
    }

    public void setTenantPolicyBackfillEnabled(boolean tenantPolicyBackfillEnabled) {
        this.tenantPolicyBackfillEnabled = tenantPolicyBackfillEnabled;
    }

    public EnvironmentObjectStorage getEnvironmentObjectStorage() {
        return environmentObjectStorage;
    }

    public static class EnvironmentObjectStorage {

        private String bucket = "";
        private String keyPrefix = "benchmark-engine-artifacts";
        private String mirrorDir = "target/benchmark-engine-artifacts/object-storage-mirror";
        private String endpointEnvName = "BENCHMARK_ENGINE_ARTIFACT_STORAGE_OBJECT_ENDPOINT";
        private String bucketEnvName = "BENCHMARK_ENGINE_ARTIFACT_STORAGE_OBJECT_BUCKET";
        private String credentialsEnvName = "BENCHMARK_ENGINE_ARTIFACT_STORAGE_OBJECT_CREDENTIALS";

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public String getMirrorDir() {
            return mirrorDir;
        }

        public void setMirrorDir(String mirrorDir) {
            this.mirrorDir = mirrorDir;
        }

        public String getEndpointEnvName() {
            return endpointEnvName;
        }

        public void setEndpointEnvName(String endpointEnvName) {
            this.endpointEnvName = endpointEnvName;
        }

        public String getBucketEnvName() {
            return bucketEnvName;
        }

        public void setBucketEnvName(String bucketEnvName) {
            this.bucketEnvName = bucketEnvName;
        }

        public String getCredentialsEnvName() {
            return credentialsEnvName;
        }

        public void setCredentialsEnvName(String credentialsEnvName) {
            this.credentialsEnvName = credentialsEnvName;
        }
    }
}
