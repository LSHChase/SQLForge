package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.ParameterizedAggProfileValues.booleanValue;
import static com.company.sqloptimization.application.service.ParameterizedAggProfileValues.mapList;
import static com.company.sqloptimization.application.service.ParameterizedAggProfileValues.mapValue;
import static com.company.sqloptimization.application.service.ParameterizedAggProfileValues.reason;
import static com.company.sqloptimization.application.service.ParameterizedAggProfileValues.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ParameterizedAggPolicy {

    private ParameterizedAggPolicy() {
    }

    static List<Map<String, Object>> structuralBlockingReasons(
        Map<String, Object> advancedStructureProfile,
        L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            reasons.add(reason("ADVANCED_STRUCTURE_PROFILE_REQUIRED", "缺少高级结构画像，不能生成 PARAMETERIZED_AGG_MV。"));
            return reasons;
        }
        if (!"AVAILABLE".equals(text(advancedStructureProfile.get("profileStatus")))) {
            reasons.add(reason("ADVANCED_STRUCTURE_PROFILE_REQUIRED", "高级结构画像未完整可用，不能生成可激活的 PARAMETERIZED_AGG_MV SQL。"));
        }
        if (grainMeasureDerivation == null
            || !L2GrainMeasureDeriver.MV_TYPE_PARAMETERIZED_AGG.equals(grainMeasureDerivation.getMvType())) {
            reasons.add(reason("PARAMETERIZED_AGG_MV_ONLY", "AMV-005 只生成 PARAMETERIZED_AGG_MV，其他高级 MV 类型留给后续任务。"));
        }
        if (grainMeasureDerivation == null || grainMeasureDerivation.getMeasures().isEmpty()) {
            reasons.add(reason("MEASURE_REQUIRED", "缺少可重聚合指标，不能生成聚合 MV。"));
        }
        if (!mapList(advancedStructureProfile.get("orderBy")).isEmpty()
            || booleanValue(mapValue(advancedStructureProfile.get("limit"), "present"))) {
            reasons.add(reason("ORDER_LIMIT_REWRITE_UNSUPPORTED", "ORDER BY 或 LIMIT 的保序 rewrite 校验留给后续静态覆盖任务。"));
        }
        if (ParameterizedAggSqlBuilder.baseTables(mapList(advancedStructureProfile.get("tables"))).isEmpty()) {
            reasons.add(reason("BASE_TABLE_REQUIRED", "缺少单表基表来源，不能生成 PARAMETERIZED_AGG_MV。"));
        }
        return reasons;
    }
}
