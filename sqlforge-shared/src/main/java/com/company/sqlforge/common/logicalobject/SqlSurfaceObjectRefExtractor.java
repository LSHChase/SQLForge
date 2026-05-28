package com.company.sqlforge.common.logicalobject;

import com.company.sqlforge.common.utils.SqlCatalogQualifierRewriteUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class SqlSurfaceObjectRefExtractor {

    private static final Pattern SURFACE_OBJECT_PATTERN = Pattern.compile(
        "(?is)\\b(?:FROM|JOIN|INTO|UPDATE)\\s+"
            + "((?:\"[^\"]+\"|`[^`]+`|\\[[^\\]]+\\]|[A-Za-z_][A-Za-z0-9_$]*)"
            + "(?:\\s*\\.\\s*(?:\"[^\"]+\"|`[^`]+`|\\[[^\\]]+\\]|[A-Za-z_][A-Za-z0-9_$]*)){0,2})"
    );

    private SqlSurfaceObjectRefExtractor() {
    }

    public static List<LogicalObjectSurface> extractSurfaceRefs(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return Collections.emptyList();
        }
        String compatibleSql = SqlCatalogQualifierRewriteUtils.rewriteBiViewCatalogQualifier(sqlText);
        String scanSql = maskSingleQuotedLiterals(stripSqlComments(compatibleSql));
        List<LogicalObjectSurface> refs = new ArrayList<LogicalObjectSurface>();
        Set<String> seenKeys = new LinkedHashSet<String>();
        Matcher matcher = SURFACE_OBJECT_PATTERN.matcher(scanSql);
        while (matcher.find()) {
            String objectName = normalizeObjectReference(matcher.group(1));
            if (!isLikelyObjectName(objectName)) {
                continue;
            }
            LogicalObjectSurface surface = toSurface(objectName);
            if (surface.getObjectKey() == null || seenKeys.contains(surface.getObjectKey())) {
                continue;
            }
            seenKeys.add(surface.getObjectKey());
            refs.add(surface);
        }
        return refs;
    }

    public static List<String> extractSurfaceObjectNames(String sqlText) {
        return surfaceObjectNames(extractSurfaceRefs(sqlText));
    }

    public static List<String> surfaceObjectNames(List<LogicalObjectSurface> refs) {
        if (refs == null || refs.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> names = new LinkedHashSet<String>();
        for (LogicalObjectSurface ref : refs) {
            String objectName = normalizeObjectReference(ref == null ? null : ref.getObjectName());
            if (objectName != null) {
                names.add(objectName.toLowerCase(Locale.ROOT));
            }
        }
        return new ArrayList<String>(names);
    }

    public static List<String> normalizeObjectNames(List<String> objectNames) {
        if (objectNames == null || objectNames.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> names = new LinkedHashSet<String>();
        for (String objectName : objectNames) {
            String normalized = normalizeObjectReference(objectName);
            if (normalized != null) {
                names.add(normalized.toLowerCase(Locale.ROOT));
            }
        }
        return new ArrayList<String>(names);
    }

    private static LogicalObjectSurface toSurface(String objectName) {
        LogicalObjectType objectType = resolveObjectType(objectName);
        String[] parts = splitQualifiedName(objectName);
        LogicalObjectSurface surface = new LogicalObjectSurface();
        surface.setObjectType(objectType.name());
        surface.setObjectName(objectName);
        surface.setObjectKey(LogicalObjectRef.buildObjectKey(objectType, objectName));
        if (parts.length >= 3) {
            surface.setCatalogName(parts[0]);
            surface.setSchemaName(parts[1]);
        } else if (parts.length == 2) {
            surface.setSchemaName(parts[0]);
        }
        surface.setMatchSource("SQL_SURFACE_TOKEN");
        surface.setResolved(Boolean.TRUE);
        surface.setMappedPhysicalTargets(Collections.<String>emptyList());
        return surface;
    }

    private static LogicalObjectType resolveObjectType(String objectName) {
        String lower = objectName == null ? "" : objectName.toLowerCase(Locale.ROOT);
        if (lower.startsWith("business_view")
            || lower.contains(".business_view.")
            || lower.endsWith("_logic")
            || lower.contains("customer_360")) {
            return LogicalObjectType.BUSINESS_VIEW;
        }
        if (lower.startsWith("vw_")
            || lower.contains(".vw_")
            || lower.endsWith("_view")
            || lower.contains(".view.")) {
            return LogicalObjectType.DB_VIEW;
        }
        return LogicalObjectType.TABLE;
    }

    private static String normalizeObjectReference(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String compatible = SqlCatalogQualifierRewriteUtils.rewriteBiViewCatalogQualifier(value);
        String normalized = compatible.trim().replaceAll("\\s*\\.\\s*", ".");
        while (normalized.endsWith(",") || normalized.endsWith(")") || normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        String[] parts = normalized.split("\\.");
        List<String> unquoted = new ArrayList<String>(parts.length);
        for (String part : parts) {
            String item = unquoteIdentifier(part);
            if (!StringUtils.hasText(item)) {
                return null;
            }
            unquoted.add(item.trim());
        }
        return String.join(".", unquoted);
    }

    private static String unquoteIdentifier(String value) {
        String normalized = value == null ? null : value.trim();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
            || (normalized.startsWith("`") && normalized.endsWith("`"))
            || (normalized.startsWith("[") && normalized.endsWith("]"))) {
            return normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private static boolean isLikelyObjectName(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            return false;
        }
        String upper = objectName.toUpperCase(Locale.ROOT);
        String firstToken = upper.split("\\s+", 2)[0];
        return !"SELECT".equals(firstToken)
            && !"WHERE".equals(firstToken)
            && !"JOIN".equals(firstToken)
            && !"ON".equals(firstToken)
            && !"GROUP".equals(firstToken)
            && !"ORDER".equals(firstToken)
            && !"HAVING".equals(firstToken)
            && !"LIMIT".equals(firstToken)
            && !"UNNEST".equals(firstToken)
            && !"VALUES".equals(firstToken)
            && !"LATERAL".equals(firstToken);
    }

    private static String[] splitQualifiedName(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            return new String[0];
        }
        String[] parts = objectName.split("\\.");
        if (parts.length >= 3) {
            return new String[] {parts[0], parts[1], parts[2]};
        }
        if (parts.length == 2) {
            return new String[] {parts[0], parts[1]};
        }
        return new String[] {parts[0]};
    }

    private static String stripSqlComments(String sqlText) {
        StringBuilder builder = new StringBuilder(sqlText.length());
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        int index = 0;
        while (index < sqlText.length()) {
            char current = sqlText.charAt(index);
            char next = index + 1 < sqlText.length() ? sqlText.charAt(index + 1) : '\0';
            if (!inSingleQuote && !inDoubleQuote && !inBacktick && current == '-' && next == '-') {
                builder.append(' ');
                index += 2;
                while (index < sqlText.length()) {
                    char item = sqlText.charAt(index);
                    if (item == '\n' || item == '\r') {
                        builder.append(item);
                        index++;
                        break;
                    }
                    builder.append(' ');
                    index++;
                }
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && !inBacktick && current == '/' && next == '*') {
                builder.append(' ');
                index += 2;
                while (index < sqlText.length()) {
                    char item = sqlText.charAt(index);
                    char following = index + 1 < sqlText.length() ? sqlText.charAt(index + 1) : '\0';
                    builder.append(item == '\n' || item == '\r' ? item : ' ');
                    index++;
                    if (item == '*' && following == '/') {
                        builder.append(' ');
                        index++;
                        break;
                    }
                }
                continue;
            }
            builder.append(current);
            if (current == '\'' && !inDoubleQuote && !inBacktick) {
                if (inSingleQuote && next == '\'') {
                    builder.append(next);
                    index += 2;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
            } else if (current == '"' && !inSingleQuote && !inBacktick) {
                inDoubleQuote = !inDoubleQuote;
            } else if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
            }
            index++;
        }
        return builder.toString();
    }

    private static String maskSingleQuotedLiterals(String sqlText) {
        StringBuilder builder = new StringBuilder(sqlText.length());
        boolean inSingleQuote = false;
        for (int index = 0; index < sqlText.length(); index++) {
            char current = sqlText.charAt(index);
            char next = index + 1 < sqlText.length() ? sqlText.charAt(index + 1) : '\0';
            if (current == '\'') {
                builder.append(' ');
                if (inSingleQuote && next == '\'') {
                    builder.append(' ');
                    index++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                continue;
            }
            builder.append(inSingleQuote && current != '\n' && current != '\r' ? ' ' : current);
        }
        return builder.toString();
    }
}
