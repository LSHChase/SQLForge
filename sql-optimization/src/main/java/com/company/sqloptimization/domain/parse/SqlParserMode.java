package com.company.sqloptimization.domain.parse;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

public enum SqlParserMode {
    APACHE_CALCITE,
    APACHE_CALCITE_WITH_PLAN;

    public static final String DEFAULT_VALUE = "APACHE_CALCITE";
    public static final String REQUEST_PATTERN = "^\\s*$|(?i:APACHE_CALCITE|APACHE_CALCITE_WITH_PLAN)";

    public static SqlParserMode resolve(String value) {
        if (!StringUtils.hasText(value)) {
            return APACHE_CALCITE;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return SqlParserMode.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "不支持的 parserMode：" + value + " [parserMode]"
            );
        }
    }

    public static SqlParserMode resolveDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return APACHE_CALCITE;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return SqlParserMode.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return APACHE_CALCITE;
        }
    }

    public SqlParserMode structureMode() {
        if (this == APACHE_CALCITE_WITH_PLAN) {
            return APACHE_CALCITE;
        }
        return this;
    }

    public boolean requiresPlanAnalysis() {
        return this == APACHE_CALCITE_WITH_PLAN;
    }
}
