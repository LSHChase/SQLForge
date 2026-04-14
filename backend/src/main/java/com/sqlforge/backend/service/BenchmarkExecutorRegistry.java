package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.BiReleaseRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkExecutorRegistry {

    private final List<BenchmarkExecutorProvider> providers;

    public BenchmarkExecutorRegistry(List<BenchmarkExecutorProvider> providers) {
        this.providers = providers == null ? new ArrayList<BenchmarkExecutorProvider>() : providers;
    }

    public Map<String, Object> buildExecutionPlan(
        BiReleaseRequest request,
        Map<String, Object> assessmentResult,
        Map<String, Object> benchmarkPlan,
        Map<String, Object> benchmarkAnalysis
    ) {
        if (providers.isEmpty()) {
            Map<String, Object> unavailable = new LinkedHashMap<String, Object>();
            unavailable.put("providerCode", "unavailable");
            unavailable.put("mode", "none");
            unavailable.put("executable", Boolean.FALSE);
            unavailable.put("summary", "当前没有可用的 benchmark executor provider");
            unavailable.put("providers", new ArrayList<String>());
            return unavailable;
        }

        BenchmarkExecutorProvider primaryProvider = providers.get(0);
        Map<String, Object> plan = new LinkedHashMap<String, Object>(
            primaryProvider.buildExecutionPlan(request, assessmentResult, benchmarkPlan, benchmarkAnalysis)
        );
        List<String> providerCodes = new ArrayList<String>();
        for (BenchmarkExecutorProvider provider : providers) {
            providerCodes.add(provider.getCode());
        }
        plan.put("providers", providerCodes);
        plan.put("recommendedProvider", primaryProvider.getCode());
        return plan;
    }
}
