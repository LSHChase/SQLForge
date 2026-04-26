package com.company.sqloptimization.infrastructure.persistence.mapper;

import com.company.sqloptimization.infrastructure.persistence.entity.AccelerationPlanRecord;

public interface AccelerationPlanMapper {

    AccelerationPlanRecord selectByPlanId(String planId);

    int insert(AccelerationPlanRecord accelerationPlanRecord);

    int update(AccelerationPlanRecord accelerationPlanRecord);
}
