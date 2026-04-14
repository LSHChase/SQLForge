package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.PlanStabilityRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PlanStabilityWorkflowService {

    private final SqlAssessmentService sqlAssessmentService;

    public PlanStabilityWorkflowService(SqlAssessmentService sqlAssessmentService) {
        this.sqlAssessmentService = sqlAssessmentService;
    }

    public Map<String, Object> execute(PlanStabilityRequest request) {
        Map<String, Object> sqlAssessment = sqlAssessmentService.assess(
            request.getTenantId(),
            request.getSql(),
            request.getSlaMs(),
            request.getTargetConcurrency()
        );
        Map<String, Object> assessment = castMap(sqlAssessment.get("assessment"));
        String fingerprint = String.valueOf(assessment.get("fingerprint"));
        Map<String, Object> stabilityAnalysis = analyze(
            fingerprint,
            normalizePlan(request.getHistoricalBestPlan()),
            normalizePlan(request.getCurrentPlan()),
            normalizePlans(request.getCandidatePlans())
        );
        Map<String, Object> report = buildPlanStabilityReport(fingerprint, stabilityAnalysis, assessment);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("workflow", "plan-stability-analysis");
        result.put("sqlAssessment", sqlAssessment);
        result.put("stabilityAnalysis", stabilityAnalysis);
        result.put("report", report);
        return result;
    }

    private Map<String, Object> analyze(
        String fingerprint,
        Map<String, Object> historicalBestPlan,
        Map<String, Object> currentPlan,
        List<Map<String, Object>> candidatePlans
    ) {
        if (currentPlan == null || currentPlan.get("planHash") == null || String.valueOf(currentPlan.get("planHash")).trim().isEmpty()) {
            throw new IllegalArgumentException("currentPlan.planHash is required");
        }

        Map<String, Object> bestPlan = historicalBestPlan == null ? currentPlan : historicalBestPlan;
        double bestLatency = asDouble(bestPlan.get("latencyMs"), 0);
        double currentLatency = asDouble(currentPlan.get("latencyMs"), 0);
        double latencyRegressionRatio = bestLatency > 0 ? (currentLatency - bestLatency) / bestLatency : 0;

        Map<String, Object> diff = mapOf(
            "fingerprint",
            fingerprint,
            "bestPlanHash",
            bestPlan.get("planHash"),
            "currentPlanHash",
            currentPlan.get("planHash"),
            "planHashChanged",
            Boolean.valueOf(!String.valueOf(bestPlan.get("planHash")).equals(String.valueOf(currentPlan.get("planHash")))),
            "joinOrderChanged",
            Boolean.valueOf(!arraysEqual(castStringList(bestPlan.get("joinOrder")), castStringList(currentPlan.get("joinOrder")))),
            "distributionChanged",
            Boolean.valueOf(!String.valueOf(bestPlan.get("distribution")).equals(String.valueOf(currentPlan.get("distribution")))),
            "statsAgeRegressionHours",
            Double.valueOf(round(asDouble(currentPlan.get("statsAgeHours"), 0) - asDouble(bestPlan.get("statsAgeHours"), 0))),
            "latencyRegressionRatio",
            Double.valueOf(round(latencyRegressionRatio))
        );

        List<String> rootCauses = new ArrayList<String>();

        if (asDouble(diff.get("statsAgeRegressionHours"), 0) >= 24) {
            rootCauses.add("stale-stats");
        }

        if (Boolean.TRUE.equals(diff.get("joinOrderChanged")) || Boolean.TRUE.equals(diff.get("distributionChanged"))) {
            rootCauses.add("plan-drift");
        }

        if (hasBetterCandidate(candidatePlans, currentLatency)) {
            rootCauses.add("better-candidate-exists");
        }

        String decision = "observe";
        String message = "未检测到需要立即干预的计划稳定性风险";

        if (latencyRegressionRatio > 0.2 && rootCauses.contains("stale-stats")) {
            decision = "refresh-stats";
            message = "当前计划明显劣化且统计信息更旧，应优先刷新统计信息并重新验证";
        } else if (latencyRegressionRatio > 0.2 && rootCauses.contains("plan-drift")) {
            decision = "protect-best-plan";
            message = "当前计划相对历史最优计划明显退化，建议优先保护已知更优计划";
        } else if (latencyRegressionRatio > 0.1) {
            decision = "investigate";
            message = "存在中度延迟劣化，需要进一步对比数据分布和候选计划";
        }

        return mapOf(
            "diff",
            diff,
            "rootCauses",
            rootCauses,
            "decision",
            decision,
            "message",
            message,
            "recommendedActions",
            recommendedActions(decision)
        );
    }

    private Map<String, Object> buildPlanStabilityReport(
        String fingerprint,
        Map<String, Object> stabilityAnalysis,
        Map<String, Object> assessment
    ) {
        return mapOf(
            "reportType",
            "plan-stability",
            "fingerprint",
            fingerprint,
            "summary",
            mapOf(
                "decision",
                stabilityAnalysis.get("decision"),
                "message",
                stabilityAnalysis.get("message")
            ),
            "assessment",
            assessment,
            "stabilityAnalysis",
            stabilityAnalysis
        );
    }

    private List<String> recommendedActions(String decision) {
        if ("refresh-stats".equals(decision)) {
            return java.util.Arrays.asList("触发 ANALYZE", "建立关键表统计信息新鲜度 SLA");
        }
        if ("protect-best-plan".equals(decision)) {
            return java.util.Arrays.asList("锁定较优 Hint 或白名单策略", "监控计划 Hash 变化");
        }
        if ("investigate".equals(decision)) {
            return java.util.Arrays.asList("回放候选计划", "检查数据分布与 CBO 参数");
        }
        return java.util.Arrays.asList("继续监控计划 Hash 和 P99");
    }

    private boolean hasBetterCandidate(List<Map<String, Object>> candidatePlans, double currentLatency) {
        for (Map<String, Object> plan : candidatePlans) {
            if (asDouble(plan.get("latencyMs"), Double.POSITIVE_INFINITY) < currentLatency) {
                return true;
            }
        }
        return false;
    }

    private boolean arraysEqual(List<String> left, List<String> right) {
        if (left.size() != right.size()) {
            return false;
        }

        for (int index = 0; index < left.size(); index += 1) {
            if (!String.valueOf(left.get(index)).equals(String.valueOf(right.get(index)))) {
                return false;
            }
        }

        return true;
    }

    private List<Map<String, Object>> normalizePlans(List<PlanStabilityRequest.PlanSummary> plans) {
        List<Map<String, Object>> normalized = new ArrayList<Map<String, Object>>();
        if (plans == null) {
            return normalized;
        }

        for (PlanStabilityRequest.PlanSummary plan : plans) {
            normalized.add(normalizePlan(plan));
        }
        return normalized;
    }

    private Map<String, Object> normalizePlan(PlanStabilityRequest.PlanSummary plan) {
        if (plan == null) {
            return null;
        }

        return mapOf(
            "planHash",
            plan.getPlanHash(),
            "latencyMs",
            plan.getLatencyMs(),
            "distribution",
            plan.getDistribution() == null ? "unknown" : plan.getDistribution(),
            "statsAgeHours",
            plan.getStatsAgeHours(),
            "joinOrder",
            plan.getJoinOrder() == null ? new ArrayList<String>() : plan.getJoinOrder()
        );
    }

    private double asDouble(Object value, double fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private double round(double value) {
        return Math.round(value * 10000.0d) / 10000.0d;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value == null ? new LinkedHashMap<String, Object>() : (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object value) {
        return value == null ? new ArrayList<String>() : (List<String>) value;
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return result;
    }
}
