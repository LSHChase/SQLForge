package com.company.benchmarkengine.application.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class BenchmarkArtifactCleanupResult {

    private final String operationStatus;
    private final String cleanupScope;
    private final String storageRecoverySource;
    private final String storageReadStatus;
    private final String cleanupTarget;
    private final Map<String, Object> operationDetails;

    public BenchmarkArtifactCleanupResult(String operationStatus,
                                          String cleanupScope,
                                          String storageRecoverySource,
                                          String storageReadStatus,
                                          String cleanupTarget,
                                          Map<String, Object> operationDetails) {
        this.operationStatus = operationStatus;
        this.cleanupScope = cleanupScope;
        this.storageRecoverySource = storageRecoverySource;
        this.storageReadStatus = storageReadStatus;
        this.cleanupTarget = cleanupTarget;
        this.operationDetails = operationDetails == null
            ? Collections.<String, Object>emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(operationDetails));
    }

    public String getOperationStatus() {
        return operationStatus;
    }

    public String getCleanupScope() {
        return cleanupScope;
    }

    public String getStorageRecoverySource() {
        return storageRecoverySource;
    }

    public String getStorageReadStatus() {
        return storageReadStatus;
    }

    public String getCleanupTarget() {
        return cleanupTarget;
    }

    public Map<String, Object> getOperationDetails() {
        return operationDetails;
    }
}
