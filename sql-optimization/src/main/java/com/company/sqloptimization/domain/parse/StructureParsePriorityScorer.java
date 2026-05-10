package com.company.sqloptimization.domain.parse;

import java.util.List;

public final class StructureParsePriorityScorer {

    private StructureParsePriorityScorer() {
    }

    public static StructureParsePriorityAssessment assess(StructureParseIssue issue) {
        StructureParseIssueScenario scenario = resolveScenario(issue);
        StructureParseIssueSeverity severity = issue == null || issue.getSeverity() == null
            ? scenario.getDefaultSeverity()
            : issue.getSeverity();
        boolean important = issue != null && issue.getImportant() != null
            ? Boolean.TRUE.equals(issue.getImportant())
            : scenario.isDefaultImportant();
        boolean urgent = issue != null && issue.getUrgent() != null
            ? Boolean.TRUE.equals(issue.getUrgent())
            : scenario.isDefaultUrgent();
        int score = baseScore(severity) + scenario.getSceneWeight();
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
            normalizeIssue(issue, scenario, severity, important, urgent);
            issue.setPriorityScore(Integer.valueOf(assessment.getPriorityScore()));
            issue.setPriorityLevel(assessment.getPriorityLevel());
        }
        return assessment;
    }

    public static StructureParseIssueScoringSnapshot snapshot(StructureParseIssue issue) {
        StructureParsePriorityAssessment assessment = assess(issue);
        StructureParseIssueScenario scenario = resolveScenario(issue);
        return new StructureParseIssueScoringSnapshot(
            issue == null || issue.getIssueScene() == null ? scenario.getIssueScene() : issue.getIssueScene(),
            issue == null || issue.getIssueDomain() == null ? scenario.getDefaultDomain() : issue.getIssueDomain(),
            issue == null || issue.getSeverity() == null ? scenario.getDefaultSeverity() : issue.getSeverity(),
            assessment.getPriorityLevel(),
            assessment.getPriorityScore(),
            assessment.isImportant(),
            assessment.isUrgent()
        );
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
        if (StructureParseIssueSeverity.CRITICAL == severity) {
            return 90;
        }
        if (StructureParseIssueSeverity.HIGH == severity) {
            return 70;
        }
        if (StructureParseIssueSeverity.MEDIUM == severity) {
            return 50;
        }
        if (StructureParseIssueSeverity.LOW == severity) {
            return 25;
        }
        return 10;
    }

    private static int normalizedImpact(Integer impact) {
        if (impact == null || impact.intValue() <= 0) {
            return 0;
        }
        return Math.min(impact.intValue(), 10);
    }

    private static StructureParseIssueScenario resolveScenario(StructureParseIssue issue) {
        return StructureParseIssueScenario.resolve(issue == null ? null : issue.getIssueScene());
    }

    private static void normalizeIssue(StructureParseIssue issue,
                                       StructureParseIssueScenario scenario,
                                       StructureParseIssueSeverity severity,
                                       boolean important,
                                       boolean urgent) {
        if (issue.getIssueScene() == null) {
            issue.setIssueScene(scenario.getIssueScene());
        }
        if (issue.getIssueDomain() == null) {
            issue.setIssueDomain(scenario.getDefaultDomain());
        }
        if (issue.getSeverity() == null) {
            issue.setSeverity(severity);
        }
        if (issue.getImportant() == null) {
            issue.setImportant(Boolean.valueOf(important));
        }
        if (issue.getUrgent() == null) {
            issue.setUrgent(Boolean.valueOf(urgent));
        }
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
