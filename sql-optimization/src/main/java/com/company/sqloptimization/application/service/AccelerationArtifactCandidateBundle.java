package com.company.sqloptimization.application.service;

import java.util.List;
import java.util.Map;

final class AccelerationArtifactCandidateBundle {

    private L2DynamicSnapshotAggregateMvCandidateGenerator.CandidateSql dynamicSnapshot;
    private L2ParameterizedAggMvCandidateGenerator.CandidateSql parameterizedAgg;
    private L2PrejoinMvCandidateGenerator.CandidateSql prejoin;
    private L2StarAggMvCandidateGenerator.CandidateSql starAgg;
    private L2RollupMvCandidateGenerator.CandidateSql rollup;
    private CommonSubgraphCandidateSql commonSubgraph;

    private AccelerationArtifactCandidateBundle() {
    }

    static AccelerationArtifactCandidateBundle generate(
        List<Map<String, Object>> blockingReasons,
        String candidateSourceSql,
        String mvName,
        String targetEngine,
        Map<String, Object> candidateAdvancedStructureProfile,
        L2PredicateClassifier.PredicateClassificationResult candidatePredicateClassification,
        L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation,
        SqlOptimizationPipelineService.ParsedSqlProfile candidateProfile,
        List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> commonSubgraphPeerSqls) {
        AccelerationArtifactCandidateBundle bundle = new AccelerationArtifactCandidateBundle();
        if (!blockingReasons.isEmpty()) {
            return bundle;
        }
        bundle.dynamicSnapshot = L2DynamicSnapshotAggregateMvCandidateGenerator.generate(
            candidateSourceSql,
            mvName,
            targetEngine,
            candidateProfile
        );
        if (bundle.dynamicSnapshot != null) {
            blockingReasons.addAll(bundle.dynamicSnapshot.getBlockingReasons());
        } else if (L2GrainMeasureDeriver.MV_TYPE_COMMON_SUBGRAPH.equals(grainMeasureDerivation.getMvType())) {
            bundle.commonSubgraph = L2CommonSubgraphMvCandidateGenerator.generate(
                candidateSourceSql,
                mvName,
                targetEngine,
                candidateAdvancedStructureProfile,
                candidateProfile,
                commonSubgraphPeerSqls
            );
            blockingReasons.addAll(bundle.commonSubgraph.getBlockingReasons());
        } else if (L2GrainMeasureDeriver.MV_TYPE_STAR_AGG.equals(grainMeasureDerivation.getMvType())) {
            bundle.starAgg = L2StarAggMvCandidateGenerator.generate(
                candidateSourceSql,
                mvName,
                targetEngine,
                candidateAdvancedStructureProfile,
                candidatePredicateClassification,
                grainMeasureDerivation
            );
            blockingReasons.addAll(bundle.starAgg.getBlockingReasons());
        } else if (L2GrainMeasureDeriver.MV_TYPE_PREJOIN.equals(grainMeasureDerivation.getMvType())) {
            bundle.prejoin = L2PrejoinMvCandidateGenerator.generate(
                candidateSourceSql,
                mvName,
                targetEngine,
                candidateAdvancedStructureProfile,
                candidatePredicateClassification,
                grainMeasureDerivation
            );
            blockingReasons.addAll(bundle.prejoin.getBlockingReasons());
        } else if (L2GrainMeasureDeriver.MV_TYPE_ROLLUP.equals(grainMeasureDerivation.getMvType())) {
            bundle.rollup = L2RollupMvCandidateGenerator.generate(
                candidateSourceSql,
                mvName,
                targetEngine,
                candidateAdvancedStructureProfile,
                candidatePredicateClassification,
                grainMeasureDerivation
            );
            blockingReasons.addAll(bundle.rollup.getBlockingReasons());
        } else {
            bundle.parameterizedAgg = L2ParameterizedAggMvCandidateGenerator.generate(
                candidateSourceSql,
                mvName,
                targetEngine,
                candidateAdvancedStructureProfile,
                candidatePredicateClassification,
                grainMeasureDerivation
            );
            blockingReasons.addAll(bundle.parameterizedAgg.getBlockingReasons());
        }
        return bundle;
    }

    String candidateRewriteSql() {
        if (dynamicSnapshot != null) {
            return dynamicSnapshot.getRewriteSql();
        }
        if (commonSubgraph != null) {
            return commonSubgraph.getRewriteSql();
        }
        if (starAgg != null) {
            return starAgg.getRewriteSql();
        }
        if (prejoin != null) {
            return prejoin.getRewriteSql();
        }
        if (rollup != null) {
            return rollup.getRewriteSql();
        }
        return parameterizedAgg == null ? null : parameterizedAgg.getRewriteSql();
    }

    void addVariantEvidence(Map<String, Object> artifact) {
        if (dynamicSnapshot != null) {
            artifact.put("dynamicSnapshotRewriteEvidence", dynamicSnapshot.getRewriteEvidence());
            artifact.put("dynamicSnapshotValidationMethods", dynamicSnapshot.getValidationMethods());
            artifact.put("grain", dynamicSnapshot.getGrain());
            artifact.put("dimensions", dynamicSnapshot.getDimensions());
            artifact.put("measures", dynamicSnapshot.getMeasures());
            artifact.put("externalizedPredicates", dynamicSnapshot.getExternalizedPredicates());
            artifact.put("retainedPredicates", dynamicSnapshot.getRetainedPredicates());
            artifact.put("reviewWarnings", dynamicSnapshot.getReviewWarnings());
        }
        if (prejoin != null) {
            artifact.put("joinKeys", prejoin.getJoinKeys());
            artifact.put("fieldMappings", prejoin.getFieldMappings());
            artifact.put("aliasDisambiguation", prejoin.getAliasDisambiguation());
            artifact.put("rowAmplificationRisk", prejoin.getRowAmplificationRisk());
        }
        if (starAgg != null) {
            artifact.put("factTable", starAgg.getFactTable());
            artifact.put("dimensionTables", starAgg.getDimensionTables());
            artifact.put("joinKeys", starAgg.getJoinKeys());
            artifact.put("dimensionSources", starAgg.getDimensionSources());
            artifact.put("measureSources", starAgg.getMeasureSources());
            artifact.put("starSchemaEvidence", starAgg.getStarSchemaEvidence());
        }
        if (rollup != null) {
            artifact.put("timeRollupEvidence", rollup.getTimeRollupEvidence());
        }
        if (commonSubgraph != null
            && commonSubgraph.getCommonSubgraphEvidence() != null
            && !commonSubgraph.getCommonSubgraphEvidence().isEmpty()) {
            artifact.put("commonSubgraphEvidence", commonSubgraph.getCommonSubgraphEvidence());
        }
    }

    void addSqlOutputs(Map<String, Object> artifact, String plannedRewriteSql) {
        if (dynamicSnapshot != null) {
            putSqlOutputs(artifact, dynamicSnapshot.getDdlSql(), dynamicSnapshot.getRefreshSql(),
                dynamicSnapshot.getRollbackSql(), dynamicSnapshot.getValidationSql(), plannedRewriteSql);
        } else if (commonSubgraph != null) {
            putSqlOutputs(artifact, commonSubgraph.getDdlSql(), commonSubgraph.getRefreshSql(),
                commonSubgraph.getRollbackSql(), commonSubgraph.getValidationSql(), plannedRewriteSql);
        } else if (starAgg != null) {
            putSqlOutputs(artifact, starAgg.getDdlSql(), starAgg.getRefreshSql(),
                starAgg.getRollbackSql(), starAgg.getValidationSql(), plannedRewriteSql);
        } else if (prejoin != null) {
            putSqlOutputs(artifact, prejoin.getDdlSql(), prejoin.getRefreshSql(),
                prejoin.getRollbackSql(), prejoin.getValidationSql(), plannedRewriteSql);
        } else if (rollup != null) {
            putSqlOutputs(artifact, rollup.getDdlSql(), rollup.getRefreshSql(),
                rollup.getRollbackSql(), rollup.getValidationSql(), plannedRewriteSql);
        } else {
            putSqlOutputs(artifact, parameterizedAgg.getDdlSql(), parameterizedAgg.getRefreshSql(),
                parameterizedAgg.getRollbackSql(), parameterizedAgg.getValidationSql(), plannedRewriteSql);
        }
    }

    L2PrejoinMvCandidateGenerator.CandidateSql prejoin() {
        return prejoin;
    }

    L2StarAggMvCandidateGenerator.CandidateSql starAgg() {
        return starAgg;
    }

    L2RollupMvCandidateGenerator.CandidateSql rollup() {
        return rollup;
    }

    CommonSubgraphCandidateSql commonSubgraph() {
        return commonSubgraph;
    }

    L2DynamicSnapshotAggregateMvCandidateGenerator.CandidateSql dynamicSnapshot() {
        return dynamicSnapshot;
    }

    private static void putSqlOutputs(Map<String, Object> artifact,
                                      String ddlSql,
                                      String refreshSql,
                                      String rollbackSql,
                                      String validationSql,
                                      String rewriteSql) {
        artifact.put("ddlSql", ddlSql);
        artifact.put("refreshSql", refreshSql);
        artifact.put("rollbackSql", rollbackSql);
        artifact.put("validationSql", validationSql);
        artifact.put("rewriteSql", rewriteSql);
    }
}
