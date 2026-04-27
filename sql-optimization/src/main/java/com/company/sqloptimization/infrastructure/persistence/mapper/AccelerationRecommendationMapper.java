package com.company.sqloptimization.infrastructure.persistence.mapper;

import com.company.sqloptimization.infrastructure.persistence.entity.AccelerationRecommendationRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccelerationRecommendationMapper {

    AccelerationRecommendationRecord selectByRecommendationId(@Param("recommendationId") String recommendationId);

    List<AccelerationRecommendationRecord> selectByTenantId(@Param("tenantId") String tenantId);

    int insert(AccelerationRecommendationRecord record);

    int update(AccelerationRecommendationRecord record);
}
