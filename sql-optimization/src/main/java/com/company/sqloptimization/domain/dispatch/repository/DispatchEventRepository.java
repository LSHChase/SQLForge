package com.company.sqloptimization.domain.dispatch.repository;

import com.company.sqloptimization.domain.dispatch.DispatchEvent;
import com.company.sqloptimization.domain.dispatch.DispatchEventStatus;
import java.util.List;

public interface DispatchEventRepository {

    DispatchEvent save(DispatchEvent event);

    DispatchEvent findByDispatchEventId(String dispatchEventId);

    List<DispatchEvent> findByTenantId(String tenantId);

    List<DispatchEvent> findByTenantIdAndStatus(String tenantId, DispatchEventStatus status);
}
