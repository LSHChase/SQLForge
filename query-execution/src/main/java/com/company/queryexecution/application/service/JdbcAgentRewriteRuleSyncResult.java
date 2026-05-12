package com.company.queryexecution.application.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

public class JdbcAgentRewriteRuleSyncResult {

    private final String syncStatus;
    private final String syncAction;
    private final String redisRewriteKey;
    private final String redisMetadataKey;
    private final String failureReason;
    private final boolean retryable;
    private final boolean alertRequired;

    private JdbcAgentRewriteRuleSyncResult(Builder builder) {
        this.syncStatus = builder.syncStatus;
        this.syncAction = builder.syncAction;
        this.redisRewriteKey = builder.redisRewriteKey;
        this.redisMetadataKey = builder.redisMetadataKey;
        this.failureReason = builder.failureReason;
        this.retryable = builder.retryable;
        this.alertRequired = builder.alertRequired;
    }

    public static JdbcAgentRewriteRuleSyncResult skipped(String action, String reason) {
        return builder()
            .syncStatus("SKIPPED")
            .syncAction(action)
            .failureReason(reason)
            .retryable(false)
            .alertRequired(false)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Object> toDetails() {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("syncStatus", syncStatus);
        details.put("syncAction", syncAction);
        details.put("retryable", Boolean.valueOf(retryable));
        details.put("alertRequired", Boolean.valueOf(alertRequired));
        if (StringUtils.hasText(redisRewriteKey)) {
            details.put("redisRewriteKey", redisRewriteKey);
        }
        if (StringUtils.hasText(redisMetadataKey)) {
            details.put("redisMetadataKey", redisMetadataKey);
        }
        if (StringUtils.hasText(failureReason)) {
            details.put("failureReason", failureReason);
        }
        return details;
    }

    public String getSyncStatus() { return syncStatus; }
    public String getSyncAction() { return syncAction; }
    public String getRedisRewriteKey() { return redisRewriteKey; }
    public String getRedisMetadataKey() { return redisMetadataKey; }
    public String getFailureReason() { return failureReason; }
    public boolean isRetryable() { return retryable; }
    public boolean isAlertRequired() { return alertRequired; }

    public static class Builder {
        private String syncStatus;
        private String syncAction;
        private String redisRewriteKey;
        private String redisMetadataKey;
        private String failureReason;
        private boolean retryable;
        private boolean alertRequired;

        public Builder syncStatus(String syncStatus) { this.syncStatus = syncStatus; return this; }
        public Builder syncAction(String syncAction) { this.syncAction = syncAction; return this; }
        public Builder redisRewriteKey(String redisRewriteKey) { this.redisRewriteKey = redisRewriteKey; return this; }
        public Builder redisMetadataKey(String redisMetadataKey) { this.redisMetadataKey = redisMetadataKey; return this; }
        public Builder failureReason(String failureReason) { this.failureReason = failureReason; return this; }
        public Builder retryable(boolean retryable) { this.retryable = retryable; return this; }
        public Builder alertRequired(boolean alertRequired) { this.alertRequired = alertRequired; return this; }
        public JdbcAgentRewriteRuleSyncResult build() { return new JdbcAgentRewriteRuleSyncResult(this); }
    }
}
