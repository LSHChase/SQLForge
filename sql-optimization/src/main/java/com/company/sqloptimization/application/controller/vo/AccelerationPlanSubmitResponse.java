package com.company.sqloptimization.application.controller.vo;

import com.company.sqloptimization.domain.plan.AccelerationPlanStatus;

public class AccelerationPlanSubmitResponse {

    private final String planId;
    private final AccelerationPlanStatus status;
    private final String statusQueryPath;
    private final String contractStage;
    private final String implementationStage;

    public AccelerationPlanSubmitResponse(String planId,
                                          AccelerationPlanStatus status,
                                          String statusQueryPath,
                                          String contractStage,
                                          String implementationStage) {
        this.planId = planId;
        this.status = status;
        this.statusQueryPath = statusQueryPath;
        this.contractStage = contractStage;
        this.implementationStage = implementationStage;
    }

    public String getPlanId() {
        return planId;
    }

    public AccelerationPlanStatus getStatus() {
        return status;
    }

    public String getStatusQueryPath() {
        return statusQueryPath;
    }

    public String getContractStage() {
        return contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }
}
