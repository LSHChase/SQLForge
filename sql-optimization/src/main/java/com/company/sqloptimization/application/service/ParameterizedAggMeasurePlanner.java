package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.ParameterizedAggProfileValues.mapList;
import static com.company.sqloptimization.application.service.ParameterizedAggProfileValues.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ParameterizedAggMeasurePlanner {

    private ParameterizedAggMeasurePlanner() {
    }

    static List<ParameterizedAggMeasureColumn> measureColumns(List<Map<String, Object>> measures) {
        List<ParameterizedAggMeasureColumn> columns = new ArrayList<ParameterizedAggMeasureColumn>();
        for (Map<String, Object> measure : measures) {
            for (Map<String, Object> component : mapList(measure.get("components"))) {
                columns.add(new ParameterizedAggMeasureColumn(
                    text(component.get("name")),
                    text(component.get("sourceExpression"))
                ));
            }
            if (mapList(measure.get("components")).isEmpty()) {
                columns.add(new ParameterizedAggMeasureColumn(text(measure.get("name")), text(measure.get("sourceExpression"))));
            }
        }
        return columns;
    }
}
