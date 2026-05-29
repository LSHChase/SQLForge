package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.CommonSubgraphReason.reason;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.springframework.util.StringUtils;

final class CommonSubgraphOutputColumnAnalyzer {

    private CommonSubgraphOutputColumnAnalyzer() {
    }

    static CommonSubgraphOutputColumns outputColumns(String subgraphSql) {
        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        LinkedHashSet<String> columns = new LinkedHashSet<String>();
        try {
            SqlSelect select = CommonSubgraphSqlParser.parseSelect(subgraphSql);
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
                String output = outputColumnName(item);
                if (!StringUtils.hasText(output)) {
                    blockingReasons.add(reason(
                        "SUBGRAPH_OUTPUT_COLUMNS_UNRESOLVED",
                        "公共子图表达式输出缺少稳定别名，不能证明上层查询字段覆盖。"
                    ));
                    continue;
                }
                columns.add(CommonSubgraphSqlText.cleanIdentifier(output));
            }
            CommonSubgraphOutputColumns textColumns = outputColumnsByText(subgraphSql);
            columns.addAll(textColumns.columns);
        } catch (RuntimeException ex) {
            return outputColumnsByText(subgraphSql);
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        for (String column : columns) {
            normalized.add(CommonSubgraphSqlText.normalizeIdentifier(column));
        }
        return new CommonSubgraphOutputColumns(new ArrayList<String>(columns), normalized, blockingReasons);
    }

    static CommonSubgraphOutputColumns withRequiredQualifiedColumns(
        CommonSubgraphOutputColumns outputColumns,
        Set<String> requiredColumns) {
        if (outputColumns == null || requiredColumns == null || requiredColumns.isEmpty()) {
            return outputColumns;
        }
        LinkedHashSet<String> columns = new LinkedHashSet<String>(outputColumns.columns);
        columns.addAll(requiredColumns);
        LinkedHashSet<String> normalized = new LinkedHashSet<String>(outputColumns.normalizedColumns);
        for (String column : requiredColumns) {
            normalized.add(CommonSubgraphSqlText.normalizeIdentifier(column));
        }
        return new CommonSubgraphOutputColumns(
            new ArrayList<String>(columns),
            normalized,
            outputColumns.blockingReasons
        );
    }

    private static CommonSubgraphOutputColumns outputColumnsByText(String subgraphSql) {
        LinkedHashSet<String> columns = new LinkedHashSet<String>();
        String firstSelectList = CommonSubgraphProjectionText.firstSelectList(subgraphSql);
        if (!StringUtils.hasText(firstSelectList)) {
            return blocked("SUBGRAPH_SELECT_UNSUPPORTED", "公共子图 SQL 无法解析出可证明的 SELECT 输出字段。");
        }
        for (String item : CommonSubgraphProjectionText.splitTopLevelComma(firstSelectList)) {
            String alias = CommonSubgraphProjectionText.trailingAlias(item);
            if (!StringUtils.hasText(alias)) {
                alias = CommonSubgraphProjectionText.trailingColumnName(item);
            }
            if (StringUtils.hasText(alias)) {
                columns.add(CommonSubgraphSqlText.cleanIdentifier(alias));
            }
        }
        if (columns.isEmpty()) {
            return blocked("SUBGRAPH_OUTPUT_COLUMNS_UNRESOLVED", "公共子图表达式输出缺少稳定别名，不能证明上层查询字段覆盖。");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        for (String column : columns) {
            normalized.add(CommonSubgraphSqlText.normalizeIdentifier(column));
        }
        return new CommonSubgraphOutputColumns(
            new ArrayList<String>(columns),
            normalized,
            Collections.<Map<String, Object>>emptyList()
        );
    }

    private static CommonSubgraphOutputColumns blocked(String code, String description) {
        return new CommonSubgraphOutputColumns(
            Collections.<String>emptyList(),
            Collections.<String>emptySet(),
            Collections.singletonList(reason(code, description))
        );
    }

    private static String outputColumnName(SqlNode item) {
        String output = aliasName(item);
        if (!StringUtils.hasText(output)) {
            SqlNode expression = stripAlias(item);
            if (expression instanceof SqlIdentifier) {
                output = identifierTail((SqlIdentifier) expression);
            }
        }
        return output;
    }

    private static boolean isStar(SqlNode item) {
        if (item instanceof SqlIdentifier) {
            return ((SqlIdentifier) item).isStar();
        }
        return item != null && "*".equals(item.toString().trim());
    }

    private static String aliasName(SqlNode item) {
        if (item instanceof SqlBasicCall && item.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) item).getOperandList();
            if (operands.size() >= 2) {
                return operands.get(1).toString();
            }
        }
        return "";
    }

    private static SqlNode stripAlias(SqlNode item) {
        if (item instanceof SqlBasicCall && item.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) item).getOperandList();
            if (!operands.isEmpty()) {
                return operands.get(0);
            }
        }
        return item;
    }

    private static String identifierTail(SqlIdentifier identifier) {
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
