package com.company.benchmarkengine.infrastructure.sqloptimization;

import java.util.List;

public interface SqlOptimizationParseResultClient {

    SqlOptimizationCombinedParseStatus getCombinedParseStatus(String parseTaskId);

    SqlOptimizationParseBatchStatus getParseBatch(String batchId);

    List<SqlOptimizationParseSqlIssueStatistic> getImportantUrgentSqls();
}
