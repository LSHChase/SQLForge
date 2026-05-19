package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

final class L2AccelerationArtifactBuilder {

    static final String RULE_PRECOMPUTE_MV = "PRECOMPUTE_MV";
    private static final String STATUS_GENERATED = "GENERATED";
    private static final String STATUS_BLOCKED = "BLOCKED";
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
        if (input == null || !containsRule(input.ruleChain, RULE_PRECOMPUTE_MV)) {
            return null;
        }
        return build(input, profile);
    }

    static Map<String, Object> buildForPrecomputeCandidate(AccelerationRecommendationInput input,
                                                           SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        if (profile == null || !hasPrecomputeSignal(profile)) {
            return null;
        }
        return build(input, profile);
    }

    private static Map<String, Object> build(AccelerationRecommendationInput input,
                                             SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        String sourceSql = trimTrailingSemicolon(firstText(
            input.sourceSqlText,
            profile == null ? null : profile.getNormalizedSql()
        ));
        String targetEngine = normalizeEngine(input.targetEngine);
        Map<String, Object> advancedStructureProfile = profile == null
            ? null
            : profile.toAdvancedStructureProfile();
        L2PredicateClassifier.PredicateClassificationResult predicateClassification =
            L2PredicateClassifier.classify(advancedStructureProfile);
        L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation =
            L2GrainMeasureDeriver.derive(advancedStructureProfile, predicateClassification);
        List<Map<String, Object>> blockingReasons = blockingReasons(
            sourceSql,
            targetEngine,
            profile,
            predicateClassification,
            grainMeasureDerivation
        );
        String mvName = mvName(input, profile);
        L2ParameterizedAggMvCandidateGenerator.CandidateSql candidateSql = null;
        if (blockingReasons.isEmpty()) {
            candidateSql = L2ParameterizedAggMvCandidateGenerator.generate(
                sourceSql,
                mvName,
                advancedStructureProfile,
                predicateClassification,
                grainMeasureDerivation
            );
            blockingReasons.addAll(candidateSql.getBlockingReasons());
        }
        LinkedHashMap<String, Object> artifact = new LinkedHashMap<String, Object>();
        artifact.put("rule", RULE_PRECOMPUTE_MV);
        artifact.put("mvType", grainMeasureDerivation.getMvType());
        artifact.put("artifactStatus", blockingReasons.isEmpty() ? STATUS_GENERATED : STATUS_BLOCKED);
        artifact.put("mvName", mvName);
        artifact.put("targetEngine", targetEngine);
        artifact.put("targetDatasource", input.targetDatasource);
        artifact.put("dialect", dialect(targetEngine));
        artifact.put("grain", grainMeasureDerivation.getGrain());
        artifact.put("dimensions", grainMeasureDerivation.getDimensions());
        artifact.put("measures", grainMeasureDerivation.getMeasures());
        artifact.put("joinGraph", grainMeasureDerivation.getJoinGraph());
        artifact.put("requiredEvidence", REQUIRED_EVIDENCE);
        artifact.put("externalizedPredicates", predicateClassification.getExternalizedPredicates());
        artifact.put("retainedPredicates", predicateClassification.getRetainedPredicates());
        artifact.put("securityPredicates", predicateClassification.getSecurityPredicates());
        artifact.put("blockedPredicates", predicateClassification.getBlockedPredicates());
        artifact.put("coverage", grainMeasureDerivation.getCoverage());
        artifact.put("blockingReasons", blockingReasons);
        artifact.put("reviewWarnings", grainMeasureDerivation.getReviewWarnings());
        artifact.put("steps", steps());
        artifact.put("refreshStrategy", "MANUAL_REFRESH_REQUIRED");
        artifact.put("governanceBoundary", "PULL_ONLY_NOT_EXECUTED_BY_SQLFORGE");
        artifact.put("governanceBoundaryZh", "SQLForge 仅生成可审查方案，不直接执行生产建表、刷新或删除。");
        artifact.put("runtimeRewriteBinding", "NOT_CREATED");
        artifact.put("source", source(input));
        if (blockingReasons.isEmpty()) {
            artifact.put("ddlSql", candidateSql.getDdlSql());
            artifact.put("refreshSql", candidateSql.getRefreshSql());
            artifact.put("rollbackSql", candidateSql.getRollbackSql());
            artifact.put("validationSql", candidateSql.getValidationSql());
            artifact.put("rewriteSql", candidateSql.getRewriteSql());
        }
        return artifact;
    }

    private static List<Map<String, Object>> blockingReasons(String sourceSql,
                                                             String targetEngine,
                                                             SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                                             L2PredicateClassifier.PredicateClassificationResult
                                                                 predicateClassification,
                                                             L2GrainMeasureDeriver.DerivationResult
                                                                 grainMeasureDerivation) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        if (!StringUtils.hasText(sourceSql)) {
            reasons.add(reason("SOURCE_SQL_REQUIRED", "缺少原 SQL，不能生成物化视图 AS SELECT。"));
        }
        if (!StringUtils.hasText(targetEngine) || "AUTO".equals(targetEngine)) {
            reasons.add(reason("TARGET_ENGINE_REQUIRED", "缺少明确目标引擎方言，不能声明 DDL 可执行。"));
        } else if (!isSupportedEngine(targetEngine)) {
            reasons.add(reason("UNSUPPORTED_TARGET_ENGINE", "当前 V1 仅生成 HETU/HIVE/SPARK 物化视图草案。"));
        }
        if (profile == null) {
            reasons.add(reason("PARSE_PROFILE_REQUIRED", "缺少解析画像，不能证明存在聚合预计算候选。"));
        } else if (!hasPrecomputeSignal(profile)) {
            reasons.add(reason("AGGREGATION_SIGNAL_REQUIRED", "未检测到聚合函数或 GROUP BY。"));
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
        if (grainMeasureDerivation != null) {
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
        return profile != null && (profile.getAggregateFunctionCount() > 0 || profile.getGroupByCount() > 0);
    }

    private static boolean isSupportedEngine(String targetEngine) {
        return "HETU".equals(targetEngine) || "HIVE".equals(targetEngine) || "SPARK".equals(targetEngine);
    }

    private static String dialect(String targetEngine) {
        if ("HETU".equals(targetEngine)) {
            return "HETU_MATERIALIZED_VIEW";
        }
        if ("HIVE".equals(targetEngine)) {
            return "HIVE_MATERIALIZED_VIEW";
        }
        if ("SPARK".equals(targetEngine)) {
            return "SPARK_MATERIALIZED_VIEW";
        }
        return "UNRESOLVED";
    }

    private static List<String> steps() {
        return Arrays.asList(
            "检查表元数据、分区键、字段血缘",
            "生成物化视图 DDL",
            "执行只读 explain / 成本评估",
            "创建或刷新物化视图",
            "对比原 SQL 与 MV 查询结果",
            "审批通过后发布 runtime rewrite binding",
            "异常时暂停绑定并回滚"
        );
    }

    private static Map<String, Object> source(AccelerationRecommendationInput input) {
        LinkedHashMap<String, Object> source = new LinkedHashMap<String, Object>();
        source.put("sqlFingerprint", input.sqlFingerprint);
        source.put("reportCode", input.reportCode);
        source.put("logicalObjectKey", input.logicalObjectKey);
        return source;
    }

    private static String mvName(AccelerationRecommendationInput input,
                                 SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        String base = firstText(input.logicalObjectKey, input.reportCode, input.sqlFingerprint);
        if (!StringUtils.hasText(base) && profile != null && !profile.getTables().isEmpty()) {
            base = profile.getTables().get(0);
        }
        if (!StringUtils.hasText(base)) {
            base = "query";
        }
        String normalized = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        normalized = normalized.replaceAll("^_+", "").replaceAll("_+$", "");
        if (!StringUtils.hasText(normalized)) {
            normalized = "query";
        }
        if (normalized.length() > 48) {
            normalized = normalized.substring(0, 48).replaceAll("_+$", "");
        }
        return "mv_" + normalized;
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

    private static String normalizeEngine(String targetEngine) {
        return StringUtils.hasText(targetEngine) ? targetEngine.trim().toUpperCase(Locale.ROOT) : null;
    }

    static final class AccelerationRecommendationInput {

        private final String sourceSqlText;
        private final String targetEngine;
        private final String targetDatasource;
        private final String sqlFingerprint;
        private final String reportCode;
        private final String logicalObjectKey;
        private final List<Map<String, Object>> ruleChain;

        AccelerationRecommendationInput(String sourceSqlText,
                                        String targetEngine,
                                        String targetDatasource,
                                        String sqlFingerprint,
                                        String reportCode,
                                        String logicalObjectKey,
                                        List<Map<String, Object>> ruleChain) {
            this.sourceSqlText = sourceSqlText;
            this.targetEngine = targetEngine;
            this.targetDatasource = targetDatasource;
            this.sqlFingerprint = sqlFingerprint;
            this.reportCode = reportCode;
            this.logicalObjectKey = logicalObjectKey;
            this.ruleChain = ruleChain;
        }
    }
}
