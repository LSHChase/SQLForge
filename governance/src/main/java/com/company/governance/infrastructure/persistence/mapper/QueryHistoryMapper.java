package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.domain.trace.entity.GovernanceQueryHistoryProjection;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;

public interface QueryHistoryMapper {

    QueryHistoryRecord selectById(String historyId);

    List<QueryHistoryRecord> selectRecentByTenant(@Param("tenantId") String tenantId, @Param("limit") int limit);

    List<QueryHistoryRecord> selectByTraceId(@Param("tenantId") String tenantId,
                                             @Param("traceId") String traceId,
                                             @Param("limit") int limit);

    List<QueryHistoryRecord> selectByTraceIds(@Param("tenantId") String tenantId,
                                              @Param("traceIds") List<String> traceIds);

    List<GovernanceQueryHistoryProjection> selectHistoryPage(@Param("tenantId") String tenantId,
                                                             @Param("reportCode") String reportCode,
                                                             @Param("datasourceCode") String datasourceCode,
                                                             @Param("stageCode") String stageCode,
                                                             @Param("bizDate") LocalDate bizDate,
                                                             @Param("queryDateStart") LocalDate queryDateStart,
                                                             @Param("queryDateEnd") LocalDate queryDateEnd,
                                                             @Param("status") String status,
                                                             @Param("cacheHit") Boolean cacheHit,
                                                             @Param("rewriteApplied") Boolean rewriteApplied,
                                                             @Param("accelerationApplied") Boolean accelerationApplied,
                                                             @Param("parameterizedSql") Boolean parameterizedSql,
                                                             @Param("logicalObjectType") String logicalObjectType,
                                                             @Param("accessChannel") String accessChannel,
                                                             @Param("engine") String engine,
                                                             @Param("submittedBy") String submittedBy,
                                                             @Param("submittedStart") LocalDateTime submittedStart,
                                                             @Param("submittedEnd") LocalDateTime submittedEnd,
                                                             @Param("orderByClause") String orderByClause,
                                                             @Param("offset") int offset,
                                                             @Param("limit") int limit);

    GovernanceQueryHistoryProjection selectHistoryDetail(@Param("tenantId") String tenantId,
                                                         @Param("historyId") String historyId);

    int insert(QueryHistoryRecord queryHistoryRecord);

    int updateById(QueryHistoryRecord queryHistoryRecord);
}
