package com.company.sqloptimization.domain.task;

/**
 * 受支持的加速建议类别。
 */
public enum AccelerationSuggestionType {
    PRECOMPUTE,
    PARTITION,
    BUCKET,
    SPLIT,
    REPLACE,
    ALL
}
