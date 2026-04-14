package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.CapacityPlanRequest;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapacityPlanWorkflowServiceTest {

    @Test
    void shouldProduceCapacityPlanWorkflowResult() {
        CapacityPlanWorkflowService service = new CapacityPlanWorkflowService();
        CapacityPlanRequest request = request();

        Map<String, Object> result = service.execute(request);

        assertEquals("promotion-capacity-planning", result.get("workflow"));
        assertTrue(result.containsKey("capacityPlan"));
        assertTrue(result.containsKey("estimatedResources"));
        assertTrue(result.containsKey("report"));

        Map<String, Object> capacityPlan = castMap(result.get("capacityPlan"));
        Map<String, Object> workerPlan = castMap(capacityPlan.get("workerPlan"));

        assertEquals(Integer.valueOf(60), workerPlan.get("requiredWorkers"));
        assertEquals(Integer.valueOf(36), workerPlan.get("additionalWorkers"));
    }

    private CapacityPlanRequest request() {
        CapacityPlanRequest request = new CapacityPlanRequest();
        request.setBaselineQps(Double.valueOf(120));
        request.setTrafficGrowthFactor(Double.valueOf(2.4));
        request.setAvgServiceTimeSec(Double.valueOf(0.135));
        request.setCurrentWorkers(Integer.valueOf(24));
        request.setTargetP99Ms(Integer.valueOf(4000));
        request.setHotDataGb(Double.valueOf(2400));
        request.setQueryMix(Arrays.asList(query("dashboard", 4.5, 180, 8.0), query("detail", 1.1, 90, 3.0)));
        return request;
    }

    private CapacityPlanRequest.QueryMixItem query(String name, double cpuTimeSec, double arrivalRate, double memoryPeakGb) {
        CapacityPlanRequest.QueryMixItem item = new CapacityPlanRequest.QueryMixItem();
        item.setName(name);
        item.setCpuTimeSec(Double.valueOf(cpuTimeSec));
        item.setArrivalRate(Double.valueOf(arrivalRate));
        item.setMemoryPeakGb(Double.valueOf(memoryPeakGb));
        return item;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
