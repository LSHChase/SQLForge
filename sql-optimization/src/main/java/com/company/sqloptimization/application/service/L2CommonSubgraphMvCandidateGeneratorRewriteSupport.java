package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlSetOperator;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.springframework.util.StringUtils;

abstract class L2CommonSubgraphMvCandidateGeneratorRewriteSupport extends L2CommonSubgraphMvCandidateGeneratorSqlSupport {

    protected static OutputColumns outputColumns(String subgraphSql) {
        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        LinkedHashSet<String> columns = new LinkedHashSet<String>();
        try {
            SqlSelect select = parseSelect(subgraphSql);
            SqlNodeList selectList = select == null ? null : select.getSelectList();
            if (selectList == null) {
                return outputColumnsByText(subgraphSql);
            }
            for (SqlNode item : selectList.getList()) {
                if (isStar(item)) {
                    blockingReasons.add(reason(
                        "EXPLICIT_PROJECTION_REQUIRED",
                        "公共子图包含 SELECT *，需要先展开字段后才能生成可审查物化视图。"
                    ));
                    continue;
                }
                String output = aliasName(item);
                if (!StringUtils.hasText(output)) {
                    SqlNode expression = stripAlias(item);
                    if (expression instanceof SqlIdentifier) {
                        output = identifierTail((SqlIdentifier) expression);
                    }
                }
                if (!StringUtils.hasText(output)) {
                    blockingReasons.add(reason(
                        "SUBGRAPH_OUTPUT_COLUMNS_UNRESOLVED",
                        "公共子图表达式输出缺少稳定别名，不能证明上层查询字段覆盖。"
                    ));
                    continue;
                }
                columns.add(cleanIdentifier(output));
            }
            OutputColumns textColumns = outputColumnsByText(subgraphSql);
            columns.addAll(textColumns.columns);
        } catch (RuntimeException ex) {
            return outputColumnsByText(subgraphSql);
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        for (String column : columns) {
            normalized.add(normalizeIdentifier(column));
        }
        return new OutputColumns(new ArrayList<String>(columns), normalized, blockingReasons);
    }

    protected static OutputColumns outputColumnsByText(String subgraphSql) {
        LinkedHashSet<String> columns = new LinkedHashSet<String>();
        String firstSelectList = firstSelectList(subgraphSql);
        if (!StringUtils.hasText(firstSelectList)) {
            return new OutputColumns(
                Collections.<String>emptyList(),
                Collections.<String>emptySet(),
                Collections.singletonList(reason(
                    "SUBGRAPH_SELECT_UNSUPPORTED",
                    "公共子图 SQL 无法解析出可证明的 SELECT 输出字段。"
                ))
            );
        }
        for (String item : splitTopLevelComma(firstSelectList)) {
            String alias = trailingAlias(item);
            if (!StringUtils.hasText(alias)) {
                alias = trailingColumnName(item);
            }
            if (StringUtils.hasText(alias)) {
                columns.add(cleanIdentifier(alias));
            }
        }
        if (columns.isEmpty()) {
            return new OutputColumns(
                Collections.<String>emptyList(),
                Collections.<String>emptySet(),
                Collections.singletonList(reason(
                    "SUBGRAPH_OUTPUT_COLUMNS_UNRESOLVED",
                    "公共子图表达式输出缺少稳定别名，不能证明上层查询字段覆盖。"
                ))
            );
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        for (String column : columns) {
            normalized.add(normalizeIdentifier(column));
        }
        return new OutputColumns(new ArrayList<String>(columns), normalized, Collections.<Map<String, Object>>emptyList());
    }

    protected static SqlSelect parseSelect(String sql) {
        SqlNode statement = parseStatement(sql);
        SqlSelect select = unwrapSelect(statement);
        if (select == null) {
            throw new IllegalArgumentException("not select");
        }
        return select;
    }

    protected static SqlNode parseStatement(String sql) {
        String normalizedSql = trimTrailingSemicolon(SqlDialectNormalizer.normalize(sql));
        try {
            return parseNormalizedStatement(normalizedSql);
        } catch (Exception ex) {
            String compatibleSql = backtickQuotedIdentifiersToDoubleQuoted(normalizedSql);
            if (!compatibleSql.equals(normalizedSql)) {
                try {
                    return parseNormalizedStatement(compatibleSql);
                } catch (Exception compatibleEx) {
                    throw new IllegalArgumentException("parse failed", compatibleEx);
                }
            }
            throw new IllegalArgumentException("parse failed", ex);
        }
    }

    protected static SqlNode parseNormalizedStatement(String sql) throws Exception {
        SqlParser.Config parserConfig = SqlParser.config()
            .withConformance(SqlConformanceEnum.LENIENT)
            .withUnquotedCasing(Casing.UNCHANGED);
        return SqlParser.create(trimTrailingSemicolon(sql), parserConfig).parseStmt();
    }

    protected static String backtickQuotedIdentifiersToDoubleQuoted(String sql) {
        if (sql == null || sql.indexOf('`') < 0) {
            return sql == null ? "" : sql;
        }
        StringBuilder builder = new StringBuilder(sql.length());
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktickQuote = false;
        for (int index = 0; index < sql.length(); index++) {
            char current = sql.charAt(index);
            char next = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';
            if (current == '\'' && !inDoubleQuote && !inBacktickQuote) {
                builder.append(current);
                if (inSingleQuote && next == '\'') {
                    builder.append(next);
                    index++;
                } else {
                    inSingleQuote = !inSingleQuote;
                }
                continue;
            }
            if (current == '"' && !inSingleQuote && !inBacktickQuote) {
                builder.append(current);
                if (inDoubleQuote && next == '"') {
                    builder.append(next);
                    index++;
                } else {
                    inDoubleQuote = !inDoubleQuote;
                }
                continue;
            }
            if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                if (inBacktickQuote && next == '`') {
                    builder.append('`');
                    index++;
                    continue;
                }
                inBacktickQuote = !inBacktickQuote;
                builder.append('"');
                continue;
            }
            if (inBacktickQuote && current == '"') {
                builder.append("\"\"");
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    protected static SqlNode parseStatementOrNull(String sql) {
        try {
            return parseStatement(sql);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    protected static boolean hasWindowFunction(SqlNode node) {
        if (node == null) {
            return false;
        }
        if (node.getKind() == SqlKind.OVER) {
            return true;
        }
        if (node instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                if (hasWindowFunction(operand)) {
                    return true;
                }
            }
        }
        if (node instanceof SqlNodeList) {
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                if (hasWindowFunction(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static boolean hasNonDeterministicFunction(SqlNode node) {
        if (node == null) {
            return false;
        }
        if (node instanceof SqlCall) {
            SqlCall call = (SqlCall) node;
            if (call.getOperator() != null) {
                String functionName = call.getOperator().getName();
                if (!call.getOperator().isDeterministic()
                    || call.getOperator().isDynamicFunction()
                    || NON_DETERMINISTIC_FUNCTION_NAMES.contains(upperText(functionName))) {
                    return true;
                }
            }
            for (SqlNode operand : call.getOperandList()) {
                if (hasNonDeterministicFunction(operand)) {
                    return true;
                }
            }
        }
        if (node instanceof SqlNodeList) {
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                if (hasNonDeterministicFunction(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static SqlSelect unwrapSelect(SqlNode node) {
        if (node instanceof SqlSelect) {
            return (SqlSelect) node;
        }
        if (node instanceof SqlOrderBy) {
            return unwrapSelect(((SqlOrderBy) node).query);
        }
        if (node instanceof SqlWith) {
            return unwrapSelect(((SqlWith) node).body);
        }
        return null;
    }

    protected static boolean isStar(SqlNode item) {
        if (item instanceof SqlIdentifier) {
            return ((SqlIdentifier) item).isStar();
        }
        return item != null && "*".equals(item.toString().trim());
    }

    protected static String aliasName(SqlNode item) {
        if (item instanceof SqlBasicCall && item.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) item).getOperandList();
            if (operands.size() >= 2) {
                return operands.get(1).toString();
            }
        }
        return "";
    }

    protected static SqlNode stripAlias(SqlNode item) {
        if (item instanceof SqlBasicCall && item.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) item).getOperandList();
            if (!operands.isEmpty()) {
                return operands.get(0);
            }
        }
        return item;
    }

    protected static String identifierTail(SqlIdentifier identifier) {
        if (identifier == null) {
            return "";
        }
        if (identifier.names != null && !identifier.names.isEmpty()) {
            return identifier.names.get(identifier.names.size() - 1);
        }
        String text = cleanIdentifier(identifier.toString());
        int dot = text.lastIndexOf('.');
        return dot >= 0 ? text.substring(dot + 1) : text;
    }

    protected static Set<String> requiredColumns(String sourceSql, SubgraphCandidate candidate) {
        return requiredColumnsByAst(sourceSql, candidate);
    }

    protected static OutputColumns withRequiredQualifiedColumns(OutputColumns outputColumns, Set<String> requiredColumns) {
        if (outputColumns == null || requiredColumns == null || requiredColumns.isEmpty()) {
            return outputColumns;
        }
        LinkedHashSet<String> columns = new LinkedHashSet<String>(outputColumns.columns);
        columns.addAll(requiredColumns);
        LinkedHashSet<String> normalized = new LinkedHashSet<String>(outputColumns.normalizedColumns);
        for (String column : requiredColumns) {
            normalized.add(normalizeIdentifier(column));
        }
        return new OutputColumns(
            new ArrayList<String>(columns),
            normalized,
            outputColumns.blockingReasons
        );
    }

    protected static Set<String> requiredColumnsByAst(String sourceSql, SubgraphCandidate candidate) {
        LinkedHashSet<String> relationNames = new LinkedHashSet<String>();
        addIfText(relationNames, normalizeIdentifier(candidate.sourceName));
        addIfText(relationNames, normalizeIdentifier(candidate.alias));
        if (relationNames.isEmpty()) {
            return Collections.emptySet();
        }
        try {
            LinkedHashSet<String> result = new LinkedHashSet<String>();
            collectRequiredColumns(parseStatement(sourceSql), candidate, relationNames, result);
            return result;
        } catch (RuntimeException ex) {
            return Collections.emptySet();
        }
    }

    protected static void collectRequiredColumns(SqlNode node,
                                               SubgraphCandidate candidate,
                                               Set<String> relationNames,
                                               Set<String> result) {
        if (node == null) {
            return;
        }
        if (node instanceof SqlNodeList) {
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                collectRequiredColumns(item, candidate, relationNames, result);
            }
            return;
        }
        if (node instanceof SqlWith) {
            SqlWith with = (SqlWith) node;
            if (with.withList != null) {
                for (SqlNode item : with.withList.getList()) {
                    if (item instanceof SqlWithItem) {
                        collectRequiredColumns(((SqlWithItem) item).query, candidate, relationNames, result);
                    }
                }
            }
            collectRequiredColumns(with.body, candidate, relationNames, result);
            return;
        }
        if (node instanceof SqlOrderBy) {
            SqlOrderBy orderBy = (SqlOrderBy) node;
            collectRequiredColumns(orderBy.query, candidate, relationNames, result);
            SqlSelect select = unwrapSelect(orderBy.query);
            if (select != null) {
                RelationUsageScope scope = relationUsageScope(select.getFrom(), candidate, relationNames);
                if (scope.referencesCandidate()) {
                    collectColumnsFromExpression(
                        orderBy.orderList,
                        scope.candidateAliases,
                        scope.allowUnqualifiedColumns(),
                        selectAliases(select.getSelectList()),
                        result
                    );
                }
            }
            return;
        }
        if (node instanceof SqlSelect) {
            SqlSelect select = (SqlSelect) node;
            RelationUsageScope scope = relationUsageScope(select.getFrom(), candidate, relationNames);
            if (scope.referencesCandidate()) {
                collectColumnsFromExpression(
                    select.getSelectList(),
                    scope.candidateAliases,
                    scope.allowUnqualifiedColumns(),
                    Collections.<String>emptySet(),
                    result
                );
                collectColumnsFromExpression(
                    select.getWhere(),
                    scope.candidateAliases,
                    scope.allowUnqualifiedColumns(),
                    Collections.<String>emptySet(),
                    result
                );
                collectColumnsFromExpression(
                    select.getGroup(),
                    scope.candidateAliases,
                    scope.allowUnqualifiedColumns(),
                    Collections.<String>emptySet(),
                    result
                );
                collectColumnsFromExpression(
                    select.getHaving(),
                    scope.candidateAliases,
                    scope.allowUnqualifiedColumns(),
                    Collections.<String>emptySet(),
                    result
                );
                collectColumnsFromExpression(
                    select.getOrderList(),
                    scope.candidateAliases,
                    scope.allowUnqualifiedColumns(),
                    selectAliases(select.getSelectList()),
                    result
                );
                collectJoinConditionColumns(select.getFrom(), scope.candidateAliases, result);
            }
            collectRequiredColumns(select.getSelectList(), candidate, relationNames, result);
            collectRequiredColumns(select.getFrom(), candidate, relationNames, result);
            collectRequiredColumns(select.getWhere(), candidate, relationNames, result);
            collectRequiredColumns(select.getGroup(), candidate, relationNames, result);
            collectRequiredColumns(select.getHaving(), candidate, relationNames, result);
            collectRequiredColumns(select.getOrderList(), candidate, relationNames, result);
            return;
        }
        if (node instanceof SqlBasicCall && node.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) node).getOperandList();
            if (!operands.isEmpty()) {
                collectRequiredColumns(operands.get(0), candidate, relationNames, result);
            }
            return;
        }
        if (node instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                collectRequiredColumns(operand, candidate, relationNames, result);
            }
        }
    }

    protected static RelationUsageScope relationUsageScope(SqlNode from,
                                                         SubgraphCandidate candidate,
                                                         Set<String> relationNames) {
        RelationUsageScope scope = new RelationUsageScope();
        collectRelationUsageScope(from, candidate, relationNames, scope);
        return scope;
    }

    protected static void collectRelationUsageScope(SqlNode from,
                                                  SubgraphCandidate candidate,
                                                  Set<String> relationNames,
                                                  RelationUsageScope scope) {
        if (from == null || scope == null) {
            return;
        }
        if (from instanceof SqlIdentifier) {
            scope.sourceCount++;
            if (identifierMatchesAnyRelation((SqlIdentifier) from, relationNames)) {
                addIfText(scope.candidateAliases, normalizeIdentifier(identifierTail((SqlIdentifier) from)));
            }
            return;
        }
        if (from instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) from;
            collectRelationUsageScope(join.getLeft(), candidate, relationNames, scope);
            collectRelationUsageScope(join.getRight(), candidate, relationNames, scope);
            return;
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
            if (operands.size() >= 2) {
                scope.sourceCount++;
                SqlNode relation = operands.get(0);
                SqlNode alias = operands.get(1);
                if (relationAliasMatchesCandidate(relation, alias, candidate, relationNames)) {
                    addIfText(scope.candidateAliases, normalizeIdentifier(alias == null ? "" : alias.toString()));
                    if (relation instanceof SqlIdentifier) {
                        addIfText(scope.candidateAliases, normalizeIdentifier(identifierTail((SqlIdentifier) relation)));
                    }
                }
            }
            return;
        }
        if (from instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) from).getOperandList()) {
                collectRelationUsageScope(operand, candidate, relationNames, scope);
            }
        }
    }

    protected static boolean relationAliasMatchesCandidate(SqlNode relation,
                                                         SqlNode alias,
                                                         SubgraphCandidate candidate,
                                                         Set<String> relationNames) {
        String normalizedAlias = normalizeIdentifier(alias == null ? "" : alias.toString());
        if ("DERIVED_TABLE".equals(candidate.sourceKind)
            && isSelectLike(relation)
            && relationNames.contains(normalizedAlias)) {
            return true;
        }
        if (relation instanceof SqlIdentifier && identifierMatchesAnyRelation((SqlIdentifier) relation, relationNames)) {
            return true;
        }
        return relationNames.contains(normalizedAlias) && isSelectLike(relation);
    }

    protected static boolean identifierMatchesAnyRelation(SqlIdentifier identifier, Set<String> relationNames) {
        if (identifier == null || relationNames == null || relationNames.isEmpty()) {
            return false;
        }
        String actual = normalizeIdentifier(identifier.toString());
        String tail = normalizeIdentifier(identifierTail(identifier));
        return relationNames.contains(actual) || relationNames.contains(tail);
    }

    protected static void collectJoinConditionColumns(SqlNode from,
                                                    Set<String> candidateAliases,
                                                    Set<String> result) {
        if (from == null) {
            return;
        }
        if (from instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) from;
            collectColumnsFromExpression(
                join.getCondition(),
                candidateAliases,
                false,
                Collections.<String>emptySet(),
                result
            );
            collectJoinConditionColumns(join.getLeft(), candidateAliases, result);
            collectJoinConditionColumns(join.getRight(), candidateAliases, result);
            return;
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            return;
        }
        if (from instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) from).getOperandList()) {
                collectJoinConditionColumns(operand, candidateAliases, result);
            }
        }
    }

    protected static void collectColumnsFromExpression(SqlNode node,
                                                     Set<String> candidateAliases,
                                                     boolean includeUnqualified,
                                                     Set<String> unqualifiedIgnore,
                                                     Set<String> result) {
        if (node == null || result == null) {
            return;
        }
        if (node instanceof SqlIdentifier) {
            SqlIdentifier identifier = (SqlIdentifier) node;
            if (identifier.isStar()) {
                return;
            }
            if (identifier.names != null && identifier.names.size() >= 2) {
                String qualifier = normalizeIdentifier(identifier.names.get(0));
                if (candidateAliases != null && candidateAliases.contains(qualifier)) {
                    String column = normalizeIdentifier(identifier.names.get(identifier.names.size() - 1));
                    if (!"*".equals(column)) {
                        result.add(column);
                    }
                }
                return;
            }
            if (includeUnqualified) {
                String column = normalizeIdentifier(identifierTail(identifier));
                if (!StringUtils.hasText(column)
                    || (candidateAliases != null && candidateAliases.contains(column))
                    || (unqualifiedIgnore != null && unqualifiedIgnore.contains(column))) {
                    return;
                }
                result.add(column);
            }
            return;
        }
        if (node instanceof SqlSelect || node instanceof SqlWith || node instanceof SqlOrderBy) {
            return;
        }
        if (node instanceof SqlNodeList) {
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                collectColumnsFromExpression(item, candidateAliases, includeUnqualified, unqualifiedIgnore, result);
            }
            return;
        }
        if (node instanceof SqlBasicCall && node.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) node).getOperandList();
            if (!operands.isEmpty()) {
                collectColumnsFromExpression(
                    operands.get(0),
                    candidateAliases,
                    includeUnqualified,
                    unqualifiedIgnore,
                    result
                );
            }
            return;
        }
        if (node instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                collectColumnsFromExpression(operand, candidateAliases, includeUnqualified, unqualifiedIgnore, result);
            }
        }
    }

    protected static Set<String> selectAliases(SqlNodeList selectList) {
        if (selectList == null) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (SqlNode item : selectList.getList()) {
            addIfText(result, normalizeIdentifier(aliasName(item)));
        }
        return result;
    }

    protected static String rewriteSql(String sourceSql, SubgraphCandidate candidate, String mvName) {
        String rewritten = rewriteSqlWithEvidence(sourceSql, candidate, mvName).sql;
        return StringUtils.hasText(rewritten) ? rewritten + ";" : "";
    }

    protected static RewriteResult rewriteSqlWithEvidence(String sourceSql, SubgraphCandidate candidate, String mvName) {
        if (!StringUtils.hasText(sourceSql) || candidate == null || !StringUtils.hasText(mvName)) {
            return new RewriteResult("", Collections.<Map<String, Object>>emptyList());
        }
        AstRewriteState state = new AstRewriteState(candidate);
        try {
            SqlNode statement = parseStatement(sourceSql);
            SqlNode rewritten;
            if ("CTE".equals(candidate.sourceKind)) {
                rewritten = rewriteCteCandidateByAst(statement, candidate, mvName, state);
            } else {
                rewritten = rewriteDerivedSourcesByAst(statement, candidate, mvName, state);
            }
            if (!state.replaced || rewritten == null) {
                return new RewriteResult("", state.rewriteAttempts);
            }
            return new RewriteResult(
                normalizeCalciteRenderedRewrite(trimTrailingSemicolon(rewritten.toString())),
                state.rewriteAttempts
            );
        } catch (RuntimeException ex) {
            state.recordError(ex);
            return new RewriteResult("", state.rewriteAttempts);
        }
    }

    protected static String normalizeCalciteRenderedRewrite(String rewriteSql) {
        if (!StringUtils.hasText(rewriteSql)) {
            return "";
        }
        String normalized = rewriteSql.replaceAll("`([A-Za-z_][A-Za-z0-9_$]*)`", "$1");
        normalized = normalized.replaceAll(
            "(?is)\\bFETCH\\s+NEXT\\s+(\\d+)\\s+ROWS\\s+ONLY\\b",
            "LIMIT $1"
        );
        normalized = normalized.replaceAll(
            "(?i)\\b(FROM|JOIN)\\s+([A-Za-z_][A-Za-z0-9_$]*(?:\\.[A-Za-z_][A-Za-z0-9_$]*)?)\\s+AS\\s+([A-Za-z_][A-Za-z0-9_$]*)\\b",
            "$1 $2 $3"
        );
        return normalized;
    }

    protected static SqlNode rewriteCteCandidateByAst(SqlNode statement,
                                                    SubgraphCandidate candidate,
                                                    String mvName,
                                                    AstRewriteState state) {
        SqlNode rewritten = rewriteRelationReferencesByAst(statement, candidate.sourceName, mvName, state);
        if (rewritten instanceof SqlOrderBy) {
            SqlOrderBy orderBy = (SqlOrderBy) rewritten;
            if (orderBy.query instanceof SqlWith) {
                SqlNode prunedQuery = pruneMaterializedAndUnusedCtes(
                    (SqlWith) orderBy.query,
                    candidate.materializedCteNames
                );
                return prunedQuery == orderBy.query
                    ? orderBy
                    : new SqlOrderBy(
                        orderBy.getParserPosition() == null ? SqlParserPos.ZERO : orderBy.getParserPosition(),
                        prunedQuery,
                        orderBy.orderList,
                        orderBy.offset,
                        orderBy.fetch
                    );
            }
            return orderBy;
        }
        if (!(rewritten instanceof SqlWith)) {
            return rewritten;
        }
        SqlWith with = (SqlWith) rewritten;
        return pruneMaterializedAndUnusedCtes(with, candidate.materializedCteNames);
    }

    protected static SqlNode pruneMaterializedAndUnusedCtes(SqlWith with, Set<String> materializedCteNames) {
        if (with == null || with.withList == null || with.withList.isEmpty()) {
            return with == null ? null : with.body;
        }
        LinkedHashMap<String, SqlWithItem> remainingByName = new LinkedHashMap<String, SqlWithItem>();
        for (SqlNode item : with.withList.getList()) {
            if (!(item instanceof SqlWithItem)) {
                continue;
            }
            SqlWithItem withItem = (SqlWithItem) item;
            String name = withItem.name == null ? "" : withItem.name.toString();
            if (containsRelationName(materializedCteNames, name)) {
                continue;
            }
            remainingByName.put(relationKey(name), withItem);
        }
        if (remainingByName.isEmpty()) {
            return with.body;
        }

        LinkedHashSet<String> neededNames = new LinkedHashSet<String>();
        addReferencedRemainingCtes(with.body, remainingByName, neededNames);
        boolean changed = true;
        while (changed) {
            changed = false;
            List<String> snapshot = new ArrayList<String>(neededNames);
            for (String name : snapshot) {
                SqlWithItem item = remainingByName.get(name);
                int before = neededNames.size();
                if (item != null) {
                    addReferencedRemainingCtes(item.query, remainingByName, neededNames);
                }
                changed = changed || neededNames.size() > before;
            }
        }

        List<SqlNode> retained = new ArrayList<SqlNode>();
        for (Map.Entry<String, SqlWithItem> entry : remainingByName.entrySet()) {
            if (neededNames.contains(entry.getKey())) {
                retained.add(entry.getValue());
            }
        }
        if (retained.isEmpty()) {
            return with.body;
        }
        with.withList = new SqlNodeList(
            retained,
            with.withList.getParserPosition() == null ? SqlParserPos.ZERO : with.withList.getParserPosition()
        );
        return with;
    }

    protected static void addReferencedRemainingCtes(SqlNode node,
                                                   Map<String, SqlWithItem> remainingByName,
                                                   Set<String> neededNames) {
        if (node == null || remainingByName == null || remainingByName.isEmpty()) {
            return;
        }
        LinkedHashSet<String> relations = new LinkedHashSet<String>();
        collectRelationReferences(node, relations);
        for (String relation : relations) {
            String key = relationKey(relation);
            String unqualified = relationKey(unqualifiedName(relation));
            if (remainingByName.containsKey(key)) {
                neededNames.add(key);
            }
            if (remainingByName.containsKey(unqualified)) {
                neededNames.add(unqualified);
            }
        }
    }

    protected static SqlNode rewriteRelationReferencesByAst(SqlNode node,
                                                          String relationName,
                                                          String mvName,
                                                          AstRewriteState state) {
        if (node == null) {
            return null;
        }
        if (node instanceof SqlOrderBy) {
            SqlOrderBy orderBy = (SqlOrderBy) node;
            SqlNode rewrittenQuery = rewriteRelationReferencesByAst(orderBy.query, relationName, mvName, state);
            return rewrittenQuery == orderBy.query
                ? orderBy
                : new SqlOrderBy(
                    orderBy.getParserPosition() == null ? SqlParserPos.ZERO : orderBy.getParserPosition(),
                    rewrittenQuery,
                    orderBy.orderList,
                    orderBy.offset,
                    orderBy.fetch
                );
        }
        if (node instanceof SqlWith) {
            SqlWith with = (SqlWith) node;
            if (with.withList != null) {
                for (SqlNode item : with.withList.getList()) {
                    if (item instanceof SqlWithItem) {
                        SqlWithItem withItem = (SqlWithItem) item;
                        withItem.query = rewriteRelationReferencesByAst(withItem.query, relationName, mvName, state);
                    }
                }
            }
            with.body = rewriteRelationReferencesByAst(with.body, relationName, mvName, state);
            return with;
        }
        if (node instanceof SqlSelect) {
            SqlSelect select = (SqlSelect) node;
            select.setFrom(rewriteRelationInFrom(select.getFrom(), relationName, mvName, state));
            rewriteChildQueries(select.getSelectList(), relationName, mvName, state);
            rewriteRelationReferencesByAst(select.getWhere(), relationName, mvName, state);
            rewriteRelationReferencesByAst(select.getHaving(), relationName, mvName, state);
            rewriteChildQueries(select.getOrderList(), relationName, mvName, state);
            return select;
        }
        if (node instanceof SqlCall) {
            SqlCall call = (SqlCall) node;
            List<SqlNode> operands = call.getOperandList();
            for (int index = 0; index < operands.size(); index++) {
                SqlNode operand = operands.get(index);
                SqlNode rewritten = rewriteRelationReferencesByAst(operand, relationName, mvName, state);
                if (rewritten != operand) {
                    setOperandIfPossible(call, index, rewritten);
                }
            }
        }
        return node;
    }

    protected static SqlNode rewriteRelationInFrom(SqlNode from,
                                                 String relationName,
                                                 String mvName,
                                                 AstRewriteState state) {
        if (from == null) {
            return null;
        }
        if (from instanceof SqlIdentifier && relationNameMatches((SqlIdentifier) from, relationName)) {
            state.replaced = true;
            return aliasedMvReference(mvName, unqualifiedName(relationName));
        }
        if (from instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) from;
            join.setLeft(rewriteRelationInFrom(join.getLeft(), relationName, mvName, state));
            join.setRight(rewriteRelationInFrom(join.getRight(), relationName, mvName, state));
            return join;
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
            if (operands.size() >= 2) {
                SqlNode relation = operands.get(0);
                SqlNode alias = operands.get(1);
                if (relation instanceof SqlIdentifier && relationNameMatches((SqlIdentifier) relation, relationName)) {
                    state.replaced = true;
                    return aliasedMvReference(mvName, alias);
                }
                SqlNode rewrittenRelation = rewriteRelationReferencesByAst(relation, relationName, mvName, state);
                if (rewrittenRelation != relation) {
                    return SqlStdOperatorTable.AS.createCall(
                        from.getParserPosition() == null ? SqlParserPos.ZERO : from.getParserPosition(),
                        rewrittenRelation,
                        alias
                    );
                }
            }
            return from;
        }
        if (from instanceof SqlCall) {
            SqlCall call = (SqlCall) from;
            List<SqlNode> operands = call.getOperandList();
            for (int index = 0; index < operands.size(); index++) {
                SqlNode operand = operands.get(index);
                SqlNode rewritten = rewriteRelationInFrom(operand, relationName, mvName, state);
                if (rewritten != operand) {
                    setOperandIfPossible(call, index, rewritten);
                }
            }
        }
        return from;
    }

    protected static boolean relationNameMatches(SqlIdentifier identifier, String relationName) {
        if (identifier == null || !StringUtils.hasText(relationName)) {
            return false;
        }
        String actual = relationKey(identifier.toString());
        String expected = relationKey(relationName);
        return actual.equals(expected)
            || relationKey(unqualifiedName(actual)).equals(relationKey(unqualifiedName(expected)));
    }

    protected static SqlNode rewriteDerivedSourcesByAst(SqlNode node,
                                                      SubgraphCandidate candidate,
                                                      String mvName,
                                                      AstRewriteState state) {
        if (node == null) {
            return null;
        }
        if (node instanceof SqlOrderBy) {
            SqlOrderBy orderBy = (SqlOrderBy) node;
            SqlNode rewrittenQuery = rewriteDerivedSourcesByAst(orderBy.query, candidate, mvName, state);
            return rewrittenQuery == orderBy.query
                ? orderBy
                : new SqlOrderBy(
                    orderBy.getParserPosition() == null ? SqlParserPos.ZERO : orderBy.getParserPosition(),
                    rewrittenQuery,
                    orderBy.orderList,
                    orderBy.offset,
                    orderBy.fetch
                );
        }
        if (node instanceof SqlWith) {
            SqlWith with = (SqlWith) node;
            if (with.withList != null) {
                for (SqlNode item : with.withList.getList()) {
                    if (item instanceof SqlWithItem) {
                        SqlWithItem withItem = (SqlWithItem) item;
                        withItem.query = rewriteDerivedSourcesByAst(withItem.query, candidate, mvName, state);
                    }
                }
            }
            with.body = rewriteDerivedSourcesByAst(with.body, candidate, mvName, state);
            return with;
        }
        if (node instanceof SqlSelect) {
            SqlSelect select = (SqlSelect) node;
            select.setFrom(rewriteDerivedInFrom(select.getFrom(), candidate, mvName, state));
            rewriteDerivedChildren(select.getSelectList(), candidate, mvName, state);
            rewriteDerivedSourcesByAst(select.getWhere(), candidate, mvName, state);
            rewriteDerivedSourcesByAst(select.getHaving(), candidate, mvName, state);
            rewriteDerivedChildren(select.getOrderList(), candidate, mvName, state);
            return select;
        }
        if (node instanceof SqlCall) {
            SqlCall call = (SqlCall) node;
            List<SqlNode> operands = call.getOperandList();
            for (int index = 0; index < operands.size(); index++) {
                SqlNode operand = operands.get(index);
                SqlNode rewritten = rewriteDerivedSourcesByAst(operand, candidate, mvName, state);
                if (rewritten != operand) {
                    setOperandIfPossible(call, index, rewritten);
                }
            }
        }
        return node;
    }

    protected static SqlNode rewriteDerivedInFrom(SqlNode from,
                                                SubgraphCandidate candidate,
                                                String mvName,
                                                AstRewriteState state) {
        if (from == null) {
            return null;
        }
        if (from instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) from;
            join.setLeft(rewriteDerivedInFrom(join.getLeft(), candidate, mvName, state));
            join.setRight(rewriteDerivedInFrom(join.getRight(), candidate, mvName, state));
            return join;
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
            if (operands.size() >= 2) {
                SqlNode relation = operands.get(0);
                SqlNode alias = operands.get(1);
                state.recordDerivedAttempt(relation, alias);
                if (derivedCandidateMatches(relation, alias, candidate)) {
                    state.replaced = true;
                    return aliasedMvReference(mvName, alias);
                }
                SqlNode rewrittenRelation = rewriteDerivedSourcesByAst(relation, candidate, mvName, state);
                if (rewrittenRelation != relation) {
                    return SqlStdOperatorTable.AS.createCall(
                        from.getParserPosition() == null ? SqlParserPos.ZERO : from.getParserPosition(),
                        rewrittenRelation,
                        alias
                    );
                }
            }
            return from;
        }
        if (from instanceof SqlCall) {
            SqlCall call = (SqlCall) from;
            List<SqlNode> operands = call.getOperandList();
            for (int index = 0; index < operands.size(); index++) {
                SqlNode operand = operands.get(index);
                SqlNode rewritten = rewriteDerivedInFrom(operand, candidate, mvName, state);
                if (rewritten != operand) {
                    setOperandIfPossible(call, index, rewritten);
                }
            }
        }
        return from;
    }

    protected static boolean derivedCandidateMatches(SqlNode relation, SqlNode alias, SubgraphCandidate candidate) {
        if (candidate == null || relation == null || !isSelectLike(relation)) {
            return false;
        }
        String candidateAlias = normalizeIdentifier(firstText(candidate.alias, candidate.sourceName));
        String actualAlias = normalizeIdentifier(alias == null ? "" : alias.toString());
        String relationSql = normalizeSubgraphSql(relation.toString());
        boolean structuralMatch = subgraphFingerprint(candidate.subgraphSql).equals(subgraphFingerprint(relationSql));
        if (structuralMatch) {
            return true;
        }
        boolean aliasMatches = !StringUtils.hasText(candidateAlias) || candidateAlias.equals(actualAlias);
        return aliasMatches && canReplaceByAliasCoverage(relationSql, candidate);
    }

    protected static boolean isSelectLike(SqlNode relation) {
        return relation instanceof SqlSelect
            || relation instanceof SqlWith
            || relation instanceof SqlOrderBy
            || isUnionAll(relation);
    }

    protected static boolean isUnionAll(SqlNode relation) {
        if (!(relation instanceof SqlCall) || relation.getKind() != SqlKind.UNION) {
            return false;
        }
        return ((SqlCall) relation).getOperator() instanceof SqlSetOperator
            && ((SqlSetOperator) ((SqlCall) relation).getOperator()).isAll();
    }

    protected static SqlNode aliasedMvReference(String mvName, String alias) {
        return aliasedMvReference(mvName, new SqlIdentifier(firstText(alias, mvName), SqlParserPos.ZERO));
    }

    protected static SqlNode aliasedMvReference(String mvName, SqlNode alias) {
        SqlNode mvIdentifier = new SqlIdentifier(mvName, SqlParserPos.ZERO);
        SqlNode resolvedAlias = alias == null ? new SqlIdentifier(mvName, SqlParserPos.ZERO) : alias;
        return SqlStdOperatorTable.AS.createCall(SqlParserPos.ZERO, mvIdentifier, resolvedAlias);
    }

    protected static void rewriteChildQueries(SqlNodeList nodes,
                                            String relationName,
                                            String mvName,
                                            AstRewriteState state) {
        if (nodes == null) {
            return;
        }
        for (SqlNode node : nodes.getList()) {
            rewriteRelationReferencesByAst(node, relationName, mvName, state);
        }
    }

    protected static void rewriteDerivedChildren(SqlNodeList nodes,
                                               SubgraphCandidate candidate,
                                               String mvName,
                                               AstRewriteState state) {
        if (nodes == null) {
            return;
        }
        for (SqlNode node : nodes.getList()) {
            rewriteDerivedSourcesByAst(node, candidate, mvName, state);
        }
    }

    protected static void setOperandIfPossible(SqlCall call, int index, SqlNode rewritten) {
        try {
            call.setOperand(index, rewritten);
        } catch (UnsupportedOperationException ex) {
            // 少数 Calcite 节点暴露不可变 operand 视图，常见查询节点已在父级 setter 中处理。
        }
    }

    protected static String replaceRelationReference(String sql, String relationName, String mvName) {
        if (!StringUtils.hasText(sql) || !StringUtils.hasText(relationName)) {
            return sql;
        }
        Pattern pattern = Pattern.compile(
            "(?i)\\b(FROM|JOIN)\\s+"
                + Pattern.quote(relationName)
                + "(\\s+(?:AS\\s+)?(?!WHERE\\b|GROUP\\b|HAVING\\b|ORDER\\b|LIMIT\\b|JOIN\\b|ON\\b)"
                + "([A-Z_][A-Z0-9_$]*))?"
        );
        Matcher matcher = pattern.matcher(sql);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String alias = matcher.group(3);
            String replacement = matcher.group(1) + " " + mvName;
            if (StringUtils.hasText(alias)) {
                replacement += " " + alias;
            } else {
                replacement += " " + relationName;
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    protected static String replaceDerivedSource(String sourceSql, SubgraphCandidate candidate, String mvName) {
        String normalizedSource = trimTrailingSemicolon(sourceSql);
        String alias = firstText(candidate.alias, candidate.sourceName);
        String subgraphPattern = Pattern.quote(stripOuterParentheses(candidate.subgraphSql));
        Pattern pattern = Pattern.compile(
            "(?is)\\(\\s*" + subgraphPattern + "\\s*\\)\\s+(?:AS\\s+)?"
                + quotedIdentifierPattern(alias)
        );
        Matcher matcher = pattern.matcher(normalizedSource);
        if (matcher.find()) {
            StringBuffer buffer = new StringBuffer();
            do {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(mvName + " " + renderAlias(alias)));
            } while (matcher.find());
            matcher.appendTail(buffer);
            return buffer.toString();
        }
        return replaceDerivedSourceByBalancedScan(normalizedSource, candidate, mvName);
    }

    protected static String replaceDerivedSourceByBalancedScan(String sourceSql, SubgraphCandidate candidate, String mvName) {
        String alias = firstText(candidate.alias, candidate.sourceName);
        if (!StringUtils.hasText(sourceSql) || !StringUtils.hasText(alias)) {
            return sourceSql;
        }
        String candidateFingerprint = subgraphFingerprint(candidate.subgraphSql);
        StringBuilder result = new StringBuilder();
        int cursor = 0;
        boolean replaced = false;
        for (int index = 0; index < sourceSql.length(); index++) {
            if (sourceSql.charAt(index) != '(') {
                continue;
            }
            int close = matchingParen(sourceSql, index);
            if (close < 0) {
                continue;
            }
            AliasMatch aliasMatch = aliasAfter(sourceSql, close + 1, alias);
            if (!aliasMatch.matched) {
                continue;
            }
            String innerSql = sourceSql.substring(index + 1, close);
            if (!candidateFingerprint.equals(subgraphFingerprint(innerSql))
                && !canReplaceByAliasCoverage(innerSql, candidate)) {
                continue;
            }
            result.append(sourceSql, cursor, index);
            result.append(mvName).append(" ").append(renderAlias(alias));
            cursor = aliasMatch.endIndex;
            index = aliasMatch.endIndex - 1;
            replaced = true;
        }
        if (!replaced) {
            return sourceSql;
        }
        result.append(sourceSql.substring(cursor));
        return result.toString();
    }

    protected static int replacementCount(String sourceSql, SubgraphCandidate candidate) {
        if ("CTE".equals(candidate.sourceKind)) {
            return relationReferenceCount(sourceSql, candidate.sourceName);
        }
        String alias = firstText(candidate.alias, candidate.sourceName);
        if (!StringUtils.hasText(sourceSql) || !StringUtils.hasText(alias)) {
            return 0;
        }
        String candidateFingerprint = subgraphFingerprint(candidate.subgraphSql);
        int count = 0;
        for (int index = 0; index < sourceSql.length(); index++) {
            if (sourceSql.charAt(index) != '(') {
                continue;
            }
            int close = matchingParen(sourceSql, index);
            if (close < 0) {
                continue;
            }
            AliasMatch anyAliasMatch = aliasAfter(sourceSql, close + 1);
            if (!anyAliasMatch.matched) {
                continue;
            }
            String innerSql = sourceSql.substring(index + 1, close);
            if (candidateFingerprint.equals(subgraphFingerprint(innerSql))) {
                count++;
                index = anyAliasMatch.endIndex - 1;
                continue;
            }
            AliasMatch expectedAliasMatch = aliasAfter(sourceSql, close + 1, alias);
            if (expectedAliasMatch.matched && canReplaceByAliasCoverage(innerSql, candidate)) {
                count++;
                index = expectedAliasMatch.endIndex - 1;
            }
        }
        return count;
    }

    protected static int relationReferenceCount(String sql, String relationName) {
        if (!StringUtils.hasText(sql) || !StringUtils.hasText(relationName)) {
            return 0;
        }
        try {
            return relationReferenceCount(parseStatement(sql), relationName);
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    protected static int relationReferenceCount(SqlNode node, String relationName) {
        if (node == null) {
            return 0;
        }
        if (node instanceof SqlWith) {
            SqlWith with = (SqlWith) node;
            int count = 0;
            if (with.withList != null) {
                for (SqlNode item : with.withList.getList()) {
                    if (item instanceof SqlWithItem) {
                        count += relationReferenceCount(((SqlWithItem) item).query, relationName);
                    }
                }
            }
            return count + relationReferenceCount(with.body, relationName);
        }
        if (node instanceof SqlOrderBy) {
            return relationReferenceCount(((SqlOrderBy) node).query, relationName);
        }
        if (node instanceof SqlSelect) {
            SqlSelect select = (SqlSelect) node;
            return relationReferenceCountInFrom(select.getFrom(), relationName)
                + relationReferenceCount(select.getSelectList(), relationName)
                + relationReferenceCount(select.getWhere(), relationName)
                + relationReferenceCount(select.getHaving(), relationName)
                + relationReferenceCount(select.getOrderList(), relationName);
        }
        if (node instanceof SqlNodeList) {
            int count = 0;
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                count += relationReferenceCount(item, relationName);
            }
            return count;
        }
        if (node instanceof SqlCall) {
            int count = 0;
            for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                count += relationReferenceCount(operand, relationName);
            }
            return count;
        }
        return 0;
    }

    protected static int relationReferenceCountInFrom(SqlNode from, String relationName) {
        if (from == null) {
            return 0;
        }
        if (from instanceof SqlIdentifier) {
            return relationNameMatches((SqlIdentifier) from, relationName) ? 1 : 0;
        }
        if (from instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) from;
            return relationReferenceCountInFrom(join.getLeft(), relationName)
                + relationReferenceCountInFrom(join.getRight(), relationName);
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
            if (operands.isEmpty()) {
                return 0;
            }
            SqlNode relation = operands.get(0);
            if (relation instanceof SqlIdentifier) {
                return relationNameMatches((SqlIdentifier) relation, relationName) ? 1 : 0;
            }
            return relationReferenceCount(relation, relationName);
        }
        if (from instanceof SqlCall) {
            int count = 0;
            for (SqlNode operand : ((SqlCall) from).getOperandList()) {
                count += relationReferenceCountInFrom(operand, relationName);
            }
            return count;
        }
        return 0;
    }

    protected static boolean canReplaceByAliasCoverage(String innerSql, SubgraphCandidate candidate) {
        if (!"DERIVED_TABLE".equals(candidate.sourceKind) || !StringUtils.hasText(innerSql)) {
            return false;
        }
        String normalizedInner = stripOuterParentheses(trimTrailingSemicolon(innerSql));
        if (!isReplacementEligibleSubquery(normalizedInner) || hasOrderOrLimit(normalizedInner)) {
            return false;
        }
        OutputColumns innerColumns = outputColumns(normalizedInner);
        OutputColumns candidateColumns = outputColumns(candidate.subgraphSql);
        if (!innerColumns.blockingReasons.isEmpty()
            || !candidateColumns.blockingReasons.isEmpty()
            || innerColumns.normalizedColumns.isEmpty()
            || candidateColumns.normalizedColumns.isEmpty()) {
            return false;
        }
        return innerColumns.normalizedColumns.containsAll(candidateColumns.normalizedColumns)
            || candidateColumns.normalizedColumns.containsAll(innerColumns.normalizedColumns);
    }

    protected static boolean isReplacementEligibleSubquery(String sql) {
        if (!StringUtils.hasText(sql)) {
            return false;
        }
        try {
            return isSelectLike(parseStatement(sql));
        } catch (RuntimeException ex) {
            return startsWithWord(sql, 0, "SELECT");
        }
    }

    protected static int matchingParen(String sql, int openIndex) {
        int depth = 0;
        char quote = 0;
        for (int index = openIndex; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (quote != 0) {
                if (current == quote) {
                    if (index + 1 < sql.length() && sql.charAt(index + 1) == quote) {
                        index++;
                    } else {
                        quote = 0;
                    }
                }
                continue;
            }
            if (current == '\'' || current == '"' || current == '`') {
                quote = current;
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
        }
        return -1;
    }

    protected static AliasMatch aliasAfter(String sql, int start, String expectedAlias) {
        AliasMatch match = aliasAfter(sql, start);
        if (!match.matched) {
            return match;
        }
        int index = skipWhitespace(sql, start);
        int aliasStart = index;
        if (startsWithWord(sql, index, "AS")) {
            aliasStart = skipWhitespace(sql, index + 2);
        }
        AliasToken token = readAliasToken(sql, aliasStart);
        return normalizeIdentifier(token.value).equals(normalizeIdentifier(expectedAlias))
            ? match
            : AliasMatch.none();
    }

    protected static AliasMatch aliasAfter(String sql, int start) {
        int index = skipWhitespace(sql, start);
        int aliasStart = index;
        if (startsWithWord(sql, index, "AS")) {
            aliasStart = skipWhitespace(sql, index + 2);
        }
        AliasToken token = readAliasToken(sql, aliasStart);
        return token.present
            ? new AliasMatch(true, token.endIndex)
            : AliasMatch.none();
    }

    protected static int skipWhitespace(String sql, int start) {
        int index = start;
        while (index < sql.length() && Character.isWhitespace(sql.charAt(index))) {
            index++;
        }
        return index;
    }

    protected static AliasToken readAliasToken(String sql, int start) {
        if (start >= sql.length()) {
            return AliasToken.none();
        }
        char first = sql.charAt(start);
        if (first == '"' || first == '`') {
            StringBuilder value = new StringBuilder();
            for (int index = start + 1; index < sql.length(); index++) {
                char current = sql.charAt(index);
                if (current == first) {
                    if (index + 1 < sql.length() && sql.charAt(index + 1) == first) {
                        value.append(current);
                        index++;
                        continue;
                    }
                    return new AliasToken(true, value.toString(), index + 1);
                }
                value.append(current);
            }
            return AliasToken.none();
        }
        int end = start;
        while (end < sql.length() && isIdentifierPart(sql.charAt(end))) {
            end++;
        }
        if (end == start) {
            return AliasToken.none();
        }
        return new AliasToken(true, sql.substring(start, end), end);
    }

    protected static boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '$';
    }

    protected static String renderAlias(String alias) {
        String cleaned = cleanIdentifier(alias);
        if (cleaned.matches("(?i)[A-Z_][A-Z0-9_$]*")) {
            return cleaned;
        }
        return "\"" + cleaned.replace("\"", "\"\"") + "\"";
    }

    protected static String removeSubgraphSql(String sourceSql, SubgraphCandidate candidate) {
        String alias = firstText(candidate.alias, candidate.sourceName);
        return replaceDerivedSource(sourceSql, candidate, alias);
    }

    protected static Map<String, Object> commonSubgraphEvidence(
        String sourceSql,
        SubgraphCandidate candidate,
        List<String> outputColumns,
        Set<String> requiredColumns,
        List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> peerSqls) {
        String fingerprint = subgraphFingerprint(candidate.subgraphSql);
        List<Map<String, Object>> matchedSourceRefs = new ArrayList<Map<String, Object>>();
        List<String> matchedSqlFingerprints = new ArrayList<String>();
        addSourceRef(matchedSourceRefs, matchedSqlFingerprints, "CURRENT_SQL", "CURRENT", sourceSql, fingerprint);
        if (peerSqls != null) {
            for (L2AccelerationArtifactBuilder.CommonSubgraphPeerSql peerSql : peerSqls) {
                if (peerSql == null || !StringUtils.hasText(peerSql.getSqlText())) {
                    continue;
                }
                List<SubgraphCandidate> peerCandidates =
                    subgraphCandidates(peerSql.getAdvancedStructureProfile());
                for (SubgraphCandidate peerCandidate : peerCandidates) {
                    if (!fingerprint.equals(subgraphFingerprint(peerCandidate.subgraphSql))) {
                        continue;
                    }
                    addSourceRef(
                        matchedSourceRefs,
                        matchedSqlFingerprints,
                        peerSql.getSourceKind(),
                        peerSql.getSourceRef(),
                        peerSql.getSqlText(),
                        fingerprint,
                        peerSql.getSqlFingerprint(),
                        peerSql.getReportCode()
                    );
                    break;
                }
            }
        }
        int currentSqlReferenceCount = Math.max(1, replacementCount(sourceSql, candidate));
        int crossSqlReferenceCount = Math.max(0, matchedSourceRefs.size() - 1);
        LinkedHashMap<String, Object> coverage = new LinkedHashMap<String, Object>();
        coverage.put("status", "COVERED");
        coverage.put("requiredColumns", new ArrayList<String>(requiredColumns));
        coverage.put("outputColumns", outputColumns);
        coverage.put("rewriteSource", "MV_ONLY");

        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("mode", evidenceMode(currentSqlReferenceCount, crossSqlReferenceCount));
        evidence.put("candidateSelectionSource", "CALCITE_AST_QBDAG_STRUCTURAL_REUSE");
        evidence.put("staticConstantMatchUsed", Boolean.FALSE);
        evidence.put("subgraphFingerprint", fingerprint);
        evidence.put("sourceKind", candidate.sourceKind);
        evidence.put("sourceName", candidate.sourceName);
        evidence.put("alias", candidate.alias);
        evidence.put("materializedCteNames", new ArrayList<String>(candidate.materializedCteNames));
        evidence.put("matchedSqlFingerprints", matchedSqlFingerprints);
        evidence.put("matchedSourceRefs", matchedSourceRefs);
        evidence.put("currentSqlReferenceCount", Integer.valueOf(currentSqlReferenceCount));
        evidence.put("crossSqlReferenceCount", Integer.valueOf(crossSqlReferenceCount));
        evidence.put("referenceCount", Integer.valueOf(currentSqlReferenceCount + crossSqlReferenceCount));
        evidence.put("rewriteReplacementCount", Integer.valueOf(currentSqlReferenceCount));
        evidence.put("outputColumns", outputColumns);
        evidence.put("rewriteCoverage", coverage);
        return evidence;
    }

    protected static String evidenceMode(int currentSqlReferenceCount, int crossSqlReferenceCount) {
        if (crossSqlReferenceCount > 0) {
            return "CROSS_SQL_SHARED_SUBGRAPH";
        }
        return currentSqlReferenceCount > 1 ? "SINGLE_SQL_REPEATED_SUBGRAPH" : "SINGLE_SQL_SUBGRAPH";
    }

    protected static void addSourceRef(List<Map<String, Object>> refs,
                                     List<String> fingerprints,
                                     String sourceKind,
                                     String sourceRef,
                                     String sqlText,
                                     String subgraphFingerprint) {
        addSourceRef(refs, fingerprints, sourceKind, sourceRef, sqlText, subgraphFingerprint, null, null);
    }

    protected static void addSourceRef(List<Map<String, Object>> refs,
                                     List<String> fingerprints,
                                     String sourceKind,
                                     String sourceRef,
                                     String sqlText,
                                     String subgraphFingerprint,
                                     String sqlFingerprint,
                                     String reportCode) {
        String resolvedSqlFingerprint = firstText(sqlFingerprint, shortSha256(canonicalSql(sqlText)));
        if (!fingerprints.contains(resolvedSqlFingerprint)) {
            fingerprints.add(resolvedSqlFingerprint);
        }
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("sourceKind", firstText(sourceKind, "UNKNOWN"));
        item.put("sourceRef", firstText(sourceRef, "UNKNOWN"));
        item.put("sqlFingerprint", resolvedSqlFingerprint);
        item.put("reportCode", reportCode);
        item.put("subgraphFingerprint", subgraphFingerprint);
        refs.add(item);
    }

}
