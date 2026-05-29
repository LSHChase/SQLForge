package com.company.sqloptimization.application.service;

import java.util.List;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;
import org.springframework.util.StringUtils;

final class CommonSubgraphRelationReferenceCounter {

    private CommonSubgraphRelationReferenceCounter() {
    }

    static int relationReferenceCount(String sql, String relationName) {
        if (!StringUtils.hasText(sql) || !StringUtils.hasText(relationName)) {
            return 0;
        }
        try {
            return relationReferenceCount(CommonSubgraphSqlParser.parseStatement(sql), relationName);
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private static int relationReferenceCount(SqlNode node, String relationName) {
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

    private static int relationReferenceCountInFrom(SqlNode from, String relationName) {
        if (from == null) {
            return 0;
        }
        if (from instanceof SqlIdentifier) {
            return CommonSubgraphRelationName.relationNameMatches((SqlIdentifier) from, relationName) ? 1 : 0;
        }
        if (from instanceof SqlJoin) {
            return relationReferenceCountInFrom(((SqlJoin) from).getLeft(), relationName)
                + relationReferenceCountInFrom(((SqlJoin) from).getRight(), relationName);
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
            if (operands.isEmpty()) {
                return 0;
            }
            SqlNode relation = operands.get(0);
            if (relation instanceof SqlIdentifier) {
                return CommonSubgraphRelationName.relationNameMatches((SqlIdentifier) relation, relationName) ? 1 : 0;
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
}
