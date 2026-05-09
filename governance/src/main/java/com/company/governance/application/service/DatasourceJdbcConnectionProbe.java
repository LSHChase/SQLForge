package com.company.governance.application.service;

import com.company.governance.domain.datasource.DatasourceConfig;

public interface DatasourceJdbcConnectionProbe {

    JdbcProbeResult probe(DatasourceConfig config, String password, int timeoutMs);

    final class JdbcProbeResult {

        private final boolean connected;
        private final String failureReason;
        private final long elapsedMs;

        public JdbcProbeResult(boolean connected, String failureReason, long elapsedMs) {
            this.connected = connected;
            this.failureReason = failureReason;
            this.elapsedMs = elapsedMs;
        }

        public boolean isConnected() {
            return connected;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public long getElapsedMs() {
            return elapsedMs;
        }
    }
}
