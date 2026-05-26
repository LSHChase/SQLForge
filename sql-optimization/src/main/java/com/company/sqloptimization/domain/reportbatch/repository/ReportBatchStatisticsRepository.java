package com.company.sqloptimization.domain.reportbatch.repository;

import com.company.sqloptimization.application.controller.vo.ParseBatchStageStatisticsVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchIssueSceneDetailVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchParseStatisticsVO;
import com.company.sqloptimization.domain.reportbatch.ReportBatch;
import com.company.sqloptimization.domain.reportbatch.ReportBatchStatisticsSummary;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface ReportBatchStatisticsRepository {

    void replaceStatistics(ReportBatch batch,
                           ReportBatchParseStatisticsVO statistics,
                           ParseBatchStageStatisticsVO planAnalysisStatistics,
                           Instant occurredAt);

    ReportBatchStatisticsSummary findSummaryByBatchId(String batchId);

    Map<String, ReportBatchStatisticsSummary> findSummariesByBatchIds(List<String> batchIds);

    ReportBatchParseStatisticsVO findParseStatistics(String batchId,
                                                     Integer pageNumber,
                                                     Integer pageSize,
                                                     String reportCode);

    ReportBatchIssueSceneDetailVO findIssueSceneDetail(String batchId,
                                                       String issueScene,
                                                       Integer pageNumber,
                                                       Integer pageSize,
                                                       Integer reportDetailPageNumber,
                                                       Integer reportDetailPageSize,
                                                       Integer logicalObjectDetailPageNumber,
                                                       Integer logicalObjectDetailPageSize,
                                                       String reportCode,
                                                       String logicalObjectKey);
}
