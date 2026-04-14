package com.sqlforge.backend.web.dto;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class PlanStabilityRequest {

    private String tenantId;

    @NotBlank
    private String sql;

    @Min(1)
    private Integer slaMs;

    @Min(1)
    private Integer targetConcurrency;

    @Valid
    private PlanSummary historicalBestPlan;

    @NotNull
    @Valid
    private PlanSummary currentPlan;

    @Valid
    private List<PlanSummary> candidatePlans;

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

    public PlanSummary getHistoricalBestPlan() {
        return historicalBestPlan;
    }

    public void setHistoricalBestPlan(PlanSummary historicalBestPlan) {
        this.historicalBestPlan = historicalBestPlan;
    }

    public PlanSummary getCurrentPlan() {
        return currentPlan;
    }

    public void setCurrentPlan(PlanSummary currentPlan) {
        this.currentPlan = currentPlan;
    }

    public List<PlanSummary> getCandidatePlans() {
        return candidatePlans;
    }

    public void setCandidatePlans(List<PlanSummary> candidatePlans) {
        this.candidatePlans = candidatePlans;
    }

    public static class PlanSummary {
        @NotBlank
        private String planHash;

        @DecimalMin("0.0")
        private Double latencyMs;

        private String distribution;

        @DecimalMin("0.0")
        private Double statsAgeHours;

        private List<String> joinOrder;

        public String getPlanHash() {
            return planHash;
        }

        public void setPlanHash(String planHash) {
            this.planHash = planHash;
        }

        public Double getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Double latencyMs) {
            this.latencyMs = latencyMs;
        }

        public String getDistribution() {
            return distribution;
        }

        public void setDistribution(String distribution) {
            this.distribution = distribution;
        }

        public Double getStatsAgeHours() {
            return statsAgeHours;
        }

        public void setStatsAgeHours(Double statsAgeHours) {
            this.statsAgeHours = statsAgeHours;
        }

        public List<String> getJoinOrder() {
            return joinOrder;
        }

        public void setJoinOrder(List<String> joinOrder) {
            this.joinOrder = joinOrder;
        }
    }
}
