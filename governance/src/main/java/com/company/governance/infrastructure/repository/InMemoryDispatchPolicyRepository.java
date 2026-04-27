package com.company.governance.infrastructure.repository;

import com.company.governance.domain.dispatchpolicy.DispatchPolicyConfig;
import com.company.governance.domain.dispatchpolicy.repository.DispatchPolicyRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryDispatchPolicyRepository implements DispatchPolicyRepository {

    private final Map<String, DispatchPolicyConfig> configs = new ConcurrentHashMap<String, DispatchPolicyConfig>();

    @Override
    public DispatchPolicyConfig save(DispatchPolicyConfig config) {
        configs.put(config.getPolicyId(), config);
        return config;
    }

    @Override
    public List<DispatchPolicyConfig> findByTenantId(String tenantId) {
        List<DispatchPolicyConfig> result = new ArrayList<DispatchPolicyConfig>();
        for (DispatchPolicyConfig config : configs.values()) {
            if (tenantId.equals(config.getTenantId())) {
                result.add(config);
            }
        }
        result.sort(Comparator.comparing(DispatchPolicyConfig::getPolicyName));
        return result;
    }
}
