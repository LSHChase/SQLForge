package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class StarAggMeasurePlan {

    final List<Map<String, Object>> blockingReasons;
    final List<StarAggMeasureColumn> columns;
    final List<Map<String, Object>> measureSources;

    private StarAggMeasurePlan(List<Map<String, Object>> blockingReasons,
                               List<StarAggMeasureColumn> columns,
                               List<Map<String, Object>> measureSources) {
        this.blockingReasons = blockingReasons;
        this.columns = columns;
        this.measureSources = measureSources;
    }

    static StarAggMeasurePlan blocked() {
        return blocked(Collections.<Map<String, Object>>emptyList());
    }

    static StarAggMeasurePlan blocked(List<Map<String, Object>> blockingReasons) {
        return new StarAggMeasurePlan(
            blockingReasons,
            Collections.<StarAggMeasureColumn>emptyList(),
            Collections.<Map<String, Object>>emptyList()
        );
    }

    static StarAggMeasurePlan generated(List<StarAggMeasureColumn> columns, List<Map<String, Object>> measureSources) {
        return new StarAggMeasurePlan(Collections.<Map<String, Object>>emptyList(), columns, measureSources);
    }

    List<String> ddlSelectItems() {
        List<String> items = new ArrayList<String>();
        for (StarAggMeasureColumn column : columns) {
            items.add(column.sourceExpression + " AS " + column.name);
        }
        return items;
    }
}
