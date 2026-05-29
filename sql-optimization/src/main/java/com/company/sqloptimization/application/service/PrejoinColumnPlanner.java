package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.cleanName;
import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.cleanReference;
import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.isIdentifierReference;
import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.normalizeName;
import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.qualifier;
import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.uniqueName;
import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.unqualifiedName;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.intValue;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.mapList;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.reason;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.stringList;
import static com.company.sqloptimization.application.service.PrejoinProfileValues.text;
import static com.company.sqloptimization.application.service.PrejoinSqlTextSupport.leftPredicateField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class PrejoinColumnPlanner {

    private PrejoinColumnPlanner() {
    }

    static PrejoinColumnPlan columnPlan(Map<String, Object> advancedStructureProfile,
                                        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
                                        PrejoinJoinPlan joinPlan,
                                        List<Map<String, Object>> baseTables) {
        LinkedHashSet<String> requiredColumns = new LinkedHashSet<String>();
        addSourceColumns(requiredColumns, mapList(advancedStructureProfile.get("projections")));
        addSourceColumns(requiredColumns, mapList(advancedStructureProfile.get("groupBy")));
        addSourceColumns(requiredColumns, mapList(advancedStructureProfile.get("aggregations")));
        if (predicateClassification != null) {
            addSourceColumns(requiredColumns, predicateClassification.getExternalizedPredicates());
            addSourceColumns(requiredColumns, predicateClassification.getSecurityPredicates());
            addSourceColumns(requiredColumns, predicateClassification.getRetainedPredicates());
        }
        for (Map<String, Object> joinKey : joinPlan.joinKeys) {
            addText(requiredColumns, text(joinKey.get("leftKey")));
            addText(requiredColumns, text(joinKey.get("rightKey")));
        }

        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        LinkedHashMap<String, Integer> unqualifiedCounts = unqualifiedCounts(requiredColumns);
        List<PrejoinColumnMapping> mappings = new ArrayList<PrejoinColumnMapping>();
        Set<String> usedOutputNames = new LinkedHashSet<String>();
        for (String sourceColumn : requiredColumns) {
            addMapping(blockingReasons, mappings, usedOutputNames, unqualifiedCounts, sourceColumn, baseTables);
        }
        if (!blockingReasons.isEmpty()) {
            return PrejoinColumnPlan.blocked(blockingReasons);
        }
        return new PrejoinColumnPlan(blockingReasons, mappings);
    }

    private static void addMapping(List<Map<String, Object>> blockingReasons,
                                   List<PrejoinColumnMapping> mappings,
                                   Set<String> usedOutputNames,
                                   LinkedHashMap<String, Integer> unqualifiedCounts,
                                   String sourceColumn,
                                   List<Map<String, Object>> baseTables) {
        if (!isIdentifierReference(sourceColumn)) {
            blockingReasons.add(reason("COMPLEX_FIELD_MAPPING_UNSUPPORTED", "PREJOIN_MV 只能映射可解析的字段引用。"));
            return;
        }
        String qualifier = qualifier(sourceColumn);
        if (!StringUtils.hasText(qualifier) && baseTables.size() > 1) {
            blockingReasons.add(reason("FIELD_AMBIGUITY_UNRESOLVED", "多表 Join 中存在未限定字段，缺少元数据时无法证明字段归属。"));
            return;
        }
        String baseName = unqualifiedName(sourceColumn);
        boolean ambiguousName = intValue(unqualifiedCounts.get(normalizeName(baseName))) > 1;
        String outputName = ambiguousName ? cleanName(qualifier + "_" + baseName) : cleanName(baseName);
        mappings.add(new PrejoinColumnMapping(sourceColumn, uniqueName(outputName, usedOutputNames), qualifier, ambiguousName));
    }

    private static void addSourceColumns(Set<String> target, List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            boolean added = false;
            for (String sourceColumn : stringList(item.get("sourceColumns"))) {
                addText(target, sourceColumn);
                added = true;
            }
            if (!added) {
                addText(target, leftPredicateField(text(item.get("expression"))));
            }
        }
    }

    private static void addText(Set<String> target, String value) {
        if (target != null && StringUtils.hasText(value)) {
            target.add(cleanReference(value));
        }
    }

    private static LinkedHashMap<String, Integer> unqualifiedCounts(Set<String> sourceColumns) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (String sourceColumn : sourceColumns) {
            String key = normalizeName(unqualifiedName(sourceColumn));
            Integer current = counts.get(key);
            counts.put(key, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
        }
        return counts;
    }
}
