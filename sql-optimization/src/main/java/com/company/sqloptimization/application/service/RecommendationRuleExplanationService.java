package com.company.sqloptimization.application.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class RecommendationRuleExplanationService {

    private RecommendationRuleExplanationService() {
    }

    public static Map<String, Object> explain(String rule) {
        LinkedHashMap<String, Object> explanation = new LinkedHashMap<String, Object>();
        String normalizedRule = normalize(rule);
        if (!StringUtils.hasText(normalizedRule)) {
            return explanation;
        }
        if ("PRECOMPUTE_MV".equals(normalizedRule)) {
            explanation.put("titleZh", "物化视图预计算");
            explanation.put("triggerZh", "检测到聚合函数或 GROUP BY，存在重复计算可能。");
            explanation.put("actionZh", "建议生成物化视图，并给出刷新、验证、回滚 SQL。");
            explanation.put("riskZh", "需要确认刷新周期、数据新鲜度、存储成本和等价性。");
            explanation.put("whyNotAutoApplyZh", "当前缺少运行时频次和元数据证明，不能自动投产。");
            return explanation;
        }
        if ("PARTITION_PRUNING".equals(normalizedRule)) {
            explanation.put("titleZh", "分区裁剪");
            explanation.put("triggerZh", "检测到时间或业务键过滤，存在按分区减少扫描的机会。");
            explanation.put("actionZh", "建议核对分区键、分区元数据和过滤条件，必要时调整分区过滤写法。");
            explanation.put("riskZh", "分区布局和数据新鲜度不清时，静态解析不能证明真实裁剪收益。");
            explanation.put("whyNotAutoApplyZh", "当前缺少分区元数据和真实执行计划证据。");
            return explanation;
        }
        if ("BUCKET_JOIN".equals(normalizedRule)) {
            explanation.put("titleZh", "分桶 Join 协同");
            explanation.put("triggerZh", "检测到 Join 查询，存在按 Join key 对齐分桶或共置的机会。");
            explanation.put("actionZh", "建议评审 Join key 稳定性、数据倾斜和外部存储布局。");
            explanation.put("riskZh", "错误分桶会增加写入、存储和重分布成本。");
            explanation.put("whyNotAutoApplyZh", "分桶属于外部物理布局变更，必须由存储 owner 确认。");
            return explanation;
        }
        if ("RESULT_CACHE".equals(normalizedRule)) {
            explanation.put("titleZh", "结果缓存");
            explanation.put("triggerZh", "检测到低变更或高复用查询形态，存在结果复用机会。");
            explanation.put("actionZh", "建议定义缓存 TTL、新鲜度边界和失效条件。");
            explanation.put("riskZh", "缓存失效策略不清会导致陈旧结果。");
            explanation.put("whyNotAutoApplyZh", "当前缺少复用频次和新鲜度 SLA 证据。");
            return explanation;
        }
        if ("TOPN_PUSHDOWN".equals(normalizedRule)) {
            explanation.put("titleZh", "Top-N 下推");
            explanation.put("triggerZh", "检测到 ORDER BY + LIMIT，存在将排序限制下推到扫描侧的机会。");
            explanation.put("actionZh", "建议核对排序键、connector 支持和结果排序稳定性。");
            explanation.put("riskZh", "排序稳定性或 connector 能力不足时可能影响结果顺序和收益。");
            explanation.put("whyNotAutoApplyZh", "当前缺少 connector 下推能力和执行计划证据。");
            return explanation;
        }
        if ("COUNT_ONE_TO_COUNT_STAR".equals(normalizedRule)) {
            explanation.put("titleZh", "COUNT 写法标准化");
            explanation.put("triggerZh", "检测到 COUNT(1) 等非空字面量计数。");
            explanation.put("actionZh", "建议改写为 COUNT(*)，让语义更清晰。");
            explanation.put("riskZh", "低风险，仍需按治理流程保留验证证据。");
            explanation.put("whyNotAutoApplyZh", "只有验证通过后才允许进入自动应用路径。");
            return explanation;
        }
        if ("DUPLICATE_GROUP_ORDER_KEY".equals(normalizedRule)) {
            explanation.put("titleZh", "重复分组/排序键去重");
            explanation.put("triggerZh", "检测到 GROUP BY 或 ORDER BY 中重复字段。");
            explanation.put("actionZh", "建议保留首次出现顺序并移除重复键。");
            explanation.put("riskZh", "低风险，仍需确认排序和聚合语义未变化。");
            explanation.put("whyNotAutoApplyZh", "需要结果 diff 证明改写等价。");
            return explanation;
        }
        explanation.put("titleZh", normalizedRule);
        explanation.put("triggerZh", "当前规则仅有工程证据，尚未配置专门中文触发说明。");
        explanation.put("actionZh", "请结合规则证据、前置条件和风险说明人工复核。");
        explanation.put("riskZh", "静态规则不能单独证明生产收益或语义等价。");
        explanation.put("whyNotAutoApplyZh", "当前缺少自动应用所需的验证证据。");
        return explanation;
    }

    public static void enrich(Map<String, Object> target, String rule) {
        if (target == null) {
            return;
        }
        target.putAll(explain(rule));
    }

    public static String statusZh(String status) {
        String normalizedStatus = normalize(status);
        if ("PULL_ONLY_CANDIDATE".equals(normalizedStatus)) {
            return "仅候选，需外部协同，不自动执行";
        }
        if ("APPLIED_TO_CANDIDATE_SQL".equals(normalizedStatus)) {
            return "已应用到候选 SQL";
        }
        if ("NOT_APPLIED".equals(normalizedStatus)) {
            return "未应用，等待证据或人工复核";
        }
        if ("RISK_ONLY".equals(normalizedStatus)) {
            return "仅风险提示";
        }
        return status;
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }
}
