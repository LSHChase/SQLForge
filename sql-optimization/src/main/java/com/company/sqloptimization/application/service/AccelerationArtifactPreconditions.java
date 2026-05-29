package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.AccelerationArtifactValues.mapList;
import static com.company.sqloptimization.application.service.AccelerationArtifactValues.reason;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class AccelerationArtifactPreconditions {

    private AccelerationArtifactPreconditions() {
    }

    static SqlOptimizationPipelineService.ParsedSqlProfile candidateProfile(
        SqlOptimizationPipelineService.ParsedSqlProfile rootProfile,
        String rootSourceSql,
        String candidateSourceSql) {
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

    static List<Map<String, Object>> blockingReasons(
        String sourceSql,
        L2MaterializedViewTargetEngineResolver.Resolution targetEngineResolution,
        SqlOptimizationPipelineService.ParsedSqlProfile profile,
        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
        L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        if (!StringUtils.hasText(sourceSql)) {
            reasons.add(reason("SOURCE_SQL_REQUIRED", "缺少原 SQL，不能生成物化视图 AS SELECT。"));
        }
        addTargetEngineReasons(reasons, targetEngineResolution);
        addProfileReasons(reasons, profile);
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

    static boolean hasPrecomputeSignal(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
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

    private static void addTargetEngineReasons(
        List<Map<String, Object>> reasons,
        L2MaterializedViewTargetEngineResolver.Resolution targetEngineResolution) {
        String targetEngine = targetEngineResolution == null ? "" : targetEngineResolution.getTargetEngine();
        if (targetEngineResolution != null && targetEngineResolution.isUnsupportedExplicitEngine()) {
            reasons.add(reason("UNSUPPORTED_TARGET_ENGINE", "当前 V1 仅生成 HETU/HIVE/SPARK 物化视图草案。"));
        } else if (!StringUtils.hasText(targetEngine) || !L2MaterializedViewDialectRenderer.supports(targetEngine)) {
            reasons.add(reason("UNSUPPORTED_TARGET_ENGINE", "当前 V1 仅生成 HETU/HIVE/SPARK 物化视图草案。"));
        }
    }

    private static void addProfileReasons(List<Map<String, Object>> reasons,
                                          SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        if (profile == null) {
            reasons.add(reason("PARSE_PROFILE_REQUIRED", "缺少解析画像，不能证明存在聚合预计算候选。"));
        } else if (!hasPrecomputeSignal(profile)) {
            reasons.add(reason("PRECOMPUTE_SIGNAL_REQUIRED", "未检测到聚合函数、GROUP BY 或可物化公共子图。"));
        }
        if (profile != null && profile.isSelectStar()) {
            reasons.add(reason("EXPLICIT_PROJECTION_REQUIRED", "SELECT * 需要先展开字段后才能生成可审查物化视图。"));
        }
    }
}
