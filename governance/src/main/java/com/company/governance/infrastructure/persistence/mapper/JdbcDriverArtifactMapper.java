package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.infrastructure.persistence.entity.JdbcDriverArtifactRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface JdbcDriverArtifactMapper {

    int insert(JdbcDriverArtifactRecord record);

    JdbcDriverArtifactRecord selectByTenantIdAndArtifactId(@Param("tenantId") String tenantId,
                                                           @Param("artifactId") String artifactId);

    List<JdbcDriverArtifactRecord> selectByTenantId(@Param("tenantId") String tenantId);
}
