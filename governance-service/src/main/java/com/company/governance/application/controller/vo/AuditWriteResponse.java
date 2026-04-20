package com.company.governance.application.controller.vo;

public class AuditWriteResponse {

    private final String serviceCode;
    private final String operationCode;
    private final String status;
    private final String messageTopic;
    private final String deliveryMode;
    private final String contractStage;
    private final String implementationStage;

    public AuditWriteResponse(String serviceCode,
                              String operationCode,
                              String status,
                              String messageTopic,
                              String deliveryMode,
                              String contractStage,
                              String implementationStage) {
        this.serviceCode = serviceCode;
        this.operationCode = operationCode;
        this.status = status;
        this.messageTopic = messageTopic;
        this.deliveryMode = deliveryMode;
        this.contractStage = contractStage;
        this.implementationStage = implementationStage;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public String getStatus() {
        return status;
    }

    public String getMessageTopic() {
        return messageTopic;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public String getContractStage() {
        return contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }
}
