package com.company.sqloptimization.domain.parse;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

public enum SqlParserMode {
    JSQLPARSER,
    APACHE_CALCITE,
    JSQLPARSER_WITH_PLAN,
    APACHE_CALCITE_WITH_PLAN;

    public static final String DEFAULT_VALUE = "JSQLPARSER";
    public static final String REQUEST_PATTERN = "^\\s*$|(?i:JSQLPARSER|APACHE_CALCITE|JSQLPARSER_WITH_PLAN|APACHE_CALCITE_WITH_PLAN)";

    public static SqlParserMode resolve(String value) {
        if (!StringUtils.hasText(value)) {
            return JSQLPARSER;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return SqlParserMode.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "Unsupported parserMode: " + value + " [parserMode]"
            );
        }
    }

    public static SqlParserMode resolveDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return JSQLPARSER;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return SqlParserMode.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return JSQLPARSER;
        }
    }

    public SqlParserMode structureMode() {
        if (this == JSQLPARSER_WITH_PLAN) {
            return JSQLPARSER;
        }
        if (this == APACHE_CALCITE_WITH_PLAN) {
            return APACHE_CALCITE;
        }
        return this;
    }

    public boolean requiresPlanAnalysis() {
        return this == JSQLPARSER_WITH_PLAN || this == APACHE_CALCITE_WITH_PLAN;
    }
}
