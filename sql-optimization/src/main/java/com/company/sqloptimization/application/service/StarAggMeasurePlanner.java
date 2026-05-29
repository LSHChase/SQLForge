package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggProfileValues.mapList;
import static com.company.sqloptimization.application.service.StarAggProfileValues.mapValue;
import static com.company.sqloptimization.application.service.StarAggProfileValues.reason;
import static com.company.sqloptimization.application.service.StarAggProfileValues.stringList;
import static com.company.sqloptimization.application.service.StarAggProfileValues.text;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.isIdentifierReference;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.normalizeExpression;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.qualifier;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.qualifiedReferences;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class StarAggMeasurePlanner {

    private StarAggMeasurePlanner() {
    }

    static StarAggMeasurePlan measurePlan(Map<String, Object> advancedStructureProfile,
                                          L2GrainMeasureDeriver.DerivationResult derivation,
                                          StarAggRelationCatalog relationCatalog,
                                          StarAggFactPlan factPlan) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        List<StarAggMeasureColumn> columns = new ArrayList<StarAggMeasureColumn>();
        List<Map<String, Object>> sources = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> measure : derivation.getMeasures()) {
            List<Map<String, Object>> components = mapList(measure.get("components"));
            if (components.isEmpty()) {
                addMeasureColumn(columns, sources, reasons, measure, advancedStructureProfile, relationCatalog, factPlan);
            } else {
                for (Map<String, Object> component : components) {
                    addMeasureColumn(columns, sources, reasons, component, advancedStructureProfile, relationCatalog, factPlan);
                }
                sources.add(measureSourceEvidence(measure, sourceColumnsForExpression(text(measure.get("sourceExpression")),
                    advancedStructureProfile), factPlan));
            }
        }
        if (columns.isEmpty()) {
            reasons.add(reason("MEASURE_REQUIRED", "缺少可写入 STAR_AGG_MV 的指标列。"));
        }
        return reasons.isEmpty() ? StarAggMeasurePlan.generated(columns, sources) : StarAggMeasurePlan.blocked(reasons);
    }

    static List<String> sourceColumnsForExpression(String expression, Map<String, Object> advancedStructureProfile) {
        String normalized = normalizeExpression(expression);
        for (Map<String, Object> aggregation : mapList(advancedStructureProfile.get("aggregations"))) {
            if (normalized.equals(normalizeExpression(text(aggregation.get("expression"))))) {
                return stringList(aggregation.get("sourceColumns"));
            }
        }
        return qualifiedReferences(expression);
    }

    static List<String> measureSourceColumns(Map<String, Object> advancedStructureProfile) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (Map<String, Object> aggregation : mapList(advancedStructureProfile.get("aggregations"))) {
            result.addAll(stringList(aggregation.get("sourceColumns")));
        }
        return new ArrayList<String>(result);
    }

    private static void addMeasureColumn(List<StarAggMeasureColumn> columns,
                                         List<Map<String, Object>> sources,
                                         List<Map<String, Object>> reasons,
                                         Map<String, Object> measure,
                                         Map<String, Object> advancedStructureProfile,
                                         StarAggRelationCatalog relationCatalog,
                                         StarAggFactPlan factPlan) {
        String sourceExpression = text(measure.get("sourceExpression"));
        String name = text(measure.get("name"));
        List<String> sourceColumns = sourceColumnsForExpression(sourceExpression, advancedStructureProfile);
        for (String sourceColumn : sourceColumns) {
            if (!isIdentifierReference(sourceColumn) || !StringUtils.hasText(qualifier(sourceColumn))) {
                reasons.add(reason("STAR_AGG_MEASURE_SOURCE_NOT_FACT", "指标字段缺少事实表限定，不能生成 STAR_AGG_MV。"));
                continue;
            }
            StarAggRelationSpec relation = relationCatalog.relationForColumn(sourceColumn);
            if (relation == null || !factPlan.factKey.equals(relation.key)) {
                reasons.add(reason("STAR_AGG_MEASURE_SOURCE_NOT_FACT", "STAR_AGG_MV 指标必须来自事实表字段。"));
            }
        }
        columns.add(new StarAggMeasureColumn(name, sourceExpression));
        sources.add(measureSourceEvidence(measure, sourceColumns, factPlan));
    }

    private static LinkedHashMap<String, Object> measureSourceEvidence(Map<String, Object> measure,
                                                                       List<String> sourceColumns,
                                                                       StarAggFactPlan factPlan) {
        LinkedHashMap<String, Object> source = new LinkedHashMap<String, Object>();
        source.put("name", text(measure.get("name")));
        source.put("measureType", text(measure.get("measureType")));
        source.put("sourceExpression", text(measure.get("sourceExpression")));
        source.put("sourceColumns", new ArrayList<String>(sourceColumns));
        source.put("factTable", mapValue(factPlan.factTable, "tableName"));
        source.put("factAlias", mapValue(factPlan.factTable, "alias"));
        return source;
    }
}
