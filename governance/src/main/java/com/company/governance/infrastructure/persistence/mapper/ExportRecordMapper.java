package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.trace.entity.ExportRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExportRecordMapper {

    ExportRecord selectById(String exportId);

    List<ExportRecord> selectRecentByTenant(@Param("tenantId") String tenantId, @Param("limit") int limit);

    List<ExportRecord> selectByTraceId(@Param("tenantId") String tenantId,
                                       @Param("traceId") String traceId,
                                       @Param("limit") int limit);

    int insert(ExportRecord exportRecord);
}
