package com.company.sqloptimization.domain.plan.repository;

import com.company.sqloptimization.domain.plan.AccelerationPlan;

public interface AccelerationPlanRepository {

    AccelerationPlan save(AccelerationPlan accelerationPlan);

    AccelerationPlan findByPlanId(String planId);
}
