package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.GrainMeasureProfileValues.immutableMapList;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MeasureDeriveOutcome {

    final Map<String, Object> measure;
    final List<Map<String, Object>> blockingReasons;

    MeasureDeriveOutcome(Map<String, Object> measure, List<Map<String, Object>> blockingReasons) {
        this.measure = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(measure));
        this.blockingReasons = immutableMapList(blockingReasons);
    }

    static MeasureDeriveOutcome single(Map<String, Object> measure) {
        return new MeasureDeriveOutcome(measure, Collections.<Map<String, Object>>emptyList());
    }
}
