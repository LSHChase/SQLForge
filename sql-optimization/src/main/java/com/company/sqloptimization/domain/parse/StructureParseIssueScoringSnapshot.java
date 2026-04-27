package com.company.sqloptimization.domain.parse;

public class StructureParseIssueScoringSnapshot {

    private final String issueScene;
    private final StructureParseIssueDomain issueDomain;
    private final StructureParseIssueSeverity severity;
    private final StructureParsePriorityLevel priorityLevel;
    private final int priorityScore;
    private final boolean important;
    private final boolean urgent;

    public StructureParseIssueScoringSnapshot(String issueScene,
                                              StructureParseIssueDomain issueDomain,
                                              StructureParseIssueSeverity severity,
                                              StructureParsePriorityLevel priorityLevel,
                                              int priorityScore,
                                              boolean important,
                                              boolean urgent) {
        this.issueScene = issueScene;
        this.issueDomain = issueDomain;
        this.severity = severity;
        this.priorityLevel = priorityLevel;
        this.priorityScore = priorityScore;
        this.important = important;
        this.urgent = urgent;
    }

    public String getIssueScene() {
        return issueScene;
    }

    public StructureParseIssueDomain getIssueDomain() {
        return issueDomain;
    }

    public StructureParseIssueSeverity getSeverity() {
        return severity;
    }

    public StructureParsePriorityLevel getPriorityLevel() {
        return priorityLevel;
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    public boolean isImportant() {
        return important;
    }

    public boolean isUrgent() {
        return urgent;
    }
}
