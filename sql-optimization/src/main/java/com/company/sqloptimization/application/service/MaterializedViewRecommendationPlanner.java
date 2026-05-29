package com.company.sqloptimization.application.service;

import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import com.company.sqloptimization.domain.rewrite.ir.RewriteCoreIrAssembler;
import com.company.sqloptimization.domain.rewrite.ir.RewriteCoreIrSnapshot;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDag;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDagBuilder;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlan;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlanBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class MaterializedViewRecommendationPlanner {

    static final String SOURCE_AST_IR = "AST_IR_RELATIONAL_MV_PLANNER";
    static final String SOURCE_AST_IR_WITH_EXPLAIN = "AST_IR_RELATIONAL_MV_PLANNER_WITH_EXPLAIN";

    private MaterializedViewRecommendationPlanner() {
    }

    static MaterializedViewPlanningEvidence plan(String sourceSql,
                                                 SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                                 String mvName,
                                                 HetuPlanAnalysisResult explainResult) {
        long startNanos = System.nanoTime();
        Map<String, Object> advancedProfile = profile == null
            ? Collections.<String, Object>emptyMap()
            : profile.toAdvancedStructureProfile();
        QueryBlockDag queryBlockDag = null;
        RelationalRewritePlan relationalPlan = null;
        RewriteCoreIrSnapshot coreIr = null;
        List<Map<String, Object>> plannerWarnings = new ArrayList<Map<String, Object>>();
        try {
            if (profile != null) {
                queryBlockDag = new QueryBlockDagBuilder().build(profile.getNormalizedSql(), advancedProfile);
                relationalPlan = new RelationalRewritePlanBuilder().build(queryBlockDag);
                coreIr = new RewriteCoreIrAssembler().assemble(
                    profile.getNormalizedSql(),
                    profile.getParserEngine(),
                    advancedProfile
                );
            }
        } catch (RuntimeException ex) {
            plannerWarnings.add(MaterializedViewPlanningEvidenceBuilder.warning(
                "AST_IR_RELATIONAL_PLANNER_PARTIAL",
                "AST/IR/QBDAG/关系代数规划证据不完整，候选只能依赖已解析结构继续阻断或人工复核。",
                ex.getMessage()
            ));
        }
        QueryWrapperPreserver.WrapperAnalysis wrapperAnalysis =
            QueryWrapperPreserver.analyze(sourceSql, queryBlockDag);
        Map<String, Object> explainEvidence = MaterializedViewPlanningEvidenceBuilder.explain(explainResult);
        Map<String, Object> metadataEvidence =
            MaterializedViewPlanningEvidenceBuilder.metadata(advancedProfile, queryBlockDag);
        String generationSource = "SUCCESS".equals(explainEvidence.get("status"))
            ? SOURCE_AST_IR_WITH_EXPLAIN
            : SOURCE_AST_IR;
        Map<String, Object> plannerEvidence = MaterializedViewPlanningEvidenceBuilder.planner(
            profile,
            coreIr,
            queryBlockDag,
            relationalPlan,
            wrapperAnalysis,
            MaterializedViewPlanningEvidenceBuilder.elapsedMillis(startNanos),
            generationSource,
            plannerWarnings
        );
        return new MaterializedViewPlanningEvidence(
            MaterializedViewPlanningEvidenceBuilder.candidateId(sourceSql, mvName),
            generationSource,
            wrapperAnalysis,
            explainEvidence,
            metadataEvidence,
            plannerEvidence,
            plannerWarnings
        );
    }
}
