package com.company.sqloptimization.domain.task;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Canonical submission command used to create a queued optimization task entity.
 */
public class OptimizationTaskSubmission {

    private final String tenantId;
    private final OptimizationTaskType taskType;
    private final String sqlText;
    private final String sqlFingerprint;
    private final DataSourceTypeEnum datasourceType;
    private final OptimizationTaskPriority priority;
    private final OptimizationParseDepth parseDepth;
    private final String callbackUrl;
    private final List<AccelerationSuggestionType> requestedSuggestionTypes;

    public OptimizationTaskSubmission(String tenantId,
                                      OptimizationTaskType taskType,
                                      String sqlText,
                                      String sqlFingerprint,
                                      DataSourceTypeEnum datasourceType,
                                      OptimizationTaskPriority priority,
                                      OptimizationParseDepth parseDepth,
                                      String callbackUrl,
                                      List<AccelerationSuggestionType> requestedSuggestionTypes) {
        this.tenantId = tenantId;
        this.taskType = taskType;
        this.sqlText = sqlText;
        this.sqlFingerprint = sqlFingerprint;
        this.datasourceType = datasourceType;
        this.priority = priority == null ? OptimizationTaskPriority.NORMAL : priority;
        this.parseDepth = parseDepth == null ? OptimizationParseDepth.DEEP : parseDepth;
        this.callbackUrl = callbackUrl;
        this.requestedSuggestionTypes = normalizeSuggestionTypes(taskType, requestedSuggestionTypes);
    }

    private List<AccelerationSuggestionType> normalizeSuggestionTypes(
        OptimizationTaskType actualTaskType,
        List<AccelerationSuggestionType> suggestionTypes
    ) {
        if (actualTaskType != OptimizationTaskType.ACCELERATION_SUGGESTION) {
            return Collections.emptyList();
        }
        if (suggestionTypes == null || suggestionTypes.isEmpty()) {
            return Collections.singletonList(AccelerationSuggestionType.ALL);
        }
        Set<AccelerationSuggestionType> normalized = new LinkedHashSet<AccelerationSuggestionType>(suggestionTypes);
        if (normalized.contains(AccelerationSuggestionType.ALL)) {
            return Collections.singletonList(AccelerationSuggestionType.ALL);
        }
        return Collections.unmodifiableList(new ArrayList<AccelerationSuggestionType>(normalized));
    }

    public String getTenantId() {
        return tenantId;
    }

    public OptimizationTaskType getTaskType() {
        return taskType;
    }

    public String getSqlText() {
        return sqlText;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public DataSourceTypeEnum getDatasourceType() {
        return datasourceType;
    }

    public OptimizationTaskPriority getPriority() {
        return priority;
    }

    public OptimizationParseDepth getParseDepth() {
        return parseDepth;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public List<AccelerationSuggestionType> getRequestedSuggestionTypes() {
        return requestedSuggestionTypes;
    }
}
