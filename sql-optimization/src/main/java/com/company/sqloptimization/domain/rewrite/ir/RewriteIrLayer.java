package com.company.sqloptimization.domain.rewrite.ir;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum RewriteIrLayer {
    L1_AST("L1_AST", "语法树层", "具体 SQL 方言节点，如 JSqlParser Expression 或 Calcite SqlNode。"),
    L2_TABLE_REFERENCE("L2_TABLE_REFERENCE", "表引用层", "物理表、CTE、派生表、别名、访问路径与谓词下推候选。"),
    L3_QUERY_BLOCK("L3_QUERY_BLOCK", "查询块层", "子查询、CTE 或根查询单元的输入、输出、谓词与聚合。"),
    L4_RELATIONAL_ALGEBRA("L4_RELATIONAL_ALGEBRA", "关系代数层", "选择、投影、聚合、连接、集合运算的标准形。"),
    L5_BUSINESS_INTENT("L5_BUSINESS_INTENT", "业务意图层", "时间锚点、度量、维度与筛选组成的业务意图模式。");

    private final String code;
    private final String titleZh;
    private final String description;

    RewriteIrLayer(String code, String titleZh, String description) {
        this.code = code;
        this.titleZh = titleZh;
        this.description = description;
    }

    public static List<RewriteIrLayer> ordered() {
        return Collections.unmodifiableList(Arrays.asList(values()));
    }

    public String getCode() {
        return code;
    }

    public String getTitleZh() {
        return titleZh;
    }

    public String getDescription() {
        return description;
    }
}
