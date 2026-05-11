package com.company.governance.infrastructure.sqloptimization;

import java.util.List;

public interface GovernanceSqlOptimizationClient {

    List<SqlOptimizationRewriteRecordResponse> listRewriteRecordsByHistoryId(String historyId);

    List<SqlOptimizationRewriteRecordResponse> listRewriteRecords(String historyId,
                                                                   String recommendationId,
                                                                   String validationStatus,
                                                                   String sourceType);
}
