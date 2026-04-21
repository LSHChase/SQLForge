package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class OptimizationFailureVO {

    private final int code;
    private final String message;
    private final String suggestedAction;
    private final boolean retryable;
    private final String failedPhase;
    private final List<OptimizationRiskVO> risks;

    public OptimizationFailureVO(int code,
                                 String message,
                                 String suggestedAction,
                                 boolean retryable,
                                 String failedPhase,
                                 List<OptimizationRiskVO> risks) {
        this.code = code;
        this.message = message;
        this.suggestedAction = suggestedAction;
        this.retryable = retryable;
        this.failedPhase = failedPhase;
        this.risks = risks;
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

    public String getFailedPhase() {
        return failedPhase;
    }

    public List<OptimizationRiskVO> getRisks() {
        return risks;
    }
}
