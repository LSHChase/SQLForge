package com.company.sqloptimization.infrastructure.persistence;

import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationTaskPhase;
import com.company.sqloptimization.domain.task.OptimizationTaskRisk;
import com.company.sqloptimization.domain.task.OptimizationTaskSourceContext;
import com.company.sqloptimization.domain.task.OptimizationTaskStatus;
import com.company.sqloptimization.domain.task.OptimizationTaskStatusTransition;
import com.company.sqloptimization.domain.task.OptimizationTaskSuggestion;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class OptimizationTaskJsonCodec {

    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<List<Map<String, Object>>>() {
    };
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<List<String>>() {
    };
    private static final TypeReference<List<OptimizationTaskRisk>> LIST_OF_RISKS = new TypeReference<List<OptimizationTaskRisk>>() {
    };

    private final ObjectMapper objectMapper = JsonUtils.objectMapper();

    OptimizationTaskSuggestion readSuggestion(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, OptimizationTaskSuggestion.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("优化建议载荷反序列化失败", ex);
        }
    }

    List<AccelerationSuggestionType> readSuggestionTypes(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<String> values = objectMapper.readValue(json, LIST_OF_STRINGS);
            List<AccelerationSuggestionType> types = new ArrayList<AccelerationSuggestionType>(values.size());
            for (String value : values) {
                types.add(AccelerationSuggestionType.valueOf(value));
            }
            return types;
        } catch (Exception ex) {
            throw new IllegalArgumentException("优化建议类型反序列化失败", ex);
        }
    }

    OptimizationTaskSourceContext readSourceContext(String json) {
        if (json == null || json.trim().isEmpty()) {
            return OptimizationTaskSourceContext.empty();
        }
        try {
            return objectMapper.readValue(json, OptimizationTaskSourceContext.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("优化任务上下文载荷反序列化失败", ex);
        }
    }

    String writeSourceContextJson(OptimizationTaskSourceContext sourceContext) {
        if (sourceContext == null || sourceContext.isEmpty()) {
            return null;
        }
        return writeJson(sourceContext);
    }

    List<OptimizationTaskStatusTransition> readStatusHistory(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> items = objectMapper.readValue(json, LIST_OF_MAPS);
            List<OptimizationTaskStatusTransition> history =
                new ArrayList<OptimizationTaskStatusTransition>(items.size());
            for (Map<String, Object> item : items) {
                history.add(new OptimizationTaskStatusTransition(
                    readEnum(item.get("previousStatus"), OptimizationTaskStatus.class),
                    readEnum(item.get("currentStatus"), OptimizationTaskStatus.class),
                    readEnum(item.get("previousPhase"), OptimizationTaskPhase.class),
                    readEnum(item.get("currentPhase"), OptimizationTaskPhase.class),
                    item.get("occurredAt") == null ? null : Instant.parse(String.valueOf(item.get("occurredAt"))),
                    item.get("note") == null ? null : String.valueOf(item.get("note"))
                ));
            }
            return history;
        } catch (Exception ex) {
            throw new IllegalArgumentException("优化任务状态历史反序列化失败", ex);
        }
    }

    OptimizationTaskPhase readFailedPhase(String failedPhase) {
        if (failedPhase == null || failedPhase.trim().isEmpty()) {
            return null;
        }
        return OptimizationTaskPhase.valueOf(failedPhase);
    }

    List<OptimizationTaskRisk> readRisks(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, LIST_OF_RISKS);
        } catch (Exception ex) {
            throw new IllegalArgumentException("优化任务风险反序列化失败", ex);
        }
    }

    String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("优化任务持久化载荷序列化失败", ex);
        }
    }

    private <T extends Enum<T>> T readEnum(Object value, Class<T> enumType) {
        if (value == null) {
            return null;
        }
        return Enum.valueOf(enumType, String.valueOf(value));
    }
}
