package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlPressurePlanRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SqlBriefingReportService {

    private final SqlRunPackageService sqlRunPackageService;

    public SqlBriefingReportService(SqlRunPackageService sqlRunPackageService) {
        this.sqlRunPackageService = sqlRunPackageService;
    }

    public Map<String, Object> buildReport(SqlPressurePlanRequest request) {
        Map<String, Object> runPackage = sqlRunPackageService.buildPackage(request);
        Map<String, Object> campaignSchedule = castMap(runPackage.get("campaignSchedule"));
        Map<String, Object> campaignSummary = castMap(campaignSchedule.get("campaignSummary"));
        Map<String, Object> executionManifest = castMap(castMap(campaignSchedule.get("executionManifest")));
        Map<String, Object> artifactSummary = castMap(executionManifest.get("artifactSummary"));

        Map<String, Object> report = new LinkedHashMap<String, Object>();
        report.put("reportVersion", "2026-04-14");
        report.put("analysisMode", "pure-structure-briefing-report");
        report.put("batchId", runPackage.get("batchId"));
        report.put("source", runPackage.get("source"));
        report.put("parsedStatementCount", runPackage.get("parsedStatementCount"));
        report.put("executiveSummary", buildExecutiveSummary(campaignSummary, artifactSummary));
        report.put("riskFocus", buildRiskFocus(artifactSummary, campaignSummary));
        report.put("nextActions", buildNextActions(campaignSchedule, runPackage));
        report.put("reviewAgenda", buildReviewAgenda(campaignSchedule));
        report.put("runPackage", runPackage);
        return report;
    }

    private Map<String, Object> buildExecutiveSummary(Map<String, Object> campaignSummary, Map<String, Object> artifactSummary) {
        List<String> highlights = new ArrayList<String>();
        highlights.add("本批次覆盖 load classes: " + joinStrings(castStringList(artifactSummary.get("loadClasses"))));
        highlights.add("总排期时长约 " + campaignSummary.get("totalDurationMinutes") + " 分钟");
        highlights.add("最高目标并发 " + campaignSummary.get("maxConcurrencyTarget"));
        highlights.add("候选 heavySet 数量 " + artifactSummary.get("heavySetSize"));

        return mapOf(
            "headline",
            "SQL 压测准备包已可进入执行评审",
            "highlights",
            highlights
        );
    }

    private List<Map<String, Object>> buildRiskFocus(Map<String, Object> artifactSummary, Map<String, Object> campaignSummary) {
        List<Map<String, Object>> risks = new ArrayList<Map<String, Object>>();
        for (String signal : castStringList(artifactSummary.get("dominantSignals"))) {
            risks.add(mapOf(
                "signal",
                signal,
                "whyItMatters",
                explainSignal(signal),
                "reviewPriority",
                priorityForSignal(signal)
            ));
        }

        risks.add(mapOf(
            "signal",
            "campaign-duration",
            "whyItMatters",
            "阶段总时长决定了执行窗口和观测节奏",
            "reviewPriority",
            asInt(campaignSummary.get("totalDurationMinutes"), 0) > 60 ? "high" : "medium"
        ));
        return risks;
    }

    private List<Map<String, Object>> buildNextActions(Map<String, Object> campaignSchedule, Map<String, Object> runPackage) {
        List<Map<String, Object>> actions = new ArrayList<Map<String, Object>>();
        actions.add(mapOf(
            "title",
            "确认交付包落盘",
            "detail",
            "按建议文件名输出 artifact，并保证执行侧可访问",
            "ownerHint",
            "operator"
        ));
        actions.add(mapOf(
            "title",
            "按阶段 review promotion gate",
            "detail",
            "逐阶段确认是否满足推进门槛，尤其是 heavy-breakpoint 前置条件",
            "ownerHint",
            "reviewer"
        ));
        actions.add(mapOf(
            "title",
            "对齐 handoff checklist",
            "detail",
            "确认执行侧理解 guardrails、fallback action 和总时长",
            "ownerHint",
            "operator"
        ));
        actions.add(mapOf(
            "title",
            "准备导出文件",
            "detail",
            "建议导出文件数: " + castList(runPackage.get("recommendedFiles")).size(),
            "ownerHint",
            "tooling"
        ));
        return actions;
    }

    private List<Map<String, Object>> buildReviewAgenda(Map<String, Object> campaignSchedule) {
        List<Map<String, Object>> agenda = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> stages = castList(campaignSchedule.get("stages"));

        agenda.add(mapOf("topic", "执行窗口总览", "focus", castMap(campaignSchedule.get("campaignSummary")).get("totalDurationMinutes") + " 分钟排期"));
        for (Map<String, Object> stage : stages) {
            agenda.add(mapOf(
                "topic",
                String.valueOf(stage.get("stageId")),
                "focus",
                "promotion gate: " + joinStrings(castStringList(stage.get("promotionGate")))
            ));
        }
        agenda.add(mapOf("topic", "交接检查", "focus", "确认 handoff notes 与导出文件"));
        return agenda;
    }

    private String explainSignal(String signal) {
        if ("full-scan".equals(signal) || "wide-result".equals(signal)) {
            return "可能导致 IO、扫描量或结果集规模明显放大";
        }
        if ("window-compute".equals(signal) || "heavy-compute".equals(signal)) {
            return "可能导致 CPU 与内存峰值上升";
        }
        if ("multi-join".equals(signal) || "complex-join".equals(signal)) {
            return "可能导致网络放大与计划稳定性下降";
        }
        return "需要在执行前明确其对资源与阶段推进的影响";
    }

    private String priorityForSignal(String signal) {
        if ("full-scan".equals(signal) || "wide-result".equals(signal) || "multi-join".equals(signal)) {
            return "high";
        }
        return "medium";
    }

    private String joinStrings(List<String> values) {
        return values.isEmpty() ? "n/a" : String.join(", ", values);
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
    private List<Map<String, Object>> castList(Object value) {
        return value == null ? new ArrayList<Map<String, Object>>() : (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object value) {
        return value == null ? new ArrayList<String>() : (List<String>) value;
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
