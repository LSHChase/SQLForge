package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggProfileValues.baseTables;
import static com.company.sqloptimization.application.service.StarAggProfileValues.booleanValue;
import static com.company.sqloptimization.application.service.StarAggProfileValues.mapList;
import static com.company.sqloptimization.application.service.StarAggProfileValues.mapValue;
import static com.company.sqloptimization.application.service.StarAggProfileValues.reason;
import static com.company.sqloptimization.application.service.StarAggProfileValues.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class StarAggStructuralPolicy {

    private StarAggStructuralPolicy() {
    }

    static List<Map<String, Object>> structuralBlockingReasons(
        Map<String, Object> advancedStructureProfile,
        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
        L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation
    ) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            reasons.add(reason("ADVANCED_STRUCTURE_PROFILE_REQUIRED", "缺少高级结构画像，不能生成 STAR_AGG_MV。"));
            return reasons;
        }
        if (!"AVAILABLE".equals(text(advancedStructureProfile.get("profileStatus")))) {
            reasons.add(reason("ADVANCED_STRUCTURE_PROFILE_REQUIRED", "高级结构画像未完整可用，不能生成可激活的 STAR_AGG_MV SQL。"));
        }
        if (grainMeasureDerivation == null
            || !L2GrainMeasureDeriver.MV_TYPE_STAR_AGG.equals(grainMeasureDerivation.getMvType())) {
            reasons.add(reason("STAR_AGG_MV_ONLY", "AMV-007 只生成事实表 Join 维表后的星型聚合 MV。"));
        }
        if (grainMeasureDerivation == null || grainMeasureDerivation.getMeasures().isEmpty()) {
            reasons.add(reason("MEASURE_REQUIRED", "缺少可重聚合指标，不能生成 STAR_AGG_MV。"));
        }
        if (!Boolean.TRUE.equals(mapValue(grainMeasureDerivation == null ? null : grainMeasureDerivation.getCoverage(),
            "coversSecurity"))) {
            reasons.add(reason("STAR_AGG_SECURITY_PREDICATE_NOT_COVERED", "安全谓词字段未进入 MV 粒度，不能生成可激活 STAR_AGG_MV rewrite。"));
        }
        if (mapList(advancedStructureProfile.get("joinGraph")).size() < 2) {
            reasons.add(reason("STAR_AGG_JOIN_COUNT_REQUIRED", "STAR_AGG_MV 至少需要两个维表 Join。"));
        }
        if (!mapList(advancedStructureProfile.get("ctes")).isEmpty()
            || !mapList(advancedStructureProfile.get("subqueries")).isEmpty()) {
            reasons.add(reason("COMMON_SUBGRAPH_MV_DEFERRED", "CTE、派生表或子查询公共子图 MV 留给 AMV-009，不在 AMV-007 中生成。"));
        }
        if (!mapList(advancedStructureProfile.get("orderBy")).isEmpty()
            || booleanValue(mapValue(advancedStructureProfile.get("limit"), "present"))) {
            reasons.add(reason("ORDER_LIMIT_REWRITE_UNSUPPORTED", "ORDER BY 或 LIMIT 的保序 rewrite 校验留给后续静态覆盖任务。"));
        }
        if (hasOrPredicate(predicateClassification)) {
            reasons.add(reason("OR_PREDICATE_REWRITE_UNSUPPORTED", "OR 谓词需要保持原逻辑分组，AMV-007 暂不生成可激活 STAR_AGG_MV rewrite。"));
        }
        if (baseTables(mapList(advancedStructureProfile.get("tables"))).size() < 3) {
            reasons.add(reason("STAR_AGG_BASE_TABLES_REQUIRED", "STAR_AGG_MV 需要一个事实表和至少两个维表。"));
        }
        return reasons;
    }

    private static boolean hasOrPredicate(L2PredicateClassifier.PredicateClassificationResult classification) {
        return hasOrPredicate(classification == null
            ? Collections.<Map<String, Object>>emptyList()
            : classification.getExternalizedPredicates())
            || hasOrPredicate(classification == null
            ? Collections.<Map<String, Object>>emptyList()
            : classification.getRetainedPredicates())
            || hasOrPredicate(classification == null
            ? Collections.<Map<String, Object>>emptyList()
            : classification.getSecurityPredicates());
    }

    private static boolean hasOrPredicate(List<Map<String, Object>> predicates) {
        for (Map<String, Object> predicate : predicates) {
            if ("OR".equalsIgnoreCase(text(predicate.get("logicalContext")))) {
                return true;
            }
        }
        return false;
    }
}
