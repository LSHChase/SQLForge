package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlPressurePlanRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SqlRunPackageService {

    private final SqlCampaignScheduleService sqlCampaignScheduleService;

    public SqlRunPackageService(SqlCampaignScheduleService sqlCampaignScheduleService) {
        this.sqlCampaignScheduleService = sqlCampaignScheduleService;
    }

    public Map<String, Object> buildPackage(SqlPressurePlanRequest request) {
        Map<String, Object> schedule = sqlCampaignScheduleService.buildSchedule(request);
        Map<String, Object> manifest = castMap(schedule.get("executionManifest"));
        Map<String, Object> blueprint = castMap(manifest.get("scenarioBlueprint"));
        Map<String, Object> pressurePlan = castMap(blueprint.get("pressurePlan"));
        Map<String, Object> analysis = castMap(pressurePlan.get("analysis"));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("packageVersion", "2026-04-14");
        result.put("analysisMode", "pure-structure-run-package");
        result.put("batchId", schedule.get("batchId"));
        result.put("source", schedule.get("source"));
        result.put("parsedStatementCount", schedule.get("parsedStatementCount"));
        result.put("artifacts", buildArtifacts(schedule, manifest, blueprint, pressurePlan, analysis));
        result.put("recommendedFiles", buildRecommendedFiles(String.valueOf(schedule.get("batchId"))));
        result.put("exportSections", buildExportSections(schedule, manifest, blueprint, pressurePlan));
        result.put("handoffChecklist", buildHandoffChecklist(schedule, manifest));
        result.put("campaignSchedule", schedule);
        return result;
    }

    private List<Map<String, Object>> buildArtifacts(
        Map<String, Object> schedule,
        Map<String, Object> manifest,
        Map<String, Object> blueprint,
        Map<String, Object> pressurePlan,
        Map<String, Object> analysis
    ) {
        List<Map<String, Object>> artifacts = new ArrayList<Map<String, Object>>();
        artifacts.add(artifact("analysis", "sql-intent-analysis.json", analysis));
        artifacts.add(artifact("pressurePlan", "sql-pressure-plan.json", pressurePlan));
        artifacts.add(artifact("scenarioBlueprint", "sql-scenario-blueprint.json", blueprint));
        artifacts.add(artifact("executionManifest", "sql-execution-manifest.json", manifest));
        artifacts.add(artifact("campaignSchedule", "sql-campaign-schedule.json", schedule));
        return artifacts;
    }

    private Map<String, Object> artifact(String name, String suggestedFileName, Map<String, Object> payload) {
        return mapOf(
            "name",
            name,
            "suggestedFileName",
            suggestedFileName,
            "topLevelKeys",
            new ArrayList<String>(payload.keySet())
        );
    }

    private List<Map<String, Object>> buildRecommendedFiles(String batchId) {
        String prefix = batchId == null || batchId.trim().isEmpty() ? "sql-run-package" : batchId.trim();
        List<Map<String, Object>> files = new ArrayList<Map<String, Object>>();
        files.add(file(prefix + "-analysis.json", "json", "基础结构分析结果"));
        files.add(file(prefix + "-pressure-plan.json", "json", "压测分组与候选集"));
        files.add(file(prefix + "-scenario-blueprint.json", "json", "阶段化压测蓝图"));
        files.add(file(prefix + "-execution-manifest.json", "json", "执行清单与阶段条件"));
        files.add(file(prefix + "-campaign-schedule.json", "json", "阶段时间表与推进门槛"));
        return files;
    }

    private Map<String, Object> file(String fileName, String format, String purpose) {
        return mapOf("fileName", fileName, "format", format, "purpose", purpose);
    }

    private List<Map<String, Object>> buildExportSections(
        Map<String, Object> schedule,
        Map<String, Object> manifest,
        Map<String, Object> blueprint,
        Map<String, Object> pressurePlan
    ) {
        List<Map<String, Object>> sections = new ArrayList<Map<String, Object>>();
        sections.add(section("summary", mapOf(
            "loadClasses",
            castMap(pressurePlan.get("pressureSummary")).get("loadClasses"),
            "stageCount",
            castMap(schedule.get("campaignSummary")).get("stageCount"),
            "totalDurationMinutes",
            castMap(schedule.get("campaignSummary")).get("totalDurationMinutes")
        )));
        sections.add(section("stages", mapOf("stages", schedule.get("stages"))));
        sections.add(section("guardrails", mapOf(
            "globalGuardrails",
            manifest.get("globalGuardrails"),
            "operatorChecklist",
            blueprint.get("operatorChecklist"),
            "handoffNotes",
            schedule.get("handoffNotes")
        )));
        return sections;
    }

    private Map<String, Object> section(String name, Map<String, Object> payload) {
        return mapOf("name", name, "payload", payload);
    }

    private List<String> buildHandoffChecklist(Map<String, Object> schedule, Map<String, Object> manifest) {
        List<String> checklist = new ArrayList<String>();
        checklist.add("确认 recommendedFiles 已按批次落盘");
        checklist.add("确认 stages 与 manifest 顺序一致");
        checklist.add("确认 global guardrails 已同步给执行侧");
        checklist.add("确认 campaign totalDurationMinutes = " + castMap(schedule.get("campaignSummary")).get("totalDurationMinutes"));
        checklist.add("确认 artifactSummary 已覆盖 smoke/standard/heavy 三类候选集: " + castMap(manifest.get("artifactSummary")).get("heavySetSize"));
        return checklist;
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
