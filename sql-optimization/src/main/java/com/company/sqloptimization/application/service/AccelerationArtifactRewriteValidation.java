package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.AccelerationArtifactCoverage.countWrapperCoverage;
import static com.company.sqloptimization.application.service.AccelerationArtifactCoverage.ensureTrailingSemicolon;
import static com.company.sqloptimization.application.service.AccelerationArtifactCoverage.removeCoverageReasons;
import static com.company.sqloptimization.application.service.AccelerationArtifactValues.firstText;

import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class AccelerationArtifactRewriteValidation {

    private AccelerationArtifactRewriteValidation() {
    }

    static Result evaluate(String sourceSql,
                           String mvName,
                           Map<String, Object> rootAdvancedStructureProfile,
                           L2PredicateClassifier.PredicateClassificationResult rootPredicateClassification,
                           L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation,
                           MaterializedViewPlanningEvidence planningEvidence,
                           boolean rootRewriteCandidate,
                           AccelerationArtifactCandidateBundle candidates,
                           List<Map<String, Object>> blockingReasons) {
        String candidateSubgraphRewriteSql = candidates.candidateRewriteSql();
        String candidateRootRewriteSql = rootRewriteCandidate
            ? candidateSubgraphRewriteSql
            : planningEvidence.getWrapperAnalysis().composeRootRewrite(candidateSubgraphRewriteSql);
        L2MaterializedViewRewriteCoverageValidator.ValidationResult rewriteValidation = null;
        String validatedRewriteSql = null;
        if (blockingReasons.isEmpty()) {
            rewriteValidation = validateRewrite(
                sourceSql,
                mvName,
                candidateRootRewriteSql,
                rootAdvancedStructureProfile,
                rootPredicateClassification,
                grainMeasureDerivation,
                candidates,
                planningEvidence,
                blockingReasons
            );
            validatedRewriteSql = rewriteValidation.getRewriteSql();
        }
        Map<String, Object> coverage = rewriteValidation == null
            ? grainMeasureDerivation.getCoverage()
            : rewriteValidation.getCoverage();
        String plannedRewriteSql = firstText(validatedRewriteSql, candidateRootRewriteSql);
        MvCoverageProofEngine.ProofResult coverageProof = null;
        if (rewriteValidation != null) {
            coverageProof = MvCoverageProofEngine.prove(new MvCoverageProofEngine.ProofInput(
                grainMeasureDerivation.getMvType(),
                mvName,
                plannedRewriteSql,
                coverage,
                planningEvidence.getWrapperAnalysis(),
                planningEvidence.getGenerationSource()
            ));
            blockingReasons.addAll(coverageProof.getBlockingReasons());
        }
        return new Result(coverage, plannedRewriteSql, candidateSubgraphRewriteSql, coverageProof);
    }

    private static L2MaterializedViewRewriteCoverageValidator.ValidationResult validateRewrite(
        String sourceSql,
        String mvName,
        String candidateRootRewriteSql,
        Map<String, Object> rootAdvancedStructureProfile,
        L2PredicateClassifier.PredicateClassificationResult rootPredicateClassification,
        L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation,
        AccelerationArtifactCandidateBundle candidates,
        MaterializedViewPlanningEvidence planningEvidence,
        List<Map<String, Object>> blockingReasons) {
        L2MaterializedViewRewriteCoverageValidator.ValidationResult rewriteValidation =
            L2MaterializedViewRewriteCoverageValidator.validate(
                new L2MaterializedViewRewriteCoverageValidator.ValidationInput(
                    sourceSql,
                    grainMeasureDerivation.getMvType(),
                    mvName,
                    candidateRootRewriteSql,
                    rootAdvancedStructureProfile,
                    rootPredicateClassification,
                    grainMeasureDerivation.getMeasures(),
                    AccelerationArtifactFields.mvFieldNames(grainMeasureDerivation, candidates),
                    AccelerationArtifactFields.additionalCoverageReferences(grainMeasureDerivation, candidates)
                )
            );
        List<Map<String, Object>> validationBlockingReasons = rewriteValidation.getBlockingReasons();
        Map<String, Object> validationCoverage = rewriteValidation.getCoverage();
        if (!rewriteValidation.isGenerated()
            && planningEvidence.getWrapperAnalysis().isCountOuterProjection()
            && planningEvidence.getWrapperAnalysis().projectionPreservedBy(candidateRootRewriteSql)) {
            validationCoverage = countWrapperCoverage(validationCoverage);
            validationBlockingReasons = removeCoverageReasons(
                validationBlockingReasons,
                L2MaterializedViewRewriteCoverageValidator.REWRITE_PROJECTION_NOT_COVERED,
                L2MaterializedViewRewriteCoverageValidator.REWRITE_MEASURE_NOT_COVERED
            );
        }
        String validatedRewriteSql = null;
        if (validationBlockingReasons.isEmpty() && StringUtils.hasText(candidateRootRewriteSql)) {
            validatedRewriteSql = rewriteValidation.getRewriteSql();
            if (!StringUtils.hasText(validatedRewriteSql)) {
                validatedRewriteSql = ensureTrailingSemicolon(candidateRootRewriteSql);
            }
        } else {
            blockingReasons.addAll(validationBlockingReasons);
        }
        return new L2MaterializedViewRewriteCoverageValidator.ValidationResult(
            validatedRewriteSql,
            validationCoverage,
            validationBlockingReasons
        );
    }

    static final class Result {
        private final Map<String, Object> coverage;
        private final String plannedRewriteSql;
        private final String candidateSubgraphRewriteSql;
        private final MvCoverageProofEngine.ProofResult coverageProof;

        private Result(Map<String, Object> coverage,
                       String plannedRewriteSql,
                       String candidateSubgraphRewriteSql,
                       MvCoverageProofEngine.ProofResult coverageProof) {
            this.coverage = coverage;
            this.plannedRewriteSql = plannedRewriteSql;
            this.candidateSubgraphRewriteSql = candidateSubgraphRewriteSql;
            this.coverageProof = coverageProof;
        }

        Map<String, Object> getCoverage() {
            return coverage;
        }

        String getPlannedRewriteSql() {
            return plannedRewriteSql;
        }

        String getCandidateSubgraphRewriteSql() {
            return candidateSubgraphRewriteSql;
        }

        MvCoverageProofEngine.ProofResult getCoverageProof() {
            return coverageProof;
        }
    }
}
