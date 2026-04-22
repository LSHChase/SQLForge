package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface QueryHistoryMapper {

    QueryHistoryRecord selectById(String historyId);

    List<QueryHistoryRecord> selectRecentByTenant(@Param("tenantId") String tenantId, @Param("limit") int limit);

    List<QueryHistoryRecord> selectByTraceId(@Param("tenantId") String tenantId,
                                             @Param("traceId") String traceId,
                                             @Param("limit") int limit);

    List<QueryHistoryRecord> selectByTraceIds(@Param("tenantId") String tenantId,
                                              @Param("traceIds") List<String> traceIds);

    int insert(QueryHistoryRecord queryHistoryRecord);
}
