package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.GrainDerivationPlanner.deriveGrain;
import static com.company.sqloptimization.application.service.GrainDerivationPlanner.withDistinctMeasureKeys;
import static com.company.sqloptimization.application.service.GrainMeasureCoverageBuilder.coverage;
import static com.company.sqloptimization.application.service.GrainMeasureProfileValues.mapList;
import static com.company.sqloptimization.application.service.MeasureDerivationPlanner.deriveMeasures;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class GrainMeasureDerivationEngine {

    private GrainMeasureDerivationEngine() {
    }

    static L2GrainMeasureDeriver.DerivationResult derive(
        Map<String, Object> advancedStructureProfile,
        L2PredicateClassifier.PredicateClassificationResult predicateClassification) {
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            return L2GrainMeasureDeriver.DerivationResult.empty();
        }

        MeasureDerivationPlan measureDerivation = deriveMeasures(
            mapList(advancedStructureProfile.get("projections")),
            mapList(advancedStructureProfile.get("aggregations"))
        );
        GrainDerivationPlan grainDerivation = withDistinctMeasureKeys(
            deriveGrain(advancedStructureProfile, predicateClassification),
            measureDerivation.measures
        );
        List<Map<String, Object>> joinGraph = mapList(advancedStructureProfile.get("joinGraph"));
        String mvType = mvType(advancedStructureProfile, joinGraph, grainDerivation, measureDerivation);
        return new L2GrainMeasureDeriver.DerivationResult(
            mvType,
            grainDerivation.grain,
            grainDerivation.grain,
            measureDerivation.measures,
            joinGraph,
            coverage(grainDerivation, measureDerivation, predicateClassification),
            measureDerivation.blockingReasons,
            reviewWarnings(joinGraph, mvType)
        );
    }

    private static String mvType(Map<String, Object> advancedStructureProfile,
                                 List<Map<String, Object>> joinGraph,
                                 GrainDerivationPlan grainDerivation,
                                 MeasureDerivationPlan measureDerivation) {
        if (!mapList(advancedStructureProfile.get("ctes")).isEmpty()
            || !mapList(advancedStructureProfile.get("subqueries")).isEmpty()) {
            return L2GrainMeasureDeriver.MV_TYPE_COMMON_SUBGRAPH;
        }
        if (!joinGraph.isEmpty()) {
            return joinGraph.size() >= 2 && !measureDerivation.measures.isEmpty()
                ? L2GrainMeasureDeriver.MV_TYPE_STAR_AGG
                : L2GrainMeasureDeriver.MV_TYPE_PREJOIN;
        }
        return grainDerivation.hasTimeRollup
            ? L2GrainMeasureDeriver.MV_TYPE_ROLLUP
            : L2GrainMeasureDeriver.MV_TYPE_PARAMETERIZED_AGG;
    }

    private static List<Map<String, Object>> reviewWarnings(List<Map<String, Object>> joinGraph, String mvType) {
        if (L2GrainMeasureDeriver.MV_TYPE_COMMON_SUBGRAPH.equals(mvType) || joinGraph == null || joinGraph.isEmpty()) {
            return Collections.emptyList();
        }
        if (L2GrainMeasureDeriver.MV_TYPE_STAR_AGG.equals(mvType)) {
            LinkedHashMap<String, Object> warning = warning(
                "STAR_SCHEMA_METADATA_MISSING",
                "缺少唯一键、维表基数与 Join 选择率元数据，STAR_AGG_MV 只能基于静态 Join 拓扑和指标来源保守生成。"
            );
            warning.put("requiredEvidence", Arrays.asList(
                "FACT_TABLE_ROW_COUNT",
                "DIMENSION_KEY_UNIQUENESS",
                "JOIN_SELECTIVITY"
            ));
            return Collections.<Map<String, Object>>singletonList(warning);
        }
        LinkedHashMap<String, Object> warning = warning(
            "ROW_AMPLIFICATION_METADATA_MISSING",
            "缺少唯一键、表基数与 Join 选择率元数据，PREJOIN_MV 只能说明 Join key 形态安全，不能证明无行数放大。"
        );
        warning.put("requiredEvidence", Arrays.asList("JOIN_KEY_UNIQUENESS", "TABLE_CARDINALITY", "JOIN_SELECTIVITY"));
        return Collections.<Map<String, Object>>singletonList(warning);
    }

    private static LinkedHashMap<String, Object> warning(String code, String description) {
        LinkedHashMap<String, Object> warning = new LinkedHashMap<String, Object>();
        warning.put("code", code);
        warning.put("description", description);
        warning.put("generatedAllowed", Boolean.TRUE);
        return warning;
    }
}
