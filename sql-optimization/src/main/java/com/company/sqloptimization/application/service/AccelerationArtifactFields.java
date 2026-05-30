package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.AccelerationArtifactValues.addIfText;
import static com.company.sqloptimization.application.service.AccelerationArtifactValues.copyMap;
import static com.company.sqloptimization.application.service.AccelerationArtifactValues.mapList;
import static com.company.sqloptimization.application.service.AccelerationArtifactValues.mapValue;
import static com.company.sqloptimization.application.service.AccelerationArtifactValues.stringList;
import static com.company.sqloptimization.application.service.AccelerationArtifactValues.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AccelerationArtifactFields {

    private AccelerationArtifactFields() {
    }

    static List<String> mvFieldNames(L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation,
                                     AccelerationArtifactCandidateBundle candidates) {
        LinkedHashSet<String> fields = new LinkedHashSet<String>();
        if (grainMeasureDerivation != null) {
            fields.addAll(grainMeasureDerivation.getDimensions());
            addMeasureFields(fields, grainMeasureDerivation.getMeasures());
        }
        if (candidates.dynamicSnapshot() != null) {
            fields.addAll(candidates.dynamicSnapshot().getGrain());
            fields.addAll(candidates.dynamicSnapshot().getDimensions());
            addMeasureFields(fields, candidates.dynamicSnapshot().getMeasures());
        }
        if (candidates.prejoin() != null) {
            for (Map<String, Object> mapping : candidates.prejoin().getFieldMappings()) {
                addIfText(fields, text(mapping.get("mvColumn")));
            }
        }
        if (candidates.starAgg() != null) {
            for (Map<String, Object> dimensionSource : candidates.starAgg().getDimensionSources()) {
                addIfText(fields, text(dimensionSource.get("mvColumn")));
            }
        }
        if (candidates.rollup() != null) {
            addIfText(fields, text(mapValue(candidates.rollup().getTimeRollupEvidence(), "mvTimeColumn")));
        }
        if (candidates.commonSubgraph() != null) {
            Map<String, Object> evidence = candidates.commonSubgraph().getCommonSubgraphEvidence();
            for (String outputColumn : stringList(evidence == null ? null : evidence.get("outputColumns"))) {
                addIfText(fields, outputColumn);
            }
        }
        return new ArrayList<String>(fields);
    }

    static List<String> additionalCoverageReferences(
        L2GrainMeasureDeriver.DerivationResult grainMeasureDerivation,
        AccelerationArtifactCandidateBundle candidates) {
        LinkedHashSet<String> references = new LinkedHashSet<String>();
        if (grainMeasureDerivation != null) {
            references.addAll(grainMeasureDerivation.getGrain());
            references.addAll(grainMeasureDerivation.getDimensions());
        }
        if (candidates.dynamicSnapshot() != null) {
            references.addAll(candidates.dynamicSnapshot().getGrain());
            references.addAll(candidates.dynamicSnapshot().getDimensions());
        }
        if (candidates.rollup() != null) {
            addRollupReferences(references, candidates.rollup().getTimeRollupEvidence());
        }
        if (candidates.commonSubgraph() != null) {
            addCommonSubgraphReferences(references, candidates.commonSubgraph().getCommonSubgraphEvidence());
        }
        return new ArrayList<String>(references);
    }

    private static void addRollupReferences(Set<String> references, Map<String, Object> evidence) {
        addIfText(references, text(evidence.get("timeSourceColumn")));
        addIfText(references, text(evidence.get("queryTimeExpression")));
        addIfText(references, text(evidence.get("rewriteRollupExpression")));
        addIfText(references, text(evidence.get("mvTimeColumn")));
    }

    private static void addCommonSubgraphReferences(Set<String> references, Map<String, Object> evidence) {
        Map<String, Object> rewriteCoverage = mapValue(evidence, "rewriteCoverage") instanceof Map<?, ?>
            ? copyMap((Map<?, ?>) mapValue(evidence, "rewriteCoverage"))
            : Collections.<String, Object>emptyMap();
        for (String requiredColumn : stringList(rewriteCoverage.get("requiredColumns"))) {
            addIfText(references, requiredColumn);
        }
    }

    private static void addMeasureFields(Set<String> fields, List<Map<String, Object>> measures) {
        for (Map<String, Object> measure : measures) {
            addIfText(fields, text(measure.get("name")));
            for (Map<String, Object> component : mapList(measure.get("components"))) {
                addIfText(fields, text(component.get("name")));
            }
        }
    }
}
