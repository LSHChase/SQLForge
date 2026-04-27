package com.company.sqloptimization.infrastructure.persistence;

import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.domain.dispatch.DispatchEvent;
import com.company.sqloptimization.domain.dispatch.DispatchEventStatus;
import com.company.sqloptimization.domain.dispatch.DispatchEventTransition;
import com.company.sqloptimization.domain.dispatch.DispatchType;
import com.company.sqloptimization.domain.dispatch.repository.DispatchEventRepository;
import com.company.sqloptimization.infrastructure.persistence.entity.DispatchEventRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.DispatchEventMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "sql-optimization.dispatch-event", name = "repository", havingValue = "database")
public class MybatisDispatchEventRepository implements DispatchEventRepository {

    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<List<Map<String, Object>>>() {
    };

    private final DispatchEventMapper dispatchEventMapper;

    public MybatisDispatchEventRepository(DispatchEventMapper dispatchEventMapper) {
        this.dispatchEventMapper = dispatchEventMapper;
    }

    @Override
    public DispatchEvent save(DispatchEvent event) {
        DispatchEventRecord record = toRecord(event);
        if (dispatchEventMapper.selectByDispatchEventId(event.getDispatchEventId()) == null) {
            dispatchEventMapper.insert(record);
        } else {
            dispatchEventMapper.update(record);
        }
        return event;
    }

    @Override
    public DispatchEvent findByDispatchEventId(String dispatchEventId) {
        DispatchEventRecord record = dispatchEventMapper.selectByDispatchEventId(dispatchEventId);
        return record == null ? null : toDomain(record);
    }

    @Override
    public List<DispatchEvent> findByTenantId(String tenantId) {
        return toDomains(dispatchEventMapper.selectByTenantId(tenantId));
    }

    @Override
    public List<DispatchEvent> findByTenantIdAndStatus(String tenantId, DispatchEventStatus status) {
        return toDomains(dispatchEventMapper.selectByTenantIdAndStatus(tenantId, status.name()));
    }

    @Override
    public List<DispatchEvent> findByTenantIdAndRecommendationId(String tenantId, String recommendationId) {
        return toDomains(dispatchEventMapper.selectByTenantIdAndRecommendationId(tenantId, recommendationId));
    }

    private List<DispatchEvent> toDomains(List<DispatchEventRecord> records) {
        List<DispatchEvent> events = new ArrayList<DispatchEvent>();
        for (DispatchEventRecord record : records) {
            events.add(toDomain(record));
        }
        return events;
    }

    private DispatchEventRecord toRecord(DispatchEvent event) {
        DispatchEventRecord record = new DispatchEventRecord();
        record.setDispatchEventId(event.getDispatchEventId());
        record.setTenantId(event.getTenantId());
        record.setRecommendationId(event.getRecommendationId());
        record.setDispatchType(event.getDispatchType().name());
        record.setDispatchPayloadJson(event.getDispatchPayloadJson());
        record.setTargetEngine(event.getTargetEngine());
        record.setTargetDatasource(event.getTargetDatasource());
        record.setReportCode(event.getReportCode());
        record.setLogicalObjectKey(event.getLogicalObjectKey());
        record.setStatus(event.getStatus().name());
        record.setPulledBy(event.getPulledBy());
        record.setPulledAt(toLocalDateTime(event.getPulledAt()));
        record.setAckedBy(event.getAckedBy());
        record.setAckedAt(toLocalDateTime(event.getAckedAt()));
        record.setFailedBy(event.getFailedBy());
        record.setFailedAt(toLocalDateTime(event.getFailedAt()));
        record.setResultMessage(event.getResultMessage());
        record.setStatusHistoryJson(JsonUtils.toJson(event.getStatusHistory()));
        record.setCreatedBy(event.getCreatedBy());
        record.setCreatedAt(toLocalDateTime(event.getCreatedAt()));
        record.setUpdatedAt(toLocalDateTime(event.getUpdatedAt()));
        return record;
    }

    private DispatchEvent toDomain(DispatchEventRecord record) {
        return DispatchEvent.restore(
            record.getDispatchEventId(),
            record.getTenantId(),
            record.getRecommendationId(),
            DispatchType.valueOf(record.getDispatchType()),
            record.getDispatchPayloadJson(),
            record.getTargetEngine(),
            record.getTargetDatasource(),
            record.getReportCode(),
            record.getLogicalObjectKey(),
            DispatchEventStatus.valueOf(record.getStatus()),
            record.getPulledBy(),
            toInstant(record.getPulledAt()),
            record.getAckedBy(),
            toInstant(record.getAckedAt()),
            record.getFailedBy(),
            toInstant(record.getFailedAt()),
            record.getResultMessage(),
            record.getCreatedBy(),
            toInstant(record.getCreatedAt()),
            toInstant(record.getUpdatedAt()),
            readStatusHistory(record.getStatusHistoryJson())
        );
    }

    private List<DispatchEventTransition> readStatusHistory(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> items = JsonUtils.objectMapper().readValue(json, LIST_OF_MAPS);
            List<DispatchEventTransition> history = new ArrayList<DispatchEventTransition>(items.size());
            for (Map<String, Object> item : items) {
                history.add(new DispatchEventTransition(
                    item.get("previousStatus") == null ? null : DispatchEventStatus.valueOf(String.valueOf(item.get("previousStatus"))),
                    item.get("currentStatus") == null ? null : DispatchEventStatus.valueOf(String.valueOf(item.get("currentStatus"))),
                    item.get("occurredAt") == null ? null : Instant.parse(String.valueOf(item.get("occurredAt"))),
                    item.get("note") == null ? null : String.valueOf(item.get("note")),
                    item.get("operator") == null ? null : String.valueOf(item.get("operator"))
                ));
            }
            return history;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to deserialize dispatch event status history", ex);
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, DATABASE_ZONE_OFFSET);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(DATABASE_ZONE_OFFSET);
    }
}
