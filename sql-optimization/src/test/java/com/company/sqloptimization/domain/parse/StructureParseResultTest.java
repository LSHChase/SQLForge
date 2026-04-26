package com.company.sqloptimization.domain.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

class StructureParseResultTest {

    @Test
    void shouldApplyAggregateAssessmentToResponseLevelFields() {
        StructureParseIssue issue = new StructureParseIssue();
        issue.setSeverity(StructureParseIssueSeverity.HIGH);
        issue.setImportant(Boolean.TRUE);
        issue.setUrgent(Boolean.TRUE);
        issue.setAffectedReportCount(Integer.valueOf(5));

        StructureParseResult result = new StructureParseResult();
        result.setIssues(Collections.singletonList(issue));
        result.applyAssessment(StructureParsePriorityScorer.assessAll(result.getIssues()));

        assertEquals(StructureParsePriorityLevel.P1, result.getPriorityLevel());
        assertTrue(result.getPriorityScore().intValue() >= 80);
        assertTrue(Boolean.TRUE.equals(result.getImportant()));
        assertTrue(Boolean.TRUE.equals(result.getUrgent()));
    }
}
