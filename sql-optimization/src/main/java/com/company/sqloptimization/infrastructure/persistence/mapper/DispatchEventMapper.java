package com.company.sqloptimization.infrastructure.persistence.mapper;

import com.company.sqloptimization.infrastructure.persistence.entity.DispatchEventRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DispatchEventMapper {

    DispatchEventRecord selectByDispatchEventId(@Param("dispatchEventId") String dispatchEventId);

    List<DispatchEventRecord> selectByTenantId(@Param("tenantId") String tenantId);

    List<DispatchEventRecord> selectByTenantIdAndStatus(@Param("tenantId") String tenantId,
                                                        @Param("status") String status);

    List<DispatchEventRecord> selectByTenantIdAndRecommendationId(@Param("tenantId") String tenantId,
                                                                  @Param("recommendationId") String recommendationId);

    int insert(DispatchEventRecord record);

    int update(DispatchEventRecord record);
}
