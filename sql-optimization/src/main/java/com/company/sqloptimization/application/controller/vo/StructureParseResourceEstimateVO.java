package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class StructureParseResourceEstimateVO {

    private String overall;
    private String cpu;
    private String io;
    private String memory;
    private String network;
    private String resultSize;
    private List<String> evidence;

    public String getOverall() { return overall; }
    public void setOverall(String overall) { this.overall = overall; }
    public String getCpu() { return cpu; }
    public void setCpu(String cpu) { this.cpu = cpu; }
    public String getIo() { return io; }
    public void setIo(String io) { this.io = io; }
    public String getMemory() { return memory; }
    public void setMemory(String memory) { this.memory = memory; }
    public String getNetwork() { return network; }
    public void setNetwork(String network) { this.network = network; }
    public String getResultSize() { return resultSize; }
    public void setResultSize(String resultSize) { this.resultSize = resultSize; }
    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }
}
