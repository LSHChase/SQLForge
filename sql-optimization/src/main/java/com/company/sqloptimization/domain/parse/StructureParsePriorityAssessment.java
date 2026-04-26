package com.company.sqloptimization.domain.parse;

public class StructureParsePriorityAssessment {

    private final int priorityScore;
    private final StructureParsePriorityLevel priorityLevel;
    private final boolean important;
    private final boolean urgent;

    public StructureParsePriorityAssessment(int priorityScore,
                                            StructureParsePriorityLevel priorityLevel,
                                            boolean important,
                                            boolean urgent) {
        this.priorityScore = priorityScore;
        this.priorityLevel = priorityLevel;
        this.important = important;
        this.urgent = urgent;
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    public StructureParsePriorityLevel getPriorityLevel() {
        return priorityLevel;
    }

    public boolean isImportant() {
        return important;
    }

    public boolean isUrgent() {
        return urgent;
    }
}
