package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.GrainMeasureProfileValues.immutableMapList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class L2GrainMeasureDeriver {

    static final String MV_TYPE_PARAMETERIZED_AGG = "PARAMETERIZED_AGG_MV";
    static final String MV_TYPE_PREJOIN = "PREJOIN_MV";
    static final String MV_TYPE_STAR_AGG = "STAR_AGG_MV";
    static final String MV_TYPE_ROLLUP = "ROLLUP_MV";
    static final String MV_TYPE_COMMON_SUBGRAPH = "COMMON_SUBGRAPH_MV";

    private L2GrainMeasureDeriver() {
    }

    static DerivationResult derive(SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                   L2PredicateClassifier.PredicateClassificationResult predicateClassification) {
        return derive(profile == null ? null : profile.toAdvancedStructureProfile(), predicateClassification);
    }

    static DerivationResult derive(Map<String, Object> advancedStructureProfile,
                                   L2PredicateClassifier.PredicateClassificationResult predicateClassification) {
        return GrainMeasureDerivationEngine.derive(advancedStructureProfile, predicateClassification);
    }

    static final class DerivationResult {
        private final String mvType;
        private final List<String> grain;
        private final List<String> dimensions;
        private final List<Map<String, Object>> measures;
        private final List<Map<String, Object>> joinGraph;
        private final Map<String, Object> coverage;
        private final List<Map<String, Object>> blockingReasons;
        private final List<Map<String, Object>> reviewWarnings;

        DerivationResult(String mvType,
                         List<String> grain,
                         List<String> dimensions,
                         List<Map<String, Object>> measures,
                         List<Map<String, Object>> joinGraph,
                         Map<String, Object> coverage,
                         List<Map<String, Object>> blockingReasons,
                         List<Map<String, Object>> reviewWarnings) {
            this.mvType = mvType;
            this.grain = Collections.unmodifiableList(new ArrayList<String>(grain));
            this.dimensions = Collections.unmodifiableList(new ArrayList<String>(dimensions));
            this.measures = immutableMapList(measures);
            this.joinGraph = immutableMapList(joinGraph);
            this.coverage = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(coverage));
            this.blockingReasons = immutableMapList(blockingReasons);
            this.reviewWarnings = immutableMapList(reviewWarnings);
        }

        static DerivationResult empty() {
            LinkedHashMap<String, Object> coverage = new LinkedHashMap<String, Object>();
            coverage.put("coversProjection", Boolean.FALSE);
            coverage.put("coversFilters", Boolean.FALSE);
            coverage.put("coversGrouping", Boolean.FALSE);
            coverage.put("coversMeasures", Boolean.FALSE);
            coverage.put("coversSecurity", Boolean.FALSE);
            return new DerivationResult(
                MV_TYPE_PARAMETERIZED_AGG,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<Map<String, Object>>emptyList(),
                Collections.<Map<String, Object>>emptyList(),
                coverage,
                Collections.<Map<String, Object>>emptyList(),
                Collections.<Map<String, Object>>emptyList()
            );
        }

        String getMvType() {
            return mvType;
        }

        List<String> getGrain() {
            return grain;
        }

        List<String> getDimensions() {
            return dimensions;
        }

        List<Map<String, Object>> getMeasures() {
            return measures;
        }

        List<Map<String, Object>> getJoinGraph() {
            return joinGraph;
        }

        Map<String, Object> getCoverage() {
            return coverage;
        }

        List<Map<String, Object>> getBlockingReasons() {
            return blockingReasons;
        }

        List<Map<String, Object>> getReviewWarnings() {
            return reviewWarnings;
        }
    }
}
