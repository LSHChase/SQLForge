package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlPressurePlanRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SqlScenarioBlueprintService {

    private final SqlPressurePlanService sqlPressurePlanService;

    public SqlScenarioBlueprintService(SqlPressurePlanService sqlPressurePlanService) {
        this.sqlPressurePlanService = sqlPressurePlanService;
    }

    public Map<String, Object> buildBlueprint(SqlPressurePlanRequest request) {
        Map<String, Object> pressurePlan = sqlPressurePlanService.buildPlan(request);
        Map<String, Object> executionMatrix = castMap(pressurePlan.get("executionMatrix"));
        Map<String, Object> candidatePack = castMap(pressurePlan.get("candidatePack"));
        List<Integer> ladder = castIntList(executionMatrix.get("concurrencyLadder"));

        Map<String, Object> blueprint = new LinkedHashMap<String, Object>();
        blueprint.put("batchId", pressurePlan.get("batchId"));
        blueprint.put("source", pressurePlan.get("source"));
        blueprint.put("analysisMode", "pure-structure-scenario-blueprint");
        blueprint.put("parsedStatementCount", pressurePlan.get("parsedStatementCount"));
        blueprint.put("pressurePlan", pressurePlan);
        blueprint.put("stages", buildStages(candidatePack, executionMatrix, ladder));
        blueprint.put("workloadMix", buildWorkloadMix(castList(pressurePlan.get("cohorts"))));
        blueprint.put("operatorChecklist", buildOperatorChecklist(executionMatrix, castMap(pressurePlan.get("pressureSummary"))));
        return blueprint;
    }

    private List<Map<String, Object>> buildStages(
        Map<String, Object> candidatePack,
        Map<String, Object> executionMatrix,
        List<Integer> ladder
    ) {
        List<Map<String, Object>> stages = new ArrayList<Map<String, Object>>();

        stages.add(stage(
            "smoke-baseline",
            "快速验证基础连通性与轻负载行为",
            castList(candidatePack.get("smokeSet")),
            sliceLadder(ladder, 0, Math.min(3, ladder.size())),
            Arrays.asList("sample"),
            Arrays.asList("uniform-baseline")
        ));

        stages.add(stage(
            "class-validation",
            "按 load class 验证结构分层与资源敏感度",
            castList(candidatePack.get("standardSet")),
            sliceLadder(ladder, 1, Math.min(4, ladder.size())),
            Arrays.asList("sample", "medium"),
            castStringList(executionMatrix.get("distributionMatrix"))
        ));

        stages.add(stage(
            "heavy-breakpoint",
            "对高复杂度 SQL 逼近吞吐和延迟拐点",
            castList(candidatePack.get("heavySet")),
            sliceLadder(ladder, Math.max(0, ladder.size() - 3), ladder.size()),
            Arrays.asList("medium", "full"),
            Arrays.asList("production-like", "skew-aware", "hot-key-check")
        ));

        stages.add(stage(
            "mixed-workload",
            "混合回放代表性 SQL 组合，观察队列与资源放大",
            castList(candidatePack.get("standardSet")),
            ladder,
            Arrays.asList("medium", "full"),
            Arrays.asList("production-like")
        ));

        return stages;
    }

    private Map<String, Object> stage(
        String stageId,
        String goal,
        List<Map<String, Object>> statements,
        List<Integer> concurrencyRange,
        List<String> dataScales,
        List<String> distributions
    ) {
        return mapOf(
            "stageId",
            stageId,
            "goal",
            goal,
            "statementIds",
            statementIds(statements),
            "concurrencyRange",
            concurrencyRange,
            "dataScales",
            dataScales,
            "distributions",
            distributions
        );
    }

    private List<Map<String, Object>> buildWorkloadMix(List<Map<String, Object>> cohorts) {
        List<Map<String, Object>> mix = new ArrayList<Map<String, Object>>();
        int total = 0;

        for (Map<String, Object> cohort : cohorts) {
            total += asInt(cohort.get("statementCount"), 0);
        }

        for (Map<String, Object> cohort : cohorts) {
            int count = asInt(cohort.get("statementCount"), 0);
            double ratio = total == 0 ? 0 : ((double) count) / total;
            mix.add(mapOf(
                "loadClass",
                cohort.get("loadClass"),
                "statementCount",
                Integer.valueOf(count),
                "trafficRatio",
                round(ratio),
                "preferredSet",
                count <= 2 ? "smokeSet" : count <= 5 ? "standardSet" : "heavySet"
            ));
        }

        return mix;
    }

    private List<String> buildOperatorChecklist(Map<String, Object> executionMatrix, Map<String, Object> pressureSummary) {
        List<String> checklist = new ArrayList<String>();
        checklist.add("先执行 smoke-baseline，确认基础延迟与错误率可控");
        checklist.add("按 class-validation 核对 load class 是否与资源行为匹配");
        checklist.add("在 heavy-breakpoint 阶段观察 " + castMap(executionMatrix.get("turningPointRule")).get("method"));
        checklist.add("混合回放前确认候选集覆盖 load classes: " + String.join(", ", castStringList(pressureSummary.get("loadClasses"))));
        checklist.add("若 dominant signals 包含 full-scan 或 wide-result，优先监控 IO 与返回集规模");
        return checklist;
    }

    private List<String> statementIds(List<Map<String, Object>> statements) {
        List<String> ids = new ArrayList<String>();
        for (Map<String, Object> statement : statements) {
            ids.add(String.valueOf(statement.get("statementId")));
        }
        return ids;
    }

    private List<Integer> sliceLadder(List<Integer> ladder, int fromInclusive, int toExclusive) {
        if (ladder.isEmpty()) {
            return new ArrayList<Integer>();
        }
        int start = Math.max(0, Math.min(fromInclusive, ladder.size()));
        int end = Math.max(start, Math.min(toExclusive, ladder.size()));
        return new ArrayList<Integer>(ladder.subList(start, end));
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

    private double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
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
