package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.governance.domain.trace.entity.TraceLookupHitRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AuditLogMapper {

    AuditLogRecord selectById(Long id);

    List<AuditLogRecord> selectRecentByTenant(@Param("tenantId") String tenantId, @Param("limit") int limit);

    List<AuditLogRecord> selectRecentBusinessByTenant(@Param("tenantId") String tenantId, @Param("limit") int limit);

    List<AuditLogRecord> selectByTraceId(@Param("tenantId") String tenantId,
                                         @Param("traceId") String traceId,
                                         @Param("limit") int limit);

    List<AuditLogRecord> selectByTraceIds(@Param("tenantId") String tenantId,
                                          @Param("traceIds") List<String> traceIds);

    List<TraceLookupHitRecord> selectTraceHitsByTargetId(@Param("tenantId") String tenantId,
                                                         @Param("targetId") String targetId,
                                                         @Param("targetTypes") List<String> targetTypes,
                                                         @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
                                                         @Param("cursorAuditId") Long cursorAuditId,
                                                         @Param("limit") int limit);

    int insert(AuditLogRecord auditLogRecord);
}
