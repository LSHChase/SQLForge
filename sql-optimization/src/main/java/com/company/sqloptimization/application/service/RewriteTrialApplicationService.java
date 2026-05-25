package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskContextDTO;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.application.controller.dto.RewriteTrialBatchRequest;
import com.company.sqloptimization.application.controller.dto.RewriteTrialRequest;
import com.company.sqloptimization.application.controller.vo.RewriteTrialItemVO;
import com.company.sqloptimization.application.controller.vo.RewriteTrialOverviewVO;
import com.company.sqloptimization.application.controller.vo.RewriteTrialRunVO;
import com.company.sqloptimization.application.controller.vo.RewriteTrialSourceIssueStatisticVO;
import com.company.sqloptimization.domain.batch.ParseBatch;
import com.company.sqloptimization.domain.batch.ParseBatchItem;
import com.company.sqloptimization.domain.batch.repository.ParseBatchItemRepository;
import com.company.sqloptimization.domain.batch.repository.ParseBatchRepository;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.BenefitLevel;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationStatus;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationType;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RiskLevel;
import com.company.sqloptimization.domain.recommendation.repository.AccelerationRecommendationRepository;
import com.company.sqloptimization.domain.task.OptimizationParseDepth;
import com.company.sqloptimization.domain.task.OptimizationTaskArtifact;
import com.company.sqloptimization.domain.task.OptimizationTaskPriority;
import com.company.sqloptimization.domain.task.OptimizationTaskSuggestion;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import com.company.sqloptimization.domain.trial.RewriteTrialItem;
import com.company.sqloptimization.domain.trial.RewriteTrialRun;
import com.company.sqloptimization.domain.trial.RewriteTrialStatus;
import com.company.sqloptimization.domain.trial.repository.RewriteTrialRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RewriteTrialApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RewriteTrialApplicationService.class);

    private static final int DEFAULT_BATCH_MAX_ITEMS = 500;
    private static final int HARD_BATCH_MAX_ITEMS = 2000;
    private static final String SOURCE_KIND_STRUCTURE_PARSE = "STRUCTURE_PARSE";
    private static final String SOURCE_KIND_PARSE_BATCH = "PARSE_BATCH";
    private static final Set<String> SAFE_REWRITE_PROBLEMS = Collections.unmodifiableSet(
        new LinkedHashSet<String>(Arrays.asList(
            "COUNT_LITERAL_TO_COUNT_STAR",
            "DEDUPLICATE_WHERE_PREDICATES",
            "DEDUPLICATE_HAVING_PREDICATES",
            "DEDUPLICATE_GROUP_BY_KEYS",
            "DEDUPLICATE_ORDER_BY_KEYS"
        ))
    );
    private static final Set<String> CANDIDATE_REWRITE_PROBLEMS = candidateRewriteProblems();
    private static final Set<String> MANUAL_REVIEW_PROBLEMS = Collections.unmodifiableSet(
        new LinkedHashSet<String>(Arrays.asList(
            "SELECT_STAR",
            "OR_PREDICATE_INDEX_RISK",
            "NESTED_SUBQUERY_RISK",
            "LEADING_WILDCARD_LIKE_RISK",
            "FUNCTION_WRAPPED_PREDICATE",
            "SCALAR_SUBQUERY_IN_SELECT",
            "CORRELATED_SUBQUERY_RISK",
            "NOT_EXISTS_ANTI_JOIN_RISK",
            "ORDER_BY_RANDOM_RISK",
            "REPEATED_TABLE_SCAN_RISK",
            "ORDER_BY_COMPLEXITY_RISK",
            "JOIN_LATENCY_RISK",
            "AGGREGATION_COMPLEXITY_RISK",
            "GROUP_BY_WITHOUT_AGGREGATE_RISK",
            "DUPLICATE_GROUP_OR_ORDER_KEY_RISK",
            "REPEATED_SUBQUERY_RISK",
            "LARGE_STRING_RESULT_RISK",
            "COMPLEX_QUERY_GRAPH_RISK",
            "SELECT_STAR_EXPANSION",
            "OR_TO_UNION_ALL",
            "FUNCTION_PREDICATE_TO_RANGE",
            "SCALAR_SUBQUERY_TO_JOIN",
            "REPEATED_SUBQUERY_TO_CTE",
            "NOT_EXISTS_TO_ANTI_JOIN",
            "ORDER_RANDOM_REVIEW",
            "PRECOMPUTE_MV",
            "PARTITION_PRUNING",
            "BUCKET_JOIN",
            "DISTINCT_DEDUP_REVIEW",
            "GROUP_BY_TO_DISTINCT",
            "HAVING_TO_WHERE_PUSHDOWN",
            "IN_SUBQUERY_TO_SEMI_JOIN",
            "EXISTS_TO_SEMI_JOIN",
            "NOT_IN_TO_ANTI_JOIN",
            "LEFT_JOIN_NULL_TO_ANTI_JOIN",
            "CROSS_JOIN_GUARD",
            "CAST_JOIN_KEY_NORMALIZE",
            "IMPLICIT_TYPE_CAST_REVIEW",
            "LIKE_PREFIX_RANGE_REVIEW",
            "REGEXP_FILTER_TO_SEARCH_INDEX",
            "LONG_IN_LIST_TO_TEMP_TABLE",
            "WINDOW_TOPN_REWRITE",
            "UNION_DEDUP_REVIEW",
            "INTERSECT_TO_SEMI_JOIN",
            "EXCEPT_TO_ANTI_JOIN",
            "JSON_EXTRACT_MATERIALIZATION",
            "UNNEST_LATERAL_REVIEW",
            "NULL_SAFE_EQUALITY_REVIEW",
            "OFFSET_TO_KEYSET_PAGINATION",
            "JOIN_REORDER_BY_STATS",
            "DYNAMIC_FILTERING_JOIN",
            "STAR_SCHEMA_MV",
            "SPLIT_SQL",
            "RESULT_CACHE",
            "REPORT_SQL_MERGE",
            "STATISTICS_REFRESH",
            "FILE_COMPACTION",
            "PROJECTION_PRUNING",
            "PREDICATE_PUSHDOWN",
            "TOPN_PUSHDOWN",
            "SMALL_TABLE_BROADCAST_JOIN",
            "SKEW_JOIN_SALTING_REVIEW",
            "PIVOT_AGGREGATE_PRECOMPUTE",
            "DATE_GRANULARITY_MV",
            "PARTITION_COMPENSATION_UNION",
            "SEMISTRUCTURED_COLUMN_INDEX",
            "FULL_SCAN_FILTER_GUARD",
            "ORDER_BY_WITHOUT_LIMIT_GUARD",
            "LIMIT_WITHOUT_ORDER_GUARD",
            "REPEATED_EXPRESSION_TO_CTE",
            "UDF_EVALUATION_ISOLATION",
            "STRING_CONCAT_PRECOMPUTE",
            "LARGE_STRING_AGGREGATE_OFFLOAD",
            "WINDOW_FRAME_PRECOMPUTE",
            "CTE_MATERIALIZATION_POLICY",
            "CASE_EXPRESSION_NORMALIZATION",
            "MULTI_COUNT_DISTINCT_DECOMPOSITION",
            "NEGATION_FILTER_REVIEW",
            "NULL_FILTER_INDEX_REVIEW",
            "ORDER_BY_EXPRESSION_PRECOMPUTE",
            "ARRAY_CONTAINS_INDEX_REVIEW",
            "RANGE_JOIN_BUCKETIZATION",
            "DISTINCT_ORDER_BY_ALIGNMENT",
            "CORRELATED_SUBQUERY_DECORRELATION",
            "NESTED_SUBQUERY_FLATTENING",
            "APPROX_DISTINCT_SKETCH_MV",
            "PERCENTILE_SKETCH_PRECOMPUTE",
            "ROLLUP_AGGREGATE_LATTICE"
        ))
    );
    private static final Set<String> DERIVED_MANUAL_REVIEW_PROBLEMS = Collections.unmodifiableSet(
        new LinkedHashSet<String>(Arrays.asList(
            "SELECT_STAR",
            "OR_PREDICATE_INDEX_RISK",
            "NESTED_SUBQUERY_RISK",
            "LEADING_WILDCARD_LIKE_RISK"
        ))
    );
    private static final Set<String> SKIPPED_PROBLEMS = Collections.unmodifiableSet(
        new LinkedHashSet<String>(Arrays.asList("SQL_SYNTAX_INVALID", "SQL_TOO_LONG"))
    );
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
    };
    private static final TypeReference<Map<String, Object>> STRING_OBJECT_MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private final RewriteTrialRepository rewriteTrialRepository;
    private final ParseBatchRepository parseBatchRepository;
    private final ParseBatchItemRepository parseBatchItemRepository;
    private final AccelerationRecommendationRepository recommendationRepository;
    private final SqlOptimizationPipelineService pipelineService;
    private final OptimizationTaskApplicationService optimizationTaskApplicationService;
    private final ObjectMapper objectMapper;

    public RewriteTrialApplicationService(RewriteTrialRepository rewriteTrialRepository,
                                          ParseBatchRepository parseBatchRepository,
                                          ParseBatchItemRepository parseBatchItemRepository,
                                          AccelerationRecommendationRepository recommendationRepository,
                                          SqlOptimizationPipelineService pipelineService,
                                          OptimizationTaskApplicationService optimizationTaskApplicationService) {
        this.rewriteTrialRepository = rewriteTrialRepository;
        this.parseBatchRepository = parseBatchRepository;
        this.parseBatchItemRepository = parseBatchItemRepository;
        this.recommendationRepository = recommendationRepository;
        this.pipelineService = pipelineService;
        this.optimizationTaskApplicationService = optimizationTaskApplicationService;
        this.objectMapper = JsonUtils.objectMapper();
    }

    private static Set<String> candidateRewriteProblems() {
        LinkedHashSet<String> values = new LinkedHashSet<String>(SAFE_REWRITE_PROBLEMS);
        values.add(L2DynamicSnapshotAggregateMvCandidateGenerator.RULE);
        return Collections.unmodifiableSet(values);
    }

    public RewriteTrialRunVO createTrial(RewriteTrialRequest request) {
        String tenantId = requireAuthorizedTenant(request == null ? null : request.getTenantId());
        if (request == null || !StringUtils.hasText(request.getSqlText())) {
            throw invalidArgument("sqlText", "sqlText 为必填项");
        }
        Instant now = Instant.now();
        RewriteTrialRun run = RewriteTrialRun.builder()
            .runId(UUID.randomUUID().toString())
            .tenantId(tenantId)
            .sourceKind(firstText(request.getSourceKind(), SOURCE_KIND_STRUCTURE_PARSE))
            .sourceId(firstText(request.getSourceId(), request.getParseHistoryId(), request.getParseTaskId(), request.getHistoryId()))
            .status(RewriteTrialStatus.RUNNING)
            .createdBy(RequestContext.getUserId())
            .createdAt(now)
            .build();
        rewriteTrialRepository.saveRun(run);
        RewriteTrialItem item = evaluateSql(
            run,
            null,
            request.getParseTaskId(),
            request.getParseHistoryId(),
            request.getHistoryId(),
            request.getDatasourceCode(),
            request.getSqlText(),
            request.getSourceProblems(),
            Collections.<String>emptySet(),
            null,
            now
        );
        rewriteTrialRepository.saveItem(item);
        refreshRunSummary(run, Collections.singletonList(item), now);
        rewriteTrialRepository.saveRun(run);
        LOGGER.info(
            "操作日志 operation=REWRITE_TRIAL_CREATE entity={} tenantId={} sourceKind={} status={} accepted={} skipped={}",
            run.getRunId(),
            tenantId,
            run.getSourceKind(),
            run.getStatus(),
            Integer.valueOf(run.getAcceptedCount()),
            Integer.valueOf(run.getSkippedCount())
        );
        return toRunVo(run, Collections.singletonList(item));
    }

    public RewriteTrialRunVO createBatchTrial(String batchId, RewriteTrialBatchRequest request) {
        String tenantId = requireAuthorizedTenant(request == null ? null : request.getTenantId());
        ParseBatch batch = requireBatch(batchId, tenantId);
        if (!Boolean.TRUE.equals(request == null ? null : request.getForceRecalculate())) {
            RewriteTrialRun latest = rewriteTrialRepository.findLatestRunByBatchId(tenantId, batch.getBatchId());
            if (latest != null) {
                return toRunVo(latest, rewriteTrialRepository.findItemsByRunId(latest.getRunId()));
            }
        }
        Set<String> issueFilter = normalizeFilter(request == null ? null : request.getIssueSceneFilter());
        int maxItems = normalizeMaxItems(request == null ? null : request.getMaxItems());
        Instant now = Instant.now();
        RewriteTrialRun run = RewriteTrialRun.builder()
            .runId(UUID.randomUUID().toString())
            .tenantId(tenantId)
            .sourceKind(SOURCE_KIND_PARSE_BATCH)
            .sourceId(batch.getBatchId())
            .batchId(batch.getBatchId())
            .status(RewriteTrialStatus.RUNNING)
            .createdBy(RequestContext.getUserId())
            .createdAt(now)
            .build();
        rewriteTrialRepository.saveRun(run);

        List<RewriteTrialItem> trialItems = new ArrayList<RewriteTrialItem>();
        List<ParseBatchItem> batchItems = parseBatchItemRepository.findByBatchId(batch.getBatchId());
        int processed = 0;
        for (ParseBatchItem batchItem : batchItems) {
            if (processed >= maxItems) {
                trialItems.add(skippedItem(run, batchItem, "超过本次批量试算 maxItems 限制", now));
                continue;
            }
            if (!"VALID".equals(batchItem.getStructureSyntaxStatus())) {
                trialItems.add(skippedItem(run, batchItem, "结构解析未通过，跳过改写试算", now));
                continue;
            }
            RewriteTrialItem item = evaluateSql(
                run,
                batchItem.getItemId(),
                batchItem.getParseTaskId(),
                batchItem.getHistoryId(),
                batchItem.getHistoryId(),
                batchItem.getDatasourceCode(),
                batchItem.getSqlText(),
                null,
                issueFilter,
                batchItem.getPlanAnalysisJson(),
                now
            );
            if (item.getTrialStatus() != RewriteTrialStatus.NOT_REQUESTED) {
                processed++;
            }
            trialItems.add(item);
        }
        for (RewriteTrialItem item : trialItems) {
            rewriteTrialRepository.saveItem(item);
        }
        refreshRunSummary(run, trialItems, now);
        rewriteTrialRepository.saveRun(run);
        LOGGER.info(
            "操作日志 operation=REWRITE_TRIAL_BATCH_CREATE entity={} tenantId={} batchId={} status={} total={} accepted={} skipped={}",
            run.getRunId(),
            tenantId,
            batch.getBatchId(),
            run.getStatus(),
            Integer.valueOf(run.getTotalCount()),
            Integer.valueOf(run.getAcceptedCount()),
            Integer.valueOf(run.getSkippedCount())
        );
        return toRunVo(run, trialItems);
    }

    public RewriteTrialRunVO getTrial(String runId) {
        RewriteTrialRun run = requireRun(runId);
        return toRunVo(run, rewriteTrialRepository.findItemsByRunId(run.getRunId()));
    }

    public RewriteTrialRunVO latestBatchTrial(String batchId) {
        String tenantId = requireContextTenant();
        RewriteTrialRun run = rewriteTrialRepository.findLatestRunByBatchId(tenantId, requireText(batchId, "batchId"));
        if (run == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "解析批次尚未生成改写试算，batchId=" + batchId
            );
        }
        return toRunVo(run, rewriteTrialRepository.findItemsByRunId(run.getRunId()));
    }

    public RewriteTrialOverviewVO overview() {
        List<RewriteTrialItem> items = rewriteTrialRepository.findItemsByTenantId(requireContextTenant());
        RewriteTrialOverviewVO vo = new RewriteTrialOverviewVO();
        vo.setEligibleSqlCount(Integer.valueOf(countEligible(items)));
        vo.setTrialedSqlCount(Integer.valueOf(countTrialed(items)));
        vo.setCandidateGeneratedCount(Integer.valueOf(countStatus(items, RewriteTrialStatus.CANDIDATE_GENERATED)
            + countStatus(items, RewriteTrialStatus.RECOMMENDED)
            + countStatus(items, RewriteTrialStatus.RECORD_CREATED)));
        vo.setNoSafeRewriteCount(Integer.valueOf(countStatus(items, RewriteTrialStatus.NO_SAFE_REWRITE)));
        vo.setManualReviewRequiredCount(Integer.valueOf(countManualReview(items)));
        vo.setValidatedEquivalentCount(Integer.valueOf(countValidation(items, "EQUIVALENT", "VERIFIED")));
        vo.setValidatedDivergedCount(Integer.valueOf(countValidation(items, "DIVERGED", "FAILED")));
        return vo;
    }

    public List<RewriteTrialSourceIssueStatisticVO> bySourceIssue() {
        List<RewriteTrialItem> items = rewriteTrialRepository.findItemsByTenantId(requireContextTenant());
        Map<String, SourceIssueAccumulator> accumulators = new LinkedHashMap<String, SourceIssueAccumulator>();
        for (RewriteTrialItem item : items) {
            Set<String> scenes = sourceIssueScenes(item.getSourceProblems());
            for (String scene : scenes) {
                SourceIssueAccumulator accumulator = accumulators.get(scene);
                if (accumulator == null) {
                    accumulator = new SourceIssueAccumulator(scene);
                    accumulators.put(scene, accumulator);
                }
                accumulator.eligibleSqlCount++;
                if (isTrialed(item)) {
                    accumulator.trialedSqlCount++;
                }
                if (hasCandidate(item)) {
                    accumulator.candidateGeneratedCount++;
                }
                if (item.getTrialStatus() == RewriteTrialStatus.NO_SAFE_REWRITE) {
                    accumulator.noSafeRewriteCount++;
                }
                if (manualReviewRequired(item)) {
                    accumulator.manualReviewRequiredCount++;
                }
                if (matchesValidation(item.getValidationStatus(), "EQUIVALENT", "VERIFIED")) {
                    accumulator.validationPassedCount++;
                }
            }
        }
        List<RewriteTrialSourceIssueStatisticVO> result = new ArrayList<RewriteTrialSourceIssueStatisticVO>();
        for (SourceIssueAccumulator accumulator : accumulators.values()) {
            RewriteTrialSourceIssueStatisticVO vo = new RewriteTrialSourceIssueStatisticVO();
            vo.setSourceIssueScene(accumulator.sourceIssueScene);
            vo.setEligibleSqlCount(Integer.valueOf(accumulator.eligibleSqlCount));
            vo.setTrialedSqlCount(Integer.valueOf(accumulator.trialedSqlCount));
            vo.setCandidateGeneratedRate(ratio(accumulator.candidateGeneratedCount, accumulator.trialedSqlCount));
            vo.setNoSafeRewriteRate(ratio(accumulator.noSafeRewriteCount, accumulator.trialedSqlCount));
            vo.setManualReviewRate(ratio(accumulator.manualReviewRequiredCount, accumulator.trialedSqlCount));
            vo.setValidationPassedRate(ratio(accumulator.validationPassedCount, accumulator.trialedSqlCount));
            result.add(vo);
        }
        return result;
    }

    private RewriteTrialItem evaluateSql(RewriteTrialRun run,
                                         String batchItemId,
                                         String parseTaskId,
                                         String parseHistoryId,
                                         String historyId,
                                         String datasourceCode,
                                         String sqlText,
                                         List<Map<String, Object>> requestedSourceProblems,
                                         Set<String> issueFilter,
                                         String planAnalysisJson,
                                         Instant now) {
        String sqlFingerprint = StringUtils.hasText(sqlText) ? SqlFingerprintUtils.fingerprint(sqlText.trim()) : null;
        try {
            SqlOptimizationPipelineService.ParsedSqlProfile profile =
                pipelineService.analyze(sqlText, DataSourceTypeEnum.AUTO);
            OptimizationTaskSuggestion suggestion = pipelineService.buildRewriteSuggestion(profile);
            List<String> appliedRules = readAppliedRules(suggestion);
            Map<String, Object> planEvidence = planEvidence(planAnalysisJson);
            List<Map<String, Object>> sourceProblems = requestedSourceProblems == null || requestedSourceProblems.isEmpty()
                ? deriveSourceProblems(profile, appliedRules, parseTaskId, parseHistoryId, historyId, batchItemId, planEvidence)
                : normalizeSourceProblems(requestedSourceProblems, parseTaskId, parseHistoryId, historyId, batchItemId);
            sourceProblems = withPlanEvidence(sourceProblems, planEvidence);
            sourceProblems = applyIssueFilter(sourceProblems, issueFilter);
            if (sourceProblems.isEmpty()) {
                return RewriteTrialItem.builder()
                    .trialItemId(UUID.randomUUID().toString())
                    .runId(run.getRunId())
                    .batchItemId(batchItemId)
                    .parseTaskId(parseTaskId)
                    .parseHistoryId(parseHistoryId)
                    .historyId(historyId)
                    .sqlFingerprint(sqlFingerprint)
                    .datasourceCode(trimToNull(datasourceCode))
                    .sourceSqlText(sqlText)
                    .trialStatus(RewriteTrialStatus.NOT_REQUESTED)
                    .failureReason("未发现可试算来源问题")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            }
            List<Map<String, Object>> issueRuleLinks = buildIssueRuleLinks(sourceProblems, appliedRules);
            List<String> selectedAppliedRules = selectedAppliedRules(sourceProblems, appliedRules);
            boolean candidateGenerated = !selectedAppliedRules.isEmpty();
            String candidateSql = candidateGenerated ? artifactContent(suggestion, "REWRITTEN_SQL", "candidateSql") : null;
            String taskId = candidateGenerated ? taskIdFor(idempotencyKey(run, sqlFingerprint, sourceProblems)) : null;
            if (candidateGenerated) {
                submitRewriteTaskIfPossible(
                    taskId,
                    run,
                    parseTaskId,
                    parseHistoryId,
                    historyId,
                    datasourceCode,
                    sqlText,
                    sqlFingerprint,
                    filterSourceProblems(sourceProblems, selectedAppliedRules)
                );
            }
            String recommendationId = candidateGenerated
                ? persistRecommendation(run, parseTaskId, parseHistoryId, historyId, datasourceCode, sqlText, sqlFingerprint,
                    candidateSql, sourceProblems, issueRuleLinks, profile, suggestion, planEvidence)
                : null;
            return RewriteTrialItem.builder()
                .trialItemId(UUID.randomUUID().toString())
                .runId(run.getRunId())
                .batchItemId(batchItemId)
                .parseTaskId(parseTaskId)
                .parseHistoryId(parseHistoryId)
                .historyId(historyId)
                .sqlFingerprint(sqlFingerprint)
                .datasourceCode(trimToNull(datasourceCode))
                .sourceSqlText(sqlText)
                .sourceProblems(sourceProblems)
                .taskId(taskId)
                .recommendationId(recommendationId)
                .trialStatus(candidateGenerated ? RewriteTrialStatus.RECOMMENDED : RewriteTrialStatus.NO_SAFE_REWRITE)
                .candidateSql(candidateGenerated ? candidateSql : null)
                .validationStatus(RewriteValidationStatus.NOT_VALIDATED.name())
                .issueRuleLinks(issueRuleLinks)
                .createdAt(now)
                .updatedAt(now)
                .build();
        } catch (RuntimeException ex) {
            return RewriteTrialItem.builder()
                .trialItemId(UUID.randomUUID().toString())
                .runId(run.getRunId())
                .batchItemId(batchItemId)
                .parseTaskId(parseTaskId)
                .parseHistoryId(parseHistoryId)
                .historyId(historyId)
                .sqlFingerprint(sqlFingerprint)
                .datasourceCode(trimToNull(datasourceCode))
                .sourceSqlText(sqlText)
                .sourceProblems(skipProblem("SQL_SYNTAX_INVALID", parseTaskId, parseHistoryId, historyId, batchItemId))
                .trialStatus(RewriteTrialStatus.FAILED)
                .failureReason(ex.getMessage())
                .createdAt(now)
                .updatedAt(now)
                .build();
        }
    }

    private RewriteTrialItem skippedItem(RewriteTrialRun run, ParseBatchItem batchItem, String reason, Instant now) {
        return RewriteTrialItem.builder()
            .trialItemId(UUID.randomUUID().toString())
            .runId(run.getRunId())
            .batchItemId(batchItem.getItemId())
            .parseTaskId(batchItem.getParseTaskId())
            .parseHistoryId(batchItem.getHistoryId())
            .historyId(batchItem.getHistoryId())
            .sqlFingerprint(StringUtils.hasText(batchItem.getSqlText()) ? SqlFingerprintUtils.fingerprint(batchItem.getSqlText()) : null)
            .datasourceCode(batchItem.getDatasourceCode())
            .sourceSqlText(batchItem.getSqlText())
            .sourceProblems(skipProblem(firstText(batchItem.getStructureSyntaxStatus(), "SQL_SYNTAX_INVALID"),
                batchItem.getParseTaskId(), batchItem.getHistoryId(), batchItem.getHistoryId(), batchItem.getItemId()))
            .trialStatus(RewriteTrialStatus.NOT_REQUESTED)
            .failureReason(reason)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    private List<Map<String, Object>> deriveSourceProblems(SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                                           List<String> appliedRules,
                                                           String parseTaskId,
                                                           String parseHistoryId,
                                                           String historyId,
                                                           String batchItemId,
                                                           Map<String, Object> planEvidence) {
        LinkedHashMap<String, Map<String, Object>> problems = new LinkedHashMap<String, Map<String, Object>>();
        for (String appliedRule : appliedRules) {
            if (CANDIDATE_REWRITE_PROBLEMS.contains(appliedRule)) {
                problems.put(appliedRule, sourceProblem("REWRITE_CANDIDATE", appliedRule, null,
                    severityFor(appliedRule), priorityFor(appliedRule), summaryFor(appliedRule),
                    parseTaskId, parseHistoryId, historyId, batchItemId));
            }
        }
        for (String warning : profile.getWarnings()) {
            if (DERIVED_MANUAL_REVIEW_PROBLEMS.contains(warning)) {
                problems.put(warning, sourceProblem("ISSUE_SCENE", warning, warning, severityFor(warning), priorityFor(warning),
                    manualProblemSummary(warning), parseTaskId, parseHistoryId, historyId, batchItemId));
            }
        }
        SqlOptimizationPipelineService.RecommendationRuleOutputModel ruleModel =
            pipelineService.buildRecommendationRuleOutputModel(profile);
        addRuleOutputProblems(
            problems,
            ruleModel.getUnappliedRules(),
            parseTaskId,
            parseHistoryId,
            historyId,
            batchItemId
        );
        addRuleOutputProblems(
            problems,
            ruleModel.getRuleChain(),
            parseTaskId,
            parseHistoryId,
            historyId,
            batchItemId
        );
        attachPlanEvidence(problems, planEvidence);
        return new ArrayList<Map<String, Object>>(problems.values());
    }

    private void addRuleOutputProblems(LinkedHashMap<String, Map<String, Object>> problems,
                                       List<Map<String, Object>> ruleEntries,
                                       String parseTaskId,
                                       String parseHistoryId,
                                       String historyId,
                                       String batchItemId) {
        if (ruleEntries == null || ruleEntries.isEmpty()) {
            return;
        }
        for (Map<String, Object> entry : ruleEntries) {
            String rule = objectText(entry.get("rule"));
            if (!isEligibleProblem(rule) || !isManualReviewRuleOutput(entry) || problems.containsKey(rule)) {
                continue;
            }
            problems.put(rule, sourceProblem(
                "ISSUE_SCENE",
                rule,
                rule,
                severityFor(rule),
                priorityFor(rule),
                firstText(objectText(entry.get("description")), manualProblemSummary(rule)),
                parseTaskId,
                parseHistoryId,
                historyId,
                batchItemId
            ));
        }
    }

    private boolean isManualReviewRuleOutput(Map<String, Object> entry) {
        if (entry == null || entry.isEmpty()) {
            return false;
        }
        String status = objectText(entry.get("status"));
        return Boolean.TRUE.equals(entry.get("manualReviewRequired"))
            || "NOT_APPLIED".equals(status)
            || "PULL_ONLY_CANDIDATE".equals(status);
    }

    private void attachPlanEvidence(LinkedHashMap<String, Map<String, Object>> problems,
                                    Map<String, Object> planEvidence) {
        if (problems == null || problems.isEmpty() || planEvidence == null || planEvidence.isEmpty()) {
            return;
        }
        for (Map<String, Object> problem : problems.values()) {
            problem.put("planEvidence", planEvidence);
        }
    }

    private List<Map<String, Object>> withPlanEvidence(List<Map<String, Object>> sourceProblems,
                                                       Map<String, Object> planEvidence) {
        if (sourceProblems == null || sourceProblems.isEmpty() || planEvidence == null || planEvidence.isEmpty()) {
            return sourceProblems;
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(sourceProblems.size());
        for (Map<String, Object> sourceProblem : sourceProblems) {
            if (sourceProblem == null || sourceProblem.isEmpty()) {
                continue;
            }
            LinkedHashMap<String, Object> copy = new LinkedHashMap<String, Object>(sourceProblem);
            if (!copy.containsKey("planEvidence")) {
                copy.put("planEvidence", planEvidence);
            }
            result.add(copy);
        }
        return result;
    }

    private List<Map<String, Object>> normalizeSourceProblems(List<Map<String, Object>> requested,
                                                              String parseTaskId,
                                                              String parseHistoryId,
                                                              String historyId,
                                                              String batchItemId) {
        LinkedHashMap<String, Map<String, Object>> result = new LinkedHashMap<String, Map<String, Object>>();
        for (Map<String, Object> problem : requested) {
            if (problem == null || problem.isEmpty()) {
                continue;
            }
            String scene = objectText(firstValue(problem, "issueScene", "issueCode", "ruleCode"));
            if (!isEligibleProblem(scene)) {
                continue;
            }
            LinkedHashMap<String, Object> normalized = new LinkedHashMap<String, Object>(problem);
            normalized.put("problemType", firstText(objectText(normalized.get("problemType")),
                CANDIDATE_REWRITE_PROBLEMS.contains(scene) ? "REWRITE_CANDIDATE" : "ISSUE_SCENE"));
            normalized.put("issueScene", scene);
            if (!normalized.containsKey("issueCode")) {
                normalized.put("issueCode", scene);
            }
            normalized.put("severity", firstText(objectText(normalized.get("severity")), severityFor(scene)));
            normalized.put("priorityLevel", firstText(objectText(normalized.get("priorityLevel")), priorityFor(scene)));
            normalized.put("summary", firstText(objectText(normalized.get("summary")), summaryFor(scene)));
            normalized.put("evidenceRef", evidenceRef(parseTaskId, parseHistoryId, historyId, batchItemId));
            result.put(scene, normalized);
        }
        return new ArrayList<Map<String, Object>>(result.values());
    }

    private List<Map<String, Object>> applyIssueFilter(List<Map<String, Object>> sourceProblems, Set<String> issueFilter) {
        if (issueFilter == null || issueFilter.isEmpty()) {
            return sourceProblems;
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> problem : sourceProblems) {
            String scene = objectText(problem.get("issueScene"));
            if (issueFilter.contains(scene)) {
                result.add(problem);
            }
        }
        return result;
    }

    private List<Map<String, Object>> filterSourceProblems(List<Map<String, Object>> sourceProblems, List<String> selectedScenes) {
        if (sourceProblems == null || sourceProblems.isEmpty() || selectedScenes == null || selectedScenes.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> selected = new LinkedHashSet<String>(selectedScenes);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> problem : sourceProblems) {
            if (selected.contains(objectText(problem.get("issueScene")))) {
                result.add(problem);
            }
        }
        return result;
    }

    private List<Map<String, Object>> buildIssueRuleLinks(List<Map<String, Object>> sourceProblems, List<String> appliedRules) {
        List<Map<String, Object>> links = new ArrayList<Map<String, Object>>();
        Set<String> applied = new LinkedHashSet<String>(appliedRules);
        for (Map<String, Object> problem : sourceProblems) {
            String scene = objectText(problem.get("issueScene"));
            if (CANDIDATE_REWRITE_PROBLEMS.contains(scene)) {
                links.add(issueRuleLink(scene, scene, candidateRuleLevel(scene), applied.contains(scene) ? "APPLIED" : "SKIPPED",
                    applied.contains(scene) ? "CANDIDATE_GENERATED" : "NO_SAFE_REWRITE", null));
            } else if ("SELECT_STAR".equals(scene)) {
                links.add(issueRuleLink(scene, "SELECT_STAR_EXPANSION", "L1", "UNAPPLIED", "REQUIRES_METADATA",
                    "缺少可信列元数据，不自动展开 SELECT *"));
            } else if ("OR_PREDICATE_INDEX_RISK".equals(scene)) {
                links.add(issueRuleLink(scene, "OR_TO_UNION_ALL", "L1", "UNAPPLIED", "REQUIRES_SEMANTIC_PROOF",
                    "OR 转 UNION ALL 需要互斥或去重证明"));
            } else if ("NESTED_SUBQUERY_RISK".equals(scene)) {
                links.add(issueRuleLink(scene, "SUBQUERY_TO_JOIN_OR_CTE", "L1", "UNAPPLIED", "REQUIRES_SEMANTIC_PROOF",
                    "子查询改 JOIN/CTE 需要唯一性、NULL 语义和引擎行为证明"));
            } else if ("LEADING_WILDCARD_LIKE_RISK".equals(scene)) {
                links.add(issueRuleLink(scene, "LEADING_LIKE_REVIEW", "L1", "SKIPPED", "NO_SAFE_REWRITE",
                    "前导通配符更偏搜索索引或文本能力治理，默认不生成 SQL 候选"));
            } else {
                links.add(issueRuleLink(scene, scene, "L1", "SKIPPED", "NO_SAFE_REWRITE", "当前来源问题没有安全自动改写规则"));
            }
        }
        return links;
    }

    private Map<String, Object> issueRuleLink(String sourceIssueScene,
                                              String ruleCode,
                                              String ruleLevel,
                                              String ruleAction,
                                              String trialConclusion,
                                              String riskReason) {
        LinkedHashMap<String, Object> link = new LinkedHashMap<String, Object>();
        link.put("sourceIssueScene", sourceIssueScene);
        link.put("ruleCode", ruleCode);
        link.put("ruleLevel", ruleLevel);
        link.put("ruleAction", ruleAction);
        link.put("trialConclusion", trialConclusion);
        link.put("riskReason", riskReason);
        return link;
    }

    private List<String> selectedAppliedRules(List<Map<String, Object>> sourceProblems, List<String> appliedRules) {
        Set<String> selectedProblems = sourceIssueScenes(sourceProblems);
        List<String> result = new ArrayList<String>();
        for (String appliedRule : appliedRules) {
            if (selectedProblems.contains(appliedRule)) {
                result.add(appliedRule);
            }
        }
        return result;
    }

    private void submitRewriteTaskIfPossible(String taskId,
                                             RewriteTrialRun run,
                                             String parseTaskId,
                                             String parseHistoryId,
                                             String historyId,
                                             String datasourceCode,
                                             String sqlText,
                                             String sqlFingerprint,
                                             List<Map<String, Object>> sourceProblems) {
        if (optimizationTaskApplicationService == null) {
            return;
        }
        OptimizationTaskContextDTO context = new OptimizationTaskContextDTO();
        context.setPriority(OptimizationTaskPriority.NORMAL);
        context.setParseDepth(OptimizationParseDepth.DEEP);
        context.setSourceType(run.getSourceKind());
        context.setSourceId(firstText(run.getSourceId(), parseHistoryId, parseTaskId, historyId));
        context.setBatchId(run.getBatchId());
        context.setHistoryId(historyId);
        context.setParseTaskId(parseTaskId);
        context.setDatasourceCode(trimToNull(datasourceCode));
        context.setIssueScenes(new ArrayList<String>(sourceIssueScenes(sourceProblems)));

        OptimizationTaskSubmitRequest submitRequest = new OptimizationTaskSubmitRequest();
        submitRequest.setTenantId(run.getTenantId());
        submitRequest.setTaskType(OptimizationTaskType.REWRITE);
        submitRequest.setSqlText(sqlText);
        submitRequest.setSqlFingerprint(sqlFingerprint);
        submitRequest.setDatasourceType(DataSourceTypeEnum.AUTO);
        submitRequest.setTaskContext(context);
        try {
            optimizationTaskApplicationService.submitInternalTaskIfAbsent(submitRequest, taskId);
        } catch (RuntimeException ex) {
            LOGGER.warn(
                "操作日志 operation=REWRITE_TRIAL_TASK_SUBMIT entity={} tenantId={} status=DEGRADED reason={}",
                taskId,
                run.getTenantId(),
                ex.getMessage()
            );
        }
    }

    private String persistRecommendation(RewriteTrialRun run,
                                         String parseTaskId,
                                         String parseHistoryId,
                                         String historyId,
                                         String datasourceCode,
                                         String sqlText,
                                         String sqlFingerprint,
                                         String candidateSql,
                                         List<Map<String, Object>> sourceProblems,
                                         List<Map<String, Object>> issueRuleLinks,
                                         SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                         OptimizationTaskSuggestion suggestion,
                                         Map<String, Object> planEvidence) {
        String recommendationId = recommendationIdFor(idempotencyKey(run, sqlFingerprint, sourceProblems));
        if (recommendationRepository.findByRecommendationId(recommendationId) != null) {
            return recommendationId;
        }
        SqlOptimizationPipelineService.RecommendationRuleOutputModel ruleModel =
            pipelineService.buildRecommendationRuleOutputModel(profile);
        Map<String, Object> accelerationArtifact = L2AccelerationArtifactBuilder.buildForPrecomputeCandidate(
            new L2AccelerationArtifactBuilder.AccelerationRecommendationInput(
                sqlText,
                DataSourceTypeEnum.AUTO.name(),
                trimToNull(datasourceCode),
                sqlFingerprint,
                reportCodeFromSql(sqlText),
                "rewrite_trial_" + firstText(sqlFingerprint, recommendationId),
                null
            ),
            profile
        );
        String targetEngine = targetEngineFromArtifact(accelerationArtifact);
        Instant now = Instant.now();
        AccelerationRecommendation recommendation = AccelerationRecommendation.builder()
            .recommendationId(recommendationId)
            .tenantId(run.getTenantId())
            .recommendationType(RecommendationType.REWRITE)
            .sourceSqlId(firstText(run.getSourceId(), parseHistoryId, parseTaskId, historyId))
            .historyId(historyId)
            .parseTaskId(parseTaskId)
            .batchId(run.getBatchId())
            .sqlFingerprint(sqlFingerprint)
            .sourceSqlText(sqlText)
            .recommendedSqlText(firstText(candidateSql, sqlText))
            .targetEngine(firstText(targetEngine, DataSourceTypeEnum.AUTO.name()))
            .targetDatasource(trimToNull(datasourceCode))
            .summary("解析问题已生成改写试算候选：" + String.join(", ", sourceIssueScenes(sourceProblems)))
            .reason(firstText(suggestion.getPrimaryRecommendation(), "已根据解析问题生成保守改写试算。"))
            .expectedGain(firstBenefitSummary(suggestion))
            .benefitLevel(BenefitLevel.LOW)
            .riskLevel(RiskLevel.MEDIUM)
            .riskSummary("试算候选仅作为推荐证据，激活前必须通过改写记录资格检查与结果校验。")
            .requiresDispatch(false)
            .status(RecommendationStatus.RECOMMENDED)
            .sourceType(GovernanceSourceType.PARSE)
            .sourceKind(resolveGovernanceSourceKind(run.getSourceKind()))
            .sourceId(firstText(run.getSourceId(), parseHistoryId, parseTaskId, historyId, run.getBatchId()))
            .evidenceLevel(recommendationEvidenceLevel(planEvidence))
            .sourceProblems(sourceProblems)
            .issueRuleLinks(issueRuleLinks)
            .ruleChain(ruleModel.getRuleChain())
            .unappliedRules(ruleModel.getUnappliedRules())
            .preconditions(ruleModel.getPreconditions())
            .semanticRisks(ruleModel.getSemanticRisks())
            .accelerationArtifact(accelerationArtifact)
            .expectedBenefit(expectedBenefitWithPlanEvidence(ruleModel.getExpectedBenefit(), planEvidence))
            .estimatedCost(estimatedCostWithPlanEvidence(ruleModel.getEstimatedCost(), planEvidence))
            .confidence(ruleModel.getConfidence())
            .validationMethod(ruleModel.getValidationMethod())
            .validationStatus(RewriteValidationStatus.NOT_VALIDATED)
            .autoApplyAllowed(Boolean.FALSE)
            .manualReviewRequired(Boolean.valueOf(ruleModel.isManualReviewRequired()))
            .createdBy("SYSTEM_REWRITE_TRIAL")
            .createdAt(now)
            .updatedAt(now)
            .build();
        recommendationRepository.save(recommendation);
        return recommendationId;
    }

    private String targetEngineFromArtifact(Map<String, Object> accelerationArtifact) {
        if (accelerationArtifact == null || accelerationArtifact.isEmpty()) {
            return null;
        }
        return objectText(accelerationArtifact.get("targetEngine"));
    }

    private String reportCodeFromSql(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return null;
        }
        String[] lines = sqlText.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.toUpperCase(Locale.ROOT).startsWith("-- YH_RPTID=")) {
                return trimToNull(trimmed.substring("-- YH_RPTID=".length()));
            }
        }
        return null;
    }

    private GovernanceSourceKind resolveGovernanceSourceKind(String sourceKind) {
        String normalized = firstText(sourceKind, SOURCE_KIND_STRUCTURE_PARSE);
        try {
            return GovernanceSourceKind.valueOf(normalized);
        } catch (RuntimeException ex) {
            return GovernanceSourceKind.STRUCTURE_PARSE;
        }
    }

    private String firstBenefitSummary(OptimizationTaskSuggestion suggestion) {
        if (suggestion != null && suggestion.getBenefits() != null && !suggestion.getBenefits().isEmpty()) {
            return suggestion.getBenefits().get(0).getSummary();
        }
        return "已生成可进入结果校验的静态试算候选。";
    }

    private Map<String, Object> planEvidence(String planAnalysisJson) {
        if (!StringUtils.hasText(planAnalysisJson)) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(planAnalysisJson, STRING_OBJECT_MAP_TYPE);
            String status = objectText(raw.get("status"));
            if (!StringUtils.hasText(status)) {
                return Collections.emptyMap();
            }
            LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
            boolean explainAvailable = "SUCCESS".equals(status);
            evidence.put("evidenceLevel", explainAvailable ? EvidenceLevel.EXPLAIN_PLAN.name() : EvidenceLevel.STATIC_PARSE.name());
            evidence.put("planAnalysisStatus", status);
            evidence.put("claimBoundary", "EXPLAIN_ONLY_NOT_RESULT_EQUIVALENCE");
            evidence.put("planTextAvailable", Boolean.valueOf(StringUtils.hasText(objectText(raw.get("planText")))));
            String datasourceCode = objectText(raw.get("datasourceCode"));
            if (StringUtils.hasText(datasourceCode)) {
                evidence.put("datasourceCode", datasourceCode);
            }
            Object costMs = raw.get("costMs");
            if (costMs != null) {
                evidence.put("explainAnalysisCostMs", costMs);
            }
            Object rawEvidence = raw.get("evidence");
            if (rawEvidence != null) {
                evidence.put("evidence", rawEvidence);
            }
            String failureReason = objectText(raw.get("failureReason"));
            if (StringUtils.hasText(failureReason)) {
                evidence.put("failureReason", failureReason);
            }
            String planText = objectText(raw.get("planText"));
            if (StringUtils.hasText(planText)) {
                evidence.put("planTextPreview", abbreviate(planText, 512));
            }
            return evidence;
        } catch (Exception ex) {
            LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
            evidence.put("evidenceLevel", EvidenceLevel.STATIC_PARSE.name());
            evidence.put("planAnalysisStatus", "UNREADABLE");
            evidence.put("claimBoundary", "EXPLAIN_EVIDENCE_UNREADABLE");
            evidence.put("failureReason", abbreviate(ex.getMessage(), 256));
            return evidence;
        }
    }

    private EvidenceLevel recommendationEvidenceLevel(Map<String, Object> planEvidence) {
        if (planEvidence != null && EvidenceLevel.EXPLAIN_PLAN.name().equals(objectText(planEvidence.get("evidenceLevel")))) {
            return EvidenceLevel.MIXED;
        }
        return EvidenceLevel.STATIC_PARSE;
    }

    private Map<String, Object> expectedBenefitWithPlanEvidence(Map<String, Object> expectedBenefit,
                                                                Map<String, Object> planEvidence) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        if (expectedBenefit != null) {
            result.putAll(expectedBenefit);
        }
        if (planEvidence != null && !planEvidence.isEmpty()) {
            result.put("planEvidenceStatus", planEvidence.get("planAnalysisStatus"));
            result.put("claimBoundary", "STATIC_REWRITE_WITH_EXPLAIN_ONLY_NOT_REAL_GAIN");
        }
        return result;
    }

    private Map<String, Object> estimatedCostWithPlanEvidence(Map<String, Object> estimatedCost,
                                                              Map<String, Object> planEvidence) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        if (estimatedCost != null) {
            result.putAll(estimatedCost);
        }
        if (planEvidence != null && !planEvidence.isEmpty()) {
            boolean explainAvailable = EvidenceLevel.EXPLAIN_PLAN.name().equals(objectText(planEvidence.get("evidenceLevel")));
            result.put("evidenceType", explainAvailable ? "STATIC_HEURISTIC_WITH_EXPLAIN_PLAN" : "STATIC_HEURISTIC");
            result.put("explainPlanAvailable", Boolean.valueOf(explainAvailable));
            result.put("planEvidence", planEvidence);
        }
        return result;
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private List<String> readAppliedRules(OptimizationTaskSuggestion suggestion) {
        String content = artifactContent(suggestion, "REWRITE_RULE_TRACE", "appliedRules");
        if (!StringUtils.hasText(content)) {
            return Collections.emptyList();
        }
        try {
            List<String> values = objectMapper.readValue(content, STRING_LIST_TYPE);
            List<String> result = new ArrayList<String>();
            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    result.add(value.trim());
                }
            }
            return result;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String artifactContent(OptimizationTaskSuggestion suggestion, String category, String name) {
        if (suggestion == null || suggestion.getArtifacts() == null) {
            return null;
        }
        for (OptimizationTaskArtifact artifact : suggestion.getArtifacts()) {
            if (artifact != null && category.equals(artifact.getCategory()) && name.equals(artifact.getName())) {
                return artifact.getContent();
            }
        }
        return null;
    }

    private void refreshRunSummary(RewriteTrialRun run, List<RewriteTrialItem> items, Instant now) {
        int accepted = 0;
        int skipped = 0;
        int failed = 0;
        int recommended = 0;
        int noSafe = 0;
        int running = 0;
        for (RewriteTrialItem item : items) {
            if (item.getTrialStatus() == RewriteTrialStatus.NOT_REQUESTED) {
                skipped++;
            } else {
                accepted++;
            }
            if (item.getTrialStatus() == RewriteTrialStatus.FAILED) {
                failed++;
            } else if (item.getTrialStatus() == RewriteTrialStatus.RECOMMENDED) {
                recommended++;
            } else if (item.getTrialStatus() == RewriteTrialStatus.NO_SAFE_REWRITE) {
                noSafe++;
            } else if (item.getTrialStatus() == RewriteTrialStatus.QUEUED || item.getTrialStatus() == RewriteTrialStatus.RUNNING) {
                running++;
            }
        }
        RewriteTrialStatus status = aggregateStatus(items.size(), accepted, failed, recommended, noSafe, running);
        run.refreshSummary(items.size(), accepted, skipped, status, now);
    }

    private RewriteTrialStatus aggregateStatus(int total, int accepted, int failed, int recommended, int noSafe, int running) {
        if (total == 0 || accepted == 0) {
            return RewriteTrialStatus.NOT_REQUESTED;
        }
        if (running > 0) {
            return RewriteTrialStatus.RUNNING;
        }
        if (failed == accepted) {
            return RewriteTrialStatus.FAILED;
        }
        if (recommended > 0) {
            return RewriteTrialStatus.RECOMMENDED;
        }
        if (noSafe > 0) {
            return RewriteTrialStatus.NO_SAFE_REWRITE;
        }
        return RewriteTrialStatus.CANDIDATE_GENERATED;
    }

    private RewriteTrialRunVO toRunVo(RewriteTrialRun run, List<RewriteTrialItem> items) {
        RewriteTrialRunVO vo = new RewriteTrialRunVO();
        vo.setRunId(run.getRunId());
        vo.setTenantId(run.getTenantId());
        vo.setSourceKind(run.getSourceKind());
        vo.setSourceId(run.getSourceId());
        vo.setBatchId(run.getBatchId());
        vo.setTrialStatus(run.getStatus().name());
        vo.setTotalCount(Integer.valueOf(run.getTotalCount()));
        vo.setAcceptedCount(Integer.valueOf(run.getAcceptedCount()));
        vo.setSkippedCount(Integer.valueOf(run.getSkippedCount()));
        vo.setQueuedCount(Integer.valueOf(countStatus(items, RewriteTrialStatus.QUEUED)));
        vo.setRunningCount(Integer.valueOf(countStatus(items, RewriteTrialStatus.RUNNING)));
        vo.setCandidateGeneratedCount(Integer.valueOf(countStatus(items, RewriteTrialStatus.CANDIDATE_GENERATED)
            + countStatus(items, RewriteTrialStatus.RECOMMENDED)
            + countStatus(items, RewriteTrialStatus.RECORD_CREATED)));
        vo.setNoSafeRewriteCount(Integer.valueOf(countStatus(items, RewriteTrialStatus.NO_SAFE_REWRITE)));
        vo.setFailedCount(Integer.valueOf(countStatus(items, RewriteTrialStatus.FAILED)));
        vo.setRecommendedCount(Integer.valueOf(countStatus(items, RewriteTrialStatus.RECOMMENDED)));
        vo.setRecordCreatedCount(Integer.valueOf(countStatus(items, RewriteTrialStatus.RECORD_CREATED)));
        vo.setCreatedBy(run.getCreatedBy());
        vo.setCreatedAt(run.getCreatedAt());
        vo.setUpdatedAt(run.getUpdatedAt());
        List<RewriteTrialItemVO> itemVos = new ArrayList<RewriteTrialItemVO>(items.size());
        for (RewriteTrialItem item : items) {
            itemVos.add(toItemVo(item));
        }
        vo.setItems(itemVos);
        return vo;
    }

    private RewriteTrialItemVO toItemVo(RewriteTrialItem item) {
        RewriteTrialItemVO vo = new RewriteTrialItemVO();
        vo.setTrialItemId(item.getTrialItemId());
        vo.setRunId(item.getRunId());
        vo.setBatchItemId(item.getBatchItemId());
        vo.setParseTaskId(item.getParseTaskId());
        vo.setParseHistoryId(item.getParseHistoryId());
        vo.setHistoryId(item.getHistoryId());
        vo.setSqlFingerprint(item.getSqlFingerprint());
        vo.setDatasourceCode(item.getDatasourceCode());
        vo.setSourceSqlText(item.getSourceSqlText());
        vo.setSourceProblems(item.getSourceProblems());
        vo.setTaskId(item.getTaskId());
        vo.setRecommendationId(item.getRecommendationId());
        vo.setRewriteRecordId(item.getRewriteRecordId());
        vo.setTrialStatus(item.getTrialStatus().name());
        vo.setFailureReason(item.getFailureReason());
        vo.setCandidateSql(item.getCandidateSql());
        vo.setValidationStatus(item.getValidationStatus());
        vo.setIssueRuleLinks(item.getIssueRuleLinks());
        vo.setCreatedAt(item.getCreatedAt());
        vo.setUpdatedAt(item.getUpdatedAt());
        return vo;
    }

    private int countStatus(List<RewriteTrialItem> items, RewriteTrialStatus status) {
        int count = 0;
        for (RewriteTrialItem item : items) {
            if (item.getTrialStatus() == status) {
                count++;
            }
        }
        return count;
    }

    private int countEligible(List<RewriteTrialItem> items) {
        int count = 0;
        for (RewriteTrialItem item : items) {
            if (!item.getSourceProblems().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private int countTrialed(List<RewriteTrialItem> items) {
        int count = 0;
        for (RewriteTrialItem item : items) {
            if (isTrialed(item)) {
                count++;
            }
        }
        return count;
    }

    private boolean isTrialed(RewriteTrialItem item) {
        return item.getTrialStatus() != RewriteTrialStatus.NOT_REQUESTED;
    }

    private boolean hasCandidate(RewriteTrialItem item) {
        return item.getTrialStatus() == RewriteTrialStatus.CANDIDATE_GENERATED
            || item.getTrialStatus() == RewriteTrialStatus.RECOMMENDED
            || item.getTrialStatus() == RewriteTrialStatus.RECORD_CREATED;
    }

    private int countManualReview(List<RewriteTrialItem> items) {
        int count = 0;
        for (RewriteTrialItem item : items) {
            if (manualReviewRequired(item)) {
                count++;
            }
        }
        return count;
    }

    private boolean manualReviewRequired(RewriteTrialItem item) {
        for (Map<String, Object> link : item.getIssueRuleLinks()) {
            String action = objectText(link.get("ruleAction"));
            String conclusion = objectText(link.get("trialConclusion"));
            if (!"APPLIED".equals(action) || !"CANDIDATE_GENERATED".equals(conclusion)) {
                return true;
            }
        }
        return item.getTrialStatus() == RewriteTrialStatus.NO_SAFE_REWRITE;
    }

    private int countValidation(List<RewriteTrialItem> items, String first, String second) {
        int count = 0;
        for (RewriteTrialItem item : items) {
            if (matchesValidation(item.getValidationStatus(), first, second)) {
                count++;
            }
        }
        return count;
    }

    private boolean matchesValidation(String validationStatus, String first, String second) {
        return first.equals(validationStatus) || second.equals(validationStatus);
    }

    private Set<String> sourceIssueScenes(List<Map<String, Object>> sourceProblems) {
        LinkedHashSet<String> scenes = new LinkedHashSet<String>();
        if (sourceProblems == null) {
            return scenes;
        }
        for (Map<String, Object> problem : sourceProblems) {
            String scene = objectText(problem.get("issueScene"));
            if (StringUtils.hasText(scene)) {
                scenes.add(scene);
            }
        }
        return scenes;
    }

    private Double ratio(int numerator, int denominator) {
        return Double.valueOf(denominator <= 0 ? 0D : (double) numerator / (double) denominator);
    }

    private List<Map<String, Object>> skipProblem(String issueScene,
                                                  String parseTaskId,
                                                  String parseHistoryId,
                                                  String historyId,
                                                  String batchItemId) {
        return Collections.singletonList(sourceProblem("ISSUE_SCENE", issueScene, issueScene, "HIGH", "P1",
            "该 SQL 无法进入安全改写试算", parseTaskId, parseHistoryId, historyId, batchItemId));
    }

    private Map<String, Object> sourceProblem(String problemType,
                                              String issueScene,
                                              String issueCode,
                                              String severity,
                                              String priorityLevel,
                                              String summary,
                                              String parseTaskId,
                                              String parseHistoryId,
                                              String historyId,
                                              String batchItemId) {
        LinkedHashMap<String, Object> problem = new LinkedHashMap<String, Object>();
        problem.put("problemType", problemType);
        problem.put("issueScene", issueScene);
        problem.put("issueCode", issueCode);
        problem.put("severity", severity);
        problem.put("priorityLevel", priorityLevel);
        problem.put("summary", summary);
        problem.put("evidenceRef", evidenceRef(parseTaskId, parseHistoryId, historyId, batchItemId));
        return problem;
    }

    private Map<String, Object> evidenceRef(String parseTaskId,
                                            String parseHistoryId,
                                            String historyId,
                                            String batchItemId) {
        LinkedHashMap<String, Object> evidenceRef = new LinkedHashMap<String, Object>();
        evidenceRef.put("parseTaskId", trimToNull(parseTaskId));
        evidenceRef.put("parseHistoryId", trimToNull(parseHistoryId));
        evidenceRef.put("historyId", trimToNull(historyId));
        evidenceRef.put("batchItemId", trimToNull(batchItemId));
        return evidenceRef;
    }

    private Object firstValue(Map<String, Object> map, String first, String second, String third) {
        Object value = map.get(first);
        if (value != null) {
            return value;
        }
        value = map.get(second);
        return value == null ? map.get(third) : value;
    }

    private String severityFor(String scene) {
        if ("OR_PREDICATE_INDEX_RISK".equals(scene) || "NESTED_SUBQUERY_RISK".equals(scene)) {
            return "HIGH";
        }
        if (L2DynamicSnapshotAggregateMvCandidateGenerator.RULE.equals(scene)) {
            return "MEDIUM";
        }
        if ("SELECT_STAR".equals(scene) || "LEADING_WILDCARD_LIKE_RISK".equals(scene)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String priorityFor(String scene) {
        if ("OR_PREDICATE_INDEX_RISK".equals(scene) || "NESTED_SUBQUERY_RISK".equals(scene)) {
            return "P2";
        }
        if (SAFE_REWRITE_PROBLEMS.contains(scene)) {
            return "P3";
        }
        return "P2";
    }

    private String summaryFor(String scene) {
        if (SAFE_REWRITE_PROBLEMS.contains(scene)) {
            return safeRuleSummary(scene);
        }
        if (L2DynamicSnapshotAggregateMvCandidateGenerator.RULE.equals(scene)) {
            return "复杂报表重复扫描可生成客户时点聚合快照改写候选";
        }
        return manualProblemSummary(scene);
    }

    private String candidateRuleLevel(String scene) {
        return L2DynamicSnapshotAggregateMvCandidateGenerator.RULE.equals(scene) ? "L2" : "L0";
    }

    private String safeRuleSummary(String rule) {
        if ("COUNT_LITERAL_TO_COUNT_STAR".equals(rule)) {
            return "COUNT 非空字面量可保守改写为 COUNT(*)";
        }
        if ("DEDUPLICATE_WHERE_PREDICATES".equals(rule)) {
            return "WHERE 中存在重复谓词，可保守去重";
        }
        if ("DEDUPLICATE_HAVING_PREDICATES".equals(rule)) {
            return "HAVING 中存在重复谓词，可保守去重";
        }
        if ("DEDUPLICATE_GROUP_BY_KEYS".equals(rule)) {
            return "GROUP BY 中存在重复分组键，可保守去重";
        }
        if ("DEDUPLICATE_ORDER_BY_KEYS".equals(rule)) {
            return "ORDER BY 中存在重复排序键，可保守去重";
        }
        return "命中保守语法改写候选";
    }

    private String manualProblemSummary(String scene) {
        if ("SELECT_STAR".equals(scene)) {
            return "SELECT * 需要列元数据后才能展开投影";
        }
        if ("OR_PREDICATE_INDEX_RISK".equals(scene)) {
            return "OR 谓词存在索引风险，但自动改写需要互斥或去重证明";
        }
        if ("NESTED_SUBQUERY_RISK".equals(scene)) {
            return "嵌套子查询改写需要语义等价证明";
        }
        if ("LEADING_WILDCARD_LIKE_RISK".equals(scene)) {
            return "前导通配符 LIKE 更适合索引或搜索能力治理";
        }
        return "解析问题需要人工评审";
    }

    private boolean isEligibleProblem(String scene) {
        return StringUtils.hasText(scene)
            && !SKIPPED_PROBLEMS.contains(scene)
            && (CANDIDATE_REWRITE_PROBLEMS.contains(scene) || MANUAL_REVIEW_PROBLEMS.contains(scene));
    }

    private Set<String> normalizeFilter(List<String> issueSceneFilter) {
        if (issueSceneFilter == null || issueSceneFilter.isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (String issueScene : issueSceneFilter) {
            String normalized = trimToNull(issueScene);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return result;
    }

    private int normalizeMaxItems(Integer maxItems) {
        if (maxItems == null || maxItems.intValue() <= 0) {
            return DEFAULT_BATCH_MAX_ITEMS;
        }
        return Math.min(maxItems.intValue(), HARD_BATCH_MAX_ITEMS);
    }

    private ParseBatch requireBatch(String batchId, String tenantId) {
        ParseBatch batch = parseBatchRepository.findByBatchId(requireText(batchId, "batchId"));
        if (batch == null) {
            throw new BizException(
                ErrorCodeConstants.SQL_OPTIMIZATION_TASK_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "解析批次不存在，batchId=" + batchId
            );
        }
        if (!tenantId.equals(batch.getTenantId())) {
            throw new AccessDeniedException("当前认证租户无权访问该解析批次");
        }
        return batch;
    }

    private RewriteTrialRun requireRun(String runId) {
        RewriteTrialRun run = rewriteTrialRepository.findRunById(requireText(runId, "runId"));
        if (run == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "改写试算不存在，runId=" + runId
            );
        }
        verifyTenantAccess(run.getTenantId());
        return run;
    }

    private String requireAuthorizedTenant(String requestTenantId) {
        String contextTenantId = requireContextTenant();
        if (StringUtils.hasText(requestTenantId) && !contextTenantId.equals(requestTenantId.trim())) {
            throw new AccessDeniedException("请求 tenantId 与已认证租户上下文不一致");
        }
        return contextTenantId;
    }

    private String requireContextTenant() {
        String tenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "已认证请求上下文缺少 tenantId"
            );
        }
        return tenantId;
    }

    private void verifyTenantAccess(String tenantId) {
        if (!requireContextTenant().equals(tenantId)) {
            throw new AccessDeniedException("当前认证租户无权访问该改写试算");
        }
    }

    private BizException invalidArgument(String field, String message) {
        return new BizException(ErrorCodeConstants.SQL_OPTIMIZATION_TASK_INVALID, HttpStatus.BAD_REQUEST,
            field + ": " + message);
    }

    private String requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw invalidArgument(field, field + " 为必填项");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first.trim() : trimToNull(second);
    }

    private String firstText(String first, String second, String third) {
        return firstText(firstText(first, second), third);
    }

    private String firstText(String first, String second, String third, String fourth) {
        return firstText(firstText(first, second, third), fourth);
    }

    private String firstText(String first, String second, String third, String fourth, String fifth) {
        return firstText(firstText(first, second, third, fourth), fifth);
    }

    private String objectText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String idempotencyKey(RewriteTrialRun run, String sqlFingerprint, List<Map<String, Object>> sourceProblems) {
        return run.getTenantId()
            + "|REWRITE_TRIAL|"
            + firstText(run.getSourceKind(), "UNKNOWN")
            + "|"
            + firstText(run.getSourceId(), run.getBatchId(), "NO_SOURCE")
            + "|"
            + firstText(sqlFingerprint, "NO_FINGERPRINT")
            + "|"
            + String.join(",", sourceIssueScenes(sourceProblems));
    }

    private String taskIdFor(String key) {
        return "rewrite-trial-task-" + UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String recommendationIdFor(String key) {
        return "rewrite-trial-reco-" + UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static final class SourceIssueAccumulator {
        private final String sourceIssueScene;
        private int eligibleSqlCount;
        private int trialedSqlCount;
        private int candidateGeneratedCount;
        private int noSafeRewriteCount;
        private int manualReviewRequiredCount;
        private int validationPassedCount;

        private SourceIssueAccumulator(String sourceIssueScene) {
            this.sourceIssueScene = sourceIssueScene;
        }
    }
}
