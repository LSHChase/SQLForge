package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWith;
import org.springframework.util.StringUtils;

final class CommonSubgraphAstCandidateExtractor {

    private CommonSubgraphAstCandidateExtractor() {
    }

    static List<CommonSubgraphCandidate> subgraphCandidatesFromAst(String sourceSql) {
        if (!StringUtils.hasText(sourceSql)) {
            return Collections.emptyList();
        }
        try {
            List<CommonSubgraphCandidate> result = new ArrayList<CommonSubgraphCandidate>();
            collectAstSubgraphCandidates(CommonSubgraphSqlParser.parseStatement(sourceSql), result, false);
            return result;
        } catch (RuntimeException ex) {
            return Collections.emptyList();
        }
    }

    private static void collectAstSubgraphCandidates(SqlNode node,
                                                     List<CommonSubgraphCandidate> result,
                                                     boolean relationContext) {
        if (node == null) {
            return;
        }
        if (node instanceof SqlBasicCall && node.getKind() == SqlKind.AS) {
            collectAsCandidate((SqlBasicCall) node, result, relationContext);
            return;
        }
        if (node instanceof SqlSelect) {
            collectSelectCandidates((SqlSelect) node, result);
            return;
        }
        if (node instanceof SqlNodeList) {
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                collectAstSubgraphCandidates(item, result, relationContext);
            }
            return;
        }
        if (node instanceof SqlWith) {
            collectAstSubgraphCandidates(((SqlWith) node).withList, result, false);
            collectAstSubgraphCandidates(((SqlWith) node).body, result, false);
            return;
        }
        if (node instanceof SqlOrderBy) {
            collectAstSubgraphCandidates(((SqlOrderBy) node).query, result, relationContext);
            return;
        }
        if (node instanceof SqlCall) {
            boolean childRelationContext = relationContext || node.getKind() == SqlKind.JOIN;
            for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                collectAstSubgraphCandidates(operand, result, childRelationContext);
            }
        }
    }

    private static void collectAsCandidate(SqlBasicCall node,
                                           List<CommonSubgraphCandidate> result,
                                           boolean relationContext) {
        List<SqlNode> operands = node.getOperandList();
        if (operands.size() < 2) {
            return;
        }
        SqlNode relation = operands.get(0);
        SqlSelect select = CommonSubgraphSqlParser.unwrapSelect(relation);
        String alias = CommonSubgraphSqlText.cleanIdentifier(operands.get(1).toString());
        if (relationContext && select != null && StringUtils.hasText(alias)) {
            String query = CommonSubgraphSqlText.normalizeSubgraphSql(relation.toString());
            if (StringUtils.hasText(query)) {
                result.add(new CommonSubgraphCandidate("DERIVED_TABLE", alias, alias, query, false));
            }
        }
        collectAstSubgraphCandidates(relation, result, relationContext);
    }

    private static void collectSelectCandidates(SqlSelect select, List<CommonSubgraphCandidate> result) {
        collectAstSubgraphCandidates(select.getSelectList(), result, false);
        collectAstSubgraphCandidates(select.getFrom(), result, true);
        collectAstSubgraphCandidates(select.getWhere(), result, false);
        collectAstSubgraphCandidates(select.getGroup(), result, false);
        collectAstSubgraphCandidates(select.getHaving(), result, false);
    }
}
