package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.dbview.entity.DatabaseViewDependency;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DatabaseViewDependencyMapper {

    List<DatabaseViewDependency> selectByTenantAndDbViewId(@Param("tenantId") String tenantId,
                                                           @Param("dbViewId") String dbViewId);
}
