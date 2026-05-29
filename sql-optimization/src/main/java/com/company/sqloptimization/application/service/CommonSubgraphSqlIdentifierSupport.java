package com.company.sqloptimization.application.service;

import java.util.List;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;

final class CommonSubgraphSqlIdentifierSupport {

    private CommonSubgraphSqlIdentifierSupport() {
    }

    static String aliasName(SqlNode item) {
        if (item instanceof SqlBasicCall && item.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) item).getOperandList();
            if (operands.size() >= 2) {
                return operands.get(1).toString();
            }
        }
        return "";
    }

    static String identifierTail(SqlIdentifier identifier) {
        if (identifier == null) {
            return "";
        }
        if (identifier.names != null && !identifier.names.isEmpty()) {
            return identifier.names.get(identifier.names.size() - 1);
        }
        String text = CommonSubgraphSqlText.cleanIdentifier(identifier.toString());
        int dot = text.lastIndexOf('.');
        return dot >= 0 ? text.substring(dot + 1) : text;
    }
}
