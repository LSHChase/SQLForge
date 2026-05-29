package com.company.sqloptimization.application.service;

import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlSetOperator;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.validate.SqlConformanceEnum;

final class CommonSubgraphSqlParser {

    private CommonSubgraphSqlParser() {
    }

    static SqlSelect parseSelect(String sql) {
        SqlNode statement = parseStatement(sql);
        SqlSelect select = unwrapSelect(statement);
        if (select == null) {
            throw new IllegalArgumentException("not select");
        }
        return select;
    }

    static SqlNode parseStatement(String sql) {
        String normalizedSql = CommonSubgraphSqlText.trimTrailingSemicolon(SqlDialectNormalizer.normalize(sql));
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

    static SqlNode parseStatementOrNull(String sql) {
        try {
            return parseStatement(sql);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    static SqlSelect unwrapSelect(SqlNode node) {
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

    static boolean isSelectLike(SqlNode relation) {
        return relation instanceof SqlSelect
            || relation instanceof SqlWith
            || relation instanceof SqlOrderBy
            || isUnionAll(relation);
    }

    private static SqlNode parseNormalizedStatement(String sql) throws Exception {
        SqlParser.Config parserConfig = SqlParser.config()
            .withConformance(SqlConformanceEnum.LENIENT)
            .withUnquotedCasing(Casing.UNCHANGED);
        return SqlParser.create(CommonSubgraphSqlText.trimTrailingSemicolon(sql), parserConfig).parseStmt();
    }

    private static String backtickQuotedIdentifiersToDoubleQuoted(String sql) {
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

    private static boolean isUnionAll(SqlNode relation) {
        if (!(relation instanceof SqlCall) || relation.getKind() != SqlKind.UNION) {
            return false;
        }
        return ((SqlCall) relation).getOperator() instanceof SqlSetOperator
            && ((SqlSetOperator) ((SqlCall) relation).getOperator()).isAll();
    }
}
