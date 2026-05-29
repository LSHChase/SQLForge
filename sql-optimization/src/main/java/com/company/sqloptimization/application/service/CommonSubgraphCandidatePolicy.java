package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.CommonSubgraphReason.reason;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.springframework.util.StringUtils;

final class CommonSubgraphCandidatePolicy {

    private static final Set<String> NON_DETERMINISTIC_FUNCTION_NAMES = new LinkedHashSet<String>(Arrays.asList(
        "RAND", "RANDOM", "UUID", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "LOCALTIME",
        "LOCALTIMESTAMP", "NOW", "SYSDATE", "SESSION_USER", "CURRENT_USER"
    ));

    private CommonSubgraphCandidatePolicy() {
    }

    static List<Map<String, Object>> structuralBlockingReasons(
        Map<String, Object> advancedStructureProfile,
        SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            reasons.add(reason(
                "ADVANCED_STRUCTURE_PROFILE_REQUIRED",
                "缺少高级结构画像，不能生成 COMMON_SUBGRAPH_MV。"
            ));
            return reasons;
        }
        if (!"AVAILABLE".equals(CommonSubgraphSqlText.text(advancedStructureProfile.get("profileStatus")))) {
            reasons.add(reason(
                "ADVANCED_STRUCTURE_PROFILE_REQUIRED",
                "高级结构画像未完整可用，不能生成可激活的 COMMON_SUBGRAPH_MV SQL。"
            ));
        }
        if (profile != null && profile.getCorrelatedSubqueryCount() > 0) {
            reasons.add(reason(
                "CORRELATED_SUBQUERY_COMMON_SUBGRAPH_UNSUPPORTED",
                "相关子查询依赖外层作用域，不能独立物化为 COMMON_SUBGRAPH_MV。"
            ));
        }
        return reasons;
    }

    static Map<String, Object> commonSubgraphCandidateRequiredReason(
        SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        if (profile != null && profile.getRepeatedSubqueryCount() > 0) {
            return reason(
                "REPEATED_SUBQUERY_COMMON_SUBGRAPH_BLOCKED",
                "重复 predicate/scalar 子查询需要证明无关联、单列输出且 rewrite 完全等价，AMV-009 先保守阻断。"
            );
        }
        return reason(
            "COMMON_SUBGRAPH_CANDIDATE_REQUIRED",
            "未找到可独立物化的命名 CTE 或 FROM/JOIN 派生表。"
        );
    }

    static List<CommonSubgraphCandidate> candidateSelectableSubgraphs(List<CommonSubgraphCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        List<CommonSubgraphCandidate> result = new ArrayList<CommonSubgraphCandidate>();
        for (CommonSubgraphCandidate candidate : candidates) {
            if (candidateBlockingReasons(candidate).isEmpty()) {
                result.add(candidate);
            }
        }
        return result;
    }

    static List<Map<String, Object>> candidateBlockingReasons(CommonSubgraphCandidate candidate) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        if (candidate == null) {
            reasons.add(reason(
                "COMMON_SUBGRAPH_CANDIDATE_REQUIRED",
                "未找到可独立物化的命名 CTE 或 FROM/JOIN 派生表。"
            ));
            return reasons;
        }
        if (candidate.recursive) {
            reasons.add(reason(
                "RECURSIVE_CTE_COMMON_SUBGRAPH_UNSUPPORTED",
                "递归 CTE 不能独立物化为 AMV-009 的公共子图 MV。"
            ));
        }
        SqlNode candidateNode = CommonSubgraphSqlParser.parseStatementOrNull(candidate.subgraphSql);
        if (candidateNode == null) {
            reasons.add(reason(
                "COMMON_SUBGRAPH_CANDIDATE_PARSE_UNSUPPORTED",
                "公共子图候选无法经 Calcite 重新解析，不能证明其可独立物化。"
            ));
            return reasons;
        }
        if (hasWindowFunction(candidateNode)) {
            reasons.add(reason(
                "WINDOW_FUNCTION_COMMON_SUBGRAPH_UNSUPPORTED",
                "公共子图包含窗口函数时需要证明窗口作用域不变，AMV-009 默认阻断。"
            ));
        }
        if (hasNonDeterministicFunction(candidateNode)) {
            reasons.add(reason(
                "NON_DETERMINISTIC_FUNCTION_COMMON_SUBGRAPH_UNSUPPORTED",
                "公共子图包含当前时间、随机或会话函数，缺少稳定化策略时不能物化。"
            ));
        }
        if (CommonSubgraphSqlText.hasOrderOrLimit(candidate.subgraphSql)) {
            reasons.add(reason(
                "SUBGRAPH_ORDER_LIMIT_UNSUPPORTED",
                "公共子图内部包含 ORDER BY 或 LIMIT，物化后可能改变排序分页语义。"
            ));
        }
        return reasons;
    }

    private static boolean hasWindowFunction(SqlNode node) {
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

    private static boolean hasNonDeterministicFunction(SqlNode node) {
        if (node == null) {
            return false;
        }
        if (node instanceof SqlCall) {
            SqlCall call = (SqlCall) node;
            if (call.getOperator() != null) {
                String functionName = call.getOperator().getName();
                if (!call.getOperator().isDeterministic()
                    || call.getOperator().isDynamicFunction()
                    || NON_DETERMINISTIC_FUNCTION_NAMES.contains(CommonSubgraphSqlText.upperText(functionName))) {
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
}
