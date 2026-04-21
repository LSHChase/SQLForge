package com.company.governance.application.controller.vo;

public class ScheduleExtensionStatusVO {

    private final String extensionPoint;
    private final String ownerService;
    private final String status;
    private final String currentMode;
    private final String contractStage;
    private final String implementationStage;

    public ScheduleExtensionStatusVO(String extensionPoint,
                                     String ownerService,
                                     String status,
                                     String currentMode,
                                     String contractStage,
                                     String implementationStage) {
        this.extensionPoint = extensionPoint;
        this.ownerService = ownerService;
        this.status = status;
        this.currentMode = currentMode;
        this.contractStage = contractStage;
        this.implementationStage = implementationStage;
    }

    public String getExtensionPoint() {
        return extensionPoint;
    }

    public String getOwnerService() {
        return ownerService;
    }

    public String getStatus() {
        return status;
    }

    public String getCurrentMode() {
        return currentMode;
    }

    public String getContractStage() {
        return contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }
}
