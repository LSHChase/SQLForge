package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlPressurePlanRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SqlExecutionManifestService {

    private final SqlScenarioBlueprintService sqlScenarioBlueprintService;

    public SqlExecutionManifestService(SqlScenarioBlueprintService sqlScenarioBlueprintService) {
        this.sqlScenarioBlueprintService = sqlScenarioBlueprintService;
    }

    public Map<String, Object> buildManifest(SqlPressurePlanRequest request) {
        Map<String, Object> blueprint = sqlScenarioBlueprintService.buildBlueprint(request);
        List<Map<String, Object>> stages = castList(blueprint.get("stages"));
        List<Map<String, Object>> manifestStages = new ArrayList<Map<String, Object>>();

        for (int index = 0; index < stages.size(); index += 1) {
            Map<String, Object> stage = stages.get(index);
            manifestStages.add(buildManifestStage(index + 1, stage));
        }

        Map<String, Object> pressurePlan = castMap(blueprint.get("pressurePlan"));
        Map<String, Object> executionMatrix = castMap(pressurePlan.get("executionMatrix"));

        Map<String, Object> manifest = new LinkedHashMap<String, Object>();
        manifest.put("manifestVersion", "2026-04-14");
        manifest.put("batchId", blueprint.get("batchId"));
        manifest.put("source", blueprint.get("source"));
        manifest.put("analysisMode", "pure-structure-execution-manifest");
        manifest.put("parsedStatementCount", blueprint.get("parsedStatementCount"));
        manifest.put("scenarioBlueprint", blueprint);
        manifest.put("stages", manifestStages);
        manifest.put("globalGuardrails", buildGlobalGuardrails(executionMatrix));
        manifest.put("artifactSummary", buildArtifactSummary(pressurePlan));
        return manifest;
    }

    private Map<String, Object> buildManifestStage(int order, Map<String, Object> stage) {
        List<Integer> concurrencyRange = castIntList(stage.get("concurrencyRange"));
        List<String> statementIds = castStringList(stage.get("statementIds"));
        String stageId = String.valueOf(stage.get("stageId"));

        return mapOf(
            "order",
            Integer.valueOf(order),
            "stageId",
            stageId,
            "goal",
            stage.get("goal"),
            "workloadMode",
            resolveWorkloadMode(stageId),
            "statementIds",
            statementIds,
            "concurrencyRange",
            concurrencyRange,
            "entryCriteria",
            buildEntryCriteria(stageId, concurrencyRange),
            "exitCriteria",
            buildExitCriteria(stageId),
            "metricFocus",
            buildMetricFocus(stageId),
            "runnerHints",
            buildRunnerHints(stageId, statementIds.size())
        );
    }

    private String resolveWorkloadMode(String stageId) {
        if ("smoke-baseline".equals(stageId)) {
            return "single-or-light-mix";
        }
        if ("class-validation".equals(stageId)) {
            return "class-grouped";
        }
        if ("heavy-breakpoint".equals(stageId)) {
            return "heavy-priority";
        }
        return "mixed-replay";
    }

    private List<String> buildEntryCriteria(String stageId, List<Integer> concurrencyRange) {
        List<String> criteria = new ArrayList<String>();
        criteria.add("目标阶段的候选 SQL 已完成结构分层确认");
        criteria.add("准备使用的并发档位: " + joinInts(concurrencyRange));

        if ("smoke-baseline".equals(stageId)) {
            criteria.add("基础链路无明显错误，且轻负载响应可观测");
        } else if ("heavy-breakpoint".equals(stageId)) {
            criteria.add("已完成 smoke 和 class-validation，允许逼近吞吐拐点");
        } else {
            criteria.add("前置阶段已完成，允许进入分层或混合回放");
        }

        return criteria;
    }

    private List<String> buildExitCriteria(String stageId) {
        if ("smoke-baseline".equals(stageId)) {
            return Arrays.asList("错误率维持低位", "基础延迟曲线稳定");
        }
        if ("class-validation".equals(stageId)) {
            return Arrays.asList("load class 与资源行为映射清晰", "主要瓶颈类型可归因");
        }
        if ("heavy-breakpoint".equals(stageId)) {
            return Arrays.asList("识别吞吐或延迟拐点", "记录高复杂 SQL 的上限区间");
        }
        return Arrays.asList("混合 workload 的队列与资源放大规律可复述", "候选集覆盖关键 load class");
    }

    private List<String> buildMetricFocus(String stageId) {
        if ("smoke-baseline".equals(stageId)) {
            return Arrays.asList("p50-latency", "error-rate", "basic-throughput");
        }
        if ("class-validation".equals(stageId)) {
            return Arrays.asList("p95-latency", "cpu-usage", "shuffle-or-io-share");
        }
        if ("heavy-breakpoint".equals(stageId)) {
            return Arrays.asList("p99-latency", "queue-depth", "resource-saturation");
        }
        return Arrays.asList("mixed-throughput", "queue-growth", "cross-class-interference");
    }

    private List<String> buildRunnerHints(String stageId, int statementCount) {
        List<String> hints = new ArrayList<String>();
        hints.add("阶段内语句数: " + statementCount);
        hints.add("每个并发档位先 warmup 再采样");

        if ("heavy-breakpoint".equals(stageId)) {
            hints.add("逐级提升并发，避免一次性跳到最高档");
        }
        if ("mixed-workload".equals(stageId)) {
            hints.add("按 workload mix 比例轮转候选语句");
        }
        return hints;
    }

    private List<String> buildGlobalGuardrails(Map<String, Object> executionMatrix) {
        List<String> guardrails = new ArrayList<String>();
        guardrails.add("所有阶段均基于纯结构分析产物，不代表真实数据库执行结论");
        guardrails.add("阶段推进遵守 " + castMap(executionMatrix.get("turningPointRule")).get("method"));
        guardrails.add("先低并发后高并发，先单类后混合");
        guardrails.add("高复杂度 SQL 默认保留在 heavySet 或后续阶段");
        return guardrails;
    }

    private Map<String, Object> buildArtifactSummary(Map<String, Object> pressurePlan) {
        Map<String, Object> pressureSummary = castMap(pressurePlan.get("pressureSummary"));
        Map<String, Object> candidatePack = castMap(pressurePlan.get("candidatePack"));

        return mapOf(
            "loadClasses",
            pressureSummary.get("loadClasses"),
            "dominantSignals",
            pressureSummary.get("dominantSignals"),
            "smokeSetSize",
            Integer.valueOf(castList(candidatePack.get("smokeSet")).size()),
            "standardSetSize",
            Integer.valueOf(castList(candidatePack.get("standardSet")).size()),
            "heavySetSize",
            Integer.valueOf(castList(candidatePack.get("heavySet")).size())
        );
    }

    private String joinInts(List<Integer> values) {
        List<String> parts = new ArrayList<String>();
        for (Integer value : values) {
            parts.add(String.valueOf(value));
        }
        return String.join(", ", parts);
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
    private List<Integer> castIntList(Object value) {
        return value == null ? new ArrayList<Integer>() : (List<Integer>) value;
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
