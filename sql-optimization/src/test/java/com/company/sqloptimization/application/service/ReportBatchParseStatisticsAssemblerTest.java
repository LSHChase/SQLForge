package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqloptimization.application.controller.vo.ParsePriorityMatrixCellVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchImportanceStatisticVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchLogicalObjectStatisticVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchParseStatisticsVO;
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
        assertEquals(Integer.valueOf(2), statistics.getOverview().getIssueSqlCount());
        assertEquals(Integer.valueOf(3), statistics.getOverview().getTotalIssueCount());
        assertEquals(Integer.valueOf(3), statistics.getOverview().getIssueSceneCount());
        assertEquals("MISSING_FILTER", statistics.getIssueSceneStatistics().get(0).getIssueScene());
        assertEquals(Integer.valueOf(1), statistics.getSeverityDistribution().get("HIGH"));
        assertEquals("RPT_A", statistics.getReportStatistics().get(0).getReportCode());
        assertEquals(Integer.valueOf(2), statistics.getReportStatistics().get(0).getSqlCount());
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
        assertTrue(statistics.getIssueSceneStatistics().isEmpty());
        assertTrue(statistics.getReportStatistics().isEmpty());
        assertTrue(statistics.getSqlStatistics().isEmpty());
        assertTrue(statistics.getPriorityMatrix().isEmpty());
        assertTrue(statistics.getLogicalObjectStatistics().isEmpty());
    }

    private ReportBatchItem item(String itemId,
                                 String reportCode,
                                 String sqlText,
                                 List<String> issueScenes,
                                 List<String> logicalObjectKeys,
                                 Instant now) {
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
            "VALID",
            "AVAILABLE",
            "CONNECTED",
            ReportBatchItem.Status.RESOLVED,
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
}
