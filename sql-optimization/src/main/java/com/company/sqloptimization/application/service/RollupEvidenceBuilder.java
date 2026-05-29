package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RollupProfileValues.immutableMapList;
import static com.company.sqloptimization.application.service.RollupTimeExpressionParser.CALENDAR_POLICY;
import static com.company.sqloptimization.application.service.RollupTimeExpressionParser.MV_FINEST_GRAIN;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RollupEvidenceBuilder {

    private RollupEvidenceBuilder() {
    }

    static Map<String, Object> timeRollupEvidence(RollupPlan rollupPlan,
                                                  List<Map<String, Object>> blockingReasons) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("status", blockingReasons == null || blockingReasons.isEmpty() ? "GENERATED" : "BLOCKED");
        evidence.put("timeSourceColumn", rollupPlan == null ? "" : rollupPlan.sourceColumn);
        evidence.put("queryTimeExpression", rollupPlan == null ? "" : rollupPlan.queryTimeExpression);
        evidence.put("mvFinestGrain", MV_FINEST_GRAIN);
        evidence.put("queryTargetGrain", rollupPlan == null ? "" : rollupPlan.queryTargetGrain);
        evidence.put("mvTimeExpression", rollupPlan == null ? "" : rollupPlan.mvTimeExpression);
        evidence.put("mvTimeColumn", rollupPlan == null ? "" : rollupPlan.mvTimeColumn);
        evidence.put("rewriteRollupExpression", rollupPlan == null ? "" : rollupPlan.rewriteRollupExpression);
        evidence.put("calendarPolicy", CALENDAR_POLICY);
        evidence.put(
            "calendarPolicyZh",
            "仅支持自然日历日粒度上卷到月、季度或年；周、财务日历和显式时区语义需要人工策略。"
        );
        evidence.put("blockingReasons", immutableMapList(blockingReasons));
        evidence.put("reviewWarnings", reviewWarnings(blockingReasons));
        return evidence;
    }

    private static List<Map<String, Object>> reviewWarnings(List<Map<String, Object>> blockingReasons) {
        if (blockingReasons != null && !blockingReasons.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, Object> warning = new LinkedHashMap<String, Object>();
        warning.put("code", "NATURAL_CALENDAR_ASSUMPTION");
        warning.put(
            "description",
            "ROLLUP_MV 静态生成按自然日历 DATE_TRUNC 语义，不声明财务日历或时区转换等价。"
        );
        warning.put("requiredEvidence", Arrays.asList("TIME_COLUMN_TYPE", "CALENDAR_POLICY", "TIMEZONE_POLICY"));
        warning.put("generatedAllowed", Boolean.TRUE);
        return Collections.<Map<String, Object>>singletonList(warning);
    }
}
