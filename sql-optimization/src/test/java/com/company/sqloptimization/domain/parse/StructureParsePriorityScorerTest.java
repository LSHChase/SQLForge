package com.company.sqloptimization.domain.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class StructureParsePriorityScorerTest {

    @Test
    void shouldElevateCriticalImportantUrgentIssuesToP1() {
        StructureParseIssue issue = new StructureParseIssue();
        issue.setSeverity(StructureParseIssueSeverity.CRITICAL);
        issue.setImportant(Boolean.TRUE);
        issue.setUrgent(Boolean.TRUE);
        issue.setAffectedReportCount(Integer.valueOf(8));
        issue.setAffectedSqlCount(Integer.valueOf(4));

        StructureParsePriorityAssessment assessment = StructureParsePriorityScorer.assess(issue);

        assertEquals(StructureParsePriorityLevel.P1, assessment.getPriorityLevel());
        assertTrue(assessment.getPriorityScore() >= 90);
        assertTrue(assessment.isImportant());
        assertTrue(assessment.isUrgent());
        assertEquals(Integer.valueOf(100), issue.getPriorityScore());
        assertEquals(StructureParsePriorityLevel.P1, issue.getPriorityLevel());
    }

    @Test
    void shouldKeepLowSignalIssuesAtP4() {
        StructureParseIssue issue = new StructureParseIssue();
        issue.setSeverity(StructureParseIssueSeverity.INFO);
        issue.setImportant(Boolean.FALSE);
        issue.setUrgent(Boolean.FALSE);

        StructureParsePriorityAssessment assessment = StructureParsePriorityScorer.assess(issue);

        assertEquals(StructureParsePriorityLevel.P4, assessment.getPriorityLevel());
        assertEquals(10, assessment.getPriorityScore());
    }

    @Test
    void shouldPromoteAggregateAssessmentToHighestIssuePriority() {
        StructureParseIssue highImpact = new StructureParseIssue();
        highImpact.setSeverity(StructureParseIssueSeverity.HIGH);
        highImpact.setImportant(Boolean.TRUE);
        highImpact.setUrgent(Boolean.FALSE);
        highImpact.setAffectedReportCount(Integer.valueOf(6));

        StructureParseIssue lowImpact = new StructureParseIssue();
        lowImpact.setSeverity(StructureParseIssueSeverity.LOW);
        lowImpact.setImportant(Boolean.FALSE);
        lowImpact.setUrgent(Boolean.FALSE);

        StructureParsePriorityAssessment assessment =
            StructureParsePriorityScorer.assessAll(Arrays.asList(lowImpact, highImpact));

        assertEquals(StructureParsePriorityLevel.P2, assessment.getPriorityLevel());
        assertTrue(assessment.getPriorityScore() >= 80);
        assertTrue(assessment.isImportant());
    }
}
