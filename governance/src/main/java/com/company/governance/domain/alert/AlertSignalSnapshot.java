package com.company.governance.domain.alert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlertSignalSnapshot {

    private final String tenantId;
    private final List<MassFailureSignal> massFailureSignals;
    private final List<DatasourceAvailabilitySignal> datasourceAvailabilitySignals;
    private final List<ServiceAvailabilitySignal> serviceAvailabilitySignals;
    private final List<ReportResolveSignal> reportResolveSignals;
    private final List<RedisRuleAvailabilitySignal> redisRuleAvailabilitySignals;
    private final List<DispatchCoordinationSignal> dispatchCoordinationSignals;
    private final List<AuditWriteSignal> auditWriteSignals;
    private final List<SqlRewriteDivergenceSignal> sqlRewriteDivergenceSignals;
    private final List<BenchmarkRegressionSignal> benchmarkRegressionSignals;

    private AlertSignalSnapshot(Builder builder) {
        this.tenantId = builder.tenantId;
        this.massFailureSignals = immutableCopy(builder.massFailureSignals);
        this.datasourceAvailabilitySignals = immutableCopy(builder.datasourceAvailabilitySignals);
        this.serviceAvailabilitySignals = immutableCopy(builder.serviceAvailabilitySignals);
        this.reportResolveSignals = immutableCopy(builder.reportResolveSignals);
        this.redisRuleAvailabilitySignals = immutableCopy(builder.redisRuleAvailabilitySignals);
        this.dispatchCoordinationSignals = immutableCopy(builder.dispatchCoordinationSignals);
        this.auditWriteSignals = immutableCopy(builder.auditWriteSignals);
        this.sqlRewriteDivergenceSignals = immutableCopy(builder.sqlRewriteDivergenceSignals);
        this.benchmarkRegressionSignals = immutableCopy(builder.benchmarkRegressionSignals);
        validate();
    }

    public static Builder builder() {
        return new Builder();
    }

    private void validate() {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("tenantId 为必填项");
        }
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<T>(values));
    }

    public String getTenantId() { return tenantId; }
    public List<MassFailureSignal> getMassFailureSignals() { return massFailureSignals; }
    public List<DatasourceAvailabilitySignal> getDatasourceAvailabilitySignals() { return datasourceAvailabilitySignals; }
    public List<ServiceAvailabilitySignal> getServiceAvailabilitySignals() { return serviceAvailabilitySignals; }
    public List<ReportResolveSignal> getReportResolveSignals() { return reportResolveSignals; }
    public List<RedisRuleAvailabilitySignal> getRedisRuleAvailabilitySignals() { return redisRuleAvailabilitySignals; }
    public List<DispatchCoordinationSignal> getDispatchCoordinationSignals() { return dispatchCoordinationSignals; }
    public List<AuditWriteSignal> getAuditWriteSignals() { return auditWriteSignals; }
    public List<SqlRewriteDivergenceSignal> getSqlRewriteDivergenceSignals() { return sqlRewriteDivergenceSignals; }
    public List<BenchmarkRegressionSignal> getBenchmarkRegressionSignals() { return benchmarkRegressionSignals; }

    public static final class Builder {
        private String tenantId;
        private final List<MassFailureSignal> massFailureSignals = new ArrayList<MassFailureSignal>();
        private final List<DatasourceAvailabilitySignal> datasourceAvailabilitySignals = new ArrayList<DatasourceAvailabilitySignal>();
        private final List<ServiceAvailabilitySignal> serviceAvailabilitySignals = new ArrayList<ServiceAvailabilitySignal>();
        private final List<ReportResolveSignal> reportResolveSignals = new ArrayList<ReportResolveSignal>();
        private final List<RedisRuleAvailabilitySignal> redisRuleAvailabilitySignals = new ArrayList<RedisRuleAvailabilitySignal>();
        private final List<DispatchCoordinationSignal> dispatchCoordinationSignals = new ArrayList<DispatchCoordinationSignal>();
        private final List<AuditWriteSignal> auditWriteSignals = new ArrayList<AuditWriteSignal>();
        private final List<SqlRewriteDivergenceSignal> sqlRewriteDivergenceSignals =
            new ArrayList<SqlRewriteDivergenceSignal>();
        private final List<BenchmarkRegressionSignal> benchmarkRegressionSignals = new ArrayList<BenchmarkRegressionSignal>();

        private Builder() {
        }

        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder addMassFailureSignal(MassFailureSignal signal) { if (signal != null) { this.massFailureSignals.add(signal); } return this; }
        public Builder addDatasourceAvailabilitySignal(DatasourceAvailabilitySignal signal) { if (signal != null) { this.datasourceAvailabilitySignals.add(signal); } return this; }
        public Builder addServiceAvailabilitySignal(ServiceAvailabilitySignal signal) { if (signal != null) { this.serviceAvailabilitySignals.add(signal); } return this; }
        public Builder addReportResolveSignal(ReportResolveSignal signal) { if (signal != null) { this.reportResolveSignals.add(signal); } return this; }
        public Builder addRedisRuleAvailabilitySignal(RedisRuleAvailabilitySignal signal) { if (signal != null) { this.redisRuleAvailabilitySignals.add(signal); } return this; }
        public Builder addDispatchCoordinationSignal(DispatchCoordinationSignal signal) { if (signal != null) { this.dispatchCoordinationSignals.add(signal); } return this; }
        public Builder addAuditWriteSignal(AuditWriteSignal signal) { if (signal != null) { this.auditWriteSignals.add(signal); } return this; }
        public Builder addSqlRewriteDivergenceSignal(SqlRewriteDivergenceSignal signal) { if (signal != null) { this.sqlRewriteDivergenceSignals.add(signal); } return this; }
        public Builder addBenchmarkRegressionSignal(BenchmarkRegressionSignal signal) { if (signal != null) { this.benchmarkRegressionSignals.add(signal); } return this; }

        public AlertSignalSnapshot build() {
            return new AlertSignalSnapshot(this);
        }
    }

    public static final class MassFailureSignal {
        private final String sourceService;
        private final String windowLabel;
        private final long totalCount;
        private final long failedCount;
        private final double failureRateThreshold;

        public MassFailureSignal(String sourceService,
                                 String windowLabel,
                                 long totalCount,
                                 long failedCount,
                                 double failureRateThreshold) {
            this.sourceService = sourceService;
            this.windowLabel = windowLabel;
            this.totalCount = totalCount;
            this.failedCount = failedCount;
            this.failureRateThreshold = failureRateThreshold <= 0 ? 0.5d : failureRateThreshold;
        }

        public String getSourceService() { return sourceService; }
        public String getWindowLabel() { return windowLabel; }
        public long getTotalCount() { return totalCount; }
        public long getFailedCount() { return failedCount; }
        public double getFailureRateThreshold() { return failureRateThreshold; }
        public boolean shouldAlert() {
            return totalCount > 0 && failedCount > 0 && ((double) failedCount / (double) totalCount) >= failureRateThreshold;
        }
    }

    public static final class DatasourceAvailabilitySignal {
        private final String datasourceId;
        private final String datasourceCode;
        private final String healthStatus;
        private final String failureReason;
        private final boolean enabled;

        public DatasourceAvailabilitySignal(String datasourceId,
                                            String datasourceCode,
                                            String healthStatus,
                                            String failureReason,
                                            boolean enabled) {
            this.datasourceId = datasourceId;
            this.datasourceCode = datasourceCode;
            this.healthStatus = healthStatus;
            this.failureReason = failureReason;
            this.enabled = enabled;
        }

        public String getDatasourceId() { return datasourceId; }
        public String getDatasourceCode() { return datasourceCode; }
        public String getHealthStatus() { return healthStatus; }
        public String getFailureReason() { return failureReason; }
        public boolean isEnabled() { return enabled; }
        public boolean shouldAlert() {
            if (!enabled) {
                return false;
            }
            String normalized = healthStatus == null ? "" : healthStatus.trim().toUpperCase();
            return "DEGRADED".equals(normalized) || "FAILED".equals(normalized) || "UNAVAILABLE".equals(normalized);
        }
    }

    public static final class ServiceAvailabilitySignal {
        private final String serviceCode;
        private final String componentCode;
        private final boolean available;
        private final String unavailableReason;

        public ServiceAvailabilitySignal(String serviceCode,
                                         String componentCode,
                                         boolean available,
                                         String unavailableReason) {
            this.serviceCode = serviceCode;
            this.componentCode = componentCode;
            this.available = available;
            this.unavailableReason = unavailableReason;
        }

        public String getServiceCode() { return serviceCode; }
        public String getComponentCode() { return componentCode; }
        public boolean isAvailable() { return available; }
        public String getUnavailableReason() { return unavailableReason; }
        public AlertEvent.AlertType resolveAlertType() {
            String service = serviceCode == null ? "" : serviceCode.trim().toUpperCase();
            String component = componentCode == null ? "" : componentCode.trim().toUpperCase();
            if ("ACCESS_PARSE".equals(service) || "ACCESS_PARSE".equals(component) || "ACCESS_PARSE_SERVICE".equals(service)) {
                return AlertEvent.AlertType.ACCESS_PARSE_SERVICE_UNAVAILABLE;
            }
            return AlertEvent.AlertType.DEPENDENCY_SERVICE_UNAVAILABLE;
        }
    }

    public static final class ReportResolveSignal {
        private final String configId;
        private final String datasourceCode;
        private final String stage;
        private final String resolverStatus;
        private final String unavailableReason;
        private final boolean enabled;

        public ReportResolveSignal(String configId,
                                   String datasourceCode,
                                   String stage,
                                   String resolverStatus,
                                   String unavailableReason,
                                   boolean enabled) {
            this.configId = configId;
            this.datasourceCode = datasourceCode;
            this.stage = stage;
            this.resolverStatus = resolverStatus;
            this.unavailableReason = unavailableReason;
            this.enabled = enabled;
        }

        public String getConfigId() { return configId; }
        public String getDatasourceCode() { return datasourceCode; }
        public String getStage() { return stage; }
        public String getResolverStatus() { return resolverStatus; }
        public String getUnavailableReason() { return unavailableReason; }
        public boolean isEnabled() { return enabled; }
        public boolean shouldAlert() {
            String normalized = resolverStatus == null ? "" : resolverStatus.trim().toUpperCase();
            return "MOCK_FALLBACK".equals(normalized) || "DISABLED".equals(normalized) || (enabled && !"ACTIVE".equals(normalized));
        }
    }

    public static final class RedisRuleAvailabilitySignal {
        private final String sourceId;
        private final String sourceName;
        private final String healthStatus;
        private final String unavailableReason;
        private final boolean enabled;
        private final boolean bypassOnUnavailable;

        public RedisRuleAvailabilitySignal(String sourceId,
                                           String sourceName,
                                           String healthStatus,
                                           String unavailableReason,
                                           boolean enabled,
                                           boolean bypassOnUnavailable) {
            this.sourceId = sourceId;
            this.sourceName = sourceName;
            this.healthStatus = healthStatus;
            this.unavailableReason = unavailableReason;
            this.enabled = enabled;
            this.bypassOnUnavailable = bypassOnUnavailable;
        }

        public String getSourceId() { return sourceId; }
        public String getSourceName() { return sourceName; }
        public String getHealthStatus() { return healthStatus; }
        public String getUnavailableReason() { return unavailableReason; }
        public boolean isEnabled() { return enabled; }
        public boolean isBypassOnUnavailable() { return bypassOnUnavailable; }
        public boolean shouldAlert() {
            if (!enabled) {
                return false;
            }
            String normalized = healthStatus == null ? "" : healthStatus.trim().toUpperCase();
            return !"SIMULATED_READY".equals(normalized) && !"DISABLED".equals(normalized);
        }
    }

    public static final class DispatchCoordinationSignal {
        private final String dispatchEventId;
        private final String dispatchStatus;
        private final String targetDatasource;
        private final String resultMessage;
        private final long staleMinutes;
        private final long staleThresholdMinutes;

        public DispatchCoordinationSignal(String dispatchEventId,
                                          String dispatchStatus,
                                          String targetDatasource,
                                          String resultMessage,
                                          long staleMinutes,
                                          long staleThresholdMinutes) {
            this.dispatchEventId = dispatchEventId;
            this.dispatchStatus = dispatchStatus;
            this.targetDatasource = targetDatasource;
            this.resultMessage = resultMessage;
            this.staleMinutes = staleMinutes;
            this.staleThresholdMinutes = staleThresholdMinutes <= 0 ? 15L : staleThresholdMinutes;
        }

        public String getDispatchEventId() { return dispatchEventId; }
        public String getDispatchStatus() { return dispatchStatus; }
        public String getTargetDatasource() { return targetDatasource; }
        public String getResultMessage() { return resultMessage; }
        public long getStaleMinutes() { return staleMinutes; }
        public long getStaleThresholdMinutes() { return staleThresholdMinutes; }
        public boolean shouldAlert() {
            String normalized = dispatchStatus == null ? "" : dispatchStatus.trim().toUpperCase();
            return "FAILED".equals(normalized)
                || (("PUBLISHED".equals(normalized) || "PULLED".equals(normalized)) && staleMinutes >= staleThresholdMinutes);
        }
    }

    public static final class AuditWriteSignal {
        private final String sourceService;
        private final long failedCount;
        private final long pendingCount;

        public AuditWriteSignal(String sourceService, long failedCount, long pendingCount) {
            this.sourceService = sourceService;
            this.failedCount = failedCount;
            this.pendingCount = pendingCount;
        }

        public String getSourceService() { return sourceService; }
        public long getFailedCount() { return failedCount; }
        public long getPendingCount() { return pendingCount; }
        public boolean shouldAlert() {
            return failedCount > 0;
        }
    }

    public static final class SqlRewriteDivergenceSignal {
        private final String sourceType;
        private final String sourceKind;
        private final String sourceId;
        private final String evidenceLevel;
        private final String historyId;
        private final String parseHistoryId;
        private final String recommendationId;
        private final String rewriteRecordId;
        private final String validationRunId;
        private final String planId;
        private final String sqlFingerprint;
        private final String comparisonStatus;
        private final String differenceType;
        private final String sampleEvidenceJson;
        private final boolean autoApplyPaused;
        private final String summary;

        public SqlRewriteDivergenceSignal(String sourceType,
                                          String sourceKind,
                                          String sourceId,
                                          String evidenceLevel,
                                          String historyId,
                                          String parseHistoryId,
                                          String recommendationId,
                                          String rewriteRecordId,
                                          String validationRunId,
                                          String planId,
                                          String sqlFingerprint,
                                          String comparisonStatus,
                                          String differenceType,
                                          String sampleEvidenceJson,
                                          boolean autoApplyPaused,
                                          String summary) {
            this.sourceType = sourceType;
            this.sourceKind = sourceKind;
            this.sourceId = sourceId;
            this.evidenceLevel = evidenceLevel;
            this.historyId = historyId;
            this.parseHistoryId = parseHistoryId;
            this.recommendationId = recommendationId;
            this.rewriteRecordId = rewriteRecordId;
            this.validationRunId = validationRunId;
            this.planId = planId;
            this.sqlFingerprint = sqlFingerprint;
            this.comparisonStatus = comparisonStatus;
            this.differenceType = differenceType;
            this.sampleEvidenceJson = sampleEvidenceJson;
            this.autoApplyPaused = autoApplyPaused;
            this.summary = summary;
        }

        public String getSourceType() { return sourceType; }
        public String getSourceKind() { return sourceKind; }
        public String getSourceId() { return sourceId; }
        public String getEvidenceLevel() { return evidenceLevel; }
        public String getHistoryId() { return historyId; }
        public String getParseHistoryId() { return parseHistoryId; }
        public String getRecommendationId() { return recommendationId; }
        public String getRewriteRecordId() { return rewriteRecordId; }
        public String getValidationRunId() { return validationRunId; }
        public String getPlanId() { return planId; }
        public String getSqlFingerprint() { return sqlFingerprint; }
        public String getComparisonStatus() { return comparisonStatus; }
        public String getDifferenceType() { return differenceType; }
        public String getSampleEvidenceJson() { return sampleEvidenceJson; }
        public boolean isAutoApplyPaused() { return autoApplyPaused; }
        public String getSummary() { return summary; }
        public boolean shouldAlert() {
            return autoApplyPaused && "DIVERGED".equalsIgnoreCase(comparisonStatus);
        }
    }

    public static final class BenchmarkRegressionSignal {
        private final String reportId;
        private final String taskId;
        private final String historyId;
        private final String sqlFingerprint;
        private final String verdict;
        private final int thresholdHitCount;
        private final int failedThresholdCount;
        private final int warningThresholdCount;
        private final String summary;
        private final String reportQueryPath;
        private final String rawDataDownloadPath;
        private final String thresholdAssessmentsJson;
        private final String executionSummaryJson;

        public BenchmarkRegressionSignal(String reportId,
                                         String taskId,
                                         String historyId,
                                         String sqlFingerprint,
                                         String verdict,
                                         int thresholdHitCount,
                                         int failedThresholdCount,
                                         int warningThresholdCount,
                                         String summary,
                                         String reportQueryPath,
                                         String rawDataDownloadPath,
                                         String thresholdAssessmentsJson,
                                         String executionSummaryJson) {
            this.reportId = reportId;
            this.taskId = taskId;
            this.historyId = historyId;
            this.sqlFingerprint = sqlFingerprint;
            this.verdict = verdict;
            this.thresholdHitCount = thresholdHitCount;
            this.failedThresholdCount = failedThresholdCount;
            this.warningThresholdCount = warningThresholdCount;
            this.summary = summary;
            this.reportQueryPath = reportQueryPath;
            this.rawDataDownloadPath = rawDataDownloadPath;
            this.thresholdAssessmentsJson = thresholdAssessmentsJson;
            this.executionSummaryJson = executionSummaryJson;
        }

        public String getReportId() { return reportId; }
        public String getTaskId() { return taskId; }
        public String getHistoryId() { return historyId; }
        public String getSqlFingerprint() { return sqlFingerprint; }
        public String getVerdict() { return verdict; }
        public int getThresholdHitCount() { return thresholdHitCount; }
        public int getFailedThresholdCount() { return failedThresholdCount; }
        public int getWarningThresholdCount() { return warningThresholdCount; }
        public String getSummary() { return summary; }
        public String getReportQueryPath() { return reportQueryPath; }
        public String getRawDataDownloadPath() { return rawDataDownloadPath; }
        public String getThresholdAssessmentsJson() { return thresholdAssessmentsJson; }
        public String getExecutionSummaryJson() { return executionSummaryJson; }
        public boolean shouldAlert() {
            return failedThresholdCount > 0
                || ("FAIL".equalsIgnoreCase(verdict) && thresholdHitCount > 0);
        }
    }
}
