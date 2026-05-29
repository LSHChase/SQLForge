package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RewriteCoverageProfileValues.mapList;
import static com.company.sqloptimization.application.service.RewriteCoverageProfileValues.text;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.cleanReference;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.stripSqlComments;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.stripStringLiterals;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.unqualifiedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class RewriteCoverageRelations {

    private static final Pattern RELATION_PATTERN = Pattern.compile(
        "(?i)\\b(FROM|JOIN)\\s+((?:[`\\\"][^`\\\"]+[`\\\"]|[A-Z_][A-Z0-9_$]*)(?:\\s*\\.\\s*(?:[`\\\"][^`\\\"]+[`\\\"]|[A-Z_][A-Z0-9_$]*))*)"
    );

    private RewriteCoverageRelations() {
    }

    static List<String> accessedOriginalSources(String rewriteSql,
                                                String mvName,
                                                Set<String> originalSources) {
        if (!StringUtils.hasText(rewriteSql) || originalSources.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (String relation : fromJoinRelations(rewriteSql)) {
            if (sameRelation(relation, mvName)) {
                continue;
            }
            String normalized = relationKey(relation);
            String unqualified = relationKey(unqualifiedName(relation));
            if (originalSources.contains(normalized) || originalSources.contains(unqualified)) {
                result.add(cleanReference(relation));
            }
        }
        return new ArrayList<String>(result);
    }

    static boolean referencesMv(String rewriteSql, String mvName) {
        if (!StringUtils.hasText(rewriteSql) || !StringUtils.hasText(mvName)) {
            return false;
        }
        for (String relation : fromJoinRelations(rewriteSql)) {
            if (sameRelation(relation, mvName)) {
                return true;
            }
        }
        return false;
    }

    static Set<String> originalBaseSources(Map<String, Object> advancedStructureProfile) {
        LinkedHashSet<String> sources = new LinkedHashSet<String>();
        for (Map<String, Object> table : mapList(advancedStructureProfile.get("tables"))) {
            String sourceType = text(table.get("sourceType"));
            if (StringUtils.hasText(sourceType) && !"BASE_TABLE".equals(sourceType)) {
                continue;
            }
            addRelationKey(sources, text(table.get("tableName")));
        }
        return sources;
    }

    private static List<String> fromJoinRelations(String sql) {
        if (!StringUtils.hasText(sql)) {
            return Collections.emptyList();
        }
        String withoutStrings = stripStringLiterals(stripSqlComments(sql));
        Matcher matcher = RELATION_PATTERN.matcher(withoutStrings);
        List<String> relations = new ArrayList<String>();
        while (matcher.find()) {
            relations.add(cleanRelationToken(matcher.group(2)));
        }
        return relations;
    }

    private static void addRelationKey(Set<String> sources, String relation) {
        if (!StringUtils.hasText(relation)) {
            return;
        }
        sources.add(relationKey(relation));
        sources.add(relationKey(unqualifiedName(relation)));
    }

    private static boolean sameRelation(String left, String right) {
        return StringUtils.hasText(left)
            && StringUtils.hasText(right)
            && (relationKey(left).equals(relationKey(right))
            || relationKey(unqualifiedName(left)).equals(relationKey(unqualifiedName(right))));
    }

    private static String relationKey(String value) {
        return cleanReference(value).replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private static String cleanRelationToken(String value) {
        String cleaned = cleanReference(value);
        while (cleaned.endsWith(")") || cleaned.endsWith(",")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        return cleaned;
    }
}
