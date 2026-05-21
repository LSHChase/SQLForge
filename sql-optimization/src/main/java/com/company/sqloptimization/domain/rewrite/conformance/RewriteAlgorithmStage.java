package com.company.sqloptimization.domain.rewrite.conformance;

import java.util.List;
import java.util.Map;

public class RewriteAlgorithmStage {

    private final String stageId;
    private final String stageName;
    private final String expectedStep;
    private final String implementationStatus;
    private final String conformanceLevel;
    private final List<Map<String, Object>> evidence;
    private final List<String> gaps;
    private final Map<String, Object> attributes;

    public RewriteAlgorithmStage(String stageId,
                                 String stageName,
                                 String expectedStep,
                                 String implementationStatus,
                                 String conformanceLevel,
                                 List<Map<String, Object>> evidence,
                                 List<String> gaps,
                                 Map<String, Object> attributes) {
        this.stageId = RewriteAlgorithmConformanceCollections.text(stageId);
        this.stageName = RewriteAlgorithmConformanceCollections.text(stageName);
        this.expectedStep = RewriteAlgorithmConformanceCollections.text(expectedStep);
        this.implementationStatus = RewriteAlgorithmConformanceCollections.text(implementationStatus);
        this.conformanceLevel = RewriteAlgorithmConformanceCollections.text(conformanceLevel);
        this.evidence = RewriteAlgorithmConformanceCollections.immutableMaps(evidence);
        this.gaps = RewriteAlgorithmConformanceCollections.immutableStrings(gaps);
        this.attributes = RewriteAlgorithmConformanceCollections.immutableMap(attributes);
    }

    public boolean hasGap(String gap) {
        return gaps.contains(RewriteAlgorithmConformanceCollections.text(gap));
    }

    public String getStageId() {
        return stageId;
    }

    public String getStageName() {
        return stageName;
    }

    public String getExpectedStep() {
        return expectedStep;
    }

    public String getImplementationStatus() {
        return implementationStatus;
    }

    public String getConformanceLevel() {
        return conformanceLevel;
    }

    public List<Map<String, Object>> getEvidence() {
        return evidence;
    }

    public List<String> getGaps() {
        return gaps;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
