package com.company.queryexecution.application.service;

import com.company.queryexecution.domain.query.QueryExecutionStep;

interface QueryExecutionResultCacheBackend {

    BackendDescriptor descriptor();

    CacheEntryReadResult read(String cacheKey);

    CacheEntryWriteResult write(String cacheKey, QueryExecutionStep step);

    CacheEntryInvalidateResult invalidateByPrefix(String cacheKeyPrefix);

    CacheEntryCountResult countByPrefix(String cacheKeyPrefix);

    CacheBackendVerifyResult verify();

    final class BackendDescriptor {

        private final String backendType;
        private final String providerName;
        private final String carrierMode;
        private final String environmentLabel;
        private final boolean distributed;

        BackendDescriptor(String backendType,
                          String providerName,
                          String carrierMode,
                          String environmentLabel,
                          boolean distributed) {
            this.backendType = backendType;
            this.providerName = providerName;
            this.carrierMode = carrierMode;
            this.environmentLabel = environmentLabel;
            this.distributed = distributed;
        }

        String getBackendType() {
            return backendType;
        }

        String getProviderName() {
            return providerName;
        }

        String getCarrierMode() {
            return carrierMode;
        }

        String getEnvironmentLabel() {
            return environmentLabel;
        }

        boolean isDistributed() {
            return distributed;
        }
    }

    final class CacheEntryReadResult {

        private final boolean available;
        private final QueryExecutionStep step;
        private final String providerEvidence;
        private final String failureReason;

        private CacheEntryReadResult(boolean available,
                                     QueryExecutionStep step,
                                     String providerEvidence,
                                     String failureReason) {
            this.available = available;
            this.step = step;
            this.providerEvidence = providerEvidence;
            this.failureReason = failureReason;
        }

        static CacheEntryReadResult hit(QueryExecutionStep step, String providerEvidence) {
            return new CacheEntryReadResult(true, step, providerEvidence, null);
        }

        static CacheEntryReadResult miss(String providerEvidence) {
            return new CacheEntryReadResult(true, null, providerEvidence, null);
        }

        static CacheEntryReadResult unavailable(String providerEvidence, String failureReason) {
            return new CacheEntryReadResult(false, null, providerEvidence, failureReason);
        }

        boolean isAvailable() {
            return available;
        }

        QueryExecutionStep getStep() {
            return step;
        }

        String getProviderEvidence() {
            return providerEvidence;
        }

        String getFailureReason() {
            return failureReason;
        }
    }

    final class CacheEntryWriteResult {

        private final boolean written;
        private final String providerEvidence;
        private final String failureReason;

        private CacheEntryWriteResult(boolean written, String providerEvidence, String failureReason) {
            this.written = written;
            this.providerEvidence = providerEvidence;
            this.failureReason = failureReason;
        }

        static CacheEntryWriteResult written(String providerEvidence) {
            return new CacheEntryWriteResult(true, providerEvidence, null);
        }

        static CacheEntryWriteResult failed(String providerEvidence, String failureReason) {
            return new CacheEntryWriteResult(false, providerEvidence, failureReason);
        }

        boolean isWritten() {
            return written;
        }

        String getProviderEvidence() {
            return providerEvidence;
        }

        String getFailureReason() {
            return failureReason;
        }
    }

    final class CacheEntryInvalidateResult {

        private final boolean completed;
        private final int invalidatedCount;
        private final String providerEvidence;
        private final String failureReason;

        private CacheEntryInvalidateResult(boolean completed,
                                           int invalidatedCount,
                                           String providerEvidence,
                                           String failureReason) {
            this.completed = completed;
            this.invalidatedCount = invalidatedCount;
            this.providerEvidence = providerEvidence;
            this.failureReason = failureReason;
        }

        static CacheEntryInvalidateResult completed(int invalidatedCount, String providerEvidence) {
            return new CacheEntryInvalidateResult(true, invalidatedCount, providerEvidence, null);
        }

        static CacheEntryInvalidateResult failed(String providerEvidence, String failureReason) {
            return new CacheEntryInvalidateResult(false, 0, providerEvidence, failureReason);
        }

        boolean isCompleted() {
            return completed;
        }

        int getInvalidatedCount() {
            return invalidatedCount;
        }

        String getProviderEvidence() {
            return providerEvidence;
        }

        String getFailureReason() {
            return failureReason;
        }
    }

    final class CacheEntryCountResult {

        private final boolean completed;
        private final int count;
        private final String providerEvidence;
        private final String failureReason;

        private CacheEntryCountResult(boolean completed, int count, String providerEvidence, String failureReason) {
            this.completed = completed;
            this.count = count;
            this.providerEvidence = providerEvidence;
            this.failureReason = failureReason;
        }

        static CacheEntryCountResult completed(int count, String providerEvidence) {
            return new CacheEntryCountResult(true, count, providerEvidence, null);
        }

        static CacheEntryCountResult failed(String providerEvidence, String failureReason) {
            return new CacheEntryCountResult(false, 0, providerEvidence, failureReason);
        }

        boolean isCompleted() {
            return completed;
        }

        int getCount() {
            return count;
        }

        String getProviderEvidence() {
            return providerEvidence;
        }

        String getFailureReason() {
            return failureReason;
        }
    }

    final class CacheBackendVerifyResult {

        private final boolean available;
        private final String providerEvidence;
        private final String failureReason;

        private CacheBackendVerifyResult(boolean available, String providerEvidence, String failureReason) {
            this.available = available;
            this.providerEvidence = providerEvidence;
            this.failureReason = failureReason;
        }

        static CacheBackendVerifyResult available(String providerEvidence) {
            return new CacheBackendVerifyResult(true, providerEvidence, null);
        }

        static CacheBackendVerifyResult unavailable(String providerEvidence, String failureReason) {
            return new CacheBackendVerifyResult(false, providerEvidence, failureReason);
        }

        boolean isAvailable() {
            return available;
        }

        String getProviderEvidence() {
            return providerEvidence;
        }

        String getFailureReason() {
            return failureReason;
        }
    }
}
