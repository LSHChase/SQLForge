package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.trace.entity.AuditLogRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AuditLogMapper {

    AuditLogRecord selectById(Long id);

    List<AuditLogRecord> selectRecentByTenant(@Param("tenantId") String tenantId, @Param("limit") int limit);

    List<AuditLogRecord> selectRecentBusinessByTenant(@Param("tenantId") String tenantId, @Param("limit") int limit);

    List<AuditLogRecord> selectByTraceId(@Param("tenantId") String tenantId,
                                         @Param("traceId") String traceId,
                                         @Param("limit") int limit);

    int insert(AuditLogRecord auditLogRecord);
}
