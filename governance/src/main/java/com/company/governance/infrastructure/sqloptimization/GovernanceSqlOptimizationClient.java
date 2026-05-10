package com.company.governance.infrastructure.sqloptimization;

import java.util.List;

public interface GovernanceSqlOptimizationClient {

    List<SqlOptimizationRewriteRecordResponse> listRewriteRecordsByHistoryId(String historyId);
}
