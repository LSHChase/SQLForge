package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.application.controller.vo.RecommendationDiffVO;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SqlDiffApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "HARN_132_SQL_DIFF_SERVICE";
    private static final int TOKEN_DIFF_LIMIT = 500;

    private final SqlOptimizationPipelineService pipelineService;

    public SqlDiffApplicationService(SqlOptimizationPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    public RecommendationDiffVO buildRecommendationDiff(AccelerationRecommendation recommendation) {
        String originalSql = safeSql(recommendation.getSourceSqlText());
        String recommendedSql = safeSql(recommendation.getRecommendedSqlText());
        List<Map<String, Object>> textDiff = buildTextDiff(originalSql, recommendedSql);
        AstDiffResult astDiff = buildAstSummaryDiff(originalSql, recommendedSql);
        List<Map<String, Object>> ruleDiff = buildRuleDiff(recommendation, textDiff);
        Map<String, Object> summary = buildDiffSummary(recommendation, textDiff, ruleDiff, astDiff);

        RecommendationDiffVO vo = new RecommendationDiffVO();
        vo.setRecommendationId(recommendation.getRecommendationId());
        vo.setTenantId(recommendation.getTenantId());
        vo.setSourceType(nameOrNull(recommendation.getSourceType()));
        vo.setSourceKind(nameOrNull(recommendation.getSourceKind()));
        vo.setSourceId(resolveSourceId(recommendation));
        vo.setEvidenceLevel(nameOrNull(recommendation.getEvidenceLevel()));
        vo.setSqlFingerprint(recommendation.getSqlFingerprint());
        vo.setOriginalSql(recommendation.getSourceSqlText());
        vo.setRecommendedSql(recommendation.getRecommendedSqlText());
        vo.setTextDiff(textDiff);
        vo.setRuleDiff(ruleDiff);
        vo.setAstSummaryDiff(astDiff.payload);
        vo.setDiffSummary(summary);
        vo.setDiffStatus(String.valueOf(summary.get("diffStatus")));
        vo.setContractStage(CONTRACT_STAGE);
        vo.setImplementationStage(IMPLEMENTATION_STAGE);
        return vo;
    }

    private List<Map<String, Object>> buildTextDiff(String originalSql, String recommendedSql) {
        if (originalSql.equals(recommendedSql)) {
            return Collections.emptyList();
        }
        DiffInput input = diffInput(originalSql, recommendedSql);
        List<DiffOperation> operations = lcsDiff(input.originalUnits, input.recommendedUnits);
        return toDiffHunks(operations, input.granularity);
    }

    private DiffInput diffInput(String originalSql, String recommendedSql) {
        List<String> originalTokens = tokenizeSql(originalSql);
        List<String> recommendedTokens = tokenizeSql(recommendedSql);
        if (!containsLineBreak(originalSql)
            && !containsLineBreak(recommendedSql)
            && originalTokens.size() <= TOKEN_DIFF_LIMIT
            && recommendedTokens.size() <= TOKEN_DIFF_LIMIT) {
            return new DiffInput(originalTokens, recommendedTokens, "TOKEN");
        }
        return new DiffInput(splitLines(originalSql), splitLines(recommendedSql), "LINE");
    }

    private List<DiffOperation> lcsDiff(List<String> originalUnits, List<String> recommendedUnits) {
        int originalSize = originalUnits.size();
        int recommendedSize = recommendedUnits.size();
        int[][] lcs = new int[originalSize + 1][recommendedSize + 1];
        for (int originalIndex = originalSize - 1; originalIndex >= 0; originalIndex--) {
            for (int recommendedIndex = recommendedSize - 1; recommendedIndex >= 0; recommendedIndex--) {
                if (originalUnits.get(originalIndex).equals(recommendedUnits.get(recommendedIndex))) {
                    lcs[originalIndex][recommendedIndex] = lcs[originalIndex + 1][recommendedIndex + 1] + 1;
                } else {
                    lcs[originalIndex][recommendedIndex] = Math.max(
                        lcs[originalIndex + 1][recommendedIndex],
                        lcs[originalIndex][recommendedIndex + 1]
                    );
                }
            }
        }

        List<DiffOperation> operations = new ArrayList<DiffOperation>();
        int originalIndex = 0;
        int recommendedIndex = 0;
        while (originalIndex < originalSize && recommendedIndex < recommendedSize) {
            String original = originalUnits.get(originalIndex);
            String recommended = recommendedUnits.get(recommendedIndex);
            if (original.equals(recommended)) {
                operations.add(new DiffOperation("EQUAL", original, null, originalIndex, recommendedIndex));
                originalIndex++;
                recommendedIndex++;
            } else if (lcs[originalIndex + 1][recommendedIndex] >= lcs[originalIndex][recommendedIndex + 1]) {
                operations.add(new DiffOperation("DELETE", original, null, originalIndex, recommendedIndex));
                originalIndex++;
            } else {
                operations.add(new DiffOperation("INSERT", null, recommended, originalIndex, recommendedIndex));
                recommendedIndex++;
            }
        }
        while (originalIndex < originalSize) {
            operations.add(new DiffOperation(
                "DELETE",
                originalUnits.get(originalIndex),
                null,
                originalIndex,
                recommendedIndex
            ));
            originalIndex++;
        }
        while (recommendedIndex < recommendedSize) {
            operations.add(new DiffOperation(
                "INSERT",
                null,
                recommendedUnits.get(recommendedIndex),
                originalIndex,
                recommendedIndex
            ));
            recommendedIndex++;
        }
        return operations;
    }

    private List<Map<String, Object>> toDiffHunks(List<DiffOperation> operations, String granularity) {
        List<Map<String, Object>> hunks = new ArrayList<Map<String, Object>>();
        int index = 0;
        int hunkNumber = 1;
        while (index < operations.size()) {
            DiffOperation operation = operations.get(index);
            if ("EQUAL".equals(operation.type)) {
                index++;
                continue;
            }
            int originalStart = operation.originalIndex;
            int recommendedStart = operation.recommendedIndex;
            List<String> originalUnits = new ArrayList<String>();
            List<String> recommendedUnits = new ArrayList<String>();
            boolean hasDelete = false;
            boolean hasInsert = false;
            while (index < operations.size() && !"EQUAL".equals(operations.get(index).type)) {
                DiffOperation current = operations.get(index);
                if ("DELETE".equals(current.type)) {
                    originalUnits.add(current.originalUnit);
                    hasDelete = true;
                } else if ("INSERT".equals(current.type)) {
                    recommendedUnits.add(current.recommendedUnit);
                    hasInsert = true;
                }
                index++;
            }
            String type = resolveHunkType(hasDelete, hasInsert);
            Map<String, Object> hunk = new LinkedHashMap<String, Object>();
            hunk.put("hunkId", "hunk-" + hunkNumber);
            hunk.put("type", type);
            hunk.put("granularity", granularity);
            hunk.put("originalStartLine", Integer.valueOf("LINE".equals(granularity) ? originalStart + 1 : 1));
            hunk.put("recommendedStartLine", Integer.valueOf("LINE".equals(granularity) ? recommendedStart + 1 : 1));
            hunk.put("originalStartUnit", Integer.valueOf(originalStart + 1));
            hunk.put("recommendedStartUnit", Integer.valueOf(recommendedStart + 1));
            hunk.put("originalText", joinUnits(originalUnits, granularity));
            hunk.put("recommendedText", joinUnits(recommendedUnits, granularity));
            hunk.put("originalUnits", originalUnits);
            hunk.put("recommendedUnits", recommendedUnits);
            hunks.add(hunk);
            hunkNumber++;
        }
        return hunks;
    }

    private String resolveHunkType(boolean hasDelete, boolean hasInsert) {
        if (hasDelete && hasInsert) {
            return "REPLACE";
        }
        if (hasDelete) {
            return "DELETE";
        }
        return "INSERT";
    }

    private AstDiffResult buildAstSummaryDiff(String originalSql, String recommendedSql) {
        ParseResult original = parseSql(originalSql);
        ParseResult recommended = parseSql(recommendedSql);
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("parseStatus", parseStatus(original, recommended));
        payload.put("original", original.summary);
        payload.put("recommended", recommended.summary);
        payload.put("changes", compareAstSummaries(original.summary, recommended.summary));
        payload.put("riskFlags", astRiskFlags(payload));
        if (!original.parsed) {
            payload.put("originalParseError", original.errorMessage);
        }
        if (!recommended.parsed) {
            payload.put("recommendedParseError", recommended.errorMessage);
        }
        boolean ready = original.parsed && recommended.parsed;
        return new AstDiffResult(payload, ready);
    }

    private ParseResult parseSql(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return ParseResult.failed("SQL text is blank");
        }
        try {
            SqlOptimizationPipelineService.ParsedSqlProfile profile =
                pipelineService.analyze(sqlText, DataSourceTypeEnum.AUTO);
            return ParseResult.parsed(profileSummary(profile));
        } catch (RuntimeException ex) {
            return ParseResult.failed(ex.getMessage());
        }
    }

    private Map<String, Object> profileSummary(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("parserEngine", profile.getParserEngine());
        summary.put("tables", profile.getTables());
        summary.put("projectionCount", Integer.valueOf(profile.getProjectionCount()));
        summary.put("projectedColumns", profile.getProjectedColumns());
        summary.put("selectStar", Boolean.valueOf(profile.isSelectStar()));
        summary.put("predicateCount", Integer.valueOf(profile.getPredicateCount()));
        summary.put("joinCount", Integer.valueOf(profile.getJoinCount()));
        summary.put("joinTypes", profile.getJoinTypes());
        summary.put("groupByCount", Integer.valueOf(profile.getGroupByCount()));
        summary.put("orderByCount", Integer.valueOf(profile.getOrderByCount()));
        summary.put("aggregateFunctionCount", Integer.valueOf(profile.getAggregateFunctionCount()));
        summary.put("aggregateFunctions", new ArrayList<String>(profile.getAggregateFunctions()));
        summary.put("limitPresent", Boolean.valueOf(profile.isLimitPresent()));
        summary.put("distinctPresent", Boolean.valueOf(profile.isDistinctPresent()));
        summary.put("setOperation", Boolean.valueOf(profile.isSetOperation()));
        summary.put("orPredicateCount", Integer.valueOf(profile.getOrPredicateCount()));
        summary.put("functionWrappedPredicateCount", Integer.valueOf(profile.getFunctionWrappedPredicateCount()));
        summary.put("subqueryCount", Integer.valueOf(profile.getSubqueryCount()));
        return summary;
    }

    private String parseStatus(ParseResult original, ParseResult recommended) {
        if (original.parsed && recommended.parsed) {
            return "BOTH_PARSED";
        }
        if (!original.parsed && !recommended.parsed) {
            return "BOTH_FAILED";
        }
        if (!original.parsed) {
            return "ORIGINAL_PARSE_FAILED";
        }
        return "RECOMMENDED_PARSE_FAILED";
    }

    private List<Map<String, Object>> compareAstSummaries(Map<String, Object> original,
                                                          Map<String, Object> recommended) {
        if (original.isEmpty() || recommended.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> changes = new ArrayList<Map<String, Object>>();
        addAstChange(changes, "PROJECTION", "projectedColumns", original, recommended);
        addAstChange(changes, "PROJECTION", "projectionCount", original, recommended);
        addAstChange(changes, "TABLE", "tables", original, recommended);
        addAstChange(changes, "JOIN", "joinTypes", original, recommended);
        addAstChange(changes, "JOIN", "joinCount", original, recommended);
        addAstChange(changes, "PREDICATE", "predicateCount", original, recommended);
        addAstChange(changes, "GROUP", "groupByCount", original, recommended);
        addAstChange(changes, "ORDER", "orderByCount", original, recommended);
        addAstChange(changes, "LIMIT", "limitPresent", original, recommended);
        addAstChange(changes, "AGGREGATE", "aggregateFunctions", original, recommended);
        addAstChange(changes, "AGGREGATE", "aggregateFunctionCount", original, recommended);
        addAstChange(changes, "DISTINCT", "distinctPresent", original, recommended);
        addAstChange(changes, "SET_OPERATION", "setOperation", original, recommended);
        return changes;
    }

    private void addAstChange(List<Map<String, Object>> changes,
                              String category,
                              String field,
                              Map<String, Object> original,
                              Map<String, Object> recommended) {
        Object originalValue = original.get(field);
        Object recommendedValue = recommended.get(field);
        if (valuesEqual(originalValue, recommendedValue)) {
            return;
        }
        Map<String, Object> change = new LinkedHashMap<String, Object>();
        change.put("category", category);
        change.put("field", field);
        change.put("original", originalValue);
        change.put("recommended", recommendedValue);
        changes.add(change);
    }

    @SuppressWarnings("unchecked")
    private List<String> astRiskFlags(Map<String, Object> astPayload) {
        List<Map<String, Object>> changes = (List<Map<String, Object>>) astPayload.get("changes");
        Set<String> flags = new LinkedHashSet<String>();
        for (Map<String, Object> change : changes) {
            String category = String.valueOf(change.get("category"));
            if ("PROJECTION".equals(category)) {
                flags.add("RETURN_COLUMNS_CHANGED");
            } else if ("PREDICATE".equals(category)) {
                flags.add("FILTER_CONDITION_CHANGED");
            } else if ("JOIN".equals(category)) {
                flags.add("JOIN_SHAPE_CHANGED");
            } else if ("GROUP".equals(category) || "AGGREGATE".equals(category)) {
                flags.add("AGGREGATION_CHANGED");
            } else if ("ORDER".equals(category) || "LIMIT".equals(category)) {
                flags.add("ORDER_OR_LIMIT_CHANGED");
            } else if ("DISTINCT".equals(category)) {
                flags.add("DISTINCT_SEMANTICS_CHANGED");
            }
        }
        return new ArrayList<String>(flags);
    }

    private List<Map<String, Object>> buildRuleDiff(AccelerationRecommendation recommendation,
                                                    List<Map<String, Object>> textDiff) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        appendRuleEntries(result, recommendation.getRuleChain(), "RULE_CHAIN", textDiff);
        appendRuleEntries(
            result,
            recommendation.getUnappliedRules(),
            "UNAPPLIED_RULE",
            Collections.<Map<String, Object>>emptyList()
        );
        appendRuleEntries(
            result,
            recommendation.getSemanticRisks(),
            "SEMANTIC_RISK",
            Collections.<Map<String, Object>>emptyList()
        );
        return result;
    }

    private void appendRuleEntries(List<Map<String, Object>> result,
                                   List<Map<String, Object>> source,
                                   String sourceType,
                                   List<Map<String, Object>> textDiff) {
        int index = result.size() + 1;
        for (Map<String, Object> item : source) {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("diffId", "rule-diff-" + index);
            entry.put("sourceType", sourceType);
            entry.put("rule", valueOrNull(item, "rule"));
            entry.put("level", valueOrNull(item, "level"));
            entry.put("status", resolveRuleStatus(item, sourceType));
            entry.put("manualReviewRequired", Boolean.valueOf(isManualReviewRequired(item, sourceType)));
            entry.put("highlightStatus", textDiff.isEmpty() ? "SUMMARY_ONLY" : "HUNK_REFERENCED");
            entry.put("textHunkIds", hunkIds(textDiff));
            entry.put("evidence", item);
            result.add(entry);
            index++;
        }
    }

    private String resolveRuleStatus(Map<String, Object> item, String sourceType) {
        Object status = item.get("status");
        if (status != null) {
            return String.valueOf(status);
        }
        if ("UNAPPLIED_RULE".equals(sourceType)) {
            return "NOT_APPLIED";
        }
        if ("SEMANTIC_RISK".equals(sourceType)) {
            return "RISK_ONLY";
        }
        return "APPLIED_TO_CANDIDATE_SQL";
    }

    private boolean isManualReviewRequired(Map<String, Object> item, String sourceType) {
        if (Boolean.TRUE.equals(item.get("manualReviewRequired"))) {
            return true;
        }
        if ("UNAPPLIED_RULE".equals(sourceType) || "SEMANTIC_RISK".equals(sourceType)) {
            return true;
        }
        return Boolean.FALSE.equals(item.get("autoApplyEligibleAfterValidation"));
    }

    private List<String> hunkIds(List<Map<String, Object>> textDiff) {
        List<String> ids = new ArrayList<String>();
        for (Map<String, Object> hunk : textDiff) {
            Object hunkId = hunk.get("hunkId");
            if (hunkId != null) {
                ids.add(String.valueOf(hunkId));
            }
        }
        return ids;
    }

    private Map<String, Object> buildDiffSummary(AccelerationRecommendation recommendation,
                                                 List<Map<String, Object>> textDiff,
                                                 List<Map<String, Object>> ruleDiff,
                                                 AstDiffResult astDiff) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("diffStatus", astDiff.ready ? "READY" : "TEXT_DIFF_READY_AST_WARNING");
        summary.put("textDiffReady", Boolean.TRUE);
        summary.put("ruleDiffReady", Boolean.TRUE);
        summary.put("astSummaryReady", Boolean.valueOf(astDiff.ready));
        summary.put("changeCount", Integer.valueOf(textDiff.size()));
        summary.put("insertCount", Integer.valueOf(countHunks(textDiff, "INSERT")));
        summary.put("deleteCount", Integer.valueOf(countHunks(textDiff, "DELETE")));
        summary.put("replaceCount", Integer.valueOf(countHunks(textDiff, "REPLACE")));
        summary.put("ruleDiffCount", Integer.valueOf(ruleDiff.size()));
        summary.put("astChangeCategories", astChangeCategories(astDiff.payload));
        summary.put("riskFlags", astDiff.payload.get("riskFlags"));
        summary.put("manualReviewRequired", Boolean.valueOf(recommendation.isManualReviewRequired()));
        summary.put("autoApplyAllowed", Boolean.valueOf(false));
        summary.put("evidenceBoundary", "DISPLAY_ONLY_NOT_SEMANTIC_PROOF");
        summary.put("writesBackRecommendation", Boolean.FALSE);
        return summary;
    }

    private int countHunks(List<Map<String, Object>> hunks, String type) {
        int count = 0;
        for (Map<String, Object> hunk : hunks) {
            if (type.equals(hunk.get("type"))) {
                count++;
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private List<String> astChangeCategories(Map<String, Object> astPayload) {
        Object changesObject = astPayload.get("changes");
        if (!(changesObject instanceof List<?>)) {
            return Collections.emptyList();
        }
        Set<String> categories = new LinkedHashSet<String>();
        for (Map<String, Object> change : (List<Map<String, Object>>) changesObject) {
            categories.add(String.valueOf(change.get("category")));
        }
        return new ArrayList<String>(categories);
    }

    private List<String> tokenizeSql(String sqlText) {
        List<String> tokens = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        for (int index = 0; index < sqlText.length(); index++) {
            char ch = sqlText.charAt(index);
            if (Character.isWhitespace(ch)) {
                flushToken(tokens, current);
            } else if (isPunctuation(ch)) {
                flushToken(tokens, current);
                tokens.add(String.valueOf(ch));
            } else {
                current.append(ch);
            }
        }
        flushToken(tokens, current);
        return tokens;
    }

    private void flushToken(List<String> tokens, StringBuilder current) {
        if (current.length() > 0) {
            tokens.add(current.toString());
            current.setLength(0);
        }
    }

    private boolean isPunctuation(char ch) {
        return "(),;=<>+-*/".indexOf(ch) >= 0;
    }

    private List<String> splitLines(String sqlText) {
        String normalized = sqlText.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        List<String> result = new ArrayList<String>();
        Collections.addAll(result, lines);
        return result;
    }

    private boolean containsLineBreak(String value) {
        return value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
    }

    private String joinUnits(List<String> units, String granularity) {
        if (units.isEmpty()) {
            return "";
        }
        String separator = "LINE".equals(granularity) ? "\n" : " ";
        StringBuilder builder = new StringBuilder();
        for (String unit : units) {
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(unit);
        }
        return builder.toString();
    }

    private String safeSql(String value) {
        return value == null ? "" : value;
    }

    private boolean valuesEqual(Object original, Object recommended) {
        return original == null ? recommended == null : original.equals(recommended);
    }

    private String nameOrNull(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String valueOrNull(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String resolveSourceId(AccelerationRecommendation recommendation) {
        if (StringUtils.hasText(recommendation.getSourceId())) {
            return recommendation.getSourceId();
        }
        if (StringUtils.hasText(recommendation.getSourceSqlId())) {
            return recommendation.getSourceSqlId();
        }
        if (StringUtils.hasText(recommendation.getHistoryId())) {
            return recommendation.getHistoryId();
        }
        if (StringUtils.hasText(recommendation.getParseTaskId())) {
            return recommendation.getParseTaskId();
        }
        if (StringUtils.hasText(recommendation.getBatchId())) {
            return recommendation.getBatchId();
        }
        return null;
    }

    private static final class DiffInput {
        private final List<String> originalUnits;
        private final List<String> recommendedUnits;
        private final String granularity;

        private DiffInput(List<String> originalUnits, List<String> recommendedUnits, String granularity) {
            this.originalUnits = originalUnits;
            this.recommendedUnits = recommendedUnits;
            this.granularity = granularity;
        }
    }

    private static final class DiffOperation {
        private final String type;
        private final String originalUnit;
        private final String recommendedUnit;
        private final int originalIndex;
        private final int recommendedIndex;

        private DiffOperation(String type,
                              String originalUnit,
                              String recommendedUnit,
                              int originalIndex,
                              int recommendedIndex) {
            this.type = type;
            this.originalUnit = originalUnit;
            this.recommendedUnit = recommendedUnit;
            this.originalIndex = originalIndex;
            this.recommendedIndex = recommendedIndex;
        }
    }

    private static final class ParseResult {
        private final boolean parsed;
        private final Map<String, Object> summary;
        private final String errorMessage;

        private ParseResult(boolean parsed, Map<String, Object> summary, String errorMessage) {
            this.parsed = parsed;
            this.summary = summary;
            this.errorMessage = errorMessage;
        }

        private static ParseResult parsed(Map<String, Object> summary) {
            return new ParseResult(true, summary, null);
        }

        private static ParseResult failed(String errorMessage) {
            Map<String, Object> summary = new LinkedHashMap<String, Object>();
            summary.put("parseStatus", "FAILED");
            return new ParseResult(false, summary, errorMessage);
        }
    }

    private static final class AstDiffResult {
        private final Map<String, Object> payload;
        private final boolean ready;

        private AstDiffResult(Map<String, Object> payload, boolean ready) {
            this.payload = payload;
            this.ready = ready;
        }
    }
}
