package com.sqlforge.backend.web.dto;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

public class BiReleaseRequest {

    @NotBlank
    private String tenantId;

    @NotBlank
    private String sql;

    @Min(1)
    private Integer slaMs;

    @Min(1)
    private Integer targetConcurrency;

    private Integer sampleLatencyMs;

    @Valid
    private DataProfile dataProfile;

    @Valid
    private List<Metric> metrics;

    private Map<String, Object> resourceBreakdown;

    @Valid
    private List<QueryMixItem> queryMix;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public Integer getSlaMs() {
        return slaMs;
    }

    public void setSlaMs(Integer slaMs) {
        this.slaMs = slaMs;
    }

    public Integer getTargetConcurrency() {
        return targetConcurrency;
    }

    public void setTargetConcurrency(Integer targetConcurrency) {
        this.targetConcurrency = targetConcurrency;
    }

    public Integer getSampleLatencyMs() {
        return sampleLatencyMs;
    }

    public void setSampleLatencyMs(Integer sampleLatencyMs) {
        this.sampleLatencyMs = sampleLatencyMs;
    }

    public DataProfile getDataProfile() {
        return dataProfile;
    }

    public void setDataProfile(DataProfile dataProfile) {
        this.dataProfile = dataProfile;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Metric> metrics) {
        this.metrics = metrics;
    }

    public Map<String, Object> getResourceBreakdown() {
        return resourceBreakdown;
    }

    public void setResourceBreakdown(Map<String, Object> resourceBreakdown) {
        this.resourceBreakdown = resourceBreakdown;
    }

    public List<QueryMixItem> getQueryMix() {
        return queryMix;
    }

    public void setQueryMix(List<QueryMixItem> queryMix) {
        this.queryMix = queryMix;
    }

    public static class DataProfile {
        private Long fullRows;
        private Long sampleRows;
        private Double skew;
        private Double hotDataGb;

        public Long getFullRows() {
            return fullRows;
        }

        public void setFullRows(Long fullRows) {
            this.fullRows = fullRows;
        }

        public Long getSampleRows() {
            return sampleRows;
        }

        public void setSampleRows(Long sampleRows) {
            this.sampleRows = sampleRows;
        }

        public Double getSkew() {
            return skew;
        }

        public void setSkew(Double skew) {
            this.skew = skew;
        }

        public Double getHotDataGb() {
            return hotDataGb;
        }

        public void setHotDataGb(Double hotDataGb) {
            this.hotDataGb = hotDataGb;
        }
    }

    public static class Metric {
        private Integer run;
        private Double latencyMs;
        private Double gcPauseMs;

        public Integer getRun() {
            return run;
        }

        public void setRun(Integer run) {
            this.run = run;
        }

        public Double getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Double latencyMs) {
            this.latencyMs = latencyMs;
        }

        public Double getGcPauseMs() {
            return gcPauseMs;
        }

        public void setGcPauseMs(Double gcPauseMs) {
            this.gcPauseMs = gcPauseMs;
        }
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
