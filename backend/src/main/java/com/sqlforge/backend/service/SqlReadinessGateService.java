package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlPressurePlanRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SqlReadinessGateService {

    private final SqlBriefingReportService sqlBriefingReportService;

    public SqlReadinessGateService(SqlBriefingReportService sqlBriefingReportService) {
        this.sqlBriefingReportService = sqlBriefingReportService;
    }

    public Map<String, Object> evaluate(SqlPressurePlanRequest request) {
        Map<String, Object> briefing = sqlBriefingReportService.buildReport(request);
        Map<String, Object> runPackage = castMap(briefing.get("runPackage"));
        Map<String, Object> campaignSchedule = castMap(runPackage.get("campaignSchedule"));
        Map<String, Object> campaignSummary = castMap(campaignSchedule.get("campaignSummary"));
        Map<String, Object> executionManifest = castMap(castMap(campaignSchedule.get("executionManifest")));
        Map<String, Object> artifactSummary = castMap(executionManifest.get("artifactSummary"));

        List<Map<String, Object>> blockers = buildBlockers(artifactSummary, campaignSummary);
        List<String> prerequisites = buildPrerequisites(campaignSchedule, runPackage);
        List<String> recommendations = buildRecommendations(briefing, blockers);
        String decision = resolveDecision(blockers, artifactSummary, campaignSummary);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("gateVersion", "2026-04-14");
        result.put("analysisMode", "pure-structure-readiness-gate");
        result.put("batchId", briefing.get("batchId"));
        result.put("source", briefing.get("source"));
        result.put("parsedStatementCount", briefing.get("parsedStatementCount"));
        result.put("decision", decision);
        result.put("blockers", blockers);
        result.put("prerequisites", prerequisites);
        result.put("recommendations", recommendations);
        result.put("briefingReport", briefing);
        return result;
    }

    private List<Map<String, Object>> buildBlockers(Map<String, Object> artifactSummary, Map<String, Object> campaignSummary) {
        List<Map<String, Object>> blockers = new ArrayList<Map<String, Object>>();
        List<String> signals = castStringList(artifactSummary.get("dominantSignals"));
        int heavySetSize = asInt(artifactSummary.get("heavySetSize"), 0);
        int totalDuration = asInt(campaignSummary.get("totalDurationMinutes"), 0);
        int maxConcurrency = asInt(campaignSummary.get("maxConcurrencyTarget"), 0);

        if (signals.contains("full-scan")) {
            blockers.add(blocker("full-scan", "high", "存在全表扫描信号，需先确认样本与阶段安排是否可控"));
        }
        if (signals.contains("wide-result")) {
            blockers.add(blocker("wide-result", "high", "存在宽结果集信号，需先明确结果控制策略"));
        }
        if (heavySetSize >= 6) {
            blockers.add(blocker("heavy-set-density", "high", "高复杂度候选集较大，建议先压缩 heavySet 再执行"));
        }
        if (totalDuration > 90) {
            blockers.add(blocker("campaign-duration", "medium", "阶段总时长较长，需确认执行窗口是否足够"));
        }
        if (maxConcurrency >= 100) {
            blockers.add(blocker("max-concurrency", "medium", "最高目标并发较高，需确认推进节奏与回退策略"));
        }

        return blockers;
    }

    private Map<String, Object> blocker(String code, String severity, String message) {
        return mapOf("code", code, "severity", severity, "message", message);
    }

    private List<String> buildPrerequisites(Map<String, Object> campaignSchedule, Map<String, Object> runPackage) {
        List<String> prerequisites = new ArrayList<String>();
        prerequisites.add("推荐输出文件已按批次命名: " + castList(runPackage.get("recommendedFiles")).size() + " 个");
        prerequisites.add("campaign totalDurationMinutes = " + castMap(campaignSchedule.get("campaignSummary")).get("totalDurationMinutes"));
        prerequisites.add("执行侧已理解 handoff notes 与 fallback action");
        prerequisites.add("promotion gate 需逐阶段人工确认");
        return prerequisites;
    }

    private List<String> buildRecommendations(Map<String, Object> briefing, List<Map<String, Object>> blockers) {
        List<String> recommendations = new ArrayList<String>();
        recommendations.add("先复核 executive summary 与 risk focus");
        recommendations.add("对 high blocker 优先做结构侧缩减或阶段拆分");
        recommendations.add("将 review agenda 作为执行前评审清单");

        if (blockers.isEmpty()) {
            recommendations.add("当前结构侧可进入执行准备，继续按 campaign schedule 推进");
        } else {
            recommendations.add("在 blocker 清零或降级前，不要直接进入重负载阶段");
        }

        return recommendations;
    }

    private String resolveDecision(List<Map<String, Object>> blockers, Map<String, Object> artifactSummary, Map<String, Object> campaignSummary) {
        int highBlockers = 0;
        for (Map<String, Object> blocker : blockers) {
            if ("high".equals(String.valueOf(blocker.get("severity")))) {
                highBlockers += 1;
            }
        }

        if (highBlockers >= 2) {
            return "blocked";
        }
        if (!blockers.isEmpty()
            || asInt(campaignSummary.get("totalDurationMinutes"), 0) > 75
            || asInt(artifactSummary.get("heavySetSize"), 0) >= 4) {
            return "caution";
        }
        return "ready";
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value == null ? new LinkedHashMap<String, Object>() : (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return value == null ? new ArrayList<Map<String, Object>>() : (List<Map<String, Object>>) value;
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
