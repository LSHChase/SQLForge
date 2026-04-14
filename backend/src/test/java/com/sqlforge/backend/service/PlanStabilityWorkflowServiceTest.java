package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.PlanStabilityRequest;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanStabilityWorkflowServiceTest {

    @Test
    void shouldRecommendRefreshingStatsWhenRegressionMatchesStatsDrift() {
        PlanStabilityWorkflowService service = new PlanStabilityWorkflowService(new SqlAssessmentService());
        PlanStabilityRequest request = request();
        request.setHistoricalBestPlan(plan("best-1", 1200, "broadcast", 4, "orders", "dim_users"));
        request.setCurrentPlan(plan("curr-1", 1700, "broadcast", 36, "orders", "dim_users"));

        Map<String, Object> result = service.execute(request);
        Map<String, Object> stabilityAnalysis = castMap(result.get("stabilityAnalysis"));

        assertEquals("plan-stability-analysis", result.get("workflow"));
        assertEquals("refresh-stats", stabilityAnalysis.get("decision"));
        assertTrue(result.containsKey("report"));
    }

    @Test
    void shouldRecommendProtectingBestPlanWhenPlanDriftRegressesLatency() {
        PlanStabilityWorkflowService service = new PlanStabilityWorkflowService(new SqlAssessmentService());
        PlanStabilityRequest request = request();
        request.setHistoricalBestPlan(plan("best-2", 1000, "broadcast", 8, "orders", "dim_users"));
        request.setCurrentPlan(plan("curr-2", 1500, "shuffle", 10, "dim_users", "orders"));

        Map<String, Object> result = service.execute(request);
        Map<String, Object> stabilityAnalysis = castMap(result.get("stabilityAnalysis"));

        assertEquals("protect-best-plan", stabilityAnalysis.get("decision"));
    }

    private PlanStabilityRequest request() {
        PlanStabilityRequest request = new PlanStabilityRequest();
        request.setTenantId("tenant-a");
        request.setSql("select o.user_id, sum(o.amount) from lake.orders o join lake.dim_users u on o.user_id = u.user_id group by 1");
        request.setSlaMs(Integer.valueOf(5000));
        request.setTargetConcurrency(Integer.valueOf(20));
        request.setCandidatePlans(Arrays.asList(plan("cand-1", 950, "broadcast", 6, "orders", "dim_users")));
        return request;
    }

    private PlanStabilityRequest.PlanSummary plan(
        String planHash,
        double latencyMs,
        String distribution,
        double statsAgeHours,
        String... joinOrder
    ) {
        PlanStabilityRequest.PlanSummary plan = new PlanStabilityRequest.PlanSummary();
        plan.setPlanHash(planHash);
        plan.setLatencyMs(Double.valueOf(latencyMs));
        plan.setDistribution(distribution);
        plan.setStatsAgeHours(Double.valueOf(statsAgeHours));
        plan.setJoinOrder(Arrays.asList(joinOrder));
        return plan;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
