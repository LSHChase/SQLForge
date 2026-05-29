package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.AccelerationArtifactValues.firstText;
import static com.company.sqloptimization.application.service.AccelerationArtifactValues.trimTrailingSemicolon;

import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import java.util.List;
import java.util.Map;

final class AccelerationArtifactCoordinator {

    private AccelerationArtifactCoordinator() {
    }

    static Map<String, Object> build(L2AccelerationArtifactBuilder.AccelerationRecommendationInput input,
                                     SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                     HetuPlanAnalysisResult explainResult) {
        String sourceSql = trimTrailingSemicolon(firstText(
            input.sourceSqlText,
            profile == null ? null : profile.getNormalizedSql()
        ));
        L2MaterializedViewTargetEngineResolver.Resolution targetEngineResolution =
            L2MaterializedViewTargetEngineResolver.resolve(input.targetEngine, input.targetDatasource, sourceSql);
        String targetEngine = targetEngineResolution.getTargetEngine();
        Map<String, Object> rootAdvancedStructureProfile = profile == null
            ? null
            : profile.toAdvancedStructureProfile();
        L2PredicateClassifier.PredicateClassificationResult rootPredicateClassification =
            L2PredicateClassifier.classify(rootAdvancedStructureProfile);
        String mvNameSeed = L2MaterializedViewNamePolicy.mvName(
            input.logicalObjectKey,
            input.reportCode,
            input.sqlFingerprint,
            sourceSql,
            profile,
            null
        );
        MaterializedViewPlanningEvidence planningEvidence =
            MaterializedViewRecommendationPlanner.plan(sourceSql, profile, mvNameSeed, explainResult);
        String candidateSourceSql = planningEvidence.getWrapperAnalysis().getCandidateSourceSql(sourceSql);
        SqlOptimizationPipelineService.ParsedSqlProfile candidateProfile =
            AccelerationArtifactPreconditions.candidateProfile(profile, sourceSql, candidateSourceSql);
        boolean rootRewriteCandidate = candidateProfile == profile;
        if (rootRewriteCandidate) {
            candidateSourceSql = sourceSql;
            candidateProfile = profile;
        }
        Map<String, Object> candidateAdvancedStructureProfile = candidateProfile == null
            ? null
            : candidateProfile.toAdvancedStructureProfile();
        L2PredicateClassifier.PredicateClassificationResult candidatePredicateClassification =
            L2PredicateClassifier.classify(candidateAdvancedStructureProfile);
        L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation =
            L2GrainMeasureDeriver.derive(candidateAdvancedStructureProfile, candidatePredicateClassification);
        List<Map<String, Object>> blockingReasons = AccelerationArtifactPreconditions.blockingReasons(
            candidateSourceSql,
            targetEngineResolution,
            candidateProfile,
            rootPredicateClassification,
            grainMeasureDerivation
        );
        String mvName = L2MaterializedViewNamePolicy.mvName(
            input.logicalObjectKey,
            input.reportCode,
            input.sqlFingerprint,
            candidateSourceSql,
            candidateProfile,
            grainMeasureDerivation
        );
        AccelerationArtifactCandidateBundle candidates = AccelerationArtifactCandidateBundle.generate(
            blockingReasons,
            candidateSourceSql,
            mvName,
            targetEngine,
            candidateAdvancedStructureProfile,
            candidatePredicateClassification,
            grainMeasureDerivation,
            candidateProfile,
            input.commonSubgraphPeerSqls
        );
        AccelerationArtifactRewriteValidation.Result rewriteResult = AccelerationArtifactRewriteValidation.evaluate(
            sourceSql,
            mvName,
            rootAdvancedStructureProfile,
            rootPredicateClassification,
            grainMeasureDerivation,
            planningEvidence,
            rootRewriteCandidate,
            candidates,
            blockingReasons
        );
        return AccelerationArtifactMapBuilder.build(
            input,
            targetEngineResolution,
            targetEngine,
            mvName,
            grainMeasureDerivation,
            rootPredicateClassification,
            planningEvidence,
            candidates,
            blockingReasons,
            grainMeasureDerivation.getReviewWarnings(),
            rewriteResult
        );
    }
}
