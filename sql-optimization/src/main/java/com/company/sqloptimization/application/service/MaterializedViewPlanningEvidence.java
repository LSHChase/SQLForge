package com.company.sqloptimization.application.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class MaterializedViewPlanningEvidence {

    private final String candidateId;
    private final String generationSource;
    private final QueryWrapperPreserver.WrapperAnalysis wrapperAnalysis;
    private final Map<String, Object> explainEvidence;
    private final Map<String, Object> metadataEvidence;
    private final Map<String, Object> plannerEvidence;
    private final List<Map<String, Object>> plannerWarnings;

    MaterializedViewPlanningEvidence(String candidateId,
                                     String generationSource,
                                     QueryWrapperPreserver.WrapperAnalysis wrapperAnalysis,
                                     Map<String, Object> explainEvidence,
                                     Map<String, Object> metadataEvidence,
                                     Map<String, Object> plannerEvidence,
                                     List<Map<String, Object>> plannerWarnings) {
        this.candidateId = candidateId;
        this.generationSource = generationSource;
        this.wrapperAnalysis = wrapperAnalysis;
        this.explainEvidence = explainEvidence;
        this.metadataEvidence = metadataEvidence;
        this.plannerEvidence = plannerEvidence;
        this.plannerWarnings = plannerWarnings == null
            ? Collections.<Map<String, Object>>emptyList()
            : plannerWarnings;
    }

    String getCandidateId() {
        return candidateId;
    }

    String getGenerationSource() {
        return generationSource;
    }

    QueryWrapperPreserver.WrapperAnalysis getWrapperAnalysis() {
        return wrapperAnalysis;
    }

    Map<String, Object> getExplainEvidence() {
        return explainEvidence;
    }

    Map<String, Object> getMetadataEvidence() {
        return metadataEvidence;
    }

    Map<String, Object> getPlannerEvidence() {
        return plannerEvidence;
    }

    List<Map<String, Object>> getPlannerWarnings() {
        return plannerWarnings;
    }
}
