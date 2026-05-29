package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RollupProfileValues.baseTables;
import static com.company.sqloptimization.application.service.RollupProfileValues.booleanValue;
import static com.company.sqloptimization.application.service.RollupProfileValues.mapList;
import static com.company.sqloptimization.application.service.RollupProfileValues.mapValue;
import static com.company.sqloptimization.application.service.RollupProfileValues.reason;
import static com.company.sqloptimization.application.service.RollupProfileValues.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class RollupStructuralPolicy {

    private RollupStructuralPolicy() {
    }

    static List<Map<String, Object>> structuralBlockingReasons(
        Map<String, Object> advancedStructureProfile,
        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
        L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation,
        RollupPlan rollupPlan) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            reasons.add(reason("ADVANCED_STRUCTURE_PROFILE_REQUIRED", "缺少高级结构画像，不能生成 ROLLUP_MV。"));
            return reasons;
        }
        if (!"AVAILABLE".equals(text(advancedStructureProfile.get("profileStatus")))) {
            reasons.add(reason(
                "ADVANCED_STRUCTURE_PROFILE_REQUIRED",
                "高级结构画像未完整可用，不能生成可激活的 ROLLUP_MV SQL。"
            ));
        }
        if (grainMeasureDerivation == null
            || !L2GrainMeasureDeriver.MV_TYPE_ROLLUP.equals(grainMeasureDerivation.getMvType())) {
            reasons.add(reason("ROLLUP_MV_ONLY", "AMV-008 只生成 ROLLUP_MV，其他高级 MV 类型由对应任务处理。"));
        }
        if (grainMeasureDerivation == null || grainMeasureDerivation.getMeasures().isEmpty()) {
            reasons.add(reason("MEASURE_REQUIRED", "缺少可重聚合指标，不能生成 Rollup MV。"));
        }
        if (!mapList(advancedStructureProfile.get("joinGraph")).isEmpty()) {
            reasons.add(reason(
                "ROLLUP_JOIN_MV_DEFERRED",
                "Join 形态的时间上卷需要与 PREJOIN_MV 或 STAR_AGG_MV 组合，AMV-008 不生成多表 Rollup SQL。"
            ));
        }
        if (!mapList(advancedStructureProfile.get("ctes")).isEmpty()
            || !mapList(advancedStructureProfile.get("subqueries")).isEmpty()) {
            reasons.add(reason(
                "COMMON_SUBGRAPH_MV_DEFERRED",
                "CTE、派生表或子查询公共子图 MV 留给 AMV-009，不在 AMV-008 中生成。"
            ));
        }
        if (!mapList(advancedStructureProfile.get("orderBy")).isEmpty()
            || booleanValue(mapValue(advancedStructureProfile.get("limit"), "present"))) {
            reasons.add(reason(
                "ORDER_LIMIT_REWRITE_UNSUPPORTED",
                "ORDER BY 或 LIMIT 的保序 Rollup rewrite 校验留给后续静态覆盖任务。"
            ));
        }
        if (hasOrPredicate(predicateClassification)) {
            reasons.add(reason(
                "OR_PREDICATE_REWRITE_UNSUPPORTED",
                "OR 谓词需要保持原逻辑分组，AMV-008 暂不生成可激活 Rollup rewrite。"
            ));
        }
        List<Map<String, Object>> baseTables = baseTables(mapList(advancedStructureProfile.get("tables")));
        if (baseTables.isEmpty()) {
            reasons.add(reason("BASE_TABLE_REQUIRED", "缺少单表基表来源，不能生成 ROLLUP_MV。"));
        } else if (baseTables.size() > 1) {
            reasons.add(reason(
                "SINGLE_BASE_TABLE_REQUIRED",
                "ROLLUP_MV 当前只支持单基表聚合，多表形态留给 Join/星型 MV 组合任务。"
            ));
        }
        if (rollupPlan == null) {
            reasons.add(reason("TIME_ROLLUP_EXPRESSION_REQUIRED", "缺少可识别的时间上卷分组表达式，不能生成 ROLLUP_MV。"));
        } else {
            reasons.addAll(rollupPlan.blockingReasons);
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
