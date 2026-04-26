package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.logicalview.entity.BusinessLogicalView;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface BusinessLogicalViewMapper {

    List<BusinessLogicalView> selectByTenantAndDatasource(@Param("tenantId") String tenantId,
                                                          @Param("datasourceCode") String datasourceCode);

    BusinessLogicalView selectByTenantAndViewCode(@Param("tenantId") String tenantId,
                                                  @Param("viewCode") String viewCode);
}
