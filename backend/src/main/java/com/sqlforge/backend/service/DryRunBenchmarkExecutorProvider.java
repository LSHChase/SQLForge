package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.BiReleaseRequest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DryRunBenchmarkExecutorProvider implements BenchmarkExecutorProvider {

    @Override
    public String getCode() {
        return "dry-run";
    }

    @Override
    public Map<String, Object> buildExecutionPlan(
        BiReleaseRequest request,
        Map<String, Object> assessmentResult,
        Map<String, Object> benchmarkPlan,
        Map<String, Object> benchmarkAnalysis
    ) {
        Map<String, Object> plan = new LinkedHashMap<String, Object>();
        plan.put("providerCode", getCode());
        plan.put("mode", "offline-simulation");
        plan.put("executable", Boolean.FALSE);
        plan.put("summary", "当前仅提供 dry-run 执行契约，用于在真实 benchmark executor 接入前固定交付面");
        plan.put(
            "nextActions",
            Arrays.asList(
                "接入真实 benchmark executor provider",
                "映射压力矩阵到实际执行编排",
                "补齐运行时指标采集与回传"
            )
        );
        plan.put("targetConcurrency", benchmarkPlan.get("targetConcurrency"));
        plan.put("riskLevel", assessmentResult.get("riskLevel"));
        plan.put("workflow", "bi-release-evaluation");
        return plan;
    }
}
