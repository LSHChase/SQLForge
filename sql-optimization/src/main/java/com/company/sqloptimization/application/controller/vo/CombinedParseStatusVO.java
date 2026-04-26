package com.company.sqloptimization.application.controller.vo;

public class CombinedParseStatusVO {

    private String parseTaskId;
    private String status;
    private StructureParseResponseVO structureParse;
    private AccessParseResponseVO accessParse;
    private CombinedParseConclusionVO conclusion;
    private java.util.List<CombinedParseStatusHistoryVO> statusHistory;
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

    public CombinedParseConclusionVO getConclusion() {
        return conclusion;
    }

    public void setConclusion(CombinedParseConclusionVO conclusion) {
        this.conclusion = conclusion;
    }

    public java.util.List<CombinedParseStatusHistoryVO> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(java.util.List<CombinedParseStatusHistoryVO> statusHistory) {
        this.statusHistory = statusHistory;
    }

    public String getDegradeReason() {
        return degradeReason;
    }

    public void setDegradeReason(String degradeReason) {
        this.degradeReason = degradeReason;
    }
}
