package com.company.sqloptimization.domain.parsehistory.repository;

import com.company.sqloptimization.domain.parsehistory.SqlParseHistory;
import com.company.sqloptimization.domain.parsehistory.SqlParseHistoryFilter;
import java.util.List;

public interface SqlParseHistoryRepository {

    SqlParseHistory save(SqlParseHistory history);

    SqlParseHistory findByParseHistoryId(String parseHistoryId);

    SqlParseHistory findByBatchKeyAndSqlFingerprint(String batchKey, String sqlFingerprint);

    List<SqlParseHistory> findPage(SqlParseHistoryFilter filter);

    int count(SqlParseHistoryFilter filter);
}
