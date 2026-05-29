package com.company.sqloptimization.application.service;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

final class CommonSubgraphRewriteAstSupport {

    private CommonSubgraphRewriteAstSupport() {
    }

    static SqlNode aliasedMvReference(String mvName, String alias) {
        return aliasedMvReference(
            mvName,
            new SqlIdentifier(CommonSubgraphSqlText.firstText(alias, mvName), SqlParserPos.ZERO)
        );
    }

    static SqlNode aliasedMvReference(String mvName, SqlNode alias) {
        SqlNode mvIdentifier = new SqlIdentifier(mvName, SqlParserPos.ZERO);
        SqlNode resolvedAlias = alias == null ? new SqlIdentifier(mvName, SqlParserPos.ZERO) : alias;
        return SqlStdOperatorTable.AS.createCall(SqlParserPos.ZERO, mvIdentifier, resolvedAlias);
    }

    static void setOperandIfPossible(SqlCall call, int index, SqlNode rewritten) {
        try {
            call.setOperand(index, rewritten);
        } catch (UnsupportedOperationException ex) {
            // 少数 Calcite 节点暴露不可变 operand 视图，常见查询节点已在父级 setter 中处理。
        }
    }
}
