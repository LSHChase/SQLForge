package com.company.sqloptimization.infrastructure.persistence.mapper;

import com.company.sqloptimization.infrastructure.persistence.entity.SqlRewriteRecordRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SqlRewriteRecordMapper {

    SqlRewriteRecordRecord selectByRewriteRecordId(@Param("rewriteRecordId") String rewriteRecordId);

    List<SqlRewriteRecordRecord> selectByTenantId(@Param("tenantId") String tenantId);

    List<SqlRewriteRecordRecord> selectByTenantIdAndHistoryId(@Param("tenantId") String tenantId,
                                                              @Param("historyId") String historyId);

    List<SqlRewriteRecordRecord> selectScheduledValidationCandidates(@Param("dueBefore") LocalDateTime dueBefore,
                                                                     @Param("limit") int limit);

    int insert(SqlRewriteRecordRecord record);

    int update(SqlRewriteRecordRecord record);
}
