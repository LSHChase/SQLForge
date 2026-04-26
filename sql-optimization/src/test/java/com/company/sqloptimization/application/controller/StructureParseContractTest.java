package com.company.sqloptimization.application.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.sqloptimization.application.controller.vo.StructureParseIssueVO;
import com.company.sqloptimization.application.controller.vo.StructureParseLogicalObjectHitVO;
import com.company.sqloptimization.application.controller.vo.StructureParseQueryDateSummaryVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import com.company.sqloptimization.domain.parse.StructureParseComplexityLevel;
import com.company.sqloptimization.domain.parse.StructureParseIssueDomain;
import com.company.sqloptimization.domain.parse.StructureLogicalObjectType;
import com.company.sqloptimization.domain.parse.StructureParseQueryDateStatus;
import com.company.sqloptimization.domain.parse.StructureParseIssueSeverity;
import com.company.sqloptimization.domain.parse.StructureParsePriorityLevel;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class StructureParseContractTest {

    @Test
    void shouldExposeStableStructureParseResponseContract() {
        StructureParseIssueVO issue = new StructureParseIssueVO();
        issue.setIssueCode("NO_PARTITION_FILTER");
        issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE.name());
        issue.setIssueScene("MISSING_PARTITION_PREDICATE");
        issue.setSeverity(StructureParseIssueSeverity.HIGH.name());
        issue.setPriorityScore(Integer.valueOf(80));
        issue.setPriorityLevel(StructureParsePriorityLevel.P2.name());
        issue.setSuggestedAction("Add a partition predicate on the partition key.");
        issue.setImportant(Boolean.TRUE);
        issue.setUrgent(Boolean.FALSE);

        StructureParseQueryDateSummaryVO queryDateSummary = new StructureParseQueryDateSummaryVO();
        queryDateSummary.setQueryDateStart("2026-04-01");
        queryDateSummary.setQueryDateEnd("2026-04-01");
        queryDateSummary.setQueryDateFields(Collections.singletonList("dt"));
        queryDateSummary.setQueryDateStatus(StructureParseQueryDateStatus.RESOLVED.name());

        StructureParseLogicalObjectHitVO logicalObjectHit = new StructureParseLogicalObjectHitVO();
        logicalObjectHit.setObjectType(StructureLogicalObjectType.BUSINESS_VIEW.name());
        logicalObjectHit.setObjectName("sales_daily_view");
        logicalObjectHit.setMatchSource("COMMENT_CONTEXT");
        logicalObjectHit.setResolved(Boolean.TRUE);
        logicalObjectHit.setMappedPhysicalTargets(Collections.singletonList("dw.sales_daily"));

        StructureParseResponseVO response = new StructureParseResponseVO();
        response.setParseTaskId("parse-001");
        response.setSyntaxStatus("VALID");
        response.setComplexityLevel(StructureParseComplexityLevel.COMPLEX.name());
        response.setSqlType("SELECT");
        response.setQueryDateSummary(queryDateSummary);
        response.setLogicalObjectHits(Collections.singletonList(logicalObjectHit));
        response.setIssues(Collections.singletonList(issue));
        response.setPriorityScore(Integer.valueOf(80));
        response.setPriorityLevel(StructureParsePriorityLevel.P2.name());
        response.setImportant(Boolean.TRUE);
        response.setUrgent(Boolean.FALSE);

        assertEquals("STRUCTURE", response.getParseType());
        assertEquals("parse-001", response.getParseTaskId());
        assertEquals("COMPLEX", response.getComplexityLevel());
        assertEquals("NO_PARTITION_FILTER", response.getIssues().get(0).getIssueCode());
        assertEquals("Add a partition predicate on the partition key.", response.getIssues().get(0).getSuggestedAction());
        assertEquals("RESOLVED", response.getQueryDateSummary().getQueryDateStatus());
        assertEquals("BUSINESS_VIEW", response.getLogicalObjectHits().get(0).getObjectType());
        assertEquals("P2", response.getPriorityLevel());
    }
}
