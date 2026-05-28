package com.company.sqloptimization.application.service;

import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDag;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

final class QueryWrapperPreserver {

    private QueryWrapperPreserver() {
    }

    static WrapperAnalysis analyze(String sourceSql, QueryBlockDag queryBlockDag) {
        WrapperShape shape = detectWrapper(sourceSql);
        List<String> blockIds = sourceQueryBlockIds(queryBlockDag);
        String replacedSubgraphId = replacedSubgraphId(queryBlockDag, shape);
        return new WrapperAnalysis(
            shape.detected,
            shape.outerProjection,
            shape.outerAlias,
            shape.outerSelectPrefix,
            shape.innerSql,
            shape.outerSuffix,
            shape.outerWherePresent,
            shape.outerGroupByPresent,
            shape.outerHavingPresent,
            shape.outerOrderByPresent,
            shape.outerLimitPresent,
            blockIds,
            replacedSubgraphId
        );
    }

    private static WrapperShape detectWrapper(String sourceSql) {
        if (!StringUtils.hasText(sourceSql)) {
            return WrapperShape.none();
        }
        String sql = trimTrailingSemicolon(sourceSql);
        int selectIndex = indexOfTopLevelKeyword(sql, "SELECT", 0);
        int fromIndex = indexOfTopLevelKeyword(sql, "FROM", selectIndex < 0 ? 0 : selectIndex + 6);
        if (selectIndex < 0 || fromIndex < 0 || fromIndex <= selectIndex) {
            return WrapperShape.none();
        }
        int fromValueIndex = skipWhitespace(sql, fromIndex + 4);
        if (fromValueIndex >= sql.length() || sql.charAt(fromValueIndex) != '(') {
            return WrapperShape.none();
        }
        int closeIndex = matchingParen(sql, fromValueIndex);
        if (closeIndex <= fromValueIndex) {
            return WrapperShape.none();
        }
        String inner = sql.substring(fromValueIndex + 1, closeIndex).trim();
        if (!startsWithSelectLike(inner)) {
            return WrapperShape.none();
        }
        String suffix = sql.substring(closeIndex + 1).trim();
        if (topLevelKeywordPresent(suffix, "JOIN")) {
            return WrapperShape.none();
        }
        String alias = firstAlias(suffix);
        String outerProjection = sql.substring(selectIndex + 6, fromIndex).trim();
        String outerSelectPrefix = sql.substring(selectIndex, fromValueIndex);
        return new WrapperShape(
            true,
            outerProjection,
            alias,
            outerSelectPrefix,
            inner,
            suffix,
            topLevelKeywordPresent(suffix, "WHERE"),
            topLevelKeywordPresent(suffix, "GROUP BY"),
            topLevelKeywordPresent(suffix, "HAVING"),
            topLevelKeywordPresent(suffix, "ORDER BY"),
            topLevelKeywordPresent(suffix, "LIMIT")
        );
    }

    private static List<String> sourceQueryBlockIds(QueryBlockDag queryBlockDag) {
        if (queryBlockDag == null) {
            return new ArrayList<String>();
        }
        if (queryBlockDag.getTopologicalOrder() != null && !queryBlockDag.getTopologicalOrder().isEmpty()) {
            return new ArrayList<String>(queryBlockDag.getTopologicalOrder());
        }
        List<String> ids = new ArrayList<String>();
        for (QueryBlockNode block : queryBlockDag.getBlocks()) {
            ids.add(block.getBlockId());
        }
        return ids;
    }

    private static String replacedSubgraphId(QueryBlockDag queryBlockDag, WrapperShape shape) {
        if (queryBlockDag == null || queryBlockDag.getBlocks().isEmpty()) {
            return "";
        }
        String rootBlockId = queryBlockDag.getRootBlockId();
        if (shape.detected) {
            for (QueryBlockNode block : queryBlockDag.getBlocks()) {
                if (rootBlockId.equals(block.getParentBlockId())
                    && ("DERIVED_TABLE".equals(block.getBlockRole()) || "SUBQUERY".equals(block.getBlockRole()))) {
                    return block.getBlockId();
                }
            }
        }
        return StringUtils.hasText(rootBlockId) ? rootBlockId : queryBlockDag.getBlocks().get(0).getBlockId();
    }

    private static boolean startsWithSelectLike(String sql) {
        if (!StringUtils.hasText(sql)) {
            return false;
        }
        String upper = stripLeadingParentheses(sql).toUpperCase(Locale.ROOT);
        return upper.startsWith("SELECT") || upper.startsWith("WITH");
    }

    private static String stripLeadingParentheses(String value) {
        String result = value == null ? "" : value.trim();
        while (result.startsWith("(") && result.endsWith(")")) {
            int close = matchingParen(result, 0);
            if (close != result.length() - 1) {
                break;
            }
            result = result.substring(1, result.length() - 1).trim();
        }
        return result;
    }

    private static String firstAlias(String suffix) {
        if (!StringUtils.hasText(suffix)) {
            return "";
        }
        String cleaned = suffix.trim();
        if (cleaned.toUpperCase(Locale.ROOT).startsWith("AS ")) {
            cleaned = cleaned.substring(3).trim();
        }
        StringBuilder alias = new StringBuilder();
        for (int i = 0; i < cleaned.length(); i++) {
            char ch = cleaned.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '$') {
                alias.append(ch);
            } else {
                break;
            }
        }
        return alias.toString();
    }

    private static boolean topLevelKeywordPresent(String text, String keyword) {
        return indexOfTopLevelKeyword(text, keyword, 0) >= 0;
    }

    private static int indexOfTopLevelKeyword(String sql, String keyword, int start) {
        if (!StringUtils.hasText(sql) || !StringUtils.hasText(keyword)) {
            return -1;
        }
        String upper = sql.toUpperCase(Locale.ROOT);
        String target = keyword.toUpperCase(Locale.ROOT);
        int depth = 0;
        boolean inString = false;
        for (int i = Math.max(0, start); i <= upper.length() - target.length(); i++) {
            char ch = upper.charAt(i);
            if (ch == '\'') {
                inString = !inString;
            }
            if (inString) {
                continue;
            }
            if (ch == '(') {
                depth++;
                continue;
            }
            if (ch == ')') {
                depth = Math.max(0, depth - 1);
                continue;
            }
            if (depth == 0
                && upper.startsWith(target, i)
                && wordBoundary(upper, i - 1)
                && wordBoundary(upper, i + target.length())) {
                return i;
            }
        }
        return -1;
    }

    private static int matchingParen(String sql, int openIndex) {
        int depth = 0;
        boolean inString = false;
        for (int i = openIndex; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\'') {
                inString = !inString;
            }
            if (inString) {
                continue;
            }
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean wordBoundary(String text, int index) {
        if (index < 0 || index >= text.length()) {
            return true;
        }
        char ch = text.charAt(index);
        return !Character.isLetterOrDigit(ch) && ch != '_';
    }

    private static int skipWhitespace(String text, int index) {
        int result = index;
        while (result < text.length() && Character.isWhitespace(text.charAt(result))) {
            result++;
        }
        return result;
    }

    private static String trimTrailingSemicolon(String sql) {
        String result = sql == null ? "" : sql.trim();
        while (result.endsWith(";")) {
            result = result.substring(0, result.length() - 1).trim();
        }
        return result;
    }

    private static final class WrapperShape {
        private final boolean detected;
        private final String outerProjection;
        private final String outerAlias;
        private final String outerSelectPrefix;
        private final String innerSql;
        private final String outerSuffix;
        private final boolean outerWherePresent;
        private final boolean outerGroupByPresent;
        private final boolean outerHavingPresent;
        private final boolean outerOrderByPresent;
        private final boolean outerLimitPresent;

        private WrapperShape(boolean detected,
                             String outerProjection,
                             String outerAlias,
                             String outerSelectPrefix,
                             String innerSql,
                             String outerSuffix,
                             boolean outerWherePresent,
                             boolean outerGroupByPresent,
                             boolean outerHavingPresent,
                             boolean outerOrderByPresent,
                             boolean outerLimitPresent) {
            this.detected = detected;
            this.outerProjection = outerProjection;
            this.outerAlias = outerAlias;
            this.outerSelectPrefix = outerSelectPrefix;
            this.innerSql = innerSql;
            this.outerSuffix = outerSuffix;
            this.outerWherePresent = outerWherePresent;
            this.outerGroupByPresent = outerGroupByPresent;
            this.outerHavingPresent = outerHavingPresent;
            this.outerOrderByPresent = outerOrderByPresent;
            this.outerLimitPresent = outerLimitPresent;
        }

        private static WrapperShape none() {
            return new WrapperShape(false, "", "", "", "", "", false, false, false, false, false);
        }
    }

    static final class WrapperAnalysis {
        private final boolean outerQueryPreserved;
        private final String outerProjection;
        private final String outerAlias;
        private final String outerSelectPrefix;
        private final String innerSql;
        private final String outerSuffix;
        private final boolean outerWherePresent;
        private final boolean outerGroupByPresent;
        private final boolean outerHavingPresent;
        private final boolean outerOrderByPresent;
        private final boolean outerLimitPresent;
        private final List<String> sourceQueryBlockIds;
        private final String replacedSubgraphId;

        private WrapperAnalysis(boolean outerQueryPreserved,
                                String outerProjection,
                                String outerAlias,
                                String outerSelectPrefix,
                                String innerSql,
                                String outerSuffix,
                                boolean outerWherePresent,
                                boolean outerGroupByPresent,
                                boolean outerHavingPresent,
                                boolean outerOrderByPresent,
                                boolean outerLimitPresent,
                                List<String> sourceQueryBlockIds,
                                String replacedSubgraphId) {
            this.outerQueryPreserved = outerQueryPreserved;
            this.outerProjection = outerProjection == null ? "" : outerProjection;
            this.outerAlias = outerAlias == null ? "" : outerAlias;
            this.outerSelectPrefix = outerSelectPrefix == null ? "" : outerSelectPrefix;
            this.innerSql = innerSql == null ? "" : innerSql;
            this.outerSuffix = outerSuffix == null ? "" : outerSuffix;
            this.outerWherePresent = outerWherePresent;
            this.outerGroupByPresent = outerGroupByPresent;
            this.outerHavingPresent = outerHavingPresent;
            this.outerOrderByPresent = outerOrderByPresent;
            this.outerLimitPresent = outerLimitPresent;
            this.sourceQueryBlockIds = sourceQueryBlockIds == null ? new ArrayList<String>() : sourceQueryBlockIds;
            this.replacedSubgraphId = replacedSubgraphId == null ? "" : replacedSubgraphId;
        }

        boolean isOuterQueryPreserved() {
            return outerQueryPreserved;
        }

        boolean isCountOuterProjection() {
            String normalized = normalizeSpace(outerProjection).toUpperCase(Locale.ROOT);
            return normalized.startsWith("COUNT(*)") || normalized.startsWith("COUNT(1)");
        }

        String getCandidateSourceSql(String fallbackSourceSql) {
            if (outerQueryPreserved && StringUtils.hasText(innerSql)) {
                return innerSql;
            }
            return fallbackSourceSql == null ? "" : fallbackSourceSql;
        }

        String composeRootRewrite(String subgraphRewriteSql) {
            if (!outerQueryPreserved || !StringUtils.hasText(subgraphRewriteSql)) {
                return subgraphRewriteSql;
            }
            StringBuilder builder = new StringBuilder();
            builder.append(outerSelectPrefix);
            builder.append("(\n");
            builder.append(trimTrailingSemicolon(subgraphRewriteSql));
            builder.append("\n)");
            if (StringUtils.hasText(outerSuffix)) {
                builder.append(' ').append(outerSuffix);
            }
            builder.append(';');
            return builder.toString();
        }

        boolean hasOrderOrLimit() {
            return outerOrderByPresent || outerLimitPresent;
        }

        boolean orderLimitPreservedBy(String rewriteSql) {
            if (!hasOrderOrLimit()) {
                return true;
            }
            String normalized = normalizeForComparison(rewriteSql);
            return (!outerOrderByPresent || normalized.contains("ORDER BY"))
                && (!outerLimitPresent || normalized.contains("LIMIT")
                || normalized.matches("(?is).*\\bFETCH\\s+NEXT\\s+\\d+\\s+ROWS\\s+ONLY\\b.*"));
        }

        boolean projectionPreservedBy(String rewriteSql) {
            if (!outerQueryPreserved || !StringUtils.hasText(outerProjection)) {
                return true;
            }
            String normalizedRewrite = normalizeForComparison(rewriteSql);
            String normalizedProjection = normalizeForComparison(outerProjection);
            if (normalizedProjection.startsWith("COUNT(")) {
                return normalizedRewrite.startsWith("SELECT " + normalizedProjection)
                    || normalizedRewrite.startsWith("WITH ") && normalizedRewrite.contains("SELECT " + normalizedProjection);
            }
            return normalizedRewrite.contains("SELECT " + normalizedProjection);
        }

        Map<String, Object> toEvidence() {
            LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
            evidence.put("outerQueryPreserved", Boolean.valueOf(outerQueryPreserved));
            evidence.put("outerProjection", outerProjection);
            evidence.put("outerAlias", outerAlias);
            evidence.put("innerSqlLength", Integer.valueOf(innerSql.length()));
            evidence.put("outerWherePresent", Boolean.valueOf(outerWherePresent));
            evidence.put("outerGroupByPresent", Boolean.valueOf(outerGroupByPresent));
            evidence.put("outerHavingPresent", Boolean.valueOf(outerHavingPresent));
            evidence.put("outerOrderByPresent", Boolean.valueOf(outerOrderByPresent));
            evidence.put("outerLimitPresent", Boolean.valueOf(outerLimitPresent));
            evidence.put("sourceQueryBlockIds", new ArrayList<String>(sourceQueryBlockIds));
            evidence.put("replacedSubgraphId", replacedSubgraphId);
            return evidence;
        }

        List<String> getSourceQueryBlockIds() {
            return new ArrayList<String>(sourceQueryBlockIds);
        }

        String getReplacedSubgraphId() {
            return replacedSubgraphId;
        }

        private String normalizeSpace(String value) {
            return value == null ? "" : value.trim().replaceAll("\\s+", " ");
        }

        private String normalizeForComparison(String value) {
            return normalizeSpace(value)
                .replace("`", "")
                .replace("\"", "")
                .replaceAll("\\s*\\.\\s*", ".")
                .toUpperCase(Locale.ROOT);
        }
    }
}
