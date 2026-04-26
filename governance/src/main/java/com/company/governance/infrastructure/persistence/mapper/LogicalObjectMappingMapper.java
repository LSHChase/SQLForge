package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.logicalview.entity.LogicalObjectMapping;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface LogicalObjectMappingMapper {

    List<LogicalObjectMapping> selectByTenantAndLogicalViewId(@Param("tenantId") String tenantId,
                                                              @Param("logicalViewId") String logicalViewId);
}
