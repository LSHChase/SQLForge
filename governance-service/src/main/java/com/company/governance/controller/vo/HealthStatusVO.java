package com.company.governance.controller.vo;

public class HealthStatusVO {

    private final String service;
    private final String status;

    public HealthStatusVO(String service, String status) {
        this.service = service;
        this.status = status;
    }

    public String getService() {
        return service;
    }

    public String getStatus() {
        return status;
    }
}
