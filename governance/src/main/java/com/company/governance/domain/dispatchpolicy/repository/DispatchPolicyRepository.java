package com.company.governance.domain.dispatchpolicy.repository;

import com.company.governance.domain.dispatchpolicy.DispatchPolicyConfig;
import java.util.List;

public interface DispatchPolicyRepository {

    DispatchPolicyConfig save(DispatchPolicyConfig config);

    List<DispatchPolicyConfig> findByTenantId(String tenantId);
}
