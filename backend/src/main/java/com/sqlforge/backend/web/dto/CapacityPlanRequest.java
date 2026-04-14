package com.sqlforge.backend.web.dto;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class CapacityPlanRequest {

    @NotNull
    @DecimalMin("0.0001")
    private Double baselineQps;

    @NotNull
    @DecimalMin("1.0")
    private Double trafficGrowthFactor;

    @NotNull
    @DecimalMin("0.0001")
    private Double avgServiceTimeSec;

    @NotNull
    @Min(1)
    private Integer currentWorkers;

    @NotNull
    @Min(1)
    private Integer targetP99Ms;

    @Min(1)
    private Integer targetConcurrency;

    @DecimalMin("0.0")
    private Double hotDataGb;

    @Valid
    private List<QueryMixItem> queryMix;

    public Double getBaselineQps() {
        return baselineQps;
    }

    public void setBaselineQps(Double baselineQps) {
        this.baselineQps = baselineQps;
    }

    public Double getTrafficGrowthFactor() {
        return trafficGrowthFactor;
    }

    public void setTrafficGrowthFactor(Double trafficGrowthFactor) {
        this.trafficGrowthFactor = trafficGrowthFactor;
    }

    public Double getAvgServiceTimeSec() {
        return avgServiceTimeSec;
    }

    public void setAvgServiceTimeSec(Double avgServiceTimeSec) {
        this.avgServiceTimeSec = avgServiceTimeSec;
    }

    public Integer getCurrentWorkers() {
        return currentWorkers;
    }

    public void setCurrentWorkers(Integer currentWorkers) {
        this.currentWorkers = currentWorkers;
    }

    public Integer getTargetP99Ms() {
        return targetP99Ms;
    }

    public void setTargetP99Ms(Integer targetP99Ms) {
        this.targetP99Ms = targetP99Ms;
    }

    public Integer getTargetConcurrency() {
        return targetConcurrency;
    }

    public void setTargetConcurrency(Integer targetConcurrency) {
        this.targetConcurrency = targetConcurrency;
    }

    public Double getHotDataGb() {
        return hotDataGb;
    }

    public void setHotDataGb(Double hotDataGb) {
        this.hotDataGb = hotDataGb;
    }

    public List<QueryMixItem> getQueryMix() {
        return queryMix;
    }

    public void setQueryMix(List<QueryMixItem> queryMix) {
        this.queryMix = queryMix;
    }

    public static class QueryMixItem {
        private String name;
        private Double cpuTimeSec;
        private Double arrivalRate;
        private Double memoryPeakGb;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Double getCpuTimeSec() {
            return cpuTimeSec;
        }

        public void setCpuTimeSec(Double cpuTimeSec) {
            this.cpuTimeSec = cpuTimeSec;
        }

        public Double getArrivalRate() {
            return arrivalRate;
        }

        public void setArrivalRate(Double arrivalRate) {
            this.arrivalRate = arrivalRate;
        }

        public Double getMemoryPeakGb() {
            return memoryPeakGb;
        }

        public void setMemoryPeakGb(Double memoryPeakGb) {
            this.memoryPeakGb = memoryPeakGb;
        }
    }
}
