package com.company.sqloptimization.application.service;

import io.trino.sql.tree.AstVisitor;
import io.trino.sql.tree.DereferenceExpression;
import io.trino.sql.tree.FunctionCall;
import io.trino.sql.tree.JoinCriteria;
import io.trino.sql.tree.Node;
import io.trino.sql.tree.QualifiedName;
import io.trino.sql.tree.Query;
import io.trino.sql.tree.QuerySpecification;
import io.trino.sql.tree.SingleColumn;
import io.trino.sql.tree.Table;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
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
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;
import org.springframework.util.StringUtils;

abstract class SqlOptimizationPipelineCollectorSupport extends SqlOptimizationPipelineModelSupport {

    protected final class ApacheCalciteProfileCollector {

        protected final Set<String> cteNames = new LinkedHashSet<String>();

        protected void collect(SqlNode node, ParsedSqlProfile profile, boolean root) {
            if (node == null) {
                return;
            }
            if (node instanceof SqlOrderBy) {
                collectOrderBy((SqlOrderBy) node, profile, root);
                return;
            }
            if (node instanceof SqlWith) {
                collectWith((SqlWith) node, profile, root);
                return;
            }
            if (node instanceof SqlSelect) {
                collectSelect((SqlSelect) node, profile, root);
                return;
            }
            if (node instanceof SqlJoin) {
                collectJoin((SqlJoin) node, profile);
                return;
            }
            if (node instanceof SqlIdentifier) {
                collectIdentifier((SqlIdentifier) node, profile);
                return;
            }
            if (node instanceof SqlBasicCall) {
                collectCall((SqlBasicCall) node, profile);
                return;
            }
            if (node instanceof SqlCall) {
                collectGenericCall((SqlCall) node, profile);
            }
        }

        protected void collectOrderBy(SqlOrderBy orderBy, ParsedSqlProfile profile, boolean root) {
            collect(orderBy.query, profile, root);
            collectOrderList(orderBy.orderList, profile);
            if (orderBy.fetch != null || orderBy.offset != null) {
                profile.limitPresent = true;
                profile.recordLimit(orderBy.fetch, orderBy.offset);
            }
        }

        protected void collectWith(SqlWith with, ParsedSqlProfile profile, boolean root) {
            List<String> addedCteNames = new ArrayList<String>();
            try {
                if (with.withList != null) {
                    for (SqlNode itemNode : with.withList.getList()) {
                        if (itemNode instanceof SqlWithItem) {
                            SqlWithItem withItem = (SqlWithItem) itemNode;
                            profile.recordCte(withItem);
                            String cteName = normalizeCalciteIdentifier(withItem.name);
                            if (StringUtils.hasText(cteName) && cteNames.add(cteName.toUpperCase(Locale.ROOT))) {
                                addedCteNames.add(cteName.toUpperCase(Locale.ROOT));
                            }
                        }
                    }
                    for (SqlNode itemNode : with.withList.getList()) {
                        if (itemNode instanceof SqlWithItem) {
                            SqlWithItem item = (SqlWithItem) itemNode;
                            collect(item.query, profile, true);
                        } else {
                            collect(itemNode, profile, false);
                        }
                    }
                }
                collect(with.body, profile, root);
            } finally {
                for (String cteName : addedCteNames) {
                    cteNames.remove(cteName);
                }
            }
        }

        protected void collectSelect(SqlSelect select, ParsedSqlProfile profile, boolean root) {
            if (!root) {
                profile.subqueryCount++;
                profile.recordSubquery(select);
                profile.updateNestedSubqueryDepth();
                boolean correlated = profile.referencesVisibleAlias(select.toString());
                profile.recordSubqueryNode(select, "SUBQUERY", "", correlated);
                if (correlated) {
                    profile.correlatedSubqueryCount++;
                }
                profile.subqueryDepth++;
            }
            profile.pushAliasScope(collectCalciteAliases(select.getFrom()));
            try {
                collectSelectList(select.getSelectList(), profile);
                collectFrom(select.getFrom(), profile);
                if (select.getWhere() != null) {
                    collectPredicate("WHERE", select.getWhere(), profile);
                }
                SqlNodeList group = select.getGroup();
                if (group != null) {
                    profile.groupByCount += group.size();
                    for (SqlNode item : group.getList()) {
                        profile.recordGroupBy(item, collectCalciteColumnReferences(item));
                        profile.recordGroupByKey(item);
                        profile.recordExpression(item);
                        collectExpression(item, profile);
                    }
                }
                if (select.getHaving() != null) {
                    collectPredicate("HAVING", select.getHaving(), profile);
                }
                collectOrderList(select.getOrderList(), profile);
                if (select.getFetch() != null || select.getOffset() != null) {
                    profile.limitPresent = true;
                    profile.recordLimit(select.getFetch(), select.getOffset());
                }
                if (select.isDistinct()) {
                    profile.distinctPresent = true;
                }
            } finally {
                profile.popAliasScope();
                if (!root) {
                    profile.subqueryDepth--;
                }
            }
        }

        protected void collectSelectList(SqlNodeList selectList, ParsedSqlProfile profile) {
            if (selectList == null) {
                return;
            }
            profile.projectionCount += selectList.size();
            for (SqlNode item : selectList.getList()) {
                SqlNode expression = calciteExpressionWithoutAlias(item);
                profile.recordProjection(item, collectCalciteColumnReferences(expression));
                if (isCalciteSelectLike(expression) || (expression != null && expression.getKind() == SqlKind.SCALAR_QUERY)) {
                    recordSubqueryOperand(expression, profile, "EXPRESSION", calciteProjectionAlias(item), true);
                    continue;
                }
                if (isStar(expression)) {
                    profile.selectStar = true;
                    profile.recordSelectStar(item);
                    continue;
                }
                if (isCalciteStringProjection(expression)) {
                    profile.stringProjectionCount++;
                }
                profile.recordExpression(expression);
                collectExpression(expression, profile);
            }
        }

        protected SqlNode calciteExpressionWithoutAlias(SqlNode node) {
            if (node instanceof SqlBasicCall && node.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) node).getOperandList();
                if (!operands.isEmpty()) {
                    return operands.get(0);
                }
            }
            return node;
        }

        protected String calciteProjectionAlias(SqlNode node) {
            if (node instanceof SqlBasicCall && node.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) node).getOperandList();
                if (operands.size() >= 2) {
                    return normalizeCalciteIdentifier(operands.get(1));
                }
            }
            return "";
        }

        protected void collectFrom(SqlNode from, ParsedSqlProfile profile) {
            if (from == null) {
                return;
            }
            if (from instanceof SqlIdentifier) {
                String table = normalizeCalciteIdentifier(from);
                if (isCteReference(table)) {
                    profile.recordTable(table, "", "CTE_REFERENCE");
                    return;
                }
                if (!profile.tables.contains(table)) {
                    profile.tables.add(table);
                }
                profile.recordTableScan(table);
                profile.recordTable(table, "", "BASE_TABLE");
                return;
            }
            if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
                if (!operands.isEmpty()) {
                    String alias = operands.size() > 1 ? normalizeCalciteIdentifier(operands.get(1)) : "";
                    SqlNode relation = operands.get(0);
                    if (relation instanceof SqlIdentifier) {
                        String table = normalizeCalciteIdentifier(relation);
                        if (isCteReference(table)) {
                            profile.recordTable(table, alias, "CTE_REFERENCE");
                            return;
                        }
                        if (!profile.tables.contains(table)) {
                            profile.tables.add(table);
                        }
                        profile.recordTableScan(table);
                        profile.recordTable(table, alias, "BASE_TABLE");
                        return;
                    }
                    if (isCalciteSelectLike(relation)) {
                        profile.subqueryCount++;
                        profile.recordSubquery(relation);
                        profile.updateNestedSubqueryDepth();
                        boolean correlated = profile.referencesVisibleAlias(relation.toString());
                        profile.recordSubqueryNode(relation, "FROM", alias, correlated);
                        if (correlated) {
                            profile.correlatedSubqueryCount++;
                        }
                        profile.subqueryDepth++;
                        try {
                            collect(relation, profile, true);
                        } finally {
                            profile.subqueryDepth--;
                        }
                        return;
                    }
                    collectFrom(relation, profile);
                }
                return;
            }
            collect(from, profile, false);
        }

        protected Set<String> collectCalciteAliases(SqlNode from) {
            LinkedHashSet<String> aliases = new LinkedHashSet<String>();
            collectCalciteAliases(from, aliases);
            return aliases;
        }

        protected void collectCalciteAliases(SqlNode node, Set<String> aliases) {
            if (node == null || aliases == null) {
                return;
            }
            if (node instanceof SqlBasicCall && node.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) node).getOperandList();
                if (operands.size() >= 2) {
                    String alias = normalizeCalciteIdentifier(operands.get(1));
                    if (StringUtils.hasText(alias)) {
                        aliases.add(alias.toUpperCase(Locale.ROOT));
                    }
                    collectCalciteAliases(operands.get(0), aliases);
                }
                return;
            }
            if (node instanceof SqlJoin) {
                collectCalciteAliases(((SqlJoin) node).getLeft(), aliases);
                collectCalciteAliases(((SqlJoin) node).getRight(), aliases);
                return;
            }
            if (node instanceof SqlIdentifier) {
                String table = normalizeCalciteIdentifier(node);
                if (StringUtils.hasText(table)) {
                    String[] parts = table.split("\\.");
                    aliases.add(parts[parts.length - 1].toUpperCase(Locale.ROOT));
                }
            }
        }

        protected boolean isCteReference(String tableName) {
            if (!StringUtils.hasText(tableName)) {
                return false;
            }
            return cteNames.contains(tableName.toUpperCase(Locale.ROOT));
        }

        protected String normalizeCalciteIdentifier(SqlNode node) {
            if (node == null) {
                return "";
            }
            return node.toString()
                .replace("\"", "")
                .replace("`", "")
                .trim();
        }

        protected List<String> collectCalciteColumnReferences(SqlNode node) {
            LinkedHashSet<String> columns = new LinkedHashSet<String>();
            collectCalciteColumnReferences(node, columns);
            return new ArrayList<String>(columns);
        }

        protected void collectCalciteColumnReferences(SqlNode node, Set<String> columns) {
            if (node == null || columns == null) {
                return;
            }
            if (node instanceof SqlIdentifier) {
                SqlIdentifier identifier = (SqlIdentifier) node;
                if (!identifier.isStar()) {
                    columns.add(removeSimpleIdentifierQuotes(identifier.toString()));
                }
                return;
            }
            if (node instanceof SqlBasicCall && node.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) node).getOperandList();
                if (!operands.isEmpty()) {
                    collectCalciteColumnReferences(operands.get(0), columns);
                }
                return;
            }
            if (node instanceof SqlCall) {
                for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                    collectCalciteColumnReferences(operand, columns);
                }
            }
        }

        protected List<String> collectCalciteFunctionNames(SqlNode node) {
            LinkedHashSet<String> names = new LinkedHashSet<String>();
            collectCalciteFunctionNames(node, names);
            return new ArrayList<String>(names);
        }

        protected void collectCalciteFunctionNames(SqlNode node, Set<String> names) {
            if (node == null || names == null) {
                return;
            }
            if (node instanceof SqlBasicCall) {
                SqlBasicCall call = (SqlBasicCall) node;
                if (call.getKind() == SqlKind.AS) {
                    List<SqlNode> operands = call.getOperandList();
                    if (!operands.isEmpty()) {
                        collectCalciteFunctionNames(operands.get(0), names);
                    }
                    return;
                }
                String functionName = call.getOperator() == null ? "" : call.getOperator().getName();
                if (StringUtils.hasText(functionName)) {
                    names.add(functionName.toUpperCase(Locale.ROOT));
                }
            }
            if (node instanceof SqlCall) {
                for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                    collectCalciteFunctionNames(operand, names);
                }
            }
        }

        protected void collectJoin(SqlJoin join, ParsedSqlProfile profile) {
            profile.joinCount++;
            if (join.getJoinType() != null) {
                profile.joinTypes.add(join.getJoinType().name());
            }
            profile.recordJoin(join.getLeft(), join);
            collectFrom(join.getLeft(), profile);
            collectFrom(join.getRight(), profile);
            if (join.getCondition() != null) {
                int predicateCount = countCalcitePredicates(join.getCondition().toString());
                profile.predicateCount += predicateCount;
                profile.joinCriteriaCount += predicateCount;
                profile.recordPredicate(
                    "JOIN_ON",
                    join.getCondition(),
                    collectCalciteColumnReferences(join.getCondition()),
                    collectCalciteFunctionNames(join.getCondition()),
                    "AND",
                    ""
                );
                profile.recordExpression(join.getCondition());
                collectExpression(join.getCondition(), profile);
            }
        }

        protected void collectIdentifier(SqlIdentifier identifier, ParsedSqlProfile profile) {
            if (identifier != null && !identifier.isStar()) {
                profile.projectedColumns.add(identifier.toString());
            }
        }

        protected void collectPredicate(String clause, SqlNode predicate, ParsedSqlProfile profile) {
            String predicateSql = predicate.toString();
            profile.predicateCount += countCalcitePredicates(predicateSql);
            profile.datePredicateColumns.addAll(extractCalciteDatePredicateColumns(predicateSql));
            recordCalcitePredicateNode(clause, predicate, profile, "AND", null, new int[] {0});
            profile.recordExpression(predicateSql);
            collectExpression(predicate, profile);
        }

        protected void recordCalcitePredicateNode(String clause,
                                                SqlNode predicate,
                                                ParsedSqlProfile profile,
                                                String logicalContext,
                                                String groupId,
                                                int[] groupCounter) {
            if (predicate == null) {
                return;
            }
            if (predicate instanceof SqlBasicCall && predicate.getKind() == SqlKind.AND) {
                for (SqlNode operand : ((SqlBasicCall) predicate).getOperandList()) {
                    recordCalcitePredicateNode(clause, operand, profile, logicalContext, groupId, groupCounter);
                }
                return;
            }
            if (predicate instanceof SqlBasicCall && predicate.getKind() == SqlKind.OR) {
                String currentGroupId = StringUtils.hasText(groupId)
                    ? groupId
                    : nextCalcitePredicateGroupId(clause, groupCounter);
                for (SqlNode operand : ((SqlBasicCall) predicate).getOperandList()) {
                    recordCalcitePredicateNode(clause, operand, profile, "OR", currentGroupId, groupCounter);
                }
                return;
            }
            profile.recordPredicate(
                clause,
                predicate,
                collectCalciteColumnReferences(predicate),
                collectCalciteFunctionNames(predicate),
                logicalContext,
                groupId
            );
        }

        protected String nextCalcitePredicateGroupId(String clause, int[] groupCounter) {
            String prefix = StringUtils.hasText(clause) ? clause.trim().toUpperCase(Locale.ROOT) : "PREDICATE";
            prefix = prefix.replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+", "").replaceAll("_+$", "");
            if (!StringUtils.hasText(prefix)) {
                prefix = "PREDICATE";
            }
            groupCounter[0]++;
            return prefix + "_OR_" + groupCounter[0];
        }

        protected void collectOrderList(SqlNodeList orderList, ParsedSqlProfile profile) {
            if (orderList == null) {
                return;
            }
            profile.orderByCount += orderList.size();
            profile.orderByExpressionCount += orderList.size();
            for (SqlNode item : orderList.getList()) {
                profile.recordOrderBy(item, collectCalciteColumnReferences(item));
                profile.recordOrderByKey(item);
                profile.recordExpression(item);
                if (isRandomOrder(item)) {
                    profile.randomOrderCount++;
                }
                collectExpression(item, profile);
            }
        }

        protected void collectExpression(SqlNode node, ParsedSqlProfile profile) {
            collect(node, profile, false);
        }

        protected void collectCall(SqlBasicCall call, ParsedSqlProfile profile) {
            SqlKind kind = call.getKind();
            if (kind == SqlKind.AS) {
                List<SqlNode> operands = call.getOperandList();
                if (!operands.isEmpty()) {
                    if (isCalciteSelectLike(operands.get(0)) || operands.get(0).getKind() == SqlKind.SCALAR_QUERY) {
                        recordSubqueryOperand(operands.get(0), profile, "EXPRESSION", normalizeCalciteIdentifier(
                            operands.size() > 1 ? operands.get(1) : null
                        ), true);
                        return;
                    }
                    collectExpression(operands.get(0), profile);
                }
                return;
            }
            if (kind == SqlKind.SCALAR_QUERY) {
                recordSubqueryOperand(call, profile, "EXPRESSION", "", true);
                return;
            }
            if (kind == SqlKind.OR) {
                profile.orPredicateCount++;
            }
            if (kind == SqlKind.LIKE && isLeadingWildcardCalciteLike(call)) {
                profile.leadingWildcardLikeCount++;
            }
            if (kind == SqlKind.NOT && call.toString().toUpperCase(Locale.ROOT).contains("NOT EXISTS")) {
                profile.notExistsCount++;
            }
            if (isComparisonKind(kind) && hasCalciteFunctionWrappedOperand(call)) {
                profile.functionWrappedPredicateCount++;
                profile.recordFunctionWrappedPredicate(call);
            }
            if (kind == SqlKind.OVER) {
                profile.windowFunctionCount++;
            }
            recordCalciteFunction(call, profile);
            collectGenericCall(call, profile);
        }

        protected void recordSubqueryOperand(SqlNode node,
                                           ParsedSqlProfile profile,
                                           String location,
                                           String alias,
                                           boolean scalar) {
            SqlNode query = unwrapScalarQuery(node);
            if (query == null) {
                return;
            }
            profile.subqueryCount++;
            if (scalar) {
                profile.scalarSubqueryCount++;
            }
            profile.recordSubquery(query);
            profile.updateNestedSubqueryDepth();
            boolean correlated = profile.referencesVisibleAlias(query.toString());
            profile.recordSubqueryNode(query, location, alias, correlated);
            if (correlated) {
                profile.correlatedSubqueryCount++;
            }
            profile.subqueryDepth++;
            try {
                collect(query, profile, true);
            } finally {
                profile.subqueryDepth--;
            }
        }

        protected SqlNode unwrapScalarQuery(SqlNode node) {
            if (node instanceof SqlBasicCall && node.getKind() == SqlKind.SCALAR_QUERY) {
                List<SqlNode> operands = ((SqlBasicCall) node).getOperandList();
                return operands.isEmpty() ? null : operands.get(0);
            }
            return isCalciteSelectLike(node) ? node : null;
        }

        protected void collectGenericCall(SqlCall call, ParsedSqlProfile profile) {
            if (call.getKind() == SqlKind.UNION || call.getKind() == SqlKind.INTERSECT || call.getKind() == SqlKind.EXCEPT) {
                profile.setOperation = true;
            }
            for (SqlNode operand : call.getOperandList()) {
                if (isCalciteSelectLike(operand) || (operand != null && operand.getKind() == SqlKind.SCALAR_QUERY)) {
                    boolean scalar = operand != null && operand.getKind() == SqlKind.SCALAR_QUERY;
                    recordSubqueryOperand(
                        operand,
                        profile,
                        "EXPRESSION",
                        "",
                        scalar
                    );
                    continue;
                }
                collect(operand, profile, false);
            }
        }

        protected void recordCalciteFunction(SqlBasicCall call, ParsedSqlProfile profile) {
            String functionName = call.getOperator() == null ? "" : call.getOperator().getName();
            if (!StringUtils.hasText(functionName)) {
                return;
            }
            String upperName = functionName.toUpperCase(Locale.ROOT);
            profile.recordFunctionSignal(call, collectCalciteColumnReferences(call));
            if (AGGREGATE_FUNCTIONS.contains(upperName)) {
                profile.aggregateFunctions.add(upperName);
                profile.aggregateFunctionCount++;
                if (STRING_AGGREGATE_FUNCTIONS.contains(upperName)) {
                    profile.largeStringAggregateCount++;
                }
            } else if (looksLikeScalarFunction(call) && !BUILT_IN_SCALAR_FUNCTIONS.contains(upperName)) {
                profile.udfFunctions.add(upperName);
            }
            if (STRING_CONCAT_FUNCTIONS.contains(upperName)) {
                profile.stringConcatenationCount++;
            }
        }

        protected boolean looksLikeScalarFunction(SqlBasicCall call) {
            SqlKind kind = call.getKind();
            return kind == SqlKind.OTHER_FUNCTION || kind == SqlKind.OTHER || kind == SqlKind.CAST;
        }

        protected boolean isStar(SqlNode node) {
            if (node instanceof SqlIdentifier) {
                return ((SqlIdentifier) node).isStar();
            }
            return node != null && "*".equals(node.toString().trim());
        }

        protected boolean isLeadingWildcardCalciteLike(SqlBasicCall call) {
            List<SqlNode> operands = call.getOperandList();
            if (operands.size() < 2 || operands.get(1) == null) {
                return false;
            }
            String right = operands.get(1).toString().trim();
            return right.startsWith("'%") || right.startsWith("\"%");
        }

        protected boolean hasCalciteFunctionWrappedOperand(SqlBasicCall call) {
            for (SqlNode operand : call.getOperandList()) {
                if (operand instanceof SqlBasicCall && ((SqlBasicCall) operand).getKind() != SqlKind.OTHER) {
                    String operatorName = ((SqlBasicCall) operand).getOperator() == null
                        ? ""
                        : ((SqlBasicCall) operand).getOperator().getName();
                    if (StringUtils.hasText(operatorName) && !isComparisonKind(((SqlBasicCall) operand).getKind())) {
                        return true;
                    }
                }
            }
            return false;
        }

        protected boolean isComparisonKind(SqlKind kind) {
            return kind == SqlKind.EQUALS
                || kind == SqlKind.NOT_EQUALS
                || kind == SqlKind.LESS_THAN
                || kind == SqlKind.LESS_THAN_OR_EQUAL
                || kind == SqlKind.GREATER_THAN
                || kind == SqlKind.GREATER_THAN_OR_EQUAL;
        }

        protected boolean isRandomOrder(SqlNode node) {
            String text = node == null ? "" : node.toString().toUpperCase(Locale.ROOT);
            return text.contains("RAND(") || text.contains("RANDOM(");
        }

        protected boolean isCalciteStringProjection(SqlNode node) {
            if (node == null) {
                return false;
            }
            String text = node.toString();
            String upperText = text.toUpperCase(Locale.ROOT);
            if (upperText.contains("||") || upperText.contains("CONCAT(") || upperText.contains("GROUP_CONCAT(")
                || upperText.contains("STRING_AGG(") || upperText.contains("LISTAGG(")) {
                return true;
            }
            if (node instanceof SqlIdentifier) {
                String[] parts = text.split("\\.");
                return parts.length > 0 && isStringLikeProjectionKey(parts[parts.length - 1]);
            }
            return false;
        }

        protected int countCalcitePredicates(String expressionSql) {
            if (expressionSql == null || expressionSql.trim().isEmpty()) {
                return 0;
            }
            String normalized = expressionSql.toUpperCase(Locale.ROOT);
            int count = 1;
            Matcher matcher = Pattern.compile("\\bAND\\b|\\bOR\\b").matcher(normalized);
            while (matcher.find()) {
                count++;
            }
            return count;
        }

        protected List<String> extractCalciteDatePredicateColumns(String expressionSql) {
            if (expressionSql == null) {
                return Collections.emptyList();
            }
            LinkedHashSet<String> columns = new LinkedHashSet<String>();
            Matcher matcher = DATE_PREDICATE_PATTERN.matcher(expressionSql.toUpperCase(Locale.ROOT));
            while (matcher.find()) {
                columns.add(matcher.group(1));
            }
            return new ArrayList<String>(columns);
        }
    }

    protected static final class TrinoProfileVisitor extends AstVisitor<Void, ParsedSqlProfile> {

        @Override
        protected Void visitNode(Node node, ParsedSqlProfile profile) {
            for (Node child : node.getChildren()) {
                process(child, profile);
            }
            return null;
        }

        @Override
        protected Void visitQuery(Query node, ParsedSqlProfile profile) {
            if (node.getWith().isPresent()) {
                process(node.getWith().get(), profile);
            }
            process(node.getQueryBody(), profile);
            if (node.getOrderBy().isPresent()) {
                List<io.trino.sql.tree.SortItem> sortItems = node.getOrderBy().get().getSortItems();
                profile.orderByCount += sortItems.size();
                profile.orderByExpressionCount += sortItems.size();
                for (io.trino.sql.tree.SortItem sortItem : sortItems) {
                    profile.recordOrderByKey(sortItem.getSortKey());
                }
                profile.recordExpression(node.getOrderBy().get());
            }
            if (node.getLimit().isPresent()) {
                profile.limitPresent = true;
            }
            return null;
        }

        @Override
        protected Void visitQuerySpecification(QuerySpecification node, ParsedSqlProfile profile) {
            profile.projectionCount += node.getSelect().getSelectItems().size();
            process(node.getSelect(), profile);
            if (node.getFrom().isPresent()) {
                process(node.getFrom().get(), profile);
            }
            if (node.getWhere().isPresent()) {
                String whereSql = node.getWhere().get().toString();
                profile.predicateCount += countTrinoPredicates(whereSql);
                profile.datePredicateColumns.addAll(extractTrinoDatePredicateColumns(whereSql));
                profile.recordExpression(whereSql);
                process(node.getWhere().get(), profile);
            }
            if (node.getGroupBy().isPresent()) {
                List<io.trino.sql.tree.GroupingElement> groupingElements = node.getGroupBy().get().getGroupingElements();
                profile.groupByCount += groupingElements.size();
                for (io.trino.sql.tree.GroupingElement groupingElement : groupingElements) {
                    profile.recordGroupByKey(groupingElement);
                }
                profile.recordExpression(node.getGroupBy().get());
                process(node.getGroupBy().get(), profile);
            }
            if (node.getHaving().isPresent()) {
                profile.predicateCount += countTrinoPredicates(node.getHaving().get().toString());
                profile.recordExpression(node.getHaving().get());
                process(node.getHaving().get(), profile);
            }
            if (node.getOrderBy().isPresent()) {
                List<io.trino.sql.tree.SortItem> sortItems = node.getOrderBy().get().getSortItems();
                profile.orderByCount += sortItems.size();
                profile.orderByExpressionCount += sortItems.size();
                for (io.trino.sql.tree.SortItem sortItem : sortItems) {
                    profile.recordOrderByKey(sortItem.getSortKey());
                }
                profile.recordExpression(node.getOrderBy().get());
                process(node.getOrderBy().get(), profile);
            }
            if (node.getLimit().isPresent()) {
                profile.limitPresent = true;
            }
            return null;
        }

        @Override
        protected Void visitTable(Table node, ParsedSqlProfile profile) {
            profile.tables.add(node.getName().toString());
            profile.recordTableScan(node.getName().toString());
            return null;
        }

        @Override
        protected Void visitJoin(io.trino.sql.tree.Join node, ParsedSqlProfile profile) {
            profile.joinCount++;
            profile.joinTypes.add(node.getType().name());
            if (node.getCriteria().isPresent()) {
                JoinCriteria criteria = node.getCriteria().get();
                String criteriaSql = criteria.toString();
                int predicateCount = countTrinoPredicates(criteriaSql);
                profile.predicateCount += predicateCount;
                profile.joinCriteriaCount += predicateCount;
                profile.recordExpression(criteriaSql);
            }
            process(node.getLeft(), profile);
            process(node.getRight(), profile);
            return null;
        }

        @Override
        protected Void visitSingleColumn(SingleColumn node, ParsedSqlProfile profile) {
            if (isTrinoStringProjection(node.getExpression())) {
                profile.stringProjectionCount++;
            }
            profile.recordExpression(node.getExpression());
            process(node.getExpression(), profile);
            return null;
        }

        @Override
        protected Void visitAllColumns(io.trino.sql.tree.AllColumns node, ParsedSqlProfile profile) {
            profile.selectStar = true;
            profile.recordSelectStar(node);
            return null;
        }

        @Override
        protected Void visitFunctionCall(FunctionCall node, ParsedSqlProfile profile) {
            QualifiedName name = node.getName();
            String functionName = name == null ? "" : name.toString().toUpperCase(Locale.ROOT);
            if (AGGREGATE_FUNCTIONS.contains(functionName)) {
                profile.aggregateFunctions.add(functionName);
                profile.aggregateFunctionCount++;
                if (STRING_AGGREGATE_FUNCTIONS.contains(functionName)) {
                    profile.largeStringAggregateCount++;
                }
            } else if (!BUILT_IN_SCALAR_FUNCTIONS.contains(functionName)) {
                profile.udfFunctions.add(functionName);
            }
            if (STRING_CONCAT_FUNCTIONS.contains(functionName)) {
                profile.stringConcatenationCount++;
            }
            if (node.getWindow().isPresent()) {
                profile.windowFunctionCount++;
            }
            for (io.trino.sql.tree.Expression argument : node.getArguments()) {
                process(argument, profile);
            }
            return null;
        }

        @Override
        protected Void visitDereferenceExpression(DereferenceExpression node, ParsedSqlProfile profile) {
            profile.projectedColumns.add(node.toString());
            return null;
        }

        @Override
        protected Void visitSubqueryExpression(io.trino.sql.tree.SubqueryExpression node, ParsedSqlProfile profile) {
            profile.subqueryCount++;
            profile.recordSubquery(node);
            return visitNode(node, profile);
        }

        protected boolean isTrinoStringProjection(io.trino.sql.tree.Expression expression) {
            if (expression == null) {
                return false;
            }
            String text = expression.toString();
            String upperText = text.toUpperCase(Locale.ROOT);
            if (upperText.contains("||") || upperText.contains("CONCAT(") || upperText.contains("GROUP_CONCAT(")
                || upperText.contains("STRING_AGG(") || upperText.contains("LISTAGG(")) {
                return true;
            }
            String[] parts = text.split("\\.");
            return parts.length > 0 && isStringLikeProjectionKey(parts[parts.length - 1]);
        }

        protected static int countTrinoPredicates(String expressionSql) {
            if (expressionSql == null || expressionSql.trim().isEmpty()) {
                return 0;
            }
            String normalized = expressionSql.toUpperCase(Locale.ROOT);
            int count = 1;
            Matcher matcher = Pattern.compile("\\bAND\\b|\\bOR\\b").matcher(normalized);
            while (matcher.find()) {
                count++;
            }
            return count;
        }

        protected static List<String> extractTrinoDatePredicateColumns(String expressionSql) {
            if (expressionSql == null) {
                return Collections.emptyList();
            }
            LinkedHashSet<String> columns = new LinkedHashSet<String>();
            Matcher matcher = DATE_PREDICATE_PATTERN.matcher(expressionSql.toUpperCase(Locale.ROOT));
            while (matcher.find()) {
                columns.add(matcher.group(1));
            }
            return new ArrayList<String>(columns);
        }
    }

    public static final class ParsedSqlProfile {

        protected final String normalizedSql;
        protected final List<String> tables = new ArrayList<String>();
        protected final Set<String> projectedColumns = new LinkedHashSet<String>();
        protected final Set<String> aggregateFunctions = new LinkedHashSet<String>();
        protected final Set<String> udfFunctions = new LinkedHashSet<String>();
        protected final Set<String> datePredicateColumns = new LinkedHashSet<String>();
        protected final Set<String> selectStarItems = new LinkedHashSet<String>();
        protected final Set<String> functionWrappedPredicateExpressions = new LinkedHashSet<String>();
        protected final Set<String> joinTypes = new LinkedHashSet<String>();
        protected final List<String> warnings = new ArrayList<String>();
        protected final LinkedHashMap<String, Integer> expressionFrequency = new LinkedHashMap<String, Integer>();
        protected final LinkedHashMap<String, Integer> tableScanFrequency = new LinkedHashMap<String, Integer>();
        protected final LinkedHashMap<String, Integer> groupByKeyFrequency = new LinkedHashMap<String, Integer>();
        protected final LinkedHashMap<String, Integer> orderByKeyFrequency = new LinkedHashMap<String, Integer>();
        protected final LinkedHashMap<String, Integer> subqueryFrequency = new LinkedHashMap<String, Integer>();
        protected final Deque<Set<String>> aliasScopes = new ArrayDeque<Set<String>>();
        protected final AdvancedStructureProfile advancedStructureProfile = new AdvancedStructureProfile();
        protected String parserEngine = "APACHE_CALCITE";
        protected int projectionCount;
        protected int predicateCount;
        protected int joinCount;
        protected int joinCriteriaCount;
        protected int groupByCount;
        protected int orderByCount;
        protected int orderByExpressionCount;
        protected int duplicateOrderByKeyCount;
        protected int duplicateGroupByKeyCount;
        protected int aggregateFunctionCount;
        protected int stringProjectionCount;
        protected int stringConcatenationCount;
        protected int largeStringAggregateCount;
        protected int repeatedSubqueryCount;
        protected int windowFunctionCount;
        protected int subqueryCount;
        protected int scalarSubqueryCount;
        protected int nestedSubqueryDepth;
        protected int correlatedSubqueryCount;
        protected int orPredicateCount;
        protected int functionWrappedPredicateCount;
        protected int leadingWildcardLikeCount;
        protected int randomOrderCount;
        protected int notExistsCount;
        protected int repeatedTableScanCount;
        protected int subqueryDepth;
        protected int repeatedExpressionCount;
        protected boolean selectStar;
        protected boolean limitPresent;
        protected boolean distinctPresent;
        protected boolean setOperation;
        protected RewriteOutcome rewriteOutcome = RewriteOutcome.empty();

        protected ParsedSqlProfile(String normalizedSql) {
            this.normalizedSql = normalizedSql;
        }

        protected Map<String, Object> toAstProfile() {
            LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("statementType", "SELECT");
            payload.put("parserEngine", parserEngine);
            payload.put("tables", tables);
            payload.put("projectionCount", Integer.valueOf(projectionCount));
            payload.put("projectedColumns", new ArrayList<String>(projectedColumns));
            payload.put("predicateCount", Integer.valueOf(predicateCount));
            payload.put("joinCount", Integer.valueOf(joinCount));
            payload.put("joinTypes", new ArrayList<String>(joinTypes));
            payload.put("groupByCount", Integer.valueOf(groupByCount));
            payload.put("orderByCount", Integer.valueOf(orderByCount));
            payload.put("orderByExpressionCount", Integer.valueOf(orderByExpressionCount));
            payload.put("duplicateOrderByKeyCount", Integer.valueOf(duplicateOrderByKeyCount));
            payload.put("duplicateGroupByKeyCount", Integer.valueOf(duplicateGroupByKeyCount));
            payload.put("groupByWithoutAggregate", Boolean.valueOf(isGroupByWithoutAggregate()));
            payload.put("aggregateFunctionCount", Integer.valueOf(aggregateFunctionCount));
            payload.put("stringProjectionCount", Integer.valueOf(stringProjectionCount));
            payload.put("stringConcatenationCount", Integer.valueOf(stringConcatenationCount));
            payload.put("largeStringAggregateCount", Integer.valueOf(largeStringAggregateCount));
            payload.put("repeatedSubqueryCount", Integer.valueOf(repeatedSubqueryCount));
            payload.put("windowFunctionCount", Integer.valueOf(windowFunctionCount));
            payload.put("udfFunctions", new ArrayList<String>(udfFunctions));
            payload.put("subqueryCount", Integer.valueOf(subqueryCount));
            payload.put("scalarSubqueryCount", Integer.valueOf(scalarSubqueryCount));
            payload.put("nestedSubqueryDepth", Integer.valueOf(nestedSubqueryDepth));
            payload.put("correlatedSubqueryCount", Integer.valueOf(correlatedSubqueryCount));
            payload.put("orPredicateCount", Integer.valueOf(orPredicateCount));
            payload.put("functionWrappedPredicateCount", Integer.valueOf(functionWrappedPredicateCount));
            payload.put("leadingWildcardLikeCount", Integer.valueOf(leadingWildcardLikeCount));
            payload.put("randomOrderCount", Integer.valueOf(randomOrderCount));
            payload.put("notExistsCount", Integer.valueOf(notExistsCount));
            payload.put("repeatedTableScanCount", Integer.valueOf(repeatedTableScanCount));
            payload.put("repeatedExpressionCount", Integer.valueOf(repeatedExpressionCount));
            payload.put("aggregateFunctions", new ArrayList<String>(aggregateFunctions));
            payload.put("datePredicateColumns", new ArrayList<String>(datePredicateColumns));
            payload.put("selectStar", Boolean.valueOf(selectStar));
            payload.put("selectStarItems", new ArrayList<String>(selectStarItems));
            payload.put("functionWrappedPredicateExpressions", new ArrayList<String>(functionWrappedPredicateExpressions));
            payload.put("limitPresent", Boolean.valueOf(limitPresent));
            payload.put("distinctPresent", Boolean.valueOf(distinctPresent));
            payload.put("setOperation", Boolean.valueOf(setOperation));
            payload.put("advancedStructureProfile", toAdvancedStructureProfile());
            return payload;
        }

        protected Map<String, Object> toAccelerationSignalProfile() {
            LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("tables", tables);
            payload.put("parserEngine", parserEngine);
            payload.put("joinCount", Integer.valueOf(joinCount));
            payload.put("predicateCount", Integer.valueOf(predicateCount));
            payload.put("groupByCount", Integer.valueOf(groupByCount));
            payload.put("aggregateFunctionCount", Integer.valueOf(aggregateFunctionCount));
            payload.put("stringProjectionCount", Integer.valueOf(stringProjectionCount));
            payload.put("largeStringAggregateCount", Integer.valueOf(largeStringAggregateCount));
            payload.put("windowFunctionCount", Integer.valueOf(windowFunctionCount));
            payload.put("udfFunctions", new ArrayList<String>(udfFunctions));
            payload.put("subqueryCount", Integer.valueOf(subqueryCount));
            payload.put("repeatedSubqueryCount", Integer.valueOf(repeatedSubqueryCount));
            payload.put("correlatedSubqueryCount", Integer.valueOf(correlatedSubqueryCount));
            payload.put("orPredicateCount", Integer.valueOf(orPredicateCount));
            payload.put("aggregateFunctions", new ArrayList<String>(aggregateFunctions));
            payload.put("datePredicateColumns", new ArrayList<String>(datePredicateColumns));
            payload.put("selectStar", Boolean.valueOf(selectStar));
            payload.put("selectStarItems", new ArrayList<String>(selectStarItems));
            payload.put("functionWrappedPredicateExpressions", new ArrayList<String>(functionWrappedPredicateExpressions));
            payload.put("warnings", warnings);
            payload.put("advancedStructureProfile", toAdvancedStructureProfile());
            return payload;
        }

        public Map<String, Object> toAdvancedStructureProfile() {
            return advancedStructureProfile.toMap(parserEngine);
        }

        public List<String> getTables() {
            return new ArrayList<String>(tables);
        }

        public String getParserEngine() {
            return parserEngine;
        }

        public String getNormalizedSql() {
            return normalizedSql;
        }

        protected RewriteOutcome getRewriteOutcome() {
            return rewriteOutcome == null ? RewriteOutcome.empty() : rewriteOutcome;
        }

        public Set<String> getAggregateFunctions() {
            return new LinkedHashSet<String>(aggregateFunctions);
        }

        public List<String> getProjectedColumns() {
            return new ArrayList<String>(projectedColumns);
        }

        public Set<String> getDatePredicateColumns() {
            return new LinkedHashSet<String>(datePredicateColumns);
        }

        public List<String> getSelectStarItems() {
            return new ArrayList<String>(selectStarItems);
        }

        public List<String> getFunctionWrappedPredicateExpressions() {
            return new ArrayList<String>(functionWrappedPredicateExpressions);
        }

        public List<String> getWarnings() {
            return new ArrayList<String>(warnings);
        }

        public int getProjectionCount() {
            return projectionCount;
        }

        public int getPredicateCount() {
            return predicateCount;
        }

        public int getJoinCount() {
            return joinCount;
        }

        public int getJoinCriteriaCount() {
            return joinCriteriaCount;
        }

        public int getGroupByCount() {
            return groupByCount;
        }

        public int getOrderByCount() {
            return orderByCount;
        }

        public int getOrderByExpressionCount() {
            return orderByExpressionCount;
        }

        public int getDuplicateOrderByKeyCount() {
            return duplicateOrderByKeyCount;
        }

        public int getDuplicateGroupByKeyCount() {
            return duplicateGroupByKeyCount;
        }

        public boolean isGroupByWithoutAggregate() {
            return groupByCount > 0 && aggregateFunctionCount == 0;
        }

        public int getAggregateFunctionCount() {
            return aggregateFunctionCount;
        }

        public int getStringProjectionCount() {
            return stringProjectionCount;
        }

        public int getStringConcatenationCount() {
            return stringConcatenationCount;
        }

        public int getLargeStringAggregateCount() {
            return largeStringAggregateCount;
        }

        public int getRepeatedSubqueryCount() {
            return repeatedSubqueryCount;
        }

        public int getWindowFunctionCount() {
            return windowFunctionCount;
        }

        public int getUdfFunctionCount() {
            return udfFunctions.size();
        }

        public int getSubqueryCount() {
            return subqueryCount;
        }

        public int getScalarSubqueryCount() {
            return scalarSubqueryCount;
        }

        public int getNestedSubqueryDepth() {
            return nestedSubqueryDepth;
        }

        public int getCorrelatedSubqueryCount() {
            return correlatedSubqueryCount;
        }

        public int getOrPredicateCount() {
            return orPredicateCount;
        }

        public int getFunctionWrappedPredicateCount() {
            return functionWrappedPredicateCount;
        }

        public int getLeadingWildcardLikeCount() {
            return leadingWildcardLikeCount;
        }

        public int getRandomOrderCount() {
            return randomOrderCount;
        }

        public int getNotExistsCount() {
            return notExistsCount;
        }

        public int getRepeatedTableScanCount() {
            return repeatedTableScanCount;
        }

        public int getComplexGraphScore() {
            return subqueryCount + joinCount + orPredicateCount + functionWrappedPredicateCount + randomOrderCount
                + repeatedSubqueryCount + largeStringAggregateCount;
        }

        public int getRepeatedExpressionCount() {
            return repeatedExpressionCount;
        }

        public boolean isSelectStar() {
            return selectStar;
        }

        public boolean isLimitPresent() {
            return limitPresent;
        }

        public boolean isDistinctPresent() {
            return distinctPresent;
        }

        public boolean isSetOperation() {
            return setOperation;
        }

        public List<String> getJoinTypes() {
            return new ArrayList<String>(joinTypes);
        }

        protected void markAdvancedProfileAvailable() {
            advancedStructureProfile.markAvailable();
        }

        protected void markAdvancedProfilePartial() {
            advancedStructureProfile.markPartial();
        }

        protected void recordCte(SqlWithItem withItem) {
            advancedStructureProfile.recordCte(withItem);
        }

        protected void recordTable(String tableName, String alias, String sourceType) {
            advancedStructureProfile.recordTable(tableName, alias, sourceType);
        }

        protected void recordProjection(SqlNode selectItem, List<String> sourceColumns) {
            advancedStructureProfile.recordProjection(selectItem, sourceColumns);
        }

        protected void recordPredicate(String clause,
                                     SqlNode predicate,
                                     List<String> sourceColumns,
                                     List<String> functionNames,
                                     String logicalContext,
                                     String groupId) {
            advancedStructureProfile.recordPredicate(
                clause,
                predicate,
                sourceColumns,
                functionNames,
                logicalContext,
                groupId
            );
        }

        protected void recordJoin(SqlNode leftItem, SqlJoin join) {
            advancedStructureProfile.recordJoin(leftItem, join);
        }

        protected void recordGroupBy(SqlNode expression, List<String> sourceColumns) {
            advancedStructureProfile.recordGroupBy(expression, sourceColumns);
        }

        protected void recordOrderBy(SqlNode orderByElement, List<String> sourceColumns) {
            advancedStructureProfile.recordOrderBy(orderByElement, sourceColumns);
        }

        protected void recordLimit(SqlNode fetch, SqlNode offset) {
            advancedStructureProfile.recordLimit(fetch, offset);
        }

        protected void recordSubqueryNode(SqlNode subSelect, String location, String alias, boolean correlated) {
            advancedStructureProfile.recordSubquery(subSelect, location, alias, subqueryDepth + 1, correlated);
        }

        protected void recordFunctionSignal(SqlBasicCall function, List<String> sourceColumns) {
            advancedStructureProfile.recordFunctionSignal(function, sourceColumns);
        }

        protected void recordSqlLevelFunctions(String sql) {
            advancedStructureProfile.recordSqlLevelFunctions(sql);
        }

        protected void recordExpression(Object expression) {
            if (expression == null) {
                return;
            }
            String key = expression.toString().trim().toUpperCase(Locale.ROOT);
            if (key.length() < 4 || isSimpleExpressionReference(expression, key)) {
                return;
            }
            Integer current = expressionFrequency.get(key);
            expressionFrequency.put(key, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
        }

        protected void recordSelectStar(Object selectItem) {
            String item = normalizeProfileText(selectItem);
            selectStarItems.add(item.isEmpty() ? "*" : item);
        }

        protected void recordFunctionWrappedPredicate(Object expression) {
            String item = normalizeProfileText(expression);
            if (!item.isEmpty()) {
                functionWrappedPredicateExpressions.add(item);
            }
        }

        protected boolean isSimpleExpressionReference(Object expression, String key) {
            if (expression instanceof SqlIdentifier
                || expression instanceof DereferenceExpression
                || expression instanceof SqlLiteral) {
                return true;
            }
            return key.matches("[A-Z_][A-Z0-9_]*(\\.[A-Z_][A-Z0-9_]*)*")
                || key.matches("\"[^\"]+\"(\\.\"[^\"]+\")*")
                || key.matches("`[^`]+`(\\.`[^`]+`)*");
        }

        protected void recalculateRepeatedExpressions() {
            int repeated = 0;
            for (Integer count : expressionFrequency.values()) {
                if (count != null && count.intValue() > 1) {
                    repeated += count.intValue() - 1;
                }
            }
            repeatedExpressionCount = repeated;
            repeatedTableScanCount = 0;
            for (Integer count : tableScanFrequency.values()) {
                if (count != null && count.intValue() > 1) {
                    repeatedTableScanCount += count.intValue() - 1;
                }
            }
            duplicateGroupByKeyCount = repeatedCount(groupByKeyFrequency);
            duplicateOrderByKeyCount = repeatedCount(orderByKeyFrequency);
            repeatedSubqueryCount = repeatedCount(subqueryFrequency);
        }

        protected void recordTableScan(String table) {
            if (table == null) {
                return;
            }
            String key = table.trim().toUpperCase(Locale.ROOT);
            if (key.isEmpty()) {
                return;
            }
            Integer current = tableScanFrequency.get(key);
            tableScanFrequency.put(key, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
        }

        protected void recordGroupByKey(Object expression) {
            recordFrequency(groupByKeyFrequency, normalizeProfileKey(expression));
        }

        protected void recordOrderByKey(Object expression) {
            recordFrequency(orderByKeyFrequency, normalizeProfileKey(expression));
        }

        protected void recordSubquery(Object subquery) {
            recordFrequency(subqueryFrequency, normalizeProfileKey(subquery));
        }

        protected List<String> getRepeatedSubquerySamples() {
            List<String> samples = new ArrayList<String>();
            for (Map.Entry<String, Integer> entry : subqueryFrequency.entrySet()) {
                if (entry.getValue() != null && entry.getValue().intValue() > 1) {
                    samples.add(entry.getKey());
                }
            }
            return samples;
        }

        protected void recordFrequency(Map<String, Integer> frequency, String key) {
            if (frequency == null || key == null || key.isEmpty()) {
                return;
            }
            Integer current = frequency.get(key);
            frequency.put(key, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
        }

        protected int repeatedCount(Map<String, Integer> frequency) {
            int repeated = 0;
            for (Integer count : frequency.values()) {
                if (count != null && count.intValue() > 1) {
                    repeated += count.intValue() - 1;
                }
            }
            return repeated;
        }

        protected String normalizeProfileKey(Object value) {
            if (value == null) {
                return "";
            }
            return normalizeProfileText(value).toUpperCase(Locale.ROOT);
        }

        protected String normalizeProfileText(Object value) {
            if (value == null) {
                return "";
            }
            return value.toString()
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim()
                .replaceAll("\\s+", " ");
        }

        protected void pushAliasScope(Set<String> aliases) {
            aliasScopes.push(aliases == null ? Collections.<String>emptySet() : aliases);
        }

        protected void popAliasScope() {
            if (!aliasScopes.isEmpty()) {
                aliasScopes.pop();
            }
        }

        protected boolean referencesVisibleAlias(String sql) {
            if (sql == null || aliasScopes.isEmpty()) {
                return false;
            }
            String normalized = removeSimpleIdentifierQuotes(sql).toUpperCase(Locale.ROOT);
            for (Set<String> scope : aliasScopes) {
                for (String alias : scope) {
                    if (alias != null && !alias.isEmpty() && normalized.contains(alias + ".")) {
                        return true;
                    }
                }
            }
            return false;
        }

        protected void updateNestedSubqueryDepth() {
            nestedSubqueryDepth = Math.max(nestedSubqueryDepth, subqueryDepth + 1);
        }
    }

}
