package com.company.sqloptimization.application.controller.dto;

import com.company.sqloptimization.domain.dispatch.DispatchType;

public class DispatchRecommendationRequest {

    private DispatchType dispatchType;
    private String resultMessage;

    public DispatchType getDispatchType() {
        return dispatchType;
    }

    public void setDispatchType(DispatchType dispatchType) {
        this.dispatchType = dispatchType;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }
}
