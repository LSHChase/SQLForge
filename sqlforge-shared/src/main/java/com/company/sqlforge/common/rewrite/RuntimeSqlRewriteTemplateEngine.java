package com.company.sqlforge.common.rewrite;

import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class RuntimeSqlRewriteTemplateEngine {

    public static final String PROGRAM_VERSION = "template-replay-v1";
    public static final String MATCH_MODE = "TEMPLATE_CONDITION_REPLAY";

    private RuntimeSqlRewriteTemplateEngine() {
    }

    public static RuntimeSqlRewriteTemplateResult rewrite(String sourceSql,
                                                          String recommendedSql,
                                                          String currentSql) {
        if (!StringUtils.hasText(sourceSql)) {
            return RuntimeSqlRewriteTemplateResult.notApplied("SOURCE_SQL_UNAVAILABLE");
        }
        if (!StringUtils.hasText(recommendedSql)) {
            return RuntimeSqlRewriteTemplateResult.notApplied("RECOMMENDED_SQL_UNAVAILABLE");
        }
        if (!StringUtils.hasText(currentSql)) {
            return RuntimeSqlRewriteTemplateResult.notApplied("CURRENT_SQL_UNAVAILABLE");
        }
        SqlShape sourceShape = SqlShape.parse(sourceSql);
        SqlShape recommendedShape = SqlShape.parse(recommendedSql);
        SqlShape currentShape = SqlShape.parse(currentSql);
        if (!sourceShape.isSupported()) {
            return RuntimeSqlRewriteTemplateResult.notApplied(sourceShape.getFailureReason());
        }
        if (!recommendedShape.isSupported()) {
            return RuntimeSqlRewriteTemplateResult.notApplied(recommendedShape.getFailureReason());
        }
        if (!currentShape.isSupported()) {
            return RuntimeSqlRewriteTemplateResult.notApplied(currentShape.getFailureReason());
        }
        if (!sourceShape.headKey().equals(currentShape.headKey())) {
            return RuntimeSqlRewriteTemplateResult.notApplied("SOURCE_HEAD_MISMATCH");
        }
        if (!sourceShape.tailKey().equals(currentShape.tailKey())) {
            return RuntimeSqlRewriteTemplateResult.notApplied("SOURCE_TAIL_MISMATCH");
        }
        String rewrittenSql = render(recommendedShape, currentShape);
        if (!StringUtils.hasText(rewrittenSql)) {
            return RuntimeSqlRewriteTemplateResult.notApplied("RENDERED_SQL_EMPTY");
        }
        return RuntimeSqlRewriteTemplateResult.applied(
            rewrittenSql,
            MATCH_MODE,
            buildProgramJson(sourceSql, recommendedSql)
        );
    }

    public static String buildProgramJson(String sourceSql, String recommendedSql) {
        Map<String, Object> program = new LinkedHashMap<String, Object>();
        SqlShape sourceShape = SqlShape.parse(sourceSql);
        SqlShape recommendedShape = SqlShape.parse(recommendedSql);
        program.put("programVersion", PROGRAM_VERSION);
        program.put("matchMode", MATCH_MODE);
        program.put("sourceSqlFingerprint", SqlFingerprintUtils.fingerprint(sourceSql));
        program.put("recommendedSqlFingerprint", SqlFingerprintUtils.fingerprint(recommendedSql));
        program.put("templateFamilyFingerprint", templateFamilyFingerprint(sourceSql));
        program.put("sourceHeadFingerprint", sourceShape.isSupported() ? SqlFingerprintUtils.fingerprint(sourceShape.getHead()) : null);
        program.put("sourceTailFingerprint", sourceShape.isSupported() ? SqlFingerprintUtils.fingerprint(sourceShape.getTail()) : null);
        program.put("sourcePredicateKeys", sourceShape.isSupported() ? sourceShape.getPredicateKeys() : new ArrayList<String>());
        program.put("recommendedPredicateKeys", recommendedShape.isSupported() ? recommendedShape.getPredicateKeys() : new ArrayList<String>());
        program.put("conditionReplayPolicy", "REPLAY_CURRENT_TOP_LEVEL_AND_PREDICATES");
        program.put("safetyBoundary", "ACTIVE_RUNTIME_BINDING_AND_TENANT_AUTH_REQUIRED");
        return JsonUtils.toJson(program);
    }

    public static String templateFamilyFingerprint(String sqlText) {
        SqlShape shape = SqlShape.parse(sqlText);
        if (!shape.isSupported()) {
            return SqlFingerprintUtils.fingerprint(sqlText);
        }
        return SqlFingerprintUtils.fingerprint(shape.getHead() + " " + shape.getTail());
    }

    private static String render(SqlShape recommendedShape, SqlShape currentShape) {
        StringBuilder builder = new StringBuilder();
        builder.append(trimRight(recommendedShape.getHead()));
        List<String> currentPredicates = currentShape.getPredicates();
        if (!currentPredicates.isEmpty()) {
            appendSpaceIfNeeded(builder);
            builder.append("WHERE ");
            for (int index = 0; index < currentPredicates.size(); index++) {
                if (index > 0) {
                    builder.append(" AND ");
                }
                builder.append(currentPredicates.get(index).trim());
            }
        }
        String currentTail = currentShape.getTail();
        String recommendedTail = recommendedShape.getTail();
        String tail = StringUtils.hasText(currentTail) ? currentTail : recommendedTail;
        if (StringUtils.hasText(tail)) {
            appendSpaceIfNeeded(builder);
            builder.append(tail.trim());
        }
        return builder.toString().trim();
    }

    private static void appendSpaceIfNeeded(StringBuilder builder) {
        if (builder.length() > 0 && !Character.isWhitespace(builder.charAt(builder.length() - 1))) {
            builder.append(' ');
        }
    }

    private static String trimRight(String value) {
        if (value == null) {
            return "";
        }
        int cursor = value.length();
        while (cursor > 0 && Character.isWhitespace(value.charAt(cursor - 1))) {
            cursor--;
        }
        return value.substring(0, cursor);
    }

    private static final class SqlShape {
        private static final String[] POST_WHERE_CLAUSES = {
            "group by",
            "having",
            "order by",
            "limit",
            "offset",
            "fetch",
            "union",
            "intersect",
            "except"
        };

        private final boolean supported;
        private final String failureReason;
        private final String head;
        private final List<String> predicates;
        private final String tail;

        private SqlShape(boolean supported,
                         String failureReason,
                         String head,
                         List<String> predicates,
                         String tail) {
            this.supported = supported;
            this.failureReason = failureReason;
            this.head = head == null ? "" : head.trim();
            this.predicates = predicates == null ? new ArrayList<String>() : predicates;
            this.tail = tail == null ? "" : tail.trim();
        }

        static SqlShape parse(String sqlText) {
            if (!StringUtils.hasText(sqlText)) {
                return unsupported("SQL_EMPTY");
            }
            String sql = trimTrailingSemicolon(stripLeadingComments(sqlText).trim());
            if (!startsWithSelect(sql)) {
                return unsupported("ONLY_SELECT_SUPPORTED");
            }
            int whereIndex = findTopLevelKeyword(sql, "where", 0);
            if (whereIndex < 0) {
                return new SqlShape(true, null, sql, new ArrayList<String>(), "");
            }
            int whereBodyStart = whereIndex + "where".length();
            int tailIndex = findFirstPostWhereClause(sql, whereBodyStart);
            String head = sql.substring(0, whereIndex);
            String whereBody = tailIndex < 0 ? sql.substring(whereBodyStart) : sql.substring(whereBodyStart, tailIndex);
            String tail = tailIndex < 0 ? "" : sql.substring(tailIndex);
            return new SqlShape(true, null, head, splitTopLevelAnd(whereBody), tail);
        }

        private static SqlShape unsupported(String reason) {
            return new SqlShape(false, reason, null, null, null);
        }

        boolean isSupported() {
            return supported;
        }

        String getFailureReason() {
            return failureReason;
        }

        String getHead() {
            return head;
        }

        List<String> getPredicates() {
            return predicates;
        }

        String getTail() {
            return tail;
        }

        List<String> getPredicateKeys() {
            List<String> keys = new ArrayList<String>(predicates.size());
            for (String predicate : predicates) {
                keys.add(SqlFingerprintUtils.normalizeForFingerprint(predicate));
            }
            return keys;
        }

        String headKey() {
            return SqlFingerprintUtils.normalizeForFingerprint(head);
        }

        String tailKey() {
            return SqlFingerprintUtils.normalizeForFingerprint(tail);
        }

        private static boolean startsWithSelect(String sql) {
            String normalized = sql.trim().toLowerCase(Locale.ROOT);
            return normalized.startsWith("select ") || "select".equals(normalized);
        }

        private static int findFirstPostWhereClause(String sql, int start) {
            int best = -1;
            for (String clause : POST_WHERE_CLAUSES) {
                int candidate = findTopLevelKeyword(sql, clause, start);
                if (candidate >= 0 && (best < 0 || candidate < best)) {
                    best = candidate;
                }
            }
            return best;
        }

        private static List<String> splitTopLevelAnd(String whereBody) {
            List<String> predicates = new ArrayList<String>();
            int cursor = 0;
            int start = 0;
            while (cursor < whereBody.length()) {
                int andIndex = findTopLevelKeyword(whereBody, "and", cursor);
                if (andIndex < 0) {
                    break;
                }
                addPredicate(predicates, whereBody.substring(start, andIndex));
                cursor = andIndex + "and".length();
                start = cursor;
            }
            addPredicate(predicates, whereBody.substring(start));
            return predicates;
        }

        private static void addPredicate(List<String> predicates, String predicate) {
            if (StringUtils.hasText(predicate)) {
                predicates.add(predicate.trim());
            }
        }

        private static String trimTrailingSemicolon(String value) {
            String result = value;
            while (result.endsWith(";")) {
                result = result.substring(0, result.length() - 1).trim();
            }
            return result;
        }

        private static String stripLeadingComments(String value) {
            String result = value == null ? "" : value.trim();
            boolean changed = true;
            while (changed) {
                changed = false;
                if (result.startsWith("--")) {
                    int newline = findLineEnd(result);
                    result = newline < 0 ? "" : result.substring(newline + 1).trim();
                    changed = true;
                } else if (result.startsWith("/*")) {
                    int close = result.indexOf("*/");
                    result = close < 0 ? "" : result.substring(close + 2).trim();
                    changed = true;
                }
            }
            return result;
        }

        private static int findLineEnd(String value) {
            int lineFeed = value.indexOf('\n');
            int carriageReturn = value.indexOf('\r');
            if (lineFeed < 0) {
                return carriageReturn;
            }
            if (carriageReturn < 0) {
                return lineFeed;
            }
            return Math.min(lineFeed, carriageReturn);
        }

        private static int findTopLevelKeyword(String sql, String keyword, int start) {
            String lowerSql = sql.toLowerCase(Locale.ROOT);
            String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
            boolean inSingleQuote = false;
            boolean inDoubleQuote = false;
            boolean inBacktick = false;
            int depth = 0;
            int index = Math.max(0, start);
            while (index <= sql.length() - lowerKeyword.length()) {
                char current = sql.charAt(index);
                char next = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';
                if (current == '\'' && !inDoubleQuote && !inBacktick) {
                    if (inSingleQuote && next == '\'') {
                        index += 2;
                        continue;
                    }
                    inSingleQuote = !inSingleQuote;
                    index++;
                    continue;
                }
                if (current == '"' && !inSingleQuote && !inBacktick) {
                    inDoubleQuote = !inDoubleQuote;
                    index++;
                    continue;
                }
                if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                    inBacktick = !inBacktick;
                    index++;
                    continue;
                }
                if (inSingleQuote || inDoubleQuote || inBacktick) {
                    index++;
                    continue;
                }
                if (current == '(') {
                    depth++;
                    index++;
                    continue;
                }
                if (current == ')') {
                    depth = Math.max(0, depth - 1);
                    index++;
                    continue;
                }
                if (depth == 0
                    && lowerSql.startsWith(lowerKeyword, index)
                    && hasKeywordBoundary(sql, index, lowerKeyword.length())) {
                    return index;
                }
                index++;
            }
            return -1;
        }

        private static boolean hasKeywordBoundary(String sql, int start, int length) {
            int before = start - 1;
            int after = start + length;
            return (before < 0 || !isIdentifierPart(sql.charAt(before)))
                && (after >= sql.length() || !isIdentifierPart(sql.charAt(after)));
        }

        private static boolean isIdentifierPart(char value) {
            return Character.isLetterOrDigit(value) || value == '_' || value == '$';
        }
    }
}
