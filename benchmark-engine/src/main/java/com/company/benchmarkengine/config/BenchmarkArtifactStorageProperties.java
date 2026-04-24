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
        private String liveEvidenceDir = "target/benchmark-engine-artifacts/object-storage-live-evidence";
        private String externalWriteDir = "";
        private String endpoint = "";
        private String credentials = "";
        private String providerName = "GENERIC_HTTP";
        private String providerContract = "HTTP_PUT_GET";
        private String recoveryProviderEndpoint = "";
        private String recoveryProviderBucket = "";
        private String recoveryProviderCredentials = "";
        private String recoveryProviderName = "";
        private String recoveryProviderContract = "";
        private String cleanupScope = "MIRROR_LIVE_EVIDENCE_EXTERNAL_WRITE_PROVIDER";
        private int providerConnectTimeoutMs = 5000;
        private int providerReadTimeoutMs = 5000;
        private String endpointEnvName = "BENCHMARK_ENGINE_ARTIFACT_STORAGE_OBJECT_ENDPOINT";
        private String bucketEnvName = "BENCHMARK_ENGINE_ARTIFACT_STORAGE_OBJECT_BUCKET";
        private String credentialsEnvName = "BENCHMARK_ENGINE_ARTIFACT_STORAGE_OBJECT_CREDENTIALS";
        private String externalWriteDirEnvName = "BENCHMARK_ENGINE_ARTIFACT_STORAGE_OBJECT_EXTERNAL_WRITE_DIR";

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

        public String getLiveEvidenceDir() {
            return liveEvidenceDir;
        }

        public void setLiveEvidenceDir(String liveEvidenceDir) {
            this.liveEvidenceDir = liveEvidenceDir;
        }

        public String getExternalWriteDir() {
            return externalWriteDir;
        }

        public void setExternalWriteDir(String externalWriteDir) {
            this.externalWriteDir = externalWriteDir;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getCredentials() {
            return credentials;
        }

        public void setCredentials(String credentials) {
            this.credentials = credentials;
        }

        public String getProviderName() {
            return providerName;
        }

        public void setProviderName(String providerName) {
            this.providerName = providerName;
        }

        public String getProviderContract() {
            return providerContract;
        }

        public void setProviderContract(String providerContract) {
            this.providerContract = providerContract;
        }

        public String getRecoveryProviderEndpoint() {
            return recoveryProviderEndpoint;
        }

        public void setRecoveryProviderEndpoint(String recoveryProviderEndpoint) {
            this.recoveryProviderEndpoint = recoveryProviderEndpoint;
        }

        public String getRecoveryProviderBucket() {
            return recoveryProviderBucket;
        }

        public void setRecoveryProviderBucket(String recoveryProviderBucket) {
            this.recoveryProviderBucket = recoveryProviderBucket;
        }

        public String getRecoveryProviderCredentials() {
            return recoveryProviderCredentials;
        }

        public void setRecoveryProviderCredentials(String recoveryProviderCredentials) {
            this.recoveryProviderCredentials = recoveryProviderCredentials;
        }

        public String getRecoveryProviderName() {
            return recoveryProviderName;
        }

        public void setRecoveryProviderName(String recoveryProviderName) {
            this.recoveryProviderName = recoveryProviderName;
        }

        public String getRecoveryProviderContract() {
            return recoveryProviderContract;
        }

        public void setRecoveryProviderContract(String recoveryProviderContract) {
            this.recoveryProviderContract = recoveryProviderContract;
        }

        public String getCleanupScope() {
            return cleanupScope;
        }

        public void setCleanupScope(String cleanupScope) {
            this.cleanupScope = cleanupScope;
        }

        public int getProviderConnectTimeoutMs() {
            return providerConnectTimeoutMs;
        }

        public void setProviderConnectTimeoutMs(int providerConnectTimeoutMs) {
            this.providerConnectTimeoutMs = providerConnectTimeoutMs;
        }

        public int getProviderReadTimeoutMs() {
            return providerReadTimeoutMs;
        }

        public void setProviderReadTimeoutMs(int providerReadTimeoutMs) {
            this.providerReadTimeoutMs = providerReadTimeoutMs;
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

        public String getExternalWriteDirEnvName() {
            return externalWriteDirEnvName;
        }

        public void setExternalWriteDirEnvName(String externalWriteDirEnvName) {
            this.externalWriteDirEnvName = externalWriteDirEnvName;
        }
    }
}
