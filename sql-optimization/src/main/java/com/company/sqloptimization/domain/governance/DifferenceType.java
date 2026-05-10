package com.company.sqloptimization.domain.governance;

public enum DifferenceType {
    NONE,
    SCHEMA_DIFF,
    ROW_COUNT_DIFF,
    KEY_SET_DIFF,
    ORDER_DIFF,
    VALUE_DIFF,
    CHECKSUM_DIFF,
    TIMEZONE_OR_PRECISION_DIFF,
    UNKNOWN
}
