package com.company.sqloptimization.application.service;

import com.company.sqloptimization.application.controller.vo.ReportBatchIssueLocationVO;
import com.company.sqloptimization.domain.parse.StructureParseIssue;
import com.company.sqloptimization.domain.parse.StructureParseIssueScoringSnapshot;
import com.company.sqloptimization.domain.parse.StructureParsePriorityScorer;
import com.company.sqloptimization.domain.reportbatch.ReportBatchItem;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class ReportBatchIssueLocationSupport {

    private static final int SNIPPET_RADIUS = 56;
    private static final int FALLBACK_SNIPPET_LENGTH = 120;
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern ORDER_BY_RANDOM_PATTERN =
        Pattern.compile("(?i)\\bORDER\\s+BY\\s+(RAND|RANDOM)\\s*\\(");
    private static final Pattern OR_PREDICATE_PATTERN = Pattern.compile("(?i)\\bOR\\b");
    private static final Pattern LEADING_WILDCARD_LIKE_PATTERN =
        Pattern.compile("(?i)\\bLIKE\\s+['\"]%");
    private static final Pattern FUNCTION_WRAPPED_PREDICATE_PATTERN =
        Pattern.compile("(?i)\\b(YEAR|MONTH|DAY|DATE_FORMAT|SUBSTR|UPPER|LOWER)\\s*\\(");
    private static final Pattern SUBQUERY_PATTERN = Pattern.compile("(?i)\\(\\s*SELECT\\b");
    private static final Pattern NOT_EXISTS_PATTERN = Pattern.compile("(?i)\\bNOT\\s+EXISTS\\s*\\(");
    private static final Pattern JOIN_PATTERN = Pattern.compile("(?i)\\bJOIN\\b");
    private static final Pattern FROM_PATTERN = Pattern.compile("(?i)\\bFROM\\b");
    private static final Pattern SELECT_PATTERN = Pattern.compile("(?i)\\bSELECT\\b");
    private static final Pattern WHERE_PATTERN = Pattern.compile("(?i)\\bWHERE\\b");

    private ReportBatchIssueLocationSupport() {
    }

    static List<ReportBatchIssueLocationVO> fromItem(ReportBatchItem item,
                                                     SqlParseDiagnosticSupport.Diagnostic diagnostic) {
        if (item == null) {
            return new ArrayList<ReportBatchIssueLocationVO>();
        }
        Set<String> scenes = new LinkedHashSet<String>(item.getIssueScenes());
        if (scenes.isEmpty() && diagnostic != null && hasDiagnosticEvidence(diagnostic)) {
            scenes.add("SQL_SYNTAX_INVALID");
        }
        List<ReportBatchIssueLocationVO> result = new ArrayList<ReportBatchIssueLocationVO>(scenes.size());
        for (String scene : scenes) {
            if (!StringUtils.hasText(scene)) {
                continue;
            }
            result.add(toLocation(scene, item.getSqlText(), diagnostic));
        }
        return result;
    }

    private static ReportBatchIssueLocationVO toLocation(String scene,
                                                         String sqlText,
                                                         SqlParseDiagnosticSupport.Diagnostic diagnostic) {
        StructureParseIssueScoringSnapshot snapshot = snapshot(scene);
        SnippetMatch snippet = snippetFor(scene, sqlText, diagnostic);
        ReportBatchIssueLocationVO vo = new ReportBatchIssueLocationVO();
        vo.setIssueScene(snapshot.getIssueScene());
        vo.setIssueDomain(snapshot.getIssueDomain().name());
        vo.setSeverity(snapshot.getSeverity().name());
        vo.setPriorityLevel(snapshot.getPriorityLevel().name());
        vo.setPriorityScore(Integer.valueOf(snapshot.getPriorityScore()));
        vo.setImportant(Boolean.valueOf(snapshot.isImportant()));
        vo.setUrgent(Boolean.valueOf(snapshot.isUrgent()));
        vo.setLocationSnippet(snippet.text);
        vo.setLocationSource(snippet.source);
        if (isSyntaxScene(scene) && diagnostic != null) {
            vo.setFailureLine(diagnostic.getFailureLine());
            vo.setFailureColumn(diagnostic.getFailureColumn());
            vo.setFailureOffset(diagnostic.getFailureOffset());
            vo.setFailureToken(diagnostic.getFailureToken());
        }
        return vo;
    }

    private static StructureParseIssueScoringSnapshot snapshot(String issueScene) {
        StructureParseIssue issue = new StructureParseIssue();
        issue.setIssueScene(issueScene);
        return StructureParsePriorityScorer.snapshot(issue);
    }

    private static SnippetMatch snippetFor(String scene,
                                           String sqlText,
                                           SqlParseDiagnosticSupport.Diagnostic diagnostic) {
        if (isSyntaxScene(scene) && diagnostic != null && StringUtils.hasText(diagnostic.getFailureSnippet())) {
            return new SnippetMatch(compact(diagnostic.getFailureSnippet(), FALLBACK_SNIPPET_LENGTH), "FAILURE_TOKEN");
        }
        Pattern pattern = patternFor(scene);
        if (pattern != null && StringUtils.hasText(sqlText)) {
            Matcher matcher = pattern.matcher(sqlText);
            if (matcher.find()) {
                return new SnippetMatch(compactAround(sqlText, matcher.start(), matcher.end()), "RULE_PATTERN");
            }
        }
        if (StringUtils.hasText(sqlText)) {
            return new SnippetMatch(compact(sqlText, FALLBACK_SNIPPET_LENGTH), "SQL_ROW_SNIPPET");
        }
        return new SnippetMatch(null, "UNAVAILABLE");
    }

    private static Pattern patternFor(String scene) {
        String normalized = scene == null ? "" : scene.toUpperCase();
        if (normalized.contains("ORDER_BY_RANDOM")) {
            return ORDER_BY_RANDOM_PATTERN;
        }
        if (normalized.contains("OR_PREDICATE")) {
            return OR_PREDICATE_PATTERN;
        }
        if (normalized.contains("LEADING_WILDCARD_LIKE")) {
            return LEADING_WILDCARD_LIKE_PATTERN;
        }
        if (normalized.contains("FUNCTION_WRAPPED_PREDICATE")) {
            return FUNCTION_WRAPPED_PREDICATE_PATTERN;
        }
        if (normalized.contains("NOT_EXISTS")) {
            return NOT_EXISTS_PATTERN;
        }
        if (normalized.contains("SUBQUERY")) {
            return SUBQUERY_PATTERN;
        }
        if (normalized.contains("JOIN")) {
            return JOIN_PATTERN;
        }
        if (normalized.contains("TABLE_SCAN") || normalized.contains("REPEATED_TABLE_SCAN")) {
            return FROM_PATTERN;
        }
        if (normalized.contains("PREDICATE")) {
            return WHERE_PATTERN;
        }
        if (normalized.contains("COMPLEX_QUERY_GRAPH")) {
            return SELECT_PATTERN;
        }
        return null;
    }

    private static boolean isSyntaxScene(String scene) {
        String normalized = scene == null ? "" : scene.toUpperCase();
        return normalized.contains("SYNTAX") || normalized.contains("INVALID");
    }

    private static boolean hasDiagnosticEvidence(SqlParseDiagnosticSupport.Diagnostic diagnostic) {
        return diagnostic.getFailureLine() != null
            || diagnostic.getFailureColumn() != null
            || diagnostic.getFailureOffset() != null
            || StringUtils.hasText(diagnostic.getFailureToken())
            || StringUtils.hasText(diagnostic.getFailureSnippet());
    }

    private static String compactAround(String text, int start, int end) {
        int safeStart = Math.max(0, start - SNIPPET_RADIUS);
        int safeEnd = Math.min(text.length(), end + SNIPPET_RADIUS);
        String prefix = safeStart > 0 ? "... " : "";
        String suffix = safeEnd < text.length() ? " ..." : "";
        return compact(prefix + text.substring(safeStart, safeEnd) + suffix, FALLBACK_SNIPPET_LENGTH);
    }

    private static String compact(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = WHITESPACE_PATTERN.matcher(text).replaceAll(" ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 4)).trim() + " ...";
    }

    private static final class SnippetMatch {
        private final String text;
        private final String source;

        private SnippetMatch(String text, String source) {
            this.text = text;
            this.source = source;
        }
    }
}
