package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.PrejoinProfileValues.baseTables;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.booleanValue;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.mapList;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.mapValue;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.reason;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class PrejoinStructuralPolicy {

    private PrejoinStructuralPolicy() {
    }

    static List<Map<String, Object>> structuralBlockingReasons(
        Map<String, Object> advancedStructureProfile,
        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
        L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            reasons.add(reason("ADVANCED_STRUCTURE_PROFILE_REQUIRED", "缺少高级结构画像，不能生成 PREJOIN_MV。"));
            return reasons;
        }
        if (!"AVAILABLE".equals(text(advancedStructureProfile.get("profileStatus")))) {
            reasons.add(reason(
                "ADVANCED_STRUCTURE_PROFILE_REQUIRED",
                "高级结构画像未完整可用，不能生成可激活的 PREJOIN_MV SQL。"
            ));
        }
        if (grainMeasureDerivation == null
            || !L2GrainMeasureDeriver.MV_TYPE_PREJOIN.equals(grainMeasureDerivation.getMvType())) {
            reasons.add(reason("PREJOIN_MV_ONLY", "AMV-006 只生成 Join + 聚合/GROUP BY 的 PREJOIN_MV。"));
        }
        if (mapList(advancedStructureProfile.get("joinGraph")).isEmpty()) {
            reasons.add(reason("JOIN_GRAPH_REQUIRED", "缺少 Join 图，不能生成 PREJOIN_MV。"));
        }
        if (!mapList(advancedStructureProfile.get("ctes")).isEmpty()
            || !mapList(advancedStructureProfile.get("subqueries")).isEmpty()) {
            reasons.add(reason(
                "COMMON_SUBGRAPH_MV_DEFERRED",
                "CTE、派生表或子查询公共子图 MV 留给 AMV-009，不在 AMV-006 中生成。"
            ));
        }
        if (!mapList(advancedStructureProfile.get("orderBy")).isEmpty()
            || booleanValue(mapValue(advancedStructureProfile.get("limit"), "present"))) {
            reasons.add(reason("ORDER_LIMIT_REWRITE_UNSUPPORTED", "ORDER BY 或 LIMIT 的保序 rewrite 校验留给后续静态覆盖任务。"));
        }
        if (hasOrPredicate(predicateClassification)) {
            reasons.add(reason(
                "OR_PREDICATE_REWRITE_UNSUPPORTED",
                "OR 谓词需要保持原逻辑分组，AMV-006 暂不生成可激活 PREJOIN_MV rewrite。"
            ));
        }
        if (baseTables(mapList(advancedStructureProfile.get("tables"))).size() < 2) {
            reasons.add(reason("JOIN_BASE_TABLES_REQUIRED", "PREJOIN_MV 至少需要两个可解析的基表来源。"));
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
