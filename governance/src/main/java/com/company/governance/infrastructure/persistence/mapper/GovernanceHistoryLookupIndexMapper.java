package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.trace.entity.TraceLookupHitRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface GovernanceHistoryLookupIndexMapper {

    List<TraceLookupHitRecord> selectTraceHitsByLookupId(@Param("tenantId") String tenantId,
                                                         @Param("lookupType") String lookupType,
                                                         @Param("lookupId") String lookupId,
                                                         @Param("windowStart") LocalDateTime windowStart,
                                                         @Param("windowEnd") LocalDateTime windowEnd,
                                                         @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
                                                         @Param("cursorAuditId") Long cursorAuditId,
                                                         @Param("limit") int limit);
}
