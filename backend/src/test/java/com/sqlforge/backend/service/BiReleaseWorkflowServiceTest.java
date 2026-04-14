package com.sqlforge.backend.service;

import com.sqlforge.backend.repository.InMemoryWorkflowBaselineRepository;
import com.sqlforge.backend.web.dto.BiReleaseRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiReleaseWorkflowServiceTest {

    @Test
    void shouldProduceBiReleaseWorkflowResult() {
        BiReleaseWorkflowService service = new BiReleaseWorkflowService(new InMemoryWorkflowBaselineRepository());
        BiReleaseRequest request = request();

        Map<String, Object> result = service.execute(request);

        assertEquals("bi-release-evaluation", result.get("workflow"));
        assertTrue(result.containsKey("benchmarkPlan"));
        assertTrue(result.containsKey("benchmarkAnalysis"));
        assertTrue(result.containsKey("executorPlan"));
        assertTrue(result.containsKey("report"));
        assertTrue(result.containsKey("decision"));
        assertEquals("dry-run", castMap(result.get("executorPlan")).get("providerCode"));
    }

    @Test
    void shouldCompareAgainstPreviousBaselineOnSecondRun() {
        BiReleaseWorkflowService service = new BiReleaseWorkflowService(new InMemoryWorkflowBaselineRepository());
        BiReleaseRequest first = request();
        BiReleaseRequest second = request();
        second.setMetrics(metrics(
            metric(1, 1000, 10),
            metric(2, 1010, 10),
            metric(3, 1020, 10),
            metric(4, 1800, 10),
            metric(5, 1900, 10),
            metric(6, 2100, 10)
        ));

        service.execute(first);
        Map<String, Object> result = service.execute(second);

        Map<String, Object> benchmarkAnalysis = castMap(result.get("benchmarkAnalysis"));
        Map<String, Object> baselineComparison = castMap(benchmarkAnalysis.get("baselineComparison"));

        assertEquals(Boolean.TRUE, baselineComparison.get("hasBaseline"));
        assertFalse("new-baseline".equals(baselineComparison.get("status")));
    }

    private BiReleaseRequest request() {
        BiReleaseRequest request = new BiReleaseRequest();
        request.setTenantId("tenant-a");
        request.setSql("select user_id, sum(amount) from lake.orders where ds >= current_date - interval '7' day group by 1");
        request.setSlaMs(Integer.valueOf(5000));
        request.setTargetConcurrency(Integer.valueOf(20));

        BiReleaseRequest.DataProfile dataProfile = new BiReleaseRequest.DataProfile();
        dataProfile.setFullRows(Long.valueOf(1800000000L));
        dataProfile.setSampleRows(Long.valueOf(36000000L));
        dataProfile.setSkew(Double.valueOf(3.2));
        dataProfile.setHotDataGb(Double.valueOf(280));
        request.setDataProfile(dataProfile);

        request.setMetrics(metrics(
            metric(1, 1000, 10),
            metric(2, 1010, 10),
            metric(3, 1020, 10),
            metric(4, 1100, 10),
            metric(5, 1120, 10),
            metric(6, 1130, 10)
        ));

        return request;
    }

    private List<BiReleaseRequest.Metric> metrics(BiReleaseRequest.Metric... items) {
        return java.util.Arrays.asList(items);
    }

    private BiReleaseRequest.Metric metric(int run, double latencyMs, double gcPauseMs) {
        BiReleaseRequest.Metric metric = new BiReleaseRequest.Metric();
        metric.setRun(Integer.valueOf(run));
        metric.setLatencyMs(Double.valueOf(latencyMs));
        metric.setGcPauseMs(Double.valueOf(gcPauseMs));
        return metric;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
