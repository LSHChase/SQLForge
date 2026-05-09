package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.infrastructure.persistence.entity.DatasourceConfigRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DatasourceConfigMapper {

    int upsert(DatasourceConfigRecord record);

    DatasourceConfigRecord selectByTenantIdAndDatasourceId(@Param("tenantId") String tenantId,
                                                           @Param("datasourceId") String datasourceId);

    DatasourceConfigRecord selectByTenantIdAndDatasourceCodeAndEngineType(@Param("tenantId") String tenantId,
                                                                           @Param("datasourceCode") String datasourceCode,
                                                                           @Param("engineType") String engineType);

    List<DatasourceConfigRecord> selectByTenantId(@Param("tenantId") String tenantId);
}
