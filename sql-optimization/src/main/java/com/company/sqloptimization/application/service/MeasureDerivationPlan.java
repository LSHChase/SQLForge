package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.GrainMeasureProfileValues.immutableMapList;

import java.util.List;
import java.util.Map;

final class MeasureDerivationPlan {

    final List<Map<String, Object>> measures;
    final List<Map<String, Object>> blockingReasons;

    MeasureDerivationPlan(List<Map<String, Object>> measures, List<Map<String, Object>> blockingReasons) {
        this.measures = immutableMapList(measures);
        this.blockingReasons = immutableMapList(blockingReasons);
    }
}
