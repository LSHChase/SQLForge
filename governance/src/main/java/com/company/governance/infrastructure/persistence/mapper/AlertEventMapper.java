package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.infrastructure.persistence.entity.AlertEventRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AlertEventMapper {

    int insert(AlertEventRecord record);

    int update(AlertEventRecord record);

    AlertEventRecord selectByAlertId(@Param("alertId") String alertId);

    List<AlertEventRecord> selectOpenByTenantId(@Param("tenantId") String tenantId);

    List<AlertEventRecord> selectByTenantIdAndDedupeKey(@Param("tenantId") String tenantId,
                                                        @Param("dedupeKey") String dedupeKey);
}
