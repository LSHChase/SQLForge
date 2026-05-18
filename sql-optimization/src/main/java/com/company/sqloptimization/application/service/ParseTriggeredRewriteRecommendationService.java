package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskContextDTO;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.application.controller.dto.StructureParseRequest;
import com.company.sqloptimization.application.controller.vo.StructureParseIssueVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
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
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.domain.task.OptimizationTaskArtifact;
import com.company.sqloptimization.domain.task.OptimizationTaskPriority;
import com.company.sqloptimization.domain.task.OptimizationTaskRisk;
import com.company.sqloptimization.domain.task.OptimizationTaskSourceContext;
import com.company.sqloptimization.domain.task.OptimizationTaskStatus;
import com.company.sqloptimization.domain.task.OptimizationTaskSuggestion;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ParseTriggeredRewriteRecommendationService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(ParseTriggeredRewriteRecommendationService.class);
    private static final Set<String> TARGET_ISSUE_SCENES = Collections.unmodifiableSet(
        new LinkedHashSet<String>(Arrays.asList(
            "OR_PREDICATE_INDEX_RISK",
            "SELECT_STAR",
            "NESTED_SUBQUERY_RISK",
            "LEADING_WILDCARD_LIKE_RISK"
        ))
    );
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
    };

    private final OptimizationTaskApplicationService optimizationTaskApplicationService;
    private final AccelerationRecommendationRepository recommendationRepository;
    private final SqlOptimizationPipelineService pipelineService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ParseTriggeredRewriteRecommendationService(OptimizationTaskApplicationService optimizationTaskApplicationService,
                                                      AccelerationRecommendationRepository recommendationRepository,
                                                      SqlOptimizationPipelineService pipelineService) {
        this.optimizationTaskApplicationService = optimizationTaskApplicationService;
        this.recommendationRepository = recommendationRepository;
        this.pipelineService = pipelineService;
        this.objectMapper = JsonUtils.objectMapper();
    }

    public void triggerAfterHistoryWrite(StructureParseResponseVO structureParse,
                                         StructureParseRequest request,
                                         String sourceType,
                                         String sourceId,
                                         String batchId) {
        try {
            if (!isTriggerable(structureParse, request)) {
                return;
            }
            List<String> targetIssueScenes = targetIssueScenes(structureParse);
            if (targetIssueScenes.isEmpty()) {
                return;
            }
            String tenantId = RequestContext.getTenantId();
            if (!StringUtils.hasText(tenantId)) {
                return;
            }
            String sqlFingerprint = resolveSqlFingerprint(structureParse, request);
            OptimizationTaskContextDTO context = new OptimizationTaskContextDTO();
            context.setPriority(OptimizationTaskPriority.NORMAL);
            context.setParseDepth(OptimizationParseDepth.DEEP);
            context.setSourceType(sourceType);
            context.setSourceId(firstText(sourceId, structureParse.getParseTaskId(), structureParse.getHistoryId()));
            context.setBatchId(batchId);
            context.setReportCode(resolveReportCode(request));
            context.setHistoryId(structureParse.getHistoryId());
            context.setParseTaskId(structureParse.getParseTaskId());
            context.setDatasourceCode(trimToNull(request.getDatasourceCode()));
            context.setIssueScenes(targetIssueScenes);

            OptimizationTaskSubmitRequest submitRequest = new OptimizationTaskSubmitRequest();
            submitRequest.setTenantId(tenantId);
            submitRequest.setTaskType(OptimizationTaskType.REWRITE);
            submitRequest.setSqlText(request.getSqlText());
            submitRequest.setSqlFingerprint(sqlFingerprint);
            submitRequest.setDatasourceType(DataSourceTypeEnum.AUTO);
            submitRequest.setTaskContext(context);
            String taskId = taskIdFor(idempotencyKey(tenantId, context, sqlFingerprint));
            optimizationTaskApplicationService.submitInternalTaskIfAbsent(submitRequest, taskId);
            LOGGER.info(
                "操作日志 operation=PARSE_TRIGGERED_REWRITE_SUBMIT entity={} tenantId={} historyId={} sourceType={} sourceId={} status=QUEUED_OR_REUSED issueScenes={}",
                taskId,
                tenantId,
                structureParse.getHistoryId(),
                context.getSourceType(),
                context.getSourceId(),
                targetIssueScenes
            );
        } catch (RuntimeException ex) {
            LOGGER.warn(
                "操作日志 operation=PARSE_TRIGGERED_REWRITE_SUBMIT entity={} tenantId={} status=DEGRADED reason={}",
                structureParse == null ? null : structureParse.getParseTaskId(),
                RequestContext.getTenantId(),
                ex.getMessage()
            );
        }
    }

    public void persistRecommendationFromSucceededRewriteTask(OptimizationTask task) {
        if (task == null
            || task.getTaskType() != OptimizationTaskType.REWRITE
            || task.getStatus() != OptimizationTaskStatus.SUCCEEDED
            || task.getSuggestion() == null
            || task.getSourceContext() == null
            || task.getSourceContext().getIssueScenes().isEmpty()) {
            return;
        }
        OptimizationTaskSourceContext context = task.getSourceContext();
        List<String> targetIssueScenes = targetIssueScenes(context.getIssueScenes());
        if (targetIssueScenes.isEmpty()) {
            return;
        }
        String recommendationId = recommendationIdFor(idempotencyKey(task.getTenantId(), context, task.getSqlFingerprint()));
        if (recommendationRepository.findByRecommendationId(recommendationId) != null) {
            return;
        }
        OptimizationTaskSuggestion suggestion = task.getSuggestion();
        List<String> appliedRules = readAppliedRules(suggestion);
        boolean safeRewriteAvailable = !appliedRules.isEmpty();
        String candidateSql = artifactContent(suggestion, "REWRITTEN_SQL", "candidateSql");
        String recommendedSql = safeRewriteAvailable && StringUtils.hasText(candidateSql)
            ? candidateSql.trim()
            : task.getSqlText();
        SqlOptimizationPipelineService.RecommendationRuleOutputModel ruleModel = buildRuleModel(task);
        List<Map<String, Object>> sourceProblems = buildSourceProblems(task, targetIssueScenes);
        List<Map<String, Object>> issueRuleLinks = buildIssueRuleLinks(targetIssueScenes, appliedRules);
        Instant now = Instant.now();
        AccelerationRecommendation recommendation = AccelerationRecommendation.builder()
            .recommendationId(recommendationId)
            .tenantId(task.getTenantId())
            .recommendationType(RecommendationType.REWRITE)
            .sourceSqlId(firstText(context.getSourceId(), context.getHistoryId(), context.getParseTaskId()))
            .historyId(context.getHistoryId())
            .parseTaskId(context.getParseTaskId())
            .batchId(context.getBatchId())
            .sqlFingerprint(task.getSqlFingerprint())
            .sourceSqlText(task.getSqlText())
            .recommendedSqlText(recommendedSql)
            .targetEngine(task.getDatasourceType() == null ? null : task.getDatasourceType().name())
            .targetDatasource(context.getDatasourceCode())
            .reportCode(context.getReportCode())
            .summary(buildSummary(targetIssueScenes, safeRewriteAvailable))
            .reason(buildReason(suggestion, targetIssueScenes, appliedRules, safeRewriteAvailable))
            .expectedGain(buildExpectedGain(suggestion, safeRewriteAvailable))
            .benefitLevel(resolveBenefitLevel(suggestion, safeRewriteAvailable))
            .riskLevel(safeRewriteAvailable ? RiskLevel.MEDIUM : RiskLevel.HIGH)
            .riskSummary(buildRiskSummary(suggestion, targetIssueScenes, safeRewriteAvailable))
            .requiresDispatch(false)
            .status(RecommendationStatus.RECOMMENDED)
            .sourceType(resolveGovernanceSourceType(context))
            .sourceKind(resolveGovernanceSourceKind(context))
            .sourceId(firstText(context.getSourceId(), context.getHistoryId(), context.getParseTaskId(), context.getBatchId()))
            .evidenceLevel(EvidenceLevel.STATIC_PARSE)
            .sourceProblems(sourceProblems)
            .issueRuleLinks(issueRuleLinks)
            .ruleChain(ruleModel.getRuleChain())
            .unappliedRules(ruleModel.getUnappliedRules())
            .preconditions(ruleModel.getPreconditions())
            .semanticRisks(ruleModel.getSemanticRisks())
            .expectedBenefit(ruleModel.getExpectedBenefit())
            .estimatedCost(ruleModel.getEstimatedCost())
            .confidence(ruleModel.getConfidence())
            .validationMethod(ruleModel.getValidationMethod())
            .validationStatus(RewriteValidationStatus.NOT_VALIDATED)
            .autoApplyAllowed(Boolean.valueOf(ruleModel.isAutoApplyAllowed()))
            .manualReviewRequired(Boolean.valueOf(ruleModel.isManualReviewRequired()))
            .createdBy("SYSTEM_PARSE_REWRITE")
            .createdAt(now)
            .updatedAt(now)
            .build();
        recommendationRepository.save(recommendation);
        LOGGER.info(
            "操作日志 operation=PARSE_TRIGGERED_REWRITE_RECOMMENDATION entity={} tenantId={} taskId={} historyId={} status=SAVED issueScenes={}",
            recommendationId,
            task.getTenantId(),
            task.getTaskId(),
            context.getHistoryId(),
            targetIssueScenes
        );
    }

    private List<Map<String, Object>> buildSourceProblems(OptimizationTask task, List<String> targetIssueScenes) {
        List<Map<String, Object>> problems = new ArrayList<Map<String, Object>>();
        OptimizationTaskSourceContext context = task.getSourceContext();
        for (String scene : targetIssueScenes) {
            java.util.LinkedHashMap<String, Object> problem = new java.util.LinkedHashMap<String, Object>();
            problem.put("problemType", "ISSUE_SCENE");
            problem.put("issueScene", scene);
            problem.put("issueCode", scene);
            problem.put("severity", "SELECT_STAR".equals(scene) ? "MEDIUM" : "HIGH");
            problem.put("priorityLevel", "P2");
            problem.put("summary", "解析问题触发改写试算：" + scene);
            java.util.LinkedHashMap<String, Object> evidenceRef = new java.util.LinkedHashMap<String, Object>();
            evidenceRef.put("parseTaskId", context.getParseTaskId());
            evidenceRef.put("parseHistoryId", context.getHistoryId());
            evidenceRef.put("batchId", context.getBatchId());
            problem.put("evidenceRef", evidenceRef);
            problems.add(problem);
        }
        return problems;
    }

    private List<Map<String, Object>> buildIssueRuleLinks(List<String> targetIssueScenes, List<String> appliedRules) {
        List<Map<String, Object>> links = new ArrayList<Map<String, Object>>();
        for (String scene : targetIssueScenes) {
            java.util.LinkedHashMap<String, Object> link = new java.util.LinkedHashMap<String, Object>();
            link.put("sourceIssueScene", scene);
            if ("SELECT_STAR".equals(scene)) {
                link.put("ruleCode", "SELECT_STAR_EXPANSION");
                link.put("ruleLevel", "L1");
                link.put("ruleAction", "UNAPPLIED");
                link.put("trialConclusion", "REQUIRES_METADATA");
                link.put("riskReason", "缺少列元数据，不自动展开星号投影");
            } else if ("OR_PREDICATE_INDEX_RISK".equals(scene)) {
                link.put("ruleCode", "OR_TO_UNION_ALL");
                link.put("ruleLevel", "L1");
                link.put("ruleAction", "UNAPPLIED");
                link.put("trialConclusion", "REQUIRES_SEMANTIC_PROOF");
                link.put("riskReason", "OR 改写需要互斥或去重证明");
            } else if ("NESTED_SUBQUERY_RISK".equals(scene)) {
                link.put("ruleCode", "SUBQUERY_TO_JOIN_OR_CTE");
                link.put("ruleLevel", "L1");
                link.put("ruleAction", "UNAPPLIED");
                link.put("trialConclusion", "REQUIRES_SEMANTIC_PROOF");
                link.put("riskReason", "子查询改写需要唯一性和 NULL 语义证明");
            } else {
                link.put("ruleCode", "LEADING_LIKE_REVIEW");
                link.put("ruleLevel", "L1");
                link.put("ruleAction", appliedRules.isEmpty() ? "SKIPPED" : "UNAPPLIED");
                link.put("trialConclusion", "NO_SAFE_REWRITE");
                link.put("riskReason", "该问题默认进入人工评审，不伪造候选 SQL");
            }
            links.add(link);
        }
        return links;
    }

    private SqlOptimizationPipelineService.RecommendationRuleOutputModel buildRuleModel(OptimizationTask task) {
        try {
            SqlOptimizationPipelineService.ParsedSqlProfile profile = pipelineService.analyze(
                task.getSqlText(),
                task.getDatasourceType() == null ? DataSourceTypeEnum.AUTO : task.getDatasourceType()
            );
            return pipelineService.buildRecommendationRuleOutputModel(profile);
        } catch (RuntimeException ex) {
            LOGGER.warn(
                "操作日志 operation=PARSE_TRIGGERED_RULE_MODEL entity={} tenantId={} status=DEGRADED reason={}",
                task.getTaskId(),
                task.getTenantId(),
                ex.getMessage()
            );
            return SqlOptimizationPipelineService.RecommendationRuleOutputModel.empty();
        }
    }

    private GovernanceSourceType resolveGovernanceSourceType(OptimizationTaskSourceContext context) {
        String sourceType = context == null ? null : trimToNull(context.getSourceType());
        if ("QUERY_HISTORY".equals(sourceType) || "SLOW_SQL".equals(sourceType)) {
            return GovernanceSourceType.QUERY;
        }
        return GovernanceSourceType.PARSE;
    }

    private GovernanceSourceKind resolveGovernanceSourceKind(OptimizationTaskSourceContext context) {
        String sourceType = context == null ? null : trimToNull(context.getSourceType());
        if ("PARSE_BATCH".equals(sourceType)) {
            return GovernanceSourceKind.PARSE_BATCH;
        }
        if ("REPORT_BATCH".equals(sourceType)) {
            return GovernanceSourceKind.REPORT_BATCH;
        }
        if ("COMBINED_PARSE".equals(sourceType)) {
            return GovernanceSourceKind.COMBINED_PARSE;
        }
        if ("QUERY_HISTORY".equals(sourceType)) {
            return GovernanceSourceKind.QUERY_HISTORY;
        }
        if ("SLOW_SQL".equals(sourceType)) {
            return GovernanceSourceKind.SLOW_SQL;
        }
        return GovernanceSourceKind.STRUCTURE_PARSE;
    }

    private boolean isTriggerable(StructureParseResponseVO structureParse, StructureParseRequest request) {
        return structureParse != null
            && request != null
            && "VALID".equals(structureParse.getSyntaxStatus())
            && Boolean.TRUE.equals(structureParse.getHistoryPersisted())
            && StringUtils.hasText(structureParse.getHistoryId())
            && StringUtils.hasText(structureParse.getParseTaskId())
            && StringUtils.hasText(request.getSqlText());
    }

    private List<String> targetIssueScenes(StructureParseResponseVO structureParse) {
        LinkedHashSet<String> scenes = new LinkedHashSet<String>();
        if (structureParse.getIssues() != null) {
            for (StructureParseIssueVO issue : structureParse.getIssues()) {
                if (issue == null) {
                    continue;
                }
                addTargetIssueScene(scenes, issue.getIssueCode());
                addTargetIssueScene(scenes, issue.getIssueScene());
            }
        }
        if (structureParse.getRiskTags() != null) {
            for (String riskTag : structureParse.getRiskTags()) {
                addTargetIssueScene(scenes, riskTag);
            }
        }
        return sortedList(scenes);
    }

    private List<String> targetIssueScenes(List<String> issueScenes) {
        LinkedHashSet<String> scenes = new LinkedHashSet<String>();
        if (issueScenes != null) {
            for (String issueScene : issueScenes) {
                addTargetIssueScene(scenes, issueScene);
            }
        }
        return sortedList(scenes);
    }

    private void addTargetIssueScene(Set<String> scenes, String candidate) {
        String normalized = trimToNull(candidate);
        if (normalized != null && TARGET_ISSUE_SCENES.contains(normalized)) {
            scenes.add(normalized);
        }
    }

    private List<String> sortedList(Set<String> scenes) {
        if (scenes == null || scenes.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> items = new ArrayList<String>(scenes);
        Collections.sort(items);
        return items;
    }

    private String resolveSqlFingerprint(StructureParseResponseVO structureParse, StructureParseRequest request) {
        if (StringUtils.hasText(structureParse.getSqlFingerprint())) {
            return structureParse.getSqlFingerprint().trim();
        }
        return SqlFingerprintUtils.fingerprint(request.getSqlText().trim());
    }

    private String resolveReportCode(StructureParseRequest request) {
        if (request == null || request.getCommentContext() == null || request.getCommentContext().isEmpty()) {
            return null;
        }
        return firstText(request.getCommentContext(), "report_code", "reportCode");
    }

    private String firstText(Map<String, Object> values, String firstKey, String secondKey) {
        if (values == null) {
            return null;
        }
        String first = objectText(values.get(firstKey));
        return first == null ? objectText(values.get(secondKey)) : first;
    }

    private String idempotencyKey(String tenantId, OptimizationTaskContextDTO context, String sqlFingerprint) {
        return tenantId
            + "|REWRITE|"
            + firstText(context.getSourceId(), context.getHistoryId(), context.getParseTaskId(), "NO_SOURCE")
            + "|"
            + firstText(sqlFingerprint, "NO_FINGERPRINT")
            + "|"
            + String.join(",", targetIssueScenes(context.getIssueScenes()));
    }

    private String idempotencyKey(String tenantId, OptimizationTaskSourceContext context, String sqlFingerprint) {
        return tenantId
            + "|REWRITE|"
            + firstText(context.getSourceId(), context.getHistoryId(), context.getParseTaskId(), "NO_SOURCE")
            + "|"
            + firstText(sqlFingerprint, "NO_FINGERPRINT")
            + "|"
            + String.join(",", targetIssueScenes(context.getIssueScenes()));
    }

    private String taskIdFor(String key) {
        return "rewrite-task-" + UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String recommendationIdFor(String key) {
        return "rewrite-reco-" + UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private List<String> readAppliedRules(OptimizationTaskSuggestion suggestion) {
        String content = artifactContent(suggestion, "REWRITE_RULE_TRACE", "appliedRules");
        if (!StringUtils.hasText(content)) {
            return Collections.emptyList();
        }
        try {
            List<String> values = objectMapper.readValue(content, STRING_LIST_TYPE);
            List<String> rules = new ArrayList<String>();
            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    rules.add(value.trim());
                }
            }
            return rules;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String artifactContent(OptimizationTaskSuggestion suggestion, String category, String name) {
        if (suggestion == null || suggestion.getArtifacts() == null) {
            return null;
        }
        for (OptimizationTaskArtifact artifact : suggestion.getArtifacts()) {
            if (artifact == null) {
                continue;
            }
            if (category.equals(artifact.getCategory()) && name.equals(artifact.getName())) {
                return artifact.getContent();
            }
        }
        return null;
    }

    private String buildSummary(List<String> targetIssueScenes, boolean safeRewriteAvailable) {
        String prefix = safeRewriteAvailable
            ? "已从解析问题生成安全改写推荐："
            : "已从解析问题生成人工评审改写推荐：";
        return prefix + String.join(", ", targetIssueScenes);
    }

    private String buildReason(OptimizationTaskSuggestion suggestion,
                               List<String> targetIssueScenes,
                               List<String> appliedRules,
                               boolean safeRewriteAvailable) {
        StringBuilder builder = new StringBuilder();
        builder.append("解析问题场景已匹配自动改写评审：")
            .append(String.join(", ", targetIssueScenes))
            .append("。");
        if (safeRewriteAvailable) {
            builder.append("已应用保守改写规则：")
                .append(String.join(", ", appliedRules))
                .append("。");
        } else {
            builder.append("缺少额外元数据时没有保守改写规则可安全应用，因此推荐保留原始 SQL。");
        }
        if (StringUtils.hasText(suggestion.getSummary())) {
            builder.append(suggestion.getSummary());
        }
        return builder.toString();
    }

    private String buildExpectedGain(OptimizationTaskSuggestion suggestion, boolean safeRewriteAvailable) {
        if (!safeRewriteAvailable) {
            return "已识别需要人工评审的目标；安全改写获批前不承诺自动性能收益。";
        }
        if (suggestion.getBenefits() != null && !suggestion.getBenefits().isEmpty()) {
            return suggestion.getBenefits().get(0).getSummary();
        }
        return "可用于校验的安全语法改写候选已生成。";
    }

    private BenefitLevel resolveBenefitLevel(OptimizationTaskSuggestion suggestion, boolean safeRewriteAvailable) {
        if (!safeRewriteAvailable || suggestion == null || suggestion.getConfidenceScore() == null) {
            return BenefitLevel.LOW;
        }
        int confidence = suggestion.getConfidenceScore().intValue();
        if (confidence >= 70) {
            return BenefitLevel.HIGH;
        }
        if (confidence >= 40) {
            return BenefitLevel.MEDIUM;
        }
        return BenefitLevel.LOW;
    }

    private String buildRiskSummary(OptimizationTaskSuggestion suggestion,
                                    List<String> targetIssueScenes,
                                    boolean safeRewriteAvailable) {
        StringBuilder builder = new StringBuilder();
        if (safeRewriteAvailable) {
            builder.append("批准前请使用原始结果集校验候选 SQL。");
        } else {
            builder.append("未匹配安全改写规则；推荐 SQL 为原始 SQL，需要人工处理。");
        }
        if (targetIssueScenes.contains("SELECT_STAR")) {
            builder.append(" 该路径无法保证列元数据，因此不会自动展开 SELECT_STAR。");
        }
        if (suggestion.getRisks() != null && !suggestion.getRisks().isEmpty()) {
            builder.append(" Worker 风险：");
            List<String> riskSummaries = new ArrayList<String>();
            for (OptimizationTaskRisk risk : suggestion.getRisks()) {
                if (risk != null && StringUtils.hasText(risk.getSummary())) {
                    riskSummaries.add(risk.getCategory() + "=" + risk.getSummary());
                }
            }
            builder.append(String.join("; ", riskSummaries));
        }
        return builder.toString();
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first.trim() : trimToNull(second);
    }

    private String firstText(String first, String second, String third) {
        return firstText(firstText(first, second), third);
    }

    private String firstText(String first, String second, String third, String fallback) {
        return firstText(firstText(first, second, third), fallback);
    }

    private String objectText(Object value) {
        return value == null ? null : trimToNull(String.valueOf(value));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
