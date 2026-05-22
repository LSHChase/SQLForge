package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingActivationRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingStateChangeRequest;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import com.company.sqloptimization.config.RewriteProductionGateProperties;
import com.company.sqloptimization.application.controller.dto.RewriteValidationRunCreateRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordCreateRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordActivationActionRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordReviewRequest;
import com.company.sqloptimization.application.controller.vo.RewriteActivationEligibilityReasonVO;
import com.company.sqloptimization.application.controller.vo.RewriteActivationEligibilityVO;
import com.company.sqloptimization.application.controller.vo.RewriteValidationRunVO;
import com.company.sqloptimization.application.controller.vo.SqlRewriteRecordVO;
import com.company.sqloptimization.domain.governance.ComparisonStatus;
import com.company.sqloptimization.domain.governance.DifferenceType;
import com.company.sqloptimization.domain.governance.RewriteActivationStatus;
import com.company.sqloptimization.domain.governance.RewriteReviewStatus;
import com.company.sqloptimization.domain.governance.ValidationRunStatus;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.repository.AccelerationRecommendationRepository;
import com.company.sqloptimization.domain.rewrite.RewriteValidationRun;
import com.company.sqloptimization.domain.rewrite.SqlRewriteRecord;
import com.company.sqloptimization.domain.rewrite.policy.RewriteActivationEligibility;
import com.company.sqloptimization.domain.rewrite.policy.RewriteActivationEligibilityPolicy;
import com.company.sqloptimization.domain.rewrite.policy.RewriteActivationEligibilityReason;
import com.company.sqloptimization.domain.rewrite.repository.SqlRewriteRecordRepository;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
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
    private static final String RESOURCE_TYPE_REWRITE_RECORD = "SQL_REWRITE_RECORD";
    private static final String CREATE_OPERATION = "SQL_REWRITE_RECORD_CREATE";
    private static final String LIST_OPERATION = "SQL_REWRITE_RECORD_LIST";
    private static final String QUERY_OPERATION = "SQL_REWRITE_RECORD_QUERY";
    private static final String REVIEW_OPERATION = "SQL_REWRITE_RECORD_REVIEW";
    private static final String ACTIVATE_OPERATION = "SQL_REWRITE_RECORD_ACTIVATE";
    private static final String PAUSE_OPERATION = "SQL_REWRITE_RECORD_PAUSE";
    private static final String VALIDATION_CREATE_OPERATION = "SQL_REWRITE_RECORD_VALIDATION_CREATE";
    private static final String VALIDATION_QUERY_OPERATION = "SQL_REWRITE_RECORD_VALIDATION_QUERY";

    private final SqlRewriteRecordRepository sqlRewriteRecordRepository;
    private final QueryExecutionResultDigestClient queryExecutionResultDigestClient;
    private final QueryExecutionRuntimeRewriteBindingClient runtimeRewriteBindingClient;
    private final GovernanceCapabilityClient governanceCapabilityClient;
    private final ResultDigestComparisonEngine resultDigestComparisonEngine;
    private final RewriteActivationEligibilityPolicy rewriteActivationEligibilityPolicy;
    private final AccelerationRecommendationRepository recommendationRepository;
    private final RewriteProductionGateProperties rewriteProductionGateProperties;

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository) {
        this(
            sqlRewriteRecordRepository,
            null,
            new ResultDigestComparisonEngine(),
            new RewriteActivationEligibilityPolicy(),
            null,
            null,
            null,
            new RewriteProductionGateProperties()
        );
    }

    @Autowired
    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              QueryExecutionResultDigestClient queryExecutionResultDigestClient,
                                              ResultDigestComparisonEngine resultDigestComparisonEngine,
                                              QueryExecutionRuntimeRewriteBindingClient runtimeRewriteBindingClient,
                                              GovernanceCapabilityClient governanceCapabilityClient,
                                              AccelerationRecommendationRepository recommendationRepository,
                                              RewriteProductionGateProperties rewriteProductionGateProperties) {
        this(
            sqlRewriteRecordRepository,
            queryExecutionResultDigestClient,
            resultDigestComparisonEngine,
            new RewriteActivationEligibilityPolicy(),
            runtimeRewriteBindingClient,
            governanceCapabilityClient,
            recommendationRepository,
            rewriteProductionGateProperties
        );
    }

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              QueryExecutionResultDigestClient queryExecutionResultDigestClient,
                                              ResultDigestComparisonEngine resultDigestComparisonEngine,
                                              QueryExecutionRuntimeRewriteBindingClient runtimeRewriteBindingClient) {
        this(
            sqlRewriteRecordRepository,
            queryExecutionResultDigestClient,
            resultDigestComparisonEngine,
            new RewriteActivationEligibilityPolicy(),
            runtimeRewriteBindingClient,
            null,
            null,
            new RewriteProductionGateProperties()
        );
    }

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              QueryExecutionResultDigestClient queryExecutionResultDigestClient,
                                              ResultDigestComparisonEngine resultDigestComparisonEngine) {
        this(
            sqlRewriteRecordRepository,
            queryExecutionResultDigestClient,
            resultDigestComparisonEngine,
            new RewriteActivationEligibilityPolicy(),
            null,
            null,
            null,
            new RewriteProductionGateProperties()
        );
    }

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              AccelerationRecommendationRepository recommendationRepository) {
        this(
            sqlRewriteRecordRepository,
            null,
            new ResultDigestComparisonEngine(),
            new RewriteActivationEligibilityPolicy(),
            null,
            null,
            recommendationRepository,
            new RewriteProductionGateProperties()
        );
    }

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              QueryExecutionResultDigestClient queryExecutionResultDigestClient,
                                              ResultDigestComparisonEngine resultDigestComparisonEngine,
                                              RewriteActivationEligibilityPolicy rewriteActivationEligibilityPolicy) {
        this(
            sqlRewriteRecordRepository,
            queryExecutionResultDigestClient,
            resultDigestComparisonEngine,
            rewriteActivationEligibilityPolicy,
            null,
            null,
            null,
            new RewriteProductionGateProperties()
        );
    }

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              QueryExecutionResultDigestClient queryExecutionResultDigestClient,
                                              ResultDigestComparisonEngine resultDigestComparisonEngine,
                                              RewriteActivationEligibilityPolicy rewriteActivationEligibilityPolicy,
                                              QueryExecutionRuntimeRewriteBindingClient runtimeRewriteBindingClient,
                                              GovernanceCapabilityClient governanceCapabilityClient,
                                              AccelerationRecommendationRepository recommendationRepository) {
        this(
            sqlRewriteRecordRepository,
            queryExecutionResultDigestClient,
            resultDigestComparisonEngine,
            rewriteActivationEligibilityPolicy,
            runtimeRewriteBindingClient,
            governanceCapabilityClient,
            recommendationRepository,
            new RewriteProductionGateProperties()
        );
    }

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              QueryExecutionResultDigestClient queryExecutionResultDigestClient,
                                              ResultDigestComparisonEngine resultDigestComparisonEngine,
                                              RewriteActivationEligibilityPolicy rewriteActivationEligibilityPolicy,
                                              QueryExecutionRuntimeRewriteBindingClient runtimeRewriteBindingClient,
                                              GovernanceCapabilityClient governanceCapabilityClient,
                                              AccelerationRecommendationRepository recommendationRepository,
                                              RewriteProductionGateProperties rewriteProductionGateProperties) {
        this.sqlRewriteRecordRepository = sqlRewriteRecordRepository;
        this.queryExecutionResultDigestClient = queryExecutionResultDigestClient;
        this.runtimeRewriteBindingClient = runtimeRewriteBindingClient;
        this.governanceCapabilityClient = governanceCapabilityClient;
        this.resultDigestComparisonEngine = resultDigestComparisonEngine;
        this.rewriteActivationEligibilityPolicy = rewriteActivationEligibilityPolicy;
        this.recommendationRepository = recommendationRepository;
        this.rewriteProductionGateProperties = rewriteProductionGateProperties == null
            ? new RewriteProductionGateProperties()
            : rewriteProductionGateProperties;
    }

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              QueryExecutionResultDigestClient queryExecutionResultDigestClient,
                                              ResultDigestComparisonEngine resultDigestComparisonEngine,
                                              RewriteActivationEligibilityPolicy rewriteActivationEligibilityPolicy,
                                              QueryExecutionRuntimeRewriteBindingClient runtimeRewriteBindingClient) {
        this(
            sqlRewriteRecordRepository,
            queryExecutionResultDigestClient,
            resultDigestComparisonEngine,
            rewriteActivationEligibilityPolicy,
            runtimeRewriteBindingClient,
            null,
            null,
            new RewriteProductionGateProperties()
        );
    }

    public SqlRewriteRecordApplicationService(SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                              QueryExecutionResultDigestClient queryExecutionResultDigestClient,
                                              ResultDigestComparisonEngine resultDigestComparisonEngine,
                                              QueryExecutionRuntimeRewriteBindingClient runtimeRewriteBindingClient,
                                              RewriteProductionGateProperties rewriteProductionGateProperties) {
        this(
            sqlRewriteRecordRepository,
            queryExecutionResultDigestClient,
            resultDigestComparisonEngine,
            new RewriteActivationEligibilityPolicy(),
            runtimeRewriteBindingClient,
            null,
            null,
            rewriteProductionGateProperties
        );
    }

    public SqlRewriteRecordVO createRewriteRecord(SqlRewriteRecordCreateRequest request) {
        String tenantId = requireAuthorizedTenant(request == null ? null : request.getTenantId());
        if (request == null) {
            throw invalidArgument("request", "rewrite record request 为必填项");
        }
        Instant now = Instant.now();
        Map<String, Object> traceRefs = buildCreateTraceRefs(request, tenantId);
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
            .activationStatus(request.getActivationStatus())
            .runtimeBindingId(trimToNull(request.getRuntimeBindingId()))
            .runtimeBindingAt(request.getRuntimeBindingAt())
            .runtimeBindingBy(trimToNull(request.getRuntimeBindingBy()))
            .runtimeBindingScope(trimToNull(request.getRuntimeBindingScope()))
            .activatedSqlFingerprint(trimToNull(request.getActivatedSqlFingerprint()))
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
            .traceRefs(traceRefs)
            .build();
        requireMvRuntimeRewriteSqlAligned(rewriteRecord);
        assertRewriteAuthorization(rewriteRecord, CREATE_OPERATION);
        return toRewriteRecordVo(sqlRewriteRecordRepository.saveRecord(rewriteRecord));
    }

    public List<SqlRewriteRecordVO> listRewriteRecords(String historyId,
                                                       String recommendationId,
                                                       String validationStatus,
                                                       String sourceType) {
        String tenantId = requireContextTenant();
        String normalizedHistoryId = trimToNull(historyId);
        assertRewriteAuthorization(tenantId, DataSourceTypeEnum.AUTO,
            normalizedHistoryId == null ? tenantId : normalizedHistoryId, LIST_OPERATION);
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
        assertRewriteAuthorization(rewriteRecord, QUERY_OPERATION);
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
        assertRewriteAuthorization(rewriteRecord, REVIEW_OPERATION);
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

    public RewriteActivationEligibilityVO getActivationEligibility(String rewriteRecordId) {
        SqlRewriteRecord rewriteRecord = requireRewriteRecord(rewriteRecordId);
        assertRewriteAuthorization(rewriteRecord, QUERY_OPERATION);
        return toActivationEligibilityVo(evaluateActivationEligibility(rewriteRecord));
    }

    public SqlRewriteRecordVO activateRewriteRecord(String rewriteRecordId,
                                                   SqlRewriteRecordActivationActionRequest request) {
        SqlRewriteRecord rewriteRecord = requireRewriteRecord(rewriteRecordId);
        requireSameActionTenant(rewriteRecord, request);
        assertRewriteAuthorization(rewriteRecord, ACTIVATE_OPERATION);
        requireDomainState(new Runnable() {
            @Override
            public void run() {
                rewriteRecord.requireActivatableRuntimeState();
            }
        });
        requireMvRuntimeRewriteSqlAligned(rewriteRecord);
        RewriteActivationEligibility eligibility = evaluateActivationEligibility(rewriteRecord);
        boolean developmentDirectActivation = shouldUseDevelopmentDirectActivation(eligibility);
        if (!developmentDirectActivation) {
            requireEligibleForRuntimeActivation(eligibility);
        }
        String operator = requireContextUser();
        String reason = actionReason(request);
        Instant now = Instant.now();
        RuntimeRewriteBindingResponse runtimeResponse;
        try {
            runtimeResponse = requireRuntimeRewriteBindingClient().activate(buildRuntimeActivationRequest(rewriteRecord, operator));
        } catch (RuntimeException ex) {
            SqlRewriteRecord failed = rewriteRecord.withActivateFailed(
                operator,
                now,
                buildRuntimeBindingTraceRefs(
                    rewriteRecord,
                    "ACTIVATE",
                    RewriteActivationStatus.ACTIVATE_FAILED,
                    reason,
                    operator,
                    now,
                    null,
                    ex,
                    developmentDirectActivation,
                    eligibility
                )
            );
            sqlRewriteRecordRepository.saveRecord(failed);
            throw ex;
        }
        requireRuntimeActivationActive(runtimeResponse);
        SqlRewriteRecord activated = rewriteRecord.withActivatedRuntimeBinding(
            runtimeResponse.getRuntimeBindingId(),
            runtimeResponse.getRuntimeRuleVersion(),
            runtimeResponse.getSqlFingerprint(),
            runtimeBindingScope(runtimeResponse),
            operator,
            now,
            buildRuntimeBindingTraceRefs(
                rewriteRecord,
                "ACTIVATE",
                RewriteActivationStatus.ACTIVE,
                reason,
                operator,
                now,
                runtimeResponse,
                null,
                developmentDirectActivation,
                eligibility
            )
        );
        return toRewriteRecordVo(sqlRewriteRecordRepository.saveRecord(activated));
    }

    public SqlRewriteRecordVO pauseRewriteRecord(String rewriteRecordId,
                                                 SqlRewriteRecordActivationActionRequest request) {
        SqlRewriteRecord rewriteRecord = requireRewriteRecord(rewriteRecordId);
        requireSameActionTenant(rewriteRecord, request);
        assertRewriteAuthorization(rewriteRecord, PAUSE_OPERATION);
        requireDomainState(new Runnable() {
            @Override
            public void run() {
                rewriteRecord.requirePauseableRuntimeState();
            }
        });
        String operator = requireContextUser();
        String reason = actionReason(request);
        Instant now = Instant.now();
        RuntimeRewriteBindingResponse runtimeResponse;
        try {
            runtimeResponse =
                requireRuntimeRewriteBindingClient().pause(buildRuntimeStateChangeRequest(rewriteRecord, operator, reason));
        } catch (RuntimeException ex) {
            SqlRewriteRecord failed = rewriteRecord.withPauseFailed(
                operator,
                now,
                buildRuntimeBindingTraceRefs(
                    rewriteRecord,
                    "PAUSE",
                    RewriteActivationStatus.PAUSE_FAILED,
                    reason,
                    operator,
                    now,
                    null,
                    ex,
                    false,
                    null
                )
            );
            sqlRewriteRecordRepository.saveRecord(failed);
            throw ex;
        }
        requireRuntimeState(runtimeResponse, "PAUSED");
        SqlRewriteRecord paused = rewriteRecord.withPausedRuntimeBinding(
            operator,
            now,
            buildRuntimeBindingTraceRefs(
                rewriteRecord,
                "PAUSE",
                RewriteActivationStatus.PAUSED,
                reason,
                operator,
                now,
                runtimeResponse,
                null,
                false,
                null
            )
        );
        return toRewriteRecordVo(sqlRewriteRecordRepository.saveRecord(paused));
    }

    private Map<String, Object> buildRuntimeBindingTraceRefs(SqlRewriteRecord rewriteRecord,
                                                             String action,
                                                             RewriteActivationStatus activationStatus,
                                                             String reason,
                                                             String operator,
                                                             Instant occurredAt,
                                                             RuntimeRewriteBindingResponse runtimeResponse,
                                                             RuntimeException failure,
                                                             boolean developmentDirectActivation,
                                                             RewriteActivationEligibility eligibility) {
        Map<String, Object> traceRefs = new LinkedHashMap<String, Object>(rewriteRecord.getTraceRefs());
        Map<String, Object> lifecycleEvidence = new LinkedHashMap<String, Object>();
        lifecycleEvidence.put("action", action);
        lifecycleEvidence.put("activationStatus", activationStatus.name());
        lifecycleEvidence.put("operator", operator);
        lifecycleEvidence.put("occurredAt", occurredAt.toString());
        lifecycleEvidence.put("runtimeBinding", Boolean.TRUE);
        if (action != null && action.contains("ACTIVATE")) {
            lifecycleEvidence.put("developmentDirectActivation", Boolean.valueOf(developmentDirectActivation));
            lifecycleEvidence.put(
                "validationGate",
                developmentDirectActivation ? "BYPASSED_FOR_DEVELOPMENT_DEBUG" : "PASSED_PRODUCTION_ELIGIBILITY"
            );
            if (eligibility != null) {
                lifecycleEvidence.put("eligibilityPolicyId", eligibility.getPolicyId());
                lifecycleEvidence.put("eligibilityRefusalCodes", eligibilityRefusalCodes(eligibility));
            }
        }
        if (runtimeResponse != null) {
            lifecycleEvidence.put("runtimeBindingId", runtimeResponse.getRuntimeBindingId());
            lifecycleEvidence.put("runtimeStatus", runtimeResponse.getStatus());
            lifecycleEvidence.put("runtimeActive", Boolean.valueOf(runtimeResponse.isActive()));
            lifecycleEvidence.put("runtimeRuleVersion", runtimeResponse.getRuntimeRuleVersion());
            lifecycleEvidence.put("runtimeSummary", runtimeResponse.getRuntimeSummary());
            lifecycleEvidence.put("runtimeDetails", parseJsonString(runtimeResponse.getRuntimeDetailsJson()));
        }
        if (failure != null) {
            lifecycleEvidence.put("failureType", failure.getClass().getSimpleName());
            lifecycleEvidence.put("failureReason", failure.getMessage());
        }
        String trimmedReason = trimToNull(reason);
        if (trimmedReason != null) {
            lifecycleEvidence.put("reason", trimmedReason);
        }
        String requestId = trimToNull(RequestContext.getRequestId());
        if (requestId != null) {
            lifecycleEvidence.put("requestId", requestId);
        }
        String traceId = trimToNull(RequestContext.getTraceId());
        if (traceId != null) {
            lifecycleEvidence.put("traceId", traceId);
        }
        if (action != null && action.contains("PAUSE")) {
            traceRefs.put("pauseEvidence", lifecycleEvidence);
        } else {
            traceRefs.put("activationEvidence", lifecycleEvidence);
        }
        traceRefs.put("lastActivationStatusTrace", lifecycleEvidence);
        return traceRefs;
    }

    private RewriteActivationEligibility evaluateActivationEligibility(SqlRewriteRecord rewriteRecord) {
        List<RewriteValidationRun> runs =
            sqlRewriteRecordRepository.findValidationRunsByRewriteRecordId(rewriteRecord.getRewriteRecordId());
        List<RewriteValidationRun> tenantRuns = new ArrayList<RewriteValidationRun>();
        for (RewriteValidationRun run : runs) {
            if (rewriteRecord.getTenantId().equals(run.getTenantId())) {
                tenantRuns.add(run);
            }
        }
        return rewriteActivationEligibilityPolicy.evaluate(rewriteRecord, tenantRuns);
    }

    public RewriteValidationRunVO createValidationRun(String rewriteRecordId,
                                                      RewriteValidationRunCreateRequest request) {
        SqlRewriteRecord rewriteRecord = requireRewriteRecord(rewriteRecordId);
        String tenantId = requireAuthorizedTenant(request == null ? null : request.getTenantId());
        if (!tenantId.equals(rewriteRecord.getTenantId())) {
            throw new AccessDeniedException("当前认证租户无权为该改写记录创建校验运行");
        }
        assertRewriteAuthorization(rewriteRecord, VALIDATION_CREATE_OPERATION);
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
        RuntimeRewriteBindingResponse runtimeResponse;
        try {
            runtimeResponse =
                requireRuntimeRewriteBindingClient().pause(buildRuntimeStateChangeRequest(rewriteRecord, operator, reason));
            requireRuntimeState(runtimeResponse, "PAUSED");
        } catch (RuntimeException ex) {
            return rewriteRecord.withTraceRefs(
                buildRuntimeBindingTraceRefs(
                    rewriteRecord,
                    "AUTO_PAUSE",
                    rewriteRecord.getActivationStatus(),
                    reason,
                    operator,
                    now,
                    null,
                    ex,
                    false,
                    null
                ),
                now
            );
        }
        return rewriteRecord.withPausedRuntimeBinding(
            operator,
            now,
            buildRuntimeBindingTraceRefs(
                rewriteRecord,
                "AUTO_PAUSE",
                RewriteActivationStatus.PAUSED,
                reason,
                operator,
                now,
                runtimeResponse,
                null,
                false,
                null
            )
        );
    }

    private boolean shouldAutoPauseRuntimeBinding(SqlRewriteRecord rewriteRecord,
                                                  RewriteValidationRun validationRun) {
        return validationRun != null
            && validationRun.getComparisonStatus() == ComparisonStatus.DIVERGED
            && validationRun.isAutoApplyPaused()
            && rewriteRecord.getActivationStatus() == RewriteActivationStatus.ACTIVE
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

    private RuntimeRewriteBindingActivationRequest buildRuntimeActivationRequest(SqlRewriteRecord rewriteRecord,
                                                                           String operator) {
        RuntimeRewriteBindingActivationRequest request = new RuntimeRewriteBindingActivationRequest();
        request.setTenantId(rewriteRecord.getTenantId());
        request.setRewriteRecordId(rewriteRecord.getRewriteRecordId());
        request.setRecommendationId(rewriteRecord.getRecommendationId());
        request.setSourceType(name(rewriteRecord.getSourceType()));
        request.setSourceKind(name(rewriteRecord.getSourceKind()));
        request.setSourceId(rewriteRecord.getSourceId());
        request.setSqlFingerprint(firstText(
            rewriteRecord.getSqlFingerprint(),
            SqlFingerprintUtils.fingerprint(rewriteRecord.getOriginalSqlText())
        ));
        request.setOriginalSqlDigest(SqlFingerprintUtils.fingerprint(rewriteRecord.getOriginalSqlText()));
        request.setRecommendedSqlText(rewriteRecord.getRecommendedSqlText());
        request.setDatasourceCode(firstText(rewriteRecord.getDatasourceCode(), "hetu_main"));
        request.setActivatedBy(operator);
        return request;
    }

    private RuntimeRewriteBindingStateChangeRequest buildRuntimeStateChangeRequest(SqlRewriteRecord rewriteRecord,
                                                                                   String operator,
                                                                                   String reason) {
        RuntimeRewriteBindingStateChangeRequest request = new RuntimeRewriteBindingStateChangeRequest();
        request.setTenantId(rewriteRecord.getTenantId());
        request.setRuntimeBindingId(rewriteRecord.getRuntimeBindingId());
        request.setSqlFingerprint(rewriteRecord.getSqlFingerprint());
        request.setOperatorId(operator);
        request.setReason(reason);
        return request;
    }

    private QueryExecutionRuntimeRewriteBindingClient requireRuntimeRewriteBindingClient() {
        if (runtimeRewriteBindingClient != null) {
            return runtimeRewriteBindingClient;
        }
        throw new BizException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_REWRITE_FAILURE,
            HttpStatus.SERVICE_UNAVAILABLE,
            "query-execution runtime rewrite binding client 未配置"
        );
    }

    private void requireRuntimeActivationActive(RuntimeRewriteBindingResponse response) {
        if (response != null
            && response.isActive()
            && "ACTIVE".equals(response.getStatus())
            && StringUtils.hasText(response.getRuntimeBindingId())
            && StringUtils.hasText(response.getRuntimeRuleVersion())) {
            return;
        }
        throw new BizException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_REWRITE_FAILURE,
            HttpStatus.CONFLICT,
            "query-execution 未返回 ACTIVE 的运行时改写绑定"
        );
    }

    private void requireRuntimeState(RuntimeRewriteBindingResponse response, String expectedStatus) {
        if (response != null && expectedStatus.equals(response.getStatus())) {
            return;
        }
        throw new BizException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_REWRITE_FAILURE,
            HttpStatus.CONFLICT,
            "query-execution 未返回 " + expectedStatus + " 的运行时改写绑定状态"
        );
    }

    private String runtimeBindingScope(RuntimeRewriteBindingResponse response) {
        return response.getTenantId() + ":" + response.getSqlFingerprint();
    }

    private void requireEligibleForRuntimeActivation(RewriteActivationEligibility eligibility) {
        if (eligibility != null && eligibility.isEligible()) {
            return;
        }
        throw new BizException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID,
            HttpStatus.CONFLICT,
            "改写记录不满足激活资格：" + summarizeRefusalReasons(eligibility)
        );
    }

    private boolean shouldUseDevelopmentDirectActivation(RewriteActivationEligibility eligibility) {
        return rewriteProductionGateProperties.isDevelopmentDirectActivationEnabled()
            && (eligibility == null || !eligibility.isEligible());
    }

    private String summarizeRefusalReasons(RewriteActivationEligibility eligibility) {
        if (eligibility == null || eligibility.getRefusalReasons().isEmpty()) {
            return "UNKNOWN";
        }
        List<String> codes = new ArrayList<String>();
        for (RewriteActivationEligibilityReason reason : eligibility.getRefusalReasons()) {
            codes.add(reason.getCode());
        }
        return codes.toString();
    }

    private List<String> eligibilityRefusalCodes(RewriteActivationEligibility eligibility) {
        if (eligibility == null || eligibility.getRefusalReasons().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> codes = new ArrayList<String>();
        for (RewriteActivationEligibilityReason reason : eligibility.getRefusalReasons()) {
            codes.add(reason.getCode());
        }
        return codes;
    }

    private void requireDomainState(Runnable stateCheck) {
        try {
            stateCheck.run();
        } catch (IllegalStateException ex) {
            throw new BizException(
                ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID,
                HttpStatus.CONFLICT,
                ex.getMessage()
            );
        }
    }

    private void assertRewriteAuthorization(SqlRewriteRecord rewriteRecord, String operationCode) {
        assertRewriteAuthorization(
            rewriteRecord.getTenantId(),
            resolveAuthorizationDatasourceType(rewriteRecord),
            rewriteRecord.getRewriteRecordId(),
            operationCode
        );
    }

    private void assertRewriteAuthorization(String tenantId,
                                            DataSourceTypeEnum datasourceType,
                                            String resourceId,
                                            String operationCode) {
        if (governanceCapabilityClient == null) {
            return;
        }
        governanceCapabilityClient.assertAuthorization(
            tenantId,
            datasourceType,
            RESOURCE_TYPE_REWRITE_RECORD,
            resourceId,
            operationCode
        );
    }

    private DataSourceTypeEnum resolveAuthorizationDatasourceType(SqlRewriteRecord rewriteRecord) {
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

    private Object parseJsonString(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return JsonUtils.fromJson(json, Map.class);
        } catch (RuntimeException ex) {
            return json;
        }
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
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
        assertRewriteAuthorization(rewriteRecord, VALIDATION_QUERY_OPERATION);
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
                                         SqlRewriteRecordActivationActionRequest request) {
        String tenantId = requireAuthorizedTenant(request == null ? null : request.getTenantId());
        if (!tenantId.equals(rewriteRecord.getTenantId())) {
            throw new AccessDeniedException("当前认证租户无权变更该改写记录的激活状态");
        }
    }

    private String actionReason(SqlRewriteRecordActivationActionRequest request) {
        return request == null ? null : trimToNull(request.getReason());
    }

    private Map<String, Object> buildCreateTraceRefs(SqlRewriteRecordCreateRequest request,
                                                     String tenantId) {
        Map<String, Object> traceRefs = enrichRewriteTrialTraceRefs(
            request.getTraceRefs(),
            request.getSourceProblems(),
            request.getIssueRuleLinks()
        );
        Map<String, Object> storedArtifact = storedGeneratedMvArtifact(request.getRecommendationId(), tenantId);
        if (storedArtifact == null) {
            return traceRefs;
        }
        requireRecommendedSqlTextMatchesArtifact(request.getRecommendedSqlText(), storedArtifact);
        return normalizeMvArtifactTraceRefs(traceRefs, storedArtifact, request.getRecommendationId());
    }

    private Map<String, Object> storedGeneratedMvArtifact(String recommendationId,
                                                          String tenantId) {
        if (recommendationRepository == null || !StringUtils.hasText(recommendationId)) {
            return null;
        }
        AccelerationRecommendation recommendation =
            recommendationRepository.findByRecommendationId(recommendationId.trim());
        if (recommendation == null) {
            return null;
        }
        if (!tenantId.equals(recommendation.getTenantId())) {
            throw new AccessDeniedException("当前认证租户无权使用该推荐生成改写记录");
        }
        Map<String, Object> sanitized =
            AccelerationArtifactSnapshotSanitizer.sanitize(recommendation.getAccelerationArtifact());
        if (sanitized == null
            || !isGeneratedMvArtifact(sanitized)
            || hasBlockingReasons(sanitized.get("blockingReasons"))) {
            return null;
        }
        return sanitized;
    }

    private void requireRecommendedSqlTextMatchesArtifact(String recommendedSqlText,
                                                          Map<String, Object> artifact) {
        String artifactRewriteSql = textValue(artifact.get("rewriteSql"));
        if (normalizeRuntimeSql(artifactRewriteSql).equals(normalizeRuntimeSql(recommendedSqlText))) {
            return;
        }
        throw invalidArgument(
            "recommendedSqlText",
            "PRECOMPUTE_MV 推荐必须以后端落库 accelerationArtifact.rewriteSql 作为 recommendedSqlText"
        );
    }

    private Map<String, Object> normalizeMvArtifactTraceRefs(Map<String, Object> traceRefs,
                                                             Map<String, Object> storedArtifact,
                                                             String recommendationId) {
        Map<String, Object> result = traceRefs == null
            ? new LinkedHashMap<String, Object>()
            : new LinkedHashMap<String, Object>(traceRefs);
        requireTraceTextIfPresent(result, "mvType", storedArtifact.get("mvType"), "traceRefs.mvType");
        requireTraceTextIfPresent(result, "mvName", storedArtifact.get("mvName"), "traceRefs.mvName");

        Map<String, Object> traceArtifact = asMap(result.get("accelerationArtifact"));
        requireTraceArtifactIfPresent(traceArtifact, storedArtifact, "traceRefs.accelerationArtifact");

        Map<String, Object> runtimeRewriteEvidence = asMap(result.get("runtimeRewriteEvidence"));
        Map<String, Object> runtimeTraceArtifact =
            asMap(runtimeRewriteEvidence == null ? null : runtimeRewriteEvidence.get("accelerationArtifact"));
        requireTraceArtifactIfPresent(
            runtimeTraceArtifact,
            storedArtifact,
            "traceRefs.runtimeRewriteEvidence.accelerationArtifact"
        );

        String mvType = textValue(storedArtifact.get("mvType"));
        if (StringUtils.hasText(mvType)) {
            result.put("mvType", mvType);
        }
        String mvName = textValue(storedArtifact.get("mvName"));
        if (StringUtils.hasText(mvName)) {
            result.put("mvName", mvName);
        }
        result.put("accelerationArtifact", mvArtifactTraceSummary(storedArtifact, recommendationId));
        return result;
    }

    private void requireTraceArtifactIfPresent(Map<String, Object> traceArtifact,
                                               Map<String, Object> storedArtifact,
                                               String fieldPrefix) {
        if (traceArtifact == null || traceArtifact.isEmpty()) {
            return;
        }
        requireTraceTextIfPresent(traceArtifact, "rule", storedArtifact.get("rule"), fieldPrefix + ".rule");
        requireTraceTextIfPresent(
            traceArtifact,
            "artifactStatus",
            storedArtifact.get("artifactStatus"),
            fieldPrefix + ".artifactStatus"
        );
        requireTraceTextIfPresent(traceArtifact, "mvType", storedArtifact.get("mvType"), fieldPrefix + ".mvType");
        requireTraceTextIfPresent(traceArtifact, "mvName", storedArtifact.get("mvName"), fieldPrefix + ".mvName");
        requireTraceTextIfPresent(
            traceArtifact,
            "targetDatasource",
            storedArtifact.get("targetDatasource"),
            fieldPrefix + ".targetDatasource"
        );
        requireTraceSqlIfPresent(
            traceArtifact,
            "rewriteSql",
            storedArtifact.get("rewriteSql"),
            fieldPrefix + ".rewriteSql"
        );
    }

    private void requireTraceTextIfPresent(Map<String, Object> traceRefs,
                                           String key,
                                           Object expected,
                                           String field) {
        Object actual = traceRefs == null ? null : traceRefs.get(key);
        if (!StringUtils.hasText(textValue(actual)) || !StringUtils.hasText(textValue(expected))) {
            return;
        }
        if (textValue(expected).equals(textValue(actual))) {
            return;
        }
        throw invalidArgument(field, field + " 与后端落库 PRECOMPUTE_MV 产物不一致");
    }

    private void requireTraceSqlIfPresent(Map<String, Object> traceRefs,
                                          String key,
                                          Object expected,
                                          String field) {
        Object actual = traceRefs == null ? null : traceRefs.get(key);
        if (!StringUtils.hasText(textValue(actual)) || !StringUtils.hasText(textValue(expected))) {
            return;
        }
        if (normalizeRuntimeSql(textValue(expected)).equals(normalizeRuntimeSql(textValue(actual)))) {
            return;
        }
        throw invalidArgument(field, field + " 与后端落库 PRECOMPUTE_MV 产物不一致");
    }

    private Map<String, Object> mvArtifactTraceSummary(Map<String, Object> storedArtifact,
                                                       String recommendationId) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        putText(summary, "rule", storedArtifact.get("rule"));
        putText(summary, "artifactStatus", storedArtifact.get("artifactStatus"));
        putText(summary, "mvType", storedArtifact.get("mvType"));
        putText(summary, "mvName", storedArtifact.get("mvName"));
        putText(summary, "targetDatasource", storedArtifact.get("targetDatasource"));
        putText(summary, "governanceBoundary", storedArtifact.get("governanceBoundary"));
        putText(summary, "rewriteSql", storedArtifact.get("rewriteSql"));
        putText(summary, "recommendationId", recommendationId);
        summary.put("artifactAuthority", "ACCELERATION_RECOMMENDATION_SNAPSHOT");
        return summary;
    }

    private void putText(Map<String, Object> target, String key, Object value) {
        String text = textValue(value);
        if (StringUtils.hasText(text)) {
            target.put(key, text);
        }
    }

    private void requireMvRuntimeRewriteSqlAligned(SqlRewriteRecord rewriteRecord) {
        String artifactRewriteSql = generatedMvArtifactRewriteSql(rewriteRecord.getTraceRefs());
        if (!StringUtils.hasText(artifactRewriteSql)) {
            return;
        }
        if (normalizeRuntimeSql(artifactRewriteSql)
            .equals(normalizeRuntimeSql(rewriteRecord.getRecommendedSqlText()))) {
            return;
        }
        throw invalidArgument(
            "recommendedSqlText",
            "PRECOMPUTE_MV 产物必须把 accelerationArtifact.rewriteSql 写入改写记录 recommendedSqlText"
        );
    }

    private String generatedMvArtifactRewriteSql(Map<String, Object> traceRefs) {
        Map<String, Object> artifact = asMap(traceRefs == null ? null : traceRefs.get("accelerationArtifact"));
        if (artifact == null) {
            Map<String, Object> runtimeRewrite = asMap(traceRefs == null ? null : traceRefs.get("runtimeRewriteEvidence"));
            artifact = asMap(runtimeRewrite == null ? null : runtimeRewrite.get("accelerationArtifact"));
        }
        if (artifact == null || !isGeneratedMvArtifact(artifact)) {
            return null;
        }
        if (hasBlockingReasons(artifact.get("blockingReasons"))) {
            return null;
        }
        return textValue(artifact.get("rewriteSql"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private boolean isGeneratedMvArtifact(Map<String, Object> artifact) {
        return "PRECOMPUTE_MV".equals(String.valueOf(artifact.get("rule")))
            && "GENERATED".equals(String.valueOf(artifact.get("artifactStatus")))
            && StringUtils.hasText(textValue(artifact.get("rewriteSql")));
    }

    private boolean hasBlockingReasons(Object value) {
        if (value instanceof List) {
            return !((List<?>) value).isEmpty();
        }
        return value != null && StringUtils.hasText(String.valueOf(value));
    }

    private String textValue(Object value) {
        return value == null ? null : trimToNull(String.valueOf(value));
    }

    private String normalizeRuntimeSql(String sql) {
        String normalized = sql == null ? "" : sql.trim();
        while (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
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
        vo.setActivationStatus(record.getActivationStatus().name());
        vo.setRuntimeBindingId(record.getRuntimeBindingId());
        vo.setRuntimeBindingAt(record.getRuntimeBindingAt());
        vo.setRuntimeBindingBy(record.getRuntimeBindingBy());
        vo.setRuntimeBindingScope(record.getRuntimeBindingScope());
        vo.setActivatedSqlFingerprint(record.getActivatedSqlFingerprint());
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

    private RewriteActivationEligibilityVO toActivationEligibilityVo(RewriteActivationEligibility eligibility) {
        RewriteActivationEligibilityVO vo = new RewriteActivationEligibilityVO();
        vo.setRewriteRecordId(eligibility.getRewriteRecordId());
        vo.setTenantId(eligibility.getTenantId());
        vo.setPolicyId(eligibility.getPolicyId());
        vo.setEligible(Boolean.valueOf(eligibility.isEligible()));
        vo.setReviewStatus(eligibility.getReviewStatus());
        vo.setValidationStatus(eligibility.getValidationStatus());
        vo.setActivationStatus(eligibility.getActivationStatus());
        vo.setAlertStatus(eligibility.getAlertStatus());
        vo.setAutoApplyAllowed(eligibility.getAutoApplyAllowed());
        vo.setLastValidationRunId(eligibility.getLastValidationRunId());
        List<RewriteActivationEligibilityReasonVO> reasons =
            new ArrayList<RewriteActivationEligibilityReasonVO>(eligibility.getRefusalReasons().size());
        for (RewriteActivationEligibilityReason reason : eligibility.getRefusalReasons()) {
            RewriteActivationEligibilityReasonVO reasonVo = new RewriteActivationEligibilityReasonVO();
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

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }
}
