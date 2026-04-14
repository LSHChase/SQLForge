package com.sqlforge.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SqlAssessmentService {

    public Map<String, Object> assess(String tenantId, String sql, Integer slaMs, Integer targetConcurrency) {
        Map<String, Object> tenant = resolveTenant(tenantId);
        Map<String, Object> assessment = buildAssessment(sql);
        String riskLevel = buildRiskLevel(castList(assessment.get("risks")));
        int resolvedTargetConcurrency = targetConcurrency == null ? 20 : targetConcurrency.intValue();

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("tenant", tenant);
        result.put("targetConcurrency", Integer.valueOf(resolvedTargetConcurrency));
        result.put("riskLevel", riskLevel);
        result.put("requiresBenchmark", Boolean.valueOf(riskRank(riskLevel) >= riskRank("high")));
        result.put("assessment", assessment);
        result.put("requestedSlaMs", slaMs);
        return result;
    }

    private Map<String, Object> resolveTenant(String tenantId) {
        Map<String, Object> tenant = new LinkedHashMap<String, Object>();

        if ("tenant-a".equals(tenantId)) {
            tenant.put("id", "tenant-a");
            tenant.put("maxConcurrency", Integer.valueOf(20));
            tenant.put("maxMemoryGb", Integer.valueOf(100));
            tenant.put("maxScanTbPerHour", Integer.valueOf(1));
            return tenant;
        }

        if ("tenant-b".equals(tenantId)) {
            tenant.put("id", "tenant-b");
            tenant.put("maxConcurrency", Integer.valueOf(50));
            tenant.put("maxMemoryGb", Integer.valueOf(200));
            tenant.put("maxScanTbPerHour", Integer.valueOf(2));
            return tenant;
        }

        tenant.put("id", tenantId == null || tenantId.trim().isEmpty() ? "default-tenant" : tenantId);
        tenant.put("maxConcurrency", Integer.valueOf(20));
        tenant.put("maxMemoryGb", Integer.valueOf(100));
        tenant.put("maxScanTbPerHour", Integer.valueOf(1));
        return tenant;
    }

    private Map<String, Object> buildAssessment(String sql) {
        String normalizedSql = normalizeSql(sql);
        List<String> tableReferences = detectTableReferences(normalizedSql);
        List<String> tables = unique(tableReferences);
        int joinCount = countMatches(normalizedSql.toLowerCase(Locale.ROOT), " join ");
        boolean hasOrderBy = normalizedSql.toLowerCase(Locale.ROOT).contains(" order by ");
        boolean hasSelectStar = normalizedSql.toLowerCase(Locale.ROOT).contains("select *");
        boolean hasLimit = normalizedSql.toLowerCase(Locale.ROOT).contains(" limit ");
        boolean hasGroupBy = normalizedSql.toLowerCase(Locale.ROOT).contains(" group by ");
        String scanPattern = detectScanPattern(normalizedSql);
        String joinType = detectJoinType(joinCount);
        String computeDensity = detectComputeDensity(normalizedSql);
        String resultSetRisk = detectResultSetRisk(hasLimit, hasGroupBy, normalizedSql);
        List<Map<String, Object>> repeatedTables = detectRepeatedTables(tableReferences);
        List<Map<String, Object>> repeatedExpressions = detectRepeatedExpressions(normalizedSql);

        Map<String, Object> labels = new LinkedHashMap<String, Object>();
        labels.put("scanPattern", scanPattern);
        labels.put("joinType", joinType);
        labels.put("computeDensity", computeDensity);
        labels.put("resourceType", detectResourceType(scanPattern, joinCount, computeDensity, hasOrderBy));
        labels.put("resultSetRisk", resultSetRisk);

        Map<String, Object> validations = buildValidations(
            normalizedSql,
            scanPattern,
            hasOrderBy,
            hasSelectStar,
            hasLimit,
            resultSetRisk,
            repeatedTables,
            repeatedExpressions
        );

        List<Map<String, Object>> risks = buildRisks(
            normalizedSql,
            scanPattern,
            joinCount,
            hasOrderBy,
            hasSelectStar,
            tables,
            computeDensity,
            repeatedTables,
            repeatedExpressions,
            resultSetRisk
        );

        Map<String, Object> resourceEstimate = new LinkedHashMap<String, Object>();
        resourceEstimate.put("score", Integer.valueOf(estimateResourceScore(scanPattern, joinCount, computeDensity, tables.size(), risks.size())));

        Map<String, Object> assessment = new LinkedHashMap<String, Object>();
        assessment.put("sql", normalizedSql);
        assessment.put("fingerprint", md5(normalizedSql));
        assessment.put("tables", tables);
        assessment.put("labels", labels);
        assessment.put("validations", validations);
        assessment.put("risks", risks);
        assessment.put("resourceEstimate", resourceEstimate);
        return assessment;
    }

    private String buildRiskLevel(List<Map<String, Object>> risks) {
        boolean hasCritical = false;
        boolean hasHigh = false;

        for (Map<String, Object> risk : risks) {
            String severity = String.valueOf(risk.get("severity"));

            if ("critical".equals(severity)) {
                hasCritical = true;
            } else if ("high".equals(severity)) {
                hasHigh = true;
            }
        }

        if (hasCritical) {
            return "critical";
        }

        if (hasHigh) {
            return "high";
        }

        return risks.isEmpty() ? "low" : "medium";
    }

    private Map<String, Object> buildValidations(
        String sql,
        String scanPattern,
        boolean hasOrderBy,
        boolean hasSelectStar,
        boolean hasLimit,
        String resultSetRisk,
        List<Map<String, Object>> repeatedTables,
        List<Map<String, Object>> repeatedExpressions
    ) {
        boolean predicatePushdownRisk = "full-table".equals(scanPattern) || sql.toLowerCase(Locale.ROOT).contains(" where lower(");
        Map<String, Object> validations = new LinkedHashMap<String, Object>();
        validations.put(
            "predicatePushdown",
            mapOf(
                "status",
                predicatePushdownRisk ? "warn" : "pass",
                "message",
                predicatePushdownRisk ? "过滤条件可能无法有效下推，需要验证分区裁剪与表达式包裹" : "过滤条件具备较好的下推机会"
            )
        );
        validations.put(
            "bucketJoin",
            mapOf(
                "status",
                sql.toLowerCase(Locale.ROOT).contains(" join ") ? "opportunity" : "na",
                "message",
                sql.toLowerCase(Locale.ROOT).contains(" join ") ? "存在相同 Join Key，可验证 Bucket Join 与 Broadcast 策略" : "当前 SQL 无 Join，不涉及分桶 Join"
            )
        );
        validations.put(
            "expressionReuse",
            mapOf(
                "status",
                repeatedExpressions.isEmpty() ? "pass" : "warn",
                "message",
                repeatedExpressions.isEmpty() ? "未检测到明显重复表达式" : "检测到重复函数表达式，可考虑复用或物化"
            )
        );
        validations.put(
            "repeatedJoin",
            mapOf(
                "status",
                repeatedTables.isEmpty() ? "pass" : "warn",
                "message",
                repeatedTables.isEmpty() ? "未检测到重复关联同表" : "同表被多次关联，可考虑 CTE 或临时表物化"
            )
        );
        validations.put(
            "sortNecessity",
            mapOf(
                "status",
                hasOrderBy && !hasLimit ? "warn" : hasOrderBy ? "observe" : "pass",
                "message",
                hasOrderBy && !hasLimit ? "ORDER BY 未配合 LIMIT，需验证是否为非必要排序" : hasOrderBy ? "存在 ORDER BY，但结果集有界，建议结合业务语义确认必要性" : "未检测到显著排序风险"
            )
        );
        validations.put(
            "projectionPushdown",
            mapOf(
                "status",
                hasSelectStar ? "warn" : "pass",
                "message",
                hasSelectStar ? "SELECT * 可能导致列裁剪不足" : "查询具备较好的投影下推条件"
            )
        );
        validations.put(
            "resultSetControl",
            mapOf(
                "status",
                "wide-open".equals(resultSetRisk) ? "warn" : "pass",
                "message",
                "wide-open".equals(resultSetRisk) ? "结果集无明显边界，需防止返回过大结果" : "结果集规模具备一定控制"
            )
        );
        return validations;
    }

    private List<Map<String, Object>> buildRisks(
        String sql,
        String scanPattern,
        int joinCount,
        boolean hasOrderBy,
        boolean hasSelectStar,
        List<String> tables,
        String computeDensity,
        List<Map<String, Object>> repeatedTables,
        List<Map<String, Object>> repeatedExpressions,
        String resultSetRisk
    ) {
        List<Map<String, Object>> risks = new ArrayList<Map<String, Object>>();

        if ("full-table".equals(scanPattern)) {
            risks.add(risk("FULL_SCAN", "high", "查询缺少有效过滤条件，存在全表扫描风险"));
        }

        if (joinCount >= 2) {
            risks.add(risk("COMPLEX_JOIN", joinCount >= 4 ? "high" : "medium", "Join 链较长，存在 Shuffle 放大和计划脆弱风险"));
        }

        if (sql.toLowerCase(Locale.ROOT).contains(" join ") && !sql.toLowerCase(Locale.ROOT).contains(" on ")) {
            risks.add(risk("MISSING_JOIN_CONDITION", "critical", "疑似缺失 Join 条件，需排查 Cartesian Product"));
        }

        if (hasOrderBy && !sql.toLowerCase(Locale.ROOT).contains(" limit ")) {
            risks.add(risk("UNBOUNDED_SORT", "medium", "ORDER BY 未配合 LIMIT，可能触发非必要全局排序"));
        }

        if (hasSelectStar) {
            risks.add(risk("SELECT_STAR", "medium", "SELECT * 可能导致投影下推失效与网络放大"));
        }

        if (tables.size() >= 3) {
            risks.add(risk("MULTI_TABLE_TOUCH", "medium", "涉及多表访问，建议重点验证统计信息与分桶策略"));
        }

        if ("window".equals(computeDensity) || "udf-heavy".equals(computeDensity)) {
            risks.add(risk("HEAVY_COMPUTE", "medium", "存在窗口函数或高复杂表达式，需关注 CPU 与内存峰值"));
        }

        if (!repeatedTables.isEmpty()) {
            risks.add(risk("REPEATED_TABLE_JOIN", "medium", "检测到同表多次关联，可能需要 CTE 或物化来降低重复计算"));
        }

        if (!repeatedExpressions.isEmpty()) {
            risks.add(risk("REPEATED_EXPRESSION", "medium", "检测到重复表达式计算，可能存在 CPU 浪费"));
        }

        if ("wide-open".equals(resultSetRisk)) {
            risks.add(risk("LARGE_RESULT_SET", "medium", "查询结果无聚合或 LIMIT 约束，可能导致结果集过大"));
        }

        return risks;
    }

    private Map<String, Object> risk(String code, String severity, String message) {
        return mapOf("code", code, "severity", severity, "message", message);
    }

    private String normalizeSql(String sql) {
        return sql == null ? "" : sql.trim().replaceAll("\\s+", " ");
    }

    private List<String> detectTableReferences(String sql) {
        List<String> references = new ArrayList<String>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("\\b(?:from|join)\\s+([a-zA-Z0-9_.]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(sql);

        while (matcher.find()) {
            references.add(matcher.group(1));
        }

        return references;
    }

    private String detectScanPattern(String sql) {
        String lower = sql.toLowerCase(Locale.ROOT);
        if (!lower.contains(" where ")) {
            return "full-table";
        }

        if (lower.contains(" between ") || lower.contains(" >") || lower.contains(" <") || lower.contains(">=") || lower.contains("<=")) {
            return "range";
        }

        return "point-lookup";
    }

    private String detectJoinType(int joinCount) {
        if (joinCount == 0) {
            return "single-table";
        }
        if (joinCount == 1) {
            return "star";
        }
        if (joinCount <= 3) {
            return "chain";
        }
        return "snowflake";
    }

    private String detectComputeDensity(String sql) {
        String lower = sql.toLowerCase(Locale.ROOT);
        if (lower.contains("over(") || lower.contains("over (")) {
            return "window";
        }
        if (lower.contains("sum(") || lower.contains("avg(") || lower.contains("max(") || lower.contains("min(") || lower.contains("count(")) {
            return "heavy-aggregation";
        }
        if (lower.contains("regexp_") || lower.contains("json_") || lower.contains("map_") || lower.contains("array_")) {
            return "udf-heavy";
        }
        return "light";
    }

    private String detectResourceType(String scanPattern, int joinCount, String computeDensity, boolean hasOrderBy) {
        if ("full-table".equals(scanPattern)) {
            return "io-bound";
        }
        if (joinCount >= 2) {
            return "network-mixed";
        }
        if (!"light".equals(computeDensity) || hasOrderBy) {
            return "cpu-bound";
        }
        return "memory-mixed";
    }

    private String detectResultSetRisk(boolean hasLimit, boolean hasGroupBy, String sql) {
        if (hasLimit) {
            return "bounded";
        }
        if (hasGroupBy || sql.toLowerCase(Locale.ROOT).contains("count(")) {
            return "aggregated";
        }
        return "wide-open";
    }

    private List<Map<String, Object>> detectRepeatedTables(List<String> tableReferences) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (String table : tableReferences) {
            Integer value = counts.get(table);
            counts.put(table, value == null ? Integer.valueOf(1) : Integer.valueOf(value.intValue() + 1));
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue().intValue() > 1) {
                result.add(mapOf("table", entry.getKey(), "count", entry.getValue()));
            }
        }
        return result;
    }

    private List<Map<String, Object>> detectRepeatedExpressions(String sql) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("\\bselect\\s+(.+?)\\s+from\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(sql);
        if (!matcher.find()) {
            return new ArrayList<Map<String, Object>>();
        }

        String[] expressions = matcher.group(1).split(",");
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();

        for (String expression : expressions) {
            String normalized = expression.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            if (normalized.contains("(")) {
                Integer value = counts.get(normalized);
                counts.put(normalized, value == null ? Integer.valueOf(1) : Integer.valueOf(value.intValue() + 1));
            }
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue().intValue() > 1) {
                result.add(mapOf("expression", entry.getKey(), "count", entry.getValue()));
            }
        }
        return result;
    }

    private int estimateResourceScore(String scanPattern, int joinCount, String computeDensity, int tableCount, int riskCount) {
        return ("full-table".equals(scanPattern) ? 40 : "range".equals(scanPattern) ? 20 : 10)
            + joinCount * 8
            + ("light".equals(computeDensity) ? 5 : "heavy-aggregation".equals(computeDensity) ? 18 : 24)
            + tableCount * 3
            + riskCount * 5;
    }

    private int countMatches(String value, String token) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = value.indexOf(token, fromIndex)) >= 0) {
            count += 1;
            fromIndex += token.length();
        }
        return count;
    }

    private String md5(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) {
                builder.append(String.format("%02x", Byte.valueOf(item)));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private int riskRank(String riskLevel) {
        if ("critical".equals(riskLevel)) {
            return 3;
        }
        if ("high".equals(riskLevel)) {
            return 2;
        }
        if ("medium".equals(riskLevel)) {
            return 1;
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return value == null ? new ArrayList<Map<String, Object>>() : (List<Map<String, Object>>) value;
    }

    private List<String> unique(List<String> values) {
        List<String> result = new ArrayList<String>();
        for (String value : values) {
            if (!result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return result;
    }
}
