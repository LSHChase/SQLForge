package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.dbview.entity.DatabaseViewRef;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DatabaseViewMapper {

    DatabaseViewRef selectByTenantDatasourceAndViewName(@Param("tenantId") String tenantId,
                                                        @Param("datasourceCode") String datasourceCode,
                                                        @Param("viewName") String viewName);

    List<DatabaseViewRef> selectByTenantAndDatasource(@Param("tenantId") String tenantId,
                                                      @Param("datasourceCode") String datasourceCode);
}
