package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestResponse;
import com.company.sqloptimization.application.controller.dto.RewriteValidationRunCreateRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordCreateRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordPublishActionRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordReviewRequest;
import com.company.sqloptimization.application.controller.vo.RewritePublishEligibilityReasonVO;
import com.company.sqloptimization.application.controller.vo.RewritePublishEligibilityVO;
import com.company.sqloptimization.application.controller.vo.RewriteValidationRunVO;
import com.company.sqloptimization.application.controller.vo.SqlRewriteRecordVO;
import com.company.sqloptimization.domain.governance.ComparisonStatus;
import com.company.sqloptimization.domain.governance.DifferenceType;
import com.company.sqloptimization.domain.governance.RewritePublishStatus;
import com.company.sqloptimization.domain.governance.RewriteReviewStatus;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import com.company.sqloptimization.domain.governance.ValidationRunStatus;
import com.company.sqloptimization.domain.rewrite.RewriteValidationRun;
import com.company.sqloptimization.domain.rewrite.SqlRewriteRecord;
import com.company.sqloptimization.domain.rewrite.policy.RewritePublishEligibility;
import com.company.sqloptimization.domain.rewrite.policy.RewritePublishEligibilityPolicy;
import com.company.sqloptimization.domain.rewrite.policy.RewritePublishEligibilityReason;
import com.company.sqloptimization.domain.rewrite.repository.SqlRewriteRecordRepository;
import com.company.sqloptimization.infrastructure.queryexecution.QueryExecutionResultDigestClient;
import com.company.sqloptimization.infrastructure.queryexecution.QueryExecutionRuntimeRewriteBindingClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SqlRewriteRecordApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "ACCELERATION_REWRITE_CONTRACT_BASELINE";

    private final SqlRewriteRecordRepository sqlRewriteRecordRepository;
    private final QueryExecutionResultDigestClient queryExecutionResultDigestClient;
    private final ResultDigestComparisonEngine resultDigestComparisonEngine;
    private final RewritePublishEligibilityPolicy rewritePublishEligibilityPolicy;

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository) {
        this(sqlRewriteRecordRepository, null, new ResultDigestComparisonEngine());
    }

    @Autowired
    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              QueryExecutionResultDigestClient queryExecutionResultDigestClient,
                                              ResultDigestComparisonEngine resultDigestComparisonEngine) {
        this(
            sqlRewriteRecordRepository,
            queryExecutionResultDigestClient,
            resultDigestComparisonEngine,
            new RewritePublishEligibilityPolicy()
        );
    }

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              QueryExecutionResultDigestClient queryExecutionResultDigestClient,
                                              ResultDigestComparisonEngine resultDigestComparisonEngine,
                                              QueryExecutionRuntimeRewriteBindingClient ignoredRuntimeRewriteBindingClient) {
        this(
            sqlRewriteRecordRepository,
            queryExecutionResultDigestClient,
            resultDigestComparisonEngine,
            new RewritePublishEligibilityPolicy()
        );
    }

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              QueryExecutionResultDigestClient queryExecutionResultDigestClient,
                                              ResultDigestComparisonEngine resultDigestComparisonEngine,
                                              RewritePublishEligibilityPolicy rewritePublishEligibilityPolicy) {
        this.sqlRewriteRecordRepository = sqlRewriteRecordRepository;
        this.queryExecutionResultDigestClient = queryExecutionResultDigestClient;
        this.resultDigestComparisonEngine = resultDigestComparisonEngine;
        this.rewritePublishEligibilityPolicy = rewritePublishEligibilityPolicy;
    }

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              QueryExecutionResultDigestClient queryExecutionResultDigestClient,
                                              ResultDigestComparisonEngine resultDigestComparisonEngine,
                                              RewritePublishEligibilityPolicy rewritePublishEligibilityPolicy,
                                              QueryExecutionRuntimeRewriteBindingClient ignoredRuntimeRewriteBindingClient) {
        this(
            sqlRewriteRecordRepository,
            queryExecutionResultDigestClient,
            resultDigestComparisonEngine,
            rewritePublishEligibilityPolicy
        );
    }

    public SqlRewriteRecordVO createRewriteRecord(SqlRewriteRecordCreateRequest request) {
        String tenantId = requireAuthorizedTenant(request == null ? null : request.getTenantId());
        if (request == null) {
            throw invalidArgument("request", "rewrite record request 为必填项");
        }
        Instant now = Instant.now();
        SqlRewriteRecord rewriteRecord = SqlRewriteRecord.builder()
            .rewriteRecordId(UUID.randomUUID().toString())
            .tenantId(tenantId)
            .recommendationId(trimToNull(request.getRecommendationId()))
            .optimizationTaskId(trimToNull(request.getOptimizationTaskId()))
            .sourceType(request.getSourceType())
            .sourceKind(request.getSourceKind())
            .sourceId(trimToNull(request.getSourceId()))
            .evidenceLevel(request.getEvidenceLevel())
            .historyId(trimToNull(request.getHistoryId()))
            .parseHistoryId(trimToNull(request.getParseHistoryId()))
            .sqlFingerprint(trimToNull(request.getSqlFingerprint()))
            .datasourceCode(trimToNull(request.getDatasourceCode()))
            .status(request.getStatus())
            .validationStatus(request.getValidationStatus())
            .autoApplyAllowed(Boolean.TRUE.equals(request.getAutoApplyAllowed()))
            .manualReviewRequired(Boolean.TRUE.equals(request.getManualReviewRequired()))
            .reviewStatus(null)
            .reviewNote(null)
            .reviewedBy(null)
            .reviewedAt(null)
            .publishStatus(request.getPublishStatus())
            .runtimeBindingId(trimToNull(request.getRuntimeBindingId()))
            .runtimeBindingAt(request.getRuntimeBindingAt())
            .runtimeBindingBy(trimToNull(request.getRuntimeBindingBy()))
            .runtimeBindingScope(trimToNull(request.getRuntimeBindingScope()))
            .publishedSqlFingerprint(trimToNull(request.getPublishedSqlFingerprint()))
            .runtimeRuleVersion(trimToNull(request.getRuntimeRuleVersion()))
            .validationPolicyId(trimToNull(request.getValidationPolicyId()))
            .alertStatus(request.getAlertStatus())
            .originalSqlText(trimToNull(request.getOriginalSqlText()))
            .recommendedSqlText(trimToNull(request.getRecommendedSqlText()))
            .executedSqlText(trimToNull(request.getExecutedSqlText()))
            .createdBy(RequestContext.getUserId())
            .createdAt(now)
            .updatedAt(now)
            .ruleChain(request.getRuleChain())
            .diffSummary(request.getDiffSummary())
            .risk(request.getRisk())
            .traceRefs(enrichRewriteTrialTraceRefs(request.getTraceRefs(), request.getSourceProblems(), request.getIssueRuleLinks()))
            .build();
        return toRewriteRecordVo(sqlRewriteRecordRepository.saveRecord(rewriteRecord));
    }

    public List<SqlRewriteRecordVO> listRewriteRecords(String historyId,
                                                       String recommendationId,
                                                       String validationStatus,
                                                       String sourceType) {
        String tenantId = requireContextTenant();
        String normalizedHistoryId = trimToNull(historyId);
        List<SqlRewriteRecord> records = normalizedHistoryId == null
            ? sqlRewriteRecordRepository.findRecordsByTenantId(tenantId)
            : sqlRewriteRecordRepository.findRecordsByTenantIdAndHistoryId(tenantId, normalizedHistoryId);
        List<SqlRewriteRecordVO> result = new ArrayList<SqlRewriteRecordVO>();
        for (SqlRewriteRecord record : records) {
            if (matches(record, normalizedHistoryId, recommendationId, validationStatus, sourceType)) {
                result.add(toRewriteRecordVo(record));
            }
        }
        return result;
    }

    public SqlRewriteRecordVO getRewriteRecord(String rewriteRecordId) {
        SqlRewriteRecord rewriteRecord = requireRewriteRecord(rewriteRecordId);
        return toRewriteRecordVo(rewriteRecord);
    }

    public SqlRewriteRecordVO reviewRewriteRecord(String rewriteRecordId,
                                                  SqlRewriteRecordReviewRequest request) {
        if (request == null) {
            throw invalidArgument("request", "rewrite record review request 为必填项");
        }
        SqlRewriteRecord rewriteRecord = requireRewriteRecord(rewriteRecordId);
        String tenantId = requireAuthorizedTenant(request.getTenantId());
        if (!tenantId.equals(rewriteRecord.getTenantId())) {
            throw new AccessDeniedException("当前认证租户无权审核该改写记录");
        }
        RewriteReviewStatus nextStatus = request.getReviewStatus();
        validateReviewTransition(rewriteRecord, nextStatus);
        String reviewNote = trimToNull(request.getReviewNote());
        requireReviewNote(nextStatus, reviewNote);
        String reviewedBy = requireContextUser();
        Instant reviewedAt = Instant.now();
        SqlRewriteRecord reviewed = rewriteRecord.withReview(
            nextStatus,
            reviewNote,
            reviewedBy,
            reviewedAt,
            buildReviewTraceRefs(rewriteRecord, nextStatus, reviewedBy, reviewedAt)
        );
        return toRewriteRecordVo(sqlRewriteRecordRepository.saveRecord(reviewed));
    }

    public RewritePublishEligibilityVO getPublishEligibility(String rewriteRecordId) {
        SqlRewriteRecord rewriteRecord = requireRewriteRecord(rewriteRecordId);
        return toPublishEligibilityVo(evaluatePublishEligibility(rewriteRecord));
    }

    public SqlRewriteRecordVO publishRewriteRecord(String rewriteRecordId,
                                                   SqlRewriteRecordPublishActionRequest request) {
        SqlRewriteRecord rewriteRecord = requireRewriteRecord(rewriteRecordId);
        requireSameActionTenant(rewriteRecord, request);
        requireApprovedForPublish(rewriteRecord);
        requirePublishStatus(rewriteRecord);
        String operator = requireContextUser();
        String reason = actionReason(request);
        Instant now = Instant.now();
        SqlRewriteRecord published = rewriteRecord.withPublishStatusOnly(
            RewritePublishStatus.PUBLISHED,
            operator,
            now,
            buildPublishStatusTraceRefs(
                rewriteRecord,
                "PUBLISH",
                RewritePublishStatus.PUBLISHED,
                reason,
                operator,
                now
            )
        );
        return toRewriteRecordVo(sqlRewriteRecordRepository.saveRecord(published));
    }

    public SqlRewriteRecordVO pauseRewriteRecord(String rewriteRecordId,
                                                 SqlRewriteRecordPublishActionRequest request) {
        SqlRewriteRecord rewriteRecord = requireRewriteRecord(rewriteRecordId);
        requireSameActionTenant(rewriteRecord, request);
        requirePublishedStatus(rewriteRecord, "pause");
        String operator = requireContextUser();
        String reason = actionReason(request);
        Instant now = Instant.now();
        SqlRewriteRecord paused = rewriteRecord.withPublishStatusOnly(
            RewritePublishStatus.PAUSED,
            operator,
            now,
            buildPublishStatusTraceRefs(
                rewriteRecord,
                "PAUSE",
                RewritePublishStatus.PAUSED,
                reason,
                operator,
                now
            )
        );
        return toRewriteRecordVo(sqlRewriteRecordRepository.saveRecord(paused));
    }

    public SqlRewriteRecordVO unpublishRewriteRecord(String rewriteRecordId,
                                                     SqlRewriteRecordPublishActionRequest request) {
        SqlRewriteRecord rewriteRecord = requireRewriteRecord(rewriteRecordId);
        requireSameActionTenant(rewriteRecord, request);
        requireUnpublishableStatus(rewriteRecord);
        String operator = requireContextUser();
        String reason = actionReason(request);
        Instant now = Instant.now();
        SqlRewriteRecord unpublished = rewriteRecord.withPublishStatusOnly(
            RewritePublishStatus.UNPUBLISHED,
            operator,
            now,
            buildPublishStatusTraceRefs(
                rewriteRecord,
                "UNPUBLISH",
                RewritePublishStatus.UNPUBLISHED,
                reason,
                operator,
                now
            )
        );
        return toRewriteRecordVo(sqlRewriteRecordRepository.saveRecord(unpublished));
    }

    private Map<String, Object> buildPublishStatusTraceRefs(SqlRewriteRecord rewriteRecord,
                                                            String action,
                                                            RewritePublishStatus publishStatus,
                                                            String reason,
                                                            String operator,
                                                            Instant occurredAt) {
        Map<String, Object> traceRefs = new LinkedHashMap<String, Object>(rewriteRecord.getTraceRefs());
        Map<String, Object> publishTrace = new LinkedHashMap<String, Object>();
        publishTrace.put("action", action);
        publishTrace.put("publishStatus", publishStatus.name());
        publishTrace.put("operator", operator);
        publishTrace.put("occurredAt", occurredAt.toString());
        publishTrace.put("statusOnly", Boolean.TRUE);
        String trimmedReason = trimToNull(reason);
        if (trimmedReason != null) {
            publishTrace.put("reason", trimmedReason);
        }
        String requestId = trimToNull(RequestContext.getRequestId());
        if (requestId != null) {
            publishTrace.put("requestId", requestId);
        }
        String traceId = trimToNull(RequestContext.getTraceId());
        if (traceId != null) {
            publishTrace.put("traceId", traceId);
        }
        traceRefs.put("lastPublishStatusTrace", publishTrace);
        return traceRefs;
    }

    private RewritePublishEligibility evaluatePublishEligibility(SqlRewriteRecord rewriteRecord) {
        List<RewriteValidationRun> runs =
            sqlRewriteRecordRepository.findValidationRunsByRewriteRecordId(rewriteRecord.getRewriteRecordId());
        List<RewriteValidationRun> tenantRuns = new ArrayList<RewriteValidationRun>();
        for (RewriteValidationRun run : runs) {
            if (rewriteRecord.getTenantId().equals(run.getTenantId())) {
                tenantRuns.add(run);
            }
        }
        return rewritePublishEligibilityPolicy.evaluate(rewriteRecord, tenantRuns);
    }

    public RewriteValidationRunVO createValidationRun(String rewriteRecordId,
                                                      RewriteValidationRunCreateRequest request) {
        SqlRewriteRecord rewriteRecord = requireRewriteRecord(rewriteRecordId);
        String tenantId = requireAuthorizedTenant(request == null ? null : request.getTenantId());
        if (!tenantId.equals(rewriteRecord.getTenantId())) {
            throw new AccessDeniedException("当前认证租户无权为该改写记录创建校验运行");
        }
        RewriteValidationRun validationRun = queryExecutionResultDigestClient == null
            ? buildClientSubmittedValidationRun(rewriteRecord, tenantId, request)
            : buildExecutedValidationRun(rewriteRecord, tenantId, request);
        sqlRewriteRecordRepository.saveValidationRun(validationRun);
        SqlRewriteRecord summarized = rewriteRecord.withValidationSummary(validationRun, Instant.now());
        sqlRewriteRecordRepository.saveRecord(applyAutomaticRuntimePauseIfRequired(summarized, validationRun));
        return toValidationRunVo(validationRun);
    }

    private SqlRewriteRecord applyAutomaticRuntimePauseIfRequired(SqlRewriteRecord rewriteRecord,
                                                                  RewriteValidationRun validationRun) {
        if (!shouldAutoPauseRuntimeBinding(rewriteRecord, validationRun)) {
            return rewriteRecord;
        }
        String operator = contextUserOrSystem();
        String reason = "定时校验发现差异：" + validationRun.getDifferenceType().name();
        Instant now = Instant.now();
        return rewriteRecord.withPublishStatusOnly(
            RewritePublishStatus.PAUSED,
            operator,
            now,
            buildPublishStatusTraceRefs(
                rewriteRecord,
                "AUTO_PAUSE",
                RewritePublishStatus.PAUSED,
                reason,
                operator,
                now
            )
        );
    }

    private boolean shouldAutoPauseRuntimeBinding(SqlRewriteRecord rewriteRecord,
                                                  RewriteValidationRun validationRun) {
        return validationRun != null
            && validationRun.getComparisonStatus() == ComparisonStatus.DIVERGED
            && validationRun.isAutoApplyPaused()
            && rewriteRecord.getPublishStatus() == RewritePublishStatus.PUBLISHED
            && "rewrite-validation-scheduler".equals(RequestContext.getUserId());
    }

    private RewriteValidationRun buildExecutedValidationRun(SqlRewriteRecord rewriteRecord,
                                                            String tenantId,
                                                            RewriteValidationRunCreateRequest request) {
        String validationRunId = UUID.randomUUID().toString();
        Instant startedAt = request == null || request.getStartedAt() == null ? Instant.now() : request.getStartedAt();
        Map<String, Object> comparisonPolicy = buildComparisonPolicy(rewriteRecord, request);
        QueryExecutionResultDigestResponse originalDigest = null;
        QueryExecutionResultDigestResponse recommendedDigest = null;
        ResultDigestComparisonResult comparisonResult;
        try {
            originalDigest = queryExecutionResultDigestClient.executeDigest(buildDigestRequest(
                rewriteRecord,
                request,
                tenantId,
                validationRunId,
                rewriteRecord.getOriginalSqlText(),
                comparisonPolicy
            ));
            recommendedDigest = queryExecutionResultDigestClient.executeDigest(buildDigestRequest(
                rewriteRecord,
                request,
                tenantId,
                validationRunId,
                rewriteRecord.getRecommendedSqlText(),
                comparisonPolicy
            ));
            comparisonResult = resultDigestComparisonEngine.compare(originalDigest, recommendedDigest, comparisonPolicy);
        } catch (RuntimeException ex) {
            comparisonResult = failedComparisonResult(ex, comparisonPolicy);
        }
        return RewriteValidationRun.builder()
            .validationRunId(validationRunId)
            .tenantId(tenantId)
            .rewriteRecordId(rewriteRecord.getRewriteRecordId())
            .recommendationId(resolveValue(
                request == null ? null : request.getRecommendationId(),
                rewriteRecord.getRecommendationId()
            ))
            .historyId(resolveValue(request == null ? null : request.getHistoryId(), rewriteRecord.getHistoryId()))
            .sqlFingerprint(resolveValue(
                request == null ? null : request.getSqlFingerprint(),
                rewriteRecord.getSqlFingerprint()
            ))
            .status(comparisonResult.getValidationRunStatus())
            .comparisonStatus(comparisonResult.getComparisonStatus())
            .differenceType(comparisonResult.getDifferenceType())
            .autoApplyPaused(comparisonResult.isAutoApplyPaused())
            .startedAt(startedAt)
            .finishedAt(Instant.now())
            .comparisonPolicy(comparisonPolicy)
            .originalResultDigest(originalDigest == null ? Collections.<String, Object>emptyMap() : originalDigest.getResultDigest())
            .recommendedResultDigest(recommendedDigest == null ? Collections.<String, Object>emptyMap() : recommendedDigest.getResultDigest())
            .differenceSample(comparisonResult.getDifferenceSample())
            .executionEvidence(comparisonResult.getExecutionEvidence())
            .build();
    }

    private RewriteValidationRun buildClientSubmittedValidationRun(SqlRewriteRecord rewriteRecord,
                                                                   String tenantId,
                                                                   RewriteValidationRunCreateRequest request) {
        Instant now = Instant.now();
        return RewriteValidationRun.builder()
            .validationRunId(UUID.randomUUID().toString())
            .tenantId(tenantId)
            .rewriteRecordId(rewriteRecord.getRewriteRecordId())
            .recommendationId(resolveValue(
                request == null ? null : request.getRecommendationId(),
                rewriteRecord.getRecommendationId()
            ))
            .historyId(resolveValue(request == null ? null : request.getHistoryId(), rewriteRecord.getHistoryId()))
            .sqlFingerprint(resolveValue(
                request == null ? null : request.getSqlFingerprint(),
                rewriteRecord.getSqlFingerprint()
            ))
            .status(request == null ? null : request.getStatus())
            .comparisonStatus(resolveComparisonStatus(request))
            .differenceType(request == null ? null : request.getDifferenceType())
            .autoApplyPaused(request != null && Boolean.TRUE.equals(request.getAutoApplyPaused()))
            .startedAt(request == null || request.getStartedAt() == null ? now : request.getStartedAt())
            .finishedAt(request == null ? null : request.getFinishedAt())
            .comparisonPolicy(request == null ? null : request.getComparisonPolicy())
            .originalResultDigest(request == null ? null : request.getOriginalResultDigest())
            .recommendedResultDigest(request == null ? null : request.getRecommendedResultDigest())
            .differenceSample(request == null ? null : request.getDifferenceSample())
            .executionEvidence(request == null ? null : request.getExecutionEvidence())
            .build();
    }

    private QueryExecutionResultDigestRequest buildDigestRequest(SqlRewriteRecord rewriteRecord,
                                                                 RewriteValidationRunCreateRequest request,
                                                                 String tenantId,
                                                                 String validationRunId,
                                                                 String sqlText,
                                                                 Map<String, Object> comparisonPolicy) {
        QueryExecutionResultDigestRequest digestRequest = new QueryExecutionResultDigestRequest();
        digestRequest.setTenantId(tenantId);
        digestRequest.setValidationRunId(validationRunId);
        digestRequest.setRewriteRecordId(rewriteRecord.getRewriteRecordId());
        digestRequest.setSqlFingerprint(resolveValue(
            request == null ? null : request.getSqlFingerprint(),
            rewriteRecord.getSqlFingerprint()
        ));
        digestRequest.setSqlText(sqlText);
        digestRequest.setDatasourceType(resolveDatasourceType(rewriteRecord, request));
        digestRequest.setDatasourceCode(rewriteRecord.getDatasourceCode());
        digestRequest.setComparisonPolicy(comparisonPolicy);
        return digestRequest;
    }

    private Map<String, Object> buildComparisonPolicy(SqlRewriteRecord rewriteRecord,
                                                       RewriteValidationRunCreateRequest request) {
        Map<String, Object> policy = new LinkedHashMap<String, Object>();
        policy.put("policyId", StringUtils.hasText(rewriteRecord.getValidationPolicyId())
            ? rewriteRecord.getValidationPolicyId()
            : "DEFAULT_READONLY_DIGEST_POLICY");
        policy.put("executionMode", "READONLY_RESULT_DIGEST");
        policy.put("sampleLimit", Integer.valueOf(5));
        policy.put("orderSensitive", Boolean.FALSE);
        policy.put("numericTolerance", "0");
        policy.put("timezone", "UTC");
        policy.put("nullHandling", "STRICT");
        if (request != null && request.getComparisonPolicy() != null) {
            policy.putAll(request.getComparisonPolicy());
        }
        if (request != null && StringUtils.hasText(request.getTriggerReason())) {
            policy.put("triggerReason", request.getTriggerReason().trim());
        }
        policy.put("readonlyOnly", Boolean.TRUE);
        policy.put("rewriteRecordId", rewriteRecord.getRewriteRecordId());
        return policy;
    }

    private DataSourceTypeEnum resolveDatasourceType(SqlRewriteRecord rewriteRecord,
                                                     RewriteValidationRunCreateRequest request) {
        if (request != null && request.getDatasourceType() != null) {
            return request.getDatasourceType();
        }
        String datasourceCode = rewriteRecord.getDatasourceCode();
        if (StringUtils.hasText(datasourceCode)) {
            try {
                return DataSourceTypeEnum.valueOf(datasourceCode.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return DataSourceTypeEnum.AUTO;
            }
        }
        return DataSourceTypeEnum.AUTO;
    }

    private ResultDigestComparisonResult failedComparisonResult(RuntimeException exception,
                                                               Map<String, Object> comparisonPolicy) {
        Map<String, Object> sample = new LinkedHashMap<String, Object>();
        sample.put("reason", "只读摘要执行失败");
        sample.put("differenceType", DifferenceType.UNKNOWN.name());
        sample.put("errorType", exception.getClass().getSimpleName());
        sample.put("errorMessage", exception.getMessage());

        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("comparisonPolicy", comparisonPolicy == null ? Collections.emptyMap() : comparisonPolicy);
        evidence.put("readonlyDigestOnly", Boolean.TRUE);
        evidence.put("errorType", exception.getClass().getSimpleName());
        evidence.put("errorMessage", exception.getMessage());
        return new ResultDigestComparisonResult(
            ValidationRunStatus.FAILED,
            ComparisonStatus.FAILED,
            DifferenceType.UNKNOWN,
            false,
            sample,
            evidence
        );
    }

    public List<RewriteValidationRunVO> listValidationRuns(String rewriteRecordId) {
        SqlRewriteRecord rewriteRecord = requireRewriteRecord(rewriteRecordId);
        List<RewriteValidationRun> runs =
            sqlRewriteRecordRepository.findValidationRunsByRewriteRecordId(rewriteRecord.getRewriteRecordId());
        List<RewriteValidationRunVO> result = new ArrayList<RewriteValidationRunVO>(runs.size());
        for (RewriteValidationRun run : runs) {
            if (rewriteRecord.getTenantId().equals(run.getTenantId())) {
                result.add(toValidationRunVo(run));
            }
        }
        return result;
    }

    private void requireSameActionTenant(SqlRewriteRecord rewriteRecord,
                                         SqlRewriteRecordPublishActionRequest request) {
        String tenantId = requireAuthorizedTenant(request == null ? null : request.getTenantId());
        if (!tenantId.equals(rewriteRecord.getTenantId())) {
            throw new AccessDeniedException("当前认证租户无权变更该改写记录的发布状态");
        }
    }

    private void requireApprovedForPublish(SqlRewriteRecord rewriteRecord) {
        if (rewriteRecord.getReviewStatus() == RewriteReviewStatus.APPROVED) {
            return;
        }
        throw new BizException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID,
            HttpStatus.CONFLICT,
            "只有 APPROVED 状态的改写记录才能发布"
        );
    }

    private void requirePublishStatus(SqlRewriteRecord rewriteRecord) {
        RewritePublishStatus publishStatus = rewriteRecord.getPublishStatus();
        if (publishStatus == RewritePublishStatus.UNPUBLISHED
            || publishStatus == RewritePublishStatus.PUBLISH_FAILED
            || publishStatus == RewritePublishStatus.PAUSED) {
            return;
        }
        throw new BizException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID,
            HttpStatus.CONFLICT,
            "只有 UNPUBLISHED、PUBLISH_FAILED 或 PAUSED 状态的改写记录才能发布"
        );
    }

    private void requirePublishedStatus(SqlRewriteRecord rewriteRecord, String action) {
        if (rewriteRecord.getPublishStatus() == RewritePublishStatus.PUBLISHED) {
            return;
        }
        throw new BizException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID,
            HttpStatus.CONFLICT,
            "只有 PUBLISHED 状态的改写记录才能执行 " + action
        );
    }

    private void requireUnpublishableStatus(SqlRewriteRecord rewriteRecord) {
        if (rewriteRecord.getPublishStatus() == RewritePublishStatus.PUBLISHED
            || rewriteRecord.getPublishStatus() == RewritePublishStatus.PAUSED) {
            return;
        }
        throw new BizException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID,
            HttpStatus.CONFLICT,
            "只有 PUBLISHED 或 PAUSED 状态的改写记录才能下线"
        );
    }

    private String actionReason(SqlRewriteRecordPublishActionRequest request) {
        return request == null ? null : trimToNull(request.getReason());
    }

    private void requireText(String value, String field) {
        if (StringUtils.hasText(value)) {
            return;
        }
        throw invalidArgument(field, field + " 为必填项");
    }

    private void validateReviewTransition(SqlRewriteRecord rewriteRecord,
                                          RewriteReviewStatus nextStatus) {
        if (nextStatus == null) {
            throw invalidArgument("reviewStatus", "reviewStatus 为必填项");
        }
        if (rewriteRecord.canTransitionReviewTo(nextStatus)) {
            return;
        }
        throw new BizException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID,
            HttpStatus.CONFLICT,
            "非法的改写评审状态流转：" + rewriteRecord.getReviewStatus() + " -> " + nextStatus
        );
    }

    private void requireReviewNote(RewriteReviewStatus nextStatus, String reviewNote) {
        if (nextStatus == RewriteReviewStatus.APPROVED || StringUtils.hasText(reviewNote)) {
            return;
        }
        throw invalidArgument("reviewNote", nextStatus + " 状态需要提供 reviewNote");
    }

    private Map<String, Object> buildReviewTraceRefs(SqlRewriteRecord rewriteRecord,
                                                     RewriteReviewStatus nextStatus,
                                                     String reviewedBy,
                                                     Instant reviewedAt) {
        Map<String, Object> traceRefs = new LinkedHashMap<String, Object>(rewriteRecord.getTraceRefs());
        Map<String, Object> reviewTrace = new LinkedHashMap<String, Object>();
        reviewTrace.put("reviewStatus", nextStatus.name());
        reviewTrace.put("reviewedBy", reviewedBy);
        reviewTrace.put("reviewedAt", reviewedAt.toString());
        String requestId = trimToNull(RequestContext.getRequestId());
        if (requestId != null) {
            reviewTrace.put("requestId", requestId);
        }
        String traceId = trimToNull(RequestContext.getTraceId());
        if (traceId != null) {
            reviewTrace.put("traceId", traceId);
        }
        traceRefs.put("lastReviewTrace", reviewTrace);
        return traceRefs;
    }

    private boolean matches(SqlRewriteRecord record,
                            String historyId,
                            String recommendationId,
                            String validationStatus,
                            String sourceType) {
        if (StringUtils.hasText(historyId) && !historyId.trim().equals(record.getHistoryId())) {
            return false;
        }
        if (StringUtils.hasText(recommendationId)
            && !recommendationId.trim().equals(record.getRecommendationId())) {
            return false;
        }
        if (StringUtils.hasText(validationStatus)
            && !validationStatus.trim().equals(record.getValidationStatus().name())) {
            return false;
        }
        return !StringUtils.hasText(sourceType) || sourceType.trim().equals(record.getSourceType().name());
    }

    private SqlRewriteRecord requireRewriteRecord(String rewriteRecordId) {
        SqlRewriteRecord rewriteRecord = sqlRewriteRecordRepository.findRecordById(rewriteRecordId);
        if (rewriteRecord == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "SQL 改写记录不存在：" + rewriteRecordId
            );
        }
        verifyTenantAccess(rewriteRecord.getTenantId());
        return rewriteRecord;
    }

    private SqlRewriteRecordVO toRewriteRecordVo(SqlRewriteRecord record) {
        SqlRewriteRecordVO vo = new SqlRewriteRecordVO();
        vo.setRewriteRecordId(record.getRewriteRecordId());
        vo.setTenantId(record.getTenantId());
        vo.setRecommendationId(record.getRecommendationId());
        vo.setOptimizationTaskId(record.getOptimizationTaskId());
        vo.setSourceType(record.getSourceType().name());
        vo.setSourceKind(record.getSourceKind().name());
        vo.setSourceId(record.getSourceId());
        vo.setEvidenceLevel(record.getEvidenceLevel().name());
        vo.setHistoryId(record.getHistoryId());
        vo.setParseHistoryId(record.getParseHistoryId());
        vo.setSqlFingerprint(record.getSqlFingerprint());
        vo.setDatasourceCode(record.getDatasourceCode());
        vo.setStatus(record.getStatus().name());
        vo.setValidationStatus(record.getValidationStatus().name());
        vo.setAutoApplyAllowed(Boolean.valueOf(record.isAutoApplyAllowed()));
        vo.setManualReviewRequired(Boolean.valueOf(record.isManualReviewRequired()));
        vo.setReviewStatus(record.getReviewStatus().name());
        vo.setReviewNote(record.getReviewNote());
        vo.setReviewedBy(record.getReviewedBy());
        vo.setReviewedAt(record.getReviewedAt());
        vo.setPublishStatus(record.getPublishStatus().name());
        vo.setRuntimeBindingId(record.getRuntimeBindingId());
        vo.setRuntimeBindingAt(record.getRuntimeBindingAt());
        vo.setRuntimeBindingBy(record.getRuntimeBindingBy());
        vo.setRuntimeBindingScope(record.getRuntimeBindingScope());
        vo.setPublishedSqlFingerprint(record.getPublishedSqlFingerprint());
        vo.setRuntimeRuleVersion(record.getRuntimeRuleVersion());
        vo.setValidationPolicyId(record.getValidationPolicyId());
        vo.setLastValidationRunId(record.getLastValidationRunId());
        vo.setLastComparedAt(record.getLastComparedAt());
        vo.setAlertStatus(record.getAlertStatus().name());
        vo.setOriginalSqlText(record.getOriginalSqlText());
        vo.setRecommendedSqlText(record.getRecommendedSqlText());
        vo.setExecutedSqlText(record.getExecutedSqlText());
        vo.setCreatedBy(record.getCreatedBy());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setUpdatedAt(record.getUpdatedAt());
        vo.setRuleChain(record.getRuleChain());
        vo.setSourceProblems(traceList(record.getTraceRefs(), "sourceProblems"));
        vo.setIssueRuleLinks(traceList(record.getTraceRefs(), "issueRuleLinks"));
        vo.setDiffSummary(record.getDiffSummary());
        vo.setRisk(record.getRisk());
        vo.setTraceRefs(record.getTraceRefs());
        vo.setContractStage(CONTRACT_STAGE);
        vo.setImplementationStage(IMPLEMENTATION_STAGE);
        return vo;
    }

    private Map<String, Object> enrichRewriteTrialTraceRefs(Map<String, Object> traceRefs,
                                                            List<Map<String, Object>> sourceProblems,
                                                            List<Map<String, Object>> issueRuleLinks) {
        Map<String, Object> result = traceRefs == null
            ? new LinkedHashMap<String, Object>()
            : new LinkedHashMap<String, Object>(traceRefs);
        if (sourceProblems != null && !sourceProblems.isEmpty()) {
            result.put("sourceProblems", sourceProblems);
        }
        if (issueRuleLinks != null && !issueRuleLinks.isEmpty()) {
            result.put("issueRuleLinks", issueRuleLinks);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> traceList(Map<String, Object> traceRefs, String key) {
        if (traceRefs == null || !(traceRefs.get(key) instanceof List)) {
            return Collections.emptyList();
        }
        return (List<Map<String, Object>>) traceRefs.get(key);
    }

    private RewritePublishEligibilityVO toPublishEligibilityVo(RewritePublishEligibility eligibility) {
        RewritePublishEligibilityVO vo = new RewritePublishEligibilityVO();
        vo.setRewriteRecordId(eligibility.getRewriteRecordId());
        vo.setTenantId(eligibility.getTenantId());
        vo.setPolicyId(eligibility.getPolicyId());
        vo.setEligible(Boolean.valueOf(eligibility.isEligible()));
        vo.setReviewStatus(eligibility.getReviewStatus());
        vo.setValidationStatus(eligibility.getValidationStatus());
        vo.setPublishStatus(eligibility.getPublishStatus());
        vo.setAlertStatus(eligibility.getAlertStatus());
        vo.setAutoApplyAllowed(eligibility.getAutoApplyAllowed());
        vo.setLastValidationRunId(eligibility.getLastValidationRunId());
        List<RewritePublishEligibilityReasonVO> reasons =
            new ArrayList<RewritePublishEligibilityReasonVO>(eligibility.getRefusalReasons().size());
        for (RewritePublishEligibilityReason reason : eligibility.getRefusalReasons()) {
            RewritePublishEligibilityReasonVO reasonVo = new RewritePublishEligibilityReasonVO();
            reasonVo.setCode(reason.getCode());
            reasonVo.setMessage(reason.getMessage());
            reasonVo.setBlocking(Boolean.valueOf(reason.isBlocking()));
            reasonVo.setField(reason.getField());
            reasonVo.setEvidenceRef(reason.getEvidenceRef());
            reasons.add(reasonVo);
        }
        vo.setRefusalReasons(reasons);
        vo.setContractStage(CONTRACT_STAGE);
        vo.setImplementationStage(IMPLEMENTATION_STAGE);
        return vo;
    }

    private RewriteValidationRunVO toValidationRunVo(RewriteValidationRun run) {
        RewriteValidationRunVO vo = new RewriteValidationRunVO();
        vo.setValidationRunId(run.getValidationRunId());
        vo.setTenantId(run.getTenantId());
        vo.setRewriteRecordId(run.getRewriteRecordId());
        vo.setRecommendationId(run.getRecommendationId());
        vo.setHistoryId(run.getHistoryId());
        vo.setSqlFingerprint(run.getSqlFingerprint());
        vo.setStatus(run.getStatus().name());
        vo.setComparisonStatus(run.getComparisonStatus().name());
        vo.setDifferenceType(run.getDifferenceType().name());
        vo.setAutoApplyPaused(Boolean.valueOf(run.isAutoApplyPaused()));
        vo.setStartedAt(run.getStartedAt());
        vo.setFinishedAt(run.getFinishedAt());
        vo.setComparisonPolicy(run.getComparisonPolicy());
        vo.setOriginalResultDigest(run.getOriginalResultDigest());
        vo.setRecommendedResultDigest(run.getRecommendedResultDigest());
        vo.setDifferenceSample(run.getDifferenceSample());
        vo.setExecutionEvidence(run.getExecutionEvidence());
        vo.setContractStage(CONTRACT_STAGE);
        vo.setImplementationStage(IMPLEMENTATION_STAGE);
        return vo;
    }

    private ComparisonStatus resolveComparisonStatus(RewriteValidationRunCreateRequest request) {
        if (request == null || request.getComparisonStatus() == null) {
            return ComparisonStatus.NOT_COMPARED;
        }
        return request.getComparisonStatus();
    }

    private String resolveValue(String candidate, String fallback) {
        String trimmed = trimToNull(candidate);
        return trimmed == null ? fallback : trimmed;
    }

    private String requireAuthorizedTenant(String requestTenantId) {
        String contextTenantId = requireContextTenant();
        if (StringUtils.hasText(requestTenantId) && !contextTenantId.equals(requestTenantId.trim())) {
            throw new AccessDeniedException("请求 tenantId 与已认证租户上下文不一致");
        }
        return contextTenantId;
    }

    private String requireContextTenant() {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "已认证请求上下文缺少 tenantId"
            );
        }
        return contextTenantId;
    }

    private String requireContextUser() {
        String contextUserId = RequestContext.getUserId();
        if (!StringUtils.hasText(contextUserId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "已认证请求上下文缺少 userId"
            );
        }
        return contextUserId;
    }

    private String contextUserOrSystem() {
        String contextUserId = RequestContext.getUserId();
        return StringUtils.hasText(contextUserId) ? contextUserId.trim() : "rewrite-validation-scheduler";
    }

    private void verifyTenantAccess(String resourceTenantId) {
        String contextTenantId = requireContextTenant();
        if (!contextTenantId.equals(resourceTenantId)) {
            throw new AccessDeniedException("当前认证租户无权访问该改写记录");
        }
    }

    private BizException invalidArgument(String field, String message) {
        return new BizException(
            ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
            HttpStatus.BAD_REQUEST,
            field + ": " + message
        );
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
