package com.company.sqloptimization.infrastructure.persistence.mapper;

import com.company.sqloptimization.domain.parsehistory.SqlParseHistoryFilter;
import com.company.sqloptimization.infrastructure.persistence.entity.SqlParseHistoryRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SqlParseHistoryMapper {

    SqlParseHistoryRecord selectByParseHistoryId(String parseHistoryId);

    SqlParseHistoryRecord selectByBatchKeyAndSqlFingerprint(@Param("batchKey") String batchKey,
                                                            @Param("sqlFingerprint") String sqlFingerprint);

    List<SqlParseHistoryRecord> selectPage(SqlParseHistoryFilter filter);

    int count(SqlParseHistoryFilter filter);

    int insert(SqlParseHistoryRecord record);

    int update(SqlParseHistoryRecord record);
}
