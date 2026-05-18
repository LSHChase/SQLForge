package com.company.sqloptimization.domain.trial;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RewriteTrialItem {

    private final String trialItemId;
    private final String runId;
    private final String batchItemId;
    private final String parseTaskId;
    private final String parseHistoryId;
    private final String historyId;
    private final String sqlFingerprint;
    private final String datasourceCode;
    private final String sourceSqlText;
    private final List<Map<String, Object>> sourceProblems;
    private final String taskId;
    private final String recommendationId;
    private final String rewriteRecordId;
    private final RewriteTrialStatus trialStatus;
    private final String failureReason;
    private final String candidateSql;
    private final String validationStatus;
    private final List<Map<String, Object>> issueRuleLinks;
    private final Instant createdAt;
    private final Instant updatedAt;

    private RewriteTrialItem(Builder builder) {
        this.trialItemId = builder.trialItemId;
        this.runId = builder.runId;
        this.batchItemId = builder.batchItemId;
        this.parseTaskId = builder.parseTaskId;
        this.parseHistoryId = builder.parseHistoryId;
        this.historyId = builder.historyId;
        this.sqlFingerprint = builder.sqlFingerprint;
        this.datasourceCode = builder.datasourceCode;
        this.sourceSqlText = builder.sourceSqlText;
        this.sourceProblems = immutableListCopy(builder.sourceProblems);
        this.taskId = builder.taskId;
        this.recommendationId = builder.recommendationId;
        this.rewriteRecordId = builder.rewriteRecordId;
        this.trialStatus = builder.trialStatus == null ? RewriteTrialStatus.NOT_REQUESTED : builder.trialStatus;
        this.failureReason = builder.failureReason;
        this.candidateSql = builder.candidateSql;
        this.validationStatus = builder.validationStatus;
        this.issueRuleLinks = immutableListCopy(builder.issueRuleLinks);
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt == null ? builder.createdAt : builder.updatedAt;
        validate();
    }

    public static Builder builder() {
        return new Builder();
    }

    private void validate() {
        requireText(trialItemId, "trialItemId");
        requireText(runId, "runId");
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt 为必填项");
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " 为必填项");
        }
    }

    private List<Map<String, Object>> immutableListCopy(List<Map<String, Object>> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(value.size());
        for (Map<String, Object> item : value) {
            if (item == null || item.isEmpty()) {
                continue;
            }
            result.add(Collections.unmodifiableMap(new LinkedHashMap<String, Object>(item)));
        }
        return Collections.unmodifiableList(result);
    }

    public String getTrialItemId() { return trialItemId; }
    public String getRunId() { return runId; }
    public String getBatchItemId() { return batchItemId; }
    public String getParseTaskId() { return parseTaskId; }
    public String getParseHistoryId() { return parseHistoryId; }
    public String getHistoryId() { return historyId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public String getDatasourceCode() { return datasourceCode; }
    public String getSourceSqlText() { return sourceSqlText; }
    public List<Map<String, Object>> getSourceProblems() { return sourceProblems; }
    public String getTaskId() { return taskId; }
    public String getRecommendationId() { return recommendationId; }
    public String getRewriteRecordId() { return rewriteRecordId; }
    public RewriteTrialStatus getTrialStatus() { return trialStatus; }
    public String getFailureReason() { return failureReason; }
    public String getCandidateSql() { return candidateSql; }
    public String getValidationStatus() { return validationStatus; }
    public List<Map<String, Object>> getIssueRuleLinks() { return issueRuleLinks; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public static final class Builder {
        private String trialItemId;
        private String runId;
        private String batchItemId;
        private String parseTaskId;
        private String parseHistoryId;
        private String historyId;
        private String sqlFingerprint;
        private String datasourceCode;
        private String sourceSqlText;
        private List<Map<String, Object>> sourceProblems;
        private String taskId;
        private String recommendationId;
        private String rewriteRecordId;
        private RewriteTrialStatus trialStatus;
        private String failureReason;
        private String candidateSql;
        private String validationStatus;
        private List<Map<String, Object>> issueRuleLinks;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder() {
        }

        public Builder trialItemId(String trialItemId) { this.trialItemId = trialItemId; return this; }
        public Builder runId(String runId) { this.runId = runId; return this; }
        public Builder batchItemId(String batchItemId) { this.batchItemId = batchItemId; return this; }
        public Builder parseTaskId(String parseTaskId) { this.parseTaskId = parseTaskId; return this; }
        public Builder parseHistoryId(String parseHistoryId) { this.parseHistoryId = parseHistoryId; return this; }
        public Builder historyId(String historyId) { this.historyId = historyId; return this; }
        public Builder sqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; return this; }
        public Builder datasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; return this; }
        public Builder sourceSqlText(String sourceSqlText) { this.sourceSqlText = sourceSqlText; return this; }
        public Builder sourceProblems(List<Map<String, Object>> sourceProblems) { this.sourceProblems = sourceProblems; return this; }
        public Builder taskId(String taskId) { this.taskId = taskId; return this; }
        public Builder recommendationId(String recommendationId) { this.recommendationId = recommendationId; return this; }
        public Builder rewriteRecordId(String rewriteRecordId) { this.rewriteRecordId = rewriteRecordId; return this; }
        public Builder trialStatus(RewriteTrialStatus trialStatus) { this.trialStatus = trialStatus; return this; }
        public Builder failureReason(String failureReason) { this.failureReason = failureReason; return this; }
        public Builder candidateSql(String candidateSql) { this.candidateSql = candidateSql; return this; }
        public Builder validationStatus(String validationStatus) { this.validationStatus = validationStatus; return this; }
        public Builder issueRuleLinks(List<Map<String, Object>> issueRuleLinks) { this.issueRuleLinks = issueRuleLinks; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public RewriteTrialItem build() {
            return new RewriteTrialItem(this);
        }
    }
}
