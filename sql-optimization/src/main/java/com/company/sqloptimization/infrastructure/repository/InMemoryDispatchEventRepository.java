package com.company.sqloptimization.infrastructure.repository;

import com.company.sqloptimization.domain.dispatch.DispatchEvent;
import com.company.sqloptimization.domain.dispatch.DispatchEventStatus;
import com.company.sqloptimization.domain.dispatch.repository.DispatchEventRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class InMemoryDispatchEventRepository implements DispatchEventRepository {

    private final Map<String, DispatchEvent> events = new ConcurrentHashMap<String, DispatchEvent>();

    @Override
    public DispatchEvent save(DispatchEvent event) {
        events.put(event.getDispatchEventId(), event);
        return event;
    }

    @Override
    public DispatchEvent findByDispatchEventId(String dispatchEventId) {
        return events.get(dispatchEventId);
    }

    @Override
    public List<DispatchEvent> findByTenantId(String tenantId) {
        return sort(filter(tenantId, null));
    }

    @Override
    public List<DispatchEvent> findByTenantIdAndStatus(String tenantId, DispatchEventStatus status) {
        return sort(filter(tenantId, status));
    }

    @Override
    public List<DispatchEvent> findByTenantIdAndRecommendationId(String tenantId, String recommendationId) {
        List<DispatchEvent> result = new ArrayList<DispatchEvent>();
        for (DispatchEvent event : events.values()) {
            if (event.getTenantId().equals(tenantId) && event.getRecommendationId().equals(recommendationId)) {
                result.add(event);
            }
        }
        return sort(result);
    }

    private List<DispatchEvent> filter(String tenantId, DispatchEventStatus status) {
        List<DispatchEvent> result = new ArrayList<DispatchEvent>();
        for (DispatchEvent event : events.values()) {
            if (event.getTenantId().equals(tenantId) && (status == null || event.getStatus() == status)) {
                result.add(event);
            }
        }
        return result;
    }

    private List<DispatchEvent> sort(List<DispatchEvent> events) {
        events.sort(Comparator.comparing(DispatchEvent::getCreatedAt).reversed());
        return events;
    }
}
