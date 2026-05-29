package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RollupProfileValues.mapList;
import static com.company.sqloptimization.application.service.RollupProfileValues.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class RollupMeasureColumnPlanner {

    private RollupMeasureColumnPlanner() {
    }

    static List<RollupMeasureColumn> measureColumns(List<Map<String, Object>> measures) {
        List<RollupMeasureColumn> columns = new ArrayList<RollupMeasureColumn>();
        for (Map<String, Object> measure : measures) {
            for (Map<String, Object> component : mapList(measure.get("components"))) {
                columns.add(new RollupMeasureColumn(text(component.get("name")), text(component.get("sourceExpression"))));
            }
            if (mapList(measure.get("components")).isEmpty()) {
                columns.add(new RollupMeasureColumn(text(measure.get("name")), text(measure.get("sourceExpression"))));
            }
        }
        return columns;
    }
}
