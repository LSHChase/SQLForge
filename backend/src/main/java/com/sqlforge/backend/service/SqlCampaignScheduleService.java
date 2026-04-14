package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlPressurePlanRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SqlCampaignScheduleService {

    private final SqlExecutionManifestService sqlExecutionManifestService;

    public SqlCampaignScheduleService(SqlExecutionManifestService sqlExecutionManifestService) {
        this.sqlExecutionManifestService = sqlExecutionManifestService;
    }

    public Map<String, Object> buildSchedule(SqlPressurePlanRequest request) {
        Map<String, Object> manifest = sqlExecutionManifestService.buildManifest(request);
        List<Map<String, Object>> manifestStages = castList(manifest.get("stages"));
        List<Map<String, Object>> scheduledStages = new ArrayList<Map<String, Object>>();
        int startMinute = 0;

        for (Map<String, Object> stage : manifestStages) {
            Map<String, Object> schedule = buildStageSchedule(stage, startMinute);
            scheduledStages.add(schedule);
            startMinute = asInt(castMap(schedule.get("window")).get("endMinute"), startMinute);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("scheduleVersion", "2026-04-14");
        result.put("analysisMode", "pure-structure-campaign-schedule");
        result.put("batchId", manifest.get("batchId"));
        result.put("source", manifest.get("source"));
        result.put("parsedStatementCount", manifest.get("parsedStatementCount"));
        result.put("executionManifest", manifest);
        result.put("stages", scheduledStages);
        result.put("campaignSummary", buildCampaignSummary(scheduledStages, manifestStages));
        result.put("handoffNotes", buildHandoffNotes());
        return result;
    }

    private Map<String, Object> buildStageSchedule(Map<String, Object> stage, int startMinute) {
        String stageId = String.valueOf(stage.get("stageId"));
        int warmup = resolveWarmupMinutes(stageId);
        int sample = resolveSampleMinutes(stageId);
        int cooldown = resolveCooldownMinutes(stageId);
        int duration = warmup + sample + cooldown;

        return mapOf(
            "stageId",
            stageId,
            "order",
            stage.get("order"),
            "goal",
            stage.get("goal"),
            "workloadMode",
            stage.get("workloadMode"),
            "statementIds",
            stage.get("statementIds"),
            "concurrencyRange",
            stage.get("concurrencyRange"),
            "window",
            mapOf(
                "startMinute",
                Integer.valueOf(startMinute),
                "warmupMinutes",
                Integer.valueOf(warmup),
                "sampleMinutes",
                Integer.valueOf(sample),
                "cooldownMinutes",
                Integer.valueOf(cooldown),
                "endMinute",
                Integer.valueOf(startMinute + duration)
            ),
            "promotionGate",
            buildPromotionGate(stageId),
            "fallbackAction",
            buildFallbackAction(stageId),
            "metricFocus",
            stage.get("metricFocus")
        );
    }

    private int resolveWarmupMinutes(String stageId) {
        if ("heavy-breakpoint".equals(stageId)) {
            return 8;
        }
        if ("mixed-workload".equals(stageId)) {
            return 10;
        }
        return 5;
    }

    private int resolveSampleMinutes(String stageId) {
        if ("smoke-baseline".equals(stageId)) {
            return 12;
        }
        if ("class-validation".equals(stageId)) {
            return 18;
        }
        if ("heavy-breakpoint".equals(stageId)) {
            return 25;
        }
        return 20;
    }

    private int resolveCooldownMinutes(String stageId) {
        if ("heavy-breakpoint".equals(stageId)) {
            return 6;
        }
        return 4;
    }

    private List<String> buildPromotionGate(String stageId) {
        if ("smoke-baseline".equals(stageId)) {
            return java.util.Arrays.asList("错误率低于预期阈值", "基础吞吐与延迟可稳定采样");
        }
        if ("class-validation".equals(stageId)) {
            return java.util.Arrays.asList("不同 load class 的资源行为差异已识别", "阶段内无持续失控错误");
        }
        if ("heavy-breakpoint".equals(stageId)) {
            return java.util.Arrays.asList("已识别逼近拐点的并发区间", "高复杂 SQL 未出现不可恢复抖动");
        }
        return java.util.Arrays.asList("混合回放对核心资源影响已可解释", "队列增长与吞吐变化可复述");
    }

    private List<String> buildFallbackAction(String stageId) {
        if ("smoke-baseline".equals(stageId)) {
            return java.util.Arrays.asList("回退到最小并发档", "单条复验 smokeSet");
        }
        if ("heavy-breakpoint".equals(stageId)) {
            return java.util.Arrays.asList("回退到上一并发档", "仅保留 heavySet 中最高复杂度样本");
        }
        return java.util.Arrays.asList("缩小并发梯度", "减少同阶段语句数量");
    }

    private Map<String, Object> buildCampaignSummary(List<Map<String, Object>> scheduledStages, List<Map<String, Object>> manifestStages) {
        int totalMinutes = 0;
        for (Map<String, Object> stage : scheduledStages) {
            totalMinutes = Math.max(totalMinutes, asInt(castMap(stage.get("window")).get("endMinute"), totalMinutes));
        }

        return mapOf(
            "stageCount",
            Integer.valueOf(scheduledStages.size()),
            "totalDurationMinutes",
            Integer.valueOf(totalMinutes),
            "maxConcurrencyTarget",
            maxConcurrency(scheduledStages),
            "workloadModes",
            workloadModes(manifestStages)
        );
    }

    private List<String> buildHandoffNotes() {
        return java.util.Arrays.asList(
            "每个阶段先 warmup，再进入 sample 窗口",
            "promotion gate 未满足时不要继续推进到下一阶段",
            "fallback action 用于控制风险，而不是直接判定真实数据库上限",
            "当前 schedule 来自纯结构分析，需与真实执行观测结合使用"
        );
    }

    private int maxConcurrency(List<Map<String, Object>> scheduledStages) {
        int max = 0;
        for (Map<String, Object> stage : scheduledStages) {
            for (Integer value : castIntList(stage.get("concurrencyRange"))) {
                max = Math.max(max, value.intValue());
            }
        }
        return max;
    }

    private List<String> workloadModes(List<Map<String, Object>> manifestStages) {
        List<String> modes = new ArrayList<String>();
        for (Map<String, Object> stage : manifestStages) {
            String mode = String.valueOf(stage.get("workloadMode"));
            if (!modes.contains(mode)) {
                modes.add(mode);
            }
        }
        return modes;
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
