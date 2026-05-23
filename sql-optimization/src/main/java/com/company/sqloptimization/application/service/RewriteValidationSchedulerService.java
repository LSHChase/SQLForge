package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.governance.GovernanceSqlRewriteDivergenceAlertLinkage;
import com.company.sqlforge.common.governance.GovernanceSqlRewriteDivergenceAlertRequest;
import com.company.sqlforge.common.governance.GovernanceSqlRewriteDivergenceAlertResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.application.controller.dto.RewriteValidationRunCreateRequest;
import com.company.sqloptimization.application.controller.vo.RewriteValidationRunVO;
import com.company.sqloptimization.config.RewriteValidationSchedulerProperties;
import com.company.sqloptimization.domain.rewrite.SqlRewriteRecord;
import com.company.sqloptimization.domain.rewrite.repository.SqlRewriteRecordRepository;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqloptimization.infrastructure.governance.OptimizationAuditRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RewriteValidationSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RewriteValidationSchedulerService.class);
    private static final String DIVERGED = "DIVERGED";
    private static final String OPERATION_DIVERGENCE_AUTO_PAUSE = "SQL_REWRITE_DIVERGENCE_AUTO_PAUSE";
    private static final String RESOURCE_TYPE_REWRITE_RECORD = "SQL_REWRITE_RECORD";

    private final RewriteValidationSchedulerProperties properties;
    private final SqlRewriteRecordRepository sqlRewriteRecordRepository;
    private final SqlRewriteRecordApplicationService sqlRewriteRecordApplicationService;
    private final GovernanceCapabilityClient governanceCapabilityClient;

    public RewriteValidationSchedulerService(RewriteValidationSchedulerProperties properties,
                                             SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                             SqlRewriteRecordApplicationService sqlRewriteRecordApplicationService,
                                             GovernanceCapabilityClient governanceCapabilityClient) {
        this.properties = properties;
        this.sqlRewriteRecordRepository = sqlRewriteRecordRepository;
        this.sqlRewriteRecordApplicationService = sqlRewriteRecordApplicationService;
        this.governanceCapabilityClient = governanceCapabilityClient;
    }

    public RewriteValidationSchedulerResult runScheduledValidationCycle() {
        if (!properties.isEnabled()) {
            return RewriteValidationSchedulerResult.disabled();
        }
        Instant dueBefore = Instant.now().minusSeconds(properties.getMaxAgeMinutes() * 60L);
        List<SqlRewriteRecord> candidates = sqlRewriteRecordRepository.findScheduledValidationCandidates(
            properties.getBatchSize(),
            dueBefore
        );
        RewriteValidationSchedulerResult result = RewriteValidationSchedulerResult.enabled(candidates.size());
        for (SqlRewriteRecord candidate : candidates) {
            result.incrementProcessed();
            try {
                RewriteValidationRunVO run = executeValidation(candidate);
                if (DIVERGED.equals(run.getComparisonStatus()) && Boolean.TRUE.equals(run.getAutoApplyPaused())) {
                    emitDivergenceAlert(candidate, run, result);
                    writeDivergencePauseAudit(candidate, run, result);
                }
            } catch (RuntimeException ex) {
                result.incrementValidationFailures();
                LOGGER.warn(
                    "调度改写校验失败，rewriteRecordId={}",
                    candidate.getRewriteRecordId(),
                    ex
                );
            }
        }
        return result;
    }

    private RewriteValidationRunVO executeValidation(SqlRewriteRecord candidate) {
        RequestContext.ContextValue previous = RequestContext.snapshot();
        try {
            Instant now = Instant.now();
            RequestContext.set(
                candidate.getTenantId(),
                "rewrite-validation-scheduler",
                "rewrite-validation-" + candidate.getRewriteRecordId() + "-" + now.toEpochMilli(),
                "rewrite-validation-" + candidate.getRewriteRecordId(),
                "scheduler",
                now.getEpochSecond(),
                now.plusSeconds(300L).getEpochSecond()
            );
            RewriteValidationRunCreateRequest request = new RewriteValidationRunCreateRequest();
            request.setTenantId(candidate.getTenantId());
            request.setRecommendationId(candidate.getRecommendationId());
            request.setHistoryId(candidate.getHistoryId());
            request.setSqlFingerprint(candidate.getSqlFingerprint());
            request.setTriggerReason(resolveTriggerReason());
            return sqlRewriteRecordApplicationService.createValidationRun(candidate.getRewriteRecordId(), request);
        } finally {
            RequestContext.restore(previous);
        }
    }

    private void writeDivergencePauseAudit(SqlRewriteRecord candidate,
                                           RewriteValidationRunVO run,
                                           RewriteValidationSchedulerResult result) {
        RequestContext.ContextValue previous = RequestContext.snapshot();
        Instant now = Instant.now();
        long start = System.currentTimeMillis();
        String resultStatus = "UNKNOWN";
        try {
            RequestContext.set(
                candidate.getTenantId(),
                "rewrite-validation-scheduler",
                "rewrite-divergence-audit-" + candidate.getRewriteRecordId() + "-" + now.toEpochMilli(),
                "rewrite-divergence-audit-" + candidate.getRewriteRecordId(),
                "scheduler",
                now.getEpochSecond(),
                now.plusSeconds(300L).getEpochSecond()
            );
            SqlRewriteRecord latest = latestOrCandidate(candidate);
            resultStatus = resolvePauseAuditResultStatus(latest);
            governanceCapabilityClient.writeAudit(
                new OptimizationAuditRecord(
                    OPERATION_DIVERGENCE_AUTO_PAUSE,
                    RESOURCE_TYPE_REWRITE_RECORD,
                    latest.getRewriteRecordId(),
                    resultStatus,
                    System.currentTimeMillis() - start,
                    JsonUtils.toJson(buildAuditRequestPayload(candidate, run)),
                    JsonUtils.toJson(buildAuditResponsePayload(latest, run)),
                    "rewrite-validation-" + latest.getRewriteRecordId(),
                    null,
                    null,
                    latest.getHistoryId()
                )
            );
            result.incrementAuditWrites();
            saveAuditTrace(candidate.getRewriteRecordId(), run, resultStatus, null);
        } catch (RuntimeException ex) {
            result.incrementAuditFailures();
            saveAuditTrace(candidate.getRewriteRecordId(), run, resultStatus, ex);
            LOGGER.warn(
                "SQL 改写差异审计写入失败，rewriteRecordId={}, validationRunId={}",
                candidate.getRewriteRecordId(),
                run.getValidationRunId(),
                ex
            );
        } finally {
            RequestContext.restore(previous);
        }
    }

    private void emitDivergenceAlert(SqlRewriteRecord candidate,
                                     RewriteValidationRunVO run,
                                     RewriteValidationSchedulerResult result) {
        RequestContext.ContextValue previous = RequestContext.snapshot();
        try {
            Instant now = Instant.now();
            RequestContext.set(
                candidate.getTenantId(),
                "rewrite-validation-scheduler",
                "rewrite-divergence-alert-" + candidate.getRewriteRecordId() + "-" + now.toEpochMilli(),
                "rewrite-divergence-alert-" + candidate.getRewriteRecordId(),
                "scheduler",
                now.getEpochSecond(),
                now.plusSeconds(300L).getEpochSecond()
            );
            GovernanceSqlRewriteDivergenceAlertResponse response =
                governanceCapabilityClient.emitSqlRewriteDivergenceAlert(buildAlertRequest(candidate, run));
            result.incrementAlertEmissions();
            saveAlertTrace(candidate.getRewriteRecordId(), run, response, null);
        } catch (RuntimeException ex) {
            result.incrementAlertFailures();
            saveAlertTrace(candidate.getRewriteRecordId(), run, null, ex);
            LOGGER.warn(
                "SQL 改写差异告警发送失败，rewriteRecordId={}, validationRunId={}",
                candidate.getRewriteRecordId(),
                run.getValidationRunId(),
                ex
            );
        } finally {
            RequestContext.restore(previous);
        }
    }

    private GovernanceSqlRewriteDivergenceAlertRequest buildAlertRequest(SqlRewriteRecord candidate,
                                                                         RewriteValidationRunVO run) {
        GovernanceSqlRewriteDivergenceAlertRequest request = new GovernanceSqlRewriteDivergenceAlertRequest();
        request.setTenantId(candidate.getTenantId());
        request.setSourceType(candidate.getSourceType().name());
        request.setSourceKind(candidate.getSourceKind().name());
        request.setSourceId(candidate.getSourceId());
        request.setEvidenceLevel(candidate.getEvidenceLevel().name());
        request.setHistoryId(candidate.getHistoryId());
        request.setParseHistoryId(candidate.getParseHistoryId());
        request.setRecommendationId(candidate.getRecommendationId());
        request.setRewriteRecordId(candidate.getRewriteRecordId());
        request.setValidationRunId(run.getValidationRunId());
        request.setSqlFingerprint(run.getSqlFingerprint());
        request.setComparisonStatus(run.getComparisonStatus());
        request.setDifferenceType(run.getDifferenceType());
        request.setSampleEvidenceJson(JsonUtils.toJson(run.getDifferenceSample()));
        request.setAutoApplyPaused(run.getAutoApplyPaused());
        request.setSummary(
            "SQL 改写结果差异：rewriteRecord="
                + candidate.getRewriteRecordId()
                + "，差异类型=" + run.getDifferenceType()
        );
        return request;
    }

    private void saveAlertTrace(String rewriteRecordId,
                                RewriteValidationRunVO run,
                                GovernanceSqlRewriteDivergenceAlertResponse response,
                                RuntimeException failure) {
        SqlRewriteRecord latest = sqlRewriteRecordRepository.findRecordById(rewriteRecordId);
        if (latest == null) {
            return;
        }
        Map<String, Object> traceRefs = new LinkedHashMap<String, Object>(latest.getTraceRefs());
        Map<String, Object> divergenceAlert = new LinkedHashMap<String, Object>();
        divergenceAlert.put("alertType", "SQL_REWRITE_RESULT_DIVERGENCE");
        divergenceAlert.put("validationRunId", run.getValidationRunId());
        divergenceAlert.put("comparisonStatus", run.getComparisonStatus());
        divergenceAlert.put("differenceType", run.getDifferenceType());
        divergenceAlert.put("autoApplyPaused", run.getAutoApplyPaused());
        if (failure == null) {
            divergenceAlert.put("emissionStatus", "EMITTED_OR_DEDUPED");
            divergenceAlert.put("alertTriggered", response == null ? Boolean.FALSE : response.getAlertTriggered());
            divergenceAlert.put("alertLinkages", response == null
                ? new ArrayList<Map<String, Object>>()
                : toLinkageMaps(response.getAlertLinkages()));
        } else {
            divergenceAlert.put("emissionStatus", "FAILED");
            divergenceAlert.put("errorType", failure.getClass().getSimpleName());
            divergenceAlert.put("errorMessage", failure.getMessage());
        }
        traceRefs.put("divergenceAlert", divergenceAlert);
        sqlRewriteRecordRepository.saveRecord(latest.withTraceRefs(traceRefs, Instant.now()));
    }

    private void saveAuditTrace(String rewriteRecordId,
                                RewriteValidationRunVO run,
                                String resultStatus,
                                RuntimeException failure) {
        SqlRewriteRecord latest = sqlRewriteRecordRepository.findRecordById(rewriteRecordId);
        if (latest == null) {
            return;
        }
        Map<String, Object> traceRefs = new LinkedHashMap<String, Object>(latest.getTraceRefs());
        Map<String, Object> auditTrace = new LinkedHashMap<String, Object>();
        auditTrace.put("operationCode", OPERATION_DIVERGENCE_AUTO_PAUSE);
        auditTrace.put("validationRunId", run.getValidationRunId());
        auditTrace.put("comparisonStatus", run.getComparisonStatus());
        auditTrace.put("differenceType", run.getDifferenceType());
        auditTrace.put("resultStatus", resultStatus);
        auditTrace.put("auditWriteStatus", failure == null ? "WRITTEN" : "FAILED");
        if (failure != null) {
            auditTrace.put("errorType", failure.getClass().getSimpleName());
            auditTrace.put("errorMessage", failure.getMessage());
        }
        traceRefs.put("divergencePauseAudit", auditTrace);
        sqlRewriteRecordRepository.saveRecord(latest.withTraceRefs(traceRefs, Instant.now()));
    }

    private SqlRewriteRecord latestOrCandidate(SqlRewriteRecord candidate) {
        SqlRewriteRecord latest = sqlRewriteRecordRepository.findRecordById(candidate.getRewriteRecordId());
        return latest == null ? candidate : latest;
    }

    private String resolvePauseAuditResultStatus(SqlRewriteRecord latest) {
        if (latest != null && "PAUSED".equals(latest.getActivationStatus().name())) {
            return "SUCCESS";
        }
        Map<String, Object> activationStatusTrace = latest == null
            ? null
            : asMap(latest.getTraceRefs().get("lastActivationStatusTrace"));
        if (activationStatusTrace != null
            && (activationStatusTrace.get("errorType") != null || activationStatusTrace.get("failureType") != null)) {
            return "FAILED";
        }
        return "UNKNOWN";
    }

    private Map<String, Object> buildAuditRequestPayload(SqlRewriteRecord candidate,
                                                         RewriteValidationRunVO run) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("tenantId", candidate.getTenantId());
        payload.put("rewriteRecordId", candidate.getRewriteRecordId());
        payload.put("validationRunId", run.getValidationRunId());
        payload.put("sqlFingerprint", run.getSqlFingerprint());
        payload.put("comparisonStatus", run.getComparisonStatus());
        payload.put("differenceType", run.getDifferenceType());
        payload.put("activationStatusBefore", candidate.getActivationStatus().name());
        payload.put("runtimeBindingIdBefore", candidate.getRuntimeBindingId());
        payload.put("requestId", RequestContext.getRequestId());
        payload.put("traceId", RequestContext.getTraceId());
        return payload;
    }

    private Map<String, Object> buildAuditResponsePayload(SqlRewriteRecord latest,
                                                          RewriteValidationRunVO run) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("rewriteRecordId", latest.getRewriteRecordId());
        payload.put("validationRunId", run.getValidationRunId());
        payload.put("recordStatus", latest.getStatus().name());
        payload.put("validationStatus", latest.getValidationStatus().name());
        payload.put("activationStatusAfter", latest.getActivationStatus().name());
        payload.put("alertStatus", latest.getAlertStatus().name());
        payload.put("autoApplyAllowed", Boolean.valueOf(latest.isAutoApplyAllowed()));
        payload.put("runtimeBindingIdAfter", latest.getRuntimeBindingId());
        payload.put("lastActivationStatusTrace", latest.getTraceRefs().get("lastActivationStatusTrace"));
        payload.put("divergenceAlert", latest.getTraceRefs().get("divergenceAlert"));
        return payload;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private List<Map<String, Object>> toLinkageMaps(List<GovernanceSqlRewriteDivergenceAlertLinkage> linkages) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (linkages == null) {
            return result;
        }
        for (GovernanceSqlRewriteDivergenceAlertLinkage linkage : linkages) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("alertId", linkage.getAlertId());
            item.put("alertType", linkage.getAlertType());
            item.put("alertLevel", linkage.getAlertLevel());
            item.put("alertStatus", linkage.getAlertStatus());
            item.put("notifyStatus", linkage.getNotifyStatus());
            item.put("summary", linkage.getSummary());
            item.put("detailPath", linkage.getDetailPath());
            item.put("linkageMode", linkage.getLinkageMode());
            item.put("notificationLogId", linkage.getNotificationLogId());
            result.add(item);
        }
        return result;
    }

    private String resolveTriggerReason() {
        return StringUtils.hasText(properties.getTriggerReason())
            ? properties.getTriggerReason().trim()
            : "SCHEDULED_VALIDATION";
    }

    public static final class RewriteValidationSchedulerResult {
        private final boolean enabled;
        private final int candidateCount;
        private int processedCount;
        private int validationFailureCount;
        private int alertEmissionCount;
        private int alertFailureCount;
        private int auditWriteCount;
        private int auditFailureCount;

        private RewriteValidationSchedulerResult(boolean enabled, int candidateCount) {
            this.enabled = enabled;
            this.candidateCount = candidateCount;
        }

        public static RewriteValidationSchedulerResult disabled() {
            return new RewriteValidationSchedulerResult(false, 0);
        }

        public static RewriteValidationSchedulerResult enabled(int candidateCount) {
            return new RewriteValidationSchedulerResult(true, candidateCount);
        }

        private void incrementProcessed() {
            processedCount++;
        }

        private void incrementValidationFailures() {
            validationFailureCount++;
        }

        private void incrementAlertEmissions() {
            alertEmissionCount++;
        }

        private void incrementAlertFailures() {
            alertFailureCount++;
        }

        private void incrementAuditWrites() {
            auditWriteCount++;
        }

        private void incrementAuditFailures() {
            auditFailureCount++;
        }

        public boolean isEnabled() { return enabled; }
        public int getCandidateCount() { return candidateCount; }
        public int getProcessedCount() { return processedCount; }
        public int getValidationFailureCount() { return validationFailureCount; }
        public int getAlertEmissionCount() { return alertEmissionCount; }
        public int getAlertFailureCount() { return alertFailureCount; }
        public int getAuditWriteCount() { return auditWriteCount; }
        public int getAuditFailureCount() { return auditFailureCount; }
    }
}
