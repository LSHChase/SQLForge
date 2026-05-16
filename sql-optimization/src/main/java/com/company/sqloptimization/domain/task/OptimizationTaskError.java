package com.company.sqloptimization.domain.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

/**
 * 异步优化任务的领域级失败详情。
 */
public class OptimizationTaskError {

    private final int code;
    private final String message;
    private final String suggestedAction;
    private final boolean retryable;
    private final OptimizationTaskPhase failedPhase;
    private final List<OptimizationTaskRisk> risks;

    @JsonCreator
    public OptimizationTaskError(@JsonProperty("code") int code,
                                 @JsonProperty("message") String message,
                                 @JsonProperty("suggestedAction") String suggestedAction,
                                 @JsonProperty("retryable") boolean retryable,
                                 @JsonProperty("failedPhase") OptimizationTaskPhase failedPhase,
                                 @JsonProperty("risks") List<OptimizationTaskRisk> risks) {
        this.code = code;
        this.message = message;
        this.suggestedAction = suggestedAction;
        this.retryable = retryable;
        this.failedPhase = failedPhase;
        this.risks = risks == null ? Collections.<OptimizationTaskRisk>emptyList() : Collections.unmodifiableList(risks);
    }

    public OptimizationTaskError(int code, String message, String suggestedAction, boolean retryable) {
        this(code, message, suggestedAction, retryable, null, Collections.<OptimizationTaskRisk>emptyList());
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public OptimizationTaskPhase getFailedPhase() {
        return failedPhase;
    }

    public List<OptimizationTaskRisk> getRisks() {
        return risks;
    }
}
