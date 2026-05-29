package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RewriteCoverageFields.addCoverageField;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.containsTopLevelFunction;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.indexOfTopLevelKeyword;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.splitTopLevel;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.splitWhitespace;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.stripLeadingComments;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class RewriteCoverageSelect {

    private static final Pattern ALIAS_PATTERN =
        Pattern.compile("(?is)\\s+AS\\s+([A-Z_][A-Z0-9_$]*)\\s*$");

    private RewriteCoverageSelect() {
    }

    static Set<String> selectedOutputs(String rewriteSql) {
        if (!StringUtils.hasText(rewriteSql)) {
            return Collections.emptySet();
        }
        String selectList = selectList(rewriteSql);
        if (!StringUtils.hasText(selectList)) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> outputs = new LinkedHashSet<String>();
        for (String item : splitTopLevel(selectList, ',')) {
            addSelectedOutput(outputs, item.trim());
        }
        return outputs;
    }

    static String selectList(String sql) {
        String normalized = stripLeadingComments(sql);
        int selectIndex = indexOfTopLevelKeyword(normalized, "SELECT", 0);
        if (selectIndex < 0) {
            return "";
        }
        int fromIndex = indexOfTopLevelKeyword(normalized, "FROM", selectIndex + "SELECT".length());
        if (fromIndex < 0 || fromIndex <= selectIndex) {
            return "";
        }
        return normalized.substring(selectIndex + "SELECT".length(), fromIndex).trim();
    }

    static boolean containsSelectStar(String sql) {
        String selectList = selectList(sql);
        if (!StringUtils.hasText(selectList)) {
            return false;
        }
        for (String item : splitTopLevel(selectList, ',')) {
            String normalized = item.trim().replaceAll("\\s+", " ");
            if ("*".equals(normalized) || normalized.matches("(?is)[A-Z_][A-Z0-9_$]*\\.\\*")) {
                return true;
            }
        }
        return false;
    }

    private static void addSelectedOutput(Set<String> outputs, String expression) {
        Matcher asMatcher = ALIAS_PATTERN.matcher(expression);
        if (asMatcher.find()) {
            addCoverageField(outputs, asMatcher.group(1));
            addCoverageField(outputs, expression.substring(0, asMatcher.start()).trim());
            return;
        }
        List<String> tokens = splitWhitespace(expression);
        if (tokens.size() > 1 && !containsTopLevelFunction(expression)) {
            addCoverageField(outputs, tokens.get(tokens.size() - 1));
        }
        addCoverageField(outputs, expression);
    }
}
