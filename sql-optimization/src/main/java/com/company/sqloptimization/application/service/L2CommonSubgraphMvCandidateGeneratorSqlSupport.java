package com.company.sqloptimization.application.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;
import org.springframework.util.StringUtils;

abstract class L2CommonSubgraphMvCandidateGeneratorSqlSupport {

    protected static final Pattern QUALIFIED_COLUMN_PATTERN =
        Pattern.compile("(?i)\\b([A-Z_][A-Z0-9_$]*)\\.([A-Z_][A-Z0-9_$]*)\\b");
    protected static final Pattern QUOTED_QUALIFIED_COLUMN_PATTERN =
        Pattern.compile("\"([^\"]+)\"\\s*\\.\\s*\"([^\"]+)\"");
    protected static final Pattern IDENTIFIER_PATTERN =
        Pattern.compile("(?i)\\b[A-Z_][A-Z0-9_$]*\\b");
    protected static final Pattern RELATION_PATTERN = Pattern.compile(
        "(?i)\\b(FROM|JOIN)\\s+((?:[`\\\"][^`\\\"]+[`\\\"]|[A-Z_][A-Z0-9_$]*)(?:\\s*\\.\\s*(?:[`\\\"][^`\\\"]+[`\\\"]|[A-Z_][A-Z0-9_$]*))*)"
    );
    protected static final Set<String> SQL_KEYWORDS = new LinkedHashSet<String>(Arrays.asList(
        "SELECT", "FROM", "WHERE", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER", "ON", "AS",
        "AND", "OR", "NOT", "GROUP", "BY", "HAVING", "ORDER", "LIMIT", "BETWEEN", "IN", "IS",
        "NULL", "DATE", "TIMESTAMP", "CASE", "WHEN", "THEN", "ELSE", "END", "DISTINCT", "TRUE",
        "FALSE", "WITH", "UNION", "ALL", "CAST", "OVER", "PARTITION"
    ));
    protected static final Set<String> SQL_FUNCTIONS = new LinkedHashSet<String>(Arrays.asList(
        "SUM", "COUNT", "MIN", "MAX", "AVG", "DATE_TRUNC", "TRUNC", "DATE_FORMAT", "YEAR", "MONTH",
        "DAY", "COALESCE", "NULLIF", "ROUND", "LOWER", "UPPER"
    ));
    protected static final Set<String> NON_DETERMINISTIC_FUNCTION_NAMES = new LinkedHashSet<String>(Arrays.asList(
        "RAND", "RANDOM", "UUID", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "LOCALTIME",
        "LOCALTIMESTAMP", "NOW", "SYSDATE", "SESSION_USER", "CURRENT_USER"
    ));
    protected static final int FINGERPRINT_CACHE_MAX_SIZE = 1024;
    protected static final Map<String, String> SUBGRAPH_FINGERPRINT_CACHE =
        Collections.synchronizedMap(new LinkedHashMap<String, String>(FINGERPRINT_CACHE_MAX_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > FINGERPRINT_CACHE_MAX_SIZE;
            }
        });

    protected static SqlNode parseStatement(String sql) {
        return L2CommonSubgraphMvCandidateGeneratorRewriteSupport.parseStatement(sql);
    }

    protected static boolean isSelectLike(SqlNode relation) {
        return L2CommonSubgraphMvCandidateGeneratorRewriteSupport.isSelectLike(relation);
    }

    protected static boolean canReplaceByAliasCoverage(String innerSql, SubgraphCandidate candidate) {
        return L2CommonSubgraphMvCandidateGeneratorRewriteSupport.canReplaceByAliasCoverage(innerSql, candidate);
    }

    protected static boolean containsRelationName(Set<String> names, String relationName) {
        return L2CommonSubgraphMvCandidateGenerator.containsRelationName(names, relationName);
    }

    protected static String relationKey(String value) {
        return L2CommonSubgraphMvCandidateGenerator.relationKey(value);
    }

    protected static String unqualifiedName(String expression) {
        return L2CommonSubgraphMvCandidateGenerator.unqualifiedName(expression);
    }

    protected static void collectRelationReferences(SqlNode node, Set<String> relations) {
        L2CommonSubgraphMvCandidateGenerator.collectRelationReferences(node, relations);
    }

    protected static List<SubgraphCandidate> subgraphCandidates(Map<String, Object> advancedStructureProfile) {
        return L2CommonSubgraphMvCandidateGenerator.subgraphCandidates(advancedStructureProfile);
    }

    protected static String extractMainQueryAfterWith(String sourceSql) {
        String sql = trimTrailingSemicolon(sourceSql);
        if (!StringUtils.hasText(sql) || !sql.trim().toUpperCase(Locale.ROOT).startsWith("WITH ")) {
            return sql;
        }
        int depth = 0;
        boolean seenCteBody = false;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (current == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (current == '(') {
                depth++;
                seenCteBody = true;
            } else if (current == ')') {
                depth--;
                if (seenCteBody && depth == 0) {
                    int cursor = i + 1;
                    while (cursor < sql.length()
                        && (Character.isWhitespace(sql.charAt(cursor)) || sql.charAt(cursor) == ',')) {
                        cursor++;
                    }
                    if (startsWithWord(sql, cursor, "SELECT")) {
                        return sql.substring(cursor).trim();
                    }
                }
            }
        }
        return sql;
    }

    protected static boolean startsWithWord(String sql, int offset, String word) {
        if (sql == null || offset < 0 || offset + word.length() > sql.length()) {
            return false;
        }
        if (!sql.regionMatches(true, offset, word, 0, word.length())) {
            return false;
        }
        int end = offset + word.length();
        return end >= sql.length() || !Character.isLetterOrDigit(sql.charAt(end));
    }

    protected static boolean hasOrderOrLimit(String sql) {
        String normalized = stripStringLiterals(sql).toUpperCase(Locale.ROOT);
        return normalized.matches("(?is).*\\bORDER\\s+BY\\b.*")
            || normalized.matches("(?is).*\\bLIMIT\\b.*");
    }

    protected static String normalizeSubgraphSql(String sql) {
        String normalized = stripOuterParentheses(trimTrailingSemicolon(sql));
        return trimTrailingSemicolon(normalized);
    }

    protected static String removeTrailingAlias(String sql, String alias) {
        if (!StringUtils.hasText(sql) || !StringUtils.hasText(alias)) {
            return sql;
        }
        return sql.replaceFirst("(?is)\\s+(?:AS\\s+)?"
            + Pattern.quote(alias)
            + "\\s*$", "");
    }

    protected static String stripOuterParentheses(String sql) {
        String normalized = sql == null ? "" : sql.trim();
        while (normalized.startsWith("(") && normalized.endsWith(")") && wrapsWholeExpression(normalized)) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }

    protected static boolean wrapsWholeExpression(String value) {
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (current == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth == 0 && i < value.length() - 1) {
                    return false;
                }
            }
        }
        return depth == 0;
    }

    protected static String subgraphFingerprint(String sql) {
        String key = trimTrailingSemicolon(sql);
        String cached = SUBGRAPH_FINGERPRINT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        String fingerprint = shortSha256(canonicalSql(key));
        SUBGRAPH_FINGERPRINT_CACHE.put(key, fingerprint);
        return fingerprint;
    }

    protected static String canonicalSql(String sql) {
        try {
            SqlNode parsed = parseStatement(sql);
            String canonical = canonicalSqlText(parsed.toString());
            LinkedHashMap<String, String> relationAliases = relationAliasReplacements(parsed);
            List<String> aliases = new ArrayList<String>(relationAliases.keySet());
            Collections.sort(aliases, new java.util.Comparator<String>() {
                @Override
                public int compare(String left, String right) {
                    return Integer.compare(right.length(), left.length());
                }
            });
            for (String alias : aliases) {
                canonical = replaceRelationAliasReferences(canonical, alias, relationAliases.get(alias));
            }
            return canonical;
        } catch (RuntimeException ex) {
            return canonicalSqlText(sql);
        }
    }

    protected static String canonicalSqlText(String sql) {
        return trimTrailingSemicolon(sql)
            .replace("`", "")
            .replace("\"", "")
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
            .replaceAll("\\s+", " ")
            .toUpperCase(Locale.ROOT);
    }

    protected static LinkedHashMap<String, String> relationAliasReplacements(SqlNode node) {
        LinkedHashSet<String> aliases = new LinkedHashSet<String>();
        collectRelationAliases(node, aliases);
        LinkedHashMap<String, String> replacements = new LinkedHashMap<String, String>();
        int index = 0;
        for (String alias : aliases) {
            String normalized = canonicalSqlText(cleanIdentifier(alias));
            if (!StringUtils.hasText(normalized) || replacements.containsKey(normalized)) {
                continue;
            }
            replacements.put(normalized, "__REL_ALIAS_" + index + "__");
            index++;
        }
        return replacements;
    }

    protected static void collectRelationAliases(SqlNode node, Set<String> aliases) {
        if (node == null) {
            return;
        }
        if (node instanceof SqlNodeList) {
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                collectRelationAliases(item, aliases);
            }
            return;
        }
        if (node instanceof SqlOrderBy) {
            SqlOrderBy orderBy = (SqlOrderBy) node;
            collectRelationAliases(orderBy.query, aliases);
            collectRelationAliases(orderBy.orderList, aliases);
            return;
        }
        if (node instanceof SqlWith) {
            SqlWith with = (SqlWith) node;
            if (with.withList != null) {
                for (SqlNode item : with.withList.getList()) {
                    if (item instanceof SqlWithItem) {
                        SqlWithItem withItem = (SqlWithItem) item;
                        if (withItem.name != null) {
                            addIfText(aliases, withItem.name.toString());
                        }
                        collectRelationAliases(withItem.query, aliases);
                    }
                }
            }
            collectRelationAliases(with.body, aliases);
            return;
        }
        if (node instanceof SqlSelect) {
            SqlSelect select = (SqlSelect) node;
            collectRelationAliasesFrom(select.getFrom(), aliases);
            collectRelationAliases(select.getWhere(), aliases);
            collectRelationAliases(select.getHaving(), aliases);
            collectRelationAliases(select.getSelectList(), aliases);
            collectRelationAliases(select.getOrderList(), aliases);
            return;
        }
        if (node instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                collectRelationAliases(operand, aliases);
            }
        }
    }

    protected static void collectRelationAliasesFrom(SqlNode from, Set<String> aliases) {
        if (from == null) {
            return;
        }
        if (from instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) from;
            collectRelationAliasesFrom(join.getLeft(), aliases);
            collectRelationAliasesFrom(join.getRight(), aliases);
            return;
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
            if (operands.size() >= 2) {
                addIfText(aliases, operands.get(1).toString());
                collectRelationAliases(operands.get(0), aliases);
            }
            return;
        }
        collectRelationAliases(from, aliases);
    }

    protected static String replaceRelationAliasReferences(String canonicalSql,
                                                         String alias,
                                                         String replacement) {
        if (!StringUtils.hasText(canonicalSql)
            || !StringUtils.hasText(alias)
            || !StringUtils.hasText(replacement)) {
            return canonicalSql;
        }
        String boundaryBefore = "(?<![\\p{L}\\p{N}_$])";
        String boundaryAfter = "(?![\\p{L}\\p{N}_$])";
        String quotedAlias = Pattern.quote(alias);
        String result = canonicalSql.replaceAll(
            boundaryBefore + quotedAlias + "\\s*\\.",
            replacement + "."
        );
        return result.replaceAll(
            "(?i)(\\bAS\\s+)" + quotedAlias + boundaryAfter,
            "$1" + replacement
        );
    }

    protected static String shortSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < bytes.length && i < 8; i++) {
                String hex = Integer.toHexString(bytes[i] & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString((value == null ? "" : value).hashCode());
        }
    }

    protected static String stripStringLiterals(String sql) {
        if (sql == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(sql.length());
        boolean inSingleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\'') {
                if (inSingleQuote && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    builder.append(' ');
                    i++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                builder.append(' ');
                continue;
            }
            builder.append(inSingleQuote ? ' ' : current);
        }
        return builder.toString();
    }

    protected static String stripSingleQuotedLiterals(String sql) {
        if (sql == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(sql.length());
        boolean inSingleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\'') {
                inSingleQuote = !inSingleQuote;
                builder.append(' ');
                continue;
            }
            builder.append(inSingleQuote ? ' ' : current);
        }
        return builder.toString();
    }

    protected static String stripDoubleQuotedIdentifiers(String sql) {
        return sql == null ? "" : sql.replaceAll("\"[^\"]+\"", " ");
    }

    protected static String firstSelectList(String sql) {
        String normalized = trimTrailingSemicolon(stripOuterParentheses(sql));
        int selectIndex = findKeywordAtDepth(normalized, "SELECT", 0);
        if (selectIndex < 0) {
            return "";
        }
        int fromIndex = findKeywordAtDepth(normalized, "FROM", selectIndex + "SELECT".length());
        if (fromIndex < 0 || fromIndex <= selectIndex) {
            return "";
        }
        return normalized.substring(selectIndex + "SELECT".length(), fromIndex).trim();
    }

    protected static int findKeywordAtDepth(String sql, String keyword, int offset) {
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktickQuote = false;
        for (int i = Math.max(0, offset); i <= sql.length() - keyword.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\'' && !inDoubleQuote && !inBacktickQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (current == '"' && !inSingleQuote && !inBacktickQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktickQuote = !inBacktickQuote;
                continue;
            }
            if (inSingleQuote || inDoubleQuote || inBacktickQuote) {
                continue;
            }
            if (current == '(') {
                depth++;
                continue;
            }
            if (current == ')') {
                depth = Math.max(0, depth - 1);
                continue;
            }
            if (depth == 0 && startsWithWord(sql, i, keyword)) {
                return i;
            }
        }
        return -1;
    }

    protected static List<String> splitTopLevelComma(String value) {
        List<String> result = new ArrayList<String>();
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktickQuote = false;
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\'' && !inDoubleQuote && !inBacktickQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (current == '"' && !inSingleQuote && !inBacktickQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktickQuote = !inBacktickQuote;
            } else if (!inSingleQuote && !inDoubleQuote && !inBacktickQuote) {
                if (current == '(') {
                    depth++;
                } else if (current == ')') {
                    depth = Math.max(0, depth - 1);
                } else if (current == ',' && depth == 0) {
                    result.add(value.substring(start, i).trim());
                    start = i + 1;
                }
            }
        }
        result.add(value.substring(start).trim());
        return result;
    }

    protected static String trailingAlias(String item) {
        Matcher quoted = Pattern.compile("(?is)\\s+AS\\s+(?:\"([^\"]+)\"|`([^`]+)`)\\s*$")
            .matcher(item == null ? "" : item);
        if (quoted.find()) {
            return firstText(quoted.group(1), quoted.group(2));
        }
        Matcher unquoted = Pattern.compile("(?is)\\s+AS\\s+([\\p{L}_][\\p{L}\\p{N}_$]*)\\s*$")
            .matcher(item == null ? "" : item);
        return unquoted.find() ? unquoted.group(1) : "";
    }

    protected static String trailingColumnName(String item) {
        Matcher quoted = Pattern.compile("(?:\"([^\"]+)\"|`([^`]+)`)\\s*$").matcher(item == null ? "" : item);
        if (quoted.find()) {
            return firstText(quoted.group(1), quoted.group(2));
        }
        Matcher unquoted = Pattern.compile("(?is)([\\p{L}_][\\p{L}\\p{N}_$]*)\\s*$")
            .matcher(item == null ? "" : item);
        return unquoted.find() ? unquoted.group(1) : "";
    }

    protected static String quotedIdentifierPattern(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return "\"[^\"]+\"|[A-Z_][A-Z0-9_$]*";
        }
        return "(?:\"" + Pattern.quote(identifier) + "\"|" + Pattern.quote(identifier) + ")";
    }

    protected static Map<String, Object> reason(String code, String description) {
        LinkedHashMap<String, Object> reason = new LinkedHashMap<String, Object>();
        reason.put("code", code);
        reason.put("description", description);
        return reason;
    }

    @SuppressWarnings("unchecked")
    protected static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?>)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map<?, ?>) {
                result.add((Map<String, Object>) item);
            }
        }
        return result;
    }

    protected static String trimTrailingSemicolon(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "";
        }
        String trimmed = sql.trim();
        while (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    protected static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    protected static String upperText(Object value) {
        return text(value).toUpperCase(Locale.ROOT);
    }

    protected static String cleanIdentifier(String value) {
        return value == null ? "" : value.replace("\"", "").replace("`", "").trim();
    }

    protected static String normalizeIdentifier(String value) {
        return cleanIdentifier(value).toUpperCase(Locale.ROOT);
    }

    protected static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    protected static void addIfText(java.util.Collection<String> values, String value) {
        if (StringUtils.hasText(value)) {
            values.add(value);
        }
    }

    static final class CandidateSql {

        protected final List<Map<String, Object>> blockingReasons;
        protected final String ddlSql;
        protected final String refreshSql;
        protected final String validationSql;
        protected final String rollbackSql;
        protected final String rewriteSql;
        protected final Map<String, Object> commonSubgraphEvidence;

        protected CandidateSql(List<Map<String, Object>> blockingReasons,
                             String ddlSql,
                             String refreshSql,
                             String validationSql,
                             String rollbackSql,
                             String rewriteSql,
                             Map<String, Object> commonSubgraphEvidence) {
            this.blockingReasons = blockingReasons;
            this.ddlSql = ddlSql;
            this.refreshSql = refreshSql;
            this.validationSql = validationSql;
            this.rollbackSql = rollbackSql;
            this.rewriteSql = rewriteSql;
            this.commonSubgraphEvidence = commonSubgraphEvidence;
        }

        static CandidateSql blocked(List<Map<String, Object>> blockingReasons) {
            return new CandidateSql(
                blockingReasons == null ? Collections.<Map<String, Object>>emptyList() : blockingReasons,
                null,
                null,
                null,
                null,
                null,
                Collections.<String, Object>emptyMap()
            );
        }

        static CandidateSql generated(String ddlSql,
                                      String refreshSql,
                                      String validationSql,
                                      String rollbackSql,
                                      String rewriteSql,
                                      Map<String, Object> commonSubgraphEvidence) {
            return new CandidateSql(
                Collections.<Map<String, Object>>emptyList(),
                ddlSql,
                refreshSql,
                validationSql,
                rollbackSql,
                rewriteSql,
                commonSubgraphEvidence
            );
        }

        List<Map<String, Object>> getBlockingReasons() {
            return blockingReasons;
        }

        String getDdlSql() {
            return ddlSql;
        }

        String getRefreshSql() {
            return refreshSql;
        }

        String getValidationSql() {
            return validationSql;
        }

        String getRollbackSql() {
            return rollbackSql;
        }

        String getRewriteSql() {
            return rewriteSql;
        }

        Map<String, Object> getCommonSubgraphEvidence() {
            return commonSubgraphEvidence;
        }
    }

    protected static final class SubgraphCandidate {

        protected final String sourceKind;
        protected final String sourceName;
        protected final String alias;
        protected final String subgraphSql;
        protected final boolean recursive;
        protected final Set<String> materializedCteNames;

        protected SubgraphCandidate(String sourceKind,
                                  String sourceName,
                                  String alias,
                                  String subgraphSql,
                                  boolean recursive) {
            this(sourceKind, sourceName, alias, subgraphSql, recursive, Collections.<String>emptySet());
        }

        protected SubgraphCandidate(String sourceKind,
                                  String sourceName,
                                  String alias,
                                  String subgraphSql,
                                  boolean recursive,
                                  Set<String> materializedCteNames) {
            this.sourceKind = sourceKind;
            this.sourceName = sourceName;
            this.alias = alias;
            this.subgraphSql = subgraphSql;
            this.recursive = recursive;
            this.materializedCteNames = materializedCteNames == null
                ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<String>(materializedCteNames));
        }
    }

    protected static final class OutputColumns {

        protected final List<String> columns;
        protected final Set<String> normalizedColumns;
        protected final List<Map<String, Object>> blockingReasons;

        protected OutputColumns(List<String> columns,
                              Set<String> normalizedColumns,
                              List<Map<String, Object>> blockingReasons) {
            this.columns = columns;
            this.normalizedColumns = normalizedColumns;
            this.blockingReasons = blockingReasons;
        }
    }

    protected static final class RelationUsageScope {

        protected final LinkedHashSet<String> candidateAliases = new LinkedHashSet<String>();
        protected int sourceCount;

        protected boolean referencesCandidate() {
            return !candidateAliases.isEmpty();
        }

        protected boolean allowUnqualifiedColumns() {
            return referencesCandidate() && sourceCount == 1;
        }
    }

    protected static final class AliasMatch {

        protected final boolean matched;
        protected final int endIndex;

        protected AliasMatch(boolean matched, int endIndex) {
            this.matched = matched;
            this.endIndex = endIndex;
        }

        protected static AliasMatch none() {
            return new AliasMatch(false, -1);
        }
    }

    protected static final class AliasToken {

        protected final boolean present;
        protected final String value;
        protected final int endIndex;

        protected AliasToken(boolean present, String value, int endIndex) {
            this.present = present;
            this.value = value;
            this.endIndex = endIndex;
        }

        protected static AliasToken none() {
            return new AliasToken(false, "", -1);
        }
    }

    protected static final class RewriteResult {

        protected final String sql;
        protected final List<Map<String, Object>> rewriteAttempts;

        protected RewriteResult(String sql, List<Map<String, Object>> rewriteAttempts) {
            this.sql = sql == null ? "" : sql;
            this.rewriteAttempts = rewriteAttempts == null
                ? Collections.<Map<String, Object>>emptyList()
                : rewriteAttempts;
        }
    }

    protected static final class AstRewriteState {

        protected final SubgraphCandidate candidate;
        protected final List<Map<String, Object>> rewriteAttempts = new ArrayList<Map<String, Object>>();
        protected boolean replaced;

        protected AstRewriteState() {
            this(null);
        }

        protected AstRewriteState(SubgraphCandidate candidate) {
            this.candidate = candidate;
        }

        protected void recordDerivedAttempt(SqlNode relation, SqlNode alias) {
            if (candidate == null || rewriteAttempts.size() >= 12) {
                return;
            }
            String candidateAlias = normalizeIdentifier(firstText(candidate.alias, candidate.sourceName));
            String actualAlias = normalizeIdentifier(alias == null ? "" : alias.toString());
            String relationSql = relation == null ? "" : normalizeSubgraphSql(relation.toString());
            String relationFingerprint = subgraphFingerprint(relationSql);
            String candidateFingerprint = subgraphFingerprint(candidate.subgraphSql);
            LinkedHashMap<String, Object> attempt = new LinkedHashMap<String, Object>();
            attempt.put("actualAlias", cleanIdentifier(alias == null ? "" : alias.toString()));
            attempt.put("candidateAlias", cleanIdentifier(firstText(candidate.alias, candidate.sourceName)));
            attempt.put("aliasMatched", Boolean.valueOf(
                !StringUtils.hasText(candidateAlias) || candidateAlias.equals(actualAlias)
            ));
            attempt.put("relationKind", relation == null ? "" : String.valueOf(relation.getKind()));
            attempt.put("relationClass", relation == null ? "" : relation.getClass().getSimpleName());
            attempt.put("selectLike", Boolean.valueOf(isSelectLike(relation)));
            attempt.put("relationFingerprint", relationFingerprint);
            attempt.put("candidateFingerprint", candidateFingerprint);
            attempt.put("fingerprintEqual", Boolean.valueOf(candidateFingerprint.equals(relationFingerprint)));
            attempt.put("aliasCoverageReplaceable", Boolean.valueOf(canReplaceByAliasCoverage(relationSql, candidate)));
            attempt.put("relationSqlPrefix", relationSql.length() <= 160 ? relationSql : relationSql.substring(0, 160));
            rewriteAttempts.add(attempt);
        }

        protected void recordError(RuntimeException ex) {
            if (rewriteAttempts.size() >= 12) {
                return;
            }
            LinkedHashMap<String, Object> attempt = new LinkedHashMap<String, Object>();
            attempt.put("error", ex == null ? "" : ex.getClass().getSimpleName());
            attempt.put("message", ex == null ? "" : ex.getMessage());
            rewriteAttempts.add(attempt);
        }
    }
}
