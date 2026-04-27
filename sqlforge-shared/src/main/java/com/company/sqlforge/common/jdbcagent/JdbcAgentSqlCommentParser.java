package com.company.sqlforge.common.jdbcagent;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class JdbcAgentSqlCommentParser {

    private JdbcAgentSqlCommentParser() {
    }

    public static Map<String, String> parseLeadingComments(String sqlText) {
        Map<String, String> commentContext = new LinkedHashMap<String, String>();
        if (!StringUtils.hasText(sqlText)) {
            return commentContext;
        }
        String[] lines = sqlText.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (!trimmed.startsWith("--")) {
                break;
            }
            String payload = trimmed.substring(2).trim();
            if (!StringUtils.hasText(payload)) {
                continue;
            }
            int separator = payload.indexOf('=');
            if (separator <= 0 || separator >= payload.length() - 1) {
                continue;
            }
            String key = payload.substring(0, separator).trim();
            String value = payload.substring(separator + 1).trim();
            if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                commentContext.put(key, value);
            }
        }
        return commentContext;
    }
}
