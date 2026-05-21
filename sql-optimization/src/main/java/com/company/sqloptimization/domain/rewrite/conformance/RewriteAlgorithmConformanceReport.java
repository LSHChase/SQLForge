package com.company.sqloptimization.domain.rewrite.conformance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RewriteAlgorithmConformanceReport {

    public static final String SCHEMA_VERSION = "rewrite-algorithm-conformance/v1";

    private final String schemaVersion;
    private final String sourceSchemaVersion;
    private final String algorithmStatus;
    private final List<RewriteAlgorithmStage> stages;
    private final List<String> criticalGaps;
    private final Map<String, Object> outputBundle;
    private final Map<String, Object> attributes;

    public RewriteAlgorithmConformanceReport(String schemaVersion,
                                             String sourceSchemaVersion,
                                             String algorithmStatus,
                                             List<RewriteAlgorithmStage> stages,
                                             List<String> criticalGaps,
                                             Map<String, Object> outputBundle,
                                             Map<String, Object> attributes) {
        this.schemaVersion = RewriteAlgorithmConformanceCollections.text(schemaVersion);
        this.sourceSchemaVersion = RewriteAlgorithmConformanceCollections.text(sourceSchemaVersion);
        this.algorithmStatus = RewriteAlgorithmConformanceCollections.text(algorithmStatus);
        this.stages = RewriteAlgorithmConformanceCollections.immutableList(stages);
        this.criticalGaps = RewriteAlgorithmConformanceCollections.immutableStrings(criticalGaps);
        this.outputBundle = RewriteAlgorithmConformanceCollections.immutableMap(outputBundle);
        this.attributes = RewriteAlgorithmConformanceCollections.immutableMap(attributes);
    }

    public boolean hasStage(String stageName) {
        return firstStage(stageName) != null;
    }

    public RewriteAlgorithmStage firstStage(String stageName) {
        String expected = RewriteAlgorithmConformanceCollections.text(stageName);
        for (RewriteAlgorithmStage stage : stages) {
            if (stage.getStageName().equals(expected)) {
                return stage;
            }
        }
        return null;
    }

    public boolean hasCriticalGap(String gap) {
        return criticalGaps.contains(RewriteAlgorithmConformanceCollections.text(gap));
    }

    public List<String> allGaps() {
        List<String> result = new ArrayList<String>();
        result.addAll(criticalGaps);
        for (RewriteAlgorithmStage stage : stages) {
            result.addAll(stage.getGaps());
        }
        return result;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getSourceSchemaVersion() {
        return sourceSchemaVersion;
    }

    public String getAlgorithmStatus() {
        return algorithmStatus;
    }

    public List<RewriteAlgorithmStage> getStages() {
        return stages;
    }

    public List<String> getCriticalGaps() {
        return criticalGaps;
    }

    public Map<String, Object> getOutputBundle() {
        return outputBundle;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
