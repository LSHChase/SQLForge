package com.company.sqlforge.common.utils;

import org.springframework.util.StringUtils;

/**
 * 执行和分析链路共用的 SQL 库名前缀兼容前处理。
 */
public final class SqlCatalogQualifierRewriteUtils {

    private SqlCatalogQualifierRewriteUtils() {
    }

    public static String rewriteBiViewCatalogQualifier(String sql) {
        if (!StringUtils.hasText(sql)) {
            return sql;
        }
        return SqlCatalogQualifierRewriteScanner.rewriteBiViewCatalogQualifier(sql);
    }
}
