package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.cleanReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

final class PrejoinSqlJoinTableScanner {

    private PrejoinSqlJoinTableScanner() {
    }

    static List<Map<String, Object>> joinRightTables(String sourceSql) {
        if (!StringUtils.hasText(sourceSql)) {
            return Collections.emptyList();
        }
        String normalized = sourceSql.replaceAll("\\s+", " ").trim();
        String upper = normalized.toUpperCase(Locale.ROOT);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        int searchFrom = 0;
        while (searchFrom < normalized.length()) {
            int joinIndex = upper.indexOf(" JOIN ", searchFrom);
            if (joinIndex < 0) {
                break;
            }
            PrejoinToken tableToken = readToken(normalized, joinIndex + " JOIN ".length());
            if (!StringUtils.hasText(tableToken.value)) {
                searchFrom = joinIndex + " JOIN ".length();
                continue;
            }
            PrejoinToken next = readToken(normalized, tableToken.endIndex);
            String alias = "";
            if ("AS".equalsIgnoreCase(next.value)) {
                PrejoinToken aliasToken = readToken(normalized, next.endIndex);
                alias = aliasToken.value;
                next = readToken(normalized, aliasToken.endIndex);
            } else if (!"ON".equalsIgnoreCase(next.value)) {
                alias = next.value;
                next = readToken(normalized, next.endIndex);
            }
            if (!"ON".equalsIgnoreCase(next.value)) {
                searchFrom = tableToken.endIndex;
                continue;
            }
            LinkedHashMap<String, Object> tableMap = new LinkedHashMap<String, Object>();
            tableMap.put("tableName", cleanTablePath(tableToken.value));
            tableMap.put("alias", cleanReference(alias));
            tableMap.put("sourceType", "BASE_TABLE");
            result.add(tableMap);
            searchFrom = next.endIndex;
        }
        return result;
    }

    private static PrejoinToken readToken(String text, int startIndex) {
        int index = Math.max(0, startIndex);
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        int start = index;
        while (index < text.length() && !Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return new PrejoinToken(start >= text.length() ? "" : text.substring(start, index), index);
    }

    private static String cleanTablePath(String value) {
        return cleanReference(value).replaceAll("\\s*\\.\\s*", ".");
    }
}
