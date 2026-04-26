package com.company.sqloptimization.application.controller.vo;

public class CombinedParseStatusVO {

    private String parseTaskId;
    private String status;
    private StructureParseResponseVO structureParse;
    private AccessParseResponseVO accessParse;
    private String degradeReason;

    public String getParseTaskId() {
        return parseTaskId;
    }

    public void setParseTaskId(String parseTaskId) {
        this.parseTaskId = parseTaskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public StructureParseResponseVO getStructureParse() {
        return structureParse;
    }

    public void setStructureParse(StructureParseResponseVO structureParse) {
        this.structureParse = structureParse;
    }

    public AccessParseResponseVO getAccessParse() {
        return accessParse;
    }

    public void setAccessParse(AccessParseResponseVO accessParse) {
        this.accessParse = accessParse;
    }

    public String getDegradeReason() {
        return degradeReason;
    }

    public void setDegradeReason(String degradeReason) {
        this.degradeReason = degradeReason;
    }
}
