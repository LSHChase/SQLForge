package com.company.sqloptimization.application.controller.dto;

public class AccessParseRequest extends StructureParseRequest {

    private Boolean connectionRequired = Boolean.TRUE;

    public Boolean getConnectionRequired() {
        return connectionRequired;
    }

    public void setConnectionRequired(Boolean connectionRequired) {
        this.connectionRequired = connectionRequired;
    }
}
