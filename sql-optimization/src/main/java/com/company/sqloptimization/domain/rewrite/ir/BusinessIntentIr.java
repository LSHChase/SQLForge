package com.company.sqloptimization.domain.rewrite.ir;

import java.util.List;
import java.util.Map;

public class BusinessIntentIr {

    private final List<Map<String, Object>> timeAnchors;
    private final List<Map<String, Object>> measures;
    private final List<Map<String, Object>> dimensions;
    private final List<Map<String, Object>> filters;
    private final Map<String, Object> attributes;

    public BusinessIntentIr(List<Map<String, Object>> timeAnchors,
                            List<Map<String, Object>> measures,
                            List<Map<String, Object>> dimensions,
                            List<Map<String, Object>> filters,
                            Map<String, Object> attributes) {
        this.timeAnchors = IrCollections.immutableMaps(timeAnchors);
        this.measures = IrCollections.immutableMaps(measures);
        this.dimensions = IrCollections.immutableMaps(dimensions);
        this.filters = IrCollections.immutableMaps(filters);
        this.attributes = IrCollections.immutableMap(attributes);
    }

    public RewriteIrLayer getLayer() {
        return RewriteIrLayer.L5_BUSINESS_INTENT;
    }

    public List<Map<String, Object>> getTimeAnchors() {
        return timeAnchors;
    }

    public List<Map<String, Object>> getMeasures() {
        return measures;
    }

    public List<Map<String, Object>> getDimensions() {
        return dimensions;
    }

    public List<Map<String, Object>> getFilters() {
        return filters;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
