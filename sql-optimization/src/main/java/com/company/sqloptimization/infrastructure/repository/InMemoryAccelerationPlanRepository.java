package com.company.sqloptimization.infrastructure.repository;

import com.company.sqloptimization.domain.plan.AccelerationPlan;
import com.company.sqloptimization.domain.plan.repository.AccelerationPlanRepository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "sql-optimization.queues", name = "mode", havingValue = "local-placeholder", matchIfMissing = true)
public class InMemoryAccelerationPlanRepository implements AccelerationPlanRepository {

    private final Map<String, AccelerationPlan> store = new ConcurrentHashMap<String, AccelerationPlan>();

    @Override
    public AccelerationPlan save(AccelerationPlan accelerationPlan) {
        store.put(accelerationPlan.getPlanId(), accelerationPlan);
        return accelerationPlan;
    }

    @Override
    public AccelerationPlan findByPlanId(String planId) {
        return store.get(planId);
    }
}
