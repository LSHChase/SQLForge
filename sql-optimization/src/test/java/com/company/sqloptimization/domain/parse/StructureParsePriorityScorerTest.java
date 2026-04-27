package com.company.sqloptimization.domain.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class StructureParsePriorityScorerTest {

    @Test
    void shouldApplyScenarioDefaultsWhenIssueOnlyHasScene() {
        StructureParseIssue issue = new StructureParseIssue();
        issue.setIssueScene("ROUTE_HINT_CONFLICT");

        StructureParsePriorityAssessment assessment = StructureParsePriorityScorer.assess(issue);

        assertEquals(StructureParseIssueDomain.ROUTING, issue.getIssueDomain());
        assertEquals(StructureParseIssueSeverity.HIGH, issue.getSeverity());
        assertTrue(issue.getImportant());
        assertTrue(issue.getUrgent());
        assertEquals(StructureParsePriorityLevel.P1, assessment.getPriorityLevel());
    }

    @Test
    void shouldKeepExplicitFlagsOverScenarioDefaults() {
        StructureParseIssue issue = new StructureParseIssue();
        issue.setIssueScene("MISSING_FILTER");
        issue.setImportant(Boolean.FALSE);
        issue.setUrgent(Boolean.FALSE);

        StructureParsePriorityAssessment assessment = StructureParsePriorityScorer.assess(issue);

        assertFalse(assessment.isImportant());
        assertFalse(assessment.isUrgent());
        assertEquals(StructureParsePriorityLevel.P2, assessment.getPriorityLevel());
    }

    @Test
    void shouldUseHighestScoredIssueForBatchAssessment() {
        StructureParseIssue low = new StructureParseIssue();
        low.setIssueScene("GENERAL_WARNING");
        StructureParseIssue high = new StructureParseIssue();
        high.setIssueScene("MISSING_FILTER");
        high.setAffectedReportCount(Integer.valueOf(5));

        StructureParsePriorityAssessment assessment = StructureParsePriorityScorer.assessAll(Arrays.asList(low, high));

        assertEquals(StructureParsePriorityLevel.P1, assessment.getPriorityLevel());
        assertTrue(assessment.isImportant());
        assertTrue(assessment.isUrgent());
    }

    @Test
    void shouldExposeStableScoringSnapshotForStatistics() {
        StructureParseIssue issue = new StructureParseIssue();
        issue.setIssueScene("QUERY_DATE_UNRESOLVED");

        StructureParseIssueScoringSnapshot snapshot = StructureParsePriorityScorer.snapshot(issue);

        assertEquals("QUERY_DATE_UNRESOLVED", snapshot.getIssueScene());
        assertEquals(StructureParseIssueDomain.DATA, snapshot.getIssueDomain());
        assertEquals(StructureParseIssueSeverity.MEDIUM, snapshot.getSeverity());
        assertEquals(StructureParsePriorityLevel.P3, snapshot.getPriorityLevel());
        assertTrue(snapshot.isImportant());
        assertFalse(snapshot.isUrgent());
    }
}
