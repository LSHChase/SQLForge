package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingPublishRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingStateChangeRequest;
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
    private final QueryExecutionRuntimeRewriteBindingClient queryExecutionRuntimeRewriteBindingClient;

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository) {
        this(sqlRewriteRecordRepository, null, new ResultDigestComparisonEngine());
    }

    @Autowired
    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              QueryExecutionResultDigestClient queryExecutionResultDigestClient,
                                              ResultDigestComparisonEngine resultDigestComparisonEngine,
                                              QueryExecutionRuntimeRewriteBindingClient queryExecutionRuntimeRewriteBindingClient) {
        this(
            sqlRewriteRecordRepository,
            queryExecutionResultDigestClient,
            resultDigestComparisonEngine,
            new RewritePublishEligibilityPolicy(),
            queryExecutionRuntimeRewriteBindingClient
        );
    }

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              QueryExecutionResultDigestClient queryExecutionResultDigestClient,
                                              ResultDigestComparisonEngine resultDigestComparisonEngine) {
        this(
            sqlRewriteRecordRepository,
            queryExecutionResultDigestClient,
            resultDigestComparisonEngine,
            (QueryExecutionRuntimeRewriteBindingClient) null
        );
    }

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              QueryExecutionResultDigestClient queryExecutionResultDigestClient,
                                              ResultDigestComparisonEngine resultDigestComparisonEngine,
                                              RewritePublishEligibilityPolicy rewritePublishEligibilityPolicy) {
        this(
            sqlRewriteRecordRepository,
            queryExecutionResultDigestClient,
            resultDigestComparisonEngine,
            rewritePublishEligibilityPolicy,
            null
        );
    }

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              QueryExecutionResultDigestClient queryExecutionResultDigestClient,
                                              ResultDigestComparisonEngine resultDigestComparisonEngine,
                                              RewritePublishEligibilityPolicy rewritePublishEligibilityPolicy,
                                              QueryExecutionRuntimeRewriteBindingClient queryExecutionRuntimeRewriteBindingClient) {
        this.sqlRewriteRecordRepository = sqlRewriteRecordRepository;
        this.queryExecutionResultDigestClient = queryExecutionResultDigestClient;
        this.resultDigestComparisonEngine = resultDigestComparisonEngine;
        this.rewritePublishEligibilityPolicy = rewritePublishEligibilityPolicy;
        this.queryExecutionRuntimeRewriteBindingClient = queryExecutionRuntimeRewriteBindingClient;
    }

    public SqlRewriteRecordVO createRewriteRecord(SqlRewriteRecordCreateRequest request) {
        String tenantId = requireAuthorizedTenant(request == null ? null : request.getTenantId());
        if (request == null) {
            throw invalidArgument("request", "rewrite record request is required");
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
            .traceRefs(request.getTraceRefs())
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
            throw invalidArgument("request", "rewrite record review request is required");
        }
        SqlRewriteRecord rewriteRecord = requireRewriteRecord(rewriteRecordId);
        String tenantId = requireAuthorizedTenant(request.getTenantId());
        if (!tenantId.equals(rewriteRecord.getTenantId())) {
            throw new AccessDeniedException("Authenticated tenant cannot review this rewrite record");
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
        QueryExecutionRuntimeRewriteBindingClient runtimeClient = requireRuntimeBindingClient();
        RewritePublishEligibility eligibility = evaluatePublishEligibility(rewriteRecord);
        if (!eligibility.isEligible()) {
            throw publishRejected(eligibility);
        }

        String operator = requireContextUser();
        String reason = actionReason(request);
        RuntimeRewriteBindingResponse response = null;
        try {
            response = runtimeClient.publish(buildRuntimePublishRequest(rewriteRecord, operator));
            requirePublishedRuntimeResponse(rewriteRecord, response);
        } catch (RuntimeException ex) {
            compensatePublishedRuntimeBinding(rewriteRecord, response, operator);
            savePublishFailureTrace(rewriteRecord, operator, reason, ex);
            throw ex;
        }

        Instant now = Instant.now();
        Map<String, Object> traceRefs = buildRuntimeBindingTraceRefs(
            rewriteRecord,
            "PUBLISH",
            RewritePublishStatus.PUBLISHED.name(),
            response,
            reason,
            operator,
            now,
            null
        );
        SqlRewriteRecord published = rewriteRecord.withPublishedRuntimeBinding(
            response.getRuntimeBindingId(),
            response.getRuntimeRuleVersion(),
            resolveValue(response.getSqlFingerprint(), rewriteRecord.getSqlFingerprint()),
            runtimeBindingScope(rewriteRecord),
            operator,
            now,
            traceRefs
        );
        try {
            return toRewriteRecordVo(sqlRewriteRecordRepository.saveRecord(published));
        } catch (RuntimeException ex) {
            compensatePublishedRuntimeBinding(rewriteRecord, response, operator);
            throw ex;
        }
    }

    public SqlRewriteRecordVO pauseRewriteRecord(String rewriteRecordId,
                                                 SqlRewriteRecordPublishActionRequest request) {
        SqlRewriteRecord rewriteRecord = requireRewriteRecord(rewriteRecordId);
        requireSameActionTenant(rewriteRecord, request);
        requirePublishedStatus(rewriteRecord, "pause");
        requireText(rewriteRecord.getRuntimeBindingId(), "runtimeBindingId");
        QueryExecutionRuntimeRewriteBindingClient runtimeClient = requireRuntimeBindingClient();

        String operator = requireContextUser();
        String reason = actionReason(request);
        RuntimeRewriteBindingResponse response;
        try {
            response = runtimeClient.pause(buildRuntimeStateChangeRequest(rewriteRecord, operator, reason));
            requireRuntimeStateResponse(rewriteRecord, response, "PAUSED");
        } catch (RuntimeException ex) {
            saveRuntimeFailureTrace(rewriteRecord, "PAUSE", operator, reason, ex);
            throw ex;
        }

        Instant now = Instant.now();
        SqlRewriteRecord paused = rewriteRecord.withPausedRuntimeBinding(
            operator,
            now,
            buildRuntimeBindingTraceRefs(
                rewriteRecord,
                "PAUSE",
                RewritePublishStatus.PAUSED.name(),
                response,
                reason,
                operator,
                now,
                null
            )
        );
        return toRewriteRecordVo(sqlRewriteRecordRepository.saveRecord(paused));
    }

    public SqlRewriteRecordVO unpublishRewriteRecord(String rewriteRecordId,
                                                     SqlRewriteRecordPublishActionRequest request) {
        SqlRewriteRecord rewriteRecord = requireRewriteRecord(rewriteRecordId);
        requireSameActionTenant(rewriteRecord, request);
        requireUnpublishableStatus(rewriteRecord);
        requireText(rewriteRecord.getRuntimeBindingId(), "runtimeBindingId");
        QueryExecutionRuntimeRewriteBindingClient runtimeClient = requireRuntimeBindingClient();

        String operator = requireContextUser();
        String reason = actionReason(request);
        RuntimeRewriteBindingResponse response;
        try {
            response = runtimeClient.unpublish(buildRuntimeStateChangeRequest(rewriteRecord, operator, reason));
            requireRuntimeStateResponse(rewriteRecord, response, "UNPUBLISHED");
        } catch (RuntimeException ex) {
            saveRuntimeFailureTrace(rewriteRecord, "UNPUBLISH", operator, reason, ex);
            throw ex;
        }

        Instant now = Instant.now();
        SqlRewriteRecord unpublished = rewriteRecord.withUnpublishedRuntimeBinding(
            operator,
            now,
            buildRuntimeBindingTraceRefs(
                rewriteRecord,
                "UNPUBLISH",
                RewritePublishStatus.UNPUBLISHED.name(),
                response,
                reason,
                operator,
                now,
                null
            )
        );
        return toRewriteRecordVo(sqlRewriteRecordRepository.saveRecord(unpublished));
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
            throw new AccessDeniedException("Authenticated tenant cannot create validation run for this rewrite record");
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
        String reason = "scheduled validation divergence: " + validationRun.getDifferenceType().name();
        Instant now = Instant.now();
        if (queryExecutionRuntimeRewriteBindingClient == null) {
            return rewriteRecord.withTraceRefs(
                buildRuntimeBindingTraceRefs(
                    rewriteRecord,
                    "AUTO_PAUSE",
                    rewriteRecord.getPublishStatus().name(),
                    null,
                    reason,
                    operator,
                    now,
                    runtimeContractFailure("query-execution runtime rewrite binding client is not configured")
                ),
                now
            );
        }
        if (!StringUtils.hasText(rewriteRecord.getRuntimeBindingId())) {
            return rewriteRecord.withTraceRefs(
                buildRuntimeBindingTraceRefs(
                    rewriteRecord,
                    "AUTO_PAUSE",
                    rewriteRecord.getPublishStatus().name(),
                    null,
                    reason,
                    operator,
                    now,
                    runtimeContractFailure("Published rewrite record runtimeBindingId is missing")
                ),
                now
            );
        }
        RuntimeRewriteBindingResponse response = null;
        try {
            response = queryExecutionRuntimeRewriteBindingClient.pause(
                buildRuntimeStateChangeRequest(rewriteRecord, operator, reason)
            );
            requireRuntimeStateResponse(rewriteRecord, response, "PAUSED");
            return rewriteRecord.withPausedRuntimeBinding(
                operator,
                now,
                buildRuntimeBindingTraceRefs(
                    rewriteRecord,
                    "AUTO_PAUSE",
                    RewritePublishStatus.PAUSED.name(),
                    response,
                    reason,
                    operator,
                    now,
                    null
                )
            );
        } catch (RuntimeException ex) {
            return rewriteRecord.withTraceRefs(
                buildRuntimeBindingTraceRefs(
                    rewriteRecord,
                    "AUTO_PAUSE",
                    rewriteRecord.getPublishStatus().name(),
                    response,
                    reason,
                    operator,
                    now,
                    ex
                ),
                now
            );
        }
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
        sample.put("reason", "readonly digest execution failed");
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

    private RuntimeRewriteBindingPublishRequest buildRuntimePublishRequest(SqlRewriteRecord rewriteRecord,
                                                                           String operator) {
        RuntimeRewriteBindingPublishRequest request = new RuntimeRewriteBindingPublishRequest();
        request.setTenantId(rewriteRecord.getTenantId());
        request.setRewriteRecordId(rewriteRecord.getRewriteRecordId());
        request.setRecommendationId(rewriteRecord.getRecommendationId());
        request.setSourceType(rewriteRecord.getSourceType().name());
        request.setSourceKind(rewriteRecord.getSourceKind().name());
        request.setSourceId(rewriteRecord.getSourceId());
        request.setSqlFingerprint(rewriteRecord.getSqlFingerprint());
        request.setOriginalSqlDigest(rewriteRecord.getSqlFingerprint());
        request.setRecommendedSqlText(rewriteRecord.getRecommendedSqlText());
        request.setDatasourceCode(rewriteRecord.getDatasourceCode());
        request.setPublishedBy(operator);
        return request;
    }

    private RuntimeRewriteBindingStateChangeRequest buildRuntimeStateChangeRequest(SqlRewriteRecord rewriteRecord,
                                                                                   String operator,
                                                                                   String reason) {
        RuntimeRewriteBindingStateChangeRequest request = new RuntimeRewriteBindingStateChangeRequest();
        request.setTenantId(rewriteRecord.getTenantId());
        request.setRuntimeBindingId(rewriteRecord.getRuntimeBindingId());
        request.setSqlFingerprint(rewriteRecord.getSqlFingerprint());
        request.setReason(reason);
        request.setOperatorId(operator);
        return request;
    }

    private void requirePublishedRuntimeResponse(SqlRewriteRecord rewriteRecord,
                                                 RuntimeRewriteBindingResponse response) {
        requireRuntimeStateResponse(rewriteRecord, response, "ACTIVE");
        if (!response.isActive()) {
            throw runtimeContractFailure("Runtime rewrite binding publish response is not active");
        }
        requireText(response.getRuntimeRuleVersion(), "runtimeRuleVersion");
    }

    private void requireRuntimeStateResponse(SqlRewriteRecord rewriteRecord,
                                             RuntimeRewriteBindingResponse response,
                                             String expectedStatus) {
        if (response == null) {
            throw runtimeContractFailure("Runtime rewrite binding response is missing");
        }
        if (!expectedStatus.equals(response.getStatus())) {
            throw runtimeContractFailure("Runtime rewrite binding response status must be " + expectedStatus);
        }
        requireText(response.getRuntimeBindingId(), "runtimeBindingId");
        if (!rewriteRecord.getTenantId().equals(response.getTenantId())) {
            throw runtimeContractFailure("Runtime rewrite binding tenantId does not match rewrite record");
        }
        if (!rewriteRecord.getRewriteRecordId().equals(response.getRewriteRecordId())) {
            throw runtimeContractFailure("Runtime rewrite binding rewriteRecordId does not match rewrite record");
        }
        if (!rewriteRecord.getSqlFingerprint().equals(response.getSqlFingerprint())) {
            throw runtimeContractFailure("Runtime rewrite binding sqlFingerprint does not match rewrite record");
        }
        if (StringUtils.hasText(rewriteRecord.getRuntimeBindingId())
            && !rewriteRecord.getRuntimeBindingId().equals(response.getRuntimeBindingId())) {
            throw runtimeContractFailure("Runtime rewrite binding id does not match rewrite record");
        }
    }

    private QueryExecutionRuntimeRewriteBindingClient requireRuntimeBindingClient() {
        if (queryExecutionRuntimeRewriteBindingClient != null) {
            return queryExecutionRuntimeRewriteBindingClient;
        }
        throw new BizException(
            ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "query-execution runtime rewrite binding client is not configured"
        );
    }

    private void requireSameActionTenant(SqlRewriteRecord rewriteRecord,
                                         SqlRewriteRecordPublishActionRequest request) {
        String tenantId = requireAuthorizedTenant(request == null ? null : request.getTenantId());
        if (!tenantId.equals(rewriteRecord.getTenantId())) {
            throw new AccessDeniedException("Authenticated tenant cannot mutate this rewrite record publish state");
        }
    }

    private void requirePublishedStatus(SqlRewriteRecord rewriteRecord, String action) {
        if (rewriteRecord.getPublishStatus() == RewritePublishStatus.PUBLISHED) {
            return;
        }
        throw new BizException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID,
            HttpStatus.CONFLICT,
            "Only PUBLISHED rewrite records can be " + action + "d"
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
            "Only PUBLISHED or PAUSED rewrite records can be unpublished"
        );
    }

    private BizException publishRejected(RewritePublishEligibility eligibility) {
        String message = "Rewrite record is not eligible for publish";
        List<RewritePublishEligibilityReason> reasons = eligibility.getRefusalReasons();
        if (!reasons.isEmpty()) {
            message = message + ": " + reasons.get(0).getCode();
        }
        return new BizException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID,
            HttpStatus.CONFLICT,
            message
        );
    }

    private BizException runtimeContractFailure(String message) {
        return new BizException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_REWRITE_FAILURE,
            HttpStatus.SERVICE_UNAVAILABLE,
            message
        );
    }

    private void savePublishFailureTrace(SqlRewriteRecord rewriteRecord,
                                         String operator,
                                         String reason,
                                         RuntimeException exception) {
        Instant now = Instant.now();
        try {
            sqlRewriteRecordRepository.saveRecord(rewriteRecord.withPublishFailed(
                operator,
                now,
                buildRuntimeBindingTraceRefs(
                    rewriteRecord,
                    "PUBLISH",
                    RewritePublishStatus.PUBLISH_FAILED.name(),
                    null,
                    reason,
                    operator,
                    now,
                    exception
                )
            ));
        } catch (RuntimeException saveException) {
            exception.addSuppressed(saveException);
        }
    }

    private void saveRuntimeFailureTrace(SqlRewriteRecord rewriteRecord,
                                         String action,
                                         String operator,
                                         String reason,
                                         RuntimeException exception) {
        Instant now = Instant.now();
        try {
            sqlRewriteRecordRepository.saveRecord(rewriteRecord.withTraceRefs(
                buildRuntimeBindingTraceRefs(
                    rewriteRecord,
                    action,
                    rewriteRecord.getPublishStatus().name(),
                    null,
                    reason,
                    operator,
                    now,
                    exception
                ),
                now
            ));
        } catch (RuntimeException saveException) {
            exception.addSuppressed(saveException);
        }
    }

    private void compensatePublishedRuntimeBinding(SqlRewriteRecord rewriteRecord,
                                                   RuntimeRewriteBindingResponse response,
                                                   String operator) {
        if (queryExecutionRuntimeRewriteBindingClient == null || response == null
            || !StringUtils.hasText(response.getRuntimeBindingId())) {
            return;
        }
        try {
            RuntimeRewriteBindingStateChangeRequest compensation = new RuntimeRewriteBindingStateChangeRequest();
            compensation.setTenantId(rewriteRecord.getTenantId());
            compensation.setRuntimeBindingId(response.getRuntimeBindingId());
            compensation.setSqlFingerprint(rewriteRecord.getSqlFingerprint());
            compensation.setOperatorId(operator);
            compensation.setReason("local rewrite record publish write-back failed");
            queryExecutionRuntimeRewriteBindingClient.unpublish(compensation);
        } catch (RuntimeException ignored) {
            // The original local write-back failure must remain the primary error.
        }
    }

    private Map<String, Object> buildRuntimeBindingTraceRefs(SqlRewriteRecord rewriteRecord,
                                                             String action,
                                                             String publishStatus,
                                                             RuntimeRewriteBindingResponse response,
                                                             String reason,
                                                             String operator,
                                                             Instant occurredAt,
                                                             RuntimeException exception) {
        Map<String, Object> traceRefs = new LinkedHashMap<String, Object>(rewriteRecord.getTraceRefs());
        Map<String, Object> runtimeTrace = new LinkedHashMap<String, Object>();
        runtimeTrace.put("action", action);
        runtimeTrace.put("publishStatus", publishStatus);
        runtimeTrace.put("operator", operator);
        runtimeTrace.put("occurredAt", occurredAt.toString());
        String trimmedReason = trimToNull(reason);
        if (trimmedReason != null) {
            runtimeTrace.put("reason", trimmedReason);
        }
        String requestId = trimToNull(RequestContext.getRequestId());
        if (requestId != null) {
            runtimeTrace.put("requestId", requestId);
        }
        String traceId = trimToNull(RequestContext.getTraceId());
        if (traceId != null) {
            runtimeTrace.put("traceId", traceId);
        }
        if (response != null) {
            runtimeTrace.put("runtimeBindingId", response.getRuntimeBindingId());
            runtimeTrace.put("runtimeRuleVersion", response.getRuntimeRuleVersion());
            runtimeTrace.put("runtimeStatus", response.getStatus());
            runtimeTrace.put("runtimeSummary", response.getRuntimeSummary());
        }
        if (exception != null) {
            runtimeTrace.put("errorType", exception.getClass().getSimpleName());
            runtimeTrace.put("errorMessage", exception.getMessage());
            runtimeTrace.put("retryable", Boolean.TRUE);
        }
        traceRefs.put("lastRuntimeBindingTrace", runtimeTrace);
        return traceRefs;
    }

    private String runtimeBindingScope(SqlRewriteRecord rewriteRecord) {
        return rewriteRecord.getTenantId() + ":" + rewriteRecord.getSqlFingerprint();
    }

    private String actionReason(SqlRewriteRecordPublishActionRequest request) {
        return request == null ? null : trimToNull(request.getReason());
    }

    private void requireText(String value, String field) {
        if (StringUtils.hasText(value)) {
            return;
        }
        throw invalidArgument(field, field + " is required");
    }

    private void validateReviewTransition(SqlRewriteRecord rewriteRecord,
                                          RewriteReviewStatus nextStatus) {
        if (nextStatus == null) {
            throw invalidArgument("reviewStatus", "reviewStatus is required");
        }
        if (rewriteRecord.canTransitionReviewTo(nextStatus)) {
            return;
        }
        throw new BizException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID,
            HttpStatus.CONFLICT,
            "Illegal rewrite review transition from " + rewriteRecord.getReviewStatus() + " to " + nextStatus
        );
    }

    private void requireReviewNote(RewriteReviewStatus nextStatus, String reviewNote) {
        if (nextStatus == RewriteReviewStatus.APPROVED || StringUtils.hasText(reviewNote)) {
            return;
        }
        throw invalidArgument("reviewNote", "reviewNote is required for " + nextStatus);
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
                "SQL rewrite record not found: " + rewriteRecordId
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
        vo.setDiffSummary(record.getDiffSummary());
        vo.setRisk(record.getRisk());
        vo.setTraceRefs(record.getTraceRefs());
        vo.setContractStage(CONTRACT_STAGE);
        vo.setImplementationStage(IMPLEMENTATION_STAGE);
        return vo;
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
            throw new AccessDeniedException("Request tenantId does not match authenticated tenant context");
        }
        return contextTenantId;
    }

    private String requireContextTenant() {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context"
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
                "userId is missing from authenticated request context"
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
            throw new AccessDeniedException("Authenticated tenant cannot access this rewrite record");
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
