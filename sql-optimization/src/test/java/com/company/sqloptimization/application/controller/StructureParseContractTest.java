package com.company.sqloptimization.application.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.sqlforge.common.logicalobject.LogicalObjectRef;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import com.company.sqlforge.common.logicalobject.LogicalObjectType;
import com.company.sqloptimization.application.controller.vo.StructureParseFeatureSummaryVO;
import com.company.sqloptimization.application.controller.vo.StructureParseIntentProfileVO;
import com.company.sqloptimization.application.controller.vo.StructureParseIssueVO;
import com.company.sqloptimization.application.controller.vo.StructureParseQueryDateSummaryVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResourceEstimateVO;
import com.company.sqloptimization.application.controller.vo.StructureParseRiskVO;
import com.company.sqloptimization.domain.parse.StructureParseComplexityLevel;
import com.company.sqloptimization.domain.parse.StructureParseIssueDomain;
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

        LogicalObjectSurface logicalObjectHit = new LogicalObjectSurface();
        logicalObjectHit.setObjectType(LogicalObjectType.BUSINESS_VIEW.name());
        logicalObjectHit.setObjectKey(LogicalObjectRef.buildObjectKey(LogicalObjectType.BUSINESS_VIEW, "sales_daily_view"));
        logicalObjectHit.setObjectName("sales_daily_view");
        logicalObjectHit.setSchemaName("sales");
        logicalObjectHit.setMatchSource("COMMENT_CONTEXT");
        logicalObjectHit.setResolved(Boolean.TRUE);
        logicalObjectHit.setMappedPhysicalTargets(Collections.singletonList("dw.sales_daily"));

        StructureParseResponseVO response = new StructureParseResponseVO();
        response.setParseTaskId("parse-001");
        response.setSyntaxStatus("VALID");
        response.setComplexityLevel(StructureParseComplexityLevel.COMPLEX.name());
        response.setSqlType("SELECT");
        response.setSqlFingerprint("fp-001");
        response.setQueryDateSummary(queryDateSummary);
        response.setLogicalObjectHits(Collections.singletonList(logicalObjectHit));
        response.setIssues(Collections.singletonList(issue));
        StructureParseIntentProfileVO intentProfile = new StructureParseIntentProfileVO();
        intentProfile.setScanMode("PARTITION_RANGE_SCAN");
        intentProfile.setJoinType("STAR");
        intentProfile.setComputeDensity("HEAVY");
        intentProfile.setResourceType("NETWORK_MIXED");
        intentProfile.setSlaLevel("REPORT_LT_30S");
        intentProfile.setConfidence("HIGH");
        intentProfile.setClassificationLabels(Collections.singletonList("REPORT_LT_30S"));
        response.setIntentProfile(intentProfile);
        StructureParseFeatureSummaryVO featureSummary = new StructureParseFeatureSummaryVO();
        featureSummary.setParserEngine("TRINO");
        featureSummary.setTableCount(Integer.valueOf(3));
        featureSummary.setJoinCount(Integer.valueOf(2));
        response.setFeatureSummary(featureSummary);
        StructureParseResourceEstimateVO resourceEstimate = new StructureParseResourceEstimateVO();
        resourceEstimate.setOverall("HIGH");
        resourceEstimate.setCpu("MEDIUM");
        resourceEstimate.setIo("HIGH");
        response.setEstimatedResourceCost(resourceEstimate);
        StructureParseRiskVO risk = new StructureParseRiskVO();
        risk.setRiskCode("FULL_TABLE_SCAN_RISK");
        risk.setSeverity("HIGH");
        response.setRiskChecklist(Collections.singletonList(risk));
        response.setPriorityScore(Integer.valueOf(80));
        response.setPriorityLevel(StructureParsePriorityLevel.P2.name());
        response.setImportant(Boolean.TRUE);
        response.setUrgent(Boolean.FALSE);

        assertEquals("STRUCTURE", response.getParseType());
        assertEquals("parse-001", response.getParseTaskId());
        assertEquals("COMPLEX", response.getComplexityLevel());
        assertEquals("fp-001", response.getSqlFingerprint());
        assertEquals("PARTITION_RANGE_SCAN", response.getIntentProfile().getScanMode());
        assertEquals("TRINO", response.getFeatureSummary().getParserEngine());
        assertEquals("HIGH", response.getEstimatedResourceCost().getOverall());
        assertEquals("FULL_TABLE_SCAN_RISK", response.getRiskChecklist().get(0).getRiskCode());
        assertEquals("NO_PARTITION_FILTER", response.getIssues().get(0).getIssueCode());
        assertEquals("Add a partition predicate on the partition key.", response.getIssues().get(0).getSuggestedAction());
        assertEquals("RESOLVED", response.getQueryDateSummary().getQueryDateStatus());
        assertEquals("BUSINESS_VIEW", response.getLogicalObjectHits().get(0).getObjectType());
        assertEquals("BUSINESS_VIEW:sales_daily_view", response.getLogicalObjectHits().get(0).getObjectKey());
        assertEquals("P2", response.getPriorityLevel());
    }
}
