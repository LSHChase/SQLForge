package com.company.sqloptimization.domain.rewrite.recommendation;

import com.company.sqloptimization.domain.rewrite.cost.CostBasedRewriteSelectionReport;
import com.company.sqloptimization.domain.rewrite.cost.RewriteCostEstimate;
import com.company.sqloptimization.domain.rewrite.parser.ParserMetadataTag;
import com.company.sqloptimization.domain.rewrite.parser.ParserStackFusionReport;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDag;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockNode;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteCandidate;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlan;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteRuleType;
import com.company.sqloptimization.domain.rewrite.rule.RuleConflictResolutionReport;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceCheck;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceReport;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceStatus;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class RewriteRecommendationGenerator {

    private static final Pattern COUNT_DISTINCT_PATTERN =
        Pattern.compile("(?is)\\bCOUNT\\s*\\(\\s*DISTINCT\\b|COUNT_DISTINCT\\s*\\(");
    private static final Pattern TIME_WINDOW_PATTERN =
        Pattern.compile("(?is)\\b(DTE|DT|BIZ_DATE|QUERY_DATE|DATE_DAY|DAY_ID|TRADE_DATE)\\b\\s*(=|>=|<=|>|<|IN|BETWEEN)");
    private static final Pattern ORG_LEVEL_PATTERN =
        Pattern.compile("(?is)\\b(BRANCH_ORG_NO|ORG_LEVEL2_NO|ORG_NO|机构|分行)\\b");

    public RewriteRecommendationReport generate(String normalizedSql,
                                                QueryBlockDag dag,
                                                RelationalRewritePlan plan,
                                                SemanticEquivalenceReport semanticReport,
                                                CostBasedRewriteSelectionReport costReport,
                                                RuleConflictResolutionReport ruleReport,
                                                ParserStackFusionReport parserReport) {
        if (plan == null || plan.getCandidates().isEmpty()) {
            return emptyReport(sourceSchemaVersion(plan, parserReport), "NO_REWRITE_CANDIDATE");
        }
        List<MutableRecommendation> mutable = new ArrayList<MutableRecommendation>();
        List<String> automationFiltered = new ArrayList<String>();
        List<String> manualReviewCandidates = new ArrayList<String>();
        Map<String, Object> weights = rankingWeights();
        for (RelationalRewriteCandidate candidate : plan.getCandidates()) {
            RewriteCostEstimate costEstimate = costEstimate(costReport, candidate.getCandidateId());
            List<SemanticEquivalenceCheck> checks = semanticChecks(semanticReport, candidate.getCandidateId());
            double confidence = confidence(candidate, checks, parserReport);
            boolean countDistinct = containsCountDistinct(candidate, checks);
            boolean timeWindowReport = isTimeWindowReport(normalizedSql);
            String riskLevel = riskLevel(confidence, countDistinct, ruleReport, checks);
            List<String> reviewRequirements = reviewRequirements(
                confidence,
                countDistinct,
                timeWindowReport,
                ruleReport
            );
            if (confidence < 0.90) {
                automationFiltered.add(candidate.getCandidateId());
            }
            if (candidate.isManualReviewRequired() || !reviewRequirements.isEmpty()) {
                manualReviewCandidates.add(candidate.getCandidateId());
            }
            ScoreComponents scoreComponents = scoreComponents(
                candidate,
                costEstimate,
                confidence,
                riskLevel,
                parserReport
            );
            mutable.add(new MutableRecommendation(
                recommendationId(candidate),
                candidate,
                confidence,
                severity(candidate, scoreComponents.performanceGain),
                beforeSummary(candidate, dag, parserReport),
                afterSummary(candidate),
                transformations(candidate, parserReport),
                equivalenceProof(candidate, checks, normalizedSql, countDistinct, timeWindowReport),
                performanceEstimate(candidate, costEstimate, riskLevel, scoreComponents.performanceGain),
                executableSql(candidate, dag),
                scoreComponents,
                false,
                candidate.isManualReviewRequired() || !reviewRequirements.isEmpty(),
                reviewRequirements,
                attributes(candidate, costEstimate, confidence, riskLevel)
            ));
        }
        Collections.sort(mutable, new Comparator<MutableRecommendation>() {
            @Override
            public int compare(MutableRecommendation left, MutableRecommendation right) {
                int score = Double.compare(right.scoreComponents.score, left.scoreComponents.score);
                if (score != 0) {
                    return score;
                }
                int costRank = Integer.valueOf(left.costRank()).compareTo(Integer.valueOf(right.costRank()));
                if (costRank != 0) {
                    return costRank;
                }
                return left.candidate.getCandidateId().compareTo(right.candidate.getCandidateId());
            }
        });

        List<RewriteRecommendation> recommendations = new ArrayList<RewriteRecommendation>();
        int rank = 1;
        for (MutableRecommendation item : mutable) {
            recommendations.add(item.toImmutable(rank));
            rank++;
        }

        LinkedHashMap<String, Object> attributes = baseAttributes();
        attributes.put("recommendationCount", Integer.valueOf(recommendations.size()));
        attributes.put("selectedRecommendationId", recommendations.isEmpty() ? "" : recommendations.get(0).getRewriteId());
        attributes.put("generationStatus", recommendations.isEmpty() ? "NO_RECOMMENDATION" : "RECOMMENDATION_GENERATED");
        attributes.put("automationFilteredCandidateCount", Integer.valueOf(automationFiltered.size()));
        attributes.put("manualReviewCandidateCount", Integer.valueOf(manualReviewCandidates.size()));
        attributes.put("sourceSqlBoundary", "STATIC_PARSE_PROFILE_AND_RELNODE_SURROGATE");
        return new RewriteRecommendationReport(
            RewriteRecommendationReport.SCHEMA_VERSION,
            sourceSchemaVersion(plan, parserReport),
            recommendations.isEmpty() ? "NO_RECOMMENDATION" : "RECOMMENDATION_GENERATED",
            recommendations.isEmpty() ? "" : recommendations.get(0).getRewriteId(),
            recommendations,
            automationFiltered,
            manualReviewCandidates,
            rankingFactors(),
            weights,
            attributes
        );
    }

    private RewriteRecommendationReport emptyReport(String sourceSchemaVersion, String reason) {
        LinkedHashMap<String, Object> attributes = baseAttributes();
        attributes.put("recommendationCount", Integer.valueOf(0));
        attributes.put("selectedRecommendationId", "");
        attributes.put("generationStatus", "NO_RECOMMENDATION");
        attributes.put("reason", reason);
        return new RewriteRecommendationReport(
            RewriteRecommendationReport.SCHEMA_VERSION,
            sourceSchemaVersion,
            "NO_RECOMMENDATION",
            "",
            Collections.<RewriteRecommendation>emptyList(),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            rankingFactors(),
            rankingWeights(),
            attributes
        );
    }

    private String sourceSchemaVersion(RelationalRewritePlan plan, ParserStackFusionReport parserReport) {
        if (parserReport != null && !parserReport.getSchemaVersion().isEmpty()) {
            return parserReport.getSchemaVersion();
        }
        return plan == null ? "" : plan.getSchemaVersion();
    }

    private RewriteCostEstimate costEstimate(CostBasedRewriteSelectionReport costReport, String candidateId) {
        if (costReport == null) {
            return null;
        }
        for (RewriteCostEstimate estimate : costReport.getEstimates()) {
            if (estimate.getCandidateId().equals(candidateId)) {
                return estimate;
            }
        }
        return null;
    }

    private List<SemanticEquivalenceCheck> semanticChecks(SemanticEquivalenceReport semanticReport, String candidateId) {
        if (semanticReport == null || semanticReport.getChecks().isEmpty()) {
            return Collections.emptyList();
        }
        List<SemanticEquivalenceCheck> result = new ArrayList<SemanticEquivalenceCheck>();
        for (SemanticEquivalenceCheck check : semanticReport.getChecks()) {
            if (check.getCandidateId().equals(candidateId)) {
                result.add(check);
            }
        }
        return result;
    }

    private double confidence(RelationalRewriteCandidate candidate,
                              List<SemanticEquivalenceCheck> checks,
                              ParserStackFusionReport parserReport) {
        SemanticEquivalenceStatus weakest = weakestStatus(checks);
        double value;
        if (SemanticEquivalenceStatus.PROVED == weakest) {
            value = 0.95;
        } else if (SemanticEquivalenceStatus.CONDITIONALLY_PROVED == weakest) {
            value = 0.92;
        } else if (SemanticEquivalenceStatus.NEEDS_CONSTRAINTS == weakest) {
            value = 0.88;
        } else if (SemanticEquivalenceStatus.UNSUPPORTED == weakest) {
            value = 0.72;
        } else {
            value = 0.65;
        }
        if (containsCountDistinct(candidate, checks)) {
            value = Math.min(value, 0.91);
        }
        if (parserReport != null && parserReport.hasMetadataTag("FANRUAN")
            && SemanticEquivalenceStatus.NEEDS_CONSTRAINTS != weakest) {
            value = Math.min(0.95, value + 0.01);
        }
        return round(value);
    }

    private SemanticEquivalenceStatus weakestStatus(List<SemanticEquivalenceCheck> checks) {
        if (checks.isEmpty()) {
            return null;
        }
        SemanticEquivalenceStatus weakest = SemanticEquivalenceStatus.PROVED;
        for (SemanticEquivalenceCheck check : checks) {
            if (statusRank(check.getStatus()) > statusRank(weakest)) {
                weakest = check.getStatus();
            }
        }
        return weakest;
    }

    private int statusRank(SemanticEquivalenceStatus status) {
        if (SemanticEquivalenceStatus.PROVED == status) {
            return 1;
        }
        if (SemanticEquivalenceStatus.CONDITIONALLY_PROVED == status) {
            return 2;
        }
        if (SemanticEquivalenceStatus.NEEDS_CONSTRAINTS == status) {
            return 3;
        }
        if (SemanticEquivalenceStatus.UNSUPPORTED == status) {
            return 4;
        }
        return 5;
    }

    private String riskLevel(double confidence,
                             boolean countDistinct,
                             RuleConflictResolutionReport ruleReport,
                             List<SemanticEquivalenceCheck> checks) {
        if (confidence < 0.75 || hasUnsupportedCheck(checks)) {
            return "HIGH";
        }
        if (confidence < 0.90 || countDistinct || hasRuleConflict(ruleReport)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private boolean hasUnsupportedCheck(List<SemanticEquivalenceCheck> checks) {
        for (SemanticEquivalenceCheck check : checks) {
            if (SemanticEquivalenceStatus.UNSUPPORTED == check.getStatus()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRuleConflict(RuleConflictResolutionReport ruleReport) {
        return ruleReport != null && !ruleReport.getConflicts().isEmpty();
    }

    private List<String> reviewRequirements(double confidence,
                                            boolean countDistinct,
                                            boolean timeWindowReport,
                                            RuleConflictResolutionReport ruleReport) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        if (confidence < 0.90) {
            result.add("CONFIDENCE_BELOW_0_90_HINT_ONLY");
        }
        if (countDistinct) {
            result.add("COUNT_DISTINCT_SEMANTIC_CHANGE_MANUAL_REVIEW_REQUIRED");
        }
        if (timeWindowReport) {
            result.add("TIME_WINDOW_REPORT_SAMPLE_COMPARE_1_TO_2_ORGS_REQUIRED");
        }
        if (hasRuleConflict(ruleReport)) {
            result.add("RULE_CONFLICT_RESOLUTION_REVIEW_REQUIRED");
        }
        result.add("PRODUCTION_AUTO_APPLY_DISABLED_BY_GOVERNANCE");
        return new ArrayList<String>(result);
    }

    private ScoreComponents scoreComponents(RelationalRewriteCandidate candidate,
                                            RewriteCostEstimate costEstimate,
                                            double confidence,
                                            String riskLevel,
                                            ParserStackFusionReport parserReport) {
        double performanceGain = performanceGain(candidate, costEstimate);
        double riskAvoidance = 1.0 - riskValue(riskLevel);
        double readabilityImprovement = readabilityImprovement(candidate, parserReport);
        double score = 0.40 * performanceGain
            + 0.30 * confidence
            + 0.20 * riskAvoidance
            + 0.10 * readabilityImprovement;
        return new ScoreComponents(
            round(performanceGain),
            round(confidence),
            round(riskAvoidance),
            round(readabilityImprovement),
            round(score)
        );
    }

    private double performanceGain(RelationalRewriteCandidate candidate, RewriteCostEstimate costEstimate) {
        int sourceCount = sourceBlockCount(candidate);
        double gain = sourceCount <= 1 ? 0.35 : (sourceCount - 1.0) / sourceCount;
        if (costEstimate != null && costEstimate.isParetoOptimal()) {
            gain += 0.05;
        }
        if (costEstimate != null && costEstimate.isSelected()) {
            gain += 0.05;
        }
        if (RelationalRewriteRuleType.HORIZONTAL_UNNESTING == candidate.getRuleType()) {
            gain += 0.05;
        }
        return Math.min(1.0, gain);
    }

    private double riskValue(String riskLevel) {
        if ("LOW".equals(riskLevel)) {
            return 0.15;
        }
        if ("MEDIUM".equals(riskLevel)) {
            return 0.45;
        }
        return 0.75;
    }

    private double readabilityImprovement(RelationalRewriteCandidate candidate, ParserStackFusionReport parserReport) {
        double value = 0.35 + Math.max(0, sourceBlockCount(candidate) - 1) * 0.12;
        if (RelationalRewriteRuleType.VERTICAL_FOLDING == candidate.getRuleType()) {
            value += 0.12;
        }
        if (parserReport != null && parserReport.hasMetadataTag("FANRUAN")) {
            value += 0.10;
        }
        return Math.min(1.0, value);
    }

    private String severity(RelationalRewriteCandidate candidate, double performanceGain) {
        if (performanceGain >= 0.60 || sourceBlockCount(candidate) >= 3) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private String beforeSummary(RelationalRewriteCandidate candidate,
                                 QueryBlockDag dag,
                                 ParserStackFusionReport parserReport) {
        int blockCount = dag == null ? sourceBlockCount(candidate) : Math.max(1, dag.getBlocks().size());
        int nestedDepth = Math.max(1, maxNestedDepth(parserReport));
        return sourceBlockCount(candidate) + "次候选查询块扫描，" + blockCount + "个静态查询块，嵌套深度"
            + nestedDepth;
    }

    private int maxNestedDepth(ParserStackFusionReport parserReport) {
        if (parserReport == null) {
            return 1;
        }
        int max = 1;
        for (ParserMetadataTag tag : parserReport.getMetadataTags()) {
            max = Math.max(max, tag.getNestedDepth());
        }
        return max;
    }

    private String afterSummary(RelationalRewriteCandidate candidate) {
        if (RelationalRewriteRuleType.HORIZONTAL_UNNESTING == candidate.getRuleType()) {
            return "1次扩展GROUP BY聚合，减少多路LEFT JOIN聚合右表";
        }
        if (RelationalRewriteRuleType.VERTICAL_FOLDING == candidate.getRuleType()) {
            return "1次共享CTE扫描，使用CASE/FILTER承载多指标聚合";
        }
        return "1次共享CTE扫描，引用点保留补偿谓词";
    }

    private List<RewriteTransformation> transformations(RelationalRewriteCandidate candidate,
                                                        ParserStackFusionReport parserReport) {
        List<RewriteTransformation> result = new ArrayList<RewriteTransformation>();
        if (RelationalRewriteRuleType.HORIZONTAL_UNNESTING == candidate.getRuleType()) {
            result.add(transformation(
                "UNNEST",
                prefixedSources("LEFT JOIN ", candidate.getSourceBlockIds()),
                targetName(candidate),
                "将多个同级 LEFT JOIN 聚合右表下推合并为一个扩展 GROUP BY。"
            ));
        } else {
            result.add(transformation(
                "MERGE",
                candidate.getSourceBlockIds(),
                targetName(candidate),
                "将等价查询块合并为共享 CTE，并在引用点保留补偿谓词。"
            ));
        }
        if (RelationalRewriteRuleType.VERTICAL_FOLDING == candidate.getRuleType()) {
            result.add(transformation(
                "PUSH_DOWN",
                candidate.getCompensationPredicates(),
                targetName(candidate),
                "把日期、阈值或分层差异下推到 CASE/FILTER 聚合表达式。"
            ));
        }
        if (parserReport != null && parserReport.hasMetadataTag("FANRUAN")) {
            result.add(transformation(
                "INLINE",
                candidate.getSourceBlockIds(),
                targetName(candidate),
                "将 BI 工具生成的中间别名折叠为关系代数子图候选。"
            ));
        }
        return result;
    }

    private RewriteTransformation transformation(String type,
                                                 List<String> source,
                                                 String target,
                                                 String description) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("representation", "RELNODE_NORMALIZED_SURROGATE");
        return new RewriteTransformation(type, source, target, description, attributes);
    }

    private List<String> prefixedSources(String prefix, List<String> values) {
        List<String> result = new ArrayList<String>();
        for (String value : values) {
            result.add(prefix + value);
        }
        return result;
    }

    private String targetName(RelationalRewriteCandidate candidate) {
        if (RelationalRewriteRuleType.HORIZONTAL_UNNESTING == candidate.getRuleType()) {
            return "horizontal_unnest_" + normalizedPrimaryBlockId(candidate);
        }
        if (RelationalRewriteRuleType.VERTICAL_FOLDING == candidate.getRuleType()) {
            return "vertical_fold_" + normalizedPrimaryBlockId(candidate);
        }
        return "common_block_" + normalizedPrimaryBlockId(candidate);
    }

    private String normalizedPrimaryBlockId(RelationalRewriteCandidate candidate) {
        String blockId = candidate == null ? "" : candidate.getPrimaryBlockId();
        return blockId == null || blockId.trim().isEmpty()
            ? "unresolved"
            : blockId.toLowerCase(Locale.ROOT);
    }

    private RewriteEquivalenceProof equivalenceProof(RelationalRewriteCandidate candidate,
                                                     List<SemanticEquivalenceCheck> checks,
                                                     String normalizedSql,
                                                     boolean countDistinct,
                                                     boolean timeWindowReport) {
        LinkedHashSet<String> dimensions = new LinkedHashSet<String>();
        dimensions.add("ROW_COUNT");
        dimensions.add("COLUMN_VALUES");
        if (RelationalRewriteRuleType.VERTICAL_FOLDING == candidate.getRuleType()
            || RelationalRewriteRuleType.HORIZONTAL_UNNESTING == candidate.getRuleType()) {
            dimensions.add("AGGREGATION_RESULTS");
        }
        if (hasNullSemantics(checks)) {
            dimensions.add("NULL_HANDLING");
        }

        LinkedHashSet<String> edgeCases = new LinkedHashSet<String>();
        edgeCases.addAll(candidate.getSemanticRisks());
        for (SemanticEquivalenceCheck check : checks) {
            edgeCases.addAll(check.getRisks());
        }
        if (countDistinct) {
            edgeCases.add("COUNT DISTINCT 语义变更必须人工审核，确认去重域、NULL 和 CASE/FILTER 口径。");
        }
        if (timeWindowReport) {
            edgeCases.add("时间窗口类报表需抽样比对 1-2 个机构，覆盖期初、期末和净增口径。");
        }
        if (ORG_LEVEL_PATTERN.matcher(RewriteRecommendationCollections.text(normalizedSql)).find()) {
            edgeCases.add("branch_org_no = org_level2_no 时客户数跨层去重差异需通过 UNION ALL/去重域证明。");
        }

        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("confidenceBasis", "SEMANTIC_EQUIVALENCE_STATUS_AND_STATIC_REWRITE_EVIDENCE");
        attributes.put("smtSolverStatus", "NOT_INTEGRATED");
        attributes.put("proofBoundary", "STATIC_PROOF_OBLIGATIONS_NOT_RESULT_DIFF");
        return new RewriteEquivalenceProof(
            proofMethod(candidate),
            new ArrayList<String>(dimensions),
            new ArrayList<String>(edgeCases),
            proofEvidence(candidate, checks),
            attributes
        );
    }

    private boolean hasNullSemantics(List<SemanticEquivalenceCheck> checks) {
        for (SemanticEquivalenceCheck check : checks) {
            if (!check.getNullSemantics().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private String proofMethod(RelationalRewriteCandidate candidate) {
        if (RelationalRewriteRuleType.CSE_ELIMINATION == candidate.getRuleType()) {
            return "STRUCTURAL_HASH + PREDICATE_SUBSUMPTION";
        }
        if (RelationalRewriteRuleType.VERTICAL_FOLDING == candidate.getRuleType()) {
            return "STRUCTURAL_HASH + PREDICATE_SUBSUMPTION + AGGREGATION_DECOMPOSITION";
        }
        return "JOIN_KEY_EQUIVALENCE + GROUP_BY_EXTENSION + NULL_EXTENSION_PRESERVATION";
    }

    private List<Map<String, Object>> proofEvidence(RelationalRewriteCandidate candidate,
                                                    List<SemanticEquivalenceCheck> checks) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        LinkedHashMap<String, Object> candidateEvidence = new LinkedHashMap<String, Object>();
        candidateEvidence.put("type", "RELATIONAL_REWRITE_CANDIDATE");
        candidateEvidence.put("candidateId", candidate.getCandidateId());
        candidateEvidence.put("ruleType", candidate.getRuleType().name());
        candidateEvidence.put("replacementForm", candidate.getReplacementForm());
        result.add(candidateEvidence);
        for (SemanticEquivalenceCheck check : checks) {
            LinkedHashMap<String, Object> checkEvidence = new LinkedHashMap<String, Object>();
            checkEvidence.put("type", "SEMANTIC_EQUIVALENCE_CHECK");
            checkEvidence.put("checkType", check.getCheckType().name());
            checkEvidence.put("status", check.getStatus().name());
            checkEvidence.put("method", check.getAttributes().get("method"));
            result.add(checkEvidence);
        }
        return result;
    }

    private RewritePerformanceEstimate performanceEstimate(RelationalRewriteCandidate candidate,
                                                          RewriteCostEstimate costEstimate,
                                                          String riskLevel,
                                                          double performanceGain) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("performanceGainScore", Double.valueOf(round(performanceGain)));
        attributes.put("costEstimateId", costEstimate == null ? "" : costEstimate.getEstimateId());
        if (costEstimate != null) {
            attributes.put("abstractCostVector", costEstimate.getCostVector().toMap());
            attributes.put("paretoOptimal", Boolean.valueOf(costEstimate.isParetoOptimal()));
        }
        attributes.put("claimBoundary", "STATIC_ESTIMATE_NOT_REAL_EXECUTION_GAIN");
        return new RewritePerformanceEstimate(
            sourceBlockCount(candidate) + "x -> 1x",
            estimatedSpeedup(sourceBlockCount(candidate)),
            memoryImpact(candidate),
            riskLevel,
            attributes
        );
    }

    private String estimatedSpeedup(int sourceBlockCount) {
        if (sourceBlockCount >= 10) {
            return "5-10x (取决于数据量和集群规模)";
        }
        if (sourceBlockCount >= 3) {
            return "2-5x (取决于数据量和集群规模)";
        }
        return "1.5-2x (取决于数据量和集群规模)";
    }

    private String memoryImpact(RelationalRewriteCandidate candidate) {
        if (RelationalRewriteRuleType.VERTICAL_FOLDING == candidate.getRuleType()) {
            return "+20% (CASE 表达式和中间聚合状态)";
        }
        if (RelationalRewriteRuleType.HORIZONTAL_UNNESTING == candidate.getRuleType()) {
            return "+25% (扩展 GROUP BY 和 hash table 状态)";
        }
        return "+12% (CTE 物化和共享引用状态)";
    }

    private String executableSql(RelationalRewriteCandidate candidate, QueryBlockDag dag) {
        QueryBlockNode block = primaryBlock(candidate, dag);
        String target = targetName(candidate);
        String selectList = executableSelectList(block);
        String fromClause = block == null || text(block.getFromClause()).isEmpty() ? "UNKNOWN_SOURCE" : block.getFromClause();
        String whereClause = block == null ? "" : text(block.getWhereClause());
        String groupBy = block == null || block.getGroupBy().isEmpty() ? "" : join(block.getGroupBy(), ", ");
        StringBuilder builder = new StringBuilder();
        builder.append("WITH ").append(target).append(" AS (\n");
        builder.append("  SELECT ").append(selectList).append("\n");
        builder.append("  FROM ").append(fromClause).append("\n");
        if (!whereClause.isEmpty()) {
            builder.append("  WHERE ").append(whereClause).append("\n");
        }
        if (!groupBy.isEmpty()) {
            builder.append("  GROUP BY ").append(groupBy).append("\n");
        }
        builder.append(")\n");
        builder.append("SELECT *\n");
        builder.append("FROM ").append(target);
        return builder.toString();
    }

    private QueryBlockNode primaryBlock(RelationalRewriteCandidate candidate, QueryBlockDag dag) {
        if (dag == null || candidate == null) {
            return null;
        }
        QueryBlockNode primary = dag.getBlock(candidate.getPrimaryBlockId());
        if (primary != null) {
            return primary;
        }
        for (String blockId : candidate.getSourceBlockIds()) {
            QueryBlockNode block = dag.getBlock(blockId);
            if (block != null) {
                return block;
            }
        }
        return null;
    }

    private String executableSelectList(QueryBlockNode block) {
        if (block == null) {
            return "COUNT(*) AS rewrite_row_count";
        }
        if (!block.getGroupBy().isEmpty()) {
            return join(block.getGroupBy(), ", ") + ", COUNT(*) AS rewrite_row_count";
        }
        if (!block.getOutputColumns().isEmpty()) {
            return join(block.getOutputColumns(), ", ");
        }
        return "COUNT(*) AS rewrite_row_count";
    }

    private LinkedHashMap<String, Object> attributes(RelationalRewriteCandidate candidate,
                                                    RewriteCostEstimate costEstimate,
                                                    double confidence,
                                                    String riskLevel) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("sourceCandidateId", candidate.getCandidateId());
        attributes.put("ruleType", candidate.getRuleType().name());
        attributes.put("confidence", Double.valueOf(confidence));
        attributes.put("riskLevel", riskLevel);
        attributes.put("autoApplyAllowed", Boolean.FALSE);
        attributes.put("automationDisposition", confidence < 0.90 ? "HINT_ONLY" : "MANUAL_REVIEW_GATE_REQUIRED");
        attributes.put("sqlGenerationMethod", "STATIC_RELNODE_SURROGATE");
        attributes.put("calciteRelToSqlConverterStatus", "NOT_INVOKED");
        attributes.put("executableSqlBoundary", "STATIC_TEMPLATE_REQUIRES_CALCITE_AND_HETU_VALIDATION");
        attributes.put("costRank", Integer.valueOf(costEstimate == null ? Integer.MAX_VALUE : costEstimate.getRank()));
        return attributes;
    }

    private boolean containsCountDistinct(RelationalRewriteCandidate candidate, List<SemanticEquivalenceCheck> checks) {
        StringBuilder builder = new StringBuilder();
        builder.append(candidate.getReplacementForm()).append(' ');
        builder.append(candidate.getPreconditions()).append(' ');
        builder.append(candidate.getSemanticRisks()).append(' ');
        builder.append(candidate.getEvidence()).append(' ');
        for (SemanticEquivalenceCheck check : checks) {
            builder.append(check.getPreconditions()).append(' ');
            builder.append(check.getProofObligations()).append(' ');
            builder.append(check.getRisks()).append(' ');
            builder.append(check.getOriginalExpression()).append(' ');
        }
        return COUNT_DISTINCT_PATTERN.matcher(builder.toString()).find()
            || builder.toString().toUpperCase(Locale.ROOT).contains("COUNT_DISTINCT");
    }

    private boolean isTimeWindowReport(String normalizedSql) {
        return TIME_WINDOW_PATTERN.matcher(RewriteRecommendationCollections.text(normalizedSql)).find();
    }

    private Map<String, Object> rankingWeights() {
        LinkedHashMap<String, Object> weights = new LinkedHashMap<String, Object>();
        weights.put("performanceGainWeight", Double.valueOf(0.40));
        weights.put("confidenceWeight", Double.valueOf(0.30));
        weights.put("riskAvoidanceWeight", Double.valueOf(0.20));
        weights.put("readabilityImprovementWeight", Double.valueOf(0.10));
        weights.put("profile", "FINANCIAL_PRODUCTION_RECOMMENDED");
        return weights;
    }

    private List<Map<String, Object>> rankingFactors() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        result.add(rankingFactor("performance_gain", 0.40, "性能收益优先"));
        result.add(rankingFactor("confidence", 0.30, "等价性确信度优先，避免数据错误"));
        result.add(rankingFactor("one_minus_risk_level", 0.20, "风险厌恶"));
        result.add(rankingFactor("readability_improvement", 0.10, "可维护性改善"));
        return result;
    }

    private Map<String, Object> rankingFactor(String factor, double weight, String description) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("factor", factor);
        result.put("weight", Double.valueOf(weight));
        result.put("description", description);
        return result;
    }

    private LinkedHashMap<String, Object> baseAttributes() {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("source", "REWRITE_CORE_STATIC_REPORTS");
        attributes.put("runtimeBoundary", "NO_SQL_EXECUTION");
        attributes.put("pageImpact", "NO_FRONTEND_PAGE_CHANGE");
        attributes.put("autoApplyAllowed", Boolean.FALSE);
        attributes.put("recommendationAlgorithm", "WEIGHTED_REWRITE_RECOMMENDATION_SCORE");
        attributes.put("scoreFormula", "0.4*performance_gain+0.3*confidence+0.2*(1-risk_level)+0.1*readability");
        attributes.put("confidenceAutomationThreshold", Double.valueOf(0.90));
        attributes.put("countDistinctPolicy", "MANUAL_REVIEW_REQUIRED");
        attributes.put("timeWindowReportPolicy", "SAMPLE_COMPARE_1_TO_2_ORGS_REQUIRED");
        attributes.put("sqlGenerationBoundary", "STATIC_RELNODE_SURROGATE_NOT_REAL_CALCITE_RELTOSQL");
        return attributes;
    }

    private String recommendationId(RelationalRewriteCandidate candidate) {
        String basis = candidate.getCandidateId() + "|" + candidate.getRuleType().name() + "|"
            + candidate.getReplacementForm();
        return UUID.nameUUIDFromBytes(basis.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private int sourceBlockCount(RelationalRewriteCandidate candidate) {
        return Math.max(1, candidate.getSourceBlockIds().size());
    }

    private String join(List<String> values, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(delimiter);
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String text(String value) {
        return RewriteRecommendationCollections.text(value);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static final class ScoreComponents {
        private final double performanceGain;
        private final double confidence;
        private final double riskAvoidance;
        private final double readabilityImprovement;
        private final double score;

        private ScoreComponents(double performanceGain,
                                double confidence,
                                double riskAvoidance,
                                double readabilityImprovement,
                                double score) {
            this.performanceGain = performanceGain;
            this.confidence = confidence;
            this.riskAvoidance = riskAvoidance;
            this.readabilityImprovement = readabilityImprovement;
            this.score = score;
        }

        private Map<String, Object> toMap() {
            LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("performanceGain", Double.valueOf(performanceGain));
            result.put("confidence", Double.valueOf(confidence));
            result.put("riskAvoidance", Double.valueOf(riskAvoidance));
            result.put("readabilityImprovement", Double.valueOf(readabilityImprovement));
            result.put("score", Double.valueOf(score));
            result.put("weights", "0.4/0.3/0.2/0.1");
            return result;
        }
    }

    private static final class MutableRecommendation {
        private final String rewriteId;
        private final RelationalRewriteCandidate candidate;
        private final double confidence;
        private final String severity;
        private final String beforeSummary;
        private final String afterSummary;
        private final List<RewriteTransformation> transformations;
        private final RewriteEquivalenceProof equivalenceProof;
        private final RewritePerformanceEstimate performance;
        private final String executableSql;
        private final ScoreComponents scoreComponents;
        private final boolean autoApplyAllowed;
        private final boolean manualReviewRequired;
        private final List<String> reviewRequirements;
        private final Map<String, Object> attributes;

        private MutableRecommendation(String rewriteId,
                                      RelationalRewriteCandidate candidate,
                                      double confidence,
                                      String severity,
                                      String beforeSummary,
                                      String afterSummary,
                                      List<RewriteTransformation> transformations,
                                      RewriteEquivalenceProof equivalenceProof,
                                      RewritePerformanceEstimate performance,
                                      String executableSql,
                                      ScoreComponents scoreComponents,
                                      boolean autoApplyAllowed,
                                      boolean manualReviewRequired,
                                      List<String> reviewRequirements,
                                      Map<String, Object> attributes) {
            this.rewriteId = rewriteId;
            this.candidate = candidate;
            this.confidence = confidence;
            this.severity = severity;
            this.beforeSummary = beforeSummary;
            this.afterSummary = afterSummary;
            this.transformations = transformations;
            this.equivalenceProof = equivalenceProof;
            this.performance = performance;
            this.executableSql = executableSql;
            this.scoreComponents = scoreComponents;
            this.autoApplyAllowed = autoApplyAllowed;
            this.manualReviewRequired = manualReviewRequired;
            this.reviewRequirements = reviewRequirements;
            this.attributes = attributes;
        }

        private int costRank() {
            Object value = attributes.get("costRank");
            return value instanceof Number ? ((Number) value).intValue() : Integer.MAX_VALUE;
        }

        private RewriteRecommendation toImmutable(int rank) {
            return new RewriteRecommendation(
                rewriteId,
                candidate.getCandidateId(),
                confidence,
                "STRUCTURAL_OPTIMIZATION",
                severity,
                beforeSummary,
                afterSummary,
                transformations,
                equivalenceProof,
                performance,
                executableSql,
                scoreComponents.score,
                rank,
                autoApplyAllowed,
                manualReviewRequired,
                reviewRequirements,
                scoreComponents.toMap(),
                attributes
            );
        }
    }
}
