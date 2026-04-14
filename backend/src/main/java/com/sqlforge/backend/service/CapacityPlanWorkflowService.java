package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.CapacityPlanRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CapacityPlanWorkflowService {

    public Map<String, Object> execute(CapacityPlanRequest request) {
        Map<String, Object> capacityPlan = buildCapacityPlan(request);
        int targetConcurrency = request.getTargetConcurrency() == null
            ? asInt(castMap(capacityPlan.get("workerPlan")).get("requiredWorkers"), 1)
            : request.getTargetConcurrency().intValue();
        double hotDataGb = request.getHotDataGb() == null ? 2000 : request.getHotDataGb().doubleValue();
        Map<String, Object> estimatedResources = estimateResources(request.getQueryMix(), targetConcurrency, hotDataGb);
        Map<String, Object> report = buildCapacityReport(capacityPlan, estimatedResources);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("workflow", "promotion-capacity-planning");
        result.put("capacityPlan", capacityPlan);
        result.put("estimatedResources", estimatedResources);
        result.put("report", report);
        return result;
    }

    private Map<String, Object> buildCapacityPlan(CapacityPlanRequest request) {
        double baselineQps = asDouble(request.getBaselineQps(), 100);
        double growthFactor = asDouble(request.getTrafficGrowthFactor(), 1);
        double avgServiceTimeSec = asDouble(request.getAvgServiceTimeSec(), 2);
        int currentWorkers = asInt(request.getCurrentWorkers(), 10);
        int targetP99Ms = asInt(request.getTargetP99Ms(), 5000);

        double projectedQps = baselineQps * growthFactor;
        double utilizationTarget = targetP99Ms <= 5000 ? 0.65 : 0.75;
        int workerDemand = (int) Math.ceil((projectedQps * avgServiceTimeSec) / utilizationTarget);
        int workerGap = Math.max(0, workerDemand - currentWorkers);
        double averageWaitingSec = round(
            Math.max(0, (projectedQps * avgServiceTimeSec) / Math.max(workerDemand, 1) - utilizationTarget)
        );

        return mapOf(
            "arrivalModel",
            mapOf("baselineQps", baselineQps, "growthFactor", growthFactor, "projectedQps", round(projectedQps)),
            "target",
            mapOf("targetP99Ms", Integer.valueOf(targetP99Ms), "utilizationTarget", utilizationTarget),
            "workerPlan",
            mapOf(
                "currentWorkers",
                Integer.valueOf(currentWorkers),
                "requiredWorkers",
                Integer.valueOf(workerDemand),
                "additionalWorkers",
                Integer.valueOf(workerGap)
            ),
            "queueEstimate",
            mapOf("averageWaitingSec", averageWaitingSec)
        );
    }

    private Map<String, Object> estimateResources(List<CapacityPlanRequest.QueryMixItem> queryMix, int targetConcurrency, double hotDataGb) {
        double cpuCoreDemand = 0;
        double executionMemoryGb = 0;

        if (queryMix != null) {
            for (CapacityPlanRequest.QueryMixItem query : queryMix) {
                cpuCoreDemand += (asDouble(query.getCpuTimeSec(), 0) * asDouble(query.getArrivalRate(), 0)) / (3600 * 0.7);
                executionMemoryGb += asDouble(query.getMemoryPeakGb(), 0);
            }
        }

        double memoryGb = executionMemoryGb * Math.max(1, targetConcurrency * 0.15) * 1.5 + hotDataGb * 0.1 + 32;
        double diskTb = hotDataGb * 0.002 + memoryGb / 512 + 0.1;
        double networkGbps = Math.max(10, round((hotDataGb * 8 * 1.5) / 1024));

        return mapOf(
            "cpuCores",
            Integer.valueOf((int) Math.ceil(cpuCoreDemand)),
            "memoryGb",
            Integer.valueOf((int) Math.ceil(memoryGb)),
            "diskTb",
            round(diskTb),
            "networkGbps",
            networkGbps
        );
    }

    private Map<String, Object> buildCapacityReport(Map<String, Object> capacityPlan, Map<String, Object> estimatedResources) {
        return mapOf(
            "reportType",
            "capacity-plan",
            "arrivalModel",
            capacityPlan.get("arrivalModel"),
            "workerPlan",
            capacityPlan.get("workerPlan"),
            "queueEstimate",
            capacityPlan.get("queueEstimate"),
            "estimatedResources",
            estimatedResources
        );
    }

    private double asDouble(Number value, double fallback) {
        return value == null ? fallback : value.doubleValue();
    }

    private int asInt(Number value, int fallback) {
        return value == null ? fallback : value.intValue();
    }

    private int asInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value == null ? new LinkedHashMap<String, Object>() : (Map<String, Object>) value;
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return result;
    }
}
