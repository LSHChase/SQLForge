package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.AccelerationArtifactCoverage.artifactStatus;
import static com.company.sqloptimization.application.service.AccelerationArtifactCoverage.coverageProofUnavailable;
import static com.company.sqloptimization.application.service.AccelerationArtifactCoverage.rewriteComposition;
import static com.company.sqloptimization.application.service.AccelerationArtifactValues.source;
import static com.company.sqloptimization.application.service.AccelerationArtifactValues.steps;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AccelerationArtifactMapBuilder {

    private AccelerationArtifactMapBuilder() {
    }

    static Map<String, Object> build(
        L2AccelerationArtifactBuilder.AccelerationRecommendationInput input,
        L2MaterializedViewTargetEngineResolver.Resolution targetEngineResolution,
        String targetEngine,
        String mvName,
        L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation,
        L2PredicateClassifier.PredicateClassificationResult rootPredicateClassification,
        MaterializedViewPlanningEvidence planningEvidence,
        AccelerationArtifactCandidateBundle candidates,
        List<Map<String, Object>> blockingReasons,
        List<Map<String, Object>> reviewWarnings,
        AccelerationArtifactRewriteValidation.Result rewriteResult) {
        LinkedHashMap<String, Object> artifact = new LinkedHashMap<String, Object>();
        artifact.put("rule", L2AccelerationArtifactBuilder.RULE_PRECOMPUTE_MV);
        artifact.put("mvType", grainMeasureDerivation.getMvType());
        artifact.put("artifactStatus", artifactStatus(blockingReasons, reviewWarnings));
        artifact.put("candidateId", planningEvidence.getCandidateId());
        artifact.put("sourceQueryBlockIds", planningEvidence.getWrapperAnalysis().getSourceQueryBlockIds());
        artifact.put("replacedSubgraphId", planningEvidence.getWrapperAnalysis().getReplacedSubgraphId());
        artifact.put("outerQueryPreserved", Boolean.valueOf(planningEvidence.getWrapperAnalysis().isOuterQueryPreserved()));
        artifact.put("mvName", mvName);
        artifact.put("targetEngine", targetEngine);
        artifact.put("targetEngineResolution", targetEngineResolution.toEvidence());
        artifact.put("targetDatasource", input.targetDatasource);
        artifact.put("dialect", L2MaterializedViewDialectRenderer.dialect(targetEngine));
        artifact.put("grain", grainMeasureDerivation.getGrain());
        artifact.put("dimensions", grainMeasureDerivation.getDimensions());
        artifact.put("measures", grainMeasureDerivation.getMeasures());
        artifact.put("joinGraph", grainMeasureDerivation.getJoinGraph());
        artifact.put("requiredEvidence", L2AccelerationArtifactBuilder.REQUIRED_EVIDENCE);
        artifact.put("externalizedPredicates", rootPredicateClassification.getExternalizedPredicates());
        artifact.put("retainedPredicates", rootPredicateClassification.getRetainedPredicates());
        artifact.put("securityPredicates", rootPredicateClassification.getSecurityPredicates());
        artifact.put("blockedPredicates", rootPredicateClassification.getBlockedPredicates());
        artifact.put("coverage", rewriteResult.getCoverage());
        artifact.put("coverageProof", rewriteResult.getCoverageProof() == null
            ? coverageProofUnavailable(rewriteResult.getCoverage(), planningEvidence)
            : rewriteResult.getCoverageProof().getCoverageProof());
        artifact.put("explainEvidence", planningEvidence.getExplainEvidence());
        artifact.put("metadataEvidence", planningEvidence.getMetadataEvidence());
        artifact.put("rewriteComposition", rewriteComposition(
            rewriteResult.getPlannedRewriteSql(),
            rewriteResult.getCandidateSubgraphRewriteSql(),
            mvName,
            planningEvidence
        ));
        artifact.put("plannerEvidence", planningEvidence.getPlannerEvidence());
        artifact.put("blockingReasons", blockingReasons);
        artifact.put("reviewWarnings", reviewWarnings);
        candidates.addVariantEvidence(artifact);
        if (candidates.dynamicSnapshot() != null) {
            artifact.put("mvType", "DYNAMIC_SNAPSHOT_AGGREGATE_MV");
        }
        artifact.put("steps", steps());
        artifact.put("refreshStrategy", "MANUAL_REFRESH_REQUIRED");
        artifact.put("governanceBoundary", "PULL_ONLY_NOT_EXECUTED_BY_SQLFORGE");
        artifact.put("governanceBoundaryZh", "SQLForge 仅生成可审查方案，不直接执行生产建表、刷新或删除。");
        artifact.put("runtimeRewriteBinding", "NOT_CREATED");
        artifact.put("generationSource", planningEvidence.getGenerationSource());
        artifact.put("source", source(input));
        if (blockingReasons.isEmpty()) {
            candidates.addSqlOutputs(artifact, rewriteResult.getPlannedRewriteSql());
        }
        return artifact;
    }
}
