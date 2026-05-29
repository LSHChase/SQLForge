package com.company.sqloptimization.application.service;

import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class L2AccelerationArtifactBuilder {

    static final String RULE_PRECOMPUTE_MV = "PRECOMPUTE_MV";
    static final List<String> REQUIRED_EVIDENCE = Collections.unmodifiableList(Arrays.asList(
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
        if (input == null || !AccelerationArtifactValues.containsRule(input.ruleChain, RULE_PRECOMPUTE_MV)) {
            return null;
        }
        return AccelerationArtifactCoordinator.build(input, profile, explainResult);
    }

    static Map<String, Object> buildForPrecomputeCandidate(AccelerationRecommendationInput input,
                                                           SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        return buildForPrecomputeCandidate(input, profile, null);
    }

    static Map<String, Object> buildForPrecomputeCandidate(AccelerationRecommendationInput input,
                                                           SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                                           HetuPlanAnalysisResult explainResult) {
        if (profile == null || !AccelerationArtifactPreconditions.hasPrecomputeSignal(profile)) {
            return null;
        }
        return AccelerationArtifactCoordinator.build(input, profile, explainResult);
    }

    static final class AccelerationRecommendationInput {
        final String sourceSqlText;
        final String targetEngine;
        final String targetDatasource;
        final String sqlFingerprint;
        final String reportCode;
        final String logicalObjectKey;
        final List<Map<String, Object>> ruleChain;
        final List<CommonSubgraphPeerSql> commonSubgraphPeerSqls;

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
