package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.infrastructure.persistence.entity.ReportInterfaceConfigRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ReportInterfaceConfigMapper {

    int upsert(ReportInterfaceConfigRecord record);

    ReportInterfaceConfigRecord selectByTenantIdAndConfigId(@Param("tenantId") String tenantId,
                                                            @Param("configId") String configId);

    List<ReportInterfaceConfigRecord> selectByTenantId(@Param("tenantId") String tenantId);

    List<ReportInterfaceConfigRecord> selectMatchCandidates(@Param("tenantId") String tenantId,
                                                            @Param("datasourceCode") String datasourceCode,
                                                            @Param("stage") String stage);
}
