package com.company.sqloptimization.domain.parse;

import java.util.List;

public final class StructureParsePriorityScorer {

    private StructureParsePriorityScorer() {
    }

    public static StructureParsePriorityAssessment assess(StructureParseIssue issue) {
        StructureParseIssueSeverity severity = issue == null || issue.getSeverity() == null
            ? StructureParseIssueSeverity.INFO
            : issue.getSeverity();
        boolean important = issue != null && Boolean.TRUE.equals(issue.getImportant());
        boolean urgent = issue != null && Boolean.TRUE.equals(issue.getUrgent());
        int score = baseScore(severity);
        if (important) {
            score += 10;
        }
        if (urgent) {
            score += 15;
        }
        score += Math.min(10, normalizedImpact(issue == null ? null : issue.getAffectedReportCount()));
        score += Math.min(5, normalizedImpact(issue == null ? null : issue.getAffectedSqlCount()));
        if (score > 100) {
            score = 100;
        }
        StructureParsePriorityAssessment assessment =
            new StructureParsePriorityAssessment(score, resolvePriorityLevel(score, important, urgent), important, urgent);
        if (issue != null) {
            issue.setPriorityScore(Integer.valueOf(assessment.getPriorityScore()));
            issue.setPriorityLevel(assessment.getPriorityLevel());
        }
        return assessment;
    }

    public static StructureParsePriorityAssessment assessAll(List<StructureParseIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return new StructureParsePriorityAssessment(0, StructureParsePriorityLevel.P4, false, false);
        }
        int bestScore = 0;
        StructureParsePriorityLevel bestLevel = StructureParsePriorityLevel.P4;
        boolean important = false;
        boolean urgent = false;
        for (StructureParseIssue issue : issues) {
            StructureParsePriorityAssessment assessment = assess(issue);
            if (assessment.getPriorityScore() > bestScore) {
                bestScore = assessment.getPriorityScore();
                bestLevel = assessment.getPriorityLevel();
            }
            important = important || assessment.isImportant();
            urgent = urgent || assessment.isUrgent();
        }
        return new StructureParsePriorityAssessment(bestScore, bestLevel, important, urgent);
    }

    private static int baseScore(StructureParseIssueSeverity severity) {
        switch (severity) {
            case CRITICAL:
                return 90;
            case HIGH:
                return 70;
            case MEDIUM:
                return 50;
            case LOW:
                return 25;
            case INFO:
            default:
                return 10;
        }
    }

    private static int normalizedImpact(Integer impact) {
        if (impact == null || impact.intValue() <= 0) {
            return 0;
        }
        return Math.min(impact.intValue(), 10);
    }

    private static StructureParsePriorityLevel resolvePriorityLevel(int score, boolean important, boolean urgent) {
        if (score >= 90 || (important && urgent && score >= 80)) {
            return StructureParsePriorityLevel.P1;
        }
        if (score >= 70) {
            return StructureParsePriorityLevel.P2;
        }
        if (score >= 40) {
            return StructureParsePriorityLevel.P3;
        }
        return StructureParsePriorityLevel.P4;
    }
}
