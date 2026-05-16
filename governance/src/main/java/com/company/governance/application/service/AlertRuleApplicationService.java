package com.company.governance.application.service;

import com.company.governance.domain.alert.AlertEvent;
import com.company.governance.domain.alert.AlertPolicy;
import com.company.governance.domain.alert.AlertPolicyBaseline;
import com.company.governance.domain.alert.AlertSignalSnapshot;
import com.company.sqlforge.common.utils.JsonUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AlertRuleApplicationService {

    private static final String DEFAULT_OPERATOR = "alert-rule-engine";

    public List<AlertEvent> evaluate(AlertSignalSnapshot snapshot) {
        return evaluate(snapshot, null, DEFAULT_OPERATOR, Instant.now());
    }

    public List<AlertEvent> evaluate(AlertSignalSnapshot snapshot,
                                     List<AlertPolicy> policies,
                                     String operator,
                                     Instant evaluatedAt) {
        requireSnapshot(snapshot);
        String effectiveOperator = normalize(operator, DEFAULT_OPERATOR);
        Instant effectiveEvaluatedAt = evaluatedAt == null ? Instant.now() : evaluatedAt;
        List<AlertPolicy> effectivePolicies = (policies == null || policies.isEmpty())
            ? AlertPolicyBaseline.defaultPoliciesForTenant(snapshot.getTenantId(), effectiveOperator, effectiveEvaluatedAt)
            : policies;
        Map<String, AlertEvent> alerts = new LinkedHashMap<String, AlertEvent>();
        evaluateMassFailures(snapshot, effectivePolicies, effectiveOperator, effectiveEvaluatedAt, alerts);
        evaluateDatasourceAvailability(snapshot, effectivePolicies, effectiveOperator, effectiveEvaluatedAt, alerts);
        evaluateServiceAvailability(snapshot, effectivePolicies, effectiveOperator, effectiveEvaluatedAt, alerts);
        evaluateReportResolve(snapshot, effectivePolicies, effectiveOperator, effectiveEvaluatedAt, alerts);
        evaluateRedisAvailability(snapshot, effectivePolicies, effectiveOperator, effectiveEvaluatedAt, alerts);
        evaluateDispatchCoordination(snapshot, effectivePolicies, effectiveOperator, effectiveEvaluatedAt, alerts);
        evaluateAuditWrites(snapshot, effectivePolicies, effectiveOperator, effectiveEvaluatedAt, alerts);
        evaluateSqlRewriteDivergence(snapshot, effectivePolicies, effectiveOperator, effectiveEvaluatedAt, alerts);
        evaluateBenchmarkRegression(snapshot, effectivePolicies, effectiveOperator, effectiveEvaluatedAt, alerts);
        return new ArrayList<AlertEvent>(alerts.values());
    }

    private void evaluateMassFailures(AlertSignalSnapshot snapshot,
                                      List<AlertPolicy> policies,
                                      String operator,
                                      Instant evaluatedAt,
                                      Map<String, AlertEvent> alerts) {
        for (AlertSignalSnapshot.MassFailureSignal signal : snapshot.getMassFailureSignals()) {
            if (!signal.shouldAlert()) {
                continue;
            }
            Map<String, Object> evidence = new LinkedHashMap<String, Object>();
            evidence.put("sourceService", signal.getSourceService());
            evidence.put("windowLabel", signal.getWindowLabel());
            evidence.put("totalCount", Long.valueOf(signal.getTotalCount()));
            evidence.put("failedCount", Long.valueOf(signal.getFailedCount()));
            evidence.put("failureRateThreshold", Double.valueOf(signal.getFailureRateThreshold()));
            recordAlert(alerts, buildAlert(
                snapshot.getTenantId(),
                policies,
                AlertEvent.AlertType.SQL_EXECUTION_MASS_FAILURE,
                operator,
                evaluatedAt,
                signal.getSourceService(),
                "检测到大量失败："
                    + normalize(signal.getSourceService(), "query-execution")
                    + "，窗口=" + normalize(signal.getWindowLabel(), "current-window")
                    + "：失败数=" + signal.getFailedCount()
                    + "/" + signal.getTotalCount(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                JsonUtils.toJson(evidence)
            ));
        }
    }

    private void evaluateDatasourceAvailability(AlertSignalSnapshot snapshot,
                                                List<AlertPolicy> policies,
                                                String operator,
                                                Instant evaluatedAt,
                                                Map<String, AlertEvent> alerts) {
        for (AlertSignalSnapshot.DatasourceAvailabilitySignal signal : snapshot.getDatasourceAvailabilitySignals()) {
            if (!signal.shouldAlert()) {
                continue;
            }
            Map<String, Object> evidence = new LinkedHashMap<String, Object>();
            evidence.put("datasourceId", signal.getDatasourceId());
            evidence.put("datasourceCode", signal.getDatasourceCode());
            evidence.put("healthStatus", signal.getHealthStatus());
            evidence.put("failureReason", signal.getFailureReason());
            recordAlert(alerts, buildAlert(
                snapshot.getTenantId(),
                policies,
                AlertEvent.AlertType.DATASOURCE_UNAVAILABLE,
                operator,
                evaluatedAt,
                "governance-datasource",
                "数据源不可用："
                    + normalize(signal.getDatasourceCode(), normalize(signal.getDatasourceId(), "未知-datasource"))
                    + " 状态=" + normalize(signal.getHealthStatus(), "UNKNOWN"),
                null,
                null,
                null,
                null,
                null,
                null,
                signal.getDatasourceId(),
                null,
                null,
                null,
                JsonUtils.toJson(evidence)
            ));
        }
    }

    private void evaluateServiceAvailability(AlertSignalSnapshot snapshot,
                                             List<AlertPolicy> policies,
                                             String operator,
                                             Instant evaluatedAt,
                                             Map<String, AlertEvent> alerts) {
        for (AlertSignalSnapshot.ServiceAvailabilitySignal signal : snapshot.getServiceAvailabilitySignals()) {
            if (signal.isAvailable()) {
                continue;
            }
            Map<String, Object> evidence = new LinkedHashMap<String, Object>();
            evidence.put("serviceCode", signal.getServiceCode());
            evidence.put("componentCode", signal.getComponentCode());
            evidence.put("unavailableReason", signal.getUnavailableReason());
            AlertEvent.AlertType alertType = signal.resolveAlertType();
            recordAlert(alerts, buildAlert(
                snapshot.getTenantId(),
                policies,
                alertType,
                operator,
                evaluatedAt,
                normalize(signal.getServiceCode(), "governance"),
                "服务不可用："
                    + normalize(signal.getComponentCode(), normalize(signal.getServiceCode(), "dependency"))
                    + " 原因=" + normalize(signal.getUnavailableReason(), "UNKNOWN"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                JsonUtils.toJson(evidence)
            ));
        }
    }

    private void evaluateReportResolve(AlertSignalSnapshot snapshot,
                                       List<AlertPolicy> policies,
                                       String operator,
                                       Instant evaluatedAt,
                                       Map<String, AlertEvent> alerts) {
        for (AlertSignalSnapshot.ReportResolveSignal signal : snapshot.getReportResolveSignals()) {
            if (!signal.shouldAlert()) {
                continue;
            }
            Map<String, Object> evidence = new LinkedHashMap<String, Object>();
            evidence.put("configId", signal.getConfigId());
            evidence.put("datasourceCode", signal.getDatasourceCode());
            evidence.put("stage", signal.getStage());
            evidence.put("resolverStatus", signal.getResolverStatus());
            evidence.put("unavailableReason", signal.getUnavailableReason());
            recordAlert(alerts, buildAlert(
                snapshot.getTenantId(),
                policies,
                AlertEvent.AlertType.REPORT_SQL_RESOLVE_FAILURE,
                operator,
                evaluatedAt,
                "report-interface",
                "报表 SQL 解析失败：数据源="
                    + normalize(signal.getDatasourceCode(), "未知")
                    + "，阶段=" + normalize(signal.getStage(), "UNKNOWN")
                    + "，状态=" + normalize(signal.getResolverStatus(), "UNKNOWN"),
                null,
                null,
                null,
                null,
                null,
                null,
                signal.getDatasourceCode(),
                null,
                null,
                null,
                JsonUtils.toJson(evidence)
            ));
        }
    }

    private void evaluateRedisAvailability(AlertSignalSnapshot snapshot,
                                           List<AlertPolicy> policies,
                                           String operator,
                                           Instant evaluatedAt,
                                           Map<String, AlertEvent> alerts) {
        for (AlertSignalSnapshot.RedisRuleAvailabilitySignal signal : snapshot.getRedisRuleAvailabilitySignals()) {
            if (!signal.shouldAlert()) {
                continue;
            }
            Map<String, Object> evidence = new LinkedHashMap<String, Object>();
            evidence.put("sourceId", signal.getSourceId());
            evidence.put("sourceName", signal.getSourceName());
            evidence.put("healthStatus", signal.getHealthStatus());
            evidence.put("unavailableReason", signal.getUnavailableReason());
            evidence.put("bypassOnUnavailable", Boolean.valueOf(signal.isBypassOnUnavailable()));
            recordAlert(alerts, buildAlert(
                snapshot.getTenantId(),
                policies,
                AlertEvent.AlertType.REDIS_RULE_SOURCE_UNAVAILABLE,
                operator,
                evaluatedAt,
                "redis-rule-source",
                "Redis 规则来源不可用："
                    + normalize(signal.getSourceName(), normalize(signal.getSourceId(), "rule-source"))
                    + " 状态=" + normalize(signal.getHealthStatus(), "UNKNOWN"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                JsonUtils.toJson(evidence)
            ));
        }
    }

    private void evaluateDispatchCoordination(AlertSignalSnapshot snapshot,
                                              List<AlertPolicy> policies,
                                              String operator,
                                              Instant evaluatedAt,
                                              Map<String, AlertEvent> alerts) {
        for (AlertSignalSnapshot.DispatchCoordinationSignal signal : snapshot.getDispatchCoordinationSignals()) {
            if (!signal.shouldAlert()) {
                continue;
            }
            Map<String, Object> evidence = new LinkedHashMap<String, Object>();
            evidence.put("dispatchEventId", signal.getDispatchEventId());
            evidence.put("dispatchStatus", signal.getDispatchStatus());
            evidence.put("targetDatasource", signal.getTargetDatasource());
            evidence.put("resultMessage", signal.getResultMessage());
            evidence.put("staleMinutes", Long.valueOf(signal.getStaleMinutes()));
            evidence.put("staleThresholdMinutes", Long.valueOf(signal.getStaleThresholdMinutes()));
            recordAlert(alerts, buildAlert(
                snapshot.getTenantId(),
                policies,
                AlertEvent.AlertType.DISPATCH_COORDINATION_FAILED,
                operator,
                evaluatedAt,
                "dispatch-event",
                "分发协同失败：事件="
                    + normalize(signal.getDispatchEventId(), "未知-dispatch")
                    + "，状态=" + normalize(signal.getDispatchStatus(), "UNKNOWN"),
                null,
                null,
                null,
                null,
                null,
                signal.getDispatchEventId(),
                signal.getTargetDatasource(),
                null,
                null,
                null,
                JsonUtils.toJson(evidence)
            ));
        }
    }

    private void evaluateAuditWrites(AlertSignalSnapshot snapshot,
                                     List<AlertPolicy> policies,
                                     String operator,
                                     Instant evaluatedAt,
                                     Map<String, AlertEvent> alerts) {
        for (AlertSignalSnapshot.AuditWriteSignal signal : snapshot.getAuditWriteSignals()) {
            if (!signal.shouldAlert()) {
                continue;
            }
            Map<String, Object> evidence = new LinkedHashMap<String, Object>();
            evidence.put("sourceService", signal.getSourceService());
            evidence.put("failedCount", Long.valueOf(signal.getFailedCount()));
            evidence.put("pendingCount", Long.valueOf(signal.getPendingCount()));
            recordAlert(alerts, buildAlert(
                snapshot.getTenantId(),
                policies,
                AlertEvent.AlertType.AUDIT_WRITE_EXCEPTION,
                operator,
                evaluatedAt,
                normalize(signal.getSourceService(), "audit-log"),
                "检测到审计写入异常：失败数="
                    + signal.getFailedCount()
                    + "，待处理=" + signal.getPendingCount(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                JsonUtils.toJson(evidence)
            ));
        }
    }

    private void evaluateBenchmarkRegression(AlertSignalSnapshot snapshot,
                                             List<AlertPolicy> policies,
                                             String operator,
                                             Instant evaluatedAt,
                                             Map<String, AlertEvent> alerts) {
        for (AlertSignalSnapshot.BenchmarkRegressionSignal signal : snapshot.getBenchmarkRegressionSignals()) {
            if (!signal.shouldAlert()) {
                continue;
            }
            Map<String, Object> evidence = new LinkedHashMap<String, Object>();
            evidence.put("reportId", signal.getReportId());
            evidence.put("taskId", signal.getTaskId());
            evidence.put("historyId", signal.getHistoryId());
            evidence.put("sqlFingerprint", signal.getSqlFingerprint());
            evidence.put("verdict", signal.getVerdict());
            evidence.put("thresholdHitCount", Integer.valueOf(signal.getThresholdHitCount()));
            evidence.put("failedThresholdCount", Integer.valueOf(signal.getFailedThresholdCount()));
            evidence.put("warningThresholdCount", Integer.valueOf(signal.getWarningThresholdCount()));
            evidence.put("reportQueryPath", signal.getReportQueryPath());
            evidence.put("rawDataDownloadPath", signal.getRawDataDownloadPath());
            evidence.put("thresholdAssessmentsJson", signal.getThresholdAssessmentsJson());
            evidence.put("executionSummaryJson", signal.getExecutionSummaryJson());
            recordAlert(alerts, buildAlert(
                snapshot.getTenantId(),
                policies,
                AlertEvent.AlertType.BENCHMARK_REGRESSION_FAILED,
                operator,
                evaluatedAt,
                "benchmark-engine",
                normalize(signal.getSummary(), "压测回归失败"),
                signal.getHistoryId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                signal.getReportId(),
                signal.getSqlFingerprint(),
                JsonUtils.toJson(evidence)
            ));
        }
    }

    private void evaluateSqlRewriteDivergence(AlertSignalSnapshot snapshot,
                                              List<AlertPolicy> policies,
                                              String operator,
                                              Instant evaluatedAt,
                                              Map<String, AlertEvent> alerts) {
        for (AlertSignalSnapshot.SqlRewriteDivergenceSignal signal : snapshot.getSqlRewriteDivergenceSignals()) {
            if (!signal.shouldAlert()) {
                continue;
            }
            Map<String, Object> evidence = new LinkedHashMap<String, Object>();
            evidence.put("sourceType", signal.getSourceType());
            evidence.put("sourceKind", signal.getSourceKind());
            evidence.put("sourceId", signal.getSourceId());
            evidence.put("evidenceLevel", signal.getEvidenceLevel());
            evidence.put("historyId", signal.getHistoryId());
            evidence.put("parseHistoryId", signal.getParseHistoryId());
            evidence.put("recommendationId", signal.getRecommendationId());
            evidence.put("rewriteRecordId", signal.getRewriteRecordId());
            evidence.put("validationRunId", signal.getValidationRunId());
            evidence.put("planId", signal.getPlanId());
            evidence.put("sqlFingerprint", signal.getSqlFingerprint());
            evidence.put("comparisonStatus", signal.getComparisonStatus());
            evidence.put("differenceType", signal.getDifferenceType());
            evidence.put("sampleEvidenceJson", signal.getSampleEvidenceJson());
            evidence.put("autoApplyPaused", Boolean.valueOf(signal.isAutoApplyPaused()));
            recordAlert(alerts, buildAlert(
                snapshot.getTenantId(),
                policies,
                AlertEvent.AlertType.SQL_REWRITE_RESULT_DIVERGENCE,
                operator,
                evaluatedAt,
                "sql-optimization",
                normalize(
                    signal.getSummary(),
                    "SQL 改写结果差异：rewriteRecord="
                        + normalize(signal.getRewriteRecordId(), "未知-rewrite")
                        + "，差异类型=" + normalize(signal.getDifferenceType(), "UNKNOWN")
                ),
                signal.getHistoryId(),
                null,
                null,
                null,
                signal.getRecommendationId(),
                null,
                null,
                signal.getSourceId(),
                signal.getRewriteRecordId(),
                signal.getSqlFingerprint(),
                JsonUtils.toJson(evidence)
            ));
        }
    }

    private void recordAlert(Map<String, AlertEvent> alerts, AlertEvent alertEvent) {
        alerts.putIfAbsent(alertEvent.getDedupeKey(), alertEvent);
    }

    private AlertEvent buildAlert(String tenantId,
                                  List<AlertPolicy> policies,
                                  AlertEvent.AlertType alertType,
                                  String operator,
                                  Instant evaluatedAt,
                                  String sourceService,
                                  String summary,
                                  String historyId,
                                  String parseTaskId,
                                  String batchId,
                                  String routeDecisionId,
                                  String recommendationId,
                                  String dispatchEventId,
                                  String datasourceId,
                                  String reportCode,
                                  String logicalObjectKey,
                                  String sqlFingerprint,
                                  String evidenceJson) {
        AlertPolicy policy = findPolicy(policies, tenantId, alertType);
        AlertEvent event = AlertEvent.builder()
            .alertId(buildAlertId(tenantId, alertType, summary, dispatchEventId, datasourceId, reportCode, logicalObjectKey))
            .tenantId(tenantId)
            .alertType(alertType)
            .alertLevel(policy == null ? null : policy.getDefaultLevel())
            .policyId(policy == null ? null : policy.getPolicyId())
            .sourceService(sourceService)
            .summary(summary)
            .historyId(historyId)
            .parseTaskId(parseTaskId)
            .batchId(batchId)
            .routeDecisionId(routeDecisionId)
            .recommendationId(recommendationId)
            .dispatchEventId(dispatchEventId)
            .reportCode(reportCode)
            .logicalObjectKey(logicalObjectKey)
            .datasourceId(datasourceId)
            .sqlFingerprint(sqlFingerprint)
            .evidenceJson(evidenceJson)
            .createdBy(operator)
            .createdAt(evaluatedAt)
            .notifyStatus(policy == null ? null : policy.getInitialNotifyStatus())
            .build();
        return event;
    }

    private AlertPolicy findPolicy(List<AlertPolicy> policies, String tenantId, AlertEvent.AlertType alertType) {
        if (policies == null) {
            return null;
        }
        for (AlertPolicy policy : policies) {
            if (policy != null
                && policy.isEnabled()
                && tenantId.equals(policy.getTenantId())
                && alertType == policy.getAlertType()) {
                return policy;
            }
        }
        return null;
    }

    private String buildAlertId(String tenantId,
                                AlertEvent.AlertType alertType,
                                String summary,
                                String dispatchEventId,
                                String datasourceId,
                                String reportCode,
                                String logicalObjectKey) {
        String seed = normalize(tenantId, "tenant")
            + "|"
            + alertType.name()
            + "|"
            + normalize(dispatchEventId, "")
            + "|"
            + normalize(datasourceId, "")
            + "|"
            + normalize(reportCode, "")
            + "|"
            + normalize(logicalObjectKey, "")
            + "|"
            + normalize(summary, "");
        int hash = Math.abs(seed.hashCode());
        return "alert-" + alertType.name().toLowerCase(Locale.ROOT).replace('_', '-') + "-" + Integer.toHexString(hash);
    }

    private void requireSnapshot(AlertSignalSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot 为必填项");
        }
    }

    private String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
