package com.company.sqloptimization.application.service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

final class StarAggCandidateEvidence {

    private StarAggCandidateEvidence() {
    }

    static Map<String, Object> starSchemaEvidence(StarAggFactPlan factPlan,
                                                  StarAggJoinPlan joinPlan,
                                                  StarAggDimensionPlan dimensionPlan,
                                                  StarAggMeasurePlan measurePlan) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("factInference", factPlan.inference);
        evidence.put("joinCount", Integer.valueOf(joinPlan.joinCount));
        evidence.put("dimensionTableCount", Integer.valueOf(factPlan.dimensionTables.size()));
        evidence.put("dimensionColumnCount", Integer.valueOf(dimensionPlan.dimensions.size()));
        evidence.put("measureColumnCount", Integer.valueOf(measurePlan.columns.size()));
        evidence.put("claimBoundary", "STATIC_STAR_SCHEMA_INFERENCE_WITHOUT_CARDINALITY_METADATA");
        evidence.put("missingRuntimeEvidence", Arrays.asList(
            "FACT_TABLE_ROW_COUNT",
            "DIMENSION_KEY_UNIQUENESS",
            "JOIN_SELECTIVITY"
        ));
        return evidence;
    }
}
