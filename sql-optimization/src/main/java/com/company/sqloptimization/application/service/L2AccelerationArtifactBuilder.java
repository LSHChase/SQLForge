package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class L2AccelerationArtifactBuilder {

    static final String RULE_PRECOMPUTE_MV = "PRECOMPUTE_MV";
    private static final String STATUS_GENERATED = "GENERATED";
    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final String STATUS_REVIEW_REQUIRED = "REVIEW_REQUIRED";
    private static final List<String> REQUIRED_EVIDENCE = Collections.unmodifiableList(Arrays.asList(
        "TARGET_ENGINE",
        "FIELD_METADATA",
        "REFRESH_POLICY",
        "PERMISSION_REVIEW",
        "RESULT_EQUIVALENCE_VALIDATION"
    ));

    private L2AccelerationArtifactBuilder() {
    }

    static Map<String, Object> buildForRecommendation(AccelerationRecommendationInput input,
                                                      SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        return buildForRecommendation(input, profile, null);
    }

    static Map<String, Object> buildForRecommendation(AccelerationRecommendationInput input,
                                                      SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                                      HetuPlanAnalysisResult explainResult) {
        if (input == null || !containsRule(input.ruleChain, RULE_PRECOMPUTE_MV)) {
            return null;
        }
        return build(input, profile, explainResult);
    }

    static Map<String, Object> buildForPrecomputeCandidate(AccelerationRecommendationInput input,
                                                           SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        return buildForPrecomputeCandidate(input, profile, null);
    }

    static Map<String, Object> buildForPrecomputeCandidate(AccelerationRecommendationInput input,
                                                           SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                                           HetuPlanAnalysisResult explainResult) {
        if (profile == null || !hasPrecomputeSignal(profile)) {
            return null;
        }
        return build(input, profile, explainResult);
    }

    private static Map<String, Object> build(AccelerationRecommendationInput input,
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
        MaterializedViewRecommendationPlanner.PlanningEvidence planningEvidence =
            MaterializedViewRecommendationPlanner.plan(sourceSql, profile, mvNameSeed, explainResult);
        String candidateSourceSql = planningEvidence.getWrapperAnalysis().getCandidateSourceSql(sourceSql);
        SqlOptimizationPipelineService.ParsedSqlProfile candidateProfile = candidateProfile(
            profile,
            sourceSql,
            candidateSourceSql
        );
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
        List<Map<String, Object>> blockingReasons = blockingReasons(
            candidateSourceSql,
            targetEngineResolution,
            candidateProfile,
            rootPredicateClassification,
            grainMeasureDerivation
        );
        L2ParameterizedAggMvCandidateGenerator.CandidateSql candidateSql = null;
        L2PrejoinMvCandidateGenerator.CandidateSql prejoinCandidateSql = null;
        L2StarAggMvCandidateGenerator.CandidateSql starAggCandidateSql = null;
        L2RollupMvCandidateGenerator.CandidateSql rollupCandidateSql = null;
        L2CommonSubgraphMvCandidateGenerator.CandidateSql commonSubgraphCandidateSql = null;
        String mvName = L2MaterializedViewNamePolicy.mvName(
            input.logicalObjectKey,
            input.reportCode,
            input.sqlFingerprint,
            candidateSourceSql,
            candidateProfile,
            grainMeasureDerivation
        );
        if (blockingReasons.isEmpty()) {
            if (L2GrainMeasureDeriver.MV_TYPE_COMMON_SUBGRAPH.equals(grainMeasureDerivation.getMvType())) {
                commonSubgraphCandidateSql = L2CommonSubgraphMvCandidateGenerator.generate(
                    candidateSourceSql,
                    mvName,
                    targetEngine,
                    candidateAdvancedStructureProfile,
                    candidateProfile,
                    input.commonSubgraphPeerSqls
                );
                blockingReasons.addAll(commonSubgraphCandidateSql.getBlockingReasons());
            } else if (L2GrainMeasureDeriver.MV_TYPE_STAR_AGG.equals(grainMeasureDerivation.getMvType())) {
                starAggCandidateSql = L2StarAggMvCandidateGenerator.generate(
                    candidateSourceSql,
                    mvName,
                    targetEngine,
                    candidateAdvancedStructureProfile,
                    candidatePredicateClassification,
                    grainMeasureDerivation
                );
                blockingReasons.addAll(starAggCandidateSql.getBlockingReasons());
            } else if (L2GrainMeasureDeriver.MV_TYPE_PREJOIN.equals(grainMeasureDerivation.getMvType())) {
                prejoinCandidateSql = L2PrejoinMvCandidateGenerator.generate(
                    candidateSourceSql,
                    mvName,
                    targetEngine,
                    candidateAdvancedStructureProfile,
                    candidatePredicateClassification,
                    grainMeasureDerivation
                );
                blockingReasons.addAll(prejoinCandidateSql.getBlockingReasons());
            } else if (L2GrainMeasureDeriver.MV_TYPE_ROLLUP.equals(grainMeasureDerivation.getMvType())) {
                rollupCandidateSql = L2RollupMvCandidateGenerator.generate(
                    candidateSourceSql,
                    mvName,
                    targetEngine,
                    candidateAdvancedStructureProfile,
                    candidatePredicateClassification,
                    grainMeasureDerivation
                );
                blockingReasons.addAll(rollupCandidateSql.getBlockingReasons());
            } else {
                candidateSql = L2ParameterizedAggMvCandidateGenerator.generate(
                    candidateSourceSql,
                    mvName,
                    targetEngine,
                    candidateAdvancedStructureProfile,
                    candidatePredicateClassification,
                    grainMeasureDerivation
                );
                blockingReasons.addAll(candidateSql.getBlockingReasons());
            }
        }
        L2MaterializedViewRewriteCoverageValidator.ValidationResult rewriteValidation = null;
        String validatedRewriteSql = null;
        String candidateSubgraphRewriteSql = candidateRewriteSql(
            candidateSql,
            prejoinCandidateSql,
            starAggCandidateSql,
            rollupCandidateSql,
            commonSubgraphCandidateSql
        );
        String candidateRootRewriteSql = rootRewriteCandidate
            ? candidateSubgraphRewriteSql
            : planningEvidence.getWrapperAnalysis().composeRootRewrite(candidateSubgraphRewriteSql);
        if (blockingReasons.isEmpty()) {
            rewriteValidation = L2MaterializedViewRewriteCoverageValidator.validate(
                new L2MaterializedViewRewriteCoverageValidator.ValidationInput(
                    sourceSql,
                    grainMeasureDerivation.getMvType(),
                    mvName,
                    candidateRootRewriteSql,
                    rootAdvancedStructureProfile,
                    rootPredicateClassification,
                    grainMeasureDerivation.getMeasures(),
                    mvFieldNames(
                        grainMeasureDerivation,
                        prejoinCandidateSql,
                        starAggCandidateSql,
                        rollupCandidateSql,
                        commonSubgraphCandidateSql
                    ),
                    additionalCoverageReferences(
                        grainMeasureDerivation,
                        rollupCandidateSql,
                        commonSubgraphCandidateSql
                    )
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
            if (validationBlockingReasons.isEmpty() && StringUtils.hasText(candidateRootRewriteSql)) {
                validatedRewriteSql = rewriteValidation.getRewriteSql();
                if (!StringUtils.hasText(validatedRewriteSql)) {
                    validatedRewriteSql = ensureTrailingSemicolon(candidateRootRewriteSql);
                }
            } else {
                blockingReasons.addAll(validationBlockingReasons);
            }
            rewriteValidation = new L2MaterializedViewRewriteCoverageValidator.ValidationResult(
                validatedRewriteSql,
                validationCoverage,
                validationBlockingReasons
            );
        }
        Map<String, Object> coverage = rewriteValidation == null
            ? grainMeasureDerivation.getCoverage()
            : rewriteValidation.getCoverage();
        String plannedRewriteSql = firstText(
            validatedRewriteSql,
            candidateRootRewriteSql
        );
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
        List<Map<String, Object>> reviewWarnings = grainMeasureDerivation.getReviewWarnings();
        LinkedHashMap<String, Object> artifact = new LinkedHashMap<String, Object>();
        artifact.put("rule", RULE_PRECOMPUTE_MV);
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
        artifact.put("requiredEvidence", REQUIRED_EVIDENCE);
        artifact.put("externalizedPredicates", rootPredicateClassification.getExternalizedPredicates());
        artifact.put("retainedPredicates", rootPredicateClassification.getRetainedPredicates());
        artifact.put("securityPredicates", rootPredicateClassification.getSecurityPredicates());
        artifact.put("blockedPredicates", rootPredicateClassification.getBlockedPredicates());
        artifact.put("coverage", coverage);
        artifact.put("coverageProof", coverageProof == null
            ? coverageProofUnavailable(coverage, planningEvidence)
            : coverageProof.getCoverageProof());
        artifact.put("explainEvidence", planningEvidence.getExplainEvidence());
        artifact.put("metadataEvidence", planningEvidence.getMetadataEvidence());
        artifact.put("rewriteComposition", rewriteComposition(
            plannedRewriteSql,
            candidateSubgraphRewriteSql,
            mvName,
            planningEvidence
        ));
        artifact.put("plannerEvidence", planningEvidence.getPlannerEvidence());
        artifact.put("blockingReasons", blockingReasons);
        artifact.put("reviewWarnings", reviewWarnings);
        if (prejoinCandidateSql != null) {
            artifact.put("joinKeys", prejoinCandidateSql.getJoinKeys());
            artifact.put("fieldMappings", prejoinCandidateSql.getFieldMappings());
            artifact.put("aliasDisambiguation", prejoinCandidateSql.getAliasDisambiguation());
            artifact.put("rowAmplificationRisk", prejoinCandidateSql.getRowAmplificationRisk());
        }
        if (starAggCandidateSql != null) {
            artifact.put("factTable", starAggCandidateSql.getFactTable());
            artifact.put("dimensionTables", starAggCandidateSql.getDimensionTables());
            artifact.put("joinKeys", starAggCandidateSql.getJoinKeys());
            artifact.put("dimensionSources", starAggCandidateSql.getDimensionSources());
            artifact.put("measureSources", starAggCandidateSql.getMeasureSources());
            artifact.put("starSchemaEvidence", starAggCandidateSql.getStarSchemaEvidence());
        }
        if (rollupCandidateSql != null) {
            artifact.put("timeRollupEvidence", rollupCandidateSql.getTimeRollupEvidence());
        }
        if (commonSubgraphCandidateSql != null
            && commonSubgraphCandidateSql.getCommonSubgraphEvidence() != null
            && !commonSubgraphCandidateSql.getCommonSubgraphEvidence().isEmpty()) {
            artifact.put("commonSubgraphEvidence", commonSubgraphCandidateSql.getCommonSubgraphEvidence());
        }
        artifact.put("steps", steps());
        artifact.put("refreshStrategy", "MANUAL_REFRESH_REQUIRED");
        artifact.put("governanceBoundary", "PULL_ONLY_NOT_EXECUTED_BY_SQLFORGE");
        artifact.put("governanceBoundaryZh", "SQLForge 仅生成可审查方案，不直接执行生产建表、刷新或删除。");
        artifact.put("runtimeRewriteBinding", "NOT_CREATED");
        artifact.put("generationSource", planningEvidence.getGenerationSource());
        artifact.put("source", source(input));
        if (blockingReasons.isEmpty()) {
            if (commonSubgraphCandidateSql != null) {
                artifact.put("ddlSql", commonSubgraphCandidateSql.getDdlSql());
                artifact.put("refreshSql", commonSubgraphCandidateSql.getRefreshSql());
                artifact.put("rollbackSql", commonSubgraphCandidateSql.getRollbackSql());
                artifact.put("validationSql", commonSubgraphCandidateSql.getValidationSql());
                artifact.put("rewriteSql", plannedRewriteSql);
            } else if (starAggCandidateSql != null) {
                artifact.put("ddlSql", starAggCandidateSql.getDdlSql());
                artifact.put("refreshSql", starAggCandidateSql.getRefreshSql());
                artifact.put("rollbackSql", starAggCandidateSql.getRollbackSql());
                artifact.put("validationSql", starAggCandidateSql.getValidationSql());
                artifact.put("rewriteSql", plannedRewriteSql);
            } else if (prejoinCandidateSql != null) {
                artifact.put("ddlSql", prejoinCandidateSql.getDdlSql());
                artifact.put("refreshSql", prejoinCandidateSql.getRefreshSql());
                artifact.put("rollbackSql", prejoinCandidateSql.getRollbackSql());
                artifact.put("validationSql", prejoinCandidateSql.getValidationSql());
                artifact.put("rewriteSql", plannedRewriteSql);
            } else if (rollupCandidateSql != null) {
                artifact.put("ddlSql", rollupCandidateSql.getDdlSql());
                artifact.put("refreshSql", rollupCandidateSql.getRefreshSql());
                artifact.put("rollbackSql", rollupCandidateSql.getRollbackSql());
                artifact.put("validationSql", rollupCandidateSql.getValidationSql());
                artifact.put("rewriteSql", plannedRewriteSql);
            } else {
                artifact.put("ddlSql", candidateSql.getDdlSql());
                artifact.put("refreshSql", candidateSql.getRefreshSql());
                artifact.put("rollbackSql", candidateSql.getRollbackSql());
                artifact.put("validationSql", candidateSql.getValidationSql());
                artifact.put("rewriteSql", plannedRewriteSql);
            }
        }
        return artifact;
    }

    private static Map<String, Object> countWrapperCoverage(Map<String, Object> coverage) {
        LinkedHashMap<String, Object> result = coverage == null
            ? new LinkedHashMap<String, Object>()
            : new LinkedHashMap<String, Object>(coverage);
        result.put("coversProjection", Boolean.TRUE);
        result.put("coversMeasures", Boolean.TRUE);
        result.put("outerCountWrapperRecomputedFromMvRows", Boolean.TRUE);
        return result;
    }

    private static List<Map<String, Object>> removeCoverageReasons(List<Map<String, Object>> reasons,
                                                                   String... removableCodes) {
        if (reasons == null || reasons.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> removable = new LinkedHashSet<String>();
        if (removableCodes != null) {
            Collections.addAll(removable, removableCodes);
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> reason : reasons) {
            String code = text(reason.get("code"));
            if (removable.contains(code)) {
                continue;
            }
            result.add(reason);
        }
        return result;
    }

    private static String ensureTrailingSemicolon(String sql) {
        if (!StringUtils.hasText(sql)) {
            return sql;
        }
        String trimmed = sql.trim();
        return trimmed.endsWith(";") ? trimmed : trimmed + ";";
    }

    private static SqlOptimizationPipelineService.ParsedSqlProfile candidateProfile(
        SqlOptimizationPipelineService.ParsedSqlProfile rootProfile,
        String rootSourceSql,
        String candidateSourceSql
    ) {
        if (!StringUtils.hasText(candidateSourceSql)
            || !StringUtils.hasText(rootSourceSql)
            || candidateSourceSql.trim().equals(rootSourceSql.trim())) {
            return rootProfile;
        }
        try {
            SqlOptimizationPipelineService.ParsedSqlProfile candidateProfile =
                new SqlOptimizationPipelineService().analyze(candidateSourceSql, DataSourceTypeEnum.AUTO);
            return hasPrecomputeSignal(candidateProfile) ? candidateProfile : rootProfile;
        } catch (RuntimeException ex) {
            return rootProfile;
        }
    }

    private static Map<String, Object> coverageProofUnavailable(
        Map<String, Object> coverage,
        MaterializedViewRecommendationPlanner.PlanningEvidence planningEvidence) {
        LinkedHashMap<String, Object> proof = new LinkedHashMap<String, Object>();
        proof.put("proofEngine", "MV_COVERAGE_PROOF_ENGINE_V1");
        proof.put("source", planningEvidence.getGenerationSource());
        proof.put("proofStatus", "NOT_EVALUATED");
        proof.put("coverage", coverage == null ? Collections.emptyMap() : coverage);
        proof.put("wrapperEvidence", planningEvidence.getWrapperAnalysis().toEvidence());
        return proof;
    }

    private static Map<String, Object> rewriteComposition(
        String plannedRewriteSql,
        String candidateSubgraphRewriteSql,
        String mvName,
        MaterializedViewRecommendationPlanner.PlanningEvidence planningEvidence) {
        LinkedHashMap<String, Object> composition = new LinkedHashMap<String, Object>();
        QueryWrapperPreserver.WrapperAnalysis wrapperAnalysis = planningEvidence.getWrapperAnalysis();
        composition.put("compositionType", wrapperAnalysis.isOuterQueryPreserved()
            ? "OUTER_QUERY_OVER_MV_SUBGRAPH"
            : "ROOT_QUERY_REWRITTEN_TO_MV");
        composition.put("rewriteSqlScope", "FULL_ROOT_QUERY");
        composition.put("candidateSubgraphRewriteSql", wrapperAnalysis.isOuterQueryPreserved()
            ? candidateSubgraphRewriteSql
            : null);
        composition.put("mvName", mvName);
        composition.put("referencesMv", Boolean.valueOf(StringUtils.hasText(plannedRewriteSql)
            && StringUtils.hasText(mvName)
            && plannedRewriteSql.toUpperCase(Locale.ROOT).contains(mvName.toUpperCase(Locale.ROOT))));
        composition.put("outerQueryPreserved", Boolean.valueOf(wrapperAnalysis.isOuterQueryPreserved()));
        composition.put("outerProjectionPreserved", Boolean.valueOf(wrapperAnalysis.projectionPreservedBy(plannedRewriteSql)));
        composition.put("orderLimitPreserved", Boolean.valueOf(wrapperAnalysis.orderLimitPreservedBy(plannedRewriteSql)));
        composition.put("sourceQueryBlockIds", wrapperAnalysis.getSourceQueryBlockIds());
        composition.put("replacedSubgraphId", wrapperAnalysis.getReplacedSubgraphId());
        return composition;
    }

    private static String artifactStatus(List<Map<String, Object>> blockingReasons,
                                         List<Map<String, Object>> reviewWarnings) {
        if (blockingReasons != null && !blockingReasons.isEmpty()) {
            return STATUS_BLOCKED;
        }
        if (reviewWarnings != null && !reviewWarnings.isEmpty()) {
            return STATUS_REVIEW_REQUIRED;
        }
        return STATUS_GENERATED;
    }

    private static String candidateRewriteSql(
                                              L2ParameterizedAggMvCandidateGenerator.CandidateSql candidateSql,
                                              L2PrejoinMvCandidateGenerator.CandidateSql prejoinCandidateSql,
                                              L2StarAggMvCandidateGenerator.CandidateSql starAggCandidateSql,
                                              L2RollupMvCandidateGenerator.CandidateSql rollupCandidateSql,
                                              L2CommonSubgraphMvCandidateGenerator.CandidateSql
                                                  commonSubgraphCandidateSql) {
        if (commonSubgraphCandidateSql != null) {
            return commonSubgraphCandidateSql.getRewriteSql();
        }
        if (starAggCandidateSql != null) {
            return starAggCandidateSql.getRewriteSql();
        }
        if (prejoinCandidateSql != null) {
            return prejoinCandidateSql.getRewriteSql();
        }
        if (rollupCandidateSql != null) {
            return rollupCandidateSql.getRewriteSql();
        }
        return candidateSql == null ? null : candidateSql.getRewriteSql();
    }

    private static List<String> mvFieldNames(L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation,
                                             L2PrejoinMvCandidateGenerator.CandidateSql prejoinCandidateSql,
                                             L2StarAggMvCandidateGenerator.CandidateSql starAggCandidateSql,
                                             L2RollupMvCandidateGenerator.CandidateSql rollupCandidateSql,
                                             L2CommonSubgraphMvCandidateGenerator.CandidateSql
                                                 commonSubgraphCandidateSql) {
        LinkedHashSet<String> fields = new LinkedHashSet<String>();
        if (grainMeasureDerivation != null) {
            fields.addAll(grainMeasureDerivation.getDimensions());
            addMeasureFields(fields, grainMeasureDerivation.getMeasures());
        }
        if (prejoinCandidateSql != null) {
            for (Map<String, Object> mapping : prejoinCandidateSql.getFieldMappings()) {
                addIfText(fields, text(mapping.get("mvColumn")));
            }
        }
        if (starAggCandidateSql != null) {
            for (Map<String, Object> dimensionSource : starAggCandidateSql.getDimensionSources()) {
                addIfText(fields, text(dimensionSource.get("mvColumn")));
            }
        }
        if (rollupCandidateSql != null) {
            addIfText(fields, text(mapValue(rollupCandidateSql.getTimeRollupEvidence(), "mvTimeColumn")));
        }
        if (commonSubgraphCandidateSql != null) {
            Map<String, Object> evidence = commonSubgraphCandidateSql.getCommonSubgraphEvidence();
            for (String outputColumn : stringList(evidence == null ? null : evidence.get("outputColumns"))) {
                addIfText(fields, outputColumn);
            }
        }
        return new ArrayList<String>(fields);
    }

    private static List<String> additionalCoverageReferences(
        L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation,
        L2RollupMvCandidateGenerator.CandidateSql rollupCandidateSql,
        L2CommonSubgraphMvCandidateGenerator.CandidateSql commonSubgraphCandidateSql) {
        LinkedHashSet<String> references = new LinkedHashSet<String>();
        if (grainMeasureDerivation != null) {
            references.addAll(grainMeasureDerivation.getGrain());
            references.addAll(grainMeasureDerivation.getDimensions());
        }
        if (rollupCandidateSql != null) {
            Map<String, Object> evidence = rollupCandidateSql.getTimeRollupEvidence();
            addIfText(references, text(evidence.get("timeSourceColumn")));
            addIfText(references, text(evidence.get("queryTimeExpression")));
            addIfText(references, text(evidence.get("rewriteRollupExpression")));
            addIfText(references, text(evidence.get("mvTimeColumn")));
        }
        if (commonSubgraphCandidateSql != null) {
            Map<String, Object> evidence = commonSubgraphCandidateSql.getCommonSubgraphEvidence();
            Map<String, Object> rewriteCoverage = mapValue(evidence, "rewriteCoverage") instanceof Map<?, ?>
                ? copyMap((Map<?, ?>) mapValue(evidence, "rewriteCoverage"))
                : Collections.<String, Object>emptyMap();
            for (String requiredColumn : stringList(rewriteCoverage.get("requiredColumns"))) {
                addIfText(references, requiredColumn);
            }
        }
        return new ArrayList<String>(references);
    }

    private static void addMeasureFields(Set<String> fields, List<Map<String, Object>> measures) {
        for (Map<String, Object> measure : measures) {
            addIfText(fields, text(measure.get("name")));
            for (Map<String, Object> component : mapList(measure.get("components"))) {
                addIfText(fields, text(component.get("name")));
            }
        }
    }

    private static List<Map<String, Object>> blockingReasons(String sourceSql,
                                                             L2MaterializedViewTargetEngineResolver.Resolution targetEngineResolution,
                                                             SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                                             L2PredicateClassifier.PredicateClassificationResult
                                                                 predicateClassification,
                                                             L2GrainMeasureDeriver.DerivationResult
                                                                 grainMeasureDerivation) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        if (!StringUtils.hasText(sourceSql)) {
            reasons.add(reason("SOURCE_SQL_REQUIRED", "缺少原 SQL，不能生成物化视图 AS SELECT。"));
        }
        String targetEngine = targetEngineResolution == null ? "" : targetEngineResolution.getTargetEngine();
        if (targetEngineResolution != null && targetEngineResolution.isUnsupportedExplicitEngine()) {
            reasons.add(reason("UNSUPPORTED_TARGET_ENGINE", "当前 V1 仅生成 HETU/HIVE/SPARK 物化视图草案。"));
        } else if (!StringUtils.hasText(targetEngine) || !L2MaterializedViewDialectRenderer.supports(targetEngine)) {
            reasons.add(reason("UNSUPPORTED_TARGET_ENGINE", "当前 V1 仅生成 HETU/HIVE/SPARK 物化视图草案。"));
        }
        if (profile == null) {
            reasons.add(reason("PARSE_PROFILE_REQUIRED", "缺少解析画像，不能证明存在聚合预计算候选。"));
        } else if (!hasPrecomputeSignal(profile)) {
            reasons.add(reason("PRECOMPUTE_SIGNAL_REQUIRED", "未检测到聚合函数、GROUP BY 或可物化公共子图。"));
        }
        if (profile != null && profile.isSelectStar()) {
            reasons.add(reason("EXPLICIT_PROJECTION_REQUIRED", "SELECT * 需要先展开字段后才能生成可审查物化视图。"));
        }
        if (predicateClassification != null && predicateClassification.hasBlockedPredicates()) {
            Map<String, Object> reason = reason(
                "BLOCKED_UNSTABLE_PREDICATE",
                "谓词包含当前时间、随机或会话上下文函数，缺少稳定化策略时不能生成高级物化视图产物。"
            );
            reason.put("blockedPredicates", predicateClassification.getBlockedPredicates());
            reasons.add(reason);
        }
        if (grainMeasureDerivation != null
            && !L2GrainMeasureDeriver.MV_TYPE_COMMON_SUBGRAPH.equals(grainMeasureDerivation.getMvType())) {
            reasons.addAll(grainMeasureDerivation.getBlockingReasons());
        }
        return reasons;
    }

    private static Map<String, Object> reason(String code, String description) {
        LinkedHashMap<String, Object> reason = new LinkedHashMap<String, Object>();
        reason.put("code", code);
        reason.put("description", description);
        return reason;
    }

    private static boolean hasPrecomputeSignal(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        if (profile == null) {
            return false;
        }
        if (profile.getAggregateFunctionCount() > 0 || profile.getGroupByCount() > 0) {
            return true;
        }
        Map<String, Object> advancedStructureProfile = profile.toAdvancedStructureProfile();
        return !mapList(advancedStructureProfile.get("ctes")).isEmpty()
            || !mapList(advancedStructureProfile.get("subqueries")).isEmpty();
    }

    private static List<String> steps() {
        return Arrays.asList(
            "检查表元数据、分区键、字段血缘",
            "生成物化视图 DDL",
            "执行只读 explain / 成本评估",
            "创建或刷新物化视图",
            "对比原 SQL 与 MV 查询结果",
            "激活 runtime rewrite binding",
            "异常时暂停绑定并保留恢复证据"
        );
    }

    private static Map<String, Object> source(AccelerationRecommendationInput input) {
        LinkedHashMap<String, Object> source = new LinkedHashMap<String, Object>();
        source.put("sqlFingerprint", input.sqlFingerprint);
        source.put("reportCode", input.reportCode);
        source.put("logicalObjectKey", input.logicalObjectKey);
        return source;
    }

    private static boolean containsRule(List<Map<String, Object>> ruleChain, String expectedRule) {
        if (ruleChain == null || ruleChain.isEmpty()) {
            return false;
        }
        for (Map<String, Object> item : ruleChain) {
            Object rule = item == null ? null : firstObject(item.get("rule"), item.get("ruleCode"));
            if (expectedRule.equals(String.valueOf(rule))) {
                return true;
            }
        }
        return false;
    }

    private static Object firstObject(Object... values) {
        for (Object value : values) {
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return value;
            }
        }
        return null;
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String trimTrailingSemicolon(String sql) {
        if (!StringUtils.hasText(sql)) {
            return null;
        }
        String trimmed = sql.trim();
        while (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static Object mapValue(Object value, String key) {
        if (!(value instanceof Map<?, ?>)) {
            return null;
        }
        return ((Map<?, ?>) value).get(key);
    }

    private static void addIfText(Set<String> values, String value) {
        if (values != null && StringUtils.hasText(value)) {
            values.add(value.trim());
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?>)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (Object item : (Iterable<?>) value) {
            if (item != null && StringUtils.hasText(String.valueOf(item))) {
                result.add(String.valueOf(item).trim());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?>)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map<?, ?>) {
                result.add((Map<String, Object>) item);
            }
        }
        return result;
    }

    private static Map<String, Object> copyMap(Map<?, ?> source) {
        LinkedHashMap<String, Object> target = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                target.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return target;
    }

    static final class AccelerationRecommendationInput {

        private final String sourceSqlText;
        private final String targetEngine;
        private final String targetDatasource;
        private final String sqlFingerprint;
        private final String reportCode;
        private final String logicalObjectKey;
        private final List<Map<String, Object>> ruleChain;
        private final List<CommonSubgraphPeerSql> commonSubgraphPeerSqls;

        AccelerationRecommendationInput(String sourceSqlText,
                                        String targetEngine,
                                        String targetDatasource,
                                        String sqlFingerprint,
                                        String reportCode,
                                        String logicalObjectKey,
                                        List<Map<String, Object>> ruleChain) {
            this(
                sourceSqlText,
                targetEngine,
                targetDatasource,
                sqlFingerprint,
                reportCode,
                logicalObjectKey,
                ruleChain,
                Collections.<CommonSubgraphPeerSql>emptyList()
            );
        }

        AccelerationRecommendationInput(String sourceSqlText,
                                        String targetEngine,
                                        String targetDatasource,
                                        String sqlFingerprint,
                                        String reportCode,
                                        String logicalObjectKey,
                                        List<Map<String, Object>> ruleChain,
                                        List<CommonSubgraphPeerSql> commonSubgraphPeerSqls) {
            this.sourceSqlText = sourceSqlText;
            this.targetEngine = targetEngine;
            this.targetDatasource = targetDatasource;
            this.sqlFingerprint = sqlFingerprint;
            this.reportCode = reportCode;
            this.logicalObjectKey = logicalObjectKey;
            this.ruleChain = ruleChain;
            this.commonSubgraphPeerSqls = commonSubgraphPeerSqls == null
                ? Collections.<CommonSubgraphPeerSql>emptyList()
                : commonSubgraphPeerSqls;
        }
    }

    static final class CommonSubgraphPeerSql {

        private final String sqlText;
        private final String sqlFingerprint;
        private final String sourceKind;
        private final String sourceRef;
        private final String reportCode;
        private final Map<String, Object> advancedStructureProfile;

        CommonSubgraphPeerSql(String sqlText,
                              String sqlFingerprint,
                              String sourceKind,
                              String sourceRef,
                              String reportCode,
                              Map<String, Object> advancedStructureProfile) {
            this.sqlText = sqlText;
            this.sqlFingerprint = sqlFingerprint;
            this.sourceKind = sourceKind;
            this.sourceRef = sourceRef;
            this.reportCode = reportCode;
            this.advancedStructureProfile = advancedStructureProfile == null
                ? Collections.<String, Object>emptyMap()
                : advancedStructureProfile;
        }

        String getSqlText() {
            return sqlText;
        }

        String getSqlFingerprint() {
            return sqlFingerprint;
        }

        String getSourceKind() {
            return sourceKind;
        }

        String getSourceRef() {
            return sourceRef;
        }

        String getReportCode() {
            return reportCode;
        }

        Map<String, Object> getAdvancedStructureProfile() {
            return advancedStructureProfile;
        }
    }
}
