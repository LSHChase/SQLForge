package com.company.sqloptimization.application.service;

import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class SqlParseDiagnosticSupport {

    private static final Pattern LINE_PATTERN = Pattern.compile("\\bline=(\\d+)");
    private static final Pattern COLUMN_PATTERN = Pattern.compile("\\bcol=(\\d+)");
    private static final Pattern OFFSET_PATTERN = Pattern.compile("\\boffset=(\\d+)");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\btoken=([^\\s]+)");
    private static final Pattern NEAR_PATTERN = Pattern.compile("\\bnear=(.+)$");

    private SqlParseDiagnosticSupport() {
    }

    static String buildStructureFailureReason(StructureParseResponseVO structureParse, int limit) {
        StringBuilder builder = new StringBuilder("STRUCTURE_PARSE_INVALID");
        if (structureParse == null) {
            return builder.toString();
        }
        if (structureParse.getFailureLine() != null) {
            builder.append(" line=").append(structureParse.getFailureLine());
        }
        if (structureParse.getFailureColumn() != null) {
            builder.append(" col=").append(structureParse.getFailureColumn());
        }
        if (structureParse.getFailureOffset() != null) {
            builder.append(" offset=").append(structureParse.getFailureOffset());
        }
        if (StringUtils.hasText(structureParse.getFailureToken())) {
            builder.append(" token=").append(compactDiagnosticText(structureParse.getFailureToken(), 24));
        }
        if (StringUtils.hasText(structureParse.getFailureSnippet())) {
            builder.append(" near=").append(compactDiagnosticText(structureParse.getFailureSnippet(), 48));
        }
        if (builder.length() == "STRUCTURE_PARSE_INVALID".length()
            && StringUtils.hasText(structureParse.getFailureReason())) {
            builder.append(" reason=").append(compactDiagnosticText(structureParse.getFailureReason(), 80));
        }
        return compactDiagnosticText(builder.toString(), limit);
    }

    static Diagnostic fromStructureParse(StructureParseResponseVO structureParse) {
        Diagnostic diagnostic = new Diagnostic();
        if (structureParse == null) {
            return diagnostic;
        }
        diagnostic.failureLine = structureParse.getFailureLine();
        diagnostic.failureColumn = structureParse.getFailureColumn();
        diagnostic.failureOffset = structureParse.getFailureOffset();
        diagnostic.failureToken = trimToNull(structureParse.getFailureToken());
        diagnostic.failureSnippet = trimToNull(structureParse.getFailureSnippet());
        return diagnostic;
    }

    static Diagnostic fromFailureReason(String failureReason) {
        Diagnostic diagnostic = new Diagnostic();
        String normalized = trimToNull(failureReason);
        if (!StringUtils.hasText(normalized)) {
            return diagnostic;
        }
        diagnostic.failureLine = findInteger(LINE_PATTERN, normalized);
        diagnostic.failureColumn = findInteger(COLUMN_PATTERN, normalized);
        diagnostic.failureOffset = findInteger(OFFSET_PATTERN, normalized);
        diagnostic.failureToken = findString(TOKEN_PATTERN, normalized);
        diagnostic.failureSnippet = findString(NEAR_PATTERN, normalized);
        return diagnostic;
    }

    static String compactDiagnosticText(String value, int limit) {
        String compact = trimToNull(value == null ? null : value.replace('\n', ' ').replace('\r', ' '));
        if (compact == null || compact.length() <= limit) {
            return compact;
        }
        return compact.substring(0, Math.max(0, limit - 3)) + "...";
    }

    private static Integer findInteger(Pattern pattern, String value) {
        String match = findString(pattern, value);
        if (!StringUtils.hasText(match)) {
            return null;
        }
        try {
            return Integer.valueOf(match);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String findString(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? trimToNull(matcher.group(1)) : null;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    static final class Diagnostic {
        private Integer failureLine;
        private Integer failureColumn;
        private Integer failureOffset;
        private String failureToken;
        private String failureSnippet;

        Integer getFailureLine() {
            return failureLine;
        }

        Integer getFailureColumn() {
            return failureColumn;
        }

        Integer getFailureOffset() {
            return failureOffset;
        }

        String getFailureToken() {
            return failureToken;
        }

        String getFailureSnippet() {
            return failureSnippet;
        }
    }
}
