package com.company.sqloptimization.infrastructure.repository;

import com.company.sqloptimization.application.controller.vo.ParseBatchStageStatisticsVO;
import com.company.sqloptimization.application.controller.vo.ParseStatisticsOverviewVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchIssueSceneDetailVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchParseStatisticsVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchSqlStatisticVO;
import com.company.sqloptimization.application.service.ReportBatchStatisticsViewBuilder;
import com.company.sqloptimization.domain.reportbatch.ReportBatch;
import com.company.sqloptimization.domain.reportbatch.ReportBatchStatisticsSummary;
import com.company.sqloptimization.domain.reportbatch.repository.ReportBatchStatisticsRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "sql-optimization.report-batch", name = "repository", havingValue = "test")
public class InMemoryReportBatchStatisticsRepository implements ReportBatchStatisticsRepository {

    private final Map<String, Snapshot> snapshots = new ConcurrentHashMap<String, Snapshot>();

    @Override
    public void replaceStatistics(ReportBatch batch,
                                  ReportBatchParseStatisticsVO statistics,
                                  ParseBatchStageStatisticsVO planAnalysisStatistics,
                                  Instant occurredAt) {
        if (batch == null) {
            return;
        }
        snapshots.put(batch.getBatchId(), new Snapshot(
            toSummary(batch, statistics, planAnalysisStatistics, occurredAt),
            statistics
        ));
    }

    @Override
    public ReportBatchStatisticsSummary findSummaryByBatchId(String batchId) {
        Snapshot snapshot = snapshots.get(batchId);
        return snapshot == null ? null : snapshot.summary;
    }

    @Override
    public Map<String, ReportBatchStatisticsSummary> findSummariesByBatchIds(List<String> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ReportBatchStatisticsSummary> result = new LinkedHashMap<String, ReportBatchStatisticsSummary>();
        for (String batchId : batchIds) {
            Snapshot snapshot = snapshots.get(batchId);
            if (snapshot != null) {
                result.put(batchId, snapshot.summary);
            }
        }
        return result;
    }

    @Override
    public ReportBatchParseStatisticsVO findParseStatistics(String batchId,
                                                           Integer pageNumber,
                                                           Integer pageSize,
                                                           String reportCode) {
        Snapshot snapshot = snapshots.get(batchId);
        return snapshot == null
            ? null
            : ReportBatchStatisticsViewBuilder.pageStatistics(snapshot.statistics, pageNumber, pageSize, reportCode);
    }

    @Override
    public ReportBatchIssueSceneDetailVO findIssueSceneDetail(String batchId,
                                                             String issueScene,
                                                             Integer pageNumber,
                                                             Integer pageSize,
                                                             Integer reportDetailPageNumber,
                                                             Integer reportDetailPageSize,
                                                             Integer logicalObjectDetailPageNumber,
                                                             Integer logicalObjectDetailPageSize,
                                                             String reportCode,
                                                             String logicalObjectKey) {
        Snapshot snapshot = snapshots.get(batchId);
        return snapshot == null
            ? null
            : ReportBatchStatisticsViewBuilder.issueSceneDetail(
                snapshot.statistics,
                issueScene,
                pageNumber,
                pageSize,
                reportDetailPageNumber,
                reportDetailPageSize,
                logicalObjectDetailPageNumber,
                logicalObjectDetailPageSize,
                reportCode,
                logicalObjectKey
            );
    }

    private ReportBatchStatisticsSummary toSummary(ReportBatch batch,
                                                   ReportBatchParseStatisticsVO statistics,
                                                   ParseBatchStageStatisticsVO planAnalysisStatistics,
                                                   Instant occurredAt) {
        int resolved = 0;
        int partial = 0;
        int failed = 0;
        if (statistics != null && statistics.getSqlStatistics() != null) {
            for (ReportBatchSqlStatisticVO statistic : statistics.getSqlStatistics()) {
                if ("RESOLVED".equals(statistic.getStatus())) {
                    resolved++;
                } else if ("PARTIAL_RESOLVED".equals(statistic.getStatus())) {
                    partial++;
                } else if ("FAILED".equals(statistic.getStatus())) {
                    failed++;
                }
            }
        }
        ParseStatisticsOverviewVO overview = statistics == null ? null : statistics.getOverview();
        return new ReportBatchStatisticsSummary(
            batch.getBatchId(),
            batch.getTenantId(),
            intValue(overview == null ? null : overview.getTotalSqlCount()),
            resolved,
            partial,
            failed,
            intValue(overview == null ? null : overview.getIssueSqlCount()),
            intValue(overview == null ? null : overview.getTotalIssueCount()),
            intValue(overview == null ? null : overview.getIssueSceneCount()),
            intValue(overview == null ? null : overview.getImportantSqlCount()),
            intValue(overview == null ? null : overview.getUrgentSqlCount()),
            intValue(statistics == null ? null : statistics.getMergeCandidateReportCount()),
            intValue(planAnalysisStatistics == null ? null : planAnalysisStatistics.getSuccessRecords()),
            intValue(planAnalysisStatistics == null ? null : planAnalysisStatistics.getPartialSuccessRecords()),
            intValue(planAnalysisStatistics == null ? null : planAnalysisStatistics.getFailedRecords()),
            occurredAt,
            occurredAt
        );
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value.intValue();
    }

    private static final class Snapshot {
        private final ReportBatchStatisticsSummary summary;
        private final ReportBatchParseStatisticsVO statistics;

        private Snapshot(ReportBatchStatisticsSummary summary, ReportBatchParseStatisticsVO statistics) {
            this.summary = summary;
            this.statistics = statistics;
        }
    }
}
