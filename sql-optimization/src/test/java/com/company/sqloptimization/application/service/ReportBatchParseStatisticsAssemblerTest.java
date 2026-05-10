package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqloptimization.application.controller.vo.ParseIssueSceneStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParsePriorityMatrixCellVO;
import com.company.sqloptimization.application.controller.vo.ParseReportStatisticVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchImportanceStatisticVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchIssueSceneDetailVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchLogicalObjectStatisticVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchParseStatisticsVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchSqlStatisticVO;
import com.company.sqloptimization.domain.reportbatch.ReportBatchItem;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportBatchParseStatisticsAssemblerTest {

    private final ReportBatchParseStatisticsAssembler assembler = new ReportBatchParseStatisticsAssembler();

    @Test
    void shouldBuildRequiredBatchAndHistoryStatisticsDimensions() {
        Instant now = Instant.parse("2026-05-05T12:00:00Z");
        List<ReportBatchItem> items = Arrays.asList(
            item(
                "item-a",
                "RPT_A",
                "SELECT * FROM orders",
                Arrays.asList("MISSING_FILTER", "WIDE_PROJECTION"),
                Arrays.asList("TABLE:orders"),
                now
            ),
            item(
                "item-b",
                "RPT_B",
                "SELECT customer_id FROM customers WHERE dt = '2026-05-05'",
                Collections.singletonList("GENERAL_WARNING"),
                Arrays.asList("TABLE:customers"),
                now
            ),
            item(
                "item-c",
                "RPT_A",
                "SELECT id FROM orders WHERE dt = '2026-05-05'",
                Collections.<String>emptyList(),
                Arrays.asList("TABLE:orders"),
                now
            )
        );

        ReportBatchParseStatisticsVO statistics = assembler.build(items);

        assertEquals(Integer.valueOf(3), statistics.getOverview().getTotalSqlCount());
        assertEquals(Integer.valueOf(3), statistics.getOverview().getIssueSqlCount());
        assertEquals(Integer.valueOf(5), statistics.getOverview().getTotalIssueCount());
        assertEquals(Integer.valueOf(4), statistics.getOverview().getIssueSceneCount());
        assertNotNull(findIssueScene(statistics.getIssueSceneStatistics(), "MISSING_FILTER"));
        assertNotNull(findIssueScene(statistics.getIssueSceneStatistics(), "REPORT_SQL_MERGE_CANDIDATE"));
        assertEquals(Integer.valueOf(1), statistics.getSeverityDistribution().get("HIGH"));
        assertEquals(Integer.valueOf(3), statistics.getSeverityDistribution().get("MEDIUM"));
        assertEquals("RPT_A", statistics.getReportStatistics().get(0).getReportCode());
        assertEquals(Integer.valueOf(2), statistics.getReportStatistics().get(0).getSqlCount());
        assertEquals(Boolean.TRUE, statistics.getReportStatistics().get(0).getMergeCandidate());
        assertEquals(Integer.valueOf(2), statistics.getReportStatistics().get(0).getMergeCandidateSqlCount());
        assertTrue(statistics.getReportStatistics().get(0).getMergeCandidateReason().contains("TABLE:orders"));
        assertTrue(statistics.getReportStatistics().get(0).getIssueScenes().contains("REPORT_SQL_MERGE_CANDIDATE"));
        assertEquals(Integer.valueOf(1), statistics.getMergeCandidateReportCount());
        assertEquals("item-a", statistics.getSqlStatistics().get(0).getItemId());
        assertEquals("P1", statistics.getSqlStatistics().get(0).getHighestPriorityLevel());
        assertNotNull(findImportance(statistics.getImportanceStatistics(), "IMPORTANT_URGENT"));
        assertNotNull(findPriority(statistics.getPriorityMatrix(), "P1", "IMPORTANT_URGENT"));
        ReportBatchLogicalObjectStatisticVO orders = findLogicalObject(
            statistics.getLogicalObjectStatistics(),
            "TABLE:orders"
        );
        assertNotNull(orders);
        assertEquals(Integer.valueOf(2), orders.getSqlCount());
        assertTrue(orders.getReportCodes().contains("RPT_A"));
    }

    @Test
    void shouldReturnEmptyStatisticsForEmptyBatch() {
        ReportBatchParseStatisticsVO statistics = assembler.build(Collections.<ReportBatchItem>emptyList());

        assertEquals(Integer.valueOf(0), statistics.getOverview().getTotalSqlCount());
        assertEquals(Integer.valueOf(0), statistics.getMergeCandidateReportCount());
        assertTrue(statistics.getIssueSceneStatistics().isEmpty());
        assertTrue(statistics.getReportStatistics().isEmpty());
        assertTrue(statistics.getSqlStatistics().isEmpty());
        assertTrue(statistics.getPriorityMatrix().isEmpty());
        assertTrue(statistics.getLogicalObjectStatistics().isEmpty());
    }

    @Test
    void shouldDetectMergeCandidatesInIssueSceneDetail() {
        Instant now = Instant.parse("2026-05-05T12:00:00Z");
        List<ReportBatchItem> items = Arrays.asList(
            item("item-a", "RPT_A", "SELECT id FROM orders WHERE dt = '2026-05-05'",
                Collections.<String>emptyList(), Arrays.asList("TABLE:orders"), now),
            item("item-b", "RPT_A", "SELECT amount FROM orders WHERE dt = '2026-05-05'",
                Collections.<String>emptyList(), Arrays.asList("TABLE:orders"), now),
            item("item-c", "RPT_A", "SELECT id FROM customers WHERE dt = '2026-05-05'",
                Collections.<String>emptyList(), Arrays.asList("TABLE:customers"), now)
        );

        ReportBatchIssueSceneDetailVO detail = assembler.buildIssueSceneDetail(
            items,
            "REPORT_SQL_MERGE_CANDIDATE",
            null,
            null,
            null,
            null
        );

        assertEquals("REPORT_SQL_MERGE_CANDIDATE", detail.getIssueScene());
        assertEquals(Integer.valueOf(2), detail.getAffectedSqlCount());
        assertEquals(Integer.valueOf(2), detail.getAffectedIssueCount());
        assertEquals(Integer.valueOf(1), detail.getReportCount());
        assertEquals("RPT_A", detail.getReportDetails().get(0).getReportCode());
        assertEquals(Integer.valueOf(2), detail.getSqlStatistics().size());
        assertTrue(detail.getSqlStatistics().get(0).getIssueScenes().contains("REPORT_SQL_MERGE_CANDIDATE"));
    }

    @Test
    void shouldScopeIssueSceneDetailSqlRowsToCurrentScene() {
        Instant now = Instant.parse("2026-05-05T12:00:00Z");
        List<ReportBatchItem> items = Collections.singletonList(
            item(
                "item-a",
                "RPT_A",
                "SELECT * FROM orders",
                Arrays.asList("MISSING_FILTER", "WIDE_PROJECTION"),
                Arrays.asList("TABLE:orders"),
                now
            )
        );

        ReportBatchIssueSceneDetailVO detail = assembler.buildIssueSceneDetail(
            items,
            "MISSING_FILTER",
            null,
            null,
            null,
            null
        );

        assertEquals(Integer.valueOf(1), detail.getAffectedSqlCount());
        assertEquals(Integer.valueOf(1), detail.getAffectedIssueCount());
        ReportBatchSqlStatisticVO row = detail.getSqlStatistics().get(0);
        assertEquals(Integer.valueOf(1), row.getIssueCount());
        assertEquals(Collections.singletonList("MISSING_FILTER"), row.getIssueScenes());
        assertTrue(row.getIssueLocations().stream()
            .allMatch(location -> "MISSING_FILTER".equals(location.getIssueScene())));
    }

    @Test
    void shouldNotMarkSingleOrCrossReportSqlAsMergeCandidate() {
        Instant now = Instant.parse("2026-05-05T12:00:00Z");
        List<ReportBatchItem> items = Arrays.asList(
            item("item-a", "RPT_A", "SELECT id FROM orders WHERE dt = '2026-05-05'",
                Collections.<String>emptyList(), Arrays.asList("TABLE:orders"), now),
            item("item-b", "RPT_B", "SELECT amount FROM orders WHERE dt = '2026-05-05'",
                Collections.<String>emptyList(), Arrays.asList("TABLE:orders"), now)
        );

        ReportBatchParseStatisticsVO statistics = assembler.build(items);

        assertEquals(Integer.valueOf(0), statistics.getMergeCandidateReportCount());
        assertFalse(Boolean.TRUE.equals(findReport(statistics.getReportStatistics(), "RPT_A").getMergeCandidate()));
        assertFalse(Boolean.TRUE.equals(findReport(statistics.getReportStatistics(), "RPT_B").getMergeCandidate()));
        assertFalse(statistics.getIssueSceneStatistics().stream()
            .anyMatch(item -> "REPORT_SQL_MERGE_CANDIDATE".equals(item.getIssueScene())));
    }

    @Test
    void shouldIgnoreInvalidSqlWhenAssessingMergeCandidates() {
        Instant now = Instant.parse("2026-05-05T12:00:00Z");
        List<ReportBatchItem> items = Arrays.asList(
            item("item-a", "RPT_A", "SELECT id FROM orders WHERE dt = '2026-05-05'",
                Collections.<String>emptyList(), Arrays.asList("TABLE:orders"), now),
            item("item-b", "RPT_A", "SELECT FROM orders",
                Collections.singletonList("SQL_SYNTAX_INVALID"), Arrays.asList("TABLE:orders"), now,
                "INVALID", ReportBatchItem.Status.FAILED)
        );

        ReportBatchParseStatisticsVO statistics = assembler.build(items);

        assertEquals(Integer.valueOf(0), statistics.getMergeCandidateReportCount());
        assertFalse(Boolean.TRUE.equals(findReport(statistics.getReportStatistics(), "RPT_A").getMergeCandidate()));
        assertFalse(statistics.getSqlStatistics().get(0).getIssueScenes().contains("REPORT_SQL_MERGE_CANDIDATE"));
    }

    @Test
    void shouldCapLargeSqlStatisticListWhileKeepingFullOverview() {
        Instant now = Instant.parse("2026-05-05T12:00:00Z");
        List<ReportBatchItem> items = new java.util.ArrayList<ReportBatchItem>();
        for (int index = 1; index <= 650; index++) {
            items.add(item(
                "item-" + index,
                "RPT_BIG",
                "SELECT " + index + " AS metric_value",
                Collections.singletonList("MISSING_FILTER"),
                Collections.singletonList("TABLE:orders"),
                now
            ));
        }

        ReportBatchParseStatisticsVO statistics = assembler.build(items);

        assertEquals(Integer.valueOf(650), statistics.getOverview().getTotalSqlCount());
        assertEquals(Integer.valueOf(500), Integer.valueOf(statistics.getSqlStatistics().size()));
        assertEquals(Integer.valueOf(500), statistics.getSqlStatisticLimit());
        assertEquals(Boolean.TRUE, statistics.getSqlStatisticTruncated());
        assertEquals(Integer.valueOf(150), statistics.getOmittedSqlStatisticCount());
    }

    private ReportBatchItem item(String itemId,
                                 String reportCode,
                                 String sqlText,
                                 List<String> issueScenes,
                                 List<String> logicalObjectKeys,
                                 Instant now) {
        return item(
            itemId,
            reportCode,
            sqlText,
            issueScenes,
            logicalObjectKeys,
            now,
            "VALID",
            ReportBatchItem.Status.RESOLVED
        );
    }

    private ReportBatchItem item(String itemId,
                                 String reportCode,
                                 String sqlText,
                                 List<String> issueScenes,
                                 List<String> logicalObjectKeys,
                                 Instant now,
                                 String structureSyntaxStatus,
                                 ReportBatchItem.Status status) {
        ReportBatchItem item = ReportBatchItem.create(
            itemId,
            "batch-stat-001",
            1,
            reportCode,
            reportCode + " name",
            "hetu_main",
            "PROD",
            "high",
            reportCode + "," + sqlText,
            "sql_1",
            Integer.valueOf(1),
            now
        );
        item.complete(
            sqlText,
            "parse-" + itemId,
            structureSyntaxStatus,
            "AVAILABLE",
            "CONNECTED",
            status,
            null,
            issueScenes,
            logicalObjectKeys,
            now
        );
        return item;
    }

    private ReportBatchImportanceStatisticVO findImportance(List<ReportBatchImportanceStatisticVO> statistics,
                                                            String bucket) {
        for (ReportBatchImportanceStatisticVO statistic : statistics) {
            if (bucket.equals(statistic.getImportanceBucket())) {
                return statistic;
            }
        }
        return null;
    }

    private ParsePriorityMatrixCellVO findPriority(List<ParsePriorityMatrixCellVO> statistics,
                                                   String priorityLevel,
                                                   String bucket) {
        for (ParsePriorityMatrixCellVO statistic : statistics) {
            if (priorityLevel.equals(statistic.getPriorityLevel()) && bucket.equals(statistic.getUrgencyBucket())) {
                return statistic;
            }
        }
        return null;
    }

    private ParseIssueSceneStatisticVO findIssueScene(List<ParseIssueSceneStatisticVO> statistics,
                                                      String issueScene) {
        for (ParseIssueSceneStatisticVO statistic : statistics) {
            if (issueScene.equals(statistic.getIssueScene())) {
                return statistic;
            }
        }
        return null;
    }

    private ReportBatchLogicalObjectStatisticVO findLogicalObject(
        List<ReportBatchLogicalObjectStatisticVO> statistics,
        String objectKey
    ) {
        for (ReportBatchLogicalObjectStatisticVO statistic : statistics) {
            if (objectKey.equals(statistic.getObjectKey())) {
                return statistic;
            }
        }
        return null;
    }

    private ParseReportStatisticVO findReport(List<ParseReportStatisticVO> statistics, String reportCode) {
        for (ParseReportStatisticVO statistic : statistics) {
            if (reportCode.equals(statistic.getReportCode())) {
                return statistic;
            }
        }
        return null;
    }
}
