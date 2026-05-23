package com.company.sqlforge.common.rewrite;

import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class RuntimeSqlRewriteTemplateEngine {

    public static final String PROGRAM_VERSION = "template-replay-v1";
    public static final String MATCH_MODE = "TEMPLATE_CONDITION_REPLAY";
    private static final Pattern SQL_LITERAL_PATTERN =
        Pattern.compile("'(?:''|[^'])*'|(?<![A-Za-z0-9_$])[-+]?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?(?![A-Za-z0-9_$])");
    private static final Pattern FAST_WHERE_ONE_EQUALS_ONE_RESIDUAL_PATTERN =
        Pattern.compile("(?is)where\\s+1\\s*=\\s*1\\s+and\\s+([^()]+?)\\s+and\\s*\\(");

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
        if (SqlFingerprintUtils.fingerprint(sourceSql).equals(SqlFingerprintUtils.fingerprint(currentSql))) {
            String rewrittenSql = sourceSql.equals(currentSql)
                ? recommendedSql
                : LiteralReplay.fromSql(sourceSql, currentSql).apply(recommendedSql);
            if (!StringUtils.hasText(rewrittenSql)) {
                return RuntimeSqlRewriteTemplateResult.notApplied("RENDERED_SQL_EMPTY");
            }
            return RuntimeSqlRewriteTemplateResult.applied(
                rewrittenSql,
                MATCH_MODE,
                replayResultProgramJson(sourceSql, recommendedSql)
            );
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
        if (sourceShape.headKey().equals(currentShape.headKey())
            && sourceShape.tailKey().equals(currentShape.tailKey())) {
            String rewrittenSql = render(recommendedShape, currentShape);
            if (!sourceSql.equals(currentSql)) {
                rewrittenSql = LiteralReplay.fromSql(sourceSql, currentSql).apply(rewrittenSql);
            }
            if (!StringUtils.hasText(rewrittenSql)) {
                return RuntimeSqlRewriteTemplateResult.notApplied("RENDERED_SQL_EMPTY");
            }
            return RuntimeSqlRewriteTemplateResult.applied(
                rewrittenSql,
                MATCH_MODE,
                replayResultProgramJson(sourceSql, recommendedSql)
            );
        }

        FastResidualReplay fastResidualReplay = FastResidualReplay.match(sourceSql, currentSql);
        if (fastResidualReplay.isMatched()) {
            String rewrittenSql = fastResidualReplay.render(LiteralReplay.fromSql(sourceSql, currentSql).apply(recommendedSql));
            if (!StringUtils.hasText(rewrittenSql)) {
                return RuntimeSqlRewriteTemplateResult.notApplied(fastResidualReplay.getFailureReason());
            }
            return RuntimeSqlRewriteTemplateResult.applied(
                rewrittenSql,
                MATCH_MODE,
                replayResultProgramJson(sourceSql, recommendedSql)
            );
        }

        NestedWhereReplay nestedWhereReplay = NestedWhereReplay.match(sourceSql, currentSql);
        if (!nestedWhereReplay.isMatched()) {
            if ("NESTED_WHERE_SKELETON_MISMATCH".equals(nestedWhereReplay.getFailureReason())) {
                return RuntimeSqlRewriteTemplateResult.notApplied("SOURCE_HEAD_MISMATCH");
            }
            return RuntimeSqlRewriteTemplateResult.notApplied(nestedWhereReplay.getFailureReason());
        }
        String rewrittenSql = nestedWhereReplay.render(recommendedSql);
        if (!StringUtils.hasText(rewrittenSql)) {
            return RuntimeSqlRewriteTemplateResult.notApplied(nestedWhereReplay.getFailureReason());
        }
        return RuntimeSqlRewriteTemplateResult.applied(
            rewrittenSql,
            MATCH_MODE,
            replayResultProgramJson(sourceSql, recommendedSql)
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
        program.put("literalSlotCount", Integer.valueOf(LiteralReplay.literalCount(sourceSql)));
        program.put("nestedWhereSegmentCount", Integer.valueOf(NestedWhereReplay.segmentCount(sourceSql)));
        program.put("conditionReplayPolicy", "REPLAY_CURRENT_TOP_LEVEL_AND_NESTED_AND_PREDICATES");
        program.put("safetyBoundary", "ACTIVE_RUNTIME_BINDING_AND_TENANT_AUTH_REQUIRED");
        return JsonUtils.toJson(program);
    }

    private static String replayResultProgramJson(String sourceSql, String recommendedSql) {
        Map<String, Object> program = new LinkedHashMap<String, Object>();
        program.put("programVersion", PROGRAM_VERSION);
        program.put("matchMode", MATCH_MODE);
        program.put("sourceSqlFingerprint", SqlFingerprintUtils.fingerprint(sourceSql));
        program.put("recommendedSqlFingerprint", SqlFingerprintUtils.fingerprint(recommendedSql));
        program.put("conditionReplayPolicy", "RUNTIME_REPLAY_USING_ACTIVATED_TEMPLATE_PROGRAM");
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

    private static boolean isQuerySql(String sql) {
        String normalized = sql.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("select ")
            || "select".equals(normalized)
            || normalized.startsWith("with ")
            || "with".equals(normalized);
    }

    private static List<String> literals(String sql) {
        List<String> result = new ArrayList<String>();
        Matcher matcher = SQL_LITERAL_PATTERN.matcher(sql == null ? "" : sql);
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }

    private static String replaceLiteralTokens(String sql, Map<String, String> literalMap) {
        if (!StringUtils.hasText(sql) || literalMap == null || literalMap.isEmpty()) {
            return sql;
        }
        Matcher matcher = SQL_LITERAL_PATTERN.matcher(sql);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = literalMap.get(matcher.group());
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement == null ? matcher.group() : replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static int findKeywordAtDepth(String sql, String keyword, int start, Integer requiredDepth) {
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
                if (inDoubleQuote && next == '"') {
                    index += 2;
                    continue;
                }
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
            if ((requiredDepth == null || depth == requiredDepth.intValue())
                && lowerSql.startsWith(lowerKeyword, index)
                && SqlShape.hasKeywordBoundary(sql, index, lowerKeyword.length())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static int findMatchingParen(String sql, int openIndex) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        int depth = 0;
        int index = openIndex;
        while (index < sql.length()) {
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
                if (inDoubleQuote && next == '"') {
                    index += 2;
                    continue;
                }
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
            } else if (current == ')') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
            index++;
        }
        return -1;
    }

    private static int depthAt(String sql, int offset) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        int depth = 0;
        int index = 0;
        while (index < offset && index < sql.length()) {
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
                if (inDoubleQuote && next == '"') {
                    index += 2;
                    continue;
                }
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
            } else if (current == ')') {
                depth = Math.max(0, depth - 1);
            }
            index++;
        }
        return depth;
    }

    private static final class LiteralReplay {
        private final Map<String, String> literalMap;

        private LiteralReplay(Map<String, String> literalMap) {
            this.literalMap = literalMap;
        }

        static LiteralReplay fromSql(String sourceSql, String currentSql) {
            Map<String, String> mapping = new LinkedHashMap<String, String>();
            List<String> sourceLiterals = literals(sourceSql);
            List<String> currentLiterals = literals(currentSql);
            addLiteralMappings(mapping, sourceLiterals, currentLiterals);
            if (sourceLiterals.size() == currentLiterals.size()) {
                return new LiteralReplay(mapping);
            }
            List<WhereSegment> sourceSegments = WhereSegment.extract(sourceSql);
            List<WhereSegment> currentSegments = WhereSegment.extract(currentSql);
            int count = Math.min(sourceSegments.size(), currentSegments.size());
            for (int index = 0; index < count; index++) {
                List<PredicateMatch> matches = PredicateMatch.match(
                    sourceSegments.get(index).getPredicates(),
                    currentSegments.get(index).getPredicates()
                );
                for (PredicateMatch match : matches) {
                    addLiteralMappings(mapping, literals(match.getSourcePredicate()), literals(match.getCurrentPredicate()));
                }
            }
            return new LiteralReplay(mapping);
        }

        static int literalCount(String sql) {
            return literals(sql).size();
        }

        String apply(String sql) {
            return replaceLiteralTokens(sql, literalMap);
        }

        private static void addLiteralMappings(Map<String, String> mapping,
                                               List<String> sourceLiterals,
                                               List<String> currentLiterals) {
            if (sourceLiterals == null || currentLiterals == null || sourceLiterals.size() != currentLiterals.size()) {
                return;
            }
            for (int index = 0; index < sourceLiterals.size(); index++) {
                String source = sourceLiterals.get(index);
                String current = currentLiterals.get(index);
                String existing = mapping.get(source);
                if (existing == null) {
                    mapping.put(source, current);
                } else if (!existing.equals(current)) {
                    mapping.remove(source);
                }
            }
        }
    }

    private static final class FastResidualReplay {
        private final boolean matched;
        private final String failureReason;
        private final List<String> residualPredicates;

        private FastResidualReplay(boolean matched, String failureReason, List<String> residualPredicates) {
            this.matched = matched;
            this.failureReason = failureReason;
            this.residualPredicates = residualPredicates == null
                ? new ArrayList<String>()
                : residualPredicates;
        }

        static FastResidualReplay match(String sourceSql, String currentSql) {
            Matcher matcher = FAST_WHERE_ONE_EQUALS_ONE_RESIDUAL_PATTERN.matcher(currentSql == null ? "" : currentSql);
            StringBuffer reverted = new StringBuffer();
            LinkedHashMap<String, String> residuals = new LinkedHashMap<String, String>();
            while (matcher.find()) {
                String predicate = matcher.group(1);
                if (!NestedWhereReplay.isPortableResidualPredicate(predicate)) {
                    return new FastResidualReplay(false, "FAST_RESIDUAL_NOT_PORTABLE", null);
                }
                residuals.put(SqlFingerprintUtils.normalizeForFingerprint(predicate), predicate.trim());
                matcher.appendReplacement(reverted, Matcher.quoteReplacement("WHERE 1 = 1\nAND\n("));
            }
            matcher.appendTail(reverted);
            if (residuals.isEmpty()) {
                return new FastResidualReplay(false, "FAST_RESIDUAL_NOT_FOUND", null);
            }
            if (!SqlFingerprintUtils.normalizeForFingerprint(sourceSql)
                .equals(SqlFingerprintUtils.normalizeForFingerprint(reverted.toString()))) {
                return new FastResidualReplay(false, "FAST_RESIDUAL_SOURCE_MISMATCH", null);
            }
            return new FastResidualReplay(true, null, new ArrayList<String>(residuals.values()));
        }

        boolean isMatched() {
            return matched;
        }

        String getFailureReason() {
            return failureReason;
        }

        String render(String recommendedSql) {
            if (!matched) {
                return null;
            }
            return NestedWhereReplay.injectResidualPredicates(recommendedSql, residualPredicates);
        }
    }

    private static final class NestedWhereReplay {
        private final boolean matched;
        private final String failureReason;
        private final List<String> residualPredicates;
        private final LiteralReplay literalReplay;

        private NestedWhereReplay(boolean matched,
                                  String failureReason,
                                  List<String> residualPredicates,
                                  LiteralReplay literalReplay) {
            this.matched = matched;
            this.failureReason = failureReason;
            this.residualPredicates = residualPredicates == null
                ? new ArrayList<String>()
                : residualPredicates;
            this.literalReplay = literalReplay;
        }

        static NestedWhereReplay match(String sourceSql, String currentSql) {
            List<WhereSegment> sourceSegments = WhereSegment.extract(sourceSql);
            List<WhereSegment> currentSegments = WhereSegment.extract(currentSql);
            if (sourceSegments.isEmpty() || currentSegments.isEmpty()) {
                return notMatched("SOURCE_HEAD_MISMATCH");
            }
            if (sourceSegments.size() != currentSegments.size()) {
                return notMatched("NESTED_WHERE_SEGMENT_COUNT_MISMATCH");
            }
            String sourceSkeleton = WhereSegment.skeleton(sourceSql, sourceSegments);
            String currentSkeleton = WhereSegment.skeleton(currentSql, currentSegments);
            if (!SqlFingerprintUtils.normalizeForFingerprint(sourceSkeleton)
                .equals(SqlFingerprintUtils.normalizeForFingerprint(currentSkeleton))) {
                return notMatched("NESTED_WHERE_SKELETON_MISMATCH");
            }
            List<String> residuals = new ArrayList<String>();
            Map<String, String> literalMap = new LinkedHashMap<String, String>();
            for (int index = 0; index < sourceSegments.size(); index++) {
                List<PredicateMatch> matches = PredicateMatch.match(
                    sourceSegments.get(index).getPredicates(),
                    currentSegments.get(index).getPredicates()
                );
                for (PredicateMatch match : matches) {
                    LiteralReplay.addLiteralMappings(
                        literalMap,
                        literals(match.getSourcePredicate()),
                        literals(match.getCurrentPredicate())
                    );
                }
                PredicateMatchResult result = PredicateMatchResult.compare(
                    sourceSegments.get(index).getPredicates(),
                    currentSegments.get(index).getPredicates()
                );
                if (!result.isMatched()) {
                    return notMatched(result.getFailureReason());
                }
                residuals.addAll(result.getResidualPredicates());
            }
            return new NestedWhereReplay(
                true,
                null,
                portableResidualPredicates(residuals),
                new LiteralReplay(literalMap)
            );
        }

        static int segmentCount(String sql) {
            return WhereSegment.extract(sql).size();
        }

        private static NestedWhereReplay notMatched(String failureReason) {
            return new NestedWhereReplay(false, failureReason, null, null);
        }

        boolean isMatched() {
            return matched;
        }

        String getFailureReason() {
            return failureReason;
        }

        String render(String recommendedSql) {
            if (!matched) {
                return null;
            }
            String rewrittenSql = literalReplay == null ? recommendedSql : literalReplay.apply(recommendedSql);
            if (residualPredicates.isEmpty()) {
                return rewrittenSql;
            }
            return injectResidualPredicates(rewrittenSql, residualPredicates);
        }

        private static List<String> portableResidualPredicates(List<String> predicates) {
            LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
            for (String predicate : predicates) {
                if (!isPortableResidualPredicate(predicate)) {
                    continue;
                }
                result.put(SqlFingerprintUtils.normalizeForFingerprint(predicate), predicate.trim());
            }
            return new ArrayList<String>(result.values());
        }

        private static boolean isPortableResidualPredicate(String predicate) {
            if (!StringUtils.hasText(predicate)) {
                return false;
            }
            String normalized = predicate.trim().toLowerCase(Locale.ROOT);
            if (normalized.indexOf('"') >= 0
                || normalized.indexOf(" select ") >= 0
                || normalized.indexOf(" group by ") >= 0
                || normalized.indexOf(" order by ") >= 0
                || normalized.indexOf(" limit ") >= 0) {
                return false;
            }
            return true;
        }

        private static String injectResidualPredicates(String recommendedSql, List<String> residualPredicates) {
            String injected = injectIntoCteWhere(recommendedSql, "raw_customer_snapshot", residualPredicates);
            if (StringUtils.hasText(injected)) {
                return injected;
            }
            return null;
        }

        private static String injectIntoCteWhere(String sql, String cteName, List<String> residualPredicates) {
            int cteIndex = sql.toLowerCase(Locale.ROOT).indexOf(cteName.toLowerCase(Locale.ROOT));
            if (cteIndex < 0) {
                return null;
            }
            int asIndex = findKeywordAtDepth(sql, "as", cteIndex + cteName.length(), null);
            if (asIndex < 0) {
                return null;
            }
            int openIndex = sql.indexOf('(', asIndex);
            if (openIndex < 0) {
                return null;
            }
            int closeIndex = findMatchingParen(sql, openIndex);
            if (closeIndex < 0) {
                return null;
            }
            List<WhereSegment> segments = WhereSegment.extract(sql.substring(openIndex + 1, closeIndex));
            if (segments.isEmpty()) {
                return null;
            }
            WhereSegment segment = segments.get(0);
            int insertionPoint = openIndex + 1 + segment.getBodyEnd();
            StringBuilder addition = new StringBuilder();
            for (String predicate : residualPredicates) {
                addition.append("\n  AND ").append(predicate.trim());
            }
            return sql.substring(0, insertionPoint) + addition + sql.substring(insertionPoint);
        }
    }

    private static final class WhereSegment {
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

        private final int bodyStart;
        private final int bodyEnd;
        private final List<String> predicates;

        private WhereSegment(int bodyStart, int bodyEnd, List<String> predicates) {
            this.bodyStart = bodyStart;
            this.bodyEnd = bodyEnd;
            this.predicates = predicates;
        }

        static List<WhereSegment> extract(String sqlText) {
            List<WhereSegment> result = new ArrayList<WhereSegment>();
            String sql = SqlShape.trimTrailingSemicolon(SqlShape.stripLeadingComments(sqlText == null ? "" : sqlText).trim());
            int cursor = 0;
            while (cursor < sql.length()) {
                int whereIndex = findKeywordAtDepth(sql, "where", cursor, null);
                if (whereIndex < 0) {
                    break;
                }
                int bodyStart = whereIndex + "where".length();
                int bodyEnd = findWhereBodyEnd(sql, bodyStart, depthAt(sql, whereIndex));
                if (bodyEnd <= bodyStart) {
                    cursor = bodyStart;
                    continue;
                }
                String whereBody = sql.substring(bodyStart, bodyEnd);
                result.add(new WhereSegment(bodyStart, bodyEnd, SqlShape.splitTopLevelAnd(whereBody)));
                cursor = bodyEnd;
            }
            return result;
        }

        static String skeleton(String sqlText, List<WhereSegment> segments) {
            String sql = SqlShape.trimTrailingSemicolon(SqlShape.stripLeadingComments(sqlText == null ? "" : sqlText).trim());
            StringBuilder builder = new StringBuilder(sql.length());
            int cursor = 0;
            for (WhereSegment segment : segments) {
                builder.append(sql, cursor, segment.bodyStart);
                builder.append(" __WHERE_BODY__ ");
                cursor = segment.bodyEnd;
            }
            builder.append(sql.substring(cursor));
            return builder.toString();
        }

        int getBodyEnd() {
            return bodyEnd;
        }

        List<String> getPredicates() {
            return predicates;
        }

        private static int findWhereBodyEnd(String sql, int start, int whereDepth) {
            boolean inSingleQuote = false;
            boolean inDoubleQuote = false;
            boolean inBacktick = false;
            int depth = whereDepth;
            int index = start;
            while (index < sql.length()) {
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
                    if (inDoubleQuote && next == '"') {
                        index += 2;
                        continue;
                    }
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
                for (String clause : POST_WHERE_CLAUSES) {
                    if (depth == whereDepth
                        && sql.toLowerCase(Locale.ROOT).startsWith(clause, index)
                        && SqlShape.hasKeywordBoundary(sql, index, clause.length())) {
                        return index;
                    }
                }
                if (current == '(') {
                    depth++;
                    index++;
                    continue;
                }
                if (current == ')') {
                    if (depth == whereDepth) {
                        return index;
                    }
                    depth = Math.max(whereDepth, depth - 1);
                    index++;
                    continue;
                }
                index++;
            }
            return sql.length();
        }
    }

    private static final class PredicateMatch {
        private final String sourcePredicate;
        private final String currentPredicate;

        private PredicateMatch(String sourcePredicate, String currentPredicate) {
            this.sourcePredicate = sourcePredicate;
            this.currentPredicate = currentPredicate;
        }

        static List<PredicateMatch> match(List<String> sourcePredicates, List<String> currentPredicates) {
            List<PredicateMatch> matches = new ArrayList<PredicateMatch>();
            boolean[] used = new boolean[currentPredicates == null ? 0 : currentPredicates.size()];
            if (sourcePredicates == null || currentPredicates == null) {
                return matches;
            }
            for (String sourcePredicate : sourcePredicates) {
                String sourceKey = SqlFingerprintUtils.normalizeForFingerprint(sourcePredicate);
                for (int index = 0; index < currentPredicates.size(); index++) {
                    if (used[index]) {
                        continue;
                    }
                    if (sourceKey.equals(SqlFingerprintUtils.normalizeForFingerprint(currentPredicates.get(index)))) {
                        used[index] = true;
                        matches.add(new PredicateMatch(sourcePredicate, currentPredicates.get(index)));
                        break;
                    }
                }
            }
            return matches;
        }

        String getSourcePredicate() {
            return sourcePredicate;
        }

        String getCurrentPredicate() {
            return currentPredicate;
        }
    }

    private static final class PredicateMatchResult {
        private final boolean matched;
        private final String failureReason;
        private final List<String> residualPredicates;

        private PredicateMatchResult(boolean matched, String failureReason, List<String> residualPredicates) {
            this.matched = matched;
            this.failureReason = failureReason;
            this.residualPredicates = residualPredicates == null ? new ArrayList<String>() : residualPredicates;
        }

        static PredicateMatchResult compare(List<String> sourcePredicates, List<String> currentPredicates) {
            if (sourcePredicates == null || currentPredicates == null) {
                return new PredicateMatchResult(false, "NESTED_WHERE_PREDICATE_UNAVAILABLE", null);
            }
            Set<Integer> matchedCurrentIndexes = new LinkedHashSet<Integer>();
            for (String sourcePredicate : sourcePredicates) {
                String sourceKey = SqlFingerprintUtils.normalizeForFingerprint(sourcePredicate);
                for (int index = 0; index < currentPredicates.size(); index++) {
                    if (matchedCurrentIndexes.contains(Integer.valueOf(index))) {
                        continue;
                    }
                    if (sourceKey.equals(SqlFingerprintUtils.normalizeForFingerprint(currentPredicates.get(index)))) {
                        matchedCurrentIndexes.add(Integer.valueOf(index));
                        break;
                    }
                }
            }
            List<String> residuals = new ArrayList<String>();
            for (int index = 0; index < currentPredicates.size(); index++) {
                if (!matchedCurrentIndexes.contains(Integer.valueOf(index))) {
                    residuals.add(currentPredicates.get(index));
                }
            }
            return new PredicateMatchResult(true, null, residuals);
        }

        boolean isMatched() {
            return matched;
        }

        String getFailureReason() {
            return failureReason;
        }

        List<String> getResidualPredicates() {
            return residualPredicates;
        }
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
            if (!isQuerySql(sql)) {
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
