package com.company.sqloptimization.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sql-optimization.rewrite-validation-scheduler")
public class RewriteValidationSchedulerProperties {

    private boolean enabled = false;
    private long fixedDelayMs = 60000L;
    private int batchSize = 20;
    private long maxAgeMinutes = 1440L;
    private String triggerReason = "SCHEDULED_VALIDATION";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs <= 0L ? 60000L : fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    public int getBatchSize() {
        return batchSize <= 0 ? 20 : batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getMaxAgeMinutes() {
        return maxAgeMinutes <= 0L ? 1440L : maxAgeMinutes;
    }

    public void setMaxAgeMinutes(long maxAgeMinutes) {
        this.maxAgeMinutes = maxAgeMinutes;
    }

    public String getTriggerReason() {
        return triggerReason;
    }

    public void setTriggerReason(String triggerReason) {
        this.triggerReason = triggerReason;
    }
}
