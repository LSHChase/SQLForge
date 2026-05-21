package com.company.sqloptimization.domain.rewrite.conformance;

import com.company.sqloptimization.domain.rewrite.cost.CostBasedRewriteSelectionReport;
import com.company.sqloptimization.domain.rewrite.parser.ParserStackFusionReport;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDag;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlan;
import com.company.sqloptimization.domain.rewrite.recommendation.RewriteRecommendation;
import com.company.sqloptimization.domain.rewrite.recommendation.RewriteRecommendationReport;
import com.company.sqloptimization.domain.rewrite.rule.RuleConflictResolutionReport;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceCheck;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceReport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class RewriteAlgorithmConformanceAnalyzer {

    public RewriteAlgorithmConformanceReport analyze(String normalizedSql,
                                                     QueryBlockDag dag,
                                                     RelationalRewritePlan plan,
                                                     SemanticEquivalenceReport semanticReport,
                                                     CostBasedRewriteSelectionReport costReport,
                                                     RuleConflictResolutionReport ruleReport,
                                                     ParserStackFusionReport parserReport,
                                                     RewriteRecommendationReport recommendationReport) {
        List<RewriteAlgorithmStage> stages = new ArrayList<RewriteAlgorithmStage>();
        stages.add(parseStage(normalizedSql, parserReport));
        stages.add(decompositionStage(dag));
        stages.add(ruleStage(ruleReport));
        stages.add(transformStage(plan));
        stages.add(verificationStage(semanticReport));
        stages.add(selectionStage(costReport));
        stages.add(generationStage(recommendationReport));
        stages.add(outputStage(recommendationReport));

        List<String> criticalGaps = criticalGaps(stages);
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("source", "REWRITE_CORE_ALGORITHM_CHAIN");
        attributes.put("runtimeBoundary", "NO_SQL_EXECUTION");
        attributes.put("pageImpact", "NO_FRONTEND_PAGE_CHANGE");
        attributes.put("autoApplyAllowed", Boolean.FALSE);
        attributes.put("algorithmSequence", Arrays.asList(
            "INPUT_PATHOLOGICAL_SQL",
            "PARSE_DUAL_STACK",
            "DECOMPOSE_QBDAG_STRUCTURAL_HASH",
            "IDENTIFY_RULES",
            "TRANSFORM_RELATIONAL_ALGEBRA",
            "VERIFY_EQUIVALENCE",
            "SELECT_COST_PARETO",
            "GENERATE_SQL_AND_REPORT",
            "OUTPUT_BUNDLE"
        ));
        attributes.put("stageCount", Integer.valueOf(stages.size()));
        attributes.put("criticalGapCount", Integer.valueOf(criticalGaps.size()));
        attributes.put("currentConclusion", conclusion(criticalGaps, recommendationReport));
        return new RewriteAlgorithmConformanceReport(
            RewriteAlgorithmConformanceReport.SCHEMA_VERSION,
            sourceSchemaVersion(recommendationReport, parserReport, plan),
            algorithmStatus(criticalGaps, recommendationReport),
            stages,
            criticalGaps,
            outputBundle(recommendationReport),
            attributes
        );
    }

    private RewriteAlgorithmStage parseStage(String normalizedSql, ParserStackFusionReport parserReport) {
        List<String> gaps = new ArrayList<String>();
        if (parserReport == null) {
            gaps.add("DUAL_STACK_FUSION_REPORT_MISSING");
        } else {
            if (!parserReport.hasMetadataTag("FANRUAN")
                && containsFanruanAlias(RewriteAlgorithmConformanceCollections.text(normalizedSql))) {
                gaps.add("BI_TOOL_ALIAS_PRESENT_BUT_METADATA_TAG_MISSING");
            }
            Object boundary = parserReport.getAttributes().get("relNodeBoundary");
            if (boundary == null) {
                boundary = parserReport.getAttributes().get("workflow");
            }
            if (String.valueOf(boundary).contains("SURROGATE")) {
                gaps.add("REAL_CALCITE_RELNODE_NOT_BUILT");
            }
        }
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("parserFusionStatus", parserReport == null ? "" : parserReport.getFusionStatus());
        attributes.put("parserRoleCount", Integer.valueOf(parserReport == null ? 0 : parserReport.getParserRoles().size()));
        attributes.put("metadataTagCount", Integer.valueOf(parserReport == null ? 0 : parserReport.getMetadataTags().size()));
        return stage(
            "ALG-01",
            "PARSE_DUAL_STACK",
            "Calcite SqlNode + JSqlParser AST -> 双栈融合",
            parserReport == null ? "MISSING" : "IMPLEMENTED_WITH_RELNODE_SURROGATE",
            gaps.isEmpty() ? "FULL" : "STATIC_SURROGATE",
            evidence("PARSER_STACK_FUSION", attributes),
            gaps,
            attributes
        );
    }

    private boolean containsFanruanAlias(String sql) {
        return sql.contains("分组和汇总") || sql.contains("Sub");
    }

    private RewriteAlgorithmStage decompositionStage(QueryBlockDag dag) {
        List<String> gaps = new ArrayList<String>();
        if (dag == null || dag.getBlocks().isEmpty()) {
            gaps.add("QBDAG_BLOCKS_MISSING");
        }
        if (dag != null && dag.getStructuralHashGroups().isEmpty()) {
            gaps.add("STRUCTURAL_HASH_GROUPS_EMPTY");
        }
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("blockCount", Integer.valueOf(dag == null ? 0 : dag.getBlocks().size()));
        attributes.put("structuralHashGroupCount", Integer.valueOf(dag == null ? 0 : dag.getStructuralHashGroups().size()));
        attributes.put("duplicateStructuralGroupCount", Integer.valueOf(dag == null ? 0 : dag.getDuplicateStructuralGroups().size()));
        attributes.put("rootBlockId", dag == null ? "" : dag.getRootBlockId());
        return stage(
            "ALG-02",
            "DECOMPOSE_QBDAG_STRUCTURAL_HASH",
            "查询块 DAG + 结构哈希 -> 识别重复/冗余模式",
            gaps.isEmpty() ? "IMPLEMENTED" : "IMPLEMENTED_WITH_LIMITS",
            gaps.isEmpty() ? "FULL" : "PARTIAL",
            evidence("QBDAG", attributes),
            gaps,
            attributes
        );
    }

    private RewriteAlgorithmStage ruleStage(RuleConflictResolutionReport ruleReport) {
        List<String> gaps = new ArrayList<String>();
        if (ruleReport == null || ruleReport.getMatchedRules().isEmpty()) {
            gaps.add("RULE_LIBRARY_NO_MATCH");
        }
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("matchedRuleCount", Integer.valueOf(ruleReport == null ? 0 : ruleReport.getMatchedRules().size()));
        attributes.put("selectedRuleIds", ruleReport == null ? Collections.emptyList() : ruleReport.getSelectedRuleIds());
        attributes.put("conflictCount", Integer.valueOf(ruleReport == null ? 0 : ruleReport.getConflicts().size()));
        attributes.put("resolutionStatus", ruleReport == null ? "" : ruleReport.getResolutionStatus());
        return stage(
            "ALG-03",
            "IDENTIFY_RULES",
            "模式匹配引擎 -> 命中规则库 {CSE, VerticalFold, HorizontalUnnest, ...}",
            gaps.isEmpty() ? "IMPLEMENTED" : "NO_STATIC_MATCH",
            gaps.isEmpty() ? "FULL" : "PARTIAL",
            evidence("RULE_CONFLICT_RESOLUTION", attributes),
            gaps,
            attributes
        );
    }

    private RewriteAlgorithmStage transformStage(RelationalRewritePlan plan) {
        List<String> gaps = new ArrayList<String>();
        if (plan == null || plan.getCandidates().isEmpty()) {
            gaps.add("RELATIONAL_REWRITE_CANDIDATES_EMPTY");
        }
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("candidateCount", Integer.valueOf(plan == null ? 0 : plan.getCandidates().size()));
        attributes.put("rewriteStatus", plan == null ? "" : plan.getAttributes().get("rewriteStatus"));
        attributes.put("candidateRules", plan == null ? Collections.emptyList() : plan.getAttributes().get("candidateRules"));
        return stage(
            "ALG-04",
            "TRANSFORM_RELATIONAL_ALGEBRA",
            "关系代数等价重写 -> 生成候选改写集合",
            gaps.isEmpty() ? "IMPLEMENTED" : "NO_CANDIDATE",
            gaps.isEmpty() ? "FULL" : "PARTIAL",
            evidence("RELATIONAL_REWRITE_PLAN", attributes),
            gaps,
            attributes
        );
    }

    private RewriteAlgorithmStage verificationStage(SemanticEquivalenceReport semanticReport) {
        List<String> gaps = new ArrayList<String>();
        if (semanticReport == null || semanticReport.getChecks().isEmpty()) {
            gaps.add("SEMANTIC_EQUIVALENCE_CHECKS_EMPTY");
        }
        if (semanticReport != null && hasUnintegratedSmt(semanticReport)) {
            gaps.add("SMT_SOLVER_NOT_INTEGRATED");
        }
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("semanticStatus", semanticReport == null ? "" : semanticReport.getStatus().name());
        attributes.put("checkCount", Integer.valueOf(semanticReport == null ? 0 : semanticReport.getChecks().size()));
        attributes.put("unverifiedCandidateIds", semanticReport == null
            ? Collections.emptyList()
            : semanticReport.getUnverifiedCandidateIds());
        attributes.put("smtSolverStatus", hasUnintegratedSmt(semanticReport) ? "NOT_INTEGRATED" : "STATIC_CHECKS_ONLY");
        return stage(
            "ALG-05",
            "VERIFY_EQUIVALENCE",
            "结构哈希 + SMT 求解 + 统计等价性 -> 过滤不安全改写",
            gaps.contains("SMT_SOLVER_NOT_INTEGRATED") ? "IMPLEMENTED_WITH_STATIC_SMT_BOUNDARY" : "IMPLEMENTED",
            gaps.isEmpty() ? "FULL" : "STATIC_SURROGATE",
            evidence("SEMANTIC_EQUIVALENCE_REPORT", attributes),
            gaps,
            attributes
        );
    }

    private boolean hasUnintegratedSmt(SemanticEquivalenceReport semanticReport) {
        if (semanticReport == null) {
            return false;
        }
        for (SemanticEquivalenceCheck check : semanticReport.getChecks()) {
            Object status = check.getAttributes().get("smtSolverStatus");
            if ("NOT_INTEGRATED".equals(String.valueOf(status))) {
                return true;
            }
        }
        return false;
    }

    private RewriteAlgorithmStage selectionStage(CostBasedRewriteSelectionReport costReport) {
        List<String> gaps = new ArrayList<String>();
        if (costReport == null || costReport.getEstimates().isEmpty()) {
            gaps.add("COST_ESTIMATES_EMPTY");
        }
        if (costReport != null && costReport.getParetoFrontierCandidateIds().isEmpty()) {
            gaps.add("PARETO_FRONTIER_EMPTY");
        }
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("selectionStatus", costReport == null ? "" : costReport.getSelectionStatus());
        attributes.put("selectedCandidateId", costReport == null ? "" : costReport.getSelectedCandidateId());
        attributes.put("estimateCount", Integer.valueOf(costReport == null ? 0 : costReport.getEstimates().size()));
        attributes.put("paretoFrontierCount", Integer.valueOf(costReport == null
            ? 0
            : costReport.getParetoFrontierCandidateIds().size()));
        return stage(
            "ALG-06",
            "SELECT_COST_PARETO",
            "帕累托代价模型 -> 选择最优改写",
            gaps.isEmpty() ? "IMPLEMENTED" : "NO_SELECTION",
            gaps.isEmpty() ? "FULL" : "PARTIAL",
            evidence("COST_BASED_SELECTION", attributes),
            gaps,
            attributes
        );
    }

    private RewriteAlgorithmStage generationStage(RewriteRecommendationReport recommendationReport) {
        List<String> gaps = new ArrayList<String>();
        if (recommendationReport == null || recommendationReport.getRecommendations().isEmpty()) {
            gaps.add("REWRITE_RECOMMENDATIONS_EMPTY");
        }
        if (recommendationReport != null
            && "STATIC_RELNODE_SURROGATE_NOT_REAL_CALCITE_RELTOSQL".equals(
                String.valueOf(recommendationReport.getAttributes().get("sqlGenerationBoundary")))) {
            gaps.add("REAL_CALCITE_RELTOSQL_NOT_INVOKED");
        }
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("generationStatus", recommendationReport == null ? "" : recommendationReport.getGenerationStatus());
        attributes.put("recommendationCount", Integer.valueOf(recommendationReport == null
            ? 0
            : recommendationReport.getRecommendations().size()));
        attributes.put("selectedRecommendationId", recommendationReport == null
            ? ""
            : recommendationReport.getSelectedRecommendationId());
        attributes.put("sqlGenerationBoundary", recommendationReport == null
            ? ""
            : recommendationReport.getAttributes().get("sqlGenerationBoundary"));
        return stage(
            "ALG-07",
            "GENERATE_SQL_AND_REPORT",
            "RelToSqlConverter -> 可执行 SQL + 推荐报告",
            gaps.contains("REAL_CALCITE_RELTOSQL_NOT_INVOKED") ? "IMPLEMENTED_WITH_SQL_SURROGATE" : "IMPLEMENTED",
            gaps.isEmpty() ? "FULL" : "STATIC_SURROGATE",
            evidence("REWRITE_RECOMMENDATION_REPORT", attributes),
            gaps,
            attributes
        );
    }

    private RewriteAlgorithmStage outputStage(RewriteRecommendationReport recommendationReport) {
        List<String> gaps = new ArrayList<String>();
        Map<String, Object> bundle = outputBundle(recommendationReport);
        if (!Boolean.TRUE.equals(bundle.get("hasRewriteSql"))) {
            gaps.add("OUTPUT_REWRITE_SQL_MISSING");
        }
        if (!Boolean.TRUE.equals(bundle.get("hasEquivalenceProof"))) {
            gaps.add("OUTPUT_EQUIVALENCE_PROOF_MISSING");
        }
        if (!Boolean.TRUE.equals(bundle.get("hasPerformanceEstimate"))) {
            gaps.add("OUTPUT_PERFORMANCE_ESTIMATE_MISSING");
        }
        if (!Boolean.TRUE.equals(bundle.get("hasRiskLevel"))) {
            gaps.add("OUTPUT_RISK_LEVEL_MISSING");
        }
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>(bundle);
        return stage(
            "ALG-08",
            "OUTPUT_BUNDLE",
            "输出: {改写 SQL, 等价性证明, 性能预估, 风险评级}",
            gaps.isEmpty() ? "IMPLEMENTED" : "INCOMPLETE_OUTPUT",
            gaps.isEmpty() ? "FULL" : "PARTIAL",
            evidence("OUTPUT_BUNDLE", attributes),
            gaps,
            attributes
        );
    }

    private Map<String, Object> outputBundle(RewriteRecommendationReport recommendationReport) {
        RewriteRecommendation recommendation = recommendationReport == null
            ? null
            : recommendationReport.firstRecommendation();
        LinkedHashMap<String, Object> output = new LinkedHashMap<String, Object>();
        output.put("hasRewriteSql", Boolean.valueOf(
            recommendation != null && !recommendation.getExecutableSql().isEmpty()
        ));
        output.put("hasEquivalenceProof", Boolean.valueOf(
            recommendation != null && recommendation.getEquivalenceProof() != null
        ));
        output.put("hasPerformanceEstimate", Boolean.valueOf(
            recommendation != null && recommendation.getPerformance() != null
        ));
        output.put("hasRiskLevel", Boolean.valueOf(
            recommendation != null
                && recommendation.getPerformance() != null
                && !recommendation.getPerformance().getRiskLevel().isEmpty()
        ));
        output.put("rewriteId", recommendation == null ? "" : recommendation.getRewriteId());
        output.put("candidateId", recommendation == null ? "" : recommendation.getCandidateId());
        output.put("riskLevel", recommendation == null ? "" : recommendation.getPerformance().getRiskLevel());
        output.put("confidence", Double.valueOf(recommendation == null ? 0.0 : recommendation.getConfidence()));
        output.put("autoApplyAllowed", Boolean.FALSE);
        return output;
    }

    private List<String> criticalGaps(List<RewriteAlgorithmStage> stages) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (RewriteAlgorithmStage stage : stages) {
            for (String gap : stage.getGaps()) {
                if ("SMT_SOLVER_NOT_INTEGRATED".equals(gap)
                    || "REAL_CALCITE_RELTOSQL_NOT_INVOKED".equals(gap)
                    || "REAL_CALCITE_RELNODE_NOT_BUILT".equals(gap)
                    || gap.endsWith("_MISSING")
                    || gap.endsWith("_EMPTY")) {
                    result.add(gap);
                }
            }
        }
        return new ArrayList<String>(result);
    }

    private String algorithmStatus(List<String> criticalGaps, RewriteRecommendationReport recommendationReport) {
        if (recommendationReport == null || recommendationReport.getRecommendations().isEmpty()) {
            return "PARTIAL_NO_FINAL_RECOMMENDATION";
        }
        if (criticalGaps.isEmpty()) {
            return "CONFORMS";
        }
        return "CONFORMS_WITH_STATIC_SURROGATES";
    }

    private String conclusion(List<String> criticalGaps, RewriteRecommendationReport recommendationReport) {
        if (recommendationReport == null || recommendationReport.getRecommendations().isEmpty()) {
            return "主干链路未产生最终推荐，需要继续补候选识别或 SQL 形态适配。";
        }
        if (criticalGaps.isEmpty()) {
            return "当前实现完整符合核心算法脉络。";
        }
        return "当前实现符合核心算法主干，但 RelNode/SMT/RelToSql 仍存在静态替代边界。";
    }

    private String sourceSchemaVersion(RewriteRecommendationReport recommendationReport,
                                       ParserStackFusionReport parserReport,
                                       RelationalRewritePlan plan) {
        if (recommendationReport != null && !recommendationReport.getSchemaVersion().isEmpty()) {
            return recommendationReport.getSchemaVersion();
        }
        if (parserReport != null && !parserReport.getSchemaVersion().isEmpty()) {
            return parserReport.getSchemaVersion();
        }
        return plan == null ? "" : plan.getSchemaVersion();
    }

    private RewriteAlgorithmStage stage(String stageId,
                                        String stageName,
                                        String expectedStep,
                                        String implementationStatus,
                                        String conformanceLevel,
                                        List<Map<String, Object>> evidence,
                                        List<String> gaps,
                                        Map<String, Object> attributes) {
        return new RewriteAlgorithmStage(
            stageId,
            stageName,
            expectedStep,
            implementationStatus,
            conformanceLevel,
            evidence,
            gaps,
            attributes
        );
    }

    private List<Map<String, Object>> evidence(String type, Map<String, Object> attributes) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("type", type);
        item.putAll(attributes);
        return Collections.<Map<String, Object>>singletonList(item);
    }
}
